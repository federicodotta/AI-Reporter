package org.fd;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Preferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class AiReporterUI {

    private LlmClient client;
    private final ExecutorService executor;
    private final Logging logging;
    private final Preferences preferences;

    private Consumer<LlmProvider> onProviderChanged;

    private JPanel rootPanel;
    private JComboBox<LlmProvider> providerCombo;
    private JTextField urlField;
    private JTextField modelField;
    private JTextField temperatureField;
    private JPasswordField apiKeyField;
    private JLabel urlLabel;
    private JLabel apiKeyLabel;
    private JLabel temperatureLabel;
    private JTextArea promptArea;
    private JTextArea responseArea;
    private JButton sendButton;
    private JLabel statusLabel;
    private JComboBox<String> htmlEncodeCombo;

    public AiReporterUI(LlmClient client, ExecutorService executor,
                        Logging logging, Preferences preferences) {
        this.client      = client;
        this.executor    = executor;
        this.logging     = logging;
        this.preferences = preferences;
    }

    public Component getUI() {
        if (rootPanel == null) {
            rootPanel = buildUI();
        }
        return rootPanel;
    }

    // Replaces the active client (es. after a provider switch)
    public void setClient(LlmClient newClient) {
        this.client = newClient;
    }

    public void setOnProviderChanged(Consumer<LlmProvider> callback) {
        this.onProviderChanged = callback;
    }

    private JPanel buildUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(buildConfigPanel(), BorderLayout.NORTH);
        panel.add(buildResponsePanel(), BorderLayout.CENTER);
        panel.add(buildPromptPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildConfigPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 — LLM Provider
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        configPanel.add(new JLabel("LLM Provider:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        providerCombo = new JComboBox<>(LlmProvider.values());
        String savedProvider = preferences.getString(Prefs.KEY_PROVIDER);
        if (savedProvider != null) {
            try {
                providerCombo.setSelectedItem(LlmProvider.valueOf(savedProvider));
            } catch (IllegalArgumentException ignored) { /* keep default */ }
        }
        providerCombo.addActionListener(e -> onProviderSwitch());
        configPanel.add(providerCombo, gbc);

        // Row 1 — Endpoint URL
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        urlLabel = new JLabel("Endpoint URL:");
        configPanel.add(urlLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        urlField = new JTextField(client.getBaseUrl(), 40);
        configPanel.add(urlField, gbc);

        // Row 2 — Model
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        configPanel.add(new JLabel("Model:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        modelField = new JTextField(client.getModel(), 40);
        configPanel.add(modelField, gbc);

        // Row 3 — API Key
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        apiKeyLabel = new JLabel("API Key:");
        configPanel.add(apiKeyLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        apiKeyField = new JPasswordField(client.getApiKey(), 40);
        configPanel.add(apiKeyField, gbc);

        // Row 4 — Temperature
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        temperatureLabel = new JLabel("Temperature:");
        configPanel.add(temperatureLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        temperatureField = new JTextField(String.valueOf(client.getTemperature()), 40);
        configPanel.add(temperatureField, gbc);

        // Row 5 — HTML encoding
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        configPanel.add(new JLabel("HTML encoding:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        htmlEncodeCombo = new JComboBox<>(new String[]{"NO (DO NOT apply HTML encoding to issue details and remediation)","YES (apply HTML encoding to issue details and remediation)"});
        String savedEncode = preferences.getString(Prefs.KEY_HTML_ENCODE);
        htmlEncodeCombo.setSelectedItem(savedEncode.startsWith("NO") ? "NO (DO NOT apply HTML encoding to issue details and remediation)" : "YES (apply HTML encoding to issue details and remediation)");
        configPanel.add(htmlEncodeCombo, gbc);

        // Apply button (spans all config rows)
        JButton applyBtn = new JButton("Apply");
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 4; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        applyBtn.addActionListener(e -> applyConfig());
        configPanel.add(applyBtn, gbc);

        // Set initial visibility based on selected provider
        updateFieldVisibility();

        return configPanel;
    }

    private JScrollPane buildResponsePanel() {
        responseArea = new JTextArea();
        responseArea.setEditable(false);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(responseArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Response"));
        return scroll;
    }

    private JPanel buildPromptPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Prompt"));

        promptArea = new JTextArea(4, 60);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        bottomPanel.add(new JScrollPane(promptArea), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusLabel = new JLabel("Ready");
        JButton clearBtn = new JButton("Clear History");
        clearBtn.addActionListener(e -> clearConversation());
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::onSend);

        controls.add(statusLabel);
        controls.add(clearBtn);
        controls.add(sendButton);
        bottomPanel.add(controls, BorderLayout.SOUTH);

        return bottomPanel;
    }

    private void onProviderSwitch() {
        LlmProvider selected = (LlmProvider) providerCombo.getSelectedItem();
        if (selected == null) return;

        updateFieldVisibility();

        if (onProviderChanged != null) {
            onProviderChanged.accept(selected);
        }
    }

    private void updateFieldVisibility() {

        boolean isOpenAi = providerCombo.getSelectedItem() == LlmProvider.OPENAI_COMPATIBLE;
        urlField.setEnabled(isOpenAi);
        urlLabel.setEnabled(isOpenAi);
        modelField.setEnabled(isOpenAi);
        apiKeyField.setEnabled(isOpenAi);
        apiKeyLabel.setEnabled(isOpenAi);
    }

    private void applyConfig() {
        String url   = urlField.getText().trim();
        String model = modelField.getText().trim();
        String key   = new String(apiKeyField.getPassword()).trim();
        String temperature = temperatureField.getText().trim();
        String htmlEncode = (String) htmlEncodeCombo.getSelectedItem();

        client.setBaseUrl(url);
        client.setModel(model);
        client.setApiKey(key);
        client.setTemperature(Double.valueOf(temperature));
        client.setHtmlEncodeIssue(htmlEncode.startsWith("YES"));

        LlmProvider provider = (LlmProvider) providerCombo.getSelectedItem();
        preferences.setString(Prefs.KEY_PROVIDER, provider != null ? provider.name() : "");
        preferences.setString(Prefs.KEY_BASE_URL, url);
        preferences.setString(Prefs.KEY_MODEL, model);
        preferences.setString(Prefs.KEY_API_KEY, key);
        preferences.setString(Prefs.KEY_TEMPERATURE, temperature);
        preferences.setString(Prefs.KEY_HTML_ENCODE, htmlEncode);

        logging.logToOutput("[AiReporterUI] Config updated -> Provider: " + provider
                + " | URL: " + client.getBaseUrl()
                + " | Model: " + client.getModel()
                + " | Temperature: " + client.getTemperature()
                + " | HTML Encode: " + ( client.htmlEncodeIssues() ? "YES" : "NO"));
        setStatus("Config applied");
    }

    private void onSend(ActionEvent e) {
        String userPrompt = promptArea.getText().trim();
        if (userPrompt.isEmpty()) return;

        promptArea.setText("");
        sendButton.setEnabled(false);
        setStatus("Sending...");

        appendToResponse(">> USER:\n" + userPrompt + "\n\n");

        executor.submit(() -> {
            try {
                String reply = client.chat(userPrompt);
                SwingUtilities.invokeLater(() -> {
                    appendToResponse("<< ASSISTANT:\n" + reply + "\n\n---\n\n");
                    setStatus("Ready");
                    sendButton.setEnabled(true);
                });
            } catch (Exception ex) {
                logging.logToError("[AiReporterUI] Request failed: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    appendToResponse("<< ERROR: " + ex.getMessage() + "\n\n---\n\n");
                    setStatus("Error");
                    sendButton.setEnabled(true);
                });
            }
        });
    }

    private void clearConversation() {
        client.clearHistory();
        responseArea.setText("");
        setStatus("History cleared");
    }

    private void appendToResponse(String text) {
        responseArea.append(text);
        responseArea.setCaretPosition(responseArea.getDocument().getLength());
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}
