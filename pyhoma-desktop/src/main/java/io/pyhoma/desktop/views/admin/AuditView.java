package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.desktop.views.itil.ItilListViewHelper;
import io.pyhoma.domain.model.core.AuditEntry;
import io.pyhoma.domain.query.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;

public class AuditView extends ListPage<AuditEntry> {

    private final PickerField<String> type = PickerField.compact(() -> {
        List<Option<String>> list = new ArrayList<>();
        App.services().audit().itemTypes().forEach(t -> list.add(new Option<>(t, I18n.has("type." + t) ? I18n.t("type." + t) : t)));
        return list;
    }, null);
    private final PickerField<Long> user = PickerField.compact(ItilListViewHelper::users, null);
    private Map<Long, String> dropdowns;

    @Override
    protected String titleText() {
        return I18n.t("nav.AUDIT");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("audit.subtitle");
    }

    @Override
    protected Node[] filters() {
        type.onChange(v -> loadPage(0));
        user.onChange(v -> loadPage(0));
        return new Node[] {ItilListViewHelper.labeled("history.type", type), ItilListViewHelper.labeled("history.who", user)};
    }

    @Override
    protected void columns(DataTable<AuditEntry> t) {
        t.col(I18n.t("history.when"), 140, e -> I18n.dateTime(e.getOccurredAt()))
                .col(I18n.t("history.who"), 150, AuditEntry::getUserName)
                .col(I18n.t("history.type"), 120, e -> I18n.has("type." + e.getItemType()) ? I18n.t("type." + e.getItemType()) : e.getItemType())
                .col("#", 70, "num", AuditEntry::getItemId)
                .col(I18n.t("history.action"), 110, e -> I18n.e(e.getAction()))
                .col(I18n.t("history.field"), 140, e -> e.getFieldName() == null ? "" : I18n.field(e.getFieldName()))
                .col(I18n.t("history.old"), 180, e -> HistoryPanel.resolve(e.getFieldName(), e.getOldValue(), dropdowns))
                .col(I18n.t("history.new"), 180, e -> HistoryPanel.resolve(e.getFieldName(), e.getNewValue(), dropdowns));
        t.onOpen(e -> App.nav().openItem(e.getItemType(), e.getItemId()));
    }

    @Override
    protected PageResult<AuditEntry> load(String text, int page, int size) {
        dropdowns = App.services().lookups().allDropdownNames();
        return App.services().audit().search(type.getValue(), user.getValue(), text, null, null, page, size);
    }
}
