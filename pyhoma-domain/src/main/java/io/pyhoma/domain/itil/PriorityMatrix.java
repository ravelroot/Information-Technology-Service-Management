package io.pyhoma.domain.itil;

public final class PriorityMatrix {

    public static final int VERY_LOW = 1;
    public static final int LOW = 2;
    public static final int MEDIUM = 3;
    public static final int HIGH = 4;
    public static final int VERY_HIGH = 5;
    public static final int MAJOR = 6;

    private static final int[][] MATRIX = {
        {1, 1, 2, 2, 2},
        {1, 2, 2, 3, 3},
        {2, 2, 3, 4, 4},
        {2, 3, 4, 4, 5},
        {2, 3, 4, 5, 5},
    };

    private PriorityMatrix() {
    }

    public static int compute(int urgency, int impact) {
        int u = clamp(urgency);
        int i = clamp(impact);
        return MATRIX[i - 1][u - 1];
    }

    private static int clamp(int v) {
        return Math.max(1, Math.min(5, v));
    }
}
