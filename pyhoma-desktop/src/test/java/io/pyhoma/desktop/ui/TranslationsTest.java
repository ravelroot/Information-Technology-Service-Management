package io.pyhoma.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TranslationsTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");
    private static final Pattern LITERAL_KEY = Pattern.compile("I18n\\.t\\(\"([a-zA-Z][\\w.]*\\w)\"\\s*[,)]");

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = TranslationsTest.class.getResourceAsStream("/io/pyhoma/desktop/i18n/" + name)) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    @Test
    void bundlesHaveTheSameKeysAndPlaceholders() throws IOException {
        Properties en = load("messages.properties");
        Properties pt = load("messages_pt_BR.properties");
        assertEquals(new TreeSet<>(en.stringPropertyNames()), new TreeSet<>(pt.stringPropertyNames()));
        for (String key : en.stringPropertyNames()) {
            assertEquals(placeholders(en.getProperty(key)), placeholders(pt.getProperty(key)), key);
        }
    }

    @Test
    void everyKeyUsedInTheCodeIsTranslated() throws IOException {
        Properties en = load("messages.properties");
        Path src = Path.of("src/main/java");
        if (!Files.isDirectory(src)) {
            return;
        }
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(src)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = LITERAL_KEY.matcher(Files.readString(f));
                while (m.find()) {
                    if (!en.containsKey(m.group(1))) {
                        missing.add(m.group(1) + " (" + f.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Missing translations: " + missing);
    }
}
