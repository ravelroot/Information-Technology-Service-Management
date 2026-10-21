package io.pyhoma.desktop.views.assets;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.asset.Supplier;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import javafx.scene.Node;

public class SuppliersView extends ListPage<Supplier> {

    @Override
    protected String titleText() {
        return I18n.t("nav.SUPPLIERS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("suppliers.subtitle");
    }

    @Override
    protected void columns(DataTable<Supplier> t) {
        t.col(I18n.t("field.name"), 260, Supplier::getName)
                .col(I18n.t("field.typeId"), 160, s -> App.lookups().dd(DropdownKind.SUPPLIER_TYPE, s.getTypeId()))
                .col(I18n.t("field.email"), 220, Supplier::getEmail)
                .col(I18n.t("field.phone"), 140, Supplier::getPhone)
                .col(I18n.t("field.city"), 140, Supplier::getCity)
                .col(I18n.t("field.website"), 200, Supplier::getWebsite);
    }

    @Override
    protected PageResult<Supplier> load(String text, int page, int size) {
        return App.services().suppliers().search(text, page, size);
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.SUPPLIER, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("suppliers.new"), "plus", () -> edit(new Supplier()))};
    }

    @Override
    protected void open(Supplier row) {
        edit(row);
    }

    private void edit(Supplier s) {
        boolean canEdit = App.can(Module.SUPPLIER, s.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(2).readOnly(!canEdit)
                .text("name", s::getName, s::setName, 2, true)
                .choice("typeId", () -> Option.of(App.lookups().dropdown(DropdownKind.SUPPLIER_TYPE)), s::getTypeId, s::setTypeId, true, false)
                .check("active", s::isActive, s::setActive)
                .text("email", s::getEmail, s::setEmail)
                .text("phone", s::getPhone, s::setPhone)
                .text("website", s::getWebsite, s::setWebsite)
                .text("city", s::getCity, s::setCity)
                .text("address", s::getAddress, s::setAddress, 2, false)
                .area("notes", s::getNotes, s::setNotes, 3, false);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(600);
        Dialogs.form(I18n.t(s.isNew() ? "suppliers.new" : "suppliers.edit"), f.node(), I18n.t("action.save"), () -> {
            if (!canEdit) {
                return true;
            }
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().suppliers().save(s);
                Toasts.success(I18n.t("toast.saved"));
                App.lookups().refreshAsync(this::reload);
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }
}
