package org.fd;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.ai.chat.PromptException;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OllamaClient implements LlmClient{

    List<String[]> history;
    Prompt prompts;
    Logging logging;
    double temperature;
    String model;
    String apiKey;
    String baseUrl;
    boolean htmlEncodeIssues;

    MontoyaApi api;
    boolean debug;
    Ai ai;

    // Lock
    private final Object historyLock = new Object();
    private long historyEpoch = 0;

    private static final String CHAT_ENDPOINT   = "/v1/chat/completions";

    public OllamaClient(MontoyaApi api, Prompt prompts, boolean debug) {
        //this.systemMessage = systemMessage(systemPrompt);
        this.api = api;
        this.prompts = prompts;
        this.history = new ArrayList<>();
        this.logging = api.logging();
        this.ai = api.ai();

        this.baseUrl = loadPref("BASE_URL", Prefs.DEFAULT_URL);
        this.model = loadPref("MODEL", Prefs.DEFAULT_MODEL);
        this.apiKey = loadPref("API_KEY", "");
        this.temperature = Double.valueOf(loadPref("TEMPERATURE", Prefs.DEFAULT_TEMPERATURE));
        this.htmlEncodeIssues = loadPref("HTML_ENCODE", Prefs.DEFAULT_HTML_ENCODE).startsWith("YES");

        this.debug = debug;

    }

    @Override
    public String oneShot(String userPrompt) throws PromptException {

        // With Burp Suite Pro, guidelines require to check if AI is enabled also with local models.
        // This check is skipped for Community Edition, that does not include Burp AI but that can
        // use the extension with local models.
        if(isAiEnabled()) {

            JSONObject payload = new JSONObject()
                    .put("model", model)
                    .put("stream", false)
                    .put("messages", new JSONArray()
                            .put(new JSONObject().put("role", "system").put("content", this.prompts.getPrompt("reportingPrompt")))
                            .put(new JSONObject().put("role", "user").put("content", userPrompt)));

            if(temperature != 0.0)
                payload.put("temperature", temperature);

            String responseBody = doPost(payload.toString());

            // Debug block
            if (debug) {
                this.logging.logToOutput("* Ollama/OpenAI one shot");
                this.logging.logToOutput("** User prompt:");
                this.logging.logToOutput(userPrompt);
                this.logging.logToOutput("** Chat payload:");
                this.logging.logToOutput(payload);
                this.logging.logToOutput("** Response body:");
                this.logging.logToOutput(responseBody);
                this.logging.logToOutput("");
            }

            try {
                return parseContentFromResponse(responseBody);
            } catch(JSONException e) {
                this.logging.logToError("Error in JSON exception:");
                this.logging.logToError(responseBody);
                return null;
            }

        } else {
            return null;
        }

    }

    @Override
    public String chat(String userPrompt) throws PromptException {

        // With Burp Suite Pro, guidelines require to check if AI is enabled also with local models.
        // This check is skipped for Community Edition, that does not include Burp AI but that can
        // use the extension with local models.
        if(isAiEnabled()) {

            String payload;
            long epochAtSend;
            synchronized (historyLock) {
                if (history.isEmpty()) {
                    history.add(new String[]{"system", this.prompts.getPrompt("chatPrompt")});
                }
                history.add(new String[]{"user", userPrompt});
                epochAtSend = historyEpoch;
                payload = buildChatPayload();
            }

            String responseBody = doPost(payload);

            try {

                String reply = parseContentFromResponse(responseBody);

                // Debug block
                if (debug) {
                    this.logging.logToOutput("* Ollama/OpenAI chat");
                    this.logging.logToOutput("** User prompt:");
                    this.logging.logToOutput(userPrompt);
                    this.logging.logToOutput("** Chat payload:");
                    this.logging.logToOutput(payload);
                    this.logging.logToOutput("** Response body:");
                    this.logging.logToOutput(responseBody);
                    this.logging.logToOutput("");
                }

                synchronized (historyLock) {
                    if (historyEpoch == epochAtSend) {
                        history.add(new String[]{"assistant", reply});
                    }
                }

                return reply;

            } catch(JSONException e) {

                logging.logToError("[AI Reporter] Error in JSON response. Response:");
                logging.logToError(responseBody);
                return null;

            }

        } else {

            return null;

        }
    }

    @Override
    public boolean isAiEnabled() {
        // With Burp Suite Pro, guidelines require to check if AI is enabled also with local models.
        // This check is skipped for Community Edition, that does not include Burp AI but that can
        // use the extension with local models.
        return ai.isEnabled() || api.burpSuite().version().edition() == BurpSuiteEdition.COMMUNITY_EDITION;

        // Uncomment and comment previous line to disable AI switch for Ollama mode
        //return true;

    }

    @Override
    public void clearHistory() {
        synchronized (historyLock) {
            history.clear();     // svuoto invece di riassegnare: il riferimento resta valido
            historyEpoch++;      // invalido eventuali turni chat() in volo
        }
    }

    @Override
    public String getBaseUrl() {
        return this.baseUrl;
    }

    @Override
    public void setBaseUrl(String url) {
        this.baseUrl = url;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public void setModel(String model) {
        this.model = model;
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
    public String getApiKey() {
        return this.apiKey;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean htmlEncodeIssues() {
        return htmlEncodeIssues;
    }

    @Override
    public void setHtmlEncodeIssue(boolean htmlEncodeIssues) {
        this.htmlEncodeIssues = htmlEncodeIssues;
    }

    private String buildChatPayload() {

        JSONArray messages = new JSONArray();

        synchronized (historyLock) {
            for (String[] msg : history) {
                messages.put(new JSONObject()
                        .put("role", msg[0])
                        .put("content", msg[1]));
            }
        }

        JSONObject payload = new JSONObject()
                .put("model", model)
                .put("stream", false)
                .put("messages", messages);

        if(temperature != 0.0)
            payload.put("temperature", temperature);

        return payload.toString();

    }

    private String doPost(String jsonPayload) {

        String fullUrl = baseUrl + CHAT_ENDPOINT;

        // Build the request through Montoya's HttpRequest builder
        HttpRequest request = HttpRequest.httpRequestFromUrl(fullUrl)
                .withMethod("POST")
                .withHeader("Content-Type", "application/json; charset=UTF-8")
                .withHeader("Accept", "application/json")
                .withBody(jsonPayload);

        if (apiKey != null && !apiKey.isBlank()) {
            request = request.withHeader("Authorization", "Bearer " + apiKey);
        }

        HttpResponse response = api.http().sendRequest(request).response();
        short status = response.statusCode();
        //String body  = response.bodyToString();
        String body = new String(response.body().getBytes(), StandardCharsets.UTF_8);

        if (status < 200 || status >= 300) {
            this.logging.logToError("Fail to send POST request to LLM model");
            this.logging.logToError("HTTP " + status + ": " + body);
        }
        return body;

    }

    private String parseContentFromResponse(String json) throws JSONException {
        return new JSONObject(json)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

    }

    @Override
    public LlmProvider getProvider() {
        return LlmProvider.OPENAI_COMPATIBLE;
    }

    private String loadPref(String key, String defaultValue) {
        String saved = this.api.persistence().preferences().getString(Prefs.PREFIX + key);
        return (saved != null && !saved.isBlank()) ? saved : defaultValue;
    }
}
