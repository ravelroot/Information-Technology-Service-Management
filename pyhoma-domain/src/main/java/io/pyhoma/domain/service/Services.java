package io.pyhoma.domain.service;

import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.port.DataStore;
import io.pyhoma.domain.security.SecurityContext;
import java.time.Clock;

public final class Services {

    public static final String VERSION = "1.0.0";

    private final DataStore store;
    private final Clock clock;
    private final SecurityContext security = new SecurityContext();

    private final SettingsService settings = new SettingsService(this);
    private final AuditService audit = new AuditService(this);
    private final NotificationService notifications = new NotificationService(this);
    private final OrgUnitService orgUnits = new OrgUnitService(this);
    private final DropdownService dropdowns = new DropdownService(this);
    private final UserService users = new UserService(this);
    private final GroupService groups = new GroupService(this);
    private final ProfileService profiles = new ProfileService(this);
    private final AuthService auth = new AuthService(this);
    private final LookupService lookups = new LookupService(this);
    private final SlaService slas = new SlaService(this);
    private final RuleService rules = new RuleService(this);
    private final TicketService tickets = new TicketService(this);
    private final ProblemService problems = new ProblemService(this);
    private final ChangeService changes = new ChangeService(this);
    private final ItilActivityService itilActivity = new ItilActivityService(this);
    private final RecurringTicketService recurringTickets = new RecurringTicketService(this);
    private final AssetService assets = new AssetService(this);
    private final SoftwareService software = new SoftwareService(this);
    private final SupplierService suppliers = new SupplierService(this);
    private final ContractService contracts = new ContractService(this);
    private final DocumentService documents = new DocumentService(this);
    private final ProjectService projects = new ProjectService(this);
    private final KnowledgeBaseService knowledgeBase = new KnowledgeBaseService(this);
    private final DashboardService dashboard = new DashboardService(this);
    private final ReportService reports = new ReportService(this);
    private final SearchService search = new SearchService(this);
    private final JobService jobs = new JobService(this);
    private final InstallationService installation = new InstallationService(this);
    private final DemoDataService demoData = new DemoDataService(this);

    public Services(DataStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public Services(DataStore store) {
        this(store, Clock.systemDefaultZone());
    }

    public DataStore store() {
        return store;
    }

    public Clock clock() {
        return clock;
    }

    public SecurityContext security() {
        return security;
    }

    public SettingsService settings() {
        return settings;
    }

    public AuditService audit() {
        return audit;
    }

    public NotificationService notifications() {
        return notifications;
    }

    public OrgUnitService orgUnits() {
        return orgUnits;
    }

    public DropdownService dropdowns() {
        return dropdowns;
    }

    public UserService users() {
        return users;
    }

    public GroupService groups() {
        return groups;
    }

    public ProfileService profiles() {
        return profiles;
    }

    public AuthService auth() {
        return auth;
    }

    public LookupService lookups() {
        return lookups;
    }

    public SlaService slas() {
        return slas;
    }

    public RuleService rules() {
        return rules;
    }

    public TicketService tickets() {
        return tickets;
    }

    public ProblemService problems() {
        return problems;
    }

    public ChangeService changes() {
        return changes;
    }

    public ItilService<? extends ItilItem> itil(ItilType type) {
        return switch (type) {
            case TICKET -> tickets;
            case PROBLEM -> problems;
            case CHANGE -> changes;
        };
    }

    public ItilActivityService itilActivity() {
        return itilActivity;
    }

    public RecurringTicketService recurringTickets() {
        return recurringTickets;
    }

    public AssetService assets() {
        return assets;
    }

    public SoftwareService software() {
        return software;
    }

    public SupplierService suppliers() {
        return suppliers;
    }

    public ContractService contracts() {
        return contracts;
    }

    public DocumentService documents() {
        return documents;
    }

    public ProjectService projects() {
        return projects;
    }

    public KnowledgeBaseService knowledgeBase() {
        return knowledgeBase;
    }

    public DashboardService dashboard() {
        return dashboard;
    }

    public ReportService reports() {
        return reports;
    }

    public SearchService search() {
        return search;
    }

    public JobService jobs() {
        return jobs;
    }

    public InstallationService installation() {
        return installation;
    }

    public DemoDataService demoData() {
        return demoData;
    }
}
