package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.itil.ItilItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_problem")
public class Problem extends ItilItem {

    @LongText
    @Column
    private String impactContent;

    @LongText
    @Column
    private String causeContent;

    @LongText
    @Column
    private String symptomContent;

    public String getImpactContent() {
        return impactContent;
    }

    public void setImpactContent(String impactContent) {
        this.impactContent = impactContent;
    }

    public String getCauseContent() {
        return causeContent;
    }

    public void setCauseContent(String causeContent) {
        this.causeContent = causeContent;
    }

    public String getSymptomContent() {
        return symptomContent;
    }

    public void setSymptomContent(String symptomContent) {
        this.symptomContent = symptomContent;
    }

    @Override
    public ItilType itilType() {
        return ItilType.PROBLEM;
    }
}
