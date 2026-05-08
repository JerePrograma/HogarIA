CREATE TABLE financial_goal (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 name VARCHAR(160) NOT NULL,
 goal_type VARCHAR(40) NOT NULL CHECK (goal_type IN ('EMERGENCY_FUND','DEBT_PAYOFF','SAVING_TARGET','INVESTMENT','BUSINESS','TRAVEL','EDUCATION','OTHER')),
 target_amount NUMERIC(19,2) NOT NULL CHECK (target_amount >= 0),
 current_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (current_amount >= 0),
 monthly_contribution NUMERIC(19,2),
 target_date DATE,
 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','CANCELLED')),
 notes VARCHAR(1000),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_financial_goal_profile ON financial_goal(profile_id);
CREATE INDEX idx_financial_goal_status ON financial_goal(status);
CREATE INDEX idx_financial_goal_type ON financial_goal(goal_type);

CREATE TABLE habit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 description VARCHAR(255) NOT NULL,
 area VARCHAR(80),
 frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY')),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_habit_profile ON habit(profile_id);
CREATE INDEX idx_habit_active ON habit(active);

CREATE TABLE habit_checkin (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 habit_id UUID NOT NULL REFERENCES habit(id) ON DELETE CASCADE,
 checkin_date DATE NOT NULL,
 completed BOOLEAN NOT NULL DEFAULT FALSE,
 note VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 UNIQUE(habit_id, checkin_date)
);
CREATE INDEX idx_habit_checkin_habit ON habit_checkin(habit_id);
CREATE INDEX idx_habit_checkin_date ON habit_checkin(checkin_date);

CREATE TABLE inflation_index (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 year INT NOT NULL CHECK (year BETWEEN 2000 AND 2100),
 month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
 category_code VARCHAR(80),
 category_name VARCHAR(160),
 monthly_rate NUMERIC(12,6) NOT NULL,
 source VARCHAR(120),
 projection BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_inflation_year_month_category
ON inflation_index (year, month, COALESCE(category_code, 'GENERAL'));
CREATE INDEX idx_inflation_year ON inflation_index(year);
CREATE INDEX idx_inflation_month ON inflation_index(month);
CREATE INDEX idx_inflation_projection ON inflation_index(projection);

CREATE TABLE excel_import_batch (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 original_file_name VARCHAR(255),
 status VARCHAR(30) NOT NULL CHECK (status IN ('PREVIEWED','PROCESSING','COMPLETED','FAILED')),
 detected_profile_type VARCHAR(30),
 year INT,
 currency VARCHAR(3) NOT NULL DEFAULT 'ARS',
 summary_json JSONB,
 warnings_json JSONB,
 errors_json JSONB,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_excel_import_batch_profile ON excel_import_batch(profile_id);
CREATE INDEX idx_excel_import_batch_status ON excel_import_batch(status);

CREATE TABLE excel_import_row (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 batch_id UUID NOT NULL REFERENCES excel_import_batch(id) ON DELETE CASCADE,
 sheet_name VARCHAR(120) NOT NULL,
 row_number INT,
 concept VARCHAR(255),
 month INT,
 amount NUMERIC(19,2),
 target_entity VARCHAR(40) CHECK (target_entity IN ('CATEGORY','ACCOUNT','INCOME','EXPENSE','SAVING','BUDGET','CARD','GOAL','HABIT','HABIT_CHECKIN','INFLATION','UNKNOWN')),
 status VARCHAR(30) NOT NULL CHECK (status IN ('READY','IMPORTED','SKIPPED','WARNING','ERROR')),
 error_message VARCHAR(1000),
 raw_json JSONB,
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_excel_import_row_batch ON excel_import_row(batch_id);
CREATE INDEX idx_excel_import_row_status ON excel_import_row(status);
