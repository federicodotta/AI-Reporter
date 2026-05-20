package org.fd;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Preferences;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class PromptDialog {

    private final Component parent;
    private final Preferences preferences;
    private final Logging logging;
    private final Prompt prompts;
    private final LinkedHashMap<String, JTextArea> textAreas = new LinkedHashMap<>();

    private boolean confirmed = false;

    public PromptDialog(Component parent, Prompt prompts, MontoyaApi api) {
        this.parent = parent;
        this.logging = api.logging();
        this.preferences = api.persistence().preferences();
        this.prompts = prompts;

        // Reload saved values (in case they changed since construction)
        for (String label : prompts.getPrompts().keySet()) {
            String saved = preferences.getString(Prompt.prefKey(label));
            if (saved != null && !saved.isBlank()) {
                prompts.setPrompt(label, saved);
            }
        }
    }

    public boolean show() {
        JTabbedPane tabbedPane = new JTabbedPane();

        for (Map.Entry<String, String> entry : prompts.getPrompts().entrySet()) {

            String label = entry.getKey();
            String value = entry.getValue();

            JTextArea textArea = new JTextArea(14, 55);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            textArea.setText(value);
            textAreas.put(label, textArea);

            JButton importBtn = new JButton("Import...");
            importBtn.addActionListener(e -> importPrompt(textArea));

            JButton exportBtn = new JButton("Export...");
            exportBtn.addActionListener(e -> exportPrompt(textArea, label));

            JButton restoreTabBtn = new JButton("Restore Default");
            restoreTabBtn.addActionListener(e ->
                    textArea.setText(prompts.getDefaultPrompt(label)));

            JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            buttonBar.add(importBtn);
            buttonBar.add(exportBtn);
            buttonBar.add(restoreTabBtn);

            JPanel tabPanel = new JPanel(new BorderLayout(5, 5));
            tabPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            tabPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            tabPanel.add(buttonBar, BorderLayout.SOUTH);

            // If this prompt has tags defined, show the tags panel above the textarea
            LinkedHashMap<String, String> tags = Prompt.PROMPT_TAGS.get(label);
            if (tags != null && !tags.isEmpty()) {
                tabPanel.add(buildTagsPanel(tags), BorderLayout.NORTH);
            }

            tabbedPane.addTab(label, tabPanel);
        }

        Window owner = SwingUtilities.getWindowAncestor(parent);
        if (owner == null) {
            owner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .getFocusedWindow();
        }

        JDialog dialog = new JDialog(owner, "Edit Prompts and templates", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setResizable(true);

        JButton okBtn     = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        okBtn.addActionListener(e -> { confirmed = true;  dialog.dispose(); });
        cancelBtn.addActionListener(e -> { confirmed = false; dialog.dispose(); });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);

        dialog.getContentPane().setLayout(new BorderLayout(5, 5));
        dialog.getContentPane().add(tabbedPane, BorderLayout.CENTER);
        dialog.getContentPane().add(btnPanel, BorderLayout.SOUTH);

        dialog.setMinimumSize(new Dimension(650, 420));
        dialog.setPreferredSize(new Dimension(700, 500));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true); // blocks until disposed

        if (confirmed) {
            saveAll();
        }
        return confirmed;
    }

    private JPanel buildTagsPanel(LinkedHashMap<String, String> tags) {
        JPanel tagsGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 6, 2, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        for (Map.Entry<String, String> tag : tags.entrySet()) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            JTextField tagField = new JTextField(tag.getKey());
            tagField.setEditable(false);
            tagField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
            tagField.setBorder(BorderFactory.createEmptyBorder());
            tagField.setBackground(null);
            tagField.setOpaque(false);
            tagsGrid.add(tagField, gbc);

            gbc.gridx = 1; gbc.weightx = 0;
            tagsGrid.add(new JLabel("—"), gbc);

            gbc.gridx = 2; gbc.weightx = 1;
            tagsGrid.add(new JLabel(tag.getValue()), gbc);

            row++;
        }

        // Scrollable area capped at ~3 rows; expands automatically if fewer tags
        int rowHeight = 26;
        int maxVisibleRows = 3;
        int preferredHeight = Math.min(tags.size(), maxVisibleRows) * rowHeight + 8;

        JScrollPane scroll = new JScrollPane(tagsGrid,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(0, preferredHeight));

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Available tags"));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void saveAll() {
        for (Map.Entry<String, JTextArea> entry : textAreas.entrySet()) {
            String value = entry.getValue().getText().trim();
            preferences.setString(Prompt.prefKey(entry.getKey()), value);
            prompts.setPrompt(entry.getKey(), value);
        }
    }

    private void importPrompt(JTextArea textArea) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Prompt");
        fc.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "md"));

        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                textArea.setText(Files.readString(fc.getSelectedFile().toPath()));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to read file: " + ex.getMessage(),
                        "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPrompt(JTextArea textArea, String label) {
        String safeName = label.toLowerCase().replace(" ", "_") + "_prompt.txt";

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Prompt");
        fc.setSelectedFile(new File(safeName));
        fc.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "md"));

        if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(fc.getSelectedFile().toPath(), textArea.getText());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to write file: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}