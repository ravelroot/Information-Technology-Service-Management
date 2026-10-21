package io.pyhoma.desktop.ui;

public final class UserMessage extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserMessage(String message) {
        super(message, null, false, false);
    }
}
