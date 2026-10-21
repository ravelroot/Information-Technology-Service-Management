package io.pyhoma.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.AccessDeniedException;
import io.pyhoma.domain.support.BusinessException;
import org.junit.jupiter.api.Test;

class SecurityAndAdminTest {

    @Test
    void installationCreatesAdminWithAllRights() {
        TestFixture f = TestFixture.installed(false);
        var session = f.services.security().current();
        assertEquals("admin", session.login());
        assertTrue(session.can(Module.SETTINGS, Right.UPDATE));
        assertEquals(InterfaceType.STANDARD, session.interfaceType());
        assertTrue(f.services.installation().isInstalled());
        assertThrows(BusinessException.class, () -> f.services.installation().install(new InstallationService.InstallRequest(
                "X", "other", "Other", null, "Blue-River-2026", "en", false)));
    }

    @Test
    void wrongPasswordLocksAccount() {
        TestFixture f = TestFixture.installed(false);
        f.services.auth().logout();
        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class, () -> f.services.auth().login("admin", "nope"));
        }
        BusinessException locked = assertThrows(BusinessException.class, () -> f.services.auth().login("admin", TestFixture.ADMIN_PASSWORD));
        assertEquals("error.invalidCredentials", locked.messageKey());
        f.clock.advance(java.time.Duration.ofMinutes(16));
        f.services.auth().login("admin", TestFixture.ADMIN_PASSWORD);
    }

    @Test
    void selfServiceCannotCreateAssets() {
        TestFixture f = TestFixture.installed(false);
        f.user("joana", "Autoatendimento");
        f.loginAs("joana");
        assertTrue(f.services.security().current().isSelfService());
        Asset a = new Asset();
        a.setName("PC");
        a.setAssetType(AssetType.COMPUTER);
        assertThrows(AccessDeniedException.class, () -> f.services.assets().save(a));
    }

    @Test
    void orgUnitVisibilityRestrictsData() {
        TestFixture f = TestFixture.installed(false);
        OrgUnit branch = new OrgUnit();
        branch.setName("Branch");
        branch.setParentId(f.services.orgUnits().root().getId());
        branch = f.services.orgUnits().save(branch);
        assertEquals("Acme > Branch", branch.getCompleteName());
        Asset rootAsset = new Asset();
        rootAsset.setName("Root PC");
        rootAsset.setAssetType(AssetType.COMPUTER);
        f.services.assets().save(rootAsset);
        var u = f.user("branchtech", "Técnico");
        f.services.users().assignments(u.getId()).forEach(a -> f.services.users().removeAssignment(a.getId()));
        f.services.users().addAssignment(u.getId(), f.profile("Técnico").getId(), branch.getId(), true);
        f.loginAs("branchtech");
        assertEquals(branch.getId(), f.services.security().current().activeOrgUnitId());
        assertEquals(0, f.services.assets().search(AssetService.AssetSearch.of(null, null)).total());
        Asset branchAsset = new Asset();
        branchAsset.setName("Branch PC");
        branchAsset.setAssetType(AssetType.LAPTOP);
        branchAsset = f.services.assets().save(branchAsset);
        assertEquals(branch.getId(), branchAsset.getOrgUnitId());
        assertTrue(branchAsset.getAssetTag().startsWith("PYH-"));
        assertEquals(1, f.services.assets().search(AssetService.AssetSearch.of(null, null)).total());
    }

    @Test
    void auditTrailRecordsChanges() {
        TestFixture f = TestFixture.installed(false);
        Asset a = new Asset();
        a.setName("PC-1");
        a.setAssetType(AssetType.COMPUTER);
        a = f.services.assets().save(a);
        a.setName("PC-2");
        a.setIpAddress("10.0.0.5");
        f.services.assets().save(a);
        var history = f.services.audit().history("Asset", a.getId(), 0, 50).items();
        assertTrue(history.stream().anyMatch(h -> "name".equals(h.getFieldName()) && "PC-2".equals(h.getNewValue())));
        assertTrue(history.stream().anyMatch(h -> "ipAddress".equals(h.getFieldName())));
        assertFalse(history.stream().anyMatch(h -> "updatedAt".equals(h.getFieldName())));
    }

    @Test
    void invalidAssetDataIsRejected() {
        TestFixture f = TestFixture.installed(false);
        Asset a = new Asset();
        a.setName("PC");
        a.setAssetType(AssetType.COMPUTER);
        a.setIpAddress("999.1.1.1");
        var e = assertThrows(io.pyhoma.domain.support.ValidationException.class, () -> f.services.assets().save(a));
        assertTrue(e.fieldErrors().containsKey("ipAddress"));
    }
}
