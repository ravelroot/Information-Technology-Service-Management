package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.asset.Supplier;
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

public class SupplierService extends AbstractService {

    public SupplierService(Services services) {
        super(services);
    }

    public PageResult<Supplier> search(String text, int page, int size) {
        require(Module.SUPPLIER, Right.READ);
        Filter f = Filter.and(security().orgFilter(), Filter.search(text, "name", "email", "city", "website", "phone"));
        return repo(Supplier.class).page(Query.where(f).orderBy(Sort.asc("name")).page(page, size));
    }

    public List<Supplier> active() {
        return repo(Supplier.class).find(Query.where(Filter.and(Filter.isTrue("active"), security().orgFilter()))
                .orderBy(Sort.asc("name")));
    }

    public Supplier get(Long id) {
        require(Module.SUPPLIER, Right.READ);
        Supplier s = repo(Supplier.class).getById(id);
        checkVisible(s);
        return s;
    }

    public Supplier save(Supplier supplier) {
        require(Module.SUPPLIER, supplier.isNew() ? Right.CREATE : Right.UPDATE);
        supplier.setName(Texts.clean(supplier.getName()));
        if (supplier.getOrgUnitId() == null) {
            supplier.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("name", supplier.getName()).email("email", supplier.getEmail()).throwIfInvalid();
        return tx(() -> persist(supplier));
    }

    public void delete(Long id) {
        require(Module.SUPPLIER, Right.DELETE);
        txRun(() -> {
            Supplier s = get(id);
            if (repo(Contract.class).exists(Filter.eq("supplierId", id))) {
                throw new BusinessException("error.inUse", s.getName());
            }
            purge(s);
        });
    }
}
