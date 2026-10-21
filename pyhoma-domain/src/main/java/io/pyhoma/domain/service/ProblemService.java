package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Problem;

public class ProblemService extends ItilService<Problem> {

    public ProblemService(Services services) {
        super(services);
    }

    @Override
    public ItilType itilType() {
        return ItilType.PROBLEM;
    }

    @Override
    protected Class<Problem> type() {
        return Problem.class;
    }

    @Override
    public Module module() {
        return Module.PROBLEM;
    }

    @Override
    protected Problem newInstance() {
        return new Problem();
    }
}
