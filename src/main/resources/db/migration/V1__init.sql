CREATE TABLE schools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    address VARCHAR(500),
    website VARCHAR(255),
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    primary_color VARCHAR(20) DEFAULT '#1B4B8A',
    secondary_color VARCHAR(20) DEFAULT '#F4B400',
    accent_color VARCHAR(20) DEFAULT '#0F9D58',
    custom_domain VARCHAR(255),
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Dar_es_Salaam',
    locale VARCHAR(20) NOT NULL DEFAULT 'en',
    currency VARCHAR(10) NOT NULL DEFAULT 'TZS',
    country VARCHAR(80),
    status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    subscription_plan VARCHAR(30) NOT NULL DEFAULT 'STARTER',
    trial_ends_at DATETIME(6),
    finance_enabled TINYINT(1) NOT NULL DEFAULT 1,
    attendance_enabled TINYINT(1) NOT NULL DEFAULT 1,
    exams_enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_schools_slug (slug),
    UNIQUE KEY uk_schools_custom_domain (custom_domain),
    KEY idx_schools_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    phone VARCHAR(30),
    avatar_url VARCHAR(500),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_school (school_id),
    KEY idx_users_role (role),
    CONSTRAINT fk_users_school FOREIGN KEY (school_id) REFERENCES schools (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_user (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    current_year TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_academic_year_school_name (school_id, name),
    KEY idx_academic_year_school (school_id),
    CONSTRAINT fk_academic_year_school FOREIGN KEY (school_id) REFERENCES schools (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE school_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    code VARCHAR(30) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_class_school_year_code (school_id, academic_year_id, code),
    KEY idx_class_school (school_id),
    KEY idx_class_year (academic_year_id),
    CONSTRAINT fk_class_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_class_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teachers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    employee_id VARCHAR(50) NOT NULL,
    qualification VARCHAR(150),
    specialization VARCHAR(150),
    department VARCHAR(100),
    joining_date DATE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_teacher_user (user_id),
    UNIQUE KEY uk_teacher_employee (school_id, employee_id),
    KEY idx_teacher_school (school_id),
    CONSTRAINT fk_teacher_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    capacity INT,
    class_teacher_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_section_class_name (school_class_id, name),
    KEY idx_section_school (school_id),
    CONSTRAINT fk_section_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_section_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id),
    CONSTRAINT fk_section_teacher FOREIGN KEY (class_teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    admission_no VARCHAR(50) NOT NULL,
    roll_number VARCHAR(30),
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    admission_date DATE,
    address VARCHAR(500),
    emergency_contact VARCHAR(30),
    academic_year_id BIGINT,
    school_class_id BIGINT,
    section_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_student_user (user_id),
    UNIQUE KEY uk_student_admission (school_id, admission_no),
    KEY idx_student_school (school_id),
    KEY idx_student_class (school_class_id),
    KEY idx_student_section (section_id),
    CONSTRAINT fk_student_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_student_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id),
    CONSTRAINT fk_student_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id),
    CONSTRAINT fk_student_section FOREIGN KEY (section_id) REFERENCES sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    occupation VARCHAR(120),
    address VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_parent_user (user_id),
    KEY idx_parent_school (school_id),
    CONSTRAINT fk_parent_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_parent_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_parents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    relationship VARCHAR(30) NOT NULL,
    primary_contact TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_student_parent (student_id, parent_id),
    KEY idx_sp_school (school_id),
    CONSTRAINT fk_sp_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_sp_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_sp_parent FOREIGN KEY (parent_id) REFERENCES parents (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_subject_school_code (school_id, code),
    KEY idx_subject_school (school_id),
    CONSTRAINT fk_subject_school FOREIGN KEY (school_id) REFERENCES schools (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teacher_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    section_id BIGINT,
    academic_year_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_teacher_subject_alloc (teacher_id, subject_id, school_class_id, section_id, academic_year_id),
    KEY idx_ts_school (school_id),
    CONSTRAINT fk_ts_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_ts_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_ts_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id),
    CONSTRAINT fk_ts_section FOREIGN KEY (section_id) REFERENCES sections (id),
    CONSTRAINT fk_ts_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE timetable_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(50),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_tt_school (school_id),
    KEY idx_tt_section_day (section_id, day_of_week),
    CONSTRAINT fk_tt_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_tt_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id),
    CONSTRAINT fk_tt_section FOREIGN KEY (section_id) REFERENCES sections (id),
    CONSTRAINT fk_tt_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_tt_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    school_class_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    exam_type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    published TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_exam_school (school_id),
    KEY idx_exam_class (school_class_id),
    CONSTRAINT fk_exam_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_exam_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id),
    CONSTRAINT fk_exam_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exam_subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    max_marks DECIMAL(8,2) NOT NULL,
    pass_marks DECIMAL(8,2) NOT NULL,
    exam_date DATE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_exam_subject (exam_id, subject_id),
    KEY idx_es_school (school_id),
    CONSTRAINT fk_es_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_es_exam FOREIGN KEY (exam_id) REFERENCES exams (id) ON DELETE CASCADE,
    CONSTRAINT fk_es_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    exam_subject_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    marks_obtained DECIMAL(8,2) NOT NULL,
    remarks VARCHAR(255),
    graded_by_teacher_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_grade_exam_student_subject (exam_id, student_id, subject_id),
    KEY idx_grade_school (school_id),
    KEY idx_grade_student (student_id),
    CONSTRAINT fk_grade_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_grade_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_grade_exam_subject FOREIGN KEY (exam_subject_id) REFERENCES exam_subjects (id),
    CONSTRAINT fk_grade_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_grade_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_grade_teacher FOREIGN KEY (graded_by_teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_attendances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    marked_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_student_attendance (student_id, attendance_date),
    KEY idx_sa_school_date (school_id, attendance_date),
    KEY idx_sa_section_date (section_id, attendance_date),
    CONSTRAINT fk_sa_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_sa_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_sa_section FOREIGN KEY (section_id) REFERENCES sections (id),
    CONSTRAINT fk_sa_marker FOREIGN KEY (marked_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teacher_attendances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    marked_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_teacher_attendance (teacher_id, attendance_date),
    KEY idx_ta_school_date (school_id, attendance_date),
    CONSTRAINT fk_ta_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_ta_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_ta_marker FOREIGN KEY (marked_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_structures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    school_class_id BIGINT,
    name VARCHAR(150) NOT NULL,
    fee_type VARCHAR(30) NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    due_date DATE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_fee_school (school_id),
    KEY idx_fee_class (school_class_id),
    CONSTRAINT fk_fee_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_fee_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id),
    CONSTRAINT fk_fee_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date DATE,
    issued_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_invoice_number (school_id, invoice_number),
    KEY idx_invoice_student (student_id),
    KEY idx_invoice_status (school_id, status),
    CONSTRAINT fk_invoice_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_invoice_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_invoice_year FOREIGN KEY (academic_year_id) REFERENCES academic_years (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    fee_structure_id BIGINT,
    description VARCHAR(200) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_ii_invoice (invoice_id),
    CONSTRAINT fk_ii_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_ii_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT fk_ii_fee FOREIGN KEY (fee_structure_id) REFERENCES fee_structures (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    transaction_ref VARCHAR(100),
    receipt_number VARCHAR(50) NOT NULL,
    paid_at DATETIME(6) NOT NULL,
    recorded_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_receipt_number (school_id, receipt_number),
    KEY idx_payment_invoice (invoice_id),
    KEY idx_payment_student (student_id),
    CONSTRAINT fk_payment_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_payment_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_payment_recorder FOREIGN KEY (recorded_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    audience VARCHAR(30) NOT NULL DEFAULT 'ALL',
    school_class_id BIGINT,
    published TINYINT(1) NOT NULL DEFAULT 0,
    publish_at DATETIME(6),
    expires_at DATETIME(6),
    created_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_notice_school (school_id),
    KEY idx_notice_audience (school_id, audience, published),
    CONSTRAINT fk_notice_school FOREIGN KEY (school_id) REFERENCES schools (id),
    CONSTRAINT fk_notice_class FOREIGN KEY (school_class_id) REFERENCES school_classes (id),
    CONSTRAINT fk_notice_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
