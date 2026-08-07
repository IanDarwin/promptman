package com.promptmanager.ai;

import com.promptmanager.util.AppSettings;

/**
 * Provider for a local Ollama server.
 *
 * <p>Ollama exposes an OpenAI-compatible {@code /v1/chat/completions} endpoint,
 * so this is a thin subclass of {@link OpenAiProvider} that simply changes the
 * default base URL and removes the API key requirement.
 *
 * Settings keys:
 * <pre>
 *   ai.ollama.baseUrl = http://localhost:11434/v1
 *   ai.ollama.model   = llama3
 * </pre>
 */
public class OllamaProvider extends OpenAiProvider {

    public static final String OLLAMA_ID = "ollama";

    private static final String DEFAULT_BASE_URL = "http://localhost:11434/v1";
    private static final String DEFAULT_MODEL    = "llama3";

    @Override public String getId()   { return OLLAMA_ID; }
    @Override public String getName() { return "Ollama (local)"; }

    @Override
    protected String endpointUrl() {
        String base = AppSettings.getInstance()
                .getProviderSetting(OLLAMA_ID, "baseUrl", DEFAULT_BASE_URL);
        return base.replaceAll("/+$", "") + "/chat/completions";
    }

    /** Ollama needs no API key. */
    @Override
    protected String apiKey() { return ""; }

    @Override
    protected String buildRequestBody(String prompt) {
        String model = AppSettings.getInstance()
                .getProviderSetting(OLLAMA_ID, "model", DEFAULT_MODEL);
        String sys = getSystemMessage();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(jsonEscape(model)).append("\",");
        sb.append("\"messages\":[");
        if (!sys.isBlank()) {
            sb.append("{\"role\":\"system\",\"content\":\"").append(jsonEscape(sys)).append("\"},");
        }
        sb.append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(prompt)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }
}
