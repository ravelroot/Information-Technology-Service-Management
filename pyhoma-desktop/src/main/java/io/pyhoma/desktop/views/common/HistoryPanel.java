package io.pyhoma.desktop.views.common;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.AuditEntry;
import java.util.Map;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class HistoryPanel extends VBox {

    private final DataTable<AuditEntry> table = new DataTable<>();
    private final String itemType;
    private final Long itemId;
    private Map<Long, String> dropdowns;

    public HistoryPanel(String itemType, Long itemId) {
        this.itemType = itemType;
        this.itemId = itemId;
        getStyleClass().add("tab-body");
        table.col(I18n.t("history.when"), 140, e -> I18n.dateTime(e.getOccurredAt()))
                .col(I18n.t("history.who"), 150, AuditEntry::getUserName)
                .col(I18n.t("history.action"), 110, e -> I18n.e(e.getAction()))
                .col(I18n.t("history.field"), 150, e -> e.getFieldName() == null ? "" : I18n.field(e.getFieldName()))
                .col(I18n.t("history.old"), 200, e -> value(e, e.getOldValue()))
                .col(I18n.t("history.new"), 200, e -> value(e, e.getNewValue()));
        table.placeholder(new EmptyState("history", I18n.t("history.empty"), null, null));
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    public void load() {
        Async.run(() -> {
            dropdowns = App.services().lookups().allDropdownNames();
            return App.services().audit().history(itemType, itemId, 0, 500).items();
        }, table::setRows);
    }

    public static String resolve(String field, String raw, Map<Long, String> dropdowns) {
        if (raw == null) {
            return "";
        }
        if (field == null) {
            return raw;
        }
        try {
            if (field.endsWith("Id") && raw.matches("\\d+")) {
                Long id = Long.valueOf(raw);
                return switch (field) {
                    case "requesterId", "assigneeId", "userId", "techUserId", "managerId", "authorId", "technicianId",
                         "approverId" -> App.lookups().user(id);
                    case "assigneeGroupId", "groupId", "techGroupId" -> App.lookups().group(id);
                    case "orgUnitId", "defaultOrgUnitId", "parentId" -> App.lookups().orgUnit(id);
                    case "supplierId" -> App.lookups().supplier(id);
                    case "slaTtoId", "slaTtrId" -> io.pyhoma.desktop.ui.LookupCache.name(App.lookups().slas(), id);
                    default -> dropdowns != null && dropdowns.containsKey(id) ? dropdowns.get(id) : raw;
                };
            }
            if (field.equals("priority") || field.equals("urgency") || field.equals("impact")) {
                return I18n.t((field.equals("priority") ? "priority." : "level.") + raw);
            }
            for (String enumType : new String[] {"ItilStatus", "TicketType", "ChangeType", "RiskLevel", "ApprovalStatus",
                "AssetType", "ProjectTaskStatus", "TaskStatus", "SolutionStatus", "RenewalType"}) {
                if (I18n.has("enum." + enumType + "." + raw)) {
                    return I18n.t("enum." + enumType + "." + raw);
                }
            }
            if ("true".equals(raw) || "false".equals(raw)) {
                return I18n.t("common." + ("true".equals(raw) ? "yes" : "no"));
            }
        } catch (RuntimeException ignored) {
            return raw;
        }
        return raw;
    }

    private String value(AuditEntry e, String raw) {
        if (e.getAction() == AuditAction.LOGIN || e.getAction() == AuditAction.LOGIN_FAILED) {
            return raw == null ? "" : raw;
        }
        return resolve(e.getFieldName(), raw, dropdowns);
    }
}
