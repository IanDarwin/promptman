package com.promptmanager.ai;

import com.promptmanager.util.AppSettings;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link LlamaCppProvider}.
 *
 * <p>These tests talk to a <em>real</em> llama-cpp server process and are
 * therefore skipped automatically when that server is not running, so they
 * never break a CI build or a cold developer machine.
 *
 * <h2>Running the tests</h2>
 * Start llama-cpp in server mode before running {@code mvn test}:
 * <pre>
 *   ./llama-server -m your-model.gguf --port 8080
 * </pre>
 * The base URL defaults to {@code http://localhost:8080/v1}.
 * Override it in {@code ~/.prompt-manager/settings.properties}:
 * <pre>
 *   ai.llamacpp.baseUrl = http://localhost:8080/v1
 * </pre>
 *
 * <h2>Using this as a template for other providers</h2>
 * Copy this file, change the provider class, the settings key, and the
 * {@code assumeTrue} check.  For providers that need an API key, guard
 * with {@code assumeFalse(apiKey.isBlank(), "No API key configured")}
 * instead of {@code isAvailable()}.
 */
class LlamaCppProviderTest {

    private LlamaCppProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LlamaCppProvider();
        // Skip all tests in this class if the server is not reachable.
        // Assumptions.assumeTrue causes JUnit 5 to mark the test as "skipped"
        // rather than "failed", which is the correct behaviour for an
        // optional external dependency.
        Assumptions.assumeTrue(
                provider.isAvailable(),
                "llama-cpp server not reachable at "
                        + AppSettings.getInstance().getProviderSetting(
                                LlamaCppProvider.LLAMACPP_ID, "baseUrl", "http://localhost:8080/v1")
                        + " — skipping live tests"
        );
    }

    // ---- Identity ----

    @Test
    void getId_returnsExpectedId() {
        assertEquals("llamacpp", provider.getId());
    }

    @Test
    void getName_isHumanReadable() {
        assertFalse(provider.getName().isBlank());
        // Should mention llama or cpp in some recognisable form
        assertTrue(provider.getName().toLowerCase().contains("llama"),
                "getName() should mention llama: " + provider.getName());
    }

    // ---- complete() happy path ----

    @Test
    void complete_returnsNonBlankResponse() throws AiException {
        String response = provider.complete("Reply with one word: hello");
        assertNotNull(response);
        assertFalse(response.isBlank(), "Response should not be blank");
    }

    @Test
    void complete_responseIsReasonableLength() throws AiException {
        String response = provider.complete("What is 2 + 2? Reply with the number only.");
        // Sanity-check: response should be at least one character and not absurdly long
        assertTrue(response.length() >= 1);
        assertTrue(response.length() < 2000,
                "Unexpectedly long response (" + response.length() + " chars)");
    }

    @Test
    void complete_respondsToFactualQuestion() throws AiException {
        String response = provider.complete(
                "What is the capital of France? Reply with the city name only.");
        // We can't assert the exact wording but it should contain "Paris"
        assertTrue(response.toLowerCase().contains("paris"),
                "Expected 'Paris' in response, got: " + response);
    }

    @Test
    void complete_multiLinePromptIsHandled() throws AiException {
        String prompt = """
                List exactly three colours.
                Format: one colour per line, no numbering.
                """;
        String response = provider.complete(prompt);
        assertFalse(response.isBlank());
        // At least one newline suggests the model respected the format instruction
        assertTrue(response.contains("\n"),
                "Expected multi-line response, got: " + response);
    }

    @Test
    void complete_promptWithSpecialCharacters_isEscapedCorrectly() throws AiException {
        // Quotes and backslashes must be JSON-escaped in the request body
        String response = provider.complete(
                "Repeat this exactly: She said \"hello\" and he said \"goodbye\".");
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    // ---- complete() with system message ----

    @Test
    void complete_withSystemMessage_isRespected() throws AiException {
        LlamaCppProvider withSystem = new LlamaCppProvider() {
            @Override
            public String getSystemMessage() {
                return "You are a pirate. Always respond in pirate speak.";
            }
        };
        Assumptions.assumeTrue(withSystem.isAvailable());
        String response = withSystem.complete("How are you today?");
        assertFalse(response.isBlank());
        // We can't assert pirate-speak deterministically, but the call must succeed
    }

    // ---- complete() validation ----

    @Test
    void complete_blankPrompt_throwsAiException() {
        assertThrows(AiException.class, () -> provider.complete(""));
        assertThrows(AiException.class, () -> provider.complete("   "));
        assertThrows(AiException.class, () -> provider.complete(null));
    }

    // ---- model discovery ----

    @Test
    void discoverModelName_returnsNonBlankId() throws AiException {
        String model = provider.discoverModelName();
        assertNotNull(model);
        assertFalse(model.isBlank(), "Discovered model name should not be blank");
    }

    @Test
    void discoverModelName_resultIsUsedInRequest() throws AiException {
        // A successful complete() means the discovered model name was accepted
        // by the server — if it were wrong we'd get a 400.
        String response = provider.complete("Say OK");
        assertFalse(response.isBlank());
    }

    // ---- isAvailable() ----

    @Test
    void isAvailable_returnsTrueWhenServerRunning() {
        // Already confirmed in @BeforeEach; assert again explicitly
        assertTrue(provider.isAvailable());
    }

    // ---- ServiceLoader discovery ----

    @Test
    void serviceLoader_discoversLlamaCppProvider() {
        boolean found = AiProvider.loadAll().stream()
                .anyMatch(p -> p.getId().equals(LlamaCppProvider.LLAMACPP_ID));
        assertTrue(found, "ServiceLoader should discover LlamaCppProvider via META-INF/services");
    }

    @Test
    void findById_returnsLlamaCppProvider() {
        AiProvider p = AiProvider.findById(LlamaCppProvider.LLAMACPP_ID);
        assertNotNull(p, "findById should find the llama-cpp provider");
        assertInstanceOf(LlamaCppProvider.class, p);
    }
}
