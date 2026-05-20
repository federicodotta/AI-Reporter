package org.fd;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.chat.PromptException;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.utilities.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AiReporterContextProvider implements ContextMenuItemsProvider {

    MontoyaApi api;
    Logging logging;
    JsonUtils jsonUtils;
    LlmClient llmClient;
    boolean debug;
    MarkdownExporter exporter;
    ExecutorService executor;
    Prompt prompts;

    public AiReporterContextProvider(MontoyaApi api, LlmClient llmClient, boolean debug, MarkdownExporter exporter,
                                     ExecutorService executor, Prompt prompts) {

        // Save a reference to the MontoyaApi object
        this.api = api;
        // Save a reference to the logging object of the MontoyaApi
        this.logging = api.logging();
        // Save a reference to JSON utilities
        this.jsonUtils = api.utilities().jsonUtils();
        // Save a reference to the object defined to handle AI
        //this.aiEngine = aiEngine;
        this.llmClient = llmClient;
        // Debug variable
        this.debug = debug;
        // Markdown exporter
        this.exporter = exporter;
        // Executor
        this.executor = executor;
        // Prompts and templates
        this.prompts = prompts;

    }

    // Burp AI often returns markdown code block tags in the response also if denied in the prompt.
    // This method strip markdown code block tags in the response using Lambda expressions.
    private String cleanJsonResponse(String response) {
        return response.lines()
                .filter(line -> !line.trim().startsWith("```"))
                .collect(Collectors.joining("\n"))
                .trim();
    }

    // Call the AI Engine and report the issue
    public void reportWithAi(String vulnerability, AuditIssueSeverity severity, AuditIssueConfidence confidence,
                             String additionalDetails, HttpRequestResponse reqRes) {

        // If AI features are enabled
        if(this.llmClient.isAiEnabled()) {

            String userMessage = prompts.getPrompt("userMessageTemplate")
                    .replace("{{aireporter_issue_name}}", vulnerability)
                    .replace("{{aireporter_additional_details}}", additionalDetails)
                    .replace("{{aireporter_request}}", reqRes.request().toString())
                    .replace("{{aireporter_response}}", reqRes.response().toString())
                    .replace("{{aireporter_request_body}}", reqRes.request().bodyToString())
                    .replace("{{aireporter_response_body}}", reqRes.response().bodyToString())
                    .replace("{{aireporter_request_headers}}",
                            reqRes.request().toByteArray().subArray(0,reqRes.request().bodyOffset()).toString())
                    .replace("{{aireporter_response_headers}}",
                            reqRes.response().toByteArray().subArray(0,reqRes.response().bodyOffset()).toString())
                    .replace("{{aireporter_request_url}}", reqRes.request().url());

            // {{aireporter_request_first_XX}}
            userMessage = Utils.replaceFirstChars("aireporter_request", userMessage,
                    reqRes.request().toByteArray(), debug, logging);
            // {{aireporter_response_last_XX}}
            userMessage = Utils.replaceLastChars("aireporter_request", userMessage,
                    reqRes.request().toByteArray(), debug, logging);
            // {{aireporter_response_first_XX}}
            userMessage = Utils.replaceFirstChars("aireporter_response", userMessage,
                    reqRes.response().toByteArray(), debug, logging);
            // {{aireporter_response_last_XX}}
            userMessage = Utils.replaceLastChars("aireporter_response", userMessage,
                    reqRes.response().toByteArray(), debug, logging);

            String title;
            String details;
            String remediation;

            try  {
                // Call the LLM with the user message
                String promptResponse = this.llmClient.oneShot(userMessage);

                if(promptResponse != null) {

                    // Debug block
                    if (this.debug) {
                        this.logging.logToOutput("* OUTPUT LLM");
                        this.logging.logToOutput(promptResponse);
                        this.logging.logToOutput("");
                    }

                    // Remove markdown code block tags in LLM response
                    String cleanedResponse = cleanJsonResponse(promptResponse);

                    // LLM response is formatted in JSON, as requested in our prompt
                    if (this.jsonUtils.isValidJson(cleanedResponse)) {

                        // Extract from JSON the details of the issue generated by the LLM
                        title = this.jsonUtils.readString(cleanedResponse, "title");
                        details = this.jsonUtils.readString(cleanedResponse, "details");
                        remediation = this.jsonUtils.readString(cleanedResponse, "remediation");

                        // Debug block
                        if (debug) {
                            this.logging.logToOutput("* New issue");
                            this.logging.logToOutput(title);
                            this.logging.logToOutput(details);
                            this.logging.logToOutput(remediation);
                            this.logging.logToOutput("");
                        }

                    } else {
                        this.logging.logToError("* AI Reporter: invalid JSON in AI response");
                        return;
                    }

                } else {
                    this.logging.logToError("* AI Reporter: AI not enabled");
                    return;
                }

            } catch (PromptException e)  {
                this.logging.logToError("Issue executing prompt", e);
                return;
            }

            // Burp Suite issues interprete some HTML tags. This way it is possible to choose if we
            // want to apply HTML encoding
            String encodedDetails = details;
            String encodedRemediation = remediation;
            if(this.llmClient.htmlEncodeIssues()) {
                encodedDetails = this.api.utilities().htmlUtils().encode(details);
                encodedRemediation = this.api.utilities().htmlUtils().encode(remediation);
            }

            if(this.api.burpSuite().version().edition() != BurpSuiteEdition.COMMUNITY_EDITION) {

                // Report issue in Burp Suite with AI details
                AuditIssue auditIssue = AuditIssue.auditIssue(title,
                        encodedDetails,
                        encodedRemediation,
                        reqRes.request().url(),
                        severity,
                        confidence,
                        null, // background
                        null, // remediationBackground
                        severity,
                        reqRes);

                this.api.siteMap().add(auditIssue);

                this.logging.logToOutput("* Issue reported in Burp suite!");

            }

            // Export the issue in Markdown (if requested)
            this.exporter.ensureConfigured();

            if (this.exporter.isExportEnabled()) {

                String markdownIssue = prompts.getPrompt("markdownTemplate")
                        .replace("{{aireporter_title}}", vulnerability)
                        .replace("{{aireporter_severity}}", severity.toString())
                        .replace("{{aireporter_confidence}}", confidence.toString())
                        .replace("{{aireporter_details}}", details)
                        .replace("{{aireporter_remediation}}", remediation)
                        .replace("{{aireporter_request}}", reqRes.request().toString())
                        .replace("{{aireporter_response}}", reqRes.response().toString())
                        .replace("{{aireporter_request_body}}", reqRes.request().bodyToString())
                        .replace("{{aireporter_response_body}}", reqRes.response().bodyToString())
                        .replace("{{aireporter_request_headers}}",
                                reqRes.request().toByteArray().subArray(0,reqRes.request().bodyOffset()).toString())
                        .replace("{{aireporter_response_headers}}",
                                reqRes.response().toByteArray().subArray(0,reqRes.response().bodyOffset()).toString())
                        .replace("{{aireporter_request_url}}", reqRes.request().url());

                // {{aireporter_request_first_XX}}
                markdownIssue = Utils.replaceFirstChars("aireporter_request", markdownIssue,
                        reqRes.request().toByteArray(), debug, logging);
                // {{aireporter_response_first_XX}}
                markdownIssue = Utils.replaceLastChars("aireporter_request", markdownIssue,
                        reqRes.request().toByteArray(), debug, logging);
                // {{aireporter_response_first_XX}}
                markdownIssue = Utils.replaceFirstChars("aireporter_response", markdownIssue,
                        reqRes.response().toByteArray(), debug, logging);
                // {{aireporter_response_last_XX}}
                markdownIssue = Utils.replaceLastChars("aireporter_response", markdownIssue,
                        reqRes.response().toByteArray(), debug, logging);

                exporter.export(vulnerability, markdownIssue);

                this.logging.logToOutput("* Issue exported to markdown file!");

            }

        } else {
            this.logging.logToError("* AI Reporter: AI features disabled.");
        }

    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {

        // Initialize an empty list that will contains our context menu entries
        List<Component> menuItems = new ArrayList<Component>();

        // Create the menu only if the menu has been created on a request/response object
        event.messageEditorRequestResponse().ifPresent(messageEditorReqRes -> {

            // Get the HTTP message
            HttpRequestResponse reqRes = messageEditorReqRes.requestResponse();

            // Add the "Report with AI" context menu item with its listener
            JMenuItem reportWithAiItem = new JMenuItem("Report with AI");
            reportWithAiItem.addActionListener(al -> SwingUtilities.invokeLater(() -> {

                // Show only if AI features are enabled
                if(this.llmClient.isAiEnabled()) {

                    AiReporterDialog dialog = AiReporterDialog.show(api.userInterface().swingUtils().suiteFrame());

                    // If the dialog is confirmed call the reportWithAi function that will call the LLM and report the issue
                    if (dialog.isConfirmed()) {
                        String vulnerability = dialog.getVulnerability();
                        AuditIssueSeverity severity = dialog.getSeverity();
                        AuditIssueConfidence confidence = dialog.getConfidence();
                        String additionalDetails = dialog.getAdditionalDetails();

                        executor.submit(() -> {
                            try {
                                reportWithAi(vulnerability, severity, confidence, additionalDetails, reqRes);
                            } catch (Exception ex) {
                                logging.logToError("[AiReporter] Reporting failed: " + ex.getMessage());
                            }
                        });
                    }

                } else {

                    JOptionPane.showMessageDialog(api.userInterface().swingUtils().suiteFrame(),
                            "Please enable Burp AI features to " +
                                    "use this extension (additional costs may be charged)",
                            "Burp AI disabled", JOptionPane.INFORMATION_MESSAGE);
                }

            }));

            menuItems.add(reportWithAiItem);

        });

        return menuItems;

    }

    // Called when the user switches LLM provider at runtime.
    public void setClient(LlmClient newClient) {
        this.llmClient = newClient;
    }

}
