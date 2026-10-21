package io.pyhoma.infrastructure.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SchemaGeneratorTest {

    @ParameterizedTest
    @EnumSource(SchemaGenerator.Dialect.class)
    void baselineMatchesEntities(SchemaGenerator.Dialect dialect) throws Exception {
        String resource = "/db/migration/" + dialect.name().toLowerCase(Locale.ROOT) + "/V1__baseline.sql";
        try (InputStream in = SchemaGeneratorTest.class.getResourceAsStream(resource)) {
            String committed = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(SchemaGenerator.generate(dialect), committed,
                    "Entities changed: run SchemaGenerator and add a new migration (" + resource + ")");
        }
    }
}
