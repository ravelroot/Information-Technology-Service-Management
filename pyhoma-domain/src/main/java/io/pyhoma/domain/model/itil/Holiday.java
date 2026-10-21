package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "pyh_holiday")
public class Holiday extends BaseEntity implements Named {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private boolean recurring;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    @Override
    public String displayName() {
        return name;
    }
}
