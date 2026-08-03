package com.promptmanager.ui;

import com.promptmanager.model.Prompt;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PromptTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Summary", "Date Created"};
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private List<Prompt> prompts = new ArrayList<>();

    public void setPrompts(List<Prompt> prompts) {
        this.prompts = new ArrayList<>(prompts);
        fireTableDataChanged();
    }

    public Prompt getPromptAt(int row) {
        return prompts.get(row);
    }

    @Override public int getRowCount()    { return prompts.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override
    public Object getValueAt(int row, int col) {
        Prompt p = prompts.get(row);
        return switch (col) {
            case 0 -> p.getSummary();
            case 1 -> p.getDateCreated() != null ? p.getDateCreated().format(DISPLAY_FMT) : "";
            default -> "";
        };
    }
}
