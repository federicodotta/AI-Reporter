package org.fd;

public enum LlmProvider {

    OPENAI_COMPATIBLE("Ollama / OpenAI"),
    BURP_AI("Burp AI");

    private final String displayName;

    LlmProvider(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}