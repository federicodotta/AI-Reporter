package org.fd;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.chat.PromptException;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import org.json.JSONArray;
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


    private static final String CHAT_ENDPOINT   = "/v1/chat/completions";

    public OllamaClient(MontoyaApi api, Prompt prompts, boolean debug) {
        //this.systemMessage = systemMessage(systemPrompt);
        this.api = api;
        this.prompts = prompts;
        this.history = null;
        this.logging = api.logging();

        this.baseUrl = loadPref("BASE_URL", Prefs.DEFAULT_URL);
        this.model = loadPref("MODEL", Prefs.DEFAULT_MODEL);
        this.apiKey = loadPref("API_KEY", "");
        this.temperature = Double.valueOf(loadPref("TEMPERATURE", Prefs.DEFAULT_TEMPERATURE));
        this.htmlEncodeIssues = loadPref("TEMPERATURE", Prefs.DEFAULT_HTML_ENCODE).startsWith("YES");

        this.debug = debug;

    }

    @Override
    public String oneShot(String userPrompt) throws PromptException {

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

        return parseContentFromResponse(responseBody);
    }

    @Override
    public String chat(String userPrompt) throws PromptException {

        if(this.history == null) {
            this.history = new ArrayList<String[]>();
            this.history.add(new String[]{"system",this.prompts.getPrompt("chatPrompt")});
        }

        history.add(new String[]{"user", userPrompt});

        String payload = buildChatPayload();

        String responseBody = doPost(payload);
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

        history.add(new String[]{"assistant", reply});
        return reply;
    }

    @Override
    public boolean isAiEnabled() {
        // Always return true with Ollama/OpenAI compatible (with all non Burp AI LLM providers)
        return true;
    }

    @Override
    public void clearHistory() {
        this.history = null;
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

        synchronized (history) {
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

    private String parseContentFromResponse(String json) {

        return new JSONObject(json)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

    }

    private String loadPref(String key, String defaultValue) {
        String saved = this.api.persistence().preferences().getString(Prefs.PREFIX + key);
        return (saved != null && !saved.isBlank()) ? saved : defaultValue;
    }
}
