package io.pyhoma.domain.support;

public class AccessDeniedException extends BusinessException {

    public AccessDeniedException(String module, String right) {
        super("error.accessDenied", module, right);
    }
}
