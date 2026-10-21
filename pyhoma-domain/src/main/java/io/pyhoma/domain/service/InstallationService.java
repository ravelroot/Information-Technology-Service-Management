package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.Holiday;
import io.pyhoma.domain.model.itil.Sla;
import io.pyhoma.domain.model.itil.SlaType;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Validator;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

public class InstallationService extends AbstractService {

    public record InstallRequest(String organizationName, String adminLogin, String adminFullName, String adminEmail,
                                 String adminPassword, String language, boolean demoData) {

        @Override
        public String toString() {
            return "InstallRequest[" + organizationName + ", " + adminLogin + ", demo=" + demoData + "]";
        }
    }

    public record SetupStatus(boolean organization, long locations, long groups, long categories, long users, boolean completed) {

        public boolean ready() {
            return organization && locations > 0 && groups > 0;
        }
    }

    public InstallationService(Services services) {
        super(services);
    }

    public boolean isInstalled() {
        return security().runAsSystem(() -> repo(User.class).count(Filter.ALL) > 0);
    }

    public void install(InstallRequest request) {
        Validator.create().required("organizationName", request.organizationName())
                .required("adminLogin", request.adminLogin())
                .required("adminFullName", request.adminFullName())
                .email("adminEmail", request.adminEmail())
                .throwIfInvalid();
        boolean pt = request.language() == null || request.language().toLowerCase().startsWith("pt");
        security().runAsSystem(() -> txRun(() -> {
            if (repo(User.class).count(Filter.ALL) > 0) {
                throw new BusinessException("error.alreadyInstalled");
            }
            OrgUnit root = new OrgUnit();
            root.setName(request.organizationName().trim());
            root.setCompleteName(root.getName());
            root.setDepth(0);
            root = insert(root);

            Profile superAdmin = profile(pt ? "Super-Administrador" : "Super-Admin", InterfaceType.STANDARD, true, false,
                    all(Right.ALL));
            profile(pt ? "Supervisor" : "Supervisor", InterfaceType.STANDARD, false, false, supervisor());
            profile(pt ? "Técnico" : "Technician", InterfaceType.STANDARD, false, false, technician());
            profile(pt ? "Observador" : "Observer", InterfaceType.STANDARD, false, false, all(Right.READ));
            profile(pt ? "Autoatendimento" : "Self-Service", InterfaceType.SELF_SERVICE, false, true, selfService());

            User admin = new User();
            admin.setLogin(request.adminLogin());
            admin.setFullName(request.adminFullName());
            admin.setEmail(request.adminEmail());
            admin.setLanguage(pt ? "pt-BR" : "en");
            admin.setDefaultOrgUnitId(root.getId());
            admin = services.users().createInternal(admin, request.adminPassword());
            services.users().addAssignmentInternal(admin.getId(), superAdmin.getId(), root.getId(), true);

            defaultDropdowns(pt);
            defaultSlas(root.getId(), pt);
            defaultHolidays(pt);
            SettingsService s = services.settings();
            s.setInternal(SettingKeys.ORGANIZATION_NAME, root.getName());
            s.setInternal(SettingKeys.INSTALLED_VERSION, Services.VERSION);
            s.setInternal(SettingKeys.INSTALLED_AT, now().toString());
            s.setInternal(SettingKeys.CALENDAR_WORK_DAYS, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
            s.setInternal(SettingKeys.CALENDAR_DAY_START, "08:00");
            s.setInternal(SettingKeys.CALENDAR_DAY_END, "18:00");
            s.setInternal(SettingKeys.TICKET_AUTO_CLOSE_DAYS, "7");
            s.setInternal(SettingKeys.TICKET_SATISFACTION, "true");
            s.setInternal(SettingKeys.SLA_WARNING_PERCENT, "25");
            s.setInternal(SettingKeys.EXPIRATION_ALERT_DAYS, "30");
            s.setInternal(SettingKeys.NOTIFICATION_RETENTION_DAYS, "90");
            s.setInternal(SettingKeys.MAX_FAILED_LOGINS, "5");
            s.setInternal(SettingKeys.LOCK_MINUTES, "15");
            s.setInternal(SettingKeys.SESSION_IDLE_MINUTES, "15");
            s.setInternal(SettingKeys.ASSET_TAG_PREFIX, "PYH-");
        }));
        services.slas().invalidateCalendar();
        if (request.demoData()) {
            services.demoData().generate(pt);
            security().runAsSystem(() -> services.settings().setInternal(SettingKeys.SETUP_COMPLETED, "true"));
        }
    }

    public boolean setupPending() {
        return !services.settings().getBoolean(SettingKeys.SETUP_COMPLETED, false);
    }

    public SetupStatus setupStatus() {
        require(Module.SETTINGS, Right.READ);
        OrgUnit root = services.orgUnits().root();
        boolean organization = root.getName() != null && !root.getName().isBlank() && root.getEmail() != null
                && !root.getEmail().isBlank();
        long locations = repo(DropdownItem.class).count(Filter.and(Filter.eq("kind", DropdownKind.LOCATION), Filter.isTrue("active")));
        long categories = repo(DropdownItem.class).count(Filter.and(Filter.eq("kind", DropdownKind.ITIL_CATEGORY),
                Filter.isTrue("active")));
        long groups = repo(io.pyhoma.domain.model.core.UserGroup.class).count(Filter.isTrue("assignable"));
        long users = repo(User.class).count(Filter.isTrue("active"));
        return new SetupStatus(organization, locations, groups, categories, users, !setupPending());
    }

    public void completeSetup() {
        require(Module.SETTINGS, Right.UPDATE);
        SetupStatus status = setupStatus();
        if (!status.organization()) {
            throw new BusinessException("setup.missingOrganization");
        }
        if (status.locations() == 0) {
            throw new BusinessException("setup.missingLocation");
        }
        if (status.groups() == 0) {
            throw new BusinessException("setup.missingGroup");
        }
        services.settings().set(SettingKeys.SETUP_COMPLETED, "true");
    }

    private Profile profile(String name, InterfaceType type, boolean protect, boolean isDefault, Map<Module, Integer> rights) {
        Profile p = new Profile();
        p.setName(name);
        p.setInterfaceType(type);
        p.setProtectedProfile(protect);
        p.setDefaultProfile(isDefault);
        return services.profiles().saveInternal(p, rights);
    }

    private static Map<Module, Integer> all(int mask) {
        Map<Module, Integer> m = new EnumMap<>(Module.class);
        for (Module module : Module.values()) {
            m.put(module, mask & module.allowedRights());
        }
        return m;
    }

    private static Map<Module, Integer> supervisor() {
        Map<Module, Integer> m = all(Right.ALL);
        for (Module module : Module.values()) {
            if (module.section() == Module.Section.ADMINISTRATION) {
                m.put(module, Right.READ);
            }
        }
        m.put(Module.GROUP, Right.CRUD);
        m.put(Module.RULE, Right.CRUD);
        m.put(Module.SLA, Right.CRUD);
        return m;
    }

    private static Map<Module, Integer> technician() {
        Map<Module, Integer> m = new EnumMap<>(Module.class);
        m.put(Module.TICKET, Right.CRUD | Right.ASSIGN | Right.SEE_ALL);
        m.put(Module.PROBLEM, Right.CRUD);
        m.put(Module.CHANGE, Right.READ | Right.CREATE | Right.UPDATE);
        m.put(Module.RECURRING_TICKET, Right.READ);
        m.put(Module.ASSET, Right.READ | Right.CREATE | Right.UPDATE);
        m.put(Module.SOFTWARE, Right.READ | Right.CREATE | Right.UPDATE);
        m.put(Module.CONTRACT, Right.READ);
        m.put(Module.SUPPLIER, Right.READ);
        m.put(Module.DOCUMENT, Right.READ | Right.CREATE | Right.UPDATE);
        m.put(Module.PROJECT, Right.READ | Right.UPDATE);
        m.put(Module.KNOWLEDGE_BASE, Right.READ | Right.CREATE | Right.UPDATE);
        m.put(Module.REPORT, Right.READ);
        m.put(Module.USER, Right.READ);
        m.put(Module.GROUP, Right.READ);
        return m;
    }

    private static Map<Module, Integer> selfService() {
        Map<Module, Integer> m = new EnumMap<>(Module.class);
        m.put(Module.TICKET, Right.READ | Right.CREATE);
        m.put(Module.KNOWLEDGE_BASE, Right.READ);
        return m;
    }

    private void defaultDropdowns(boolean pt) {
        DropdownService d = services.dropdowns();
        int i = 0;
        for (String s : pt ? new String[] {"Telefone", "E-mail", "Portal", "Presencial", "Chat"}
                : new String[] {"Phone", "E-mail", "Portal", "Walk-in", "Chat"}) {
            d.create(DropdownKind.REQUEST_SOURCE, s, null, null, false, i++);
        }
        String[][] states = pt
                ? new String[][] {{"Em uso", "#16a34a"}, {"Em estoque", "#2563eb"}, {"Em manutenção", "#d97706"},
                    {"Emprestado", "#7c3aed"}, {"Descartado", "#64748b"}}
                : new String[][] {{"In use", "#16a34a"}, {"In stock", "#2563eb"}, {"Under repair", "#d97706"},
                    {"On loan", "#7c3aed"}, {"Disposed", "#64748b"}};
        i = 0;
        for (String[] s : states) {
            d.create(DropdownKind.ASSET_STATE, s[0], null, s[1], i == 0, i++);
        }
        String[][] projectStates = pt
                ? new String[][] {{"Planejado", "#64748b", "0"}, {"Em andamento", "#2563eb", "0"}, {"Em pausa", "#d97706", "0"},
                    {"Concluído", "#16a34a", "1"}, {"Cancelado", "#dc2626", "1"}}
                : new String[][] {{"Planned", "#64748b", "0"}, {"In progress", "#2563eb", "0"}, {"On hold", "#d97706", "0"},
                    {"Completed", "#16a34a", "1"}, {"Canceled", "#dc2626", "1"}};
        i = 0;
        for (String[] s : projectStates) {
            d.create(DropdownKind.PROJECT_STATE, s[0], null, s[1], "1".equals(s[2]), i++);
        }
        simple(DropdownKind.SOLUTION_TYPE, pt ? new String[] {"Resolvido remotamente", "Atendimento no local",
                "Substituição de equipamento", "Orientação ao usuário", "Solução de contorno"}
                : new String[] {"Solved remotely", "On-site intervention", "Hardware replacement", "User guidance", "Workaround"});
        simple(DropdownKind.TASK_CATEGORY, pt ? new String[] {"Diagnóstico", "Instalação", "Configuração",
                "Atendimento em campo", "Documentação"}
                : new String[] {"Diagnosis", "Installation", "Configuration", "Field work", "Documentation"});
        simple(DropdownKind.CONTRACT_TYPE, pt ? new String[] {"Manutenção", "Suporte", "Locação", "Licenciamento", "Garantia estendida"}
                : new String[] {"Maintenance", "Support", "Leasing", "Licensing", "Extended warranty"});
        simple(DropdownKind.SUPPLIER_TYPE, pt ? new String[] {"Fabricante", "Revenda", "Prestador de serviços", "Operadora"}
                : new String[] {"Manufacturer", "Reseller", "Service provider", "Carrier"});
        simple(DropdownKind.DOCUMENT_CATEGORY, pt ? new String[] {"Manual", "Contrato", "Nota fiscal", "Procedimento"}
                : new String[] {"Manual", "Contract", "Invoice", "Procedure"});
        simple(DropdownKind.SOFTWARE_CATEGORY, pt ? new String[] {"Produtividade", "Segurança", "Desenvolvimento", "Comunicação", "Sistema"}
                : new String[] {"Productivity", "Security", "Development", "Communication", "System"});
        simple(DropdownKind.OPERATING_SYSTEM, new String[] {"Windows 11", "Windows 10", "Windows Server 2022", "Ubuntu 24.04 LTS",
                "macOS", "Android", "iOS"});
        String[][] categories = pt
                ? new String[][] {{"Hardware", "Computador", "Notebook", "Impressora", "Periféricos"},
                    {"Software", "Sistema operacional", "E-mail", "Office", "Sistemas internos"},
                    {"Rede", "Internet", "Wi-Fi", "VPN", "Telefonia"},
                    {"Acessos", "Senha", "Permissões", "Novo usuário"}}
                : new String[][] {{"Hardware", "Desktop", "Laptop", "Printer", "Peripherals"},
                    {"Software", "Operating system", "E-mail", "Office", "Internal systems"},
                    {"Network", "Internet", "Wi-Fi", "VPN", "Telephony"},
                    {"Access", "Password", "Permissions", "New user"}};
        String[] categoryColors = {"#6B6262", "#000000", "#AB9D9D", "#8A2F2F"};
        i = 0;
        for (String[] group : categories) {
            String color = categoryColors[i % categoryColors.length];
            DropdownItem parent = d.create(DropdownKind.ITIL_CATEGORY, group[0], null, color, false, i++);
            for (int j = 1; j < group.length; j++) {
                d.create(DropdownKind.ITIL_CATEGORY, group[j], parent.getId(), color, false, j);
            }
        }
        simple(DropdownKind.KB_CATEGORY, pt ? new String[] {"Primeiros passos", "Procedimentos de TI", "Perguntas frequentes"}
                : new String[] {"Getting started", "IT procedures", "Frequently asked questions"});
    }

    private void simple(DropdownKind kind, String[] names) {
        int i = 0;
        for (String n : names) {
            services.dropdowns().create(kind, n, null, null, false, i++);
        }
    }

    private void defaultSlas(Long rootId, boolean pt) {
        sla(rootId, pt ? "Atendimento padrão (2 h)" : "Standard response (2 h)", SlaType.TTO, 120, null);
        sla(rootId, pt ? "Atendimento crítico (30 min)" : "Critical response (30 min)", SlaType.TTO, 30, 5);
        sla(rootId, pt ? "Atendimento grave (15 min)" : "Major response (15 min)", SlaType.TTO, 15, 6);
        int[][] ttr = {{1, 40 * 60}, {2, 24 * 60}, {3, 16 * 60}, {4, 8 * 60}, {5, 4 * 60}, {6, 2 * 60}};
        for (int[] t : ttr) {
            String label = (pt ? "Resolução P" : "Resolution P") + t[0] + " (" + (t[1] / 60) + " h)";
            sla(rootId, label, SlaType.TTR, t[1], t[0]);
        }
    }

    private void sla(Long orgUnitId, String name, SlaType type, int minutes, Integer priority) {
        Sla s = new Sla();
        s.setOrgUnitId(orgUnitId);
        s.setName(name);
        s.setSlaType(type);
        s.setDurationMinutes(minutes);
        s.setPriority(priority);
        s.setBusinessHours(true);
        s.setActive(true);
        insert(s);
    }

    private void defaultHolidays(boolean pt) {
        String[][] days = pt
                ? new String[][] {{"01-01", "Confraternização Universal"}, {"04-21", "Tiradentes"},
                    {"05-01", "Dia do Trabalho"}, {"09-07", "Independência do Brasil"}, {"10-12", "Nossa Senhora Aparecida"},
                    {"11-02", "Finados"}, {"11-15", "Proclamação da República"}, {"11-20", "Dia da Consciência Negra"},
                    {"12-25", "Natal"}}
                : new String[][] {{"01-01", "New Year's Day"}, {"12-25", "Christmas Day"}};
        int year = today().getYear();
        for (String[] d : days) {
            Holiday h = new Holiday();
            h.setName(d[1]);
            h.setHolidayDate(LocalDate.parse(year + "-" + d[0]));
            h.setRecurring(true);
            insert(h);
        }
    }
}
