package org.fd;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.ai.chat.Message;
import burp.api.montoya.ai.chat.PromptException;
import burp.api.montoya.ai.chat.PromptOptions;
import burp.api.montoya.ai.chat.PromptResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Persistence;

import java.util.ArrayList;
import java.util.List;

import static burp.api.montoya.ai.chat.Message.*;

public class BurpAiClient implements LlmClient {

    Ai ai;
    List<Message> history;
    Prompt prompts;
    Logging logging;
    Persistence persistence;
    double temperature;
    boolean htmlEncodeIssues;

    boolean debug;

    private final Object historyLock = new Object();
    private long historyEpoch = 0;

    public BurpAiClient(MontoyaApi api, Prompt prompts, boolean debug) {
        this.ai = api.ai();
        this.persistence = api.persistence();
        this.prompts = prompts;
        this.history = new ArrayList<>();;
        this.logging = api.logging();
        this.temperature = Double.valueOf(loadPref("TEMPERATURE", Prefs.DEFAULT_TEMPERATURE));
        this.debug = debug;
        this.htmlEncodeIssues = loadPref("HTML_ENCODE", Prefs.DEFAULT_HTML_ENCODE).startsWith("YES");
    }

    // Function that calls LLM without history
    public String oneShot(String userPrompt) throws PromptException {

        if (isAiEnabled()) {

            // Create the message array that includes the system prompt and the user message
            Message[] messages = new Message[]{systemMessage(this.prompts.getPrompt("reportingPrompt")), userMessage(userPrompt)};

            // We execute the LLM call
            PromptResponse response;
            if(this.temperature != 0.0)
                response = this.ai.prompt().execute(PromptOptions.promptOptions().withTemperature(temperature), messages);
            else
                response = this.ai.prompt().execute(messages);

            // Debug block
            if (debug) {
                this.logging.logToOutput("* BurpAI Client one shot");
                this.logging.logToOutput("** User prompt:");
                this.logging.logToOutput(userPrompt);
                this.logging.logToOutput("** Response:");
                this.logging.logToOutput(response.content());
                this.logging.logToOutput("");
            }

            // We return the assistant response
            return response.content();

        } else {
            return null;
        }

    }

    // Function that call LLM using history
    public String chat(String userPrompt) throws PromptException {

        if(isAiEnabled()) {

            Message[] snapshot;
            long epochAtSend;

            synchronized (historyLock) {
                if (history.isEmpty()) {
                    history.add(systemMessage(this.prompts.getPrompt("chatPrompt")));
                }
                history.add(userMessage(userPrompt));
                epochAtSend = historyEpoch;
                snapshot = history.toArray(Message[]::new);
            }

            // We send the full message list to the AI, in order to receive a response
            // that consider the whole message history
            PromptResponse response;
            if(this.temperature != 0.0)
                response = this.ai.prompt().execute(PromptOptions.promptOptions().withTemperature(temperature),
                        snapshot);
            else
                response = this.ai.prompt().execute(snapshot);

            // Debug block
            if (debug) {
                this.logging.logToOutput("* BurpAI Client chat");
                this.logging.logToOutput("** User prompt:");
                this.logging.logToOutput(userPrompt);
                this.logging.logToOutput("** Response:");
                this.logging.logToOutput(response.content());
                this.logging.logToOutput("");
            }

            // We save the LLM response as an assistant message in the history
            synchronized (historyLock) {
                if (historyEpoch == epochAtSend) {
                    history.add(assistantMessage(response.content()));
                }
            }

            // N.B. It is a very simple chat for quick questions. It does not implement trimming, etc. necessary
            // for complex uses

            // We return the assistant response
            return response.content();

        } else {

            return null;

        }

    }

    public boolean isAiEnabled() {
        return this.ai.isEnabled();
    }

    public void clearHistory() {
        //this.history = null;
        synchronized (historyLock) {
            history.clear();
            historyEpoch++;
        }
    }

    @Override
    public String getBaseUrl() {
        //this.logging.logToError("Burp AI has not changeable base url");
        return "";
    }

    @Override
    public void setBaseUrl(String url) {
        //this.logging.logToError("Burp AI has not changeable base url");
    }

    @Override
    public String getModel() {
        //this.logging.logToError("Burp AI has not changeable base url");
        return "";
    }

    @Override
    public void setModel(String model) {
        //this.logging.logToError("Burp AI has not changeable base url");
    }

    @Override
    public double getTemperature() {
        return this.temperature;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public boolean htmlEncodeIssues() {
        return htmlEncodeIssues;
    }

    @Override
    public void setHtmlEncodeIssue(boolean htmlEncodeIssues) {
        this.htmlEncodeIssues = htmlEncodeIssues;
    }

    @Override
    public String getApiKey() {
        //this.logging.logToError("Burp AI has not changeable Api key");
        return "";
    }

    @Override
    public void setApiKey(String apiKey) {
        //this.logging.logToError("Burp AI has not changeable Api key");
    }

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.BURP_AI;
    }

    private String loadPref(String key, String defaultValue) {
        String saved = persistence.preferences().getString(Prefs.PREFIX + key);
        return (saved != null && !saved.isBlank()) ? saved : defaultValue;
    }

}
