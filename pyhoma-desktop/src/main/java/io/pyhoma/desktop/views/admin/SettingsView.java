package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.StartupFlow;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Theme;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.views.setup.DatabaseForm;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.SettingKeys;
import io.pyhoma.domain.service.SettingsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsView extends View {

    private final VBox content = new VBox(16);

    @Override
    public String title() {
        return I18n.t("nav.SETTINGS");
    }

    @Override
    protected Node build() {
        PageHeader header = new PageHeader(I18n.t("nav.SETTINGS"), I18n.t("settings.subtitle"));
        VBox root = new VBox(16, header, content);
        root.getStyleClass().add("page");
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        return sp;
    }

    private static final class Values {
        String organization;
        Integer autoClose;
        boolean satisfaction;
        Integer slaWarning;
        Integer alertDays;
        Integer retention;
        Integer maxFailed;
        Integer lockMinutes;
        Integer idleMinutes;
        String tagPrefix;
        String theme;
        String language;
    }

    @Override
    public void onShow() {
        SettingsService s = App.services().settings();
        Values v = new Values();
        v.organization = s.get(SettingKeys.ORGANIZATION_NAME, "");
        v.autoClose = s.getInt(SettingKeys.TICKET_AUTO_CLOSE_DAYS, 7);
        v.satisfaction = s.getBoolean(SettingKeys.TICKET_SATISFACTION, true);
        v.slaWarning = s.getInt(SettingKeys.SLA_WARNING_PERCENT, 25);
        v.alertDays = s.getInt(SettingKeys.EXPIRATION_ALERT_DAYS, 30);
        v.retention = s.getInt(SettingKeys.NOTIFICATION_RETENTION_DAYS, 90);
        v.maxFailed = s.getInt(SettingKeys.MAX_FAILED_LOGINS, 5);
        v.lockMinutes = s.getInt(SettingKeys.LOCK_MINUTES, 15);
        v.idleMinutes = s.getInt(SettingKeys.SESSION_IDLE_MINUTES, 15);
        v.tagPrefix = s.get(SettingKeys.ASSET_TAG_PREFIX, "PYH-");
        v.theme = Theme.mode().name();
        v.language = I18n.languageTag();
        boolean edit = App.can(Module.SETTINGS, Right.UPDATE);

        Form general = new Form(3).readOnly(!edit)
                .text("organizationName", () -> v.organization, x -> v.organization = x, 3, true)
                .integer("autoCloseDays", () -> v.autoClose, x -> v.autoClose = x)
                .check("satisfactionEnabled", () -> v.satisfaction, x -> v.satisfaction = x)
                .integer("slaWarningPercent", () -> v.slaWarning, x -> v.slaWarning = x)
                .integer("expirationAlertDays", () -> v.alertDays, x -> v.alertDays = x)
                .integer("notificationRetentionDays", () -> v.retention, x -> v.retention = x)
                .text("assetTagPrefix", () -> v.tagPrefix, x -> v.tagPrefix = x)
                .integer("maxFailedLogins", () -> v.maxFailed, x -> v.maxFailed = x)
                .integer("lockMinutes", () -> v.lockMinutes, x -> v.lockMinutes = x)
                .integer("sessionIdleMinutes", () -> v.idleMinutes, x -> v.idleMinutes = x);
        general.load();
        HBox generalActions = Ui.row(8, Ui.spacer());
        if (edit) {
            generalActions.getChildren().add(Ui.primary(I18n.t("action.save"), "save", () -> {
                if (!general.commit()) {
                    return;
                }
                Map<String, String> values = new LinkedHashMap<>();
                values.put(SettingKeys.ORGANIZATION_NAME, v.organization);
                values.put(SettingKeys.TICKET_AUTO_CLOSE_DAYS, String.valueOf(v.autoClose));
                values.put(SettingKeys.TICKET_SATISFACTION, String.valueOf(v.satisfaction));
                values.put(SettingKeys.SLA_WARNING_PERCENT, String.valueOf(v.slaWarning));
                values.put(SettingKeys.EXPIRATION_ALERT_DAYS, String.valueOf(v.alertDays));
                values.put(SettingKeys.NOTIFICATION_RETENTION_DAYS, String.valueOf(v.retention));
                values.put(SettingKeys.ASSET_TAG_PREFIX, v.tagPrefix);
                values.put(SettingKeys.MAX_FAILED_LOGINS, String.valueOf(v.maxFailed));
                values.put(SettingKeys.LOCK_MINUTES, String.valueOf(v.lockMinutes));
                values.put(SettingKeys.SESSION_IDLE_MINUTES, String.valueOf(v.idleMinutes));
                Async.exec(() -> App.services().store().inTransaction(() -> values.forEach(s::set)),
                        () -> Toasts.success(I18n.t("toast.saved")));
            }));
        }

        Form look = new Form(3)
                .choice("theme", () -> List.of(new Option<>("LIGHT", I18n.t("settings.theme.light")),
                        new Option<>("DARK", I18n.t("settings.theme.dark"))), () -> v.theme, x -> v.theme = x, false, true)
                .choice("language", () -> List.of(new Option<>("pt-BR", "Português (Brasil)"), new Option<>("en", "English")),
                        () -> v.language, x -> v.language = x, false, true);
        look.load();
        HBox lookActions = Ui.row(8, Ui.muted(I18n.t("settings.languageHint")), Ui.spacer(), Ui.primary(I18n.t("action.apply"), "check", () -> {
            look.commit();
            Theme.setMode(Theme.Mode.valueOf(v.theme));
            App.config().set("ui.theme", v.theme);
            boolean langChanged = !v.language.equals(I18n.languageTag());
            App.config().set("ui.language", v.language);
            App.config().save();
            if (langChanged) {
                I18n.setLanguage(v.language);
                StartupFlow.instance().reopenMainWindow();
            } else {
                Toasts.success(I18n.t("toast.saved"));
            }
        }));

        VBox dbInfo = new VBox(10,
                Ui.fact(I18n.t("settings.db.current"), App.demo() ? I18n.t("demo.connection")
                        : App.config().database().map(d -> d.safeDescription()).orElse("—")),
                Ui.fact(I18n.t("settings.db.server"), (App.demo() ? I18n.t("demo.footer") : App.services().store().describe())),
                Ui.fact(I18n.t("settings.db.config"), App.config().file().toString()),
                Ui.muted(I18n.t("settings.db.hint")));
        HBox dbActions = Ui.row(8, Ui.spacer(), App.demo()
                ? Ui.primary(I18n.t("demo.setup"), "database", () -> StartupFlow.instance().leaveDemo(true))
                : Ui.button(I18n.t("settings.db.change"), "database", this::changeDatabase));

        content.getChildren().setAll(
                Ui.panel(I18n.t("settings.general"), new VBox(14, general.node(), generalActions)),
                Ui.panel(I18n.t("settings.appearance"), new VBox(14, look.node(), lookActions)),
                Ui.panel(I18n.t("settings.database"), new VBox(14, dbInfo, dbActions)));
    }

    private void changeDatabase() {
        DatabaseForm form = new DatabaseForm(StartupFlow.instance().provider(), App.config().database().orElse(null));
        form.setPrefWidth(660);
        Dialogs.form(I18n.t("settings.db.change"), form, I18n.t("settings.db.saveRestart"), () -> {
            if (!form.validateFields()) {
                return false;
            }
            App.config().setDatabase(form.settings());
            App.config().save();
            StartupFlow.instance().restart();
            return true;
        });
    }
}
