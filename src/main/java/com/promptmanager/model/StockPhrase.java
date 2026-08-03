package com.promptmanager.model;

public class StockPhrase {
    private int id;
    private String text;
    private int sortOrder;

    public StockPhrase() {}

    public StockPhrase(String text, int sortOrder) {
        this.text = text;
        this.sortOrder = sortOrder;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public String toString() {
        return text != null ? text : "";
    }
}
