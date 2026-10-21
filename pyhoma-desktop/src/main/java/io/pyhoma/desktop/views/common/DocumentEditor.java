package io.pyhoma.desktop.views.common;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.domain.model.core.Document;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.support.ValidationException;
import java.io.File;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public final class DocumentEditor {

    private DocumentEditor() {
    }

    public static Document edit(Document doc) {
        Document[] saved = new Document[1];
        Form form = new Form(2)
                .text("name", doc::getName, doc::setName, 2, true)
                .choice("categoryId", () -> Option.of(App.lookups().dropdown(DropdownKind.DOCUMENT_CATEGORY)),
                        doc::getCategoryId, doc::setCategoryId, true, false)
                .text("uri", doc::getUri, doc::setUri, 2, true)
                .area("notes", doc::getNotes, doc::setNotes, 3, false);
        form.load();
        var browse = Ui.ghost(I18n.t("documents.browse"), "folder", () -> {
            FileChooser chooser = new FileChooser();
            File f = chooser.showOpenDialog(Dialogs.owner());
            if (f != null) {
                ((javafx.scene.control.TextField) form.box("uri").control()).setText(f.getAbsolutePath());
                var name = (javafx.scene.control.TextField) form.box("name").control();
                if (name.getText() == null || name.getText().isBlank()) {
                    name.setText(f.getName());
                }
            }
        });
        VBox content = new VBox(10, form.node(), browse);
        content.setPrefWidth(560);
        boolean ok = Dialogs.form(I18n.t(doc.isNew() ? "documents.new" : "documents.edit"), content, I18n.t("action.save"), () -> {
            if (!form.commit()) {
                return false;
            }
            try {
                saved[0] = App.services().documents().save(doc);
                return true;
            } catch (ValidationException e) {
                form.showErrors(e);
                return false;
            }
        });
        if (ok) {
            Toasts.success(I18n.t("toast.saved"));
        }
        return ok ? saved[0] : null;
    }
}
