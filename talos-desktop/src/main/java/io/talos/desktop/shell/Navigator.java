package io.talos.desktop.shell;

import io.talos.desktop.views.DashboardView;
import io.talos.desktop.views.HomeView;
import io.talos.desktop.views.KnowledgeBaseView;
import io.talos.desktop.views.KbArticleView;
import io.talos.desktop.views.ReportsView;
import io.talos.desktop.views.admin.AuditView;
import io.talos.desktop.views.admin.DropdownsView;
import io.talos.desktop.views.admin.GroupsView;
import io.talos.desktop.views.admin.OrgUnitsView;
import io.talos.desktop.views.admin.ProfilesView;
import io.talos.desktop.views.admin.RulesView;
import io.talos.desktop.views.admin.SettingsView;
import io.talos.desktop.views.admin.SlaView;
import io.talos.desktop.views.admin.UserDetailView;
import io.talos.desktop.views.admin.UsersView;
import io.talos.desktop.views.assets.AssetDetailView;
import io.talos.desktop.views.assets.AssetsView;
import io.talos.desktop.views.assets.ContractDetailView;
import io.talos.desktop.views.assets.ContractsView;
import io.talos.desktop.views.assets.DocumentsView;
import io.talos.desktop.views.assets.SoftwareDetailView;
import io.talos.desktop.views.assets.SoftwareView;
import io.talos.desktop.views.assets.SuppliersView;
import io.talos.desktop.views.itil.ItilDetailView;
import io.talos.desktop.views.itil.ItilListView;
import io.talos.desktop.views.itil.KanbanView;
import io.talos.desktop.views.itil.PlanningView;
import io.talos.desktop.views.itil.RecurringTicketsView;
import io.talos.desktop.views.projects.PortfolioView;
import io.talos.desktop.views.projects.ProjectDetailView;
import io.talos.desktop.views.projects.ProjectsView;
import io.talos.domain.model.itil.ItilType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.layout.StackPane;

public final class Navigator {

    private final StackPane content;
    private final Consumer<Route> onRoute;
    private final Consumer<String> onTitle;
    private final Map<Route, View> cache = new EnumMap<>(Route.class);
    private final Deque<View> stack = new ArrayDeque<>();
    private Route current;
    private View currentView;

    public Navigator(StackPane content, Consumer<Route> onRoute, Consumer<String> onTitle) {
        this.content = content;
        this.onRoute = onRoute;
        this.onTitle = onTitle;
    }

    public Route current() {
        return current;
    }

    public void go(Route route) {
        stack.clear();
        current = route;
        View v = cache.computeIfAbsent(route, Navigator::create);
        display(v);
        onRoute.accept(route);
    }

    public void open(View view) {
        if (currentView != null) {
            stack.push(currentView);
        }
        display(view);
    }

    public void back() {
        if (stack.isEmpty()) {
            if (current != null) {
                go(current);
            }
            return;
        }
        display(stack.pop());
    }

    public void refresh() {
        if (currentView != null) {
            currentView.onShow();
        }
    }

    public void reset() {
        cache.clear();
        stack.clear();
        currentView = null;
    }

    private void display(View v) {
        currentView = v;
        content.getChildren().setAll(v.root());
        io.talos.desktop.ui.Motion.pageIn(v.root());
        onTitle.accept(v.title());
        javafx.animation.PauseTransition settle = new javafx.animation.PauseTransition(javafx.util.Duration.millis(
                io.talos.desktop.ui.Motion.enabled() ? 120 : 1));
        settle.setOnFinished(e -> {
            if (currentView == v) {
                v.onShow();
            }
        });
        settle.play();
    }

    public void openItem(String type, Long id) {
        if (type == null) {
            return;
        }
        if (id == null) {
            switch (type) {
                case "Asset" -> go(Route.ASSETS);
                case "Contract" -> go(Route.CONTRACTS);
                case "Software" -> go(Route.SOFTWARE);
                default -> { }
            }
            return;
        }
        switch (type) {
            case "Ticket" -> open(new ItilDetailView(ItilType.TICKET, id));
            case "Problem" -> open(new ItilDetailView(ItilType.PROBLEM, id));
            case "Change" -> open(new ItilDetailView(ItilType.CHANGE, id));
            case "Asset" -> open(new AssetDetailView(id));
            case "Software" -> open(new SoftwareDetailView(id));
            case "Contract" -> open(new ContractDetailView(id));
            case "Project" -> open(new ProjectDetailView(id));
            case "KbArticle" -> open(new KbArticleView(id));
            case "User" -> open(new UserDetailView(id));
            case "Supplier" -> go(Route.SUPPLIERS);
            default -> { }
        }
    }

    private static View create(Route r) {
        return switch (r) {
            case HOME -> new HomeView();
            case DASHBOARD -> new DashboardView();
            case TICKETS -> new ItilListView(ItilType.TICKET);
            case PROBLEMS -> new ItilListView(ItilType.PROBLEM);
            case CHANGES -> new ItilListView(ItilType.CHANGE);
            case KANBAN -> new KanbanView();
            case PLANNING -> new PlanningView();
            case RECURRING -> new RecurringTicketsView();
            case PROJECTS -> new ProjectsView();
            case PORTFOLIO -> new PortfolioView();
            case ASSETS -> new AssetsView();
            case SOFTWARE -> new SoftwareView();
            case CONTRACTS -> new ContractsView();
            case SUPPLIERS -> new SuppliersView();
            case DOCUMENTS -> new DocumentsView();
            case KNOWLEDGE -> new KnowledgeBaseView();
            case REPORTS -> new ReportsView();
            case USERS -> new UsersView();
            case GROUPS -> new GroupsView();
            case PROFILES -> new ProfilesView();
            case ORG_UNITS -> new OrgUnitsView();
            case DROPDOWNS -> new DropdownsView();
            case RULES -> new RulesView();
            case SLAS -> new SlaView();
            case INTEGRATIONS -> new io.talos.desktop.views.admin.IntegrationsView();
            case AUDIT -> new AuditView();
            case SETTINGS -> new SettingsView();
        };
    }
}
