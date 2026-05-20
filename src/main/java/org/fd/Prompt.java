package org.fd;

import burp.api.montoya.persistence.Preferences;

import java.util.LinkedHashMap;
import java.util.Map;

public class Prompt {

    private final Preferences preferences;

    private final LinkedHashMap<String, String> prompts;
    private final LinkedHashMap<String, String> defaultPrompts;

    // Tags available for substitution, keyed by prompt name
    public static final LinkedHashMap<String, LinkedHashMap<String, String>> PROMPT_TAGS = new LinkedHashMap<>();
    static {

        LinkedHashMap<String, String> markdownTags = new LinkedHashMap<>();
        markdownTags.put("{{aireporter_title}}", "Issue title");
        markdownTags.put("{{aireporter_severity}}", "Issue severity");
        markdownTags.put("{{aireporter_confidence}}", "Issue confidence");
        markdownTags.put("{{aireporter_details}}", "Issue details");
        markdownTags.put("{{aireporter_remediation}}", "Issue remediation");
        markdownTags.put("{{aireporter_request}}", "HTTP request used for issue generation");
        markdownTags.put("{{aireporter_response}}", "HTTP response used for issue generation");
        markdownTags.put("{{aireporter_request_first_XX}}", "HTTP request first XX bytes");
        markdownTags.put("{{aireporter_response_first_XX}}", "HTTP response first XX bytes");
        markdownTags.put("{{aireporter_request_last_XX}}", "HTTP request last XX bytes");
        markdownTags.put("{{aireporter_response_last_XX}}", "HTTP response last XX bytes");
        markdownTags.put("{{aireporter_request_body}}", "HTTP request body");
        markdownTags.put("{{aireporter_response_body}}", "HTTP response body");
        markdownTags.put("{{aireporter_request_headers}}", "HTTP request headers");
        markdownTags.put("{{aireporter_response_headers}}", "HTTP response headers");
        markdownTags.put("{{aireporter_request_url}}", "HTTP request URL");

        PROMPT_TAGS.put("markdownTemplate", markdownTags);

        LinkedHashMap<String, String> userMessageTags = new LinkedHashMap<>();
        userMessageTags.put("{{aireporter_issue_name}}", "Issue name");
        userMessageTags.put("{{aireporter_additional_details}}", "Issue additional details");
        userMessageTags.put("{{aireporter_request}}", "HTTP request");
        userMessageTags.put("{{aireporter_response}}", "HTTP response");
        userMessageTags.put("{{aireporter_request_first_XX}}", "HTTP request first XX bytes");
        userMessageTags.put("{{aireporter_response_first_XX}}", "HTTP response first XX bytes");
        userMessageTags.put("{{aireporter_request_last_XX}}", "HTTP request last XX bytes");
        userMessageTags.put("{{aireporter_response_last_XX}}", "HTTP response last XX bytes");
        userMessageTags.put("{{aireporter_request_body}}", "HTTP request body");
        userMessageTags.put("{{aireporter_response_body}}", "HTTP response body");
        userMessageTags.put("{{aireporter_request_headers}}", "HTTP request headers");
        userMessageTags.put("{{aireporter_response_headers}}", "HTTP response headers");
        userMessageTags.put("{{aireporter_request_url}}", "HTTP request URL");

        PROMPT_TAGS.put("userMessageTemplate", userMessageTags);
    }

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
            - Respond ONLY with a valid JSON object, no additional text before or after it. The JSON should contain a SINGLE vulnerability in JSON object.
            
            Output format (strict JSON, with ONLY these fields, without additional or missing ones):
            {
              "title": "Specific descriptive title of the finding",
              "details": "Detailed description including evidence, location, and impact",
              "remediation": "Specific and actionable remediation steps"
            }
            """;

    private static final String DEFAULT_USER_MESSAGE_TEMPLATE = """    
            Vulnerability name: {{aireporter_issue_name}}
        
            HTTP Request:
                    {{aireporter_request}}
        
            HTTP Response:
                    {{aireporter_response}}
        
            Additional details: {{aireporter_additional_details}}
            """;

    private static final String DEFAULT_MARKDOWN_TEMPLATE = """
            # Title
            {{aireporter_title}}
            
            # Severity
            {{aireporter_severity}}
            
            # Confidence
            {{aireporter_confidence}}
            
            # Issue detail
            {{aireporter_details}}
            
            # Remediation detail
            {{aireporter_remediation}}
            
            # Request/Response
            
            **Request**
            ```http
            {{aireporter_request}}
            ```
            
            **Response**
            ```http
            {{aireporter_response}}
            ```
            """;

    public Prompt(Preferences preferences) {
        this.preferences = preferences;

        this.defaultPrompts = new LinkedHashMap<>();
        defaultPrompts.put("chatPrompt", DEFAULT_CHAT_PROMPT);
        defaultPrompts.put("reportingPrompt", DEFAULT_REPORTING_PROMPT);
        defaultPrompts.put("userMessageTemplate", DEFAULT_USER_MESSAGE_TEMPLATE);
        defaultPrompts.put("markdownTemplate", DEFAULT_MARKDOWN_TEMPLATE);

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
