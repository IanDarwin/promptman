package com.promptmanager.ai;

import com.promptmanager.util.AppSettings;

/**
 * Provider for Google Gemini via the Generative Language REST API.
 *
 * <p>Gemini's API shape differs from both OpenAI and Claude:
 * <ul>
 *   <li>Auth is via a {@code ?key=...} query parameter, not a header</li>
 *   <li>The request uses {@code "contents"} → {@code "parts"} nesting</li>
 *   <li>System instructions are a separate top-level field
 *       {@code "systemInstruction"}</li>
 *   <li>The model name is embedded in the URL path</li>
 * </ul>
 *
 * Settings keys:
 * <pre>
 *   ai.gemini.apiKey = AIza...
 *   ai.gemini.model  = gemini-1.5-flash
 * </pre>
 */
public class GeminiProvider extends AbstractRestProvider {

    public static final String ID = "gemini";

    private static final String BASE_URL      = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

    @Override public String getId()   { return ID; }
    @Override public String getName() { return "Gemini (Google)"; }

    /** The actual Gemini API key, embedded in the URL rather than a header. */
    private String geminiApiKey() {
        return AppSettings.getInstance().getProviderSetting(ID, "apiKey", "");
    }

    private String model() {
        return AppSettings.getInstance().getProviderSetting(ID, "model", DEFAULT_MODEL);
    }

    @Override
    protected String endpointUrl() {
        // Auth key goes in the query string for Gemini, not a header
        return BASE_URL + model() + ":generateContent?key=" + geminiApiKey();
    }

    /**
     * Returns empty so the base class does not add an Authorization header.
     * Gemini auth is in the query string; see {@link #endpointUrl()}.
     */
    @Override
    protected String apiKey() { return ""; }

    @Override
    protected String buildRequestBody(String prompt) {
        // Shape:
        // {
        //   "systemInstruction": {"parts": [{"text": "..."}]},   <- optional
        //   "contents": [{"parts": [{"text": "..."}]}]
        // }
        String sys = getSystemMessage();
        StringBuilder sb = new StringBuilder("{");
        if (!sys.isBlank()) {
            sb.append("\"systemInstruction\":{\"parts\":[{\"text\":\"")
              .append(jsonEscape(sys))
              .append("\"}]},");
        }
        sb.append("\"contents\":[{\"parts\":[{\"text\":\"")
          .append(jsonEscape(prompt))
          .append("\"}]}]}");
        return sb.toString();
    }

    @Override
    protected String parseResponseBody(String body) throws AiException {
        // Response shape:
        // {"candidates":[{"content":{"parts":[{"text":"..."}],...},...}],...}
        int partsIdx = body.indexOf("\"parts\"");
        if (partsIdx < 0) throw new AiException("No 'parts' field in Gemini response: " + body);
        String afterParts = body.substring(partsIdx);
        return jsonString(afterParts, "text");
    }
}
