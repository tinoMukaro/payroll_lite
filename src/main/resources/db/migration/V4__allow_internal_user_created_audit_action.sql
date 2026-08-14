-- Keep the database audit-action constraint aligned with AuditAction.
-- PostgreSQL check constraints cannot be extended in place, so replace it.

ALTER TABLE audit_events
    DROP CONSTRAINT ck_audit_events_action;

ALTER TABLE audit_events
    ADD CONSTRAINT ck_audit_events_action CHECK (action IN (
        'ADMIN_BOOTSTRAPPED',
        'USER_REGISTERED',
        'INTERNAL_USER_CREATED',
        'USER_ROLE_CHANGED',
        'EMPLOYEE_CREATED',
        'EMPLOYEE_UPDATED',
        'EMPLOYEE_DELETED',
        'NSSA_RULE_CREATED',
        'NSSA_RULE_UPDATED',
        'PAYE_TABLE_CREATED',
        'PAYE_TABLE_UPDATED',
        'RECURRING_PAY_ITEM_CREATED',
        'RECURRING_PAY_ITEM_UPDATED',
        'PAYROLL_RUN_CREATED',
        'PAYROLL_RUN_PROCESSED',
        'PAYROLL_ADJUSTMENT_CREATED',
        'PAYROLL_ADJUSTMENT_DELETED',
        'PAYSLIP_DOWNLOADED'
    ));
