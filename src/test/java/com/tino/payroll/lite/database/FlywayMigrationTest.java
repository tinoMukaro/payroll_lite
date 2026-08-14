package com.tino.payroll.lite.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FlywayMigrationTest {

    @Test
    void migrationsBuildACompleteSchemaFromAnEmptyDatabase() {
        assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the Flyway migration integration test"
        );

        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .cleanDisabled(true)
                    .load();

            var result = flyway.migrate();
            assertEquals(3, result.migrationsExecuted);

            var dataSource = new DriverManagerDataSource(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
            );
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            Integer tableCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name IN (
                        'users', 'employees', 'nssa_rules', 'paye_tax_tables',
                        'paye_tax_bands', 'payroll_runs', 'payroll_adjustments',
                        'recurring_pay_items', 'payslips', 'payslip_line_items',
                        'audit_events'
                      )
                    """, Integer.class);
            assertEquals(11, tableCount);

            Integer successfulMigrations = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                    Integer.class
            );
            assertEquals(3, successfulMigrations);
            assertTrue(flyway.validateWithResult().validationSuccessful);

            Long auditId = jdbc.queryForObject("""
                    INSERT INTO audit_events (
                        actor_email, action, entity_type, entity_id, details
                    ) VALUES (
                        'SYSTEM', 'EMPLOYEE_CREATED', 'EMPLOYEE', 42, 'test event'
                    ) RETURNING id
                    """, Long.class);
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.dao.DataAccessException.class,
                    () -> jdbc.update("UPDATE audit_events SET details = 'changed' WHERE id = ?", auditId)
            );
            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.dao.DataAccessException.class,
                    () -> jdbc.update("DELETE FROM audit_events WHERE id = ?", auditId)
            );
        }
    }
}
