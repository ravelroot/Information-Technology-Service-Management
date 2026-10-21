package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class PageHeader extends HBox {

    private final Label title;
    private final Label subtitle;
    private final HBox actions = new HBox(8);
    private final HBox titleRow = new HBox(10);

    public PageHeader(String titleText, String subtitleText) {
        getStyleClass().add("page-header");
        setAlignment(Pos.CENTER_LEFT);
        title = Ui.label(titleText, "page-title");
        subtitle = Ui.label(subtitleText == null ? "" : subtitleText, "page-subtitle");
        subtitle.setManaged(subtitleText != null);
        subtitle.setVisible(subtitleText != null);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(title);
        VBox texts = new VBox(2, titleRow, subtitle);
        actions.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(texts, Ui.spacer(), actions);
    }

    public PageHeader actions(Node... nodes) {
        actions.getChildren().setAll(nodes);
        return this;
    }

    public PageHeader tags(Node... nodes) {
        titleRow.getChildren().setAll(title);
        titleRow.getChildren().addAll(nodes);
        return this;
    }

    public void setTitle(String t) {
        title.setText(t);
    }

    public void setSubtitle(String s) {
        subtitle.setText(s);
        subtitle.setManaged(s != null);
        subtitle.setVisible(s != null);
    }

    public PageHeader back(Runnable action) {
        getChildren().add(0, Ui.iconButton("arrow-left", io.pyhoma.desktop.ui.I18n.t("action.back"), action));
        return this;
    }
}
