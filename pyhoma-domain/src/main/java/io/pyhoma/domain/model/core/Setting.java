package io.pyhoma.domain.model.core;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_setting")
public class Setting extends BaseEntity {

    @Column(nullable = false, length = 120, unique = true)
    private String settingKey;

    @Column(length = 2000)
    private String settingValue;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
