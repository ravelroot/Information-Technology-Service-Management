package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.Notification;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.support.Texts;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NotificationService extends AbstractService {

    public enum Severity { INFO, SUCCESS, WARNING, DANGER }

    public NotificationService(Services services) {
        super(services);
    }

    public static final String ARG_SEPARATOR = "\u001F";

    public void notify(Collection<Long> userIds, Severity severity, String titleKey, List<String> args,
                       String itemType, Long itemId) {
        String message = args == null ? null : String.join(ARG_SEPARATOR, args.stream()
                .map(a -> a == null ? "" : a.replace(ARG_SEPARATOR, " ")).toList());
        String title = titleKey;
        Set<Long> targets = new LinkedHashSet<>();
        Long me = services.security().isAuthenticated() ? services.security().current().userId() : null;
        for (Long id : userIds) {
            if (id != null && !Objects.equals(id, me)) {
                targets.add(id);
            }
        }
        for (Long userId : targets) {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setTitle(Texts.abbreviate(title, 200));
            n.setMessage(Texts.abbreviate(message, 2000));
            n.setItemType(itemType);
            n.setItemId(itemId);
            n.setSeverity(severity.name());
            saveQuietly(n);
        }
    }

    public long unreadCount() {
        Long me = currentUserId();
        return me == null ? 0 : repo(Notification.class).count(Filter.and(Filter.eq("userId", me), Filter.isFalse("seen")));
    }

    public PageResult<Notification> mine(boolean unreadOnly, int page, int size) {
        Filter f = Filter.and(Filter.eq("userId", currentUserId()), unreadOnly ? Filter.isFalse("seen") : Filter.ALL);
        return repo(Notification.class).page(Query.where(f).orderBy(Sort.desc("createdAt"), Sort.desc("id")).page(page, size));
    }

    public void markSeen(Long notificationId) {
        txRun(() -> repo(Notification.class).findById(notificationId).ifPresent(n -> {
            if (Objects.equals(n.getUserId(), currentUserId()) && !n.isSeen()) {
                n.setSeen(true);
                saveQuietly(n);
            }
        }));
    }

    public void markAllSeen() {
        txRun(() -> {
            for (Notification n : repo(Notification.class).find(Filter.and(Filter.eq("userId", currentUserId()),
                    Filter.isFalse("seen")))) {
                n.setSeen(true);
                saveQuietly(n);
            }
        });
    }

    int purgeOld(int retentionDays) {
        return repo(Notification.class).deleteWhere(Filter.and(Filter.isTrue("seen"),
                Filter.lt("createdAt", now().minusDays(retentionDays))));
    }
}
