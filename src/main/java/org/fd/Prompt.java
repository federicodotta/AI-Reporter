package org.fd;

import burp.api.montoya.persistence.Preferences;

import java.util.LinkedHashMap;
import java.util.Map;

public class Prompt {

    private final Preferences preferences;

    private final LinkedHashMap<String, String> prompts;
    private final LinkedHashMap<String, String> defaultPrompts;

    private static final String DEFAULT_CHAT_PROMPT = """
            You are a helpful cybersecurity assistant specialized in \
            penetration testing and web application security.
            """;

    private static final String DEFAULT_REPORTING_PROMPT  = """
            You are an expert penetration tester and application security analyst. Your task is to analyze an HTTP request/response pair in which a specific vulnerability has been identified.
            
            You will receive:
            - The vulnerability name (issue type)
            - The HTTP request
            - The HTTP response
            - Optionally, additional details provided by the analyst
            
            Your objectives:
            1. **Analyze** the request and response to locate concrete evidence of the reported vulnerability.
            2. **Generate a title** that is specific and descriptive for this particular instance of the vulnerability (do not just repeat the generic vulnerability name — include context such as the affected parameter, endpoint, or functionality).
            3. **Generate a detailed description** of the finding that includes:
               - What the vulnerability is and why it is a security concern
               - Where exactly in the request/response the vulnerability manifests (cite specific parameters, headers, response content, or behavior)
               - The potential impact if exploited by an attacker
            4. **Generate remediation advice** that is specific and actionable for this particular case, not just generic best practices.
            
            Rules:
            - Be precise: reference actual values, parameters, endpoints, and response content from the provided data.
            - If additional details are provided by the analyst, incorporate them into your analysis.
            - If you cannot find clear evidence of the vulnerability in the request/response, state this explicitly in the details field.
            - Write in a professional tone suitable for a penetration testing report.
            - Respond ONLY with a valid JSON object, no additional text before or after it.
            
            Output format (strict JSON):
            {
              "title": "Specific descriptive title of the finding",
              "details": "Detailed description including evidence, location, and impact",
              "remediation": "Specific and actionable remediation steps"
            }
            """;

    public Prompt(Preferences preferences) {
        this.preferences = preferences;

        this.defaultPrompts = new LinkedHashMap<>();
        defaultPrompts.put("chatPrompt", DEFAULT_CHAT_PROMPT);
        defaultPrompts.put("reportingPrompt", DEFAULT_REPORTING_PROMPT);

        this.prompts = new LinkedHashMap<>(defaultPrompts);

        // Override defaults with any previously saved values
        for (String label : prompts.keySet()) {
            String saved = preferences.getString(prefKey(label));
            if (saved != null && !saved.isBlank()) {
                prompts.put(label, saved);
            }
        }
    }

    public LinkedHashMap<String, String> getPrompts()       {
        return prompts;
    }
    public String getPrompt(String key)                     {
        return prompts.get(key);
    }
    public void   setPrompt(String key, String value)       {
        prompts.put(key, value);
    }

    public String getDefaultPrompt(String key)              {
        return defaultPrompts.get(key);
    }

    public void resetToDefaults() {
        for (Map.Entry<String, String> entry : defaultPrompts.entrySet()) {
            prompts.put(entry.getKey(), entry.getValue());
            preferences.setString(prefKey(entry.getKey()), entry.getValue());
        }
    }

    static String prefKey(String label) {
        return Prefs.PREFIX + label;
    }
}
