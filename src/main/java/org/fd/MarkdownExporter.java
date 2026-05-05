package org.fd;

import burp.api.montoya.persistence.PersistedObject;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


public class MarkdownExporter {

    private final PersistedObject persistence;
    private final Component parent;

    private String  exportDir;
    private boolean exportEnabled;
    private boolean configured; // true once the user has been asked at least once

    public MarkdownExporter(Component parent, PersistedObject persistence) {
        this.parent = parent;
        this.persistence = persistence;
        this.exportDir = persistence.getString(Prefs.KEY_REPORT_DIR);

        String enabledStr    = persistence.getString(Prefs.KEY_EXPORT_ENABLED);
        String configuredStr = persistence.getString(Prefs.KEY_EXPORT_CONFIGURED);

        this.configured = "true".equals(configuredStr);
        this.exportEnabled = "true".equals(enabledStr);
    }

    public boolean isExportEnabled() {
        return exportEnabled;
    }

    public void setExportEnabled(boolean enabled) {
        this.exportEnabled = enabled;
        persistence.setString(Prefs.KEY_EXPORT_ENABLED, String.valueOf(enabled));
    }

    public void ensureConfigured() {
        if (configured) return;

        int choice = JOptionPane.showConfirmDialog(parent,
                "Do you also want to save issues as Markdown files?\n\n"
                        + "  Yes — issues will be saved to Markdown files and to Burp.\n"
                        + "  No  — issues will be saved to Burp only.\n\n"
                        + "You can change this later from the AI Reporter → Change export options menu.",
                "Markdown Export",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        configured = true;
        persistence.setString(Prefs.KEY_EXPORT_CONFIGURED, "true");

        if (choice == JOptionPane.YES_OPTION) {
            setExportEnabled(true);
        } else {
            setExportEnabled(false);
        }
    }

    public void showExportOptionsDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0 — enable checkbox
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JCheckBox enabledCheckbox = new JCheckBox(
                "Save issues to Markdown files", exportEnabled);
        panel.add(enabledCheckbox, gbc);

        // Row 1 — export dir label
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Export directory:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        String dirDisplay = (exportDir != null && !exportDir.isBlank())
                ? exportDir : "(not set)";
        JLabel dirLabel = new JLabel(dirDisplay);
        dirLabel.setToolTipText(dirDisplay);
        panel.add(dirLabel, gbc);

        // Row 2 — browse button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select Export Directory");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            if (exportDir != null && !exportDir.isBlank()) {
                fc.setCurrentDirectory(new File(exportDir));
            }
            if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                String chosen = fc.getSelectedFile().getAbsolutePath();
                dirLabel.setText(chosen);
                dirLabel.setToolTipText(chosen);
            }
        });
        panel.add(browseBtn, gbc);

        // Toggle field visibility based on checkbox
        Runnable updateFields = () -> {
            boolean on = enabledCheckbox.isSelected();
            dirLabel.setEnabled(on);
            browseBtn.setEnabled(on);
        };
        enabledCheckbox.addActionListener(e -> updateFields.run());
        updateFields.run();

        int result = JOptionPane.showConfirmDialog(parent, panel,
                "Export Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            // Persist enable/disable
            setExportEnabled(enabledCheckbox.isSelected());

            // Mark as configured so ensureConfigured() won't ask again
            configured = true;
            persistence.setString(Prefs.KEY_EXPORT_CONFIGURED, "true");

            // Persist chosen directory (may have been updated via Browse)
            String newDir = dirLabel.getText();
            if (!"(not set)".equals(newDir)) {
                exportDir = newDir;
                persistence.setString(Prefs.KEY_REPORT_DIR, exportDir);
            }
        }
    }

    public Path export(String filename, String content) {
        if (exportDir == null || exportDir.isBlank()) {
            if (!askExportDir()) return null;
        }

        File dir = new File(exportDir);
        if (!dir.isDirectory()) {
            int choice = JOptionPane.showConfirmDialog(parent,
                    "The configured export directory no longer exists:\n"
                            + exportDir + "\n\nChoose a new one?",
                    "Directory Not Found", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION || !askExportDir()) return null;
            dir = new File(exportDir);
        }

        String safeName = filename
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_");

        Path target = resolveUniquePath(dir.toPath(), safeName, ".md");

        try {
            Files.writeString(target, content);
            return target;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to write file: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private boolean askExportDir() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Export Directory");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        if (exportDir != null && !exportDir.isBlank()) {
            fc.setCurrentDirectory(new File(exportDir));
        }

        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        exportDir = fc.getSelectedFile().getAbsolutePath();
        persistence.setString(Prefs.KEY_REPORT_DIR, exportDir);
        return true;
    }

    private Path resolveUniquePath(Path dir, String baseName, String extension) {
        Path candidate = dir.resolve(baseName + extension);
        if (!Files.exists(candidate)) return candidate;

        int counter = 1;
        do {
            candidate = dir.resolve(baseName + "(" + counter + ")" + extension);
            counter++;
        } while (Files.exists(candidate));

        return candidate;
    }
}
