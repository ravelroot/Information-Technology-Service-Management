package io.pyhoma.desktop.views.common;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.UserMessage;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.domain.model.core.Document;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.security.Right;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class DocumentsPanel extends VBox {

    private final DataTable<Document> table = new DataTable<>();
    private final String itemType;
    private final Long itemId;

    public DocumentsPanel(String itemType, Long itemId) {
        super(10);
        this.itemType = itemType;
        this.itemId = itemId;
        getStyleClass().add("tab-body");
        table.col(I18n.t("field.name"), 240, Document::getName)
                .col(I18n.t("field.categoryId"), 150, d -> App.lookups().dd(DropdownKind.DOCUMENT_CATEGORY, d.getCategoryId()))
                .col(I18n.t("field.uri"), 360, "mono", Document::getUri);
        table.placeholder(new EmptyState("paperclip", I18n.t("documents.empty"), I18n.t("documents.emptyHint"), null));
        table.onOpen(DocumentsPanel::openDocument);
        HBox actions = Ui.row(8, Ui.spacer());
        if (App.can(Module.DOCUMENT, Right.READ)) {
            actions.getChildren().add(Ui.button(I18n.t("documents.attach"), "paperclip", this::attach));
        }
        if (App.can(Module.DOCUMENT, Right.CREATE)) {
            actions.getChildren().add(Ui.button(I18n.t("documents.new"), "plus", this::createAndAttach));
        }
        actions.getChildren().add(Ui.button(I18n.t("documents.detach"), "unlink", this::detach, "ghost"));
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(actions, table);
    }

    public void load() {
        Async.run(() -> App.services().documents().documentsOf(itemType, itemId), table::setRows);
    }

    public static void openDocument(Document d) {
        String uri = d.getUri() == null ? "" : d.getUri().strip();
        io.pyhoma.domain.support.DocumentPolicy.Kind kind;
        try {
            kind = io.pyhoma.domain.support.DocumentPolicy.classify(uri);
        } catch (RuntimeException e) {
            Dialogs.error(e);
            return;
        }
        if (!Dialogs.confirm(I18n.t("documents.open"), I18n.t("documents.confirmOpen", uri), I18n.t("documents.open"), false)) {
            return;
        }
        try {
            if (kind == io.pyhoma.domain.support.DocumentPolicy.Kind.FILE) {
                File file = new File(uri);
                if (!file.isFile() || io.pyhoma.domain.support.DocumentPolicy.isBlockedFile(file.getCanonicalPath())) {
                    throw new UserMessage(I18n.t("documents.cannotOpen", uri));
                }
                Desktop.getDesktop().open(file);
            } else if (kind == io.pyhoma.domain.support.DocumentPolicy.Kind.MAIL) {
                Desktop.getDesktop().mail(new URI(uri));
            } else {
                Desktop.getDesktop().browse(new URI(uri));
            }
        } catch (Exception e) {
            Dialogs.error(new UserMessage(I18n.t("documents.cannotOpen", uri)));
        }
    }

    private void attach() {
        PickerField<Long> picker = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            App.services().documents().search(null, null, 0, 1000).items()
                    .forEach(d -> list.add(new Option<>(d.getId(), d.getName(), d.getUri(), null)));
            return list;
        }, false);
        picker.setPrefWidth(420);
        boolean ok = Dialogs.form(I18n.t("documents.attach"), picker, I18n.t("action.attach"), () -> picker.getValue() != null);
        if (ok) {
            Async.exec(() -> App.services().documents().attach(picker.getValue(), itemType, itemId), () -> {
                Toasts.success(I18n.t("documents.attached"));
                load();
            });
        }
    }

    private void createAndAttach() {
        Document d = DocumentEditor.edit(new Document());
        if (d != null) {
            Async.exec(() -> App.services().documents().attach(d.getId(), itemType, itemId), this::load);
        }
    }

    private void detach() {
        Document d = table.selected();
        if (d != null) {
            Async.exec(() -> App.services().documents().detach(d.getId(), itemType, itemId), this::load);
        }
    }
}
