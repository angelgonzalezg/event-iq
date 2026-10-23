package com.eventiq;

import java.io.IOException;
import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.MountableFile;

/**
 * A disposable Oracle Database Free for repository tests, with the tables from
 * {@code db/schema.sql}. Import it in a {@code @DataJpaTest}; every test class that imports it
 * shares the same container. See {@code docs/testing.md}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class OracleTestDatabase {

	/**
	 * Same Oracle version as the application's container (README, step 2) in the image the
	 * Testcontainers Oracle Free module is built for. Pinned like every other version (ADR-008).
	 */
	static final String IMAGE = "gvenzl/oracle-free:23.26.3-slim-faststart";

	private static final String SCHEMA_IN_CONTAINER = "/tmp/schema.sql";

	@Bean
	@ServiceConnection
	@SuppressWarnings("resource")
	OracleContainer oracleContainer() throws IOException, InterruptedException {
		OracleContainer oracle = new OracleContainer(IMAGE)
			.withCopyFileToContainer(MountableFile.forHostPath("db/schema.sql"), SCHEMA_IN_CONTAINER)
			.withStartupTimeout(Duration.ofMinutes(3));
		oracle.start();
		createSchema(oracle);
		return oracle;
	}

	/**
	 * Runs {@code db/schema.sql} with SQL*Plus inside the container, the same way the README
	 * does: the script uses SQL*Plus commands that JDBC cannot run.
	 */
	private static void createSchema(OracleContainer oracle) throws IOException, InterruptedException {
		String login = "%s/%s@//localhost:1521/%s".formatted(oracle.getUsername(), oracle.getPassword(),
				oracle.getDatabaseName());
		ExecResult result = oracle.execInContainer("sqlplus", "-S", "-L", login, "@" + SCHEMA_IN_CONTAINER);
		if (result.getExitCode() != 0) {
			throw new IllegalStateException("db/schema.sql failed in the test database:%n%s%s"
				.formatted(result.getStdout(), result.getStderr()));
		}
	}

}
