package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.asset.ContractItem;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.time.LocalDate;
import java.util.List;

public class ContractService extends AbstractService {

    public ContractService(Services services) {
        super(services);
    }

    public static LocalDate endDate(Contract c) {
        if (c.getStartDate() == null || c.getDurationMonths() == null || c.getDurationMonths() <= 0) {
            return null;
        }
        return c.getStartDate().plusMonths(c.getDurationMonths()).minusDays(1);
    }

    public static LocalDate noticeDate(Contract c) {
        LocalDate end = endDate(c);
        if (end == null || c.getNoticeMonths() == null || c.getNoticeMonths() <= 0) {
            return end;
        }
        return end.minusMonths(c.getNoticeMonths());
    }

    public PageResult<Contract> search(String text, boolean trash, int page, int size) {
        require(Module.CONTRACT, Right.READ);
        Filter f = Filter.and(trash ? Filter.isTrue("deleted") : Filter.isFalse("deleted"), security().orgFilter(),
                Filter.search(text, "name", "contractNumber", "notes"));
        return repo(Contract.class).page(Query.where(f).orderBy(Sort.asc("name")).page(page, size));
    }

    public Contract get(Long id) {
        require(Module.CONTRACT, Right.READ);
        Contract c = repo(Contract.class).getById(id);
        checkVisible(c);
        return c;
    }

    public Contract save(Contract contract) {
        require(Module.CONTRACT, contract.isNew() ? Right.CREATE : Right.UPDATE);
        contract.setName(Texts.clean(contract.getName()));
        if (contract.getOrgUnitId() == null) {
            contract.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("name", contract.getName())
                .positive("durationMonths", contract.getDurationMonths())
                .positive("noticeMonths", contract.getNoticeMonths())
                .positive("cost", contract.getCost())
                .check(contract.getNoticeMonths() == null || contract.getDurationMonths() == null
                        || contract.getNoticeMonths() <= contract.getDurationMonths(), "noticeMonths", "validation.range")
                .throwIfInvalid();
        return tx(() -> persist(contract));
    }

    public void trash(Long id, boolean deleted) {
        require(Module.CONTRACT, Right.DELETE);
        txRun(() -> {
            Contract c = get(id);
            c.setDeleted(deleted);
            saveQuietly(c);
            services.audit().record(c, deleted ? AuditAction.DELETE : AuditAction.RESTORE, null, null, null);
        });
    }

    public void purgeContract(Long id) {
        require(Module.CONTRACT, Right.PURGE);
        txRun(() -> {
            Contract c = get(id);
            repo(ContractItem.class).deleteWhere(Filter.eq("contractId", id));
            purge(c);
        });
    }

    public List<Asset> assets(Long contractId) {
        List<Long> ids = repo(ContractItem.class).find(Filter.eq("contractId", contractId)).stream()
                .map(ContractItem::getAssetId).toList();
        return repo(Asset.class).findAllById(ids);
    }

    public List<Contract> contractsOfAsset(Long assetId) {
        List<Long> ids = repo(ContractItem.class).find(Filter.eq("assetId", assetId)).stream()
                .map(ContractItem::getContractId).toList();
        return repo(Contract.class).findAllById(ids);
    }

    public void addAsset(Long contractId, Long assetId) {
        require(Module.CONTRACT, Right.UPDATE);
        txRun(() -> {
            Contract c = get(contractId);
            if (repo(ContractItem.class).exists(Filter.and(Filter.eq("contractId", contractId), Filter.eq("assetId", assetId)))) {
                throw new BusinessException("error.alreadyLinked");
            }
            ContractItem i = new ContractItem();
            i.setContractId(contractId);
            i.setAssetId(assetId);
            saveQuietly(i);
            services.audit().record(c, AuditAction.LINK, "asset", null, repo(Asset.class).getById(assetId).getName());
        });
    }

    public void removeAsset(Long contractId, Long assetId) {
        require(Module.CONTRACT, Right.UPDATE);
        txRun(() -> {
            Contract c = get(contractId);
            repo(ContractItem.class).deleteWhere(Filter.and(Filter.eq("contractId", contractId), Filter.eq("assetId", assetId)));
            services.audit().record(c, AuditAction.UNLINK, "asset", String.valueOf(assetId), null);
        });
    }

    public List<Contract> expiring(int days) {
        LocalDate today = today();
        LocalDate limit = today.plusDays(days);
        return repo(Contract.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                        Filter.notNull("startDate"))))
                .stream().filter(c -> {
                    LocalDate end = endDate(c);
                    return end != null && !end.isBefore(today) && !end.isAfter(limit);
                }).toList();
    }
}
