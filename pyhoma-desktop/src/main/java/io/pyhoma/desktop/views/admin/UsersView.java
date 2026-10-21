package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Avatar;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class UsersView extends ListPage<User> {

    @Override
    protected String titleText() {
        return I18n.t("nav.USERS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("users.subtitle");
    }

    @Override
    protected void columns(DataTable<User> t) {
        t.node(I18n.t("field.fullName"), 240, u -> {
            HBox h = new HBox(10, new Avatar(u.getFullName(), 26), new Label(u.getFullName()));
            h.setAlignment(Pos.CENTER_LEFT);
            return h;
        }, User::getFullName)
                .col(I18n.t("field.login"), 160, "mono", User::getLogin)
                .col(I18n.t("field.email"), 220, User::getEmail)
                .col(I18n.t("field.jobTitle"), 180, User::getJobTitle)
                .col(I18n.t("field.lastLoginAt"), 140, u -> I18n.relative(u.getLastLoginAt()))
                .node(I18n.t("field.active"), 100, u -> Badge.of(I18n.t(u.isActive() ? "common.active" : "common.inactive"),
                        u.isActive() ? "green" : "gray"), u -> u.isActive());
    }

    @Override
    protected PageResult<User> load(String text, int page, int size) {
        return App.services().users().search(text, null, null, page, size);
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.USER, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("users.new"), "user-plus", () -> App.nav().open(new UserDetailView(null)))};
    }

    @Override
    protected void open(User row) {
        App.nav().open(new UserDetailView(row.getId()));
    }
}
