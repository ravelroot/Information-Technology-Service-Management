package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetConnection;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.asset.ContractItem;
import io.pyhoma.domain.model.asset.SoftwareInstallation;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilAsset;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Csv;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AssetService extends AbstractService {

    public record AssetSearch(String text, AssetType type, Long stateId, Long locationId, Long userId,
                              boolean trash, Integer warrantyWithinDays, String sortField, boolean ascending,
                              int page, int size) {

        public static AssetSearch of(String text, AssetType type) {
            return new AssetSearch(text, type, null, null, null, false, null, "name", true, 0, 50);
        }

        public AssetSearch withSize(int newSize) {
            return new AssetSearch(text, type, stateId, locationId, userId, trash, warrantyWithinDays, sortField, ascending, 0, newSize);
        }

        public AssetSearch withPage(int newPage, int newSize) {
            return new AssetSearch(text, type, stateId, locationId, userId, trash, warrantyWithinDays, sortField, ascending, newPage, newSize);
        }
    }

    public record ImportResult(int created, int updated, List<String> errors) {
    }

    public AssetService(Services services) {
        super(services);
    }

    public PageResult<Asset> search(AssetSearch s) {
        require(Module.ASSET, Right.READ);
        List<Filter> parts = new ArrayList<>();
        parts.add(s.trash() ? Filter.isTrue("deleted") : Filter.isFalse("deleted"));
        parts.add(security().orgFilter());
        parts.add(Filter.search(s.text(), "name", "serialNumber", "assetTag", "hostname", "ipAddress", "macAddress", "notes"));
        if (s.type() != null) {
            parts.add(Filter.eq("assetType", s.type()));
        }
        if (s.stateId() != null) {
            parts.add(Filter.eq("stateId", s.stateId()));
        }
        if (s.locationId() != null) {
            parts.add(Filter.eq("locationId", s.locationId()));
        }
        if (s.userId() != null) {
            parts.add(Filter.eq("userId", s.userId()));
        }
        if (s.warrantyWithinDays() != null) {
            parts.add(Filter.between("warrantyExpiresOn", today(), today().plusDays(s.warrantyWithinDays())));
        }
        Query q = Query.where(Filter.and(parts)).orderBy(new Sort(s.sortField() == null ? "name" : s.sortField(), s.ascending()))
                .page(s.page(), s.size());
        return repo(Asset.class).page(q);
    }

    public Asset get(Long id) {
        require(Module.ASSET, Right.READ);
        Asset a = repo(Asset.class).getById(id);
        checkVisible(a);
        return a;
    }

    public List<Asset> ofUser(Long userId) {
        return repo(Asset.class).find(Query.where(Filter.and(Filter.eq("userId", userId), Filter.isFalse("deleted")))
                .orderBy(Sort.asc("name")));
    }

    public Asset save(Asset asset) {
        require(Module.ASSET, asset.isNew() ? Right.CREATE : Right.UPDATE);
        return saveInternal(asset);
    }

    Asset saveInternal(Asset asset) {
        asset.setName(Texts.clean(asset.getName()));
        asset.setSerialNumber(Texts.clean(asset.getSerialNumber()));
        asset.setAssetTag(Texts.clean(asset.getAssetTag()));
        asset.setMacAddress(asset.getMacAddress() == null ? null : Texts.clean(asset.getMacAddress().toUpperCase(Locale.ROOT)));
        if (asset.getOrgUnitId() == null) {
            asset.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create()
                .required("name", asset.getName()).maxLength("name", asset.getName(), 200)
                .required("assetType", asset.getAssetType())
                .ipAddress("ipAddress", asset.getIpAddress())
                .macAddress("macAddress", asset.getMacAddress())
                .positive("purchaseCost", asset.getPurchaseCost())
                .positive("ramMb", asset.getRamMb())
                .positive("storageGb", asset.getStorageGb())
                .check(asset.getPurchaseDate() == null || asset.getWarrantyExpiresOn() == null
                        || !asset.getWarrantyExpiresOn().isBefore(asset.getPurchaseDate()), "warrantyExpiresOn", "validation.endBeforeStart")
                .throwIfInvalid();
        return tx(() -> {
            if (asset.getAssetTag() != null) {
                boolean duplicate = repo(Asset.class).find(Filter.eq("assetTag", asset.getAssetTag())).stream()
                        .anyMatch(a -> !Objects.equals(a.getId(), asset.getId()));
                if (duplicate) {
                    throw new BusinessException("error.duplicateAssetTag", asset.getAssetTag());
                }
            }
            if (asset.isNew() && asset.getAssetTag() == null) {
                asset.setAssetTag(nextAssetTag());
            }
            return persist(asset);
        });
    }

    String nextAssetTag() {
        String prefix = services.settings().get(SettingKeys.ASSET_TAG_PREFIX, "PYH-");
        long next = repo(Asset.class).count(Filter.ALL) + 1;
        String tag;
        do {
            tag = prefix + String.format("%06d", next++);
        } while (repo(Asset.class).exists(Filter.eq("assetTag", tag)));
        return tag;
    }

    public Asset duplicate(Long id) {
        Asset source = get(id);
        Asset copy = copy(source);
        copy.setId(null);
        copy.setVersion(null);
        copy.setName(source.getName() + " (2)");
        copy.setSerialNumber(null);
        copy.setAssetTag(null);
        copy.setIpAddress(null);
        copy.setMacAddress(null);
        copy.setUserId(null);
        return save(copy);
    }

    public void trash(Long id) {
        require(Module.ASSET, Right.DELETE);
        txRun(() -> {
            Asset a = get(id);
            a.setDeleted(true);
            saveQuietly(a);
            services.audit().record(a, AuditAction.DELETE, null, null, null);
        });
    }

    public void restore(Long id) {
        require(Module.ASSET, Right.DELETE);
        txRun(() -> {
            Asset a = get(id);
            a.setDeleted(false);
            saveQuietly(a);
            services.audit().record(a, AuditAction.RESTORE, null, null, null);
        });
    }

    public void purgeAsset(Long id) {
        require(Module.ASSET, Right.PURGE);
        txRun(() -> {
            Asset a = get(id);
            repo(AssetConnection.class).deleteWhere(Filter.or(Filter.eq("assetId", id), Filter.eq("connectedAssetId", id)));
            repo(SoftwareInstallation.class).deleteWhere(Filter.eq("assetId", id));
            repo(ContractItem.class).deleteWhere(Filter.eq("assetId", id));
            repo(ItilAsset.class).deleteWhere(Filter.eq("assetId", id));
            purge(a);
        });
    }

    public List<Asset> connections(Long assetId) {
        List<Long> ids = new ArrayList<>();
        for (AssetConnection c : repo(AssetConnection.class).find(Filter.or(Filter.eq("assetId", assetId),
                Filter.eq("connectedAssetId", assetId)))) {
            ids.add(Objects.equals(c.getAssetId(), assetId) ? c.getConnectedAssetId() : c.getAssetId());
        }
        return repo(Asset.class).findAllById(ids);
    }

    public void connect(Long assetId, Long otherId) {
        require(Module.ASSET, Right.UPDATE);
        if (Objects.equals(assetId, otherId)) {
            throw new BusinessException("error.selfLink");
        }
        txRun(() -> {
            Asset a = get(assetId);
            Asset b = get(otherId);
            boolean exists = repo(AssetConnection.class).exists(Filter.or(
                    Filter.and(Filter.eq("assetId", assetId), Filter.eq("connectedAssetId", otherId)),
                    Filter.and(Filter.eq("assetId", otherId), Filter.eq("connectedAssetId", assetId))));
            if (exists) {
                throw new BusinessException("error.alreadyLinked");
            }
            AssetConnection c = new AssetConnection();
            c.setAssetId(assetId);
            c.setConnectedAssetId(otherId);
            saveQuietly(c);
            services.audit().record(a, AuditAction.LINK, "connection", null, b.getName());
            services.audit().record(b, AuditAction.LINK, "connection", null, a.getName());
        });
    }

    public void disconnect(Long assetId, Long otherId) {
        require(Module.ASSET, Right.UPDATE);
        txRun(() -> {
            repo(AssetConnection.class).deleteWhere(Filter.or(
                    Filter.and(Filter.eq("assetId", assetId), Filter.eq("connectedAssetId", otherId)),
                    Filter.and(Filter.eq("assetId", otherId), Filter.eq("connectedAssetId", assetId))));
            services.audit().record("Asset", assetId, AuditAction.UNLINK, "connection", String.valueOf(otherId), null);
        });
    }

    public Map<AssetType, Long> countByType() {
        Map<AssetType, Long> result = new java.util.EnumMap<>(AssetType.class);
        repo(Asset.class).countBy("assetType", Filter.and(Filter.isFalse("deleted"), security().orgFilter()))
                .forEach((k, v) -> result.put((AssetType) k, v));
        return result;
    }

    public Map<Long, Long> countByState() {
        Map<Long, Long> result = new HashMap<>();
        repo(Asset.class).countBy("stateId", Filter.and(Filter.isFalse("deleted"), security().orgFilter()))
                .forEach((k, v) -> result.put((Long) k, v));
        return result;
    }

    public static final List<String> CSV_COLUMNS = List.of("name", "assetType", "serialNumber", "assetTag", "state",
            "location", "manufacturer", "model", "userLogin", "hostname", "ipAddress", "macAddress", "operatingSystem",
            "osVersion", "cpu", "ramMb", "storageGb", "purchaseDate", "warrantyExpiresOn", "purchaseCost");

    public ImportResult importCsv(String csvText) {
        require(Module.ASSET, Right.CREATE);
        List<Map<String, String>> rows = Csv.parseWithHeader(csvText);
        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        int line = 1;
        for (Map<String, String> row : rows) {
            line++;
            try {
                boolean isNew = tx(() -> importRow(row));
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (BusinessException e) {
                errors.add(line + ": " + e.messageKey() + " " + java.util.Arrays.toString(e.args()));
            } catch (RuntimeException e) {
                errors.add(line + ": " + e.getMessage());
            }
        }
        return new ImportResult(created, updated, errors);
    }

    private boolean importRow(Map<String, String> row) {
        AssetType type;
        try {
            type = AssetType.valueOf(row.getOrDefault("assetType", "COMPUTER").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("error.invalidAssetType", row.get("assetType"));
        }
        String serial = Texts.clean(row.get("serialNumber"));
        Asset asset = serial == null ? new Asset() : repo(Asset.class)
                .findFirst(Filter.and(Filter.eq("serialNumber", serial), Filter.eq("assetType", type))).orElseGet(Asset::new);
        boolean isNew = asset.isNew();
        asset.setAssetType(type);
        asset.setName(row.get("name"));
        asset.setSerialNumber(serial);
        if (Texts.clean(row.get("assetTag")) != null) {
            asset.setAssetTag(row.get("assetTag"));
        }
        DropdownService dd = services.dropdowns();
        asset.setStateId(idOf(dd.findOrCreate(DropdownKind.ASSET_STATE, row.get("state"))));
        asset.setLocationId(idOf(dd.findOrCreate(DropdownKind.LOCATION, row.get("location"))));
        asset.setManufacturerId(idOf(dd.findOrCreate(DropdownKind.MANUFACTURER, row.get("manufacturer"))));
        asset.setModelId(idOf(dd.findOrCreate(DropdownKind.ASSET_MODEL, row.get("model"))));
        asset.setOperatingSystemId(idOf(dd.findOrCreate(DropdownKind.OPERATING_SYSTEM, row.get("operatingSystem"))));
        String login = Texts.clean(row.get("userLogin"));
        if (login != null) {
            asset.setUserId(repo(User.class).findFirst(Filter.eq("login", login.toLowerCase(Locale.ROOT)))
                    .map(User::getId).orElse(null));
        }
        asset.setHostname(Texts.clean(row.get("hostname")));
        asset.setIpAddress(Texts.clean(row.get("ipAddress")));
        asset.setMacAddress(Texts.clean(row.get("macAddress")));
        asset.setOsVersion(Texts.clean(row.get("osVersion")));
        asset.setCpu(Texts.clean(row.get("cpu")));
        asset.setRamMb(Csv.toInteger(row.get("ramMb")));
        asset.setStorageGb(Csv.toInteger(row.get("storageGb")));
        asset.setPurchaseDate(Csv.toDate(row.get("purchaseDate")));
        asset.setWarrantyExpiresOn(Csv.toDate(row.get("warrantyExpiresOn")));
        String cost = Texts.clean(row.get("purchaseCost"));
        asset.setPurchaseCost(cost == null ? null : new BigDecimal(cost.replace(",", ".")));
        asset.setLastInventoryAt(now());
        saveInternal(asset);
        return isNew;
    }

    private static Long idOf(io.pyhoma.domain.model.core.DropdownItem item) {
        return item == null ? null : item.getId();
    }

    public List<Asset> warrantyExpiring(int days) {
        LocalDate today = today();
        return repo(Asset.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter(),
                Filter.between("warrantyExpiresOn", today, today.plusDays(days)))).orderBy(Sort.asc("warrantyExpiresOn")));
    }
}
