package io.talos.desktop.views.admin;

import io.talos.desktop.App;
import io.talos.desktop.shell.View;
import io.talos.desktop.ui.Async;
import io.talos.desktop.ui.Dialogs;
import io.talos.desktop.ui.I18n;
import io.talos.desktop.ui.Icons;
import io.talos.desktop.ui.Toasts;
import io.talos.desktop.ui.Ui;
import io.talos.desktop.ui.components.Badge;
import io.talos.desktop.ui.components.EmptyState;
import io.talos.desktop.ui.components.PageHeader;
import io.talos.domain.model.core.Module;
import io.talos.domain.security.Right;
import io.talos.domain.service.IntegrationService.Delivery;
import io.talos.domain.service.IntegrationService.Event;
import io.talos.domain.service.IntegrationService.Kind;
import io.talos.domain.service.IntegrationService.Webhook;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Administração > Integrações: webhooks de saída e exportação de agenda. */
public class IntegrationsView extends View {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final VBox hooks = new VBox(10);
    private final VBox deliveries = new VBox();
    private boolean canEdit;

    @Override
    public String title() {
        return I18n.t("nav.INTEGRATIONS");
    }

    @Override
    protected Node build() {
        canEdit = App.can(Module.SETTINGS, Right.UPDATE);
        Button add = Ui.primary(I18n.t("integrations.add"), "plus", () -> edit(Webhook.blank()));
        add.setDisable(!canEdit);
        PageHeader header = new PageHeader(I18n.t("nav.INTEGRATIONS"), I18n.t("integrations.subtitle")).actions(add);

        Label calloutText = new Label(I18n.t("integrations.callout"));
        calloutText.setWrapText(true);
        HBox callout = new HBox(10, Icons.of("webhook", 18), calloutText);
        callout.getStyleClass().add("callout");
        callout.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(calloutText, Priority.ALWAYS);

        VBox hooksPanel = Ui.panel(I18n.t("integrations.webhooks"), hooks);

        Button refresh = Ui.ghost(I18n.t("action.refresh"), "refresh-cw", this::renderDeliveries);
        VBox deliveriesPanel = Ui.panel(I18n.t("integrations.deliveries"), deliveries, refresh);

        Label icsText = new Label(I18n.t("integrations.icsText"));
        icsText.setWrapText(true);
        Button ics = Ui.button(I18n.t("ics.export"), "calendar-plus", () -> App.nav().go(io.talos.desktop.shell.Route.PLANNING));
        VBox icsBox = new VBox(10, icsText, ics);
        VBox icsPanel = Ui.panel(I18n.t("integrations.calendar"), icsBox);

        GridPane lower = new GridPane();
        lower.setHgap(16);
        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
        c1.setPercentWidth(64);
        javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints();
        c2.setPercentWidth(36);
        lower.getColumnConstraints().addAll(c1, c2);
        lower.add(deliveriesPanel, 0, 0);
        lower.add(icsPanel, 1, 0);
        javafx.scene.layout.GridPane.setValignment(icsPanel, javafx.geometry.VPos.TOP);

        VBox root = new VBox(16, header, callout, hooksPanel, lower);
        root.getStyleClass().add("page");
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().integrations().webhooks(), this::renderHooks);
        renderDeliveries();
    }

    private void renderHooks(List<Webhook> list) {
        hooks.getChildren().clear();
        if (list.isEmpty()) {
            FlowPane quick = new FlowPane(8, 8);
            quick.setAlignment(Pos.CENTER);
            for (Kind k : Kind.values()) {
                Button b = Ui.button(I18n.e(k), icon(k), () -> edit(new Webhook(null, I18n.e(k), k, "", "", true,
                        EnumSet.of(Event.ITEM_CREATED, Event.ITEM_SOLVED))));
                b.setDisable(!canEdit);
                quick.getChildren().add(b);
            }
            hooks.getChildren().add(new EmptyState("plug", I18n.t("integrations.empty"), I18n.t("integrations.emptyHint"), quick));
            return;
        }
        for (Webhook w : list) {
            hooks.getChildren().add(card(w));
        }
    }

    private Node card(Webhook w) {
        StackPane logo = new StackPane(Icons.of(icon(w.kind()), 18));
        logo.getStyleClass().addAll("integration-logo", "kind-" + w.kind().name().toLowerCase(java.util.Locale.ROOT));

        Label name = Ui.label(w.name(), "integration-name");
        Label kind = Ui.label(I18n.e(w.kind()), "muted");
        HBox titleRow = new HBox(8, name, kind);
        titleRow.setAlignment(Pos.BASELINE_LEFT);
        Label url = Ui.label(mask(w.url()), "integration-url");
        Label events = Ui.label(w.events().stream().map(I18n::e).collect(Collectors.joining("  ·  ")), "integration-events");
        events.setWrapText(true);
        VBox texts = new VBox(3, titleRow, url, events);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Badge state = w.enabled() ? Badge.of(I18n.t("integrations.active"), "green") : Badge.of(I18n.t("integrations.paused"), "gray");
        state.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Button test = Ui.button(I18n.t("integrations.test"), "send", () -> test(w));
        Button toggle = Ui.iconButton("power", I18n.t(w.enabled() ? "integrations.pause" : "integrations.resume"),
                () -> Async.exec(() -> App.services().integrations().setEnabled(w.id(), !w.enabled()), this::onShow));
        Button editBtn = Ui.iconButton("pencil", I18n.t("action.edit"), () -> edit(w));
        Button delete = Ui.iconButton("trash-2", I18n.t("action.delete"), () -> {
            if (Dialogs.confirm(I18n.t("integrations.deleteTitle"), I18n.t("integrations.deleteText", w.name()),
                    I18n.t("action.delete"), true)) {
                Async.exec(() -> App.services().integrations().delete(w.id()), this::onShow);
            }
        });
        for (Button b : List.of(test, toggle, editBtn, delete)) {
            b.setDisable(!canEdit);
        }
        HBox row = new HBox(14, logo, texts, state, test, toggle, editBtn, delete);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("integration-card");
        return row;
    }

    private void renderDeliveries() {
        deliveries.getChildren().clear();
        List<Delivery> list = App.services().integrations().deliveries();
        if (list.isEmpty()) {
            deliveries.getChildren().add(new EmptyState("activity", I18n.t("integrations.noDeliveries"), null, null));
            return;
        }
        for (Delivery d : list.stream().limit(12).toList()) {
            Label when = Ui.label(d.at().format(TIME), "delivery-time");
            when.setMinWidth(110);
            Label hook = Ui.label(d.webhookName(), "cell-strong");
            hook.setMinWidth(140);
            Label event = Ui.label(I18n.e(d.event()), "muted");
            Label detail = Ui.label(d.ok() ? "HTTP " + d.httpStatus() : d.detail(), "muted");
            detail.setMaxWidth(260);
            Badge badge = d.ok() ? Badge.of(I18n.t("integrations.delivered"), "green") : Badge.of(I18n.t("integrations.failed"), "red");
            badge.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            HBox row = new HBox(10, when, hook, event, Ui.spacer(), detail, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("delivery-row");
            deliveries.getChildren().add(row);
        }
    }

    private void test(Webhook w) {
        Toasts.info(I18n.t("integrations.testing", w.name()));
        App.services().integrations().test(w).whenComplete((d, e) -> javafx.application.Platform.runLater(() -> {
            if (e != null) {
                Dialogs.error(e);
            } else if (d.ok()) {
                Toasts.success(I18n.t("integrations.testOk", w.name(), d.httpStatus()));
            } else {
                Toasts.warning(I18n.t("integrations.testFailed", w.name(), d.detail()));
            }
            renderDeliveries();
        }));
    }

    private void edit(Webhook original) {
        TextField name = new TextField(original.name());
        ComboBox<Kind> kind = new ComboBox<>();
        kind.getItems().setAll(Kind.values());
        kind.setValue(original.kind());
        kind.setCellFactory(lv -> kindCell());
        kind.setButtonCell(kindCell());
        kind.setMaxWidth(Double.MAX_VALUE);
        TextField url = new TextField(original.url());
        url.setPromptText("https://");
        url.getStyleClass().add("mono");
        PasswordField secret = new PasswordField();
        secret.setText(original.secret());
        secret.setPromptText(I18n.t("integrations.secretPrompt"));
        Label urlHint = Ui.label("", "muted");
        urlHint.setWrapText(true);
        Runnable hint = () -> {
            urlHint.setText(I18n.t("integrations.hint." + kind.getValue().name()));
            secret.setDisable(kind.getValue() != Kind.GENERIC);
        };
        kind.valueProperty().addListener((o, a, b) -> hint.run());
        hint.run();
        CheckBox enabled = new CheckBox(I18n.t("integrations.enabled"));
        enabled.setSelected(original.enabled());

        Map<Event, CheckBox> checks = new EnumMap<>(Event.class);
        VBox eventsBox = new VBox(6);
        for (Event e : Event.selectable()) {
            CheckBox cb = new CheckBox(I18n.e(e));
            cb.setSelected(original.events().contains(e));
            checks.put(e, cb);
            eventsBox.getChildren().add(cb);
        }

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(10);
        int r = 0;
        g.addRow(r++, Ui.label(I18n.t("integrations.name"), "field-label"), name);
        g.addRow(r++, Ui.label(I18n.t("integrations.kind"), "field-label"), kind);
        g.addRow(r++, Ui.label(I18n.t("integrations.url"), "field-label"), url);
        g.add(urlHint, 1, r++);
        g.addRow(r++, Ui.label(I18n.t("integrations.secret"), "field-label"), secret);
        g.addRow(r++, Ui.label(I18n.t("integrations.events"), "field-label"), eventsBox);
        g.add(enabled, 1, r);
        javafx.scene.layout.ColumnConstraints lc = new javafx.scene.layout.ColumnConstraints();
        lc.setMinWidth(120);
        javafx.scene.layout.ColumnConstraints vc = new javafx.scene.layout.ColumnConstraints();
        vc.setPrefWidth(440);
        vc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(lc, vc);

        boolean saved = Dialogs.form(I18n.t(original.id() == null ? "integrations.new" : "integrations.edit"), g,
                I18n.t("action.save"), () -> {
                    Set<Event> events = EnumSet.noneOf(Event.class);
                    checks.forEach((e, cb) -> {
                        if (cb.isSelected()) {
                            events.add(e);
                        }
                    });
                    App.services().integrations().save(new Webhook(original.id(), name.getText(), kind.getValue(),
                            url.getText(), kind.getValue() == Kind.GENERIC ? secret.getText() : "", enabled.isSelected(), events));
                    return true;
                });
        if (saved) {
            Toasts.success(I18n.t("integrations.saved"));
            onShow();
        }
    }

    private static ListCell<Kind> kindCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Kind k, boolean empty) {
                super.updateItem(k, empty);
                setText(empty || k == null ? null : I18n.e(k));
                setGraphic(empty || k == null ? null : Icons.of(icon(k), 14));
            }
        };
    }

    static String icon(Kind k) {
        return switch (k) {
            case SLACK -> "hash";
            case TEAMS -> "users-round";
            case DISCORD -> "message-circle";
            case GOOGLE_CHAT -> "message-square";
            case GENERIC -> "braces";
        };
    }

    /** Mostra só o domínio e o final do endereço: a URL de um webhook funciona como senha. */
    static String mask(String url) {
        try {
            URI u = URI.create(url);
            String tail = url.length() > 4 ? url.substring(url.length() - 4) : "";
            return u.getScheme() + "://" + u.getHost() + "/••••" + tail;
        } catch (RuntimeException e) {
            return "••••";
        }
    }
}
