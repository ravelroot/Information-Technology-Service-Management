package io.pyhoma.desktop.views;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.views.common.Markdown;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.kb.KbArticle;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class KbArticleView extends View {

    private Long id;
    private KbArticle article;
    private boolean editing;
    private final PageHeader header = new PageHeader("", null);
    private final StackPane body = new StackPane();

    public KbArticleView(Long id) {
        this.id = id;
        this.editing = id == null;
    }

    @Override
    public String title() {
        return article == null || article.getTitle() == null ? I18n.t("kb.new") : article.getTitle();
    }

    @Override
    protected Node build() {
        header.back(() -> App.nav().back());
        BorderPane root = new BorderPane(body);
        root.setTop(header);
        BorderPane.setMargin(body, new javafx.geometry.Insets(16, 0, 0, 0));
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        if (id == null) {
            article = new KbArticle();
            article.setPublished(true);
            render();
        } else {
            Async.run(() -> App.services().knowledgeBase().read(id), a -> {
                article = a;
                render();
            });
        }
    }

    private void render() {
        boolean canEdit = App.can(Module.KNOWLEDGE_BASE, article.isNew() ? Right.CREATE : Right.UPDATE);
        header.setTitle(article.isNew() ? I18n.t("kb.new") : article.getTitle());
        header.setSubtitle(article.isNew() ? null : I18n.t("kb.meta", App.lookups().user(article.getAuthorId()),
                I18n.dateTime(article.getUpdatedAt()), article.getViewCount()));
        List<Node> tags = new ArrayList<>();
        if (article.isFaq()) {
            tags.add(Badge.of("FAQ", "teal"));
        }
        if (article.getCategoryId() != null) {
            tags.add(Badge.of(App.lookups().dd(DropdownKind.KB_CATEGORY, article.getCategoryId()), "slate"));
        }
        header.tags(tags.toArray(Node[]::new));
        List<Node> actions = new ArrayList<>();
        if (canEdit && !editing) {
            actions.add(Ui.button(I18n.t("action.edit"), "pencil", () -> {
                editing = true;
                render();
            }));
            if (App.can(Module.KNOWLEDGE_BASE, Right.DELETE)) {
                actions.add(0, Ui.button(I18n.t("action.trash"), "trash-2", () -> {
                    if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("confirm.trash"), I18n.t("action.trash"), true)) {
                        Async.exec(() -> App.services().knowledgeBase().trash(id, true), () -> App.nav().back());
                    }
                }, "ghost"));
            }
        }
        if (editing) {
            body.getChildren().setAll(editor(actions));
        } else {
            VBox reader = new VBox(Markdown.render(article.getContent() == null ? "" : article.getContent()));
            reader.getStyleClass().addAll("panel", "kb-reader");
            ScrollPane sp = new ScrollPane(reader);
            sp.setFitToWidth(true);
            body.getChildren().setAll(sp);
        }
        header.actions(actions.toArray(Node[]::new));
    }

    private Node editor(List<Node> actions) {
        Form meta = new Form(4)
                .text("title", article::getTitle, article::setTitle, 2, true)
                .choice("categoryId", () -> Option.of(App.lookups().dropdown(DropdownKind.KB_CATEGORY)), article::getCategoryId,
                        article::setCategoryId, true, false)
                .check("faq", article::isFaq, article::setFaq)
                .check("published", article::isPublished, article::setPublished);
        meta.load();
        TextArea content = new TextArea(article.getContent());
        content.setWrapText(true);
        content.getStyleClass().add("md-editor");
        StackPane preview = new StackPane(Markdown.render(content.getText() == null ? "" : content.getText()));
        preview.getStyleClass().add("md-preview");
        content.textProperty().addListener((o, a, b) -> preview.getChildren().setAll(Markdown.render(b)));
        ScrollPane previewScroll = new ScrollPane(preview);
        previewScroll.setFitToWidth(true);
        HBox split = new HBox(16, content, previewScroll);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox.setHgrow(previewScroll, Priority.ALWAYS);
        content.setPrefWidth(500);
        previewScroll.setPrefWidth(500);
        VBox.setVgrow(split, Priority.ALWAYS);
        actions.add(Ui.button(I18n.t("action.cancel"), null, () -> {
            if (article.isNew()) {
                App.nav().back();
            } else {
                editing = false;
                onShow();
            }
        }, "ghost"));
        actions.add(Ui.primary(I18n.t("action.save"), "save", () -> {
            if (!meta.commit()) {
                return;
            }
            article.setContent(content.getText());
            Async.run(() -> App.services().knowledgeBase().save(article), saved -> {
                id = saved.getId();
                article = saved;
                editing = false;
                Toasts.success(I18n.t("toast.saved"));
                render();
            }, e -> {
                if (e instanceof ValidationException v && meta.showErrors(v)) {
                    return;
                }
                Dialogs.error(e);
            });
        }));
        VBox box = new VBox(14, meta.node(), Ui.muted(I18n.t("kb.markdownHint")), split);
        box.getStyleClass().add("panel");
        return box;
    }
}
