package com.promptmanager.ui;

import com.promptmanager.util.AppSettings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Settings dialog.
 * AI and OCR fields are present and persisted but not yet wired to real logic.
 */
public class SettingsDialog extends JDialog {

    private final AppSettings settings = AppSettings.getInstance();

    // AI
    private final JComboBox<String> providerBox  = new JComboBox<>(new String[]{"OpenAI", "Anthropic", "Ollama"});
    private final JTextField baseUrlField        = new JTextField(40);
    private final JPasswordField apiKeyField     = new JPasswordField(40);
    private final JTextField modelField          = new JTextField(40);

    // OCR
    private final JTextField tessDataField       = new JTextField(40);

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        initUI();
        loadFromSettings();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));

        // ---- AI section ----
        JPanel aiPanel = new JPanel(new GridBagLayout());
        aiPanel.setBorder(new TitledBorder("AI Provider (not yet active)"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        addRow(aiPanel, gc, 0, "Provider:", providerBox);
        addRow(aiPanel, gc, 1, "Base URL:", baseUrlField);
        addRow(aiPanel, gc, 2, "API Key:", apiKeyField);
        addRow(aiPanel, gc, 3, "Model:", modelField);

        JLabel aiNote = new JLabel("  ⚠ AI integration is not yet implemented. Settings are saved for future use.");
        aiNote.setForeground(new Color(160, 100, 0));
        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2;
        aiPanel.add(aiNote, gc);

        // ---- OCR section ----
        JPanel ocrPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc2 = new GridBagConstraints();
        gc2.insets = new Insets(4, 4, 4, 4);
        gc2.anchor = GridBagConstraints.WEST;

        addRow(ocrPanel, gc2, 0, "Tesseract data path:", tessDataField);
        JButton browseBtn = new JButton("Browse…");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                tessDataField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gc2.gridx = 2; gc2.gridy = 0;
        ocrPanel.add(browseBtn, gc2);

        centre.add(aiPanel);
        centre.add(Box.createVerticalStrut(8));
        centre.add(ocrPanel);

        // ---- Buttons ----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn   = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        buttons.add(saveBtn);
        buttons.add(cancelBtn);

        saveBtn.addActionListener(e -> { saveToSettings(); dispose(); });
        cancelBtn.addActionListener(e -> dispose());

        add(centre, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        panel.add(field, gc);
        gc.weightx = 0;
    }

    private void loadFromSettings() {
        providerBox.setSelectedItem(settings.getAiProvider());
        baseUrlField.setText(settings.getAiBaseUrl());
        apiKeyField.setText(settings.getAiApiKey());
        modelField.setText(settings.getAiModel());
        tessDataField.setText(settings.getTesseractDataPath());
    }

    private void saveToSettings() {
        settings.setAiProvider((String) providerBox.getSelectedItem());
        settings.setAiBaseUrl(baseUrlField.getText().trim());
        settings.setAiApiKey(new String(apiKeyField.getPassword()));
        settings.setAiModel(modelField.getText().trim());
        settings.setTesseractDataPath(tessDataField.getText().trim());
        settings.save();
    }
}
