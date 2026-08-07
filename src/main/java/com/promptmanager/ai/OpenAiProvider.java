package com.promptmanager.ai;

import com.promptmanager.util.AppSettings;

/**
 * Provider for OpenAI-compatible APIs.
 *
 * <p>This single implementation covers three cases because they all share
 * the same {@code /v1/chat/completions} request/response shape:
 * <ul>
 *   <li><b>ChatGPT</b> — base URL {@code https://api.openai.com/v1}, API key required</li>
 *   <li><b>Ollama</b>  — base URL {@code http://localhost:11434/v1}, no API key</li>
 *   <li><b>llama-cpp</b> — base URL {@code http://localhost:8080/v1}, no API key</li>
 * </ul>
 *
 * Settings keys (in settings.properties):
 * <pre>
 *   ai.openai.baseUrl = https://api.openai.com/v1
 *   ai.openai.apiKey  = sk-...
 *   ai.openai.model   = gpt-4o
 * </pre>
 */
public class OpenAiProvider extends AbstractRestProvider {

    public static final String ID = "openai";

    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL    = "gpt-4o";

    @Override public String getId()   { return ID; }
    @Override public String getName() { return "ChatGPT (OpenAI)"; }

    @Override
    protected String endpointUrl() {
        String base = AppSettings.getInstance().getProviderSetting(ID, "baseUrl", DEFAULT_BASE_URL);
        // Normalise: strip trailing slash, append path
        return base.replaceAll("/+$", "") + "/chat/completions";
    }

    @Override
    protected String apiKey() {
        return AppSettings.getInstance().getProviderSetting(ID, "apiKey", "");
    }

    private String model() {
        return AppSettings.getInstance().getProviderSetting(ID, "model", DEFAULT_MODEL);
    }

    @Override
    protected String buildRequestBody(String prompt) throws AiException {
        String sys = getSystemMessage();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(jsonEscape(model())).append("\",");
        sb.append("\"messages\":[");
        if (!sys.isBlank()) {
            sb.append("{\"role\":\"system\",\"content\":\"").append(jsonEscape(sys)).append("\"},");
        }
        sb.append("{\"role\":\"user\",\"content\":\"").append(jsonEscape(prompt)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }

    @Override
    protected String parseResponseBody(String body) throws AiException {
        // Response shape: {"choices":[{"message":{"content":"..."}}]}
        // We locate "content" after the first "message" occurrence to avoid
        // matching "content" in the echo of the request (some servers include it).
        int msgIdx = body.indexOf("\"message\"");
        if (msgIdx < 0) throw new AiException("No 'message' field in OpenAI response: " + body);
        String afterMsg = body.substring(msgIdx);
        return jsonString(afterMsg, "content");
    }
}
