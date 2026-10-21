package io.pyhoma.domain.itil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriorityMatrixTest {

    @ParameterizedTest(name = "urgency {0} x impact {1} = {2}")
    @CsvSource({"1,1,1", "3,3,3", "5,5,5", "5,1,2", "1,5,2", "4,4,4", "5,4,5", "9,9,5", "0,0,1"})
    void computesPriority(int urgency, int impact, int expected) {
        assertEquals(expected, PriorityMatrix.compute(urgency, impact));
    }
}
