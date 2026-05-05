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

            //this.logging.logToOutput("SHOW");
            //this.logging.logToOutput(label + " - " + value);

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

            tabbedPane.addTab(label, tabPanel);
        }

        tabbedPane.setPreferredSize(new Dimension(650, 350));

        Window owner = SwingUtilities.getWindowAncestor(parent);
        if (owner == null) {
            owner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .getFocusedWindow();
        }

        int result = JOptionPane.showConfirmDialog(
                parent, tabbedPane,
                "Edit Prompts",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        confirmed = (result == JOptionPane.OK_OPTION);
        if (confirmed) {
            saveAll();
        }
        return confirmed;
    }

    private void saveAll() {
        for (Map.Entry<String, JTextArea> entry : textAreas.entrySet()) {
            String value = entry.getValue().getText().trim();

            //this.logging.logToOutput("SAVEALL");
            //this.logging.logToOutput(Prompt.prefKey(entry.getKey()) + " - " + entry.getKey() + " - " + value);

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
