package io.talos.desktop.views.itil;

import java.util.List;
import io.talos.desktop.ui.Option;
import javafx.scene.Node;

public final class ItilListViewHelper {

    private ItilListViewHelper() {
    }

    public static Node labeled(String key, Node picker) {
        return ItilListView.labeled(key, picker);
    }

    public static List<Option<Long>> technicians() {
        return ItilDetailView.technicianOptions();
    }

    public static List<Option<Long>> users() {
        return ItilDetailView.userOptions();
    }
}
