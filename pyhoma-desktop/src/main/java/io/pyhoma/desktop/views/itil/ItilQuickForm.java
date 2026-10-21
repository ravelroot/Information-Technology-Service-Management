package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PeopleField;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.Change;
import io.pyhoma.domain.model.itil.ChangeType;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ItilService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class ItilQuickForm {

    private final ItilStatus column;
    private final Consumer<ItilItem> onCreated;
    private final PickerField<ItilType> board;
    private final PickerField<Object> kind;
    private final PickerField<Long> category;
    private final PeopleField assignees;
    private final PeopleField followers;
    private final Label where = Ui.muted("");
    private final String[] title = {null};
    private final String[] content = {null};
    private boolean openFull;

    private ItilQuickForm(ItilType type, ItilStatus column, Consumer<ItilItem> onCreated) {
        this.column = column;
        this.onCreated = onCreated;
        board = new PickerField<>(ItilQuickForm::boards, false);
        board.setValue(type);
        kind = new PickerField<>(this::kinds, false);
        category = new PickerField<>(() -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)), true);
        assignees = new PeopleField(PeopleField::activeUsers, I18n.t("people.addAssignee"));
        followers = new PeopleField(PeopleField::activeUsers, I18n.t("people.addFollower"));
    }

    public static boolean canCreate(ItilType type) {
        return App.can(module(type), Right.CREATE);
    }

    public static boolean canCreateAny() {
        return !boards().isEmpty();
    }

    public static void open(ItilType type, ItilStatus column, Consumer<ItilItem> onCreated) {
        ItilType start = canCreate(type) ? type : boards().stream().map(Option::value).findFirst().orElse(null);
        if (start == null) {
            return;
        }
        new ItilQuickForm(start, column, onCreated).show();
    }

    public static void openCreated(ItilItem created) {
        App.nav().open(new ItilDetailView(created.itilType(), created.getId()));
    }

    static Module module(ItilType type) {
        return switch (type) {
            case TICKET -> Module.TICKET;
            case PROBLEM -> Module.PROBLEM;
            case CHANGE -> Module.CHANGE;
        };
    }

    private static List<Option<ItilType>> boards() {
        List<Option<ItilType>> list = new ArrayList<>();
        for (ItilType t : ItilType.values()) {
            if (canCreate(t)) {
                list.add(new Option<>(t, I18n.t("kanban.board." + t.name())));
            }
        }
        return list;
    }

    private List<Option<Object>> kinds() {
        List<Option<Object>> list = new ArrayList<>();
        ItilType t = board.getValue();
        if (t == ItilType.TICKET) {
            for (TicketType v : TicketType.values()) {
                list.add(new Option<>(v, I18n.e(v)));
            }
        } else if (t == ItilType.CHANGE) {
            for (ChangeType v : ChangeType.values()) {
                list.add(new Option<>(v, I18n.e(v)));
            }
        }
        return list;
    }

    private ItilStatus target() {
        ItilType t = board.getValue();
        if (column == null || column == ItilStatus.NEW || column.isTerminal() || !ItilStatus.workflow(t).contains(column)
                || !App.can(module(t), Right.UPDATE)) {
            return ItilStatus.NEW;
        }
        return column;
    }

    private void refreshBoard() {
        kind.reload();
        List<Option<Object>> k = kinds();
        kind.setValue(k.isEmpty() ? null : k.get(k.size() > 1 && board.getValue() == ItilType.CHANGE ? 1 : 0).value());
        kind.setDisable(k.isEmpty());
        where.setText(I18n.t("kanban.createIn", I18n.e(target())));
    }

    private void show() {
        Form f = new Form(2)
                .text("title", () -> title[0], v -> title[0] = v, 2, true)
                .custom("board", board, 1)
                .custom("itilKind", kind, 1);
        HBox catRow = new HBox(8, category);
        HBox.setHgrow(category, Priority.ALWAYS);
        category.setMaxWidth(Double.MAX_VALUE);
        catRow.setAlignment(Pos.CENTER_LEFT);
        if (CategoryDialog.allowed()) {
            catRow.getChildren().add(CategoryDialog.button(() -> category.reload()));
        }
        f.custom("categoryId", catRow, 2)
                .custom("assignees", assignees, 2)
                .custom("followers", followers, 2)
                .area("content", () -> content[0], v -> content[0] = v, 5, false);
        f.load();
        board.onChange(v -> refreshBoard());
        refreshBoard();
        Hyperlink full = new Hyperlink(I18n.t("itil.quick.full"));
        full.setOnAction(e -> {
            openFull = true;
            full.getScene().getWindow().hide();
        });
        HBox foot = new HBox(8, where, Ui.spacer(), full);
        foot.setAlignment(Pos.CENTER_LEFT);
        VBox body = new VBox(14, f.node(), foot);
        body.setPrefWidth(620);
        ItilType[] created = {null};
        Dialogs.form(I18n.t("itil.quick.title"), body, I18n.t("action.create"), () -> {
            if (!f.commit()) {
                return false;
            }
            ItilType t = board.getValue();
            @SuppressWarnings("unchecked")
            ItilService<ItilItem> service = (ItilService<ItilItem>) App.services().itil(t);
            ItilItem item = service.draft();
            item.setTitle(title[0]);
            item.setContent(content[0]);
            item.setCategoryId(category.getValue());
            if (item instanceof Ticket tk && kind.getValue() instanceof TicketType tt) {
                tk.setTicketType(tt);
            }
            if (item instanceof Change ch && kind.getValue() instanceof ChangeType ct) {
                ch.setChangeType(ct);
            }
            try {
                ItilItem saved = service.createWithPeople(item, target(), assignees.getValues(), followers.getValues());
                created[0] = t;
                Toasts.success(I18n.t("itil.created", saved.reference()));
                onCreated.accept(saved);
                return true;
            } catch (io.pyhoma.domain.support.ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
        if (openFull && created[0] == null) {
            App.nav().open(ItilDetailView.create(board.getValue()));
        }
    }
}
