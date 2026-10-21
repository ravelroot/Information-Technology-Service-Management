package io.pyhoma.domain.support;

import java.util.Arrays;

public class BusinessException extends RuntimeException {

    private final String messageKey;
    private final transient Object[] args;

    public BusinessException(String messageKey, Object... args) {
        super(messageKey + (args.length == 0 ? "" : " " + Arrays.toString(args)));
        this.messageKey = messageKey;
        this.args = args;
    }

    public String messageKey() {
        return messageKey;
    }

    public Object[] args() {
        return args.clone();
    }
}
