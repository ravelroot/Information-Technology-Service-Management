package io.talos.domain.model;

public interface SoftDeletable {

    boolean isDeleted();

    void setDeleted(boolean deleted);
}
