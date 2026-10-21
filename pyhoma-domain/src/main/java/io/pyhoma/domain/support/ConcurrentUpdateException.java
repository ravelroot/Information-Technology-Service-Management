package io.pyhoma.domain.support;

public class ConcurrentUpdateException extends BusinessException {

    public ConcurrentUpdateException(String type, Object id) {
        super("error.concurrentUpdate", type, id);
    }
}
