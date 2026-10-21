package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.domain.query.PageResult;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class Pager extends HBox {

    private final Label info = Ui.label("", "muted");
    private final Button prev;
    private final Button next;
    private int page;
    private int pages = 1;

    public Pager(IntConsumer onPage) {
        super(6);
        getStyleClass().add("pager");
        setAlignment(Pos.CENTER_RIGHT);
        prev = Ui.iconButton("chevron-left", I18n.t("pager.previous"), () -> onPage.accept(page - 1));
        next = Ui.iconButton("chevron-right", I18n.t("pager.next"), () -> onPage.accept(page + 1));
        getChildren().addAll(info, prev, next);
    }

    public void update(PageResult<?> result) {
        page = result.pageIndex();
        pages = result.pageCount();
        long from = result.total() == 0 ? 0 : result.offset() + 1L;
        long to = result.offset() + (long) result.items().size();
        info.setText(I18n.t("pager.range", from, to, result.total()));
        prev.setDisable(page <= 0);
        next.setDisable(page >= pages - 1);
    }
}
