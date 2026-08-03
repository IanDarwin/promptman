package com.promptmanager.dao;

import com.promptmanager.model.Prompt;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PromptDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager db;

    public PromptDAO(DatabaseManager db) {
        this.db = db;
    }

    /** Insert a new prompt and return it with its generated id set. */
    public Prompt insert(Prompt p) throws SQLException {
        String sql = "INSERT INTO prompts (summary, wording, date_created, comments) VALUES (?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getSummary());
            ps.setString(2, p.getWording());
            ps.setString(3, LocalDateTime.now().format(FMT));
            ps.setString(4, nvl(p.getComments()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                p.setId(keys.getInt(1));
            }
        }
        return p;
    }

    /** Update all fields of an existing prompt. */
    public void update(Prompt p) throws SQLException {
        String sql = "UPDATE prompts SET summary=?, wording=?, comments=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getSummary());
            ps.setString(2, p.getWording());
            ps.setString(3, nvl(p.getComments()));
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        }
    }

    /** Delete a prompt by id. */
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM prompts WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Return all prompts ordered by date created descending. */
    public List<Prompt> findAll() throws SQLException {
        return query("SELECT * FROM prompts ORDER BY date_created DESC", null);
    }

    /**
     * Search across summary, wording, and comments (case-insensitive).
     * Passing a blank/null term returns all records.
     */
    public List<Prompt> search(String term) throws SQLException {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        String sql = """
            SELECT * FROM prompts
            WHERE  lower(summary)  LIKE ?
               OR  lower(wording)  LIKE ?
               OR  lower(comments) LIKE ?
            ORDER BY date_created DESC
            """;
        String pattern = "%" + term.toLowerCase() + "%";
        return query(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
        });
    }

    // ---- helpers ----

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Prompt> query(String sql, Binder binder) throws SQLException {
        List<Prompt> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Prompt map(ResultSet rs) throws SQLException {
        Prompt p = new Prompt();
        p.setId(rs.getInt("id"));
        p.setSummary(rs.getString("summary"));
        p.setWording(rs.getString("wording"));
        p.setDateCreated(LocalDateTime.parse(rs.getString("date_created"), FMT));
        p.setComments(rs.getString("comments"));
        return p;
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
