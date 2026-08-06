package com.promptmanager.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PromptTest {

    @Test
    void constructorInitializesFields() {
        Prompt prompt = new Prompt("Summary", "Body");

        assertEquals("Summary", prompt.getSummary());
        assertEquals("Body", prompt.getWording());
        assertNotNull(prompt.getDateCreated());
        assertEquals("", prompt.getComments());
    }

    @Test
    void gettersAndSettersWork() {
        Prompt prompt = new Prompt();
        LocalDateTime now = LocalDateTime.now();

        prompt.setId(10);
        prompt.setSummary("Test");
        prompt.setWording("Text");
        prompt.setDateCreated(now);
        prompt.setComments("Comment");

        assertEquals(10, prompt.getId());
        assertEquals("Test", prompt.getSummary());
        assertEquals("Text", prompt.getWording());
        assertEquals(now, prompt.getDateCreated());
        assertEquals("Comment", prompt.getComments());
    }

    @Test
    void toStringReturnsSummary() {
        Prompt prompt = new Prompt();
        prompt.setSummary("My Prompt");

        assertEquals("My Prompt", prompt.toString());
    }

    @Test
    void toStringReturnsUntitledWhenSummaryNull() {
        Prompt prompt = new Prompt();

        assertEquals("(untitled)", prompt.toString());
    }
}
