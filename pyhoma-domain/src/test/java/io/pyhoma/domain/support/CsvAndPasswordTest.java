package io.pyhoma.domain.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.security.PasswordHasher;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvAndPasswordTest {

    @Test
    void csvRoundTripWithQuotesAndNewLines() {
        String text = Csv.write(List.of("name", "notes"), List.of(List.of("PC 01", "a;b"), List.of("PC \"2\"", "line1\nline2")), ';');
        var rows = Csv.parseWithHeader(text);
        assertEquals(2, rows.size());
        assertEquals("a;b", rows.get(0).get("notes"));
        assertEquals("PC \"2\"", rows.get(1).get("name"));
        assertEquals("line1\nline2", rows.get(1).get("notes"));
    }

    @Test
    void csvDetectsCommaSeparator() {
        var rows = Csv.parseWithHeader("name,serialNumber\nA,1\nB,2\n");
        assertEquals("2", rows.get(1).get("serialNumber"));
    }

    @Test
    void passwordHashing() {
        String h1 = PasswordHasher.hash("S3cret-pass");
        String h2 = PasswordHasher.hash("S3cret-pass");
        assertNotEquals(h1, h2, "salted");
        assertTrue(PasswordHasher.verify("S3cret-pass", h1));
        assertFalse(PasswordHasher.verify("wrong", h1));
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.isStrongEnough("short1"));
        assertFalse(PasswordHasher.isStrongEnough("onlyletters"));
        assertTrue(PasswordHasher.isStrongEnough("lantern-73-quiet"));
    }
}
