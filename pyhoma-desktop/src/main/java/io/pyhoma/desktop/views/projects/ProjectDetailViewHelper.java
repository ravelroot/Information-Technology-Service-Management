package io.pyhoma.desktop.views.projects;

import io.pyhoma.desktop.ui.Option;
import io.pyhoma.domain.model.core.DropdownKind;
import java.util.List;

public final class ProjectDetailViewHelper {

    private ProjectDetailViewHelper() {
    }

    public static List<Option<Long>> colored(DropdownKind kind) {
        return ProjectDetailView.colored(kind);
    }

    public static List<Option<Long>> users() {
        return ProjectDetailView.users();
    }
}
