package io.talos.domain.service;

import io.talos.domain.model.asset.Contract;
import io.talos.domain.model.asset.Supplier;
import io.talos.domain.model.core.Module;
import io.talos.domain.query.Filter;
import io.talos.domain.query.PageResult;
import io.talos.domain.query.Query;
import io.talos.domain.query.Sort;
import io.talos.domain.security.Right;
import io.talos.domain.support.BusinessException;
import io.talos.domain.support.Texts;
import io.talos.domain.support.Validator;
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
