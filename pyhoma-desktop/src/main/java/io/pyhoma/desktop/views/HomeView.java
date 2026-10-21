package io.pyhoma.desktop.views;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.MainWindow;
import io.pyhoma.desktop.shell.Route;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.views.itil.ItilDetailView;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.kb.KbArticle;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ItilSearch;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class HomeView extends View {

    private final VBox tickets = new VBox(2);
    private final VBox faq = new VBox(2);

    @Override
    public String title() {
        return I18n.t("nav.HOME");
    }

    @Override
    protected Node build() {
        Label hello = Ui.label(I18n.t("home.hello", App.session().fullName().split(" ")[0]), "home-hello");
        Label question = Ui.label(I18n.t("home.question"), "home-question");
        HBox cta = Ui.row(10);
        if (App.can(Module.TICKET, Right.CREATE)) {
            cta.getChildren().add(Ui.primary(I18n.t("home.openTicket"), "plus", () -> io.pyhoma.desktop.views.itil.ItilQuickForm.open(ItilType.TICKET,
                    io.pyhoma.domain.model.itil.ItilStatus.NEW, io.pyhoma.desktop.views.itil.ItilQuickForm::openCreated)));
        }
        if (App.can(Module.KNOWLEDGE_BASE, Right.READ)) {
            cta.getChildren().add(Ui.button(I18n.t("home.browseFaq"), "book-open", () -> App.nav().go(Route.KNOWLEDGE)));
        }
        VBox hero = new VBox(8, MainWindow.brand(15), hello, question, cta);
        hero.getStyleClass().add("home-hero");
        VBox ticketsPanel = Ui.panel(I18n.t("home.myTickets"), tickets, Ui.ghost(I18n.t("dash.seeAll"), null, () -> App.nav().go(Route.TICKETS)));
        VBox faqPanel = Ui.panel(I18n.t("home.faq"), faq);
        HBox panels = new HBox(16, ticketsPanel, faqPanel);
        HBox.setHgrow(ticketsPanel, Priority.ALWAYS);
        HBox.setHgrow(faqPanel, Priority.ALWAYS);
        ticketsPanel.setPrefWidth(600);
        faqPanel.setPrefWidth(420);
        VBox root = new VBox(18, hero, panels);
        root.getStyleClass().add("page");
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        return sp;
    }

    @Override
    public void onShow() {
        if (App.can(Module.TICKET, Right.READ)) {
            Async.run(() -> App.services().tickets().search(ItilSearch.create().view(ItilSearch.View.MINE_REQUESTED).page(0, 8)).items(),
                    this::renderTickets);
        }
        if (App.can(Module.KNOWLEDGE_BASE, Right.READ)) {
            Async.run(() -> App.services().knowledgeBase().popular(6), this::renderFaq);
        }
    }

    private void renderTickets(List<? extends ItilItem> items) {
        tickets.getChildren().clear();
        if (items.isEmpty()) {
            tickets.getChildren().add(new EmptyState("ticket", I18n.t("home.noTickets"), I18n.t("home.noTicketsHint"), null));
        }
        for (ItilItem t : items) {
            Label title = Ui.label(t.getTitle(), "queue-title");
            Label meta = Ui.label("#" + t.getId() + "  •  " + I18n.t("home.updated", I18n.relative(t.getLastActivityAt())), "muted");
            HBox row = new HBox(10, new VBox(1, title, meta), Ui.spacer(), Badge.status(t.getStatus()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("queue-row");
            row.setOnMouseClicked(e -> App.nav().open(new ItilDetailView(ItilType.TICKET, t.getId())));
            tickets.getChildren().add(row);
        }
    }

    private void renderFaq(List<KbArticle> articles) {
        faq.getChildren().clear();
        if (articles.isEmpty()) {
            faq.getChildren().add(new EmptyState("book-open", I18n.t("kb.empty"), null, null));
        }
        for (KbArticle a : articles) {
            HBox row = new HBox(10, Icons.of("book-open", 15), Ui.label(a.getTitle(), "queue-title"));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("queue-row");
            row.setOnMouseClicked(e -> App.nav().open(new KbArticleView(a.getId())));
            faq.getChildren().add(row);
        }
    }
}
