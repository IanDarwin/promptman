package com.promptmanager.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Shared HTTP plumbing for REST-based AI providers.
 *
 * <p>Subclasses supply:
 * <ul>
 *   <li>{@link #buildRequestBody(String)} — provider-specific JSON payload</li>
 *   <li>{@link #parseResponseBody(String)} — extract the reply text from JSON</li>
 *   <li>{@link #endpointUrl()} — the full POST URL</li>
 *   <li>{@link #apiKey()} — Bearer token / API key, or empty string if none</li>
 * </ul>
 *
 * <p>All providers share a single {@link HttpClient} instance (thread-safe,
 * reuses connections) configured with a 30-second timeout.
 */
public abstract class AbstractRestProvider implements AiProvider {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    // ---- Template method: subclasses implement these ----

    /** Full URL of the chat/completions endpoint. */
    protected abstract String endpointUrl();

    /**
     * API key / Bearer token.  Return an empty string for providers that
     * need no authentication (e.g. local Ollama, llama-cpp).
     */
    protected abstract String apiKey();

    /**
     * Build the JSON request body for the given user prompt.
     * Implementations should honour {@link #getSystemMessage()} if the
     * provider supports a system role.
     */
    protected abstract String buildRequestBody(String prompt) throws AiException;

    /**
     * Extract the reply text from the raw JSON response body.
     *
     * @throws AiException if the response cannot be parsed or indicates an error
     */
    protected abstract String parseResponseBody(String responseBody) throws AiException;

    // ---- Shared HTTP execution ----

    @Override
    public String complete(String prompt) throws AiException {
        if (prompt == null || prompt.isBlank()) {
            throw new AiException("Prompt must not be blank");
        }

        String body = buildRequestBody(prompt);  // may throw AiException — propagates directly

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json");

        String key = apiKey();
        if (key != null && !key.isBlank()) {
            builder.header("Authorization", "Bearer " + key);
        }

        builder.POST(HttpRequest.BodyPublishers.ofString(body));

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AiException("Network error contacting " + getName() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Request to " + getName() + " was interrupted", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiException(getName() + " returned HTTP " + response.statusCode()
                    + ": " + truncate(response.body(), 200));
        }

        return parseResponseBody(response.body());
    }

    @Override
    public boolean isAvailable() {
        try {
            // A minimal probe: send a tiny prompt and check we get a response.
            // Subclasses may override with a cheaper health-check if available.
            complete("Say OK");
            return true;
        } catch (AiException e) {
            return false;
        }
    }

    // ---- Minimal JSON helpers (no external library required) ----

    /**
     * Extracts the string value of a top-level JSON key from a flat object.
     * Enough for the simple response shapes we deal with; not a general parser.
     *
     * Example: {@code jsonString("{\"content\":\"hello\"}", "content")} → {@code "hello"}
     */
    protected static String jsonString(String json, String key) throws AiException {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) throw new AiException("Key '" + key + "' not found in JSON response");
        int colon = json.indexOf(':', keyIdx + search.length());
        if (colon < 0) throw new AiException("Malformed JSON: no colon after key '" + key + "'");
        int start = json.indexOf('"', colon + 1);
        if (start < 0) throw new AiException("Malformed JSON: no string value for key '" + key + "'");
        int end = start + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '\\') { end += 2; continue; }  // skip escape
            if (json.charAt(end) == '"')  { break; }
            end++;
        }
        return json.substring(start + 1, end)
                   .replace("\\n", "\n")
                   .replace("\\t", "\t")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\");
    }

    /** Safely escape a string for embedding in a JSON value. */
    protected static String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
