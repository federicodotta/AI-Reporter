package org.fd;

import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;

import javax.swing.*;
import java.awt.*;

public class AiReporterDialog {

    private final String vulnerability;
    private final AuditIssueSeverity severity;
    private final AuditIssueConfidence confidence;
    private final String additionalDetails;
    private final boolean confirmed;

    private AiReporterDialog(String vulnerability, AuditIssueSeverity severity, AuditIssueConfidence confidence, String additionalDetails, boolean confirmed) {
        this.vulnerability = vulnerability;
        this.severity = severity;
        this.confidence = confidence;
        this.additionalDetails = additionalDetails;
        this.confirmed = confirmed;
    }

    public static AiReporterDialog show(Component parent) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // First line
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Vulnerability:"), gbc);

        JTextField vulnerabilityField = new JTextField(30);
        gbc.gridx = 1;
        panel.add(vulnerabilityField, gbc);

        // Second line
        gbc.gridx = 0;
        gbc.gridy = 1;

        panel.add(new JLabel("Severity:"), gbc);

        JComboBox<AuditIssueSeverity> severityCombo = new JComboBox<>(new AuditIssueSeverity[]{
                AuditIssueSeverity.HIGH,
                AuditIssueSeverity.MEDIUM,
                AuditIssueSeverity.LOW,
                AuditIssueSeverity.INFORMATION
        });
        gbc.gridx = 1;
        panel.add(severityCombo, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Confidence:"), gbc);

        JComboBox<AuditIssueConfidence> confidenceCombo = new JComboBox<>(new AuditIssueConfidence[]{
                AuditIssueConfidence.CERTAIN,
                AuditIssueConfidence.FIRM,
                AuditIssueConfidence.TENTATIVE
        });
        gbc.gridx = 3;
        panel.add(confidenceCombo, gbc);

        // Third line
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Additional details:"), gbc);

        JTextArea detailsArea = new JTextArea(8, 30);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        gbc.gridx = 1;
        panel.add(scrollPane, gbc);

        // Show dialog
        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Report issue with AI",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        boolean confirmed = (result == JOptionPane.OK_OPTION);

        return new AiReporterDialog(
                vulnerabilityField.getText().trim(),
                (AuditIssueSeverity) severityCombo.getSelectedItem(),
                (AuditIssueConfidence) confidenceCombo.getSelectedItem(),
                detailsArea.getText().trim(),
                confirmed
        );
    }

    // Getters
    public String getVulnerability() {
        return vulnerability;
    }

    public AuditIssueSeverity getSeverity() {
        return severity;
    }

    public AuditIssueConfidence getConfidence() {
        return confidence;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public boolean isConfirmed() {
        return confirmed;
    }


}