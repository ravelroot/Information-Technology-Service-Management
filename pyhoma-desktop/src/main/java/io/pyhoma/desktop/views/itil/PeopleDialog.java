package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.PeopleField;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.service.ItilPeople;
import javafx.scene.layout.VBox;

public final class PeopleDialog {

    private PeopleDialog() {
    }

    public static void open(ItilType type, Long id, String reference, Runnable after) {
        Async.run(() -> App.services().itil(type).people(id), people -> show(type, id, reference, people, after), Dialogs::error);
    }

    private static void show(ItilType type, Long id, String reference, ItilPeople people, Runnable after) {
        PeopleField assignees = new PeopleField(PeopleField::activeUsers, I18n.t("people.addAssignee"));
        PeopleField followers = new PeopleField(PeopleField::activeUsers, I18n.t("people.addFollower"));
        assignees.setValues(people.assignees());
        followers.setValues(people.followers());
        VBox body = new VBox(8,
                Ui.label(I18n.t("field.assignees"), "field-label"), assignees, Ui.muted(I18n.t("people.assigneesHint")),
                Ui.label(I18n.t("field.followers"), "field-label"), followers, Ui.muted(I18n.t("people.followersHint")));
        VBox.setMargin(body.getChildren().get(3), new javafx.geometry.Insets(10, 0, 0, 0));
        body.setPrefWidth(560);
        Dialogs.form(I18n.t("people.title", reference), body, I18n.t("action.save"), () -> {
            App.services().itil(type).setPeople(id, assignees.getValues(), followers.getValues());
            Toasts.success(I18n.t("people.saved"));
            after.run();
            return true;
        });
    }
}
