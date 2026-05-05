package org.fd;

import burp.api.montoya.ai.chat.PromptException;

public interface LlmClient {

    public String oneShot(String userPrompt) throws PromptException;
    public String chat(String userPrompt) throws PromptException;
    public boolean isAiEnabled();

    void clearHistory();

    String getBaseUrl();
    void   setBaseUrl(String url);

    String getModel();
    void   setModel(String model);

    double getTemperature();
    void   setTemperature(double temperature);

    String getApiKey();
    void   setApiKey(String apiKey);

    boolean htmlEncodeIssues();
    void setHtmlEncodeIssue(boolean htmlEncodeIssues);

}
