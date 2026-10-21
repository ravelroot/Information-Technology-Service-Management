package io.pyhoma.desktop.views.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.VBox;

public final class Markdown {

    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|`(.+?)`");

    private Markdown() {
    }

    public static Node render(String text) {
        VBox box = new VBox(6);
        box.getStyleClass().add("md");
        List<String> paragraph = new ArrayList<>();
        for (String raw : text.replace("\r", "").split("\n", -1)) {
            String line = raw.stripTrailing();
            if (line.isBlank()) {
                flush(box, paragraph);
                continue;
            }
            if (line.startsWith("#")) {
                flush(box, paragraph);
                int level = (int) line.chars().takeWhile(c -> c == '#').count();
                TextFlow h = inline(line.substring(level).trim());
                h.getStyleClass().add("md-h" + Math.min(3, level));
                box.getChildren().add(h);
            } else if (line.matches("^\\s*[-*•]\\s+.*")) {
                flush(box, paragraph);
                TextFlow li = inline("•  " + line.replaceFirst("^\\s*[-*•]\\s+", ""));
                li.getStyleClass().add("md-li");
                box.getChildren().add(li);
            } else if (line.matches("^\\s*\\d+[.)]\\s+.*")) {
                flush(box, paragraph);
                TextFlow li = inline(line.trim());
                li.getStyleClass().add("md-li");
                box.getChildren().add(li);
            } else {
                paragraph.add(line);
            }
        }
        flush(box, paragraph);
        return box;
    }

    private static void flush(VBox box, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        TextFlow p = inline(String.join("\n", paragraph));
        p.getStyleClass().add("md-p");
        box.getChildren().add(p);
        paragraph.clear();
    }

    static TextFlow inline(String s) {
        TextFlow flow = new TextFlow();
        Matcher m = INLINE.matcher(s);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                flow.getChildren().add(text(s.substring(last, m.start()), "md-text"));
            }
            if (m.group(1) != null) {
                flow.getChildren().add(text(m.group(1), "md-bold"));
            } else if (m.group(2) != null) {
                flow.getChildren().add(text(m.group(2), "md-italic"));
            } else {
                flow.getChildren().add(text(m.group(3), "md-code"));
            }
            last = m.end();
        }
        if (last < s.length()) {
            flow.getChildren().add(text(s.substring(last), "md-text"));
        }
        return flow;
    }

    private static Text text(String s, String style) {
        Text t = new Text(s);
        t.getStyleClass().add(style);
        return t;
    }
}
