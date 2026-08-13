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
            assertEquals(2, result.migrationsExecuted);

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
                        'recurring_pay_items', 'payslips', 'payslip_line_items'
                      )
                    """, Integer.class);
            assertEquals(10, tableCount);

            Integer successfulMigrations = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success",
                    Integer.class
            );
            assertEquals(2, successfulMigrations);
            assertTrue(flyway.validateWithResult().validationSuccessful);
        }
    }
}
