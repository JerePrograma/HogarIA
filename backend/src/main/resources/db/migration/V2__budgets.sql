CREATE TABLE budget_year (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 profile_id UUID NOT NULL REFERENCES financial_profile(id),
 year INT NOT NULL CHECK (year BETWEEN 2000 AND 2100),
 target_income NUMERIC(19,2),
 target_saving NUMERIC(19,2),
 notes VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now(),
 UNIQUE(profile_id, year)
);
CREATE TABLE budget_month (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 budget_year_id UUID NOT NULL REFERENCES budget_year(id),
 month SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
 notes VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now(),
 UNIQUE(budget_year_id, month)
);
CREATE TABLE budget_category_item (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 budget_month_id UUID NOT NULL REFERENCES budget_month(id),
 category_id UUID NOT NULL REFERENCES category(id),
 budget_amount NUMERIC(19,2) NOT NULL CHECK (budget_amount >= 0),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 updated_at TIMESTAMP NOT NULL DEFAULT now(),
 UNIQUE(budget_month_id, category_id)
);
CREATE INDEX idx_budget_year_profile_year ON budget_year(profile_id, year);
CREATE INDEX idx_budget_month_year_month ON budget_month(budget_year_id, month);
CREATE INDEX idx_budget_item_month ON budget_category_item(budget_month_id);
CREATE INDEX idx_budget_item_category ON budget_category_item(category_id);
