package com.promptmanager.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StockPhraseTest {

    @Test
    void constructorInitializesFields() {
        StockPhrase phrase = new StockPhrase("Hello", 3);

        assertEquals("Hello", phrase.getText());
        assertEquals(3, phrase.getSortOrder());
    }

    @Test
    void gettersAndSettersWork() {
        StockPhrase phrase = new StockPhrase();

        phrase.setId(5);
        phrase.setText("Testing");
        phrase.setSortOrder(9);

        assertEquals(5, phrase.getId());
        assertEquals("Testing", phrase.getText());
        assertEquals(9, phrase.getSortOrder());
    }

    @Test
    void toStringReturnsText() {
        StockPhrase phrase = new StockPhrase();
        phrase.setText("Example");

        assertEquals("Example", phrase.toString());
    }

    @Test
    void toStringReturnsEmptyStringWhenNull() {
        StockPhrase phrase = new StockPhrase();

        assertEquals("", phrase.toString());
    }
}
