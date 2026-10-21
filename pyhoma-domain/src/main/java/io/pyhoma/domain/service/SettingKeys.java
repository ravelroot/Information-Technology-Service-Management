package io.pyhoma.domain.service;

public final class SettingKeys {

    public static final String ORGANIZATION_NAME = "app.organizationName";
    public static final String INSTALLED_VERSION = "app.installedVersion";
    public static final String INSTALLED_AT = "app.installedAt";
    public static final String CALENDAR_WORK_DAYS = "calendar.workDays";
    public static final String CALENDAR_DAY_START = "calendar.dayStart";
    public static final String CALENDAR_DAY_END = "calendar.dayEnd";
    public static final String TICKET_AUTO_CLOSE_DAYS = "ticket.autoCloseDays";
    public static final String TICKET_SATISFACTION = "ticket.satisfactionEnabled";
    public static final String SLA_WARNING_PERCENT = "sla.warningPercent";
    public static final String EXPIRATION_ALERT_DAYS = "alert.expirationDays";
    public static final String NOTIFICATION_RETENTION_DAYS = "notification.retentionDays";
    public static final String MAX_FAILED_LOGINS = "security.maxFailedLogins";
    public static final String LOCK_MINUTES = "security.lockMinutes";
    public static final String SESSION_IDLE_MINUTES = "security.sessionIdleMinutes";
    public static final String ASSET_TAG_PREFIX = "asset.tagPrefix";
    public static final String SETUP_COMPLETED = "setup.completed";

    private SettingKeys() {
    }
}
