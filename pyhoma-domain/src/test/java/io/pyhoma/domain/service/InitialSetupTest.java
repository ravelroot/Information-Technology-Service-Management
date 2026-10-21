package io.pyhoma.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.UserGroup;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.support.BusinessException;
import org.junit.jupiter.api.Test;

class InitialSetupTest {

    @Test
    void realInstallationStartsEmptyAndRequiresTheBasics() {
        TestFixture f = TestFixture.installed(false);
        Services s = f.services;
        assertTrue(s.installation().setupPending());
        assertEquals(0, s.store().repository(Ticket.class).count(Filter.ALL));

        InstallationService.SetupStatus status = s.installation().setupStatus();
        assertFalse(status.ready());
        assertTrue(status.categories() > 0, "default ticket categories are created");
        assertEquals("setup.missingOrganization",
                assertThrows(BusinessException.class, () -> s.installation().completeSetup()).messageKey());

        OrgUnit root = s.orgUnits().root();
        root.setEmail("ti@empresa.example");
        s.orgUnits().save(root);
        assertEquals("setup.missingLocation",
                assertThrows(BusinessException.class, () -> s.installation().completeSetup()).messageKey());

        DropdownItem location = new DropdownItem();
        location.setKind(DropdownKind.LOCATION);
        location.setName("Matriz");
        s.dropdowns().save(location);
        assertEquals("setup.missingGroup",
                assertThrows(BusinessException.class, () -> s.installation().completeSetup()).messageKey());

        UserGroup group = new UserGroup();
        group.setName("Service Desk");
        group.setOrgUnitId(root.getId());
        s.groups().save(group);

        s.installation().completeSetup();
        assertFalse(s.installation().setupPending());
        assertTrue(s.installation().setupStatus().completed());
    }

    @Test
    void demoInstallationNeverAsksForSetup() {
        TestFixture f = TestFixture.installed(true);
        assertFalse(f.services.installation().setupPending());
    }
}
