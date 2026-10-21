package io.pyhoma.desktop.views;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.desktop.views.itil.ItilListViewHelper;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.kb.KbArticle;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

public class KnowledgeBaseView extends ListPage<KbArticle> {

    private final PickerField<Long> category = PickerField.compact(() -> Option.of(App.lookups().dropdown(DropdownKind.KB_CATEGORY)), null);
    private final CheckBox faqOnly = new CheckBox();

    @Override
    protected String titleText() {
        return I18n.t("nav.KNOWLEDGE");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("kb.subtitle");
    }

    @Override
    protected Node[] filters() {
        category.onChange(v -> loadPage(0));
        faqOnly.setText(I18n.t("kb.faqOnly"));
        faqOnly.setOnAction(e -> loadPage(0));
        return new Node[] {ItilListViewHelper.labeled("field.categoryId", category), faqOnly};
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.KNOWLEDGE_BASE, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("kb.new"), "plus", () -> App.nav().open(new KbArticleView(null)))};
    }

    @Override
    protected Node emptyState() {
        return new EmptyState("book-open", I18n.t("kb.empty"), I18n.t("kb.emptyHint"), null);
    }

    @Override
    protected void columns(DataTable<KbArticle> t) {
        t.node(I18n.t("field.title"), 380, KnowledgeBaseView::titleCell, KbArticle::getTitle)
                .col(I18n.t("field.categoryId"), 200, a -> App.lookups().dd(DropdownKind.KB_CATEGORY, a.getCategoryId()))
                .col(I18n.t("field.authorId"), 160, a -> App.lookups().user(a.getAuthorId()))
                .col(I18n.t("field.viewCount"), 90, "num", KbArticle::getViewCount)
                .col(I18n.t("field.updatedAt"), 130, a -> I18n.relative(a.getUpdatedAt()));
    }

    static Node titleCell(KbArticle a) {
        HBox h = new HBox(8, Ui.label(a.getTitle(), "cell-strong"));
        h.setAlignment(Pos.CENTER_LEFT);
        if (a.isFaq()) {
            h.getChildren().add(Badge.of("FAQ", "teal"));
        }
        if (!a.isPublished()) {
            h.getChildren().add(Badge.of(I18n.t("kb.draft"), "amber"));
        }
        return h;
    }

    @Override
    protected PageResult<KbArticle> load(String text, int page, int size) {
        return App.services().knowledgeBase().search(text, category.getValue(), faqOnly.isSelected(), false, page, size);
    }

    @Override
    protected void open(KbArticle row) {
        App.nav().open(new KbArticleView(row.getId()));
    }
}
