package io.pyhoma.domain.service;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetConnection;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.asset.ContractItem;
import io.pyhoma.domain.model.asset.RenewalType;
import io.pyhoma.domain.model.asset.Software;
import io.pyhoma.domain.model.asset.SoftwareInstallation;
import io.pyhoma.domain.model.asset.SoftwareLicense;
import io.pyhoma.domain.model.asset.Supplier;
import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.GroupMember;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.core.UserGroup;
import io.pyhoma.domain.model.itil.Change;
import io.pyhoma.domain.model.itil.ChangeType;
import io.pyhoma.domain.model.itil.ItilAsset;
import io.pyhoma.domain.model.itil.ItilFollowup;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilRelation;
import io.pyhoma.domain.model.itil.ItilSolution;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilActorRole;
import io.pyhoma.domain.model.itil.ItilObserver;
import io.pyhoma.domain.model.itil.ItilTask;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Periodicity;
import io.pyhoma.domain.model.itil.Problem;
import io.pyhoma.domain.model.itil.RecurringTicket;
import io.pyhoma.domain.model.itil.RelationType;
import io.pyhoma.domain.model.itil.RiskLevel;
import io.pyhoma.domain.model.itil.SolutionStatus;
import io.pyhoma.domain.model.itil.TaskStatus;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.model.kb.KbArticle;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.model.project.ProjectMember;
import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskDependency;
import io.pyhoma.domain.model.project.ProjectTaskStatus;
import io.pyhoma.domain.model.rules.Rule;
import io.pyhoma.domain.model.rules.RuleAction;
import io.pyhoma.domain.model.rules.RuleActionType;
import io.pyhoma.domain.model.rules.RuleCriterion;
import io.pyhoma.domain.model.rules.RuleField;
import io.pyhoma.domain.model.rules.RuleOperator;
import io.pyhoma.domain.model.rules.RuleTrigger;
import io.pyhoma.domain.itil.PriorityMatrix;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.rules.RuleDefinition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DemoDataService extends AbstractService {

    private Random random;
    private boolean pt;

    public DemoDataService(Services services) {
        super(services);
    }

    private String t(String portuguese, String english) {
        return pt ? portuguese : english;
    }

    public void generate(boolean portuguese) {
        this.pt = portuguese;
        this.random = new Random(42);
        security().runAsSystem(() -> txRun(this::generateAll));
    }

    private <T extends BaseEntity> T put(T entity, LocalDateTime at) {
        entity.setCreatedAt(at);
        entity.setUpdatedAt(at);
        return repo(entityClass(entity)).save(entity);
    }

    private <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private void actor(Long ticketId, Long userId, ItilActorRole role, LocalDateTime at) {
        ItilObserver o = new ItilObserver();
        o.setItemType(ItilType.TICKET);
        o.setItemId(ticketId);
        o.setUserId(userId);
        o.setActorRole(role);
        put(o, at);
    }

    private Long dd(DropdownKind kind, String name) {
        return services.dropdowns().findOrCreate(kind, name).getId();
    }

    private void generateAll() {
        LocalDateTime now = now();
        OrgUnit root = services.orgUnits().root();
        OrgUnit hq = put(unit(t("Sede São Paulo", "Headquarters"), root), now.minusDays(120));
        OrgUnit branch = put(unit(t("Filial Rio de Janeiro", "Branch office"), root), now.minusDays(120));

        Long locHq = dd(DropdownKind.LOCATION, t("São Paulo", "Main campus"));
        Long locBranch = dd(DropdownKind.LOCATION, t("Rio de Janeiro", "Branch campus"));
        List<Long> locations = new ArrayList<>();
        for (String floor : List.of(t("1º andar", "1st floor"), t("2º andar", "2nd floor"), t("Datacenter", "Datacenter"))) {
            DropdownItem d = new DropdownItem();
            d.setKind(DropdownKind.LOCATION);
            d.setName(floor);
            d.setParentId(locHq);
            locations.add(services.dropdowns().saveInternal(d).getId());
        }
        DropdownItem rioFloor = new DropdownItem();
        rioFloor.setKind(DropdownKind.LOCATION);
        rioFloor.setName(t("Térreo", "Ground floor"));
        rioFloor.setParentId(locBranch);
        locations.add(services.dropdowns().saveInternal(rioFloor).getId());

        String[] names = {"Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Rocha", "Eduarda Alves", "Felipe Costa",
            "Gabriela Nunes", "Henrique Dias", "Isabela Martins", "João Pereira", "Larissa Gomes", "Marcos Ribeiro",
            "Natália Carvalho", "Otávio Fernandes", "Paula Teixeira", "Rafael Barros"};
        String[] titles = pt
                ? new String[] {"Analista de Suporte N1", "Analista de Suporte N1", "Analista de Suporte N2", "Analista de Infraestrutura",
                    "Coordenadora de TI", "Analista de Redes", "Desenvolvedora", "Analista Financeiro", "Gerente Comercial",
                    "Assistente Administrativo", "Analista de RH", "Comprador", "Analista de Marketing", "Contador",
                    "Recepcionista", "Diretor de Operações"}
                : new String[] {"Service Desk Analyst", "Service Desk Analyst", "Support Engineer", "Infrastructure Engineer",
                    "IT Manager", "Network Engineer", "Developer", "Financial Analyst", "Sales Manager",
                    "Administrative Assistant", "HR Analyst", "Buyer", "Marketing Analyst", "Accountant",
                    "Receptionist", "Operations Director"};
        Map<String, Profile> profiles = new java.util.HashMap<>();
        services.profiles().all().forEach(p -> profiles.put(p.getName(), p));
        Profile tech = profiles.get(t("Técnico", "Technician"));
        Profile supervisor = profiles.get("Supervisor");
        Profile self = profiles.get(t("Autoatendimento", "Self-Service"));
        List<User> users = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            User u = new User();
            String login = java.text.Normalizer.normalize(names[i], java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                    .toLowerCase().replace(' ', '.');
            u.setLogin(login);
            u.setFullName(names[i]);
            u.setEmail(login + "@example.com");
            u.setJobTitle(titles[i]);
            u.setPhone("(11) 4000-" + (1000 + i * 37));
            u.setLanguage(pt ? "pt-BR" : "en");
            u.setDefaultOrgUnitId(i % 4 == 3 ? branch.getId() : hq.getId());
            u.setLocationId(i % 4 == 3 ? locations.get(3) : locations.get(i % 2));
            u.setActive(true);
            u = put(u, now.minusDays(110));
            users.add(u);
            Profile p = i < 4 || i == 5 || i == 6 ? tech : (i == 4 ? supervisor : self);
            ProfileAssignment a = new ProfileAssignment();
            a.setUserId(u.getId());
            a.setProfileId(p.getId());
            a.setOrgUnitId(root.getId());
            a.setIncludeChildren(true);
            put(a, now.minusDays(110));
        }
        List<User> technicians = List.of(users.get(0), users.get(1), users.get(2), users.get(3), users.get(5));
        List<User> requesters = users.subList(7, users.size());

        UserGroup desk = group(t("Service Desk (N1)", "Service Desk (L1)"), root.getId(), now);
        UserGroup infra = group(t("Infraestrutura (N2)", "Infrastructure (L2)"), root.getId(), now);
        UserGroup dev = group(t("Sistemas", "Applications"), root.getId(), now);
        member(desk, users.get(0), false, now);
        member(desk, users.get(1), false, now);
        member(desk, users.get(4), true, now);
        User installer = repo(User.class).find(io.pyhoma.domain.query.Query.all()
                .orderBy(io.pyhoma.domain.query.Sort.asc("id")).limit(1)).get(0);
        member(desk, installer, false, now);
        LocalDateTime weekStart = now.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        member(infra, users.get(2), false, now);
        member(infra, users.get(3), false, now);
        member(infra, users.get(5), false, now);
        member(infra, users.get(4), true, now);
        member(dev, users.get(6), false, now);
        member(dev, users.get(4), true, now);

        Long reseller = dd(DropdownKind.SUPPLIER_TYPE, t("Revenda", "Reseller"));
        Long provider = dd(DropdownKind.SUPPLIER_TYPE, t("Prestador de serviços", "Service provider"));
        Supplier s1 = supplier("Nimbus Tecnologia", reseller, "contato@nimbus.example.com", t("São Paulo", "Springfield"), now);
        Supplier s2 = supplier("Orion Redes e Telecom", provider, "suporte@orion.example.com", t("Rio de Janeiro", "Riverside"), now);
        Supplier s3 = supplier("Vetor Impressão Digital", provider, "atendimento@vetor.example.com", t("Campinas", "Lakeside"), now);
        Supplier s4 = supplier("Quasar Software", reseller, "vendas@quasar.example.com", t("Curitiba", "Hillview"), now);

        Long inUse = dd(DropdownKind.ASSET_STATE, t("Em uso", "In use"));
        Long stock = dd(DropdownKind.ASSET_STATE, t("Em estoque", "In stock"));
        Long repair = dd(DropdownKind.ASSET_STATE, t("Em manutenção", "Under repair"));
        Long dell = dd(DropdownKind.MANUFACTURER, "Dell");
        Long lenovo = dd(DropdownKind.MANUFACTURER, "Lenovo");
        Long hp = dd(DropdownKind.MANUFACTURER, "HP");
        Long cisco = dd(DropdownKind.MANUFACTURER, "Cisco");
        Long samsung = dd(DropdownKind.MANUFACTURER, "Samsung");
        Long lg = dd(DropdownKind.MANUFACTURER, "LG");
        Long win11 = dd(DropdownKind.OPERATING_SYSTEM, "Windows 11");
        Long winSrv = dd(DropdownKind.OPERATING_SYSTEM, "Windows Server 2022");
        Long ubuntu = dd(DropdownKind.OPERATING_SYSTEM, "Ubuntu 24.04 LTS");
        Long android = dd(DropdownKind.OPERATING_SYSTEM, "Android");
        List<Asset> assets = new ArrayList<>();
        List<Asset> computers = new ArrayList<>();
        int seq = 1;
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            boolean laptop = i % 3 != 2;
            Long maker = laptop ? lenovo : dell;
            Asset c = asset(laptop ? AssetType.LAPTOP : AssetType.COMPUTER,
                    (laptop ? "NB-" : "DT-") + String.format("%03d", seq), u.getDefaultOrgUnitId(), maker,
                    dd(DropdownKind.ASSET_MODEL, laptop ? "ThinkPad T14 Gen 5" : "OptiPlex 7020"), u, inUse,
                    u.getLocationId(), s1, now.minusDays(400 + random.nextInt(500)), seq++);
            c.setOperatingSystemId(win11);
            c.setOsVersion("24H2");
            c.setCpu(laptop ? "Intel Core Ultra 5 135U" : "Intel Core i5-14500");
            c.setRamMb(laptop ? 16384 : 32768);
            c.setStorageGb(512);
            c.setHostname(c.getName().toLowerCase());
            c.setIpAddress("10.10." + (i % 4 == 3 ? 2 : 1) + "." + (20 + i));
            c.setMacAddress(mac());
            c = put(c, now.minusDays(100));
            assets.add(c);
            computers.add(c);
            if (!laptop || i % 4 == 0) {
                Asset m = asset(AssetType.MONITOR, "MON-" + String.format("%03d", seq), u.getDefaultOrgUnitId(), i % 2 == 0 ? lg : samsung,
                        dd(DropdownKind.ASSET_MODEL, i % 2 == 0 ? "UltraFine 27\"" : "ViewFinity S6 24\""), u, inUse,
                        u.getLocationId(), s1, now.minusDays(300 + random.nextInt(400)), seq++);
                m = put(m, now.minusDays(100));
                assets.add(m);
                AssetConnection conn = new AssetConnection();
                conn.setAssetId(c.getId());
                conn.setConnectedAssetId(m.getId());
                put(conn, now.minusDays(100));
            }
        }
        for (int i = 0; i < 4; i++) {
            Asset srv = asset(AssetType.SERVER, "SRV-" + new String[] {"AD01", "FILE01", "APP01", "DB01"}[i], hq.getId(), dell,
                    dd(DropdownKind.ASSET_MODEL, "PowerEdge R760"), null, inUse, locations.get(2), s1,
                    now.minusDays(700), seq++);
            srv.setOperatingSystemId(i == 3 ? ubuntu : winSrv);
            srv.setCpu("2x Intel Xeon Silver 4410Y");
            srv.setRamMb(131072);
            srv.setStorageGb(4000);
            srv.setIpAddress("10.10.0." + (10 + i));
            srv.setHostname(srv.getName().toLowerCase());
            srv.setTechGroupId(infra.getId());
            srv.setTechUserId(users.get(3).getId());
            assets.add(put(srv, now.minusDays(200)));
        }
        String[][] network = {{"SW-CORE-01", "Catalyst 9300"}, {"SW-ACC-01", "Catalyst 9200"}, {"SW-ACC-02", "Catalyst 9200"},
            {"FW-EDGE-01", "Firepower 1120"}, {"AP-1A", "Catalyst 9120AX"}, {"AP-2A", "Catalyst 9120AX"}};
        for (int i = 0; i < network.length; i++) {
            Asset n = asset(AssetType.NETWORK_DEVICE, network[i][0], i < 5 ? hq.getId() : branch.getId(), cisco,
                    dd(DropdownKind.ASSET_MODEL, network[i][1]), null, inUse, locations.get(i < 4 ? 2 : (i == 4 ? 0 : 3)), s2,
                    now.minusDays(500), seq++);
            n.setIpAddress("10.10.0." + (100 + i));
            n.setTechGroupId(infra.getId());
            assets.add(put(n, now.minusDays(200)));
        }
        for (int i = 0; i < 4; i++) {
            Asset p = asset(AssetType.PRINTER, "PRN-" + String.format("%02d", i + 1), i == 3 ? branch.getId() : hq.getId(), hp,
                    dd(DropdownKind.ASSET_MODEL, "LaserJet Pro 4003dn"), null, i == 2 ? repair : inUse,
                    locations.get(i == 3 ? 3 : i % 2), s3, now.minusDays(360), seq++);
            p.setIpAddress("10.10." + (i == 3 ? 2 : 1) + "." + (200 + i));
            assets.add(put(p, now.minusDays(200)));
        }
        for (int i = 0; i < 6; i++) {
            Asset ph = asset(AssetType.PHONE, "CEL-" + String.format("%02d", i + 1), hq.getId(), samsung,
                    dd(DropdownKind.ASSET_MODEL, "Galaxy A55"), i < 5 ? users.get(i + 7) : null, i < 5 ? inUse : stock,
                    locations.get(0), s2, now.minusDays(200), seq++);
            ph.setOperatingSystemId(android);
            assets.add(put(ph, now.minusDays(150)));
        }
        for (int i = 0; i < 3; i++) {
            Asset spare = asset(AssetType.LAPTOP, "NB-" + String.format("%03d", 900 + i), hq.getId(), lenovo,
                    dd(DropdownKind.ASSET_MODEL, "ThinkPad T14 Gen 5"), null, stock, locations.get(0), s1, now.minusDays(40), seq++);
            spare.setOperatingSystemId(win11);
            spare.setWarrantyExpiresOn(today().plusDays(5 + i * 9));
            assets.add(put(spare, now.minusDays(30)));
        }

        Long productivity = dd(DropdownKind.SOFTWARE_CATEGORY, t("Produtividade", "Productivity"));
        Long security = dd(DropdownKind.SOFTWARE_CATEGORY, t("Segurança", "Security"));
        Long devCat = dd(DropdownKind.SOFTWARE_CATEGORY, t("Desenvolvimento", "Development"));
        Long comm = dd(DropdownKind.SOFTWARE_CATEGORY, t("Comunicação", "Communication"));
        Object[][] catalog = {
            {"Microsoft 365 Apps", "Microsoft", productivity, 20, 0},
            {"Adobe Acrobat Pro", "Adobe", productivity, 5, 60},
            {"ESET Endpoint Security", "ESET", security, 30, 200},
            {"IntelliJ IDEA Ultimate", "JetBrains", devCat, 2, 25},
            {"Zoom Workplace", "Zoom", comm, 10, 120},
            {"7-Zip", "Igor Pavlov", productivity, -1, 0},
            {"Mozilla Firefox", "Mozilla", productivity, -1, 0},
        };
        for (Object[] item : catalog) {
            Software sw = new Software();
            sw.setOrgUnitId(root.getId());
            sw.setName((String) item[0]);
            sw.setPublisherId(dd(DropdownKind.MANUFACTURER, (String) item[1]));
            sw.setCategoryId((Long) item[2]);
            sw = put(sw, now.minusDays(150));
            SoftwareLicense lic = null;
            int seats = (Integer) item[3];
            if (seats > 0) {
                lic = new SoftwareLicense();
                lic.setOrgUnitId(root.getId());
                lic.setSoftwareId(sw.getId());
                lic.setName(sw.getName() + " - " + t("Assinatura anual", "Annual subscription"));
                lic.setSeats(seats);
                int expiresIn = (Integer) item[4];
                lic.setExpiresOn(expiresIn == 0 ? today().plusDays(20) : today().plusDays(expiresIn));
                lic.setSupplierId(s4.getId());
                lic.setPurchaseCost(BigDecimal.valueOf(seats * (150L + random.nextInt(600))));
                lic = put(lic, now.minusDays(150));
            }
            int installs = seats > 0 ? Math.min(computers.size(), seats + (sw.getName().startsWith("Adobe") ? 2 : 0)) : computers.size();
            for (int k = 0; k < installs && k < computers.size(); k++) {
                SoftwareInstallation inst = new SoftwareInstallation();
                inst.setAssetId(computers.get(k).getId());
                inst.setSoftwareId(sw.getId());
                inst.setSoftwareVersion(sw.getName().contains("365") ? "2409" : "2026." + (1 + random.nextInt(3)));
                inst.setLicenseId(lic == null ? null : lic.getId());
                inst.setInstalledOn(today().minusDays(30 + random.nextInt(200)));
                put(inst, now.minusDays(90));
            }
        }

        Contract c1 = contract(t("Suporte e manutenção de servidores", "Server support and maintenance"), "CT-2025-014",
                dd(DropdownKind.CONTRACT_TYPE, t("Manutenção", "Maintenance")), s1, today().minusMonths(11), 12, 2, RenewalType.EXPRESS,
                new BigDecimal("48000.00"), root.getId(), now);
        Contract c2 = contract(t("Link de internet dedicado 500 Mbps", "Dedicated 500 Mbps internet link"), "CT-2024-031",
                dd(DropdownKind.CONTRACT_TYPE, t("Suporte", "Support")), s2, today().minusMonths(20), 24, 3, RenewalType.TACIT,
                new BigDecimal("36000.00"), root.getId(), now);
        contract(t("Outsourcing de impressão", "Managed print services"), "CT-2026-002",
                dd(DropdownKind.CONTRACT_TYPE, t("Locação", "Leasing")), s3, today().minusMonths(3), 36, 3, RenewalType.TACIT,
                new BigDecimal("21600.00"), root.getId(), now);
        for (Asset a : assets) {
            if (a.getAssetType() == AssetType.SERVER) {
                ContractItem ci = new ContractItem();
                ci.setContractId(c1.getId());
                ci.setAssetId(a.getId());
                put(ci, now.minusDays(90));
            }
            if (a.getAssetType() == AssetType.NETWORK_DEVICE && a.getName().startsWith("FW")) {
                ContractItem ci = new ContractItem();
                ci.setContractId(c2.getId());
                ci.setAssetId(a.getId());
                put(ci, now.minusDays(90));
            }
        }

        RuleDefinition printers = new RuleDefinition(rule(t("Impressoras → Infraestrutura", "Printers → Infrastructure"), 1),
                List.of(criterion(RuleField.TITLE, RuleOperator.REGEX, t("impressora|toner|imprimir", "printer|toner|print"))),
                List.of(action(RuleActionType.ASSIGN_GROUP, infra.getId()), action(RuleActionType.SET_CATEGORY,
                        dd(DropdownKind.ITIL_CATEGORY, t("Impressora", "Printer")))));
        services.rules().saveInternal(printers);
        RuleDefinition vip = new RuleDefinition(rule(t("Diretoria → prioridade alta", "Executives → high priority"), 2),
                List.of(criterion(RuleField.REQUESTER, RuleOperator.IS, String.valueOf(users.get(15).getId()))),
                List.of(action(RuleActionType.SET_URGENCY, 4L), action(RuleActionType.ADD_OBSERVER, users.get(4).getId())));
        services.rules().saveInternal(vip);
        RuleDefinition access = new RuleDefinition(rule(t("Senhas e acessos → N1", "Passwords → L1"), 3),
                List.of(criterion(RuleField.TITLE, RuleOperator.REGEX, t("senha|acesso|bloquead", "password|access|locked"))),
                List.of(action(RuleActionType.ASSIGN_GROUP, desk.getId()), action(RuleActionType.SET_CATEGORY,
                        dd(DropdownKind.ITIL_CATEGORY, t("Senha", "Password")))));
        services.rules().saveInternal(access);

        String[][] templates = {
            {"Notebook não liga", "Laptop does not power on", "Notebook", "Laptop", "I"},
            {"Impressora do 2º andar atolando papel", "2nd floor printer jamming paper", "Impressora", "Printer", "I"},
            {"Sem acesso à internet", "No internet access", "Internet", "Internet", "I"},
            {"Wi-Fi caindo na sala de reuniões", "Wi-Fi drops in the meeting room", "Wi-Fi", "Wi-Fi", "I"},
            {"Senha expirada no domínio", "Domain password expired", "Senha", "Password", "R"},
            {"Usuário bloqueado após tentativas", "Account locked after attempts", "Senha", "Password", "I"},
            {"Instalar Adobe Acrobat", "Install Adobe Acrobat", "Office", "Office", "R"},
            {"Outlook não sincroniza e-mails", "Outlook does not sync e-mails", "E-mail", "E-mail", "I"},
            {"Criar acesso para novo colaborador", "Create access for new employee", "Novo usuário", "New user", "R"},
            {"VPN não conecta em home office", "VPN does not connect from home", "VPN", "VPN", "I"},
            {"Lentidão no sistema financeiro", "Finance system is slow", "Sistemas internos", "Internal systems", "I"},
            {"Trocar toner da impressora", "Replace printer toner", "Impressora", "Printer", "R"},
            {"Monitor piscando", "Monitor flickering", "Periféricos", "Peripherals", "I"},
            {"Solicitar mouse e teclado", "Request mouse and keyboard", "Periféricos", "Peripherals", "R"},
            {"Permissão na pasta do Financeiro", "Permission to Finance shared folder", "Permissões", "Permissions", "R"},
            {"Atualização do Windows travada", "Windows update stuck", "Sistema operacional", "Operating system", "I"},
            {"Ramal sem tom de discagem", "Desk phone has no dial tone", "Telefonia", "Telephony", "I"},
            {"Computador muito lento", "Computer is very slow", "Computador", "Desktop", "I"},
            {"Configurar e-mail no celular", "Set up e-mail on mobile phone", "E-mail", "E-mail", "R"},
            {"Erro ao abrir planilhas compartilhadas", "Error opening shared spreadsheets", "Office", "Office", "I"},
        };
        String[] followupsPt = {"Pode verificar ainda hoje?", "Testei novamente e o erro continua.", "Obrigado pelo retorno!",
            "Estou disponível na minha mesa até as 17h.", "Segue em anexo o print do erro."};
        String[] followupsEn = {"Could you check it today?", "I tested again and the error persists.", "Thanks for the update!",
            "I'm at my desk until 5 pm.", "Screenshot of the error is attached."};
        String[] tasksPt = {"Diagnóstico remoto realizado.", "Reinstalação do driver e testes.",
            "Verificado cabeamento e porta do switch.", "Limpeza de cache e perfil do usuário.", "Contato com o fornecedor aberto."};
        String[] tasksEn = {"Remote diagnosis performed.", "Driver reinstalled and tested.", "Checked cabling and switch port.",
            "Cleared cache and user profile.", "Opened a case with the vendor."};
        String[] solutionsPt = {"Problema resolvido após reinstalação do driver.", "Senha redefinida e acesso validado com o usuário.",
            "Equipamento substituído por unidade de estoque.", "Configuração ajustada e testada com o usuário.",
            "Atualização aplicada; desempenho normalizado."};
        String[] solutionsEn = {"Fixed after reinstalling the driver.", "Password reset and access validated with the user.",
            "Equipment replaced with a spare unit.", "Settings adjusted and tested with the user.", "Update applied; performance back to normal."};
        Long phone = dd(DropdownKind.REQUEST_SOURCE, t("Telefone", "Phone"));
        Long email = dd(DropdownKind.REQUEST_SOURCE, "E-mail");
        Long portal = dd(DropdownKind.REQUEST_SOURCE, "Portal");
        Long solvedRemote = dd(DropdownKind.SOLUTION_TYPE, t("Resolvido remotamente", "Solved remotely"));
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            String[] tpl = templates[random.nextInt(templates.length)];
            int daysAgo = (int) Math.floor(Math.pow(random.nextDouble(), 1.6) * 75);
            LocalDateTime opened = now.minusDays(daysAgo).withHour(8 + random.nextInt(9)).withMinute(random.nextInt(60)).withSecond(0);
            if (opened.isAfter(now)) {
                opened = now.minusMinutes(30 + random.nextInt(300));
            }
            User requester = pick(requesters);
            Ticket tk = new Ticket();
            tk.setOrgUnitId(requester.getDefaultOrgUnitId());
            tk.setTitle(t(tpl[0], tpl[1]));
            tk.setContent(t("Olá, equipe de TI.\n\n", "Hello IT team,\n\n") + t(tpl[0], tpl[1]) + "."
                    + t("\nIsso está impactando meu trabalho.", "\nThis is affecting my work."));
            tk.setTicketType("R".equals(tpl[4]) ? TicketType.REQUEST : TicketType.INCIDENT);
            tk.setCategoryId(dd(DropdownKind.ITIL_CATEGORY, t(tpl[2], tpl[3])));
            tk.setRequesterId(requester.getId());
            tk.setLocationId(requester.getLocationId());
            tk.setSourceId(pick(List.of(phone, email, portal, portal)));
            tk.setUrgency(1 + random.nextInt(5));
            tk.setImpact(1 + random.nextInt(4));
            tk.setPriority(PriorityMatrix.compute(tk.getUrgency(), tk.getImpact()));
            tk.setOpenedAt(opened);
            tk.setWaitingMinutes(0);
            services.slas().apply(tk);
            boolean infraTicket = tk.getTitle().matches("(?i).*(impressora|printer|wi-fi|vpn|internet|ramal|phone).*");
            UserGroup g = infraTicket ? infra : desk;
            double r = random.nextDouble();
            ItilStatus status;
            if (daysAgo > 12) {
                status = r < 0.86 ? ItilStatus.CLOSED : (r < 0.95 ? ItilStatus.SOLVED : ItilStatus.WAITING);
            } else if (daysAgo > 3) {
                status = r < 0.45 ? ItilStatus.CLOSED : r < 0.65 ? ItilStatus.SOLVED : r < 0.82 ? ItilStatus.ASSIGNED
                        : r < 0.9 ? ItilStatus.PLANNED : ItilStatus.WAITING;
            } else {
                status = r < 0.25 ? ItilStatus.NEW : r < 0.6 ? ItilStatus.ASSIGNED : r < 0.72 ? ItilStatus.PLANNED
                        : r < 0.82 ? ItilStatus.WAITING : r < 0.94 ? ItilStatus.SOLVED : ItilStatus.CLOSED;
            }
            User technician = null;
            if (status != ItilStatus.NEW) {
                tk.setAssigneeGroupId(g.getId());
                technician = infraTicket ? pick(List.of(users.get(2), users.get(3), users.get(5)))
                        : pick(List.of(users.get(0), users.get(1), installer));
                tk.setAssigneeId(technician.getId());
                tk.setTakenAt(opened.plusMinutes(5 + random.nextInt(150)));
            } else if (random.nextBoolean()) {
                tk.setAssigneeGroupId(g.getId());
            }
            tk.setStatus(status);
            int resolutionMinutes = 30 + (int) (random.nextDouble() * (tk.getPriority() >= 4 ? 600 : 2400));
            if (status == ItilStatus.SOLVED || status == ItilStatus.CLOSED) {
                LocalDateTime solved = opened.plusMinutes(resolutionMinutes);
                if (solved.isAfter(now)) {
                    solved = now.minusMinutes(10);
                }
                tk.setSolvedAt(solved);
                if (status == ItilStatus.CLOSED) {
                    tk.setClosedAt(solved.plusHours(2 + random.nextInt(40)).isAfter(now) ? now : solved.plusHours(2 + random.nextInt(40)));
                    if (random.nextDouble() < 0.6) {
                        tk.setSatisfaction(random.nextDouble() < 0.75 ? 5 : 3 + random.nextInt(2));
                    }
                }
            }
            if (status == ItilStatus.WAITING) {
                tk.setWaitingSince(now.minusHours(1 + random.nextInt(30)));
            }
            tk.setLastActivityAt(tk.getClosedAt() != null ? tk.getClosedAt() : (tk.getSolvedAt() != null ? tk.getSolvedAt() : opened));
            tk.setActionTimeMinutes(0);
            tk = put(tk, opened);
            tickets.add(tk);
            if (technician != null) {
                int minutes = 15 + random.nextInt(8) * 15;
                ItilTask task = new ItilTask();
                task.setItemType(ItilType.TICKET);
                task.setItemId(tk.getId());
                task.setAuthorId(technician.getId());
                task.setTechnicianId(technician.getId());
                task.setContent(pt ? tasksPt[random.nextInt(tasksPt.length)] : tasksEn[random.nextInt(tasksEn.length)]);
                task.setDurationMinutes(minutes);
                task.setStatus(status.isTerminal() ? TaskStatus.DONE : TaskStatus.TODO);
                if (status == ItilStatus.PLANNED || (status == ItilStatus.ASSIGNED && random.nextDouble() < 0.6)) {
                    task.setPlannedStartAt(weekStart.plusDays(random.nextInt(5)).withHour(8 + random.nextInt(9))
                            .withMinute(random.nextBoolean() ? 0 : 30));
                    task.setPlannedEndAt(task.getPlannedStartAt().plusMinutes(minutes));
                }
                put(task, tk.getTakenAt() == null ? opened : tk.getTakenAt());
                tk.setActionTimeMinutes(minutes);
                tk = repo(Ticket.class).save(tk);
                if (status.isOpen() && random.nextDouble() < 0.35) {
                    User second = pick(infraTicket ? List.of(users.get(2), users.get(3), users.get(5))
                            : List.of(users.get(0), users.get(1), installer));
                    if (!second.getId().equals(technician.getId())) {
                        actor(tk.getId(), second.getId(), ItilActorRole.ASSIGNEE, opened);
                    }
                }
                if (status.isOpen() && random.nextDouble() < 0.25) {
                    actor(tk.getId(), pick(requesters).getId(), ItilActorRole.FOLLOWER, opened);
                }
                if (random.nextDouble() < 0.55) {
                    ItilFollowup f = new ItilFollowup();
                    f.setItemType(ItilType.TICKET);
                    f.setItemId(tk.getId());
                    f.setAuthorId(requester.getId());
                    f.setContent(pt ? followupsPt[random.nextInt(followupsPt.length)] : followupsEn[random.nextInt(followupsEn.length)]);
                    put(f, opened.plusMinutes(20 + random.nextInt(120)));
                }
                if (tk.getSolvedAt() != null) {
                    ItilSolution sol = new ItilSolution();
                    sol.setItemType(ItilType.TICKET);
                    sol.setItemId(tk.getId());
                    sol.setAuthorId(technician.getId());
                    sol.setSolutionTypeId(solvedRemote);
                    sol.setContent(pt ? solutionsPt[random.nextInt(solutionsPt.length)] : solutionsEn[random.nextInt(solutionsEn.length)]);
                    sol.setStatus(status == ItilStatus.CLOSED ? SolutionStatus.ACCEPTED : SolutionStatus.PROPOSED);
                    if (status == ItilStatus.CLOSED) {
                        sol.setAnsweredById(requester.getId());
                        sol.setAnsweredAt(tk.getClosedAt());
                    }
                    put(sol, tk.getSolvedAt());
                }
            }
            if (random.nextDouble() < 0.35) {
                Asset a = computers.stream().filter(x -> requester.getId().equals(x.getUserId())).findFirst().orElse(pick(computers));
                ItilAsset ia = new ItilAsset();
                ia.setItemType(ItilType.TICKET);
                ia.setItemId(tk.getId());
                ia.setAssetId(a.getId());
                put(ia, opened);
            }
        }

        Problem p1 = problem(t("Quedas recorrentes do Wi-Fi no 2º andar", "Recurring Wi-Fi drops on the 2nd floor"),
                t("Diversos chamados de desconexão nos access points do 2º andar.", "Several disconnection tickets on 2nd floor access points."),
                ItilStatus.ASSIGNED, users.get(5), infra, now.minusDays(9), root.getId());
        problem(t("Lentidão do sistema financeiro no fechamento", "Finance system slowness at month end"),
                t("Consultas lentas no banco do ERP durante o fechamento mensal.", "Slow ERP database queries during month-end closing."),
                ItilStatus.ACCEPTED, users.get(3), infra, now.minusDays(20), root.getId());
        problem(t("Atolamento frequente na impressora PRN-02", "Frequent jams on printer PRN-02"),
                t("Rolete de tração com desgaste.", "Pickup roller is worn out."), ItilStatus.SOLVED, users.get(2), infra,
                now.minusDays(35), root.getId());
        for (Ticket tk : tickets) {
            if (tk.getTitle().contains("Wi-Fi") && random.nextBoolean()) {
                ItilRelation rel = new ItilRelation();
                rel.setSourceType(ItilType.PROBLEM);
                rel.setSourceId(p1.getId());
                rel.setTargetType(ItilType.TICKET);
                rel.setTargetId(tk.getId());
                rel.setRelationType(RelationType.CAUSED_BY);
                put(rel, now.minusDays(8));
            }
        }
        change(t("Substituição dos access points do 2º andar", "Replace 2nd floor access points"), ChangeType.NORMAL,
                RiskLevel.MEDIUM, ItilStatus.APPROVAL, now.plusDays(6), users.get(5), infra, now.minusDays(5), root.getId());
        change(t("Atualização do firmware do firewall", "Firewall firmware upgrade"), ChangeType.NORMAL, RiskLevel.HIGH,
                ItilStatus.ACCEPTED, now.plusDays(3), users.get(3), infra, now.minusDays(7), root.getId());
        change(t("Migração do servidor de arquivos", "File server migration"), ChangeType.NORMAL, RiskLevel.HIGH,
                ItilStatus.TESTING, now.plusDays(12), users.get(2), infra, now.minusDays(14), root.getId());
        change(t("Inclusão de usuário no grupo VPN", "Add user to the VPN group"), ChangeType.STANDARD, RiskLevel.LOW,
                ItilStatus.CLOSED, now.minusDays(2), users.get(0), desk, now.minusDays(4), root.getId());
        change(t("Reinicialização emergencial do cluster de banco", "Emergency database cluster restart"), ChangeType.EMERGENCY,
                RiskLevel.HIGH, ItilStatus.REVIEW, now.minusDays(1), users.get(3), infra, now.minusDays(1), root.getId());

        Long planned = dd(DropdownKind.PROJECT_STATE, t("Planejado", "Planned"));
        Long progress = dd(DropdownKind.PROJECT_STATE, t("Em andamento", "In progress"));
        LocalDate today = today();
        Project rollout = project("PRJ-001", t("Renovação do parque de notebooks", "Laptop fleet refresh"),
                t("Substituição de 40 notebooks com mais de 4 anos de uso.", "Replace 40 laptops older than 4 years."),
                progress, users.get(4), infra, today.minusDays(20), today.plusDays(40), root.getId(), "#2563eb", now);
        Project network2 = project("PRJ-002", t("Modernização da rede Wi-Fi", "Wi-Fi network modernization"),
                t("Novos access points Wi-Fi 6E e reestruturação das VLANs.", "New Wi-Fi 6E access points and VLAN redesign."),
                progress, users.get(5), infra, today.minusDays(10), today.plusDays(50), root.getId(), "#7c3aed", now);
        Project itsm = project("PRJ-003", t("Implantação do catálogo de serviços", "Service catalog rollout"),
                t("Padronizar categorias, SLAs e artigos da base de conhecimento.", "Standardize categories, SLAs and knowledge base articles."),
                planned, users.get(4), desk, today.plusDays(10), today.plusDays(70), root.getId(), "#0d9488", now);
        String[][] rolloutTasks = pt
                ? new String[][] {{"Levantamento do inventário", "0", "6", "DONE"}, {"Cotação com fornecedores", "7", "16", "DONE"},
                    {"Aprovação do orçamento", "17", "17", "DONE", "M"}, {"Compra e recebimento", "18", "32", "IN_PROGRESS"},
                    {"Preparação da imagem padrão", "18", "27", "REVIEW"}, {"Entrega aos usuários - lote 1", "33", "42", "TODO"},
                    {"Entrega aos usuários - lote 2", "43", "52", "TODO"}, {"Descarte dos equipamentos antigos", "53", "60", "TODO"}}
                : new String[][] {{"Inventory survey", "0", "6", "DONE"}, {"Vendor quotations", "7", "16", "DONE"},
                    {"Budget approval", "17", "17", "DONE", "M"}, {"Purchase and delivery", "18", "32", "IN_PROGRESS"},
                    {"Build the standard image", "18", "27", "REVIEW"}, {"Hand over to users - batch 1", "33", "42", "TODO"},
                    {"Hand over to users - batch 2", "43", "52", "TODO"}, {"Dispose of old equipment", "53", "60", "TODO"}};
        tasks(rollout, rolloutTasks, List.of(users.get(2), users.get(3), users.get(4)), today.minusDays(20), now,
                new int[][] {{1, 0}, {2, 1}, {3, 2}, {4, 2}, {5, 3}, {5, 4}, {6, 5}, {7, 6}});
        String[][] wifiTasks = pt
                ? new String[][] {{"Site survey", "0", "7", "DONE"}, {"Projeto das VLANs", "5", "14", "IN_PROGRESS"},
                    {"Instalação dos APs - 1º andar", "15", "24", "TODO"}, {"Instalação dos APs - 2º andar", "25", "34", "TODO"},
                    {"Testes de cobertura", "35", "42", "TODO"}, {"Go-live", "43", "43", "TODO", "M"}}
                : new String[][] {{"Site survey", "0", "7", "DONE"}, {"VLAN design", "5", "14", "IN_PROGRESS"},
                    {"Install APs - 1st floor", "15", "24", "TODO"}, {"Install APs - 2nd floor", "25", "34", "TODO"},
                    {"Coverage tests", "35", "42", "TODO"}, {"Go-live", "43", "43", "TODO", "M"}};
        tasks(network2, wifiTasks, List.of(users.get(5), users.get(3)), today.minusDays(10), now,
                new int[][] {{1, 0}, {2, 1}, {3, 2}, {4, 3}, {5, 4}});
        String[][] catalogTasks = pt
                ? new String[][] {{"Workshop com as áreas", "0", "5", "TODO"}, {"Revisão das categorias", "6", "15", "TODO"},
                    {"Definição de SLAs", "10", "20", "TODO"}, {"Artigos da base de conhecimento", "16", "40", "TODO"},
                    {"Treinamento da equipe", "41", "50", "TODO"}}
                : new String[][] {{"Workshops with departments", "0", "5", "TODO"}, {"Review categories", "6", "15", "TODO"},
                    {"Define SLAs", "10", "20", "TODO"}, {"Knowledge base articles", "16", "40", "TODO"},
                    {"Team training", "41", "50", "TODO"}};
        tasks(itsm, catalogTasks, List.of(users.get(0), users.get(1), users.get(4)), today.plusDays(10), now,
                new int[][] {{1, 0}, {2, 0}, {3, 1}, {4, 3}});
        for (Project p : List.of(rollout, network2, itsm)) {
            services.projects().rollup(p.getId());
        }

        Long kbFaq = dd(DropdownKind.KB_CATEGORY, t("Perguntas frequentes", "Frequently asked questions"));
        Long kbProc = dd(DropdownKind.KB_CATEGORY, t("Procedimentos de TI", "IT procedures"));
        kb(t("Como redefinir sua senha de rede", "How to reset your network password"), kbFaq, true,
                t("# Redefinição de senha\n\n1. Pressione **Ctrl+Alt+Del** e escolha *Alterar senha*.\n2. Informe a senha atual e a nova senha duas vezes.\n3. A senha deve ter pelo menos 8 caracteres, com letras e números.\n\nSe a conta estiver bloqueada, abra um chamado na categoria **Acessos > Senha**.",
                  "# Password reset\n\n1. Press **Ctrl+Alt+Del** and choose *Change a password*.\n2. Type your current password and the new one twice.\n3. Passwords need at least 8 characters with letters and numbers.\n\nIf your account is locked, open a ticket in **Access > Password**."), users.get(0), now);
        kb(t("Conectando à VPN corporativa", "Connecting to the corporate VPN"), kbFaq, true,
                t("# VPN\n\n- Abra o cliente de VPN pela bandeja do sistema.\n- Use seu usuário de rede e o código do aplicativo autenticador.\n- Se aparecer erro de certificado, reinicie o computador e tente novamente.",
                  "# VPN\n\n- Open the VPN client from the system tray.\n- Sign in with your network account and the authenticator code.\n- If a certificate error appears, restart the computer and try again."), users.get(5), now);
        kb(t("Configurar e-mail no celular", "Set up e-mail on your phone"), kbFaq, true,
                t("# E-mail no celular\n\n1. Instale o aplicativo oficial de e-mail.\n2. Entre com seu e-mail corporativo.\n3. Aceite a política de segurança do dispositivo.",
                  "# E-mail on mobile\n\n1. Install the official e-mail app.\n2. Sign in with your corporate e-mail.\n3. Accept the device security policy."), users.get(1), now);
        kb(t("Procedimento de entrega de notebook", "Laptop hand-over procedure"), kbProc, false,
                t("# Entrega de notebook\n\n1. Conferir patrimônio e número de série no inventário.\n2. Vincular o ativo ao usuário no Pyhoma.\n3. Colher assinatura do termo de responsabilidade.\n4. Registrar a entrega como tarefa no chamado.",
                  "# Laptop hand-over\n\n1. Check asset tag and serial number in the inventory.\n2. Assign the asset to the user in Pyhoma.\n3. Collect the signed responsibility form.\n4. Log the hand-over as a ticket task."), users.get(2), now);
        kb(t("Checklist de abertura de mudança", "Change request checklist"), kbProc, false,
                t("# Antes de abrir uma mudança\n\n- Descreva o impacto e os serviços afetados.\n- Anexe o plano de implantação e o plano de retorno.\n- Defina a janela de manutenção.\n- Solicite aprovação do coordenador.",
                  "# Before opening a change\n\n- Describe impact and affected services.\n- Attach rollout and backout plans.\n- Define the maintenance window.\n- Request the manager's approval."), users.get(4), now);
        kb(t("Impressora: como limpar atolamento de papel", "Printer: clearing a paper jam"), kbFaq, true,
                t("# Atolamento de papel\n\n1. Desligue a impressora.\n2. Abra a tampa frontal e retire o papel com cuidado.\n3. Verifique a bandeja e ligue novamente.",
                  "# Paper jam\n\n1. Turn the printer off.\n2. Open the front cover and gently remove the paper.\n3. Check the tray and turn it back on."), users.get(2), now);

        recurring(t("Verificação semanal de backups", "Weekly backup check"), t("Conferir rotinas de backup", "Check backup jobs"),
                Periodicity.WEEKLY, infra, root.getId(), now);
        recurring(t("Revisão mensal de acessos", "Monthly access review"), t("Revisar contas e grupos privilegiados", "Review privileged accounts and groups"),
                Periodicity.MONTHLY, desk, root.getId(), now);
    }

    private OrgUnit unit(String name, OrgUnit parent) {
        OrgUnit u = new OrgUnit();
        u.setName(name);
        u.setParentId(parent.getId());
        u.setDepth(parent.getDepth() + 1);
        u.setCompleteName(parent.getCompleteName() + " > " + name);
        return u;
    }

    private UserGroup group(String name, Long orgUnitId, LocalDateTime now) {
        UserGroup g = new UserGroup();
        g.setName(name);
        g.setOrgUnitId(orgUnitId);
        return put(g, now.minusDays(110));
    }

    private void member(UserGroup g, User u, boolean manager, LocalDateTime now) {
        GroupMember m = new GroupMember();
        m.setGroupId(g.getId());
        m.setUserId(u.getId());
        m.setManager(manager);
        put(m, now.minusDays(110));
    }

    private Supplier supplier(String name, Long type, String email, String city, LocalDateTime now) {
        Supplier s = new Supplier();
        s.setOrgUnitId(services.orgUnits().root().getId());
        s.setName(name);
        s.setTypeId(type);
        s.setEmail(email);
        s.setCity(city);
        s.setPhone("(11) 3000-" + (1000 + random.nextInt(8999)));
        s.setWebsite("https://" + name.toLowerCase().split(" ")[0] + ".example.com");
        return put(s, now.minusDays(150));
    }

    private Asset asset(AssetType type, String name, Long orgUnitId, Long manufacturer, Long model, User user, Long state,
                        Long location, Supplier supplier, LocalDateTime purchased, int seq) {
        Asset a = new Asset();
        a.setAssetType(type);
        a.setName(name);
        a.setOrgUnitId(orgUnitId);
        a.setManufacturerId(manufacturer);
        a.setModelId(model);
        a.setUserId(user == null ? null : user.getId());
        a.setStateId(state);
        a.setLocationId(location);
        a.setSupplierId(supplier.getId());
        a.setPurchaseDate(purchased.toLocalDate());
        a.setWarrantyExpiresOn(purchased.toLocalDate().plusYears(type == AssetType.SERVER ? 5 : 3));
        a.setSerialNumber(Long.toString(Math.abs(random.nextLong()), 36).toUpperCase().substring(0, 10));
        a.setAssetTag("PYH-" + String.format("%06d", seq));
        a.setPurchaseCost(BigDecimal.valueOf(switch (type) {
            case SERVER -> 68000 + random.nextInt(9000);
            case LAPTOP -> 7200 + random.nextInt(1500);
            case COMPUTER -> 5400 + random.nextInt(1200);
            case MONITOR -> 1300 + random.nextInt(600);
            case NETWORK_DEVICE -> 9000 + random.nextInt(15000);
            case PRINTER -> 2800 + random.nextInt(800);
            case PHONE -> 2100 + random.nextInt(400);
            default -> 500;
        }));
        a.setLastInventoryAt(now().minusDays(random.nextInt(20)));
        return a;
    }

    private String mac() {
        StringBuilder sb = new StringBuilder("00:1A");
        for (int i = 0; i < 4; i++) {
            sb.append(':').append(String.format("%02X", random.nextInt(256)));
        }
        return sb.toString();
    }

    private Contract contract(String name, String number, Long type, Supplier supplier, LocalDate start, int months, int notice,
                              RenewalType renewal, BigDecimal cost, Long orgUnitId, LocalDateTime now) {
        Contract c = new Contract();
        c.setName(name);
        c.setContractNumber(number);
        c.setTypeId(type);
        c.setSupplierId(supplier.getId());
        c.setStartDate(start);
        c.setDurationMonths(months);
        c.setNoticeMonths(notice);
        c.setRenewal(renewal);
        c.setCost(cost);
        c.setOrgUnitId(orgUnitId);
        return put(c, now.minusDays(120));
    }

    private Rule rule(String name, int ranking) {
        Rule r = new Rule();
        r.setName(name);
        r.setRanking(ranking);
        r.setTriggerEvent(RuleTrigger.ON_CREATE);
        r.setMatchAll(true);
        r.setActive(true);
        return r;
    }

    private RuleCriterion criterion(RuleField field, RuleOperator op, String pattern) {
        RuleCriterion c = new RuleCriterion();
        c.setField(field);
        c.setMatchOperator(op);
        c.setPattern(pattern);
        return c;
    }

    private RuleAction action(RuleActionType type, Long value) {
        RuleAction a = new RuleAction();
        a.setActionType(type);
        a.setActionValue(String.valueOf(value));
        return a;
    }

    private <T extends ItilItem> T itil(T item, String title, String content, ItilStatus status, User assignee, UserGroup group,
                                        LocalDateTime opened, Long orgUnitId) {
        item.setTitle(title);
        item.setContent(content);
        item.setStatus(status);
        item.setAssigneeId(assignee.getId());
        item.setAssigneeGroupId(group.getId());
        item.setRequesterId(assignee.getId());
        item.setOpenedAt(opened);
        item.setOrgUnitId(orgUnitId);
        item.setUrgency(3 + random.nextInt(2));
        item.setImpact(3 + random.nextInt(2));
        item.setPriority(PriorityMatrix.compute(item.getUrgency(), item.getImpact()));
        item.setLastActivityAt(opened.plusHours(4));
        if (status == ItilStatus.SOLVED || status == ItilStatus.CLOSED) {
            item.setSolvedAt(opened.plusDays(3));
        }
        if (status == ItilStatus.CLOSED) {
            item.setClosedAt(opened.plusDays(3).plusHours(2));
        }
        return item;
    }

    private Problem problem(String title, String cause, ItilStatus status, User assignee, UserGroup group, LocalDateTime opened,
                            Long orgUnitId) {
        Problem p = itil(new Problem(), title, cause, status, assignee, group, opened, orgUnitId);
        p.setSymptomContent(title);
        p.setCauseContent(status == ItilStatus.SOLVED ? cause : null);
        return put(p, opened);
    }

    private void change(String title, ChangeType type, RiskLevel risk, ItilStatus status, LocalDateTime planned, User assignee,
                        UserGroup group, LocalDateTime opened, Long orgUnitId) {
        Change c = itil(new Change(), title, title + ".", status, assignee, group, opened, orgUnitId);
        c.setChangeType(type);
        c.setRisk(risk);
        c.setPlannedStartAt(planned.withHour(22).withMinute(0).withSecond(0));
        c.setPlannedEndAt(c.getPlannedStartAt().plusHours(type == ChangeType.STANDARD ? 1 : 4));
        c.setRolloutPlan(t("1. Comunicar usuários\n2. Executar procedimento\n3. Validar serviços",
                "1. Notify users\n2. Run the procedure\n3. Validate services"));
        c.setBackoutPlan(t("Restaurar configuração anterior a partir do backup.", "Restore the previous configuration from backup."));
        put(c, opened);
    }

    private Project project(String code, String name, String description, Long state, User manager, UserGroup group,
                            LocalDate start, LocalDate end, Long orgUnitId, String color, LocalDateTime now) {
        Project p = new Project();
        p.setCode(code);
        p.setName(name);
        p.setDescription(description);
        p.setStateId(state);
        p.setManagerId(manager.getId());
        p.setGroupId(group.getId());
        p.setPlanStart(start);
        p.setPlanEnd(end);
        p.setOrgUnitId(orgUnitId);
        p.setColor(color);
        p.setPriority(3);
        p.setProgress(0);
        p = put(p, now.minusDays(30));
        ProjectMember m = new ProjectMember();
        m.setProjectId(p.getId());
        m.setUserId(manager.getId());
        m.setRoleName(t("Gerente do projeto", "Project manager"));
        put(m, now.minusDays(30));
        return p;
    }

    private void tasks(Project project, String[][] spec, List<User> team, LocalDate start, LocalDateTime now, int[][] deps) {
        List<ProjectTask> created = new ArrayList<>();
        int order = 1;
        for (String[] s : spec) {
            ProjectTask task = new ProjectTask();
            task.setProjectId(project.getId());
            task.setName(s[0]);
            task.setPlanStart(start.plusDays(Integer.parseInt(s[1])));
            task.setPlanEnd(start.plusDays(Integer.parseInt(s[2])));
            task.setStatus(ProjectTaskStatus.valueOf(s[3]));
            task.setMilestone(s.length > 4);
            task.setAssigneeId(team.get((order - 1) % team.size()).getId());
            task.setSortOrder(order++);
            task.setEffortHours(task.isMilestone() ? 0 : 8 * (Integer.parseInt(s[2]) - Integer.parseInt(s[1]) + 1));
            task.setProgress(switch (task.getStatus()) {
                case DONE -> 100;
                case REVIEW -> 90;
                case IN_PROGRESS -> 40 + random.nextInt(30);
                default -> 0;
            });
            if (task.getStatus() != ProjectTaskStatus.TODO) {
                task.setRealStart(task.getPlanStart());
            }
            if (task.getStatus() == ProjectTaskStatus.DONE) {
                task.setRealEnd(task.getPlanEnd());
            }
            created.add(put(task, now.minusDays(25)));
        }
        for (int[] d : deps) {
            ProjectTaskDependency dep = new ProjectTaskDependency();
            dep.setTaskId(created.get(d[0]).getId());
            dep.setPredecessorId(created.get(d[1]).getId());
            dep.setLagDays(0);
            put(dep, now.minusDays(25));
        }
        for (User u : team) {
            if (!repo(ProjectMember.class).exists(Filter.and(Filter.eq("projectId", project.getId()), Filter.eq("userId", u.getId())))) {
                ProjectMember m = new ProjectMember();
                m.setProjectId(project.getId());
                m.setUserId(u.getId());
                m.setRoleName(t("Equipe", "Team"));
                put(m, now.minusDays(25));
            }
        }
    }

    private void kb(String title, Long category, boolean faq, String content, User author, LocalDateTime now) {
        KbArticle a = new KbArticle();
        a.setOrgUnitId(services.orgUnits().root().getId());
        a.setTitle(title);
        a.setCategoryId(category);
        a.setFaq(faq);
        a.setPublished(true);
        a.setContent(content);
        a.setAuthorId(author.getId());
        a.setUpdatedById(author.getId());
        a.setViewCount(10 + random.nextInt(240));
        put(a, now.minusDays(10 + random.nextInt(60)));
    }

    private void recurring(String name, String title, Periodicity periodicity, UserGroup group, Long orgUnitId, LocalDateTime now) {
        RecurringTicket r = new RecurringTicket();
        r.setName(name);
        r.setTitle(title);
        r.setContent(title + ".");
        r.setTicketType(TicketType.REQUEST);
        r.setPeriodicity(periodicity);
        r.setIntervalCount(1);
        r.setAssigneeGroupId(group.getId());
        r.setOrgUnitId(orgUnitId);
        r.setUrgency(2);
        r.setImpact(2);
        r.setActive(true);
        r.setNextRunAt(now.plusDays(periodicity == Periodicity.WEEKLY ? 3 : 12).withHour(8).withMinute(0).withSecond(0));
        put(r, now.minusDays(60));
    }
}
