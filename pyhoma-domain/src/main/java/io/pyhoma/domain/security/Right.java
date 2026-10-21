package io.pyhoma.domain.security;

import java.util.List;

public final class Right {

    public static final int READ = 1;
    public static final int CREATE = 1 << 1;
    public static final int UPDATE = 1 << 2;
    public static final int DELETE = 1 << 3;
    public static final int PURGE = 1 << 4;
    public static final int ASSIGN = 1 << 5;
    public static final int SEE_ALL = 1 << 6;
    public static final int APPROVE = 1 << 7;

    public static final int CRUD = READ | CREATE | UPDATE | DELETE;
    public static final int ALL = CRUD | PURGE | ASSIGN | SEE_ALL | APPROVE;

    public static final List<Integer> BITS = List.of(READ, CREATE, UPDATE, DELETE, PURGE, ASSIGN, SEE_ALL, APPROVE);

    private Right() {
    }

    public static boolean has(int mask, int right) {
        return (mask & right) == right;
    }

    public static String name(int bit) {
        return switch (bit) {
            case READ -> "READ";
            case CREATE -> "CREATE";
            case UPDATE -> "UPDATE";
            case DELETE -> "DELETE";
            case PURGE -> "PURGE";
            case ASSIGN -> "ASSIGN";
            case SEE_ALL -> "SEE_ALL";
            case APPROVE -> "APPROVE";
            default -> "UNKNOWN";
        };
    }
}
