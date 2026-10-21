package io.pyhoma.desktop.views.assets;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.views.common.DocumentEditor;
import io.pyhoma.desktop.views.common.DocumentsPanel;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.core.Document;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;

public class DocumentsView extends ListPage<Document> {

    @Override
    protected String titleText() {
        return I18n.t("nav.DOCUMENTS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("documents.subtitle");
    }

    @Override
    protected void columns(DataTable<Document> t) {
        t.col(I18n.t("field.name"), 260, Document::getName)
                .col(I18n.t("field.categoryId"), 160, d -> App.lookups().dd(DropdownKind.DOCUMENT_CATEGORY, d.getCategoryId()))
                .col(I18n.t("field.uri"), 380, "mono", Document::getUri)
                .col(I18n.t("field.updatedAt"), 130, d -> I18n.dateTime(d.getUpdatedAt()));
    }

    @Override
    protected PageResult<Document> load(String text, int page, int size) {
        return App.services().documents().search(text, null, page, size);
    }

    @Override
    protected Node[] actions() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(Ui.button(I18n.t("documents.open"), "external-link", () -> {
            Document d = table.selected();
            if (d != null) {
                DocumentsPanel.openDocument(d);
            }
        }));
        if (App.can(Module.DOCUMENT, Right.PURGE)) {
            nodes.add(Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                Document d = table.selected();
                if (d != null && Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                    Async.exec(() -> App.services().documents().delete(d.getId()), this::reload);
                }
            }, "ghost"));
        }
        if (App.can(Module.DOCUMENT, Right.CREATE)) {
            nodes.add(Ui.primary(I18n.t("documents.new"), "plus", () -> {
                if (DocumentEditor.edit(new Document()) != null) {
                    reload();
                }
            }));
        }
        return nodes.toArray(Node[]::new);
    }

    @Override
    protected void open(Document row) {
        if (App.can(Module.DOCUMENT, Right.UPDATE)) {
            if (DocumentEditor.edit(row) != null) {
                reload();
            }
        } else {
            DocumentsPanel.openDocument(row);
        }
    }
}
