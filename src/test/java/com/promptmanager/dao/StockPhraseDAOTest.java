package com.promptmanager.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.*;

import com.promptmanager.model.StockPhrase;

class StockPhraseDAOTest {

    private StockPhraseDAO dao;

    @BeforeEach
    void setup() throws Exception {
        dao = new StockPhraseDAO(DatabaseManager.getInstance());
    }

    @Test
    void insertAddsPhrase() throws Exception {

        StockPhrase phrase = new StockPhrase("JUnit Phrase", 100);

        dao.insert(phrase);

        assertTrue(phrase.getId() > 0);
    }

    @Test
    void updateChangesPhrase() throws Exception {

        StockPhrase phrase = dao.insert(new StockPhrase("Old", 5));

        phrase.setText("New");
        phrase.setSortOrder(99);

        dao.update(phrase);

        List<StockPhrase> list = dao.findAll();

        StockPhrase updated = list.stream()
                .filter(p -> p.getId() == phrase.getId())
                .findFirst()
                .orElseThrow();

        assertEquals("New", updated.getText());
        assertEquals(99, updated.getSortOrder());
    }

    @Test
    void deleteRemovesPhrase() throws Exception {

        StockPhrase phrase = dao.insert(new StockPhrase("Delete Me", 1));

        dao.delete(phrase.getId());

        assertFalse(dao.findAll().stream()
                .anyMatch(p -> p.getId() == phrase.getId()));
    }

    @Test
    void reorderUpdatesSortOrder() throws Exception {

        StockPhrase first = dao.insert(new StockPhrase("One", 100));
        StockPhrase second = dao.insert(new StockPhrase("Two", 101));

        dao.reorder(List.of(second, first));

        List<StockPhrase> list = dao.findAll();

        StockPhrase updatedSecond = list.stream()
                .filter(p -> p.getId() == second.getId())
                .findFirst()
                .orElseThrow();

        StockPhrase updatedFirst = list.stream()
                .filter(p -> p.getId() == first.getId())
                .findFirst()
                .orElseThrow();

        assertEquals(1, updatedSecond.getSortOrder());
        assertEquals(2, updatedFirst.getSortOrder());
    }
}