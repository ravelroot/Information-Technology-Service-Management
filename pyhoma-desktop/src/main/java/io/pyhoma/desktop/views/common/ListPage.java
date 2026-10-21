package io.pyhoma.desktop.views.common;

import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.Pager;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.support.Csv;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

@SuppressWarnings("this-escape")
public abstract class ListPage<T> extends View {

    protected final DataTable<T> table = new DataTable<>();
    protected final TextField search = new TextField();
    protected PageHeader header;
    private final Pager pager = new Pager(this::loadPage);
    private int page;
    protected int pageSize = 50;
    private final HBox toolbar = new HBox(8);

    protected abstract String titleText();

    protected String subtitleText() {
        return null;
    }

    protected abstract void columns(DataTable<T> table);

    protected abstract PageResult<T> load(String text, int page, int size) throws Exception;

    protected Node[] filters() {
        return new Node[0];
    }

    protected Node[] actions() {
        return new Node[0];
    }

    protected void open(T row) {
    }

    protected Node emptyState() {
        return new EmptyState("inbox", I18n.t("list.empty"), I18n.t("list.emptyHint"), null);
    }

    protected Node top() {
        return null;
    }

    protected String exportName() {
        return "pyhoma-export";
    }

    @Override
    public String title() {
        return titleText();
    }

    @Override
    protected Node build() {
        header = new PageHeader(titleText(), subtitleText()).actions(actions());
        search.setPromptText(I18n.t("list.search"));
        search.getStyleClass().add("search-field");
        HBox searchBox = new HBox(6, Icons.of("search", 14), search);
        searchBox.getStyleClass().add("search-input");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPrefWidth(300);
        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        debounce.setOnFinished(e -> loadPage(0));
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        toolbar.getChildren().add(searchBox);
        toolbar.getChildren().addAll(filters());
        toolbar.getChildren().addAll(Ui.spacer(), Ui.iconButton("download", I18n.t("list.export"), this::exportCsv),
                Ui.iconButton("refresh-cw", I18n.t("action.refresh"), this::reload));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        columns(table);
        table.onOpen(this::open);
        table.placeholder(emptyState());
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox tableBox = new VBox(table, pager);
        tableBox.getStyleClass().add("table-panel");
        VBox.setVgrow(tableBox, Priority.ALWAYS);
        VBox root = new VBox(16, header);
        Node top = top();
        if (top != null) {
            root.getChildren().add(top);
        }
        root.getChildren().addAll(toolbar, tableBox);
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        loadPage(page);
    }

    public void reload() {
        loadPage(page);
    }

    protected void loadPage(int index) {
        String text = search.getText();
        int target = Math.max(0, index);
        Async.run(() -> load(text, target, pageSize), result -> {
            page = result.pageIndex();
            table.setRows(result.items());
            pager.update(result);
        });
    }

    protected void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(exportName() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(Dialogs.owner());
        if (file == null) {
            return;
        }
        String text = search.getText();
        Async.run(() -> {
            List<List<String>> rows = new ArrayList<>();
            for (T row : load(text, 0, 20_000).items()) {
                rows.add(table.exportRow(row));
            }
            char sep = I18n.isPortuguese() ? ';' : ',';
            Files.writeString(file.toPath(), "﻿" + Csv.write(table.exportHeaders(), rows, sep), StandardCharsets.UTF_8);
            return rows.size();
        }, n -> Toasts.success(I18n.t("list.exported", n, file.getName())));
    }
}
