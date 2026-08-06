package com.promptmanager.util;

import java.io.*;
import java.util.Properties;

/**
 * Thin wrapper around a Properties file stored at
 * ~/.prompt-manager/settings.properties.
 * All AI / OCR settings are declared here as typed getters/setters
 * so the rest of the code never hard-codes property keys.
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

    // ---- AI settings (stubbed, not yet wired) ----

    public String getAiProvider()    { return props.getProperty("ai.provider",  "OpenAI"); }
    public void   setAiProvider(String v) { props.setProperty("ai.provider", v); }

    public String getAiBaseUrl()     { return props.getProperty("ai.baseUrl",   "https://api.openai.com/v1"); }
    public void   setAiBaseUrl(String v)  { props.setProperty("ai.baseUrl", v); }

    public String getAiApiKey()      { return props.getProperty("ai.apiKey",    ""); }
    public void   setAiApiKey(String v)   { props.setProperty("ai.apiKey", v); }

    public String getAiModel()       { return props.getProperty("ai.model",     "gpt-4o"); }
    public void   setAiModel(String v)    { props.setProperty("ai.model", v); }

    // ---- OCR settings ----

    public String getTesseractDataPath() { return props.getProperty("ocr.tesseractDataPath", ""); }
    public void   setTesseractDataPath(String v) { props.setProperty("ocr.tesseractDataPath", v); }

    // ---- UI preferences ----

    public int getDividerLocation()       { return Integer.parseInt(props.getProperty("ui.divider", "300")); }
    public void setDividerLocation(int v) { props.setProperty("ui.divider", String.valueOf(v)); }

    // ---- persistence ----

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
