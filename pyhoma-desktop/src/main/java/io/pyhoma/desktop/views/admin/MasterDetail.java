package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.components.PageHeader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

abstract class MasterDetail extends View {

    protected final StackPane detail = new StackPane();
    protected PageHeader header;

    protected abstract String titleText();

    protected abstract String subtitleText();

    protected abstract Node master();

    protected Node[] actions() {
        return new Node[0];
    }

    @Override
    public String title() {
        return titleText();
    }

    @Override
    protected Node build() {
        header = new PageHeader(titleText(), subtitleText()).actions(actions());
        Node m = master();
        VBox masterBox = new VBox(m);
        masterBox.getStyleClass().add("master");
        masterBox.setPrefWidth(320);
        masterBox.setMinWidth(260);
        VBox.setVgrow(m, Priority.ALWAYS);
        detail.getStyleClass().add("detail");
        HBox body = new HBox(16, masterBox, detail);
        HBox.setHgrow(detail, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(16, header, body);
        root.getStyleClass().add("page");
        return root;
    }
}
