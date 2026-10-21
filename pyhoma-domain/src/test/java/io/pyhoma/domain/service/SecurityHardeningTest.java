package io.pyhoma.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.security.PasswordHasher;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityHardeningTest {

    private TestFixture f;
    private Services s;

    @BeforeEach
    void setUp() {
        f = TestFixture.installed(false);
        s = f.services;
    }

    private Profile userAdminProfile() {
        Profile p = new Profile();
        p.setName("Gestor de contas");
        p.setInterfaceType(InterfaceType.STANDARD);
        Map<Module, Integer> rights = new EnumMap<>(Module.class);
        rights.put(Module.USER, Right.CRUD);
        rights.put(Module.PROFILE, Right.CRUD);
        rights.put(Module.TICKET, Right.READ);
        return s.profiles().save(p, rights);
    }

    @Test
    void passwordPolicyRejectsWeakChoices() {
        assertFalse(PasswordHasher.isAcceptable("Senha12345", null, null));
        assertFalse(PasswordHasher.isAcceptable("Password123", null, null));
        assertFalse(PasswordHasher.isAcceptable("Short1", null, null));
        assertFalse(PasswordHasher.isAcceptable("Maria.Silva2026", "maria.silva", null));
        assertFalse(PasswordHasher.isAcceptable("henrique-2026x", null, "Henrique Dias"));
        assertFalse(PasswordHasher.isAcceptable("a1a1a1a1a1a1", null, null));
        assertTrue(PasswordHasher.isAcceptable("Granite-Ocean-91", "admin", "Administrador"));
    }

    @Test
    void unknownUserAndWrongPasswordLookTheSame() {
        s.auth().logout();
        BusinessException unknown = assertThrows(BusinessException.class, () -> s.auth().login("nobody", "Whatever-2026"));
        BusinessException wrong = assertThrows(BusinessException.class, () -> s.auth().login("admin", "Whatever-2026"));
        assertEquals(unknown.messageKey(), wrong.messageKey());
    }

    @Test
    void oldHashesAreUpgradedOnLogin() throws Exception {
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        PBEKeySpec spec = new PBEKeySpec(TestFixture.ADMIN_PASSWORD.toCharArray(), salt, 210_000, 256);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        String legacy = "pbkdf2$210000$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
        s.security().runAsSystem(() -> {
            User admin = s.store().repository(User.class).findFirst(Filter.eq("login", "admin")).orElseThrow();
            admin.setPasswordHash(legacy);
            s.store().repository(User.class).save(admin);
        });
        f.loginAdmin();
        User admin = s.security().runAsSystem(() -> s.store().repository(User.class).findFirst(Filter.eq("login", "admin")).orElseThrow());
        assertFalse(PasswordHasher.needsRehash(admin.getPasswordHash()));
        assertTrue(PasswordHasher.verify(TestFixture.ADMIN_PASSWORD, admin.getPasswordHash()));
    }

    @Test
    void delegatedAdminsCannotEscalate() {
        Profile manager = userAdminProfile();
        User ana = f.user("ana", "Técnico");
        s.users().addAssignment(ana.getId(), manager.getId(), s.orgUnits().root().getId(), true);
        User bruno = f.user("bruno", "Técnico");
        Long superAdmin = f.profile("Super-Administrador").getId();
        Long adminId = s.security().runAsSystem(() -> s.store().repository(User.class)
                .findFirst(Filter.eq("login", "admin")).orElseThrow().getId());
        f.loginAs("ana");

        assertEquals("error.privilegeEscalation", assertThrows(BusinessException.class,
                () -> s.users().addAssignment(bruno.getId(), superAdmin, s.orgUnits().root().getId(), true)).messageKey());
        assertEquals("error.ownAccessChange", assertThrows(BusinessException.class,
                () -> s.users().addAssignment(ana.getId(), manager.getId(), s.orgUnits().root().getId(), false)).messageKey());
        assertEquals("error.privilegeEscalation", assertThrows(BusinessException.class,
                () -> s.users().resetPassword(adminId, "Fresh-Start-2026", false)).messageKey());

        Profile greedy = new Profile();
        greedy.setName("Ganancioso");
        greedy.setInterfaceType(InterfaceType.STANDARD);
        Map<Module, Integer> rights = new EnumMap<>(Module.class);
        rights.put(Module.SETTINGS, Right.UPDATE | Right.READ);
        assertEquals("error.privilegeEscalation", assertThrows(BusinessException.class,
                () -> s.profiles().save(greedy, rights)).messageKey());

        Profile protectedProfile = s.profiles().get(superAdmin);
        protectedProfile.setName("Renomeado");
        assertEquals("error.protectedProfile", assertThrows(BusinessException.class,
                () -> s.profiles().save(protectedProfile, null)).messageKey());
    }

    @Test
    void usersCannotClearTheirOwnForcedPasswordChange() {
        User tech = f.user("tech", "Técnico");
        s.users().resetPassword(tech.getId(), "Fresh-Start-2026", true);
        s.auth().logout();
        s.auth().login("tech", "Fresh-Start-2026");
        User me = s.users().get(tech.getId());
        me.setMustChangePassword(false);
        me.setFullName("Tech Renomeado");
        s.users().save(me);
        assertTrue(s.users().get(tech.getId()).isMustChangePassword());
        assertTrue(s.auth().mustChangePassword());
    }

    @Test
    void approvalsNeedSomeoneElse() {
        Ticket t = s.tickets().draft();
        t.setTitle("Acesso ao sistema financeiro");
        t.setContent("Liberar acesso");
        t.setUrgency(3);
        t.setImpact(3);
        Ticket saved = s.tickets().create(t);
        Long me = s.security().currentUserId();
        assertEquals("error.selfApproval", assertThrows(BusinessException.class,
                () -> s.itilActivity().requestApproval(ItilType.TICKET, saved.getId(), me, null)).messageKey());
    }

    @Test
    void sessionEndsWhenTheAccountIsDisabled() {
        User tech = f.user("tech", "Técnico");
        f.loginAs("tech");
        assertTrue(s.auth().validateSession());
        s.security().runAsSystem(() -> {
            User u = s.store().repository(User.class).getById(tech.getId());
            u.setActive(false);
            s.store().repository(User.class).save(u);
        });
        assertFalse(s.auth().validateSession());
        assertFalse(s.security().isAuthenticated());
    }

    @Test
    void securitySettingsHaveSafeBounds() {
        assertEquals("error.settingRange", assertThrows(BusinessException.class,
                () -> s.settings().set(SettingKeys.MAX_FAILED_LOGINS, "100")).messageKey());
        assertEquals("error.settingRange", assertThrows(BusinessException.class,
                () -> s.settings().set(SettingKeys.SESSION_IDLE_MINUTES, "0")).messageKey());
        s.settings().set(SettingKeys.SESSION_IDLE_MINUTES, "30");
        assertEquals(30, s.settings().getInt(SettingKeys.SESSION_IDLE_MINUTES, 15));
    }

    @Test
    void unsafeDocumentLinksAreRejected() {
        io.pyhoma.domain.model.core.Document d = new io.pyhoma.domain.model.core.Document();
        d.setName("Instalador");
        d.setUri("C:\\Temp\\setup.exe");
        assertEquals("error.unsafeDocument", assertThrows(BusinessException.class, () -> s.documents().save(d)).messageKey());
        d.setUri("https://intranet.example.com/manual.pdf");
        assertTrue(s.documents().save(d).getId() != null);
    }
}
