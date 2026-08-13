-- Add database-level invariants that protect payroll data even when it is
-- written outside the application, plus indexes for the main lookup paths.

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'HR', 'EMPLOYEE'));

ALTER TABLE employees
    ADD CONSTRAINT ck_employees_basic_salary CHECK (basic_salary >= 0),
    ADD CONSTRAINT ck_employees_currency CHECK (salary_currency IN ('USD', 'ZWG')),
    ADD CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED'));

ALTER TABLE nssa_rules
    ADD CONSTRAINT ck_nssa_rules_currency CHECK (currency IN ('USD', 'ZWG')),
    ADD CONSTRAINT ck_nssa_rules_employee_rate CHECK (employee_rate BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_nssa_rules_employer_rate CHECK (employer_rate BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_nssa_rules_ceiling CHECK (pensionable_earnings_ceiling >= 0),
    ADD CONSTRAINT ck_nssa_rules_dates CHECK (effective_to IS NULL OR effective_to >= effective_from);

ALTER TABLE paye_tax_tables
    ADD CONSTRAINT ck_paye_tables_currency CHECK (currency IN ('USD', 'ZWG')),
    ADD CONSTRAINT ck_paye_tables_aids_levy_rate CHECK (aids_levy_rate BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_paye_tables_dates CHECK (effective_to IS NULL OR effective_to >= effective_from);

ALTER TABLE paye_tax_bands
    ADD CONSTRAINT ck_paye_bands_lower_bound CHECK (lower_bound >= 0),
    ADD CONSTRAINT ck_paye_bands_upper_bound CHECK (upper_bound IS NULL OR upper_bound > lower_bound),
    ADD CONSTRAINT ck_paye_bands_rate CHECK (tax_rate BETWEEN 0 AND 1);

ALTER TABLE payroll_runs
    ADD CONSTRAINT ck_payroll_runs_month CHECK (month BETWEEN 1 AND 12),
    ADD CONSTRAINT ck_payroll_runs_year CHECK (year >= 2000),
    ADD CONSTRAINT ck_payroll_runs_currency CHECK (currency IN ('USD', 'ZWG')),
    ADD CONSTRAINT ck_payroll_runs_status CHECK (status IN ('DRAFT', 'PROCESSED', 'CANCELLED')),
    ADD CONSTRAINT ck_payroll_runs_processed_at CHECK (
        (status = 'PROCESSED' AND processed_at IS NOT NULL)
        OR (status <> 'PROCESSED' AND processed_at IS NULL)
    );

ALTER TABLE payroll_adjustments
    ADD CONSTRAINT ck_payroll_adjustments_type CHECK (type IN ('EARNING', 'DEDUCTION')),
    ADD CONSTRAINT ck_payroll_adjustments_amount CHECK (amount > 0);

ALTER TABLE recurring_pay_items
    ADD CONSTRAINT ck_recurring_pay_items_type CHECK (type IN ('EARNING', 'DEDUCTION')),
    ADD CONSTRAINT ck_recurring_pay_items_amount CHECK (amount > 0),
    ADD CONSTRAINT ck_recurring_pay_items_dates CHECK (effective_to IS NULL OR effective_to >= effective_from);

ALTER TABLE payslips
    ADD CONSTRAINT ck_payslips_currency CHECK (currency IN ('USD', 'ZWG')),
    ADD CONSTRAINT ck_payslips_basic_salary CHECK (basic_salary >= 0),
    ADD CONSTRAINT ck_payslips_gross_salary CHECK (gross_salary >= 0),
    ADD CONSTRAINT ck_payslips_pensionable_earnings CHECK (pensionable_earnings >= 0),
    ADD CONSTRAINT ck_payslips_nssa_deduction CHECK (nssa_deduction >= 0),
    ADD CONSTRAINT ck_payslips_employer_nssa CHECK (employer_nssa_contribution >= 0),
    ADD CONSTRAINT ck_payslips_paye CHECK (paye_deduction >= 0),
    ADD CONSTRAINT ck_payslips_total_deductions CHECK (total_deductions >= 0);

ALTER TABLE payslip_line_items
    ADD CONSTRAINT ck_payslip_line_items_type CHECK (type IN ('EARNING', 'DEDUCTION')),
    ADD CONSTRAINT ck_payslip_line_items_amount CHECK (amount > 0),
    ADD CONSTRAINT ck_payslip_line_items_source CHECK (source IS NULL OR source IN ('ONE_OFF', 'RECURRING'));

CREATE UNIQUE INDEX uk_users_email_case_insensitive ON users (LOWER(email));
CREATE UNIQUE INDEX uk_employees_email_case_insensitive ON employees (LOWER(email));
CREATE INDEX idx_employees_status_currency ON employees (status, salary_currency);
CREATE INDEX idx_nssa_rules_lookup ON nssa_rules (currency, active, effective_from, effective_to);
CREATE INDEX idx_paye_tax_tables_lookup ON paye_tax_tables (currency, active, effective_from, effective_to);
CREATE INDEX idx_payroll_adjustments_run ON payroll_adjustments (payroll_run_id);
CREATE INDEX idx_payroll_adjustments_employee ON payroll_adjustments (employee_id);
CREATE INDEX idx_payslips_payroll_run ON payslips (payroll_run_id);
CREATE INDEX idx_payslip_line_items_payslip ON payslip_line_items (payslip_id);
