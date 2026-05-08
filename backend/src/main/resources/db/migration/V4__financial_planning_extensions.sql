CREATE TABLE financial_goal (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 name VARCHAR(120) NOT NULL,
 goal_type VARCHAR(40) NOT NULL,
 target_amount NUMERIC(19,2) NOT NULL CHECK (target_amount > 0),
 current_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (current_amount >= 0),
 monthly_target_amount NUMERIC(19,2),
 target_date DATE,
 priority INT NOT NULL DEFAULT 3 CHECK (priority BETWEEN 1 AND 5),
 status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
 notes TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE habit (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 name VARCHAR(160) NOT NULL,
 description TEXT,
 area VARCHAR(60) NOT NULL DEFAULT 'FINANZAS',
 frequency VARCHAR(30) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE habit_checkin (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 habit_id UUID NOT NULL REFERENCES habit(id) ON DELETE CASCADE,
 check_date DATE NOT NULL,
 done BOOLEAN NOT NULL DEFAULT FALSE,
 notes TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now(),
 UNIQUE(habit_id, check_date)
);

CREATE TABLE inflation_index (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 year INT NOT NULL CHECK (year BETWEEN 2000 AND 2100),
 month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
 rate NUMERIC(12,8) NOT NULL,
 source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
 observed BOOLEAN NOT NULL DEFAULT TRUE,
 notes TEXT,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now(),
 UNIQUE(year, month, source)
);
