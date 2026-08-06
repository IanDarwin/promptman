package com.promptmanager.ui;

import com.promptmanager.dao.StockPhraseDAO;
import com.promptmanager.model.StockPhrase;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Dialog for adding, editing, deleting and reordering stock phrases.
 */
public class StockPhrasesDialog extends JDialog {

    private final StockPhraseDAO dao;
    private final DefaultListModel<StockPhrase> listModel = new DefaultListModel<>();
    private final JList<StockPhrase> phraseList = new JList<>(listModel);
    private final JTextArea textArea = new JTextArea(4, 40);

    public StockPhrasesDialog(Frame owner, StockPhraseDAO dao) {
        super(owner, "Manage Stock Phrases", true);
        this.dao = dao;
        initUI();
        loadPhrases();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));

        // Left: list
        JPanel listPanel = new JPanel(new BorderLayout(4, 4));
        listPanel.setBorder(new TitledBorder("Phrases"));
        phraseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        phraseList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSelect();
        });
        listPanel.add(new JScrollPane(phraseList), BorderLayout.CENTER);

        // Reorder buttons
        JPanel reorderPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        JButton upBtn   = new JButton("▲ Up");
        JButton downBtn = new JButton("▼ Down");
        upBtn.addActionListener(e -> moveSelected(-1));
        downBtn.addActionListener(e -> moveSelected(1));
        reorderPanel.add(upBtn);
        reorderPanel.add(downBtn);
        listPanel.add(reorderPanel, BorderLayout.EAST);

        // Right: editor
        JPanel editPanel = new JPanel(new BorderLayout(4, 4));
        editPanel.setBorder(new TitledBorder("Edit phrase text"));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        editPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Edit action buttons
        JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn    = new JButton("Add New");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        editButtons.add(addBtn);
        editButtons.add(updateBtn);
        editButtons.add(deleteBtn);

        addBtn.addActionListener(e -> addPhrase());
        updateBtn.addActionListener(e -> updatePhrase());
        deleteBtn.addActionListener(e -> deletePhrase());
        editPanel.add(editButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, editPanel);
        split.setDividerLocation(280);

        // Bottom close
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);

        add(split,  BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadPhrases() {
        try {
            listModel.clear();
            for (StockPhrase sp : dao.findAll()) {
                listModel.addElement(sp);
            }
        } catch (SQLException e) {
            showError("Could not load stock phrases: " + e.getMessage());
        }
    }

    private void onSelect() {
        StockPhrase sp = phraseList.getSelectedValue();
        textArea.setText(sp == null ? "" : sp.getText());
    }

    private void addPhrase() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) { showError("Please enter phrase text."); return; }
        try {
            StockPhrase sp = new StockPhrase(text, listModel.size() + 1);
            dao.insert(sp);
            loadPhrases();
        } catch (SQLException e) {
            showError("Could not add phrase: " + e.getMessage());
        }
    }

    private void updatePhrase() {
        StockPhrase sp = phraseList.getSelectedValue();
        if (sp == null) { showError("Select a phrase to update."); return; }
        sp.setText(textArea.getText().trim());
        try {
            dao.update(sp);
            loadPhrases();
        } catch (SQLException e) {
            showError("Could not update phrase: " + e.getMessage());
        }
    }

    private void deletePhrase() {
        StockPhrase sp = phraseList.getSelectedValue();
        if (sp == null) { showError("Select a phrase to delete."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this phrase?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dao.delete(sp.getId());
                loadPhrases();
                textArea.setText("");
            } catch (SQLException e) {
                showError("Could not delete phrase: " + e.getMessage());
            }
        }
    }

    private void moveSelected(int delta) {
        int idx = phraseList.getSelectedIndex();
        int newIdx = idx + delta;
        if (idx < 0 || newIdx < 0 || newIdx >= listModel.size()) return;
        // Swap in list model
        StockPhrase a = listModel.get(idx);
        StockPhrase b = listModel.get(newIdx);
        listModel.set(idx, b);
        listModel.set(newIdx, a);
        phraseList.setSelectedIndex(newIdx);
        // Persist new order
        try {
            dao.reorder(java.util.Collections.list(listModel.elements()));
        } catch (SQLException e) {
            showError("Could not reorder: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
