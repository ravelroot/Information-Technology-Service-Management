package io.pyhoma.domain.service;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.AuditEntry;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.security.UserSession;
import io.pyhoma.domain.support.Fields;
import io.pyhoma.domain.support.Texts;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AuditService extends AbstractService {

    private static final Set<String> IGNORED = Set.of("id", "version", "createdAt", "updatedAt", "passwordHash",
            "failedAttempts", "lockedUntil", "lastLoginAt", "lastActivityAt", "viewCount", "slaWarned",
            "slaBreachNotified", "completeName", "depth", "licenseKey");
    private static final Set<String> SECRET = Set.of("passwordHash", "licenseKey");

    public AuditService(Services services) {
        super(services);
    }

    public void record(BaseEntity entity, AuditAction action, String field, String oldValue, String newValue) {
        record(entity.itemTypeName(), entity.getId(), action, field, oldValue, newValue);
    }

    public void record(String itemType, Long itemId, AuditAction action, String field, String oldValue, String newValue) {
        AuditEntry e = new AuditEntry();
        e.setItemType(itemType);
        e.setItemId(itemId);
        UserSession s = services.security().isAuthenticated() ? services.security().current() : null;
        e.setUserId(s == null ? null : s.userId());
        e.setUserName(s == null ? "system" : s.fullName());
        e.setAction(action);
        e.setFieldName(field);
        e.setOldValue(Texts.abbreviate(oldValue, 2000));
        e.setNewValue(Texts.abbreviate(newValue, 2000));
        LocalDateTime now = now();
        e.setOccurredAt(now);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        repo(AuditEntry.class).save(e);
    }

    public void recordDiff(BaseEntity before, BaseEntity after) {
        for (Field f : Fields.of(after.getClass())) {
            if (IGNORED.contains(f.getName()) && !SECRET.contains(f.getName())) {
                continue;
            }
            try {
                Object o = f.get(before);
                Object n = f.get(after);
                if (!same(o, n)) {
                    if (SECRET.contains(f.getName())) {
                        record(after, AuditAction.UPDATE, f.getName(), "•••", "•••");
                    } else {
                        record(after, AuditAction.UPDATE, f.getName(), format(o), format(n));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public PageResult<AuditEntry> history(String itemType, Long itemId, int page, int size) {
        Query q = Query.where(Filter.and(Filter.eq("itemType", itemType), Filter.eq("itemId", itemId)))
                .orderBy(Sort.desc("occurredAt"), Sort.desc("id"))
                .page(page, size);
        return repo(AuditEntry.class).page(q);
    }

    public PageResult<AuditEntry> search(String itemType, Long userId, String text, LocalDate from, LocalDate to,
                                         int page, int size) {
        require(Module.AUDIT, Right.READ);
        Filter f = Filter.and(
                itemType == null ? Filter.ALL : Filter.eq("itemType", itemType),
                userId == null ? Filter.ALL : Filter.eq("userId", userId),
                Filter.search(text, "fieldName", "oldValue", "newValue", "userName", "itemType"),
                from == null ? Filter.ALL : Filter.ge("occurredAt", from.atStartOfDay()),
                to == null ? Filter.ALL : Filter.lt("occurredAt", to.plusDays(1).atStartOfDay()));
        return repo(AuditEntry.class).page(Query.where(f).orderBy(Sort.desc("occurredAt"), Sort.desc("id")).page(page, size));
    }

    public List<String> itemTypes() {
        return repo(AuditEntry.class).countBy("itemType", Filter.ALL).keySet().stream()
                .filter(Objects::nonNull).map(Object::toString).sorted().toList();
    }

    private static boolean same(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        if (a instanceof String x && b == null) {
            return x.isEmpty();
        }
        if (b instanceof String y && a == null) {
            return y.isEmpty();
        }
        return Objects.equals(a, b);
    }

    static String format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal d) {
            return d.toPlainString();
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return value.toString();
    }
}
