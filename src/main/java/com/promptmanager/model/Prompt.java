package com.promptmanager.model;

import java.time.LocalDateTime;

public class Prompt {
    private int id;
    private String summary;
    private String wording;
    private LocalDateTime dateCreated;
    private String comments;

    public Prompt() {}

    public Prompt(String summary, String wording) {
        this.summary = summary;
        this.wording = wording;
        this.dateCreated = LocalDateTime.now();
        this.comments = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getWording() { return wording; }
    public void setWording(String wording) { this.wording = wording; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    @Override
    public String toString() {
        return summary != null ? summary : "(untitled)";
    }
}
