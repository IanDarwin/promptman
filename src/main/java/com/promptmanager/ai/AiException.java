package com.promptmanager.ai;

/**
 * Checked exception thrown by {@link AiProvider} implementations.
 *
 * Using a checked exception forces callers to decide explicitly how to handle
 * provider failures (show a dialog, log and ignore, etc.) rather than letting
 * them propagate silently as runtime exceptions.
 */
public class AiException extends Exception {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
