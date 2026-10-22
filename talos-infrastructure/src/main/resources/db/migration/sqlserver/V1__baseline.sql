CREATE TABLE pyh_asset (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    asset_type NVARCHAR(30) NOT NULL,
    name NVARCHAR(200) NOT NULL,
    serial_number NVARCHAR(120),
    asset_tag NVARCHAR(120),
    state_id BIGINT,
    location_id BIGINT,
    manufacturer_id BIGINT,
    model_id BIGINT,
    user_id BIGINT,
    group_id BIGINT,
    tech_user_id BIGINT,
    tech_group_id BIGINT,
    supplier_id BIGINT,
    purchase_date DATE,
    warranty_expires_on DATE,
    purchase_cost DECIMAL(15,2),
    hostname NVARCHAR(150),
    ip_address NVARCHAR(60),
    mac_address NVARCHAR(60),
    operating_system_id BIGINT,
    os_version NVARCHAR(80),
    cpu NVARCHAR(150),
    ram_mb INT,
    storage_gb INT,
    last_inventory_at DATETIME2,
    notes NVARCHAR(MAX),
    deleted BIT NOT NULL
);

CREATE TABLE pyh_asset_connection (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    asset_id BIGINT NOT NULL,
    connected_asset_id BIGINT NOT NULL
);

CREATE TABLE pyh_audit_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(40) NOT NULL,
    item_id BIGINT,
    user_id BIGINT,
    user_name NVARCHAR(150),
    action NVARCHAR(20) NOT NULL,
    field_name NVARCHAR(80),
    old_value NVARCHAR(2000),
    new_value NVARCHAR(2000),
    occurred_at DATETIME2 NOT NULL
);

CREATE TABLE pyh_change (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    status NVARCHAR(20) NOT NULL,
    urgency INT NOT NULL,
    impact INT NOT NULL,
    priority INT NOT NULL,
    category_id BIGINT,
    location_id BIGINT,
    requester_id BIGINT,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    opened_at DATETIME2 NOT NULL,
    due_at DATETIME2,
    solved_at DATETIME2,
    closed_at DATETIME2,
    last_activity_at DATETIME2,
    action_time_minutes INT NOT NULL,
    deleted BIT NOT NULL,
    change_type NVARCHAR(20) NOT NULL,
    risk NVARCHAR(20) NOT NULL,
    impact_content NVARCHAR(MAX),
    rollout_plan NVARCHAR(MAX),
    backout_plan NVARCHAR(MAX),
    checklist NVARCHAR(MAX),
    planned_start_at DATETIME2,
    planned_end_at DATETIME2,
    approval_status NVARCHAR(20) NOT NULL
);

CREATE TABLE pyh_contract (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    contract_number NVARCHAR(80),
    type_id BIGINT,
    supplier_id BIGINT,
    start_date DATE,
    duration_months INT,
    notice_months INT,
    renewal NVARCHAR(20) NOT NULL,
    cost DECIMAL(15,2),
    notes NVARCHAR(MAX),
    deleted BIT NOT NULL
);

CREATE TABLE pyh_contract_item (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    contract_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL
);

CREATE TABLE pyh_document (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    category_id BIGINT,
    uri NVARCHAR(1000) NOT NULL,
    notes NVARCHAR(MAX),
    deleted BIT NOT NULL
);

CREATE TABLE pyh_document_link (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    document_id BIGINT NOT NULL,
    item_type NVARCHAR(40) NOT NULL,
    item_id BIGINT NOT NULL
);

CREATE TABLE pyh_dropdown (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    kind NVARCHAR(40) NOT NULL,
    name NVARCHAR(150) NOT NULL,
    parent_id BIGINT,
    complete_name NVARCHAR(600),
    color NVARCHAR(20),
    flag BIT NOT NULL,
    active BIT NOT NULL,
    sort_order INT NOT NULL,
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_group (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(150) NOT NULL,
    parent_id BIGINT,
    assignable BIT NOT NULL,
    requester BIT NOT NULL,
    email NVARCHAR(150),
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_group_member (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    manager BIT NOT NULL
);

CREATE TABLE pyh_holiday (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    name NVARCHAR(150) NOT NULL,
    holiday_date DATE NOT NULL,
    recurring BIT NOT NULL
);

CREATE TABLE pyh_itil_approval (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    requested_by_id BIGINT,
    approver_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL,
    request_comment NVARCHAR(2000),
    answer_comment NVARCHAR(2000),
    answered_at DATETIME2
);

CREATE TABLE pyh_itil_asset (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL
);

CREATE TABLE pyh_itil_followup (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT,
    content NVARCHAR(MAX) NOT NULL,
    private_note BIT NOT NULL,
    source_id BIGINT
);

CREATE TABLE pyh_itil_observer (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    user_id BIGINT,
    group_id BIGINT,
    actor_role NVARCHAR(20) NOT NULL
);

CREATE TABLE pyh_itil_relation (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    source_type NVARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type NVARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    relation_type NVARCHAR(20) NOT NULL,
    created_by_id BIGINT
);

CREATE TABLE pyh_itil_solution (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT,
    solution_type_id BIGINT,
    content NVARCHAR(MAX) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    answered_by_id BIGINT,
    answered_at DATETIME2
);

CREATE TABLE pyh_itil_task (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    item_type NVARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT,
    technician_id BIGINT,
    group_id BIGINT,
    content NVARCHAR(MAX) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    category_id BIGINT,
    planned_start_at DATETIME2,
    planned_end_at DATETIME2,
    duration_minutes INT NOT NULL,
    private_note BIT NOT NULL,
    done_at DATETIME2
);

CREATE TABLE pyh_job_lock (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    job_name NVARCHAR(80) NOT NULL,
    locked_until DATETIME2,
    locked_by NVARCHAR(150),
    last_run_at DATETIME2
);

CREATE TABLE pyh_kb_article (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    category_id BIGINT,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    faq BIT NOT NULL,
    published BIT NOT NULL,
    author_id BIGINT,
    updated_by_id BIGINT,
    view_count INT NOT NULL,
    deleted BIT NOT NULL
);

CREATE TABLE pyh_notification (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    user_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    message NVARCHAR(2000),
    item_type NVARCHAR(40),
    item_id BIGINT,
    seen BIT NOT NULL,
    severity NVARCHAR(20)
);

CREATE TABLE pyh_org_unit (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    name NVARCHAR(150) NOT NULL,
    parent_id BIGINT,
    complete_name NVARCHAR(600),
    depth INT NOT NULL,
    email NVARCHAR(150),
    phone NVARCHAR(60),
    address NVARCHAR(300),
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_problem (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    status NVARCHAR(20) NOT NULL,
    urgency INT NOT NULL,
    impact INT NOT NULL,
    priority INT NOT NULL,
    category_id BIGINT,
    location_id BIGINT,
    requester_id BIGINT,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    opened_at DATETIME2 NOT NULL,
    due_at DATETIME2,
    solved_at DATETIME2,
    closed_at DATETIME2,
    last_activity_at DATETIME2,
    action_time_minutes INT NOT NULL,
    deleted BIT NOT NULL,
    impact_content NVARCHAR(MAX),
    cause_content NVARCHAR(MAX),
    symptom_content NVARCHAR(MAX)
);

CREATE TABLE pyh_profile (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    name NVARCHAR(100) NOT NULL,
    interface_type NVARCHAR(20) NOT NULL,
    default_profile BIT NOT NULL,
    protected_profile BIT NOT NULL,
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_profile_assignment (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    user_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    include_children BIT NOT NULL
);

CREATE TABLE pyh_profile_right (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    profile_id BIGINT NOT NULL,
    module NVARCHAR(30) NOT NULL,
    rights INT NOT NULL
);

CREATE TABLE pyh_project (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    code NVARCHAR(40),
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    state_id BIGINT,
    priority INT NOT NULL,
    manager_id BIGINT,
    group_id BIGINT,
    plan_start DATE,
    plan_end DATE,
    real_start DATE,
    real_end DATE,
    progress INT NOT NULL,
    parent_id BIGINT,
    color NVARCHAR(20),
    deleted BIT NOT NULL
);

CREATE TABLE pyh_project_dependency (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    task_id BIGINT NOT NULL,
    predecessor_id BIGINT NOT NULL,
    lag_days INT NOT NULL
);

CREATE TABLE pyh_project_member (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_name NVARCHAR(100)
);

CREATE TABLE pyh_project_task (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    project_id BIGINT NOT NULL,
    parent_id BIGINT,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    status NVARCHAR(20) NOT NULL,
    plan_start DATE,
    plan_end DATE,
    real_start DATE,
    real_end DATE,
    progress INT NOT NULL,
    assignee_id BIGINT,
    effort_hours INT,
    milestone BIT NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE pyh_recurring_ticket (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(150) NOT NULL,
    active BIT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    ticket_type NVARCHAR(20) NOT NULL,
    category_id BIGINT,
    urgency INT NOT NULL,
    impact INT NOT NULL,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    periodicity NVARCHAR(20) NOT NULL,
    interval_count INT NOT NULL,
    next_run_at DATETIME2,
    last_run_at DATETIME2
);

CREATE TABLE pyh_rule (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    name NVARCHAR(150) NOT NULL,
    description NVARCHAR(1000),
    trigger_event NVARCHAR(20) NOT NULL,
    ranking INT NOT NULL,
    active BIT NOT NULL,
    match_all BIT NOT NULL,
    stop_processing BIT NOT NULL
);

CREATE TABLE pyh_rule_action (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    rule_id BIGINT NOT NULL,
    action_type NVARCHAR(30) NOT NULL,
    action_value NVARCHAR(500)
);

CREATE TABLE pyh_rule_criterion (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    rule_id BIGINT NOT NULL,
    field NVARCHAR(30) NOT NULL,
    match_operator NVARCHAR(20) NOT NULL,
    pattern NVARCHAR(500)
);

CREATE TABLE pyh_setting (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    setting_key NVARCHAR(120) NOT NULL,
    setting_value NVARCHAR(2000)
);

CREATE TABLE pyh_sla (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(150) NOT NULL,
    sla_type NVARCHAR(10) NOT NULL,
    duration_minutes INT NOT NULL,
    business_hours BIT NOT NULL,
    priority INT,
    active BIT NOT NULL,
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_software (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    publisher_id BIGINT,
    category_id BIGINT,
    notes NVARCHAR(MAX),
    deleted BIT NOT NULL
);

CREATE TABLE pyh_software_install (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    asset_id BIGINT NOT NULL,
    software_id BIGINT NOT NULL,
    software_version NVARCHAR(60),
    license_id BIGINT,
    installed_on DATE
);

CREATE TABLE pyh_software_license (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    software_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    license_key NVARCHAR(300),
    seats INT,
    expires_on DATE,
    supplier_id BIGINT,
    purchase_cost DECIMAL(15,2),
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_supplier (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name NVARCHAR(200) NOT NULL,
    type_id BIGINT,
    email NVARCHAR(150),
    phone NVARCHAR(60),
    website NVARCHAR(300),
    address NVARCHAR(300),
    city NVARCHAR(120),
    active BIT NOT NULL,
    notes NVARCHAR(MAX)
);

CREATE TABLE pyh_ticket (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    org_unit_id BIGINT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content NVARCHAR(MAX),
    status NVARCHAR(20) NOT NULL,
    urgency INT NOT NULL,
    impact INT NOT NULL,
    priority INT NOT NULL,
    category_id BIGINT,
    location_id BIGINT,
    requester_id BIGINT,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    opened_at DATETIME2 NOT NULL,
    due_at DATETIME2,
    solved_at DATETIME2,
    closed_at DATETIME2,
    last_activity_at DATETIME2,
    action_time_minutes INT NOT NULL,
    deleted BIT NOT NULL,
    ticket_type NVARCHAR(20) NOT NULL,
    source_id BIGINT,
    sla_tto_id BIGINT,
    sla_ttr_id BIGINT,
    tto_due_at DATETIME2,
    taken_at DATETIME2,
    waiting_since DATETIME2,
    waiting_minutes INT NOT NULL,
    sla_warned BIT NOT NULL,
    sla_breach_notified BIT NOT NULL,
    satisfaction INT,
    satisfaction_comment NVARCHAR(2000),
    approval_status NVARCHAR(20) NOT NULL,
    recurring_id BIGINT
);

CREATE TABLE pyh_user (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    version BIGINT,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    login NVARCHAR(80) NOT NULL,
    full_name NVARCHAR(150) NOT NULL,
    email NVARCHAR(150),
    phone NVARCHAR(60),
    job_title NVARCHAR(120),
    password_hash NVARCHAR(255),
    active BIT NOT NULL,
    language NVARCHAR(10),
    theme NVARCHAR(20),
    default_org_unit_id BIGINT,
    location_id BIGINT,
    last_login_at DATETIME2,
    failed_attempts INT NOT NULL,
    locked_until DATETIME2,
    must_change_password BIT NOT NULL,
    notes NVARCHAR(MAX)
);

CREATE INDEX ix_asset_org_unit_id ON pyh_asset (org_unit_id);
CREATE INDEX ix_asset_asset_type ON pyh_asset (asset_type);
CREATE INDEX ix_asset_serial_number ON pyh_asset (serial_number);
CREATE INDEX ix_asset_asset_tag ON pyh_asset (asset_tag);
CREATE INDEX ix_asset_state_id ON pyh_asset (state_id);
CREATE INDEX ix_asset_location_id ON pyh_asset (location_id);
CREATE INDEX ix_asset_user_id ON pyh_asset (user_id);
CREATE INDEX ix_asset_connection_asset_id ON pyh_asset_connection (asset_id);
CREATE INDEX ix_asset_connection_conne_9ac6 ON pyh_asset_connection (connected_asset_id);
CREATE INDEX ix_audit_log_item_type ON pyh_audit_log (item_type);
CREATE INDEX ix_audit_log_item_id ON pyh_audit_log (item_id);
CREATE INDEX ix_audit_log_occurred_at ON pyh_audit_log (occurred_at);
CREATE INDEX ix_contract_org_unit_id ON pyh_contract (org_unit_id);
CREATE INDEX ix_contract_supplier_id ON pyh_contract (supplier_id);
CREATE INDEX ix_contract_item_contract_id ON pyh_contract_item (contract_id);
CREATE INDEX ix_contract_item_asset_id ON pyh_contract_item (asset_id);
CREATE INDEX ix_document_org_unit_id ON pyh_document (org_unit_id);
CREATE INDEX ix_document_link_document_id ON pyh_document_link (document_id);
CREATE INDEX ix_document_link_item_id ON pyh_document_link (item_id);
CREATE INDEX ix_dropdown_kind ON pyh_dropdown (kind);
CREATE INDEX ix_group_org_unit_id ON pyh_group (org_unit_id);
CREATE INDEX ix_group_member_group_id ON pyh_group_member (group_id);
CREATE INDEX ix_group_member_user_id ON pyh_group_member (user_id);
CREATE INDEX ix_itil_approval_item_id ON pyh_itil_approval (item_id);
CREATE INDEX ix_itil_approval_approver_id ON pyh_itil_approval (approver_id);
CREATE INDEX ix_itil_asset_item_id ON pyh_itil_asset (item_id);
CREATE INDEX ix_itil_asset_asset_id ON pyh_itil_asset (asset_id);
CREATE INDEX ix_itil_followup_item_id ON pyh_itil_followup (item_id);
CREATE INDEX ix_itil_observer_item_id ON pyh_itil_observer (item_id);
CREATE INDEX ix_itil_observer_user_id ON pyh_itil_observer (user_id);
CREATE INDEX ix_itil_relation_source_id ON pyh_itil_relation (source_id);
CREATE INDEX ix_itil_relation_target_id ON pyh_itil_relation (target_id);
CREATE INDEX ix_itil_solution_item_id ON pyh_itil_solution (item_id);
CREATE INDEX ix_itil_task_item_id ON pyh_itil_task (item_id);
CREATE INDEX ix_itil_task_technician_id ON pyh_itil_task (technician_id);
CREATE UNIQUE INDEX ux_job_lock_job_name ON pyh_job_lock (job_name);
CREATE INDEX ix_kb_article_org_unit_id ON pyh_kb_article (org_unit_id);
CREATE INDEX ix_kb_article_category_id ON pyh_kb_article (category_id);
CREATE INDEX ix_notification_user_id ON pyh_notification (user_id);
CREATE INDEX ix_org_unit_parent_id ON pyh_org_unit (parent_id);
CREATE UNIQUE INDEX ux_profile_name ON pyh_profile (name);
CREATE INDEX ix_profile_assignment_user_id ON pyh_profile_assignment (user_id);
CREATE INDEX ix_profile_right_profile_id ON pyh_profile_right (profile_id);
CREATE INDEX ix_project_org_unit_id ON pyh_project (org_unit_id);
CREATE INDEX ix_project_dependency_task_id ON pyh_project_dependency (task_id);
CREATE INDEX ix_project_dependency_pre_d848 ON pyh_project_dependency (predecessor_id);
CREATE INDEX ix_project_member_project_id ON pyh_project_member (project_id);
CREATE INDEX ix_project_task_project_id ON pyh_project_task (project_id);
CREATE INDEX ix_project_task_assignee_id ON pyh_project_task (assignee_id);
CREATE INDEX ix_recurring_ticket_org_u_c5ce ON pyh_recurring_ticket (org_unit_id);
CREATE INDEX ix_rule_action_rule_id ON pyh_rule_action (rule_id);
CREATE INDEX ix_rule_criterion_rule_id ON pyh_rule_criterion (rule_id);
CREATE UNIQUE INDEX ux_setting_setting_key ON pyh_setting (setting_key);
CREATE INDEX ix_sla_org_unit_id ON pyh_sla (org_unit_id);
CREATE INDEX ix_software_org_unit_id ON pyh_software (org_unit_id);
CREATE INDEX ix_software_install_asset_id ON pyh_software_install (asset_id);
CREATE INDEX ix_software_install_softw_c3f5 ON pyh_software_install (software_id);
CREATE INDEX ix_software_install_license_id ON pyh_software_install (license_id);
CREATE INDEX ix_software_license_org_u_8556 ON pyh_software_license (org_unit_id);
CREATE INDEX ix_software_license_softw_68f2 ON pyh_software_license (software_id);
CREATE INDEX ix_supplier_org_unit_id ON pyh_supplier (org_unit_id);
CREATE UNIQUE INDEX ux_user_login ON pyh_user (login);
