package com.promptmanager.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

    @Test
    void connectionCanBeOpened() throws Exception {

        DatabaseManager db = DatabaseManager.getInstance();

        Connection conn = db.getConnection();

        assertNotNull(conn);
        assertFalse(conn.isClosed());
    }

    @Test
    void closeClosesConnection() throws Exception {

        DatabaseManager db = DatabaseManager.getInstance();

        db.close();

        assertTrue(db.getConnection().isClosed());
    }
}