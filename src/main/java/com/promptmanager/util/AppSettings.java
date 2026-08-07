package com.promptmanager.util;

import java.io.*;
import java.util.Properties;

/**
 * Thin wrapper around a Properties file stored at
 * ~/.prompt-manager/settings.properties.
 * Typed getters/setters ensure the rest of the code never hard-codes
 * property keys.
 *
 * Per-provider AI settings use the scheme:
 *   ai.<providerId>.<field>
 * e.g. ai.claude.apiKey, ai.openai.model, ai.ollama.baseUrl
 */
public class AppSettings {

    private static final String DIR  = System.getProperty("user.home") + File.separator + ".prompt-manager";
    private static final String PATH = DIR + File.separator + "settings.properties";

    private static AppSettings instance;
    private final Properties props = new Properties();

    private AppSettings() {
        load();
    }

    public static synchronized AppSettings getInstance() {
        if (instance == null) instance = new AppSettings();
        return instance;
    }

    // ---- Active provider ----

    /** The id of the currently active provider, e.g. "claude", "openai", "ollama". */
    public String getActiveProviderId()         { return props.getProperty("ai.activeProvider", "openai"); }
    public void   setActiveProviderId(String v) { props.setProperty("ai.activeProvider", v); }

    // ---- Per-provider settings ----

    /**
     * Returns a per-provider setting.
     * Key in properties file: {@code ai.<providerId>.<field>}
     * e.g. {@code ai.claude.apiKey}, {@code ai.ollama.baseUrl}.
     *
     * @param providerId  the provider's id, e.g. "claude", "openai", "ollama"
     * @param field       setting name, e.g. "apiKey", "model", "baseUrl"
     * @param defaultVal  value to return if the key is absent
     */
    public String getProviderSetting(String providerId, String field, String defaultVal) {
        String val = props.getProperty("ai." + providerId + "." + field);
        return (val == null || val.isBlank()) ? defaultVal : val;
    }

    public void setProviderSetting(String providerId, String field, String value) {
        props.setProperty("ai." + providerId + "." + field, value);
    }

    // ---- OCR settings ----

    public String getTesseractDataPath()          { return props.getProperty("ocr.tesseractDataPath", ""); }
    public void   setTesseractDataPath(String v)  { props.setProperty("ocr.tesseractDataPath", v); }

    // ---- UI preferences ----

    public int  getDividerLocation()       { return Integer.parseInt(props.getProperty("ui.divider", "300")); }
    public void setDividerLocation(int v)  { props.setProperty("ui.divider", String.valueOf(v)); }

    // ---- Persistence ----

    public void save() {
        try {
            new File(DIR).mkdirs();
            try (OutputStream out = new FileOutputStream(PATH)) {
                props.store(out, "AI Prompt Manager Settings");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save settings: " + e.getMessage());
        }
    }

    private void load() {
        File f = new File(PATH);
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Could not load settings: " + e.getMessage());
            }
        }
    }
}