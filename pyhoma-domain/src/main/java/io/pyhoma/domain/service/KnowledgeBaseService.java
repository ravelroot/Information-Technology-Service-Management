package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.kb.KbArticle;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.List;

public class KnowledgeBaseService extends AbstractService {

    public KnowledgeBaseService(Services services) {
        super(services);
    }

    public PageResult<KbArticle> search(String text, Long categoryId, boolean faqOnly, boolean trash, int page, int size) {
        require(Module.KNOWLEDGE_BASE, Right.READ);
        boolean editor = can(Module.KNOWLEDGE_BASE, Right.UPDATE);
        Filter f = Filter.and(trash ? Filter.isTrue("deleted") : Filter.isFalse("deleted"),
                security().orgFilter(),
                Filter.search(text, "title", "content"),
                categoryId == null ? Filter.ALL : Filter.eq("categoryId", categoryId),
                faqOnly || security().current().isSelfService() ? Filter.isTrue("faq") : Filter.ALL,
                editor ? Filter.ALL : Filter.isTrue("published"));
        return repo(KbArticle.class).page(Query.where(f).orderBy(Sort.desc("updatedAt")).page(page, size));
    }

    public List<KbArticle> popular(int max) {
        require(Module.KNOWLEDGE_BASE, Right.READ);
        return repo(KbArticle.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), Filter.isTrue("published"),
                security().orgFilter())).orderBy(Sort.desc("viewCount")).limit(max));
    }

    public KbArticle get(Long id) {
        require(Module.KNOWLEDGE_BASE, Right.READ);
        KbArticle a = repo(KbArticle.class).getById(id);
        checkVisible(a);
        if (!a.isPublished() && !can(Module.KNOWLEDGE_BASE, Right.UPDATE)) {
            throw new io.pyhoma.domain.support.NotFoundException("KbArticle", id);
        }
        return a;
    }

    public KbArticle read(Long id) {
        return tx(() -> {
            KbArticle a = get(id);
            a.setViewCount(a.getViewCount() + 1);
            return saveQuietly(a);
        });
    }

    public KbArticle save(KbArticle article) {
        require(Module.KNOWLEDGE_BASE, article.isNew() ? Right.CREATE : Right.UPDATE);
        article.setTitle(Texts.clean(article.getTitle()));
        if (article.getOrgUnitId() == null) {
            article.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("title", article.getTitle()).maxLength("title", article.getTitle(), 255)
                .required("content", article.getContent()).throwIfInvalid();
        return tx(() -> {
            if (article.isNew()) {
                article.setAuthorId(currentUserId());
                article.setViewCount(0);
            } else {
                article.setViewCount(repo(KbArticle.class).getById(article.getId()).getViewCount());
            }
            article.setUpdatedById(currentUserId());
            return persist(article);
        });
    }

    public void trash(Long id, boolean deleted) {
        require(Module.KNOWLEDGE_BASE, Right.DELETE);
        txRun(() -> {
            KbArticle a = get(id);
            a.setDeleted(deleted);
            saveQuietly(a);
            services.audit().record(a, deleted ? AuditAction.DELETE : AuditAction.RESTORE, null, null, null);
        });
    }

    public void purgeArticle(Long id) {
        require(Module.KNOWLEDGE_BASE, Right.PURGE);
        txRun(() -> purge(get(id)));
    }
}
