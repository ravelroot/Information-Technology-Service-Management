package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.Software;
import io.pyhoma.domain.model.asset.SoftwareInstallation;
import io.pyhoma.domain.model.asset.SoftwareLicense;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SoftwareService extends AbstractService {

    public record LicenseUsage(SoftwareLicense license, String softwareName, long used) {

        public boolean unlimited() {
            return license.getSeats() == null;
        }

        public boolean overUsed() {
            return !unlimited() && used > license.getSeats();
        }

        public long available() {
            return unlimited() ? Long.MAX_VALUE : license.getSeats() - used;
        }
    }

    public record InstallationRow(SoftwareInstallation installation, String assetName, String softwareName, String licenseName) {
    }

    public SoftwareService(Services services) {
        super(services);
    }

    public PageResult<Software> search(String text, boolean trash, int page, int size) {
        require(Module.SOFTWARE, Right.READ);
        Filter f = Filter.and(trash ? Filter.isTrue("deleted") : Filter.isFalse("deleted"), security().orgFilter(),
                Filter.search(text, "name", "notes"));
        return repo(Software.class).page(Query.where(f).orderBy(Sort.asc("name")).page(page, size));
    }

    public Software get(Long id) {
        require(Module.SOFTWARE, Right.READ);
        Software s = repo(Software.class).getById(id);
        checkVisible(s);
        return s;
    }

    public Software save(Software software) {
        require(Module.SOFTWARE, software.isNew() ? Right.CREATE : Right.UPDATE);
        software.setName(Texts.clean(software.getName()));
        if (software.getOrgUnitId() == null) {
            software.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("name", software.getName()).throwIfInvalid();
        return tx(() -> persist(software));
    }

    public void trash(Long id, boolean deleted) {
        require(Module.SOFTWARE, Right.DELETE);
        txRun(() -> {
            Software s = get(id);
            s.setDeleted(deleted);
            saveQuietly(s);
            services.audit().record(s, deleted ? AuditAction.DELETE : AuditAction.RESTORE, null, null, null);
        });
    }

    public void purgeSoftware(Long id) {
        require(Module.SOFTWARE, Right.PURGE);
        txRun(() -> {
            Software s = get(id);
            repo(SoftwareInstallation.class).deleteWhere(Filter.eq("softwareId", id));
            repo(SoftwareLicense.class).deleteWhere(Filter.eq("softwareId", id));
            purge(s);
        });
    }

    public List<SoftwareLicense> licenses(Long softwareId) {
        return repo(SoftwareLicense.class).find(Query.where(Filter.eq("softwareId", softwareId)).orderBy(Sort.asc("name")));
    }

    public SoftwareLicense saveLicense(SoftwareLicense license) {
        require(Module.SOFTWARE, Right.UPDATE);
        license.setName(Texts.clean(license.getName()));
        Validator.create().required("name", license.getName()).required("softwareId", license.getSoftwareId())
                .positive("seats", license.getSeats()).positive("purchaseCost", license.getPurchaseCost()).throwIfInvalid();
        return tx(() -> {
            Software s = get(license.getSoftwareId());
            license.setOrgUnitId(s.getOrgUnitId());
            return persist(license);
        });
    }

    public void deleteLicense(Long licenseId) {
        require(Module.SOFTWARE, Right.DELETE);
        txRun(() -> {
            SoftwareLicense l = repo(SoftwareLicense.class).getById(licenseId);
            for (SoftwareInstallation i : repo(SoftwareInstallation.class).find(Filter.eq("licenseId", licenseId))) {
                i.setLicenseId(null);
                saveQuietly(i);
            }
            purge(l);
        });
    }

    public List<LicenseUsage> compliance() {
        require(Module.SOFTWARE, Right.READ);
        List<LicenseUsage> result = new ArrayList<>();
        var names = services.lookups().names(Software.class);
        for (SoftwareLicense l : repo(SoftwareLicense.class).find(Query.where(security().orgFilter()).orderBy(Sort.asc("name")))) {
            long used = repo(SoftwareInstallation.class).count(Filter.eq("licenseId", l.getId()));
            result.add(new LicenseUsage(l, names.get(l.getSoftwareId()), used));
        }
        return result;
    }

    public List<SoftwareLicense> expiringLicenses(int days) {
        LocalDate today = today();
        return repo(SoftwareLicense.class).find(Query.where(Filter.and(security().orgFilter(),
                Filter.between("expiresOn", today, today.plusDays(days)))).orderBy(Sort.asc("expiresOn")));
    }

    public List<InstallationRow> installationsOfAsset(Long assetId) {
        return rows(repo(SoftwareInstallation.class).find(Filter.eq("assetId", assetId)));
    }

    public List<InstallationRow> installationsOfSoftware(Long softwareId) {
        return rows(repo(SoftwareInstallation.class).find(Filter.eq("softwareId", softwareId)));
    }

    private List<InstallationRow> rows(List<SoftwareInstallation> installs) {
        var assets = services.lookups().names(Asset.class, installs.stream().map(SoftwareInstallation::getAssetId).toList());
        var software = services.lookups().names(Software.class, installs.stream().map(SoftwareInstallation::getSoftwareId).toList());
        var licenses = services.lookups().names(SoftwareLicense.class, installs.stream().map(SoftwareInstallation::getLicenseId).toList());
        return installs.stream().map(i -> new InstallationRow(i, assets.get(i.getAssetId()), software.get(i.getSoftwareId()),
                i.getLicenseId() == null ? null : licenses.get(i.getLicenseId()))).toList();
    }

    public SoftwareInstallation install(Long assetId, Long softwareId, String version, Long licenseId) {
        require(Module.SOFTWARE, Right.UPDATE);
        return tx(() -> {
            Asset asset = services.assets().get(assetId);
            get(softwareId);
            if (repo(SoftwareInstallation.class).exists(Filter.and(Filter.eq("assetId", assetId), Filter.eq("softwareId", softwareId),
                    Filter.eq("softwareVersion", Texts.clean(version))))) {
                throw new BusinessException("error.alreadyInstalled");
            }
            if (licenseId != null) {
                SoftwareLicense l = repo(SoftwareLicense.class).getById(licenseId);
                if (!Objects.equals(l.getSoftwareId(), softwareId)) {
                    throw new BusinessException("error.licenseMismatch");
                }
            }
            SoftwareInstallation i = new SoftwareInstallation();
            i.setAssetId(assetId);
            i.setSoftwareId(softwareId);
            i.setSoftwareVersion(Texts.clean(version));
            i.setLicenseId(licenseId);
            i.setInstalledOn(today());
            SoftwareInstallation saved = insert(i);
            services.audit().record(asset, AuditAction.LINK, "software", null, services.lookups().names(Software.class).get(softwareId));
            return saved;
        });
    }

    public void uninstall(Long installationId) {
        require(Module.SOFTWARE, Right.UPDATE);
        txRun(() -> purge(repo(SoftwareInstallation.class).getById(installationId)));
    }
}
