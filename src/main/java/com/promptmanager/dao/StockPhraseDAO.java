package com.promptmanager.dao;

import com.promptmanager.model.StockPhrase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockPhraseDAO {

    private final DatabaseManager db;

    public StockPhraseDAO(DatabaseManager db) {
        this.db = db;
    }

    public List<StockPhrase> findAll() throws SQLException {
        List<StockPhrase> list = new ArrayList<>();
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM stock_phrases ORDER BY sort_order, id")) {
            while (rs.next()) {
                StockPhrase sp = new StockPhrase();
                sp.setId(rs.getInt("id"));
                sp.setText(rs.getString("text"));
                sp.setSortOrder(rs.getInt("sort_order"));
                list.add(sp);
            }
        }
        return list;
    }

    public StockPhrase insert(StockPhrase sp) throws SQLException {
        String sql = "INSERT INTO stock_phrases (text, sort_order) VALUES (?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sp.getText());
            ps.setInt(2, sp.getSortOrder());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) sp.setId(keys.getInt(1));
        }
        return sp;
    }

    public void update(StockPhrase sp) throws SQLException {
        String sql = "UPDATE stock_phrases SET text=?, sort_order=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, sp.getText());
            ps.setInt(2, sp.getSortOrder());
            ps.setInt(3, sp.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM stock_phrases WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Persist a reordered list by updating each row's sort_order. */
    public void reorder(List<StockPhrase> ordered) throws SQLException {
        String sql = "UPDATE stock_phrases SET sort_order=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < ordered.size(); i++) {
                ps.setInt(1, i + 1);
                ps.setInt(2, ordered.get(i).getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
