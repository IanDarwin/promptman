package com.promptmanager.ui;

import com.promptmanager.dao.DatabaseManager;
import com.promptmanager.dao.PromptDAO;
import com.promptmanager.dao.StockPhraseDAO;
import com.promptmanager.model.Prompt;
import com.promptmanager.model.StockPhrase;
import com.promptmanager.util.AppSettings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main application window.
 *
 * Layout (left-to-right split pane):
 *   LEFT  — search bar + prompt table
 *   RIGHT — detail / edit panel
 *
 * A second JSplitPane inside the right side separates the
 * edit fields (top) from the stock-phrases panel (bottom).
 */
public class MainWindow extends JFrame {

    // ---- DAOs ----
    private final DatabaseManager dbManager;
    private final PromptDAO       promptDAO;
    private final StockPhraseDAO  stockPhraseDAO;

    // ---- Left panel ----
    private final JTextField        searchField  = new JTextField();
    private final PromptTableModel  tableModel   = new PromptTableModel();
    private final JTable            promptTable  = new JTable(tableModel);

    // ---- Right panel – detail fields ----
    private final JTextField  summaryField  = new JTextField();
    private final JLabel      dateLabel     = new JLabel(" ");
    private final JTextArea   wordingArea   = new JTextArea(10, 40);
    private final JTextArea   commentsArea  = new JTextArea(5, 40);

    // ---- Right panel – action buttons ----
    private final JButton newBtn    = new JButton("New");
    private final JButton saveBtn   = new JButton("Save");
    private final JButton deleteBtn = new JButton("Delete");

    // ---- Stub buttons (not yet wired) ----
    private final JButton loadScreenshotBtn = new JButton("Load Screenshot…");
    private final JButton refineBtn         = new JButton("Refine with AI…");

    // ---- Stock phrases panel ----
    private final DefaultListModel<StockPhrase> phraseListModel = new DefaultListModel<>();
    private final JList<StockPhrase>            phraseList      = new JList<>(phraseListModel);
    private final JRadioButton prependRadio = new JRadioButton("Prepend");
    private final JRadioButton replaceRadio = new JRadioButton("Replace");
    private final JRadioButton appendRadio  = new JRadioButton("Append", true);

    // ---- State ----
    private Prompt currentPrompt = null;   // null means no selection / new record
    private boolean dirty = false;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    public MainWindow(DatabaseManager dbManager) {
        super("AI Prompt Manager");
        this.dbManager     = dbManager;
        this.promptDAO     = new PromptDAO(dbManager);
        this.stockPhraseDAO = new StockPhraseDAO(dbManager);

        initUI();
        loadPrompts(null);
        loadStockPhrases();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onExit(); }
        });

        AppSettings s = AppSettings.getInstance();
        setSize(1100, 680);
        setLocationRelativeTo(null);
    }

    // =========================================================
    //  UI construction
    // =========================================================

    private void initUI() {
        setJMenuBar(buildMenuBar());

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(),
                buildRightPanel());
        mainSplit.setDividerLocation(AppSettings.getInstance().getDividerLocation());
        mainSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e ->
                AppSettings.getInstance().setDividerLocation((int) e.getNewValue()));

        add(mainSplit);
    }

    // ---- Menu bar ----

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem  = new JMenuItem("New Prompt");
        JMenuItem exitItem = new JMenuItem("Exit");
        newItem.addActionListener(e -> onNew());
        exitItem.addActionListener(e -> onExit());
        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem managePhrasesItem = new JMenuItem("Manage Stock Phrases…");
        JMenuItem settingsItem      = new JMenuItem("Settings…");
        managePhrasesItem.addActionListener(e -> openManagePhrases());
        settingsItem.addActionListener(e -> openSettings());
        toolsMenu.add(managePhrasesItem);
        toolsMenu.addSeparator();
        toolsMenu.add(settingsItem);

        bar.add(fileMenu);
        bar.add(toolsMenu);
        return bar;
    }

    // ---- Left panel ----

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 3));

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(4, 0));
        searchBar.add(new JLabel("Search:"), BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);
        JButton clearBtn = new JButton("✕");
        clearBtn.setToolTipText("Clear search");
        clearBtn.setMargin(new Insets(1, 5, 1, 5));
        clearBtn.addActionListener(e -> { searchField.setText(""); loadPrompts(null); });
        searchBar.add(clearBtn, BorderLayout.EAST);

        // Live search on typing
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { liveSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { liveSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { liveSearch(); }
        });

        // Table
        promptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        promptTable.setRowHeight(24);
        promptTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        promptTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        promptTable.getTableHeader().setReorderingAllowed(false);
        promptTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTableSelect();
        });

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(promptTable), BorderLayout.CENTER);

        JLabel hint = new JLabel("  " + tableModel.getRowCount() + " prompt(s)");
        hint.setForeground(Color.GRAY);
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    // ---- Right panel ----

    private JSplitPane buildRightPanel() {
        JSplitPane rightSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                buildDetailPanel(),
                buildStockPhrasesPanel());
        rightSplit.setDividerLocation(420);
        rightSplit.setResizeWeight(0.75);
        return rightSplit;
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 3, 3, 6));

        // Fields
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(4, 4, 4, 4);
        gc.anchor  = GridBagConstraints.NORTHWEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;

        // Summary
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        fields.add(new JLabel("Summary:"), gc);
        gc.gridx = 1; gc.weightx = 1.0; gc.gridwidth = 2;
        fields.add(summaryField, gc);

        // Date
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.gridwidth = 1;
        fields.add(new JLabel("Date Created:"), gc);
        gc.gridx = 1; gc.gridwidth = 2;
        dateLabel.setForeground(Color.GRAY);
        fields.add(dateLabel, gc);

        // Wording
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE;
        fields.add(new JLabel("Wording:"), gc);
        wordingArea.setLineWrap(true);
        wordingArea.setWrapStyleWord(true);
        JScrollPane wordingScroll = new JScrollPane(wordingArea);
        wordingScroll.setPreferredSize(new Dimension(0, 160));
        gc.gridx = 1; gc.gridy = 2; gc.weightx = 1.0; gc.weighty = 0.6;
        gc.fill = GridBagConstraints.BOTH; gc.gridwidth = 1;
        fields.add(wordingScroll, gc);

        // Buttons beside wording
        JPanel wordingButtons = new JPanel(new GridLayout(2, 1, 4, 4));
        loadScreenshotBtn.setEnabled(true);
        loadScreenshotBtn.setToolTipText("Load an image via browse, drag-and-drop, or clipboard paste, then OCR into Wording");
        loadScreenshotBtn.addActionListener(e -> onLoadScreenshot());
        refineBtn.setEnabled(false);
        refineBtn.setToolTipText("AI refinement not yet implemented");
        wordingButtons.add(loadScreenshotBtn);
        wordingButtons.add(refineBtn);
        gc.gridx = 2; gc.weightx = 0; gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.NORTH;
        fields.add(wordingButtons, gc);

        // Comments
        gc.gridx = 0; gc.gridy = 3; gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.NORTHWEST; gc.weightx = 0; gc.weighty = 0;
        fields.add(new JLabel("Comments:"), gc);
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        JScrollPane commentsScroll = new JScrollPane(commentsArea);
        commentsScroll.setPreferredSize(new Dimension(0, 100));
        gc.gridx = 1; gc.gridy = 3; gc.weightx = 1.0; gc.weighty = 0.4;
        gc.fill = GridBagConstraints.BOTH; gc.gridwidth = 2;
        fields.add(commentsScroll, gc);

        panel.add(fields, BorderLayout.CENTER);

        // Action buttons at the bottom
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        newBtn.addActionListener(e -> onNew());
        saveBtn.addActionListener(e -> onSave());
        deleteBtn.addActionListener(e -> onDelete());
        btnPanel.add(newBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(deleteBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);

        clearDetail();
        return panel;
    }

    private JPanel buildStockPhrasesPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new TitledBorder("Stock Phrases"));

        phraseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        phraseList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) applyStockPhrase();
            }
        });

        // Insert mode radio buttons
        ButtonGroup group = new ButtonGroup();
        group.add(prependRadio);
        group.add(replaceRadio);
        group.add(appendRadio);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        radioPanel.add(new JLabel("Insert mode:"));
        radioPanel.add(prependRadio);
        radioPanel.add(replaceRadio);
        radioPanel.add(appendRadio);

        JButton applyBtn = new JButton("Apply ▶");
        applyBtn.addActionListener(e -> applyStockPhrase());
        radioPanel.add(Box.createHorizontalStrut(12));
        radioPanel.add(applyBtn);

        panel.add(new JScrollPane(phraseList), BorderLayout.CENTER);
        panel.add(radioPanel, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================
    //  Actions
    // =========================================================

    private void onNew() {
        if (!checkDirty()) return;
        currentPrompt = null;
        clearDetail();
        summaryField.requestFocus();
    }

    private void onSave() {
        String summary = summaryField.getText().trim();
        if (summary.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Summary cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            summaryField.requestFocus();
            return;
        }
        try {
            if (currentPrompt == null) {
                // New record
                currentPrompt = new Prompt(summary, wordingArea.getText());
                currentPrompt.setComments(commentsArea.getText());
                promptDAO.insert(currentPrompt);
            } else {
                currentPrompt.setSummary(summary);
                currentPrompt.setWording(wordingArea.getText());
                currentPrompt.setComments(commentsArea.getText());
                promptDAO.update(currentPrompt);
            }
            dirty = false;
            dateLabel.setText(currentPrompt.getDateCreated().format(DISPLAY_FMT));
            loadPrompts(searchField.getText());
            // Re-select the saved row
            selectPromptInTable(currentPrompt.getId());
        } catch (SQLException e) {
            showError("Could not save prompt: " + e.getMessage());
        }
    }

    private void onDelete() {
        if (currentPrompt == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this prompt?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            promptDAO.delete(currentPrompt.getId());
            currentPrompt = null;
            dirty = false;
            clearDetail();
            loadPrompts(searchField.getText());
        } catch (SQLException e) {
            showError("Could not delete prompt: " + e.getMessage());
        }
    }

    private void onTableSelect() {
        int row = promptTable.getSelectedRow();
        if (row < 0) return;
        if (!checkDirty()) {
            // Re-select the previous row without triggering another event
            return;
        }
        Prompt p = tableModel.getPromptAt(row);
        currentPrompt = p;
        summaryField.setText(p.getSummary());
        dateLabel.setText(p.getDateCreated() != null ? p.getDateCreated().format(DISPLAY_FMT) : "");
        wordingArea.setText(p.getWording());
        commentsArea.setText(p.getComments());
        wordingArea.setCaretPosition(0);
        commentsArea.setCaretPosition(0);
        deleteBtn.setEnabled(true);
        dirty = false;
    }

    private void onExit() {
        if (!checkDirty()) return;
        AppSettings.getInstance().save();
        dbManager.close();
        dispose();
        System.exit(0);
    }

    private void onLoadScreenshot() {
        ScreenshotOcrDialog dlg = new ScreenshotOcrDialog(this);
        dlg.setVisible(true);
        String text = dlg.getExtractedText();
        if (text != null && !text.isBlank()) {
            wordingArea.setText(text);
            wordingArea.setCaretPosition(0);
            markDirty();
        }
    }

    private void applyStockPhrase() {
        StockPhrase sp = phraseList.getSelectedValue();
        if (sp == null) return;
        String phrase  = sp.getText();
        String current = wordingArea.getText();

        if (replaceRadio.isSelected()) {
            wordingArea.setText(phrase);
        } else if (prependRadio.isSelected()) {
            wordingArea.setText(phrase + (current.isEmpty() ? "" : "\n\n" + current));
        } else { // append
            wordingArea.setText(current + (current.isEmpty() ? "" : "\n\n") + phrase);
        }
        wordingArea.requestFocus();
        dirty = true;
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private void loadPrompts(String searchTerm) {
        try {
            List<Prompt> list = (searchTerm == null || searchTerm.isBlank())
                    ? promptDAO.findAll()
                    : promptDAO.search(searchTerm);
            tableModel.setPrompts(list);
        } catch (SQLException e) {
            showError("Could not load prompts: " + e.getMessage());
        }
    }

    private void loadStockPhrases() {
        try {
            phraseListModel.clear();
            for (StockPhrase sp : stockPhraseDAO.findAll()) {
                phraseListModel.addElement(sp);
            }
        } catch (SQLException e) {
            showError("Could not load stock phrases: " + e.getMessage());
        }
    }

    private void liveSearch() {
        loadPrompts(searchField.getText());
    }

    private void clearDetail() {
        summaryField.setText("");
        dateLabel.setText("(will be set on save)");
        wordingArea.setText("");
        commentsArea.setText("");
        deleteBtn.setEnabled(false);
        dirty = false;
    }

    private void selectPromptInTable(int id) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getPromptAt(i).getId() == id) {
                promptTable.setRowSelectionInterval(i, i);
                promptTable.scrollRectToVisible(promptTable.getCellRect(i, 0, true));
                return;
            }
        }
    }

    /**
     * If there are unsaved changes, ask the user what to do.
     * Returns true if it's safe to proceed, false if the user cancelled.
     */
    private boolean checkDirty() {
        if (!dirty) return true;
        int choice = JOptionPane.showConfirmDialog(this,
                "You have unsaved changes. Discard them?",
                "Unsaved Changes",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dirty = false;
            return true;
        }
        return false;
    }

    private void markDirty() { dirty = true; }

    private void openSettings() {
        new SettingsDialog(this).setVisible(true);
    }

    private void openManagePhrases() {
        new StockPhrasesDialog(this, stockPhraseDAO).setVisible(true);
        loadStockPhrases();  // refresh after dialog closes
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Wire dirty-tracking after all fields are created
    public void wireDirtyTracking() {
        summaryField.getDocument().addDocumentListener(docListener());
        wordingArea .getDocument().addDocumentListener(docListener());
        commentsArea.getDocument().addDocumentListener(docListener());
    }

    private DocumentListener docListener() {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { markDirty(); }
            @Override public void removeUpdate(DocumentEvent e)  { markDirty(); }
            @Override public void changedUpdate(DocumentEvent e) { markDirty(); }
        };
    }
}
