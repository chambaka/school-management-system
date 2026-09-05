CREATE TABLE tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    country VARCHAR(80),
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Dar_es_Salaam',
    currency VARCHAR(10) NOT NULL DEFAULT 'TZS',
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    subscription_plan VARCHAR(30) NOT NULL DEFAULT 'STARTER',
    trial_ends_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_tenants_slug (slug),
    KEY idx_tenants_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE schools
    ADD COLUMN tenant_id BIGINT NULL AFTER id;

ALTER TABLE users
    ADD COLUMN tenant_id BIGINT NULL AFTER id,
    ADD COLUMN campus_id BIGINT NULL AFTER school_id;

INSERT INTO tenants (
    name, slug, email, phone, country, timezone, currency, status, subscription_plan, trial_ends_at, created_at, updated_at
)
SELECT
    name,
    CONCAT(slug, '-org'),
    email,
    phone,
    country,
    timezone,
    currency,
    status,
    subscription_plan,
    trial_ends_at,
    created_at,
    updated_at
FROM schools;

UPDATE schools s
INNER JOIN tenants t ON t.slug = CONCAT(s.slug, '-org')
SET s.tenant_id = t.id;

CREATE TABLE campuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(30) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(30),
    email VARCHAR(150),
    timezone VARCHAR(80),
    primary_campus TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_campus_school_code (school_id, code),
    KEY idx_campus_tenant (tenant_id),
    KEY idx_campus_school (school_id),
    CONSTRAINT fk_campus_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_campus_school FOREIGN KEY (school_id) REFERENCES schools (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO campuses (
    tenant_id, school_id, name, code, address, phone, email, timezone, primary_campus, created_at, updated_at
)
SELECT
    tenant_id,
    id,
    'Main campus',
    'MAIN',
    address,
    phone,
    email,
    timezone,
    1,
    created_at,
    updated_at
FROM schools
WHERE tenant_id IS NOT NULL;

UPDATE users u
INNER JOIN schools s ON u.school_id = s.id
SET u.tenant_id = s.tenant_id;

UPDATE users u
INNER JOIN campuses c ON c.school_id = u.school_id AND c.primary_campus = 1
SET u.campus_id = c.id
WHERE u.school_id IS NOT NULL;

ALTER TABLE schools
    MODIFY tenant_id BIGINT NOT NULL,
    ADD KEY idx_schools_tenant (tenant_id),
    ADD CONSTRAINT fk_schools_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id);

ALTER TABLE users
    ADD KEY idx_users_tenant (tenant_id),
    ADD KEY idx_users_campus (campus_id),
    ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    ADD CONSTRAINT fk_users_campus FOREIGN KEY (campus_id) REFERENCES campuses (id);
