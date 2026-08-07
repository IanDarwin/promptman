package com.promptmanager.ui;

import com.promptmanager.ai.AiProvider;
import com.promptmanager.util.AppSettings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings dialog.
 *
 * The AI section shows one panel per discovered provider, swapped in via a
 * JComboBox.  Each provider panel contains whichever fields are relevant to
 * that provider (base URL, API key, model).  Fields are loaded from and saved
 * to AppSettings using the namespaced ai.<providerId>.<field> scheme.
 */
public class SettingsDialog extends JDialog {

    private final AppSettings settings = AppSettings.getInstance();

    // ---- AI: provider selector ----
    private final JComboBox<String>         providerCombo  = new JComboBox<>();
    private final JPanel                    providerCards  = new JPanel(new CardLayout());
    /** Maps provider id → the fields shown for that provider. */
    private final Map<String, ProviderPanel> providerPanels = new LinkedHashMap<>();

    // ---- OCR ----
    private final JTextField tessDataField = new JTextField(40);

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        initUI();
        loadFromSettings();
        pack();
        setMinimumSize(new Dimension(540, 0));
        setLocationRelativeTo(owner);
    }

    // =========================================================
    //  UI construction
    // =========================================================

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));

        centre.add(buildAiPanel());
        centre.add(Box.createVerticalStrut(8));
        centre.add(buildOcrPanel());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn   = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        saveBtn.addActionListener(e -> { saveToSettings(); dispose(); });
        cancelBtn.addActionListener(e -> dispose());
        buttons.add(saveBtn);
        buttons.add(cancelBtn);

        add(centre,  BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private JPanel buildAiPanel() {
        JPanel outer = new JPanel(new BorderLayout(4, 6));
        outer.setBorder(new TitledBorder("AI Provider"));

        // Provider selector row
        JPanel selectorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        selectorRow.add(new JLabel("Active provider:"));
        selectorRow.add(providerCombo);
        outer.add(selectorRow, BorderLayout.NORTH);

        // Discover providers; build one card per provider
        List<AiProvider> providers = AiProvider.loadAll();
        if (providers.isEmpty()) {
            // Fallback if ServiceLoader finds nothing (e.g. running from IDE without resources)
            providers = List.of(
                new com.promptmanager.ai.OpenAiProvider(),
                new com.promptmanager.ai.ClaudeProvider(),
                new com.promptmanager.ai.GeminiProvider(),
                new com.promptmanager.ai.OllamaProvider(),
                new com.promptmanager.ai.LlamaCppProvider()
            );
        }

        for (AiProvider p : providers) {
            providerCombo.addItem(p.getName());
            ProviderPanel panel = new ProviderPanel(p.getId());
            providerPanels.put(p.getName(), panel);
            providerCards.add(panel, p.getName());
        }

        providerCombo.addActionListener(e -> {
            String selected = (String) providerCombo.getSelectedItem();
            ((CardLayout) providerCards.getLayout()).show(providerCards, selected);
        });

        outer.add(providerCards, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildOcrPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("OCR / Tesseract"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        addRow(panel, gc, 0, "Tesseract data path:", tessDataField);

        JButton browseBtn = new JButton("Browse…");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                tessDataField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gc.gridx = 2; gc.gridy = 0; gc.fill = GridBagConstraints.NONE;
        panel.add(browseBtn, gc);

        return panel;
    }

    // =========================================================
    //  Load / Save
    // =========================================================

    private void loadFromSettings() {
        // Select the active provider in the combo
        String activeId = settings.getActiveProviderId();
        for (int i = 0; i < providerCombo.getItemCount(); i++) {
            String name = providerCombo.getItemAt(i);
            ProviderPanel pp = providerPanels.get(name);
            if (pp != null && pp.providerId.equals(activeId)) {
                providerCombo.setSelectedIndex(i);
                break;
            }
        }

        // Load each provider's fields
        for (ProviderPanel pp : providerPanels.values()) {
            pp.load();
        }

        tessDataField.setText(settings.getTesseractDataPath());
    }

    private void saveToSettings() {
        // Save active provider id
        String selectedName = (String) providerCombo.getSelectedItem();
        ProviderPanel active = providerPanels.get(selectedName);
        if (active != null) {
            settings.setActiveProviderId(active.providerId);
        }

        // Save all provider fields
        for (ProviderPanel pp : providerPanels.values()) {
            pp.save();
        }

        settings.setTesseractDataPath(tessDataField.getText().trim());
        settings.save();
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private void addRow(JPanel panel, GridBagConstraints gc, int row,
                        String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1;
        gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        panel.add(new JLabel(label), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        panel.add(field, gc);
        gc.weightx = 0;
    }

    // =========================================================
    //  Inner class: per-provider field panel
    // =========================================================

    /**
     * A small panel holding the settings fields for one provider.
     * Fields that don't apply to a given provider are simply left empty
     * and invisible, keeping the layout consistent.
     */
    private class ProviderPanel extends JPanel {

        final String         providerId;
        final JTextField     baseUrlField = new JTextField(36);
        final JPasswordField apiKeyField  = new JPasswordField(36);
        final JTextField     modelField   = new JTextField(36);

        ProviderPanel(String providerId) {
            super(new GridBagLayout());
            this.providerId = providerId;
            buildFields();
        }

        private void buildFields() {
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(3, 4, 3, 4);
            gc.anchor = GridBagConstraints.WEST;

            // Base URL — shown for all providers; local ones use localhost defaults
            addRow(this, gc, 0, "Base URL:", baseUrlField);

            // API Key — shown for all; local providers will leave it blank
            addRow(this, gc, 1, "API Key:", apiKeyField);

            // Model
            addRow(this, gc, 2, "Model:", modelField);
        }

        void load() {
            baseUrlField.setText(settings.getProviderSetting(providerId, "baseUrl", ""));
            apiKeyField.setText( settings.getProviderSetting(providerId, "apiKey",  ""));
            modelField.setText(  settings.getProviderSetting(providerId, "model",   ""));
        }

        void save() {
            settings.setProviderSetting(providerId, "baseUrl", baseUrlField.getText().trim());
            settings.setProviderSetting(providerId, "apiKey",  new String(apiKeyField.getPassword()));
            settings.setProviderSetting(providerId, "model",   modelField.getText().trim());
        }
    }
}
