package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.ApprovalStatus;
import io.pyhoma.domain.model.itil.Change;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.support.Validator;

public class ChangeService extends ItilService<Change> {

    public ChangeService(Services services) {
        super(services);
    }

    @Override
    public ItilType itilType() {
        return ItilType.CHANGE;
    }

    @Override
    protected Class<Change> type() {
        return Change.class;
    }

    @Override
    public Module module() {
        return Module.CHANGE;
    }

    @Override
    protected Change newInstance() {
        return new Change();
    }

    @Override
    protected void validate(Change item) {
        super.validate(item);
        Validator.create()
                .check(item.getPlannedStartAt() == null || item.getPlannedEndAt() == null
                        || !item.getPlannedEndAt().isBefore(item.getPlannedStartAt()), "plannedEndAt", "validation.endBeforeStart")
                .throwIfInvalid();
    }

    @Override
    protected void checkManualTransition(Change before, ItilStatus target) {
        super.checkManualTransition(before, target);
        boolean needsApproval = target == ItilStatus.ACCEPTED || target == ItilStatus.TESTING
                || target == ItilStatus.QUALIFICATION || target == ItilStatus.APPLIED;
        if (needsApproval && before.getApprovalStatus() == ApprovalStatus.WAITING) {
            throw new io.pyhoma.domain.support.BusinessException("error.approvalPending");
        }
        if (needsApproval && before.getApprovalStatus() == ApprovalStatus.REFUSED) {
            throw new io.pyhoma.domain.support.BusinessException("error.approvalRefused");
        }
    }

    void onApprovalResult(Change change, ApprovalStatus result) {
        change.setApprovalStatus(result);
        if (change.getStatus() == ItilStatus.APPROVAL) {
            if (result == ApprovalStatus.ACCEPTED) {
                transition(change, ItilStatus.ACCEPTED);
            } else if (result == ApprovalStatus.REFUSED) {
                transition(change, ItilStatus.EVALUATION);
            }
        }
    }
}
