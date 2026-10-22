package io.talos.domain.service;

import io.talos.domain.model.asset.Asset;
import io.talos.domain.model.asset.Contract;
import io.talos.domain.model.asset.Software;
import io.talos.domain.model.asset.Supplier;
import io.talos.domain.model.core.Module;
import io.talos.domain.model.core.User;
import io.talos.domain.model.itil.ItilItem;
import io.talos.domain.model.kb.KbArticle;
import io.talos.domain.model.project.Project;
import io.talos.domain.query.Filter;
import io.talos.domain.query.Query;
import io.talos.domain.query.Sort;
import io.talos.domain.security.Right;
import io.talos.domain.support.Texts;
import java.util.ArrayList;
import java.util.List;

public class SearchService extends AbstractService {

    public record Hit(String type, Long id, String title, String subtitle) {
    }

    public SearchService(Services services) {
        super(services);
    }

    public List<Hit> search(String text, int perType) {
        List<Hit> hits = new ArrayList<>();
        String q = Texts.clean(text);
        if (q == null || q.length() < 2 && !q.matches("\\d+")) {
            return hits;
        }
        itil(hits, services.tickets(), q, perType);
        itil(hits, services.problems(), q, perType);
        itil(hits, services.changes(), q, perType);
        if (can(Module.ASSET, Right.READ)) {
            for (Asset a : repo(Asset.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                    Filter.search(q, "name", "serialNumber", "assetTag", "ipAddress", "hostname"))).orderBy(Sort.asc("name")).limit(perType))) {
                hits.add(new Hit("Asset", a.getId(), a.getName(), join(a.getAssetTag(), a.getSerialNumber(), a.getIpAddress())));
            }
        }
        if (can(Module.USER, Right.READ)) {
            for (User u : repo(User.class).find(Query.where(Filter.search(q, "fullName", "login", "email"))
                    .orderBy(Sort.asc("fullName")).limit(perType))) {
                hits.add(new Hit("User", u.getId(), u.getFullName(), join(u.getLogin(), u.getEmail())));
            }
        }
        if (can(Module.KNOWLEDGE_BASE, Right.READ)) {
            boolean editor = can(Module.KNOWLEDGE_BASE, Right.UPDATE);
            for (KbArticle a : repo(KbArticle.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                    editor ? Filter.ALL : Filter.isTrue("published"), Filter.search(q, "title", "content"))).limit(perType))) {
                hits.add(new Hit("KbArticle", a.getId(), a.getTitle(), a.isFaq() ? "FAQ" : null));
            }
        }
        if (can(Module.PROJECT, Right.READ)) {
            for (Project p : repo(Project.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                    Filter.search(q, "name", "code"))).limit(perType))) {
                hits.add(new Hit("Project", p.getId(), p.getName(), p.getCode()));
            }
        }
        if (can(Module.SOFTWARE, Right.READ)) {
            for (Software s : repo(Software.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                    Filter.search(q, "name"))).limit(perType))) {
                hits.add(new Hit("Software", s.getId(), s.getName(), null));
            }
        }
        if (can(Module.CONTRACT, Right.READ)) {
            for (Contract c : repo(Contract.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                    Filter.search(q, "name", "contractNumber"))).limit(perType))) {
                hits.add(new Hit("Contract", c.getId(), c.getName(), c.getContractNumber()));
            }
        }
        if (can(Module.SUPPLIER, Right.READ)) {
            for (Supplier s : repo(Supplier.class).find(Query.where(Filter.and(security().orgFilter(),
                    Filter.search(q, "name", "email"))).limit(perType))) {
                hits.add(new Hit("Supplier", s.getId(), s.getName(), s.getEmail()));
            }
        }
        return hits;
    }

    private <T extends ItilItem> void itil(List<Hit> hits, ItilService<T> svc, String q, int perType) {
        if (!can(svc.module(), Right.READ)) {
            return;
        }
        ItilSearch s = ItilSearch.create().view(ItilSearch.View.ALL).text(q).page(0, perType);
        for (T item : svc.search(s).items()) {
            hits.add(new Hit(item.getClass().getSimpleName(), item.getId(), item.getTitle(), item.reference()));
        }
    }

    private static String join(String... parts) {
        List<String> p = new ArrayList<>();
        for (String s : parts) {
            if (!Texts.isBlank(s)) {
                p.add(s);
            }
        }
        return p.isEmpty() ? null : String.join(" · ", p);
    }
}
