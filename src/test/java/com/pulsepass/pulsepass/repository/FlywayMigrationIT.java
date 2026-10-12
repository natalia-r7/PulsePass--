package com.pulsepass.pulsepass.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("pulsepass")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Test
    void shouldApplyAllFlywayMigrationsFromEmptyDatabase() {

        Flyway flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .load();

        flyway.migrate();

        var info = flyway.info();

        assertEquals(3, info.applied().length);

        assertEquals(
                "1",
                info.applied()[0].getVersion().getVersion()
        );

        assertEquals(
                "2",
                info.applied()[1].getVersion().getVersion()
        );

        assertEquals(
                "3",
                info.applied()[2].getVersion().getVersion()
        );
    }
}