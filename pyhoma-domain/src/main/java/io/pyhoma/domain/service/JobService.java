package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.JobLock;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.core.ProfileRight;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.NotificationService.Severity;
import io.pyhoma.domain.support.ConcurrentUpdateException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class JobService extends AbstractService {

    private static final System.Logger LOG = System.getLogger(JobService.class.getName());

    private final String nodeId = hostName() + "/" + UUID.randomUUID().toString().substring(0, 8);

    public JobService(Services services) {
        super(services);
    }

    public void runFrequent() {
        security().runAsSystem(() -> {
            exclusive("sla-monitor", Duration.ofMinutes(5), () -> services.tickets().monitorSla());
            exclusive("recurring-tickets", Duration.ofMinutes(5), () -> services.recurringTickets().runDue());
        });
    }

    public void runHourly() {
        security().runAsSystem(() -> {
            exclusive("auto-close", Duration.ofMinutes(30), () -> services.tickets().autoClose());
            JobLock daily = repo(JobLock.class).findFirst(Filter.eq("jobName", "daily")).orElse(null);
            boolean ranToday = daily != null && daily.getLastRunAt() != null
                    && daily.getLastRunAt().toLocalDate().equals(today());
            if (!ranToday) {
                exclusive("daily", Duration.ofMinutes(30), () -> {
                    int n = expirationAlerts();
                    n += services.notifications().purgeOld(services.settings().getInt(SettingKeys.NOTIFICATION_RETENTION_DAYS, 90));
                    return n;
                });
            }
        });
    }

    private void exclusive(String job, Duration ttl, Supplier<Integer> work) {
        if (!acquire(job, ttl)) {
            return;
        }
        try {
            int n = work.get();
            if (n > 0) {
                LOG.log(System.Logger.Level.INFO, "Job {0}: {1} item(s)", job, n);
            }
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Job " + job + " failed", e);
        } finally {
            release(job);
        }
    }

    boolean acquire(String job, Duration ttl) {
        try {
            return tx(() -> {
                LocalDateTime now = now();
                JobLock lock = repo(JobLock.class).findFirst(Filter.eq("jobName", job)).orElseGet(() -> {
                    JobLock l = new JobLock();
                    l.setJobName(job);
                    return l;
                });
                if (lock.getLockedUntil() != null && lock.getLockedUntil().isAfter(now) && !nodeId.equals(lock.getLockedBy())) {
                    return false;
                }
                lock.setLockedBy(nodeId);
                lock.setLockedUntil(now.plus(ttl));
                saveQuietly(lock);
                return true;
            });
        } catch (ConcurrentUpdateException e) {
            return false;
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.DEBUG, "Lock " + job + " not acquired: " + e.getMessage());
            return false;
        }
    }

    void release(String job) {
        try {
            txRun(() -> repo(JobLock.class).findFirst(Filter.eq("jobName", job)).ifPresent(lock -> {
                lock.setLockedUntil(null);
                lock.setLastRunAt(now());
                saveQuietly(lock);
            }));
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Cannot release lock " + job, e);
        }
    }

    int expirationAlerts() {
        int days = services.settings().getInt(SettingKeys.EXPIRATION_ALERT_DAYS, 30);
        int warranties = services.assets().warrantyExpiring(days).size();
        int contracts = services.contracts().expiring(days).size();
        int licenses = services.software().expiringLicenses(days).size();
        long overUsed = services.software().compliance().stream().filter(SoftwareService.LicenseUsage::overUsed).count();
        Set<Long> admins = administrators();
        int sent = 0;
        if (warranties > 0) {
            services.notifications().notify(admins, Severity.WARNING, "notif.alert.warranty",
                    List.of(String.valueOf(warranties), String.valueOf(days)), "Asset", null);
            sent++;
        }
        if (contracts > 0) {
            services.notifications().notify(admins, Severity.WARNING, "notif.alert.contract",
                    List.of(String.valueOf(contracts), String.valueOf(days)), "Contract", null);
            sent++;
        }
        if (licenses > 0 || overUsed > 0) {
            services.notifications().notify(admins, Severity.WARNING, "notif.alert.license",
                    List.of(String.valueOf(licenses), String.valueOf(overUsed)), "Software", null);
            sent++;
        }
        return sent;
    }

    Set<Long> administrators() {
        Set<Long> profiles = new LinkedHashSet<>();
        for (ProfileRight r : repo(ProfileRight.class).find(Filter.eq("module", Module.SETTINGS))) {
            if (Right.has(r.getRights(), Right.UPDATE)) {
                profiles.add(r.getProfileId());
            }
        }
        Set<Long> users = new LinkedHashSet<>();
        for (ProfileAssignment a : repo(ProfileAssignment.class).find(Filter.in("profileId", profiles))) {
            users.add(a.getUserId());
        }
        return users;
    }

    private static String hostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            return "pyhoma";
        }
    }
}
