package com.promptmanager.ai;

import com.promptmanager.util.AppSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Provider for Anthropic's Claude via the Messages API.
 *
 * <p>Claude's API differs from the OpenAI shape in two important ways:
 * <ul>
 *   <li>Authentication uses {@code x-api-key} and {@code anthropic-version}
 *       headers, not {@code Authorization: Bearer}</li>
 *   <li>The system message is a top-level field, not a message with role "system"</li>
 * </ul>
 * Because of the custom headers, this class manages its own HTTP call rather
 * than delegating to {@link AbstractRestProvider#complete}, but it still
 * extends {@link AbstractRestProvider} for the shared JSON helpers.
 *
 * Settings keys:
 * <pre>
 *   ai.claude.apiKey  = sk-ant-...
 *   ai.claude.model   = claude-sonnet-4-6
 * </pre>
 */
public class ClaudeProvider extends AbstractRestProvider {

    public static final String ID = "claude";

    private static final String ENDPOINT         = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL    = "claude-sonnet-4-6";
    private static final int    MAX_TOKENS       = 1024;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override public String getId()   { return ID; }
    @Override public String getName() { return "Claude (Anthropic)"; }

    // endpointUrl and apiKey are used only by isAvailable(); complete() overrides directly.
    @Override protected String endpointUrl() { return ENDPOINT; }

    @Override
    protected String apiKey() {
        return AppSettings.getInstance().getProviderSetting(ID, "apiKey", "");
    }

    private String model() {
        return AppSettings.getInstance().getProviderSetting(ID, "model", DEFAULT_MODEL);
    }

    /**
     * Overrides the base implementation to inject Anthropic-specific headers.
     */
    @Override
    public String complete(String prompt) throws AiException {
        if (prompt == null || prompt.isBlank()) {
            throw new AiException("Prompt must not be blank");
        }
        String key = apiKey();
        if (key.isBlank()) {
            throw new AiException("No API key configured for Claude. Set ai.claude.apiKey in Settings.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type",       "application/json")
                .header("x-api-key",          key)
                .header("anthropic-version",  ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)))
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AiException("Network error contacting Claude: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Request to Claude was interrupted", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiException("Claude returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        return parseResponseBody(response.body());
    }

    @Override
    protected String buildRequestBody(String prompt) {
        // Claude Messages API shape:
        // {
        //   "model": "...",
        //   "max_tokens": 1024,
        //   "system": "...",          <- optional, top-level
        //   "messages": [{"role":"user","content":"..."}]
        // }
        String sys = getSystemMessage();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(jsonEscape(model())).append("\",");
        sb.append("\"max_tokens\":").append(MAX_TOKENS).append(",");
        if (!sys.isBlank()) {
            sb.append("\"system\":\"").append(jsonEscape(sys)).append("\",");
        }
        sb.append("\"messages\":[{\"role\":\"user\",\"content\":\"")
          .append(jsonEscape(prompt))
          .append("\"}]");
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected String parseResponseBody(String body) throws AiException {
        // Response shape: {"content":[{"type":"text","text":"..."}], ...}
        // Locate "text" after the first "content" array entry.
        int contentIdx = body.indexOf("\"content\"");
        if (contentIdx < 0) throw new AiException("No 'content' field in Claude response: " + body);
        String afterContent = body.substring(contentIdx);
        return jsonString(afterContent, "text");
    }
}
