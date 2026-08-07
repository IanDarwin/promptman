package com.promptmanager.ai;

import java.util.ServiceLoader;
import java.util.List;
import java.util.ArrayList;

/**
 * Core plugin interface for AI provider implementations.
 *
 * <p>Every provider — whether a cloud REST API (Claude, ChatGPT, Gemini)
 * or a local server (Ollama, llama-cpp) — implements this interface.
 * The application discovers providers at runtime via {@link ServiceLoader};
 * third-party providers can be added by dropping a JAR on the classpath
 * that contains an implementation and a META-INF/services registration.
 *
 * <p>Providers are stateless with respect to conversation history: each call
 * to {@link #complete} is an independent request.  Conversation threading,
 * if needed later, can be layered on top.
 *
 * <h2>Registering a provider</h2>
 * Add a file {@code META-INF/services/com.promptmanager.ai.AiProvider}
 * containing the fully-qualified class name of your implementation.
 */
public interface AiProvider {

    /**
     * Send {@code prompt} to the provider and return the response text.
     *
     * @param prompt  the user prompt; never null or blank
     * @return        the provider's response; never null
     * @throws AiException if the request fails for any reason (network error,
     *                     auth failure, rate limit, malformed response, etc.)
     */
    String complete(String prompt) throws AiException;

    /**
     * Optional system message prepended to every request, e.g.
     * "You are a prompt engineer. Improve the following prompt."
     * Defaults to an empty string (no system message).
     * Providers that don't support system messages should ignore it.
     */
    default String getSystemMessage() { return ""; }

    /**
     * Human-readable display name shown in the Settings UI,
     * e.g. {@code "Claude (Anthropic)"} or {@code "Ollama (local)"}.
     */
    String getName();

    /**
     * Short stable identifier used as the key in settings.properties,
     * e.g. {@code "claude"}, {@code "openai"}, {@code "ollama"}.
     * Must be lowercase, no spaces.
     */
    String getId();

    /**
     * Performs a lightweight check to confirm the provider is reachable
     * and (where possible) that credentials are valid, without consuming
     * significant tokens or quota.
     *
     * @return {@code true} if the provider appears ready to use
     */
    boolean isAvailable();

    /**
     * Returns all provider implementations discoverable on the current
     * classpath via {@link ServiceLoader}.
     */
    static List<AiProvider> loadAll() {
        List<AiProvider> providers = new ArrayList<>();
        ServiceLoader.load(AiProvider.class).forEach(providers::add);
        return providers;
    }

    /**
     * Finds a provider by its {@link #getId() id}, or returns {@code null}
     * if none is registered with that id.
     */
    static AiProvider findById(String id) {
        if (id == null) return null;
        return loadAll().stream()
                .filter(p -> p.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }
}
