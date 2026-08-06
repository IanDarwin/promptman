package com.promptmanager.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.*;

import com.promptmanager.model.Prompt;

class PromptDAOTest {

    private PromptDAO dao;

    @BeforeEach
    void setup() throws Exception {
        dao = new PromptDAO(DatabaseManager.getInstance(true));
    }

    @Test
    void insertAndFind() throws Exception {

        Prompt prompt = new Prompt("JUnit", "Testing");

        dao.insert(prompt);

        assertTrue(prompt.getId() > 0);

        List<Prompt> prompts = dao.findAll();

        assertTrue(prompts.stream()
                .anyMatch(p -> p.getId() == prompt.getId()));
    }

    @Test
    void updateChangesPrompt() throws Exception {

        Prompt prompt = dao.insert(new Prompt("Old", "Body"));

        prompt.setSummary("New");
        prompt.setComments("Updated");

        dao.update(prompt);

        Prompt updated = dao.search("New").get(0);

        assertEquals("New", updated.getSummary());
        assertEquals("Updated", updated.getComments());
    }

    @Test
    void deleteRemovesPrompt() throws Exception {

        Prompt prompt = dao.insert(new Prompt("Delete", "Body"));

        dao.delete(prompt.getId());

        List<Prompt> results = dao.search("Delete");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchFindsSummary() throws Exception {

        dao.insert(new Prompt("UniqueSummary123", "Text"));

        List<Prompt> results = dao.search("uniquesummary123");

        assertFalse(results.isEmpty());
    }

    @Test
    void blankSearchReturnsAll() throws Exception {

        List<Prompt> all = dao.findAll();

        assertEquals(all.size(), dao.search("").size());
        assertEquals(all.size(), dao.search(null).size());
    }
}
