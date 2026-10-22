CREATE TABLE pyh_asset (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    asset_type VARCHAR(30) NOT NULL,
    name VARCHAR(200) NOT NULL,
    serial_number VARCHAR(120),
    asset_tag VARCHAR(120),
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
    hostname VARCHAR(150),
    ip_address VARCHAR(60),
    mac_address VARCHAR(60),
    operating_system_id BIGINT,
    os_version VARCHAR(80),
    cpu VARCHAR(150),
    ram_mb INTEGER,
    storage_gb INTEGER,
    last_inventory_at DATETIME(6),
    notes LONGTEXT,
    deleted BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_asset_connection (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    asset_id BIGINT NOT NULL,
    connected_asset_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    item_id BIGINT,
    user_id BIGINT,
    user_name VARCHAR(150),
    action VARCHAR(20) NOT NULL,
    field_name VARCHAR(80),
    old_value VARCHAR(2000),
    new_value VARCHAR(2000),
    occurred_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_change (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    status VARCHAR(20) NOT NULL,
    urgency INTEGER NOT NULL,
    impact INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    category_id BIGINT,
    location_id BIGINT,
    requester_id BIGINT,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    opened_at DATETIME(6) NOT NULL,
    due_at DATETIME(6),
    solved_at DATETIME(6),
    closed_at DATETIME(6),
    last_activity_at DATETIME(6),
    action_time_minutes INTEGER NOT NULL,
    deleted BOOLEAN NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    risk VARCHAR(20) NOT NULL,
    impact_content LONGTEXT,
    rollout_plan LONGTEXT,
    backout_plan LONGTEXT,
    checklist LONGTEXT,
    planned_start_at DATETIME(6),
    planned_end_at DATETIME(6),
    approval_status VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_contract (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    contract_number VARCHAR(80),
    type_id BIGINT,
    supplier_id BIGINT,
    start_date DATE,
    duration_months INTEGER,
    notice_months INTEGER,
    renewal VARCHAR(20) NOT NULL,
    cost DECIMAL(15,2),
    notes LONGTEXT,
    deleted BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_contract_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    contract_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    category_id BIGINT,
    uri VARCHAR(1000) NOT NULL,
    notes LONGTEXT,
    deleted BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_document_link (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    document_id BIGINT NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    item_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_dropdown (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    parent_id BIGINT,
    complete_name VARCHAR(600),
    color VARCHAR(20),
    flag BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_group (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    parent_id BIGINT,
    assignable BOOLEAN NOT NULL,
    requester BOOLEAN NOT NULL,
    email VARCHAR(150),
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_group_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    manager BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_holiday (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(150) NOT NULL,
    holiday_date DATE NOT NULL,
    recurring BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_approval (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    requested_by_id BIGINT,
    approver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_comment VARCHAR(2000),
    answer_comment VARCHAR(2000),
    answered_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_asset (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_followup (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT,
    content LONGTEXT NOT NULL,
    private_note BOOLEAN NOT NULL,
    source_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_observer (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    user_id BIGINT,
    group_id BIGINT,
    actor_role VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_relation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    created_by_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_solution (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT,
    solution_type_id BIGINT,
    content LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    answered_by_id BIGINT,
    answered_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_itil_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT,
    technician_id BIGINT,
    group_id BIGINT,
    content LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    category_id BIGINT,
    planned_start_at DATETIME(6),
    planned_end_at DATETIME(6),
    duration_minutes INTEGER NOT NULL,
    private_note BOOLEAN NOT NULL,
    done_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_job_lock (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    job_name VARCHAR(80) NOT NULL,
    locked_until DATETIME(6),
    locked_by VARCHAR(150),
    last_run_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_kb_article (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    category_id BIGINT,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    faq BOOLEAN NOT NULL,
    published BOOLEAN NOT NULL,
    author_id BIGINT,
    updated_by_id BIGINT,
    view_count INTEGER NOT NULL,
    deleted BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000),
    item_type VARCHAR(40),
    item_id BIGINT,
    seen BOOLEAN NOT NULL,
    severity VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_org_unit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(150) NOT NULL,
    parent_id BIGINT,
    complete_name VARCHAR(600),
    depth INTEGER NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(60),
    address VARCHAR(300),
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_problem (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    status VARCHAR(20) NOT NULL,
    urgency INTEGER NOT NULL,
    impact INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    category_id BIGINT,
    location_id BIGINT,
    requester_id BIGINT,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    opened_at DATETIME(6) NOT NULL,
    due_at DATETIME(6),
    solved_at DATETIME(6),
    closed_at DATETIME(6),
    last_activity_at DATETIME(6),
    action_time_minutes INTEGER NOT NULL,
    deleted BOOLEAN NOT NULL,
    impact_content LONGTEXT,
    cause_content LONGTEXT,
    symptom_content LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(100) NOT NULL,
    interface_type VARCHAR(20) NOT NULL,
    default_profile BOOLEAN NOT NULL,
    protected_profile BOOLEAN NOT NULL,
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_profile_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    include_children BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_profile_right (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    profile_id BIGINT NOT NULL,
    module VARCHAR(30) NOT NULL,
    rights INTEGER NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_project (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    code VARCHAR(40),
    name VARCHAR(200) NOT NULL,
    description LONGTEXT,
    state_id BIGINT,
    priority INTEGER NOT NULL,
    manager_id BIGINT,
    group_id BIGINT,
    plan_start DATE,
    plan_end DATE,
    real_start DATE,
    real_end DATE,
    progress INTEGER NOT NULL,
    parent_id BIGINT,
    color VARCHAR(20),
    deleted BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_project_dependency (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    task_id BIGINT NOT NULL,
    predecessor_id BIGINT NOT NULL,
    lag_days INTEGER NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_project_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_name VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_project_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    project_id BIGINT NOT NULL,
    parent_id BIGINT,
    name VARCHAR(200) NOT NULL,
    description LONGTEXT,
    status VARCHAR(20) NOT NULL,
    plan_start DATE,
    plan_end DATE,
    real_start DATE,
    real_end DATE,
    progress INTEGER NOT NULL,
    assignee_id BIGINT,
    effort_hours INTEGER,
    milestone BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_recurring_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    ticket_type VARCHAR(20) NOT NULL,
    category_id BIGINT,
    urgency INTEGER NOT NULL,
    impact INTEGER NOT NULL,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    periodicity VARCHAR(20) NOT NULL,
    interval_count INTEGER NOT NULL,
    next_run_at DATETIME(6),
    last_run_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    trigger_event VARCHAR(20) NOT NULL,
    ranking INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    match_all BOOLEAN NOT NULL,
    stop_processing BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_rule_action (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    rule_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    action_value VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_rule_criterion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    rule_id BIGINT NOT NULL,
    field VARCHAR(30) NOT NULL,
    match_operator VARCHAR(20) NOT NULL,
    pattern VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_setting (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    setting_key VARCHAR(120) NOT NULL,
    setting_value VARCHAR(2000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_sla (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    sla_type VARCHAR(10) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    business_hours BOOLEAN NOT NULL,
    priority INTEGER,
    active BOOLEAN NOT NULL,
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_software (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    publisher_id BIGINT,
    category_id BIGINT,
    notes LONGTEXT,
    deleted BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_software_install (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    asset_id BIGINT NOT NULL,
    software_id BIGINT NOT NULL,
    software_version VARCHAR(60),
    license_id BIGINT,
    installed_on DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_software_license (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    software_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    license_key VARCHAR(300),
    seats INTEGER,
    expires_on DATE,
    supplier_id BIGINT,
    purchase_cost DECIMAL(15,2),
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_supplier (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    type_id BIGINT,
    email VARCHAR(150),
    phone VARCHAR(60),
    website VARCHAR(300),
    address VARCHAR(300),
    city VARCHAR(120),
    active BOOLEAN NOT NULL,
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT,
    status VARCHAR(20) NOT NULL,
    urgency INTEGER NOT NULL,
    impact INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    category_id BIGINT,
    location_id BIGINT,
    requester_id BIGINT,
    assignee_id BIGINT,
    assignee_group_id BIGINT,
    opened_at DATETIME(6) NOT NULL,
    due_at DATETIME(6),
    solved_at DATETIME(6),
    closed_at DATETIME(6),
    last_activity_at DATETIME(6),
    action_time_minutes INTEGER NOT NULL,
    deleted BOOLEAN NOT NULL,
    ticket_type VARCHAR(20) NOT NULL,
    source_id BIGINT,
    sla_tto_id BIGINT,
    sla_ttr_id BIGINT,
    tto_due_at DATETIME(6),
    taken_at DATETIME(6),
    waiting_since DATETIME(6),
    waiting_minutes INTEGER NOT NULL,
    sla_warned BOOLEAN NOT NULL,
    sla_breach_notified BOOLEAN NOT NULL,
    satisfaction INTEGER,
    satisfaction_comment VARCHAR(2000),
    approval_status VARCHAR(20) NOT NULL,
    recurring_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pyh_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    login VARCHAR(80) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(60),
    job_title VARCHAR(120),
    password_hash VARCHAR(255),
    active BOOLEAN NOT NULL,
    language VARCHAR(10),
    theme VARCHAR(20),
    default_org_unit_id BIGINT,
    location_id BIGINT,
    last_login_at DATETIME(6),
    failed_attempts INTEGER NOT NULL,
    locked_until DATETIME(6),
    must_change_password BOOLEAN NOT NULL,
    notes LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
