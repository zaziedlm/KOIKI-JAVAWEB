package org.koikifw.buildsupport.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

final class MigrationFixtureSupport {

    private static final String URL_ENVIRONMENT = "KOIKI_P2_C1_JDBC_URL";
    private static final String USER_ENVIRONMENT = "KOIKI_P2_C1_JDBC_USER";
    private static final String PASSWORD_ENVIRONMENT = "KOIKI_P2_C1_JDBC_PASSWORD";

    private MigrationFixtureSupport() {}

    static String jdbcUrl() {
        return requiredEnvironment(URL_ENVIRONMENT);
    }

    static String username() {
        return requiredEnvironment(USER_ENVIRONMENT);
    }

    static String password() {
        return requiredEnvironment(PASSWORD_ENVIRONMENT);
    }

    static ConfigurableApplicationContext start(Map<String, Object> overrides) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl());
        properties.put("spring.datasource.username", username());
        properties.put("spring.datasource.password", password());
        properties.put("spring.datasource.hikari.maximum-pool-size", "3");
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.jpa.open-in-view", "false");
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.main.web-application-type", "none");
        List<String> arguments = new java.util.ArrayList<>();
        arguments.add("--debug=false");
        arguments.add("--spring.main.log-startup-info=false");
        arguments.add("--logging.level.root=WARN");
        overrides.forEach((name, value) ->
                arguments.add("--" + name + "=" + value));
        return new SpringApplicationBuilder(MigrationFixtureApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties)
                .run(arguments.toArray(String[]::new));
    }

    static ConfigurableApplicationContext start() {
        return start(Map.of());
    }

    static JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    static JdbcClient jdbcClient() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(jdbcUrl(), username(), password());
        return JdbcClient.create(dataSource);
    }

    static List<String> history(JdbcClient jdbcClient, String table) {
        return jdbcClient.sql("""
                        SELECT installed_rank || '|' || coalesce(version, '') || '|' ||
                               description || '|' || type || '|' || script || '|' ||
                               coalesce(checksum::text, '') || '|' || success
                        FROM %s
                        ORDER BY installed_rank
                        """.formatted(table))
                .query(String.class)
                .list();
    }

    static List<String> sqlVersions(JdbcClient jdbcClient, String table) {
        return jdbcClient.sql("""
                        SELECT version
                        FROM %s
                        WHERE type = 'SQL' AND success
                        ORDER BY installed_rank
                        """.formatted(table))
                .query(String.class)
                .list();
    }

    static List<String> applicationTables(JdbcClient jdbcClient) {
        return jdbcClient.sql("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_type = 'BASE TABLE'
                          AND (table_name LIKE 'koiki_%'
                               OR table_name LIKE 'kkref_%'
                               OR table_name LIKE 'spring_session%')
                          AND table_name NOT IN ('koiki_flyway_history', 'flyway_schema_history')
                        ORDER BY table_name
                        """)
                .query(String.class)
                .list();
    }

    static int count(JdbcClient jdbcClient, String sql) {
        return jdbcClient.sql(sql).query(Integer.class).single();
    }

    static boolean tableExists(JdbcClient jdbcClient, String table) {
        return jdbcClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.tables
                            WHERE table_schema = 'public' AND table_name = :table)
                        """)
                .param("table", table)
                .query(Boolean.class)
                .single();
    }

    static void resetPublicSchema() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl(), username(), password());
                Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required P2-C1 fixture environment is missing: " + name);
        }
        return value;
    }
}
