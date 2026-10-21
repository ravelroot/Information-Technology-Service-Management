package io.pyhoma.domain.support;

public class NotFoundException extends BusinessException {

    public NotFoundException(String type, Object id) {
        super("error.notFound", type, id);
    }
}
