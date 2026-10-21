package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Document;
import io.pyhoma.domain.model.core.DocumentLink;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.List;

public class DocumentService extends AbstractService {

    public DocumentService(Services services) {
        super(services);
    }

    public PageResult<Document> search(String text, Long categoryId, int page, int size) {
        require(Module.DOCUMENT, Right.READ);
        Filter f = Filter.and(Filter.isFalse("deleted"), security().orgFilter(), Filter.search(text, "name", "uri", "notes"),
                categoryId == null ? Filter.ALL : Filter.eq("categoryId", categoryId));
        return repo(Document.class).page(Query.where(f).orderBy(Sort.asc("name")).page(page, size));
    }

    public Document get(Long id) {
        require(Module.DOCUMENT, Right.READ);
        Document d = repo(Document.class).getById(id);
        checkVisible(d);
        return d;
    }

    public Document save(Document doc) {
        require(Module.DOCUMENT, doc.isNew() ? Right.CREATE : Right.UPDATE);
        doc.setName(Texts.clean(doc.getName()));
        doc.setUri(Texts.clean(doc.getUri()));
        if (doc.getOrgUnitId() == null) {
            doc.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("name", doc.getName()).required("uri", doc.getUri())
                .maxLength("name", doc.getName(), 200).throwIfInvalid();
        io.pyhoma.domain.support.DocumentPolicy.classify(doc.getUri());
        return tx(() -> persist(doc));
    }

    public void delete(Long id) {
        require(Module.DOCUMENT, Right.PURGE);
        txRun(() -> {
            Document d = get(id);
            repo(DocumentLink.class).deleteWhere(Filter.eq("documentId", id));
            purge(d);
        });
    }

    public List<Document> documentsOf(String itemType, Long itemId) {
        List<Long> ids = repo(DocumentLink.class).find(Filter.and(Filter.eq("itemType", itemType), Filter.eq("itemId", itemId)))
                .stream().map(DocumentLink::getDocumentId).toList();
        return repo(Document.class).findAllById(ids);
    }

    public void attach(Long documentId, String itemType, Long itemId) {
        require(Module.DOCUMENT, Right.READ);
        txRun(() -> {
            Document d = get(documentId);
            if (repo(DocumentLink.class).exists(Filter.and(Filter.eq("documentId", documentId), Filter.eq("itemType", itemType),
                    Filter.eq("itemId", itemId)))) {
                throw new BusinessException("error.alreadyLinked");
            }
            DocumentLink l = new DocumentLink();
            l.setDocumentId(documentId);
            l.setItemType(itemType);
            l.setItemId(itemId);
            saveQuietly(l);
            services.audit().record(itemType, itemId, AuditAction.LINK, "document", null, d.getName());
        });
    }

    public void detach(Long documentId, String itemType, Long itemId) {
        require(Module.DOCUMENT, Right.READ);
        txRun(() -> {
            repo(DocumentLink.class).deleteWhere(Filter.and(Filter.eq("documentId", documentId), Filter.eq("itemType", itemType),
                    Filter.eq("itemId", itemId)));
            services.audit().record(itemType, itemId, AuditAction.UNLINK, "document", String.valueOf(documentId), null);
        });
    }
}
