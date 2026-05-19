package org.fd;

public enum LlmProvider {

    BURP_AI("Burp AI"),
    OPENAI_COMPATIBLE("Ollama / OpenAI");

    private final String displayName;

    LlmProvider(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}