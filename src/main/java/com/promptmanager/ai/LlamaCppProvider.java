package com.promptmanager.ai;

import com.promptmanager.util.AppSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Provider for a local llama-cpp server started with {@code --server} mode.
 *
 * <p>The model name is discovered automatically from the server's
 * {@code /v1/models} endpoint on first use and cached for the lifetime
 * of the instance.  No configuration of the model name is required.
 *
 * <p>Start llama-cpp in server mode with:
 * <pre>
 *   ./llama-server -m your-model.gguf --port 8080
 * </pre>
 *
 * Settings keys:
 * <pre>
 *   ai.llamacpp.baseUrl = http://localhost:8080/v1   (default)
 * </pre>
 */
public class LlamaCppProvider extends OpenAiProvider {

    public static final String LLAMACPP_ID    = "llamacpp";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/v1";

    /** Cached after the first successful call to {@link #discoverModelName()}. */
    private String discoveredModel = null;

    @Override public String getId()   { return LLAMACPP_ID; }
    @Override public String getName() { return "llama-cpp (local)"; }

    @Override
    protected String endpointUrl() {
        return baseUrl() + "/chat/completions";
    }

    /** llama-cpp needs no API key. */
    @Override
    protected String apiKey() { return ""; }

    @Override
    protected String buildRequestBody(String prompt) throws AiException {
        String model = resolvedModel();
        String sys   = getSystemMessage();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(jsonEscape(model)).append("\",");
        sb.append("\"messages\":[");
        if (!sys.isBlank()) {
            sb.append("{\"role\":\"system\",\"content\":\"")
              .append(jsonEscape(sys)).append("\"},");
        }
        sb.append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(prompt)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }

    // ---- model discovery ----

    /**
     * Returns the model name to use in requests.
     * Uses the explicitly configured value if present; otherwise calls
     * {@link #discoverModelName()} to ask the server.
     */
    private String resolvedModel() throws AiException {
        String configured = AppSettings.getInstance()
                .getProviderSetting(LLAMACPP_ID, "model", "");
        if (!configured.isBlank()) {
            return configured;
        }
        if (discoveredModel == null) {
            discoveredModel = discoverModelName();
        }
        return discoveredModel;
    }

    /**
     * Calls {@code GET /v1/models} and returns the {@code id} of the first
     * entry in the {@code data} array — the model the server has loaded.
     *
     * @throws AiException if the server is unreachable or returns no models
     */
    String discoverModelName() throws AiException {
        String url = baseUrl() + "/models";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AiException("Cannot reach llama-cpp at " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Model discovery interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new AiException("llama-cpp /v1/models returned HTTP " + response.statusCode());
        }

        // Parse: {"data":[{"id":"<model-name>", ...}, ...], ...}
        // We just need the id of the first entry.
        String body = response.body();
        int dataIdx = body.indexOf("\"data\"");
        if (dataIdx < 0) throw new AiException("No 'data' field in /v1/models response: " + body);
        String afterData = body.substring(dataIdx);
        // jsonString finds the first "id" key after "data"
        String modelId = jsonString(afterData, "id");
        if (modelId.isBlank()) throw new AiException("Empty model id in /v1/models response: " + body);
        return modelId;
    }

    private String baseUrl() {
        return AppSettings.getInstance()
                .getProviderSetting(LLAMACPP_ID, "baseUrl", DEFAULT_BASE_URL)
                .replaceAll("/+$", "");
    }
}
