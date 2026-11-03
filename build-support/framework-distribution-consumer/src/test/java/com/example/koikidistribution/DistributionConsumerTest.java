package com.example.koikidistribution;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.IdentityQuery;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/** One finite integration journey: migration, coexistence, HTTP denial, restart and scan boundary. */
class DistributionConsumerTest {
    private static Map<String, Object> properties(boolean explicitScan) {
        return Map.of(
                "spring.datasource.url", System.getenv("E1_JDBC_URL"),
                "spring.datasource.username", "e1_consumer",
                "spring.datasource.password", System.getenv("E1_DB_PASSWORD"),
                "consumer.explicit-entity-scan", explicitScan,
                "spring.security.user.password", "unused-local-deny-all-fixture");
    }

    private static ConfigurableApplicationContext start(boolean explicitScan) {
        return new SpringApplicationBuilder(ConsumerApplication.class).properties(properties(explicitScan)).run();
    }

    @Test
    @Timeout(120)
    void fixedDistributionConsumerJourney() throws Exception {
        int frameworkHistory;
        try (var context = start(true)) {
            var entityManager = context.getBean(EntityManagerFactory.class);
            Set<String> entities = entityManager.getMetamodel().getEntities().stream()
                    .map(entity -> entity.getJavaType().getSimpleName()).collect(Collectors.toSet());
            assertTrue(entities.containsAll(Set.of("CustomerRecord", "IdentityUserEntity", "AuditEventEntity")));
            System.out.println("E1-C01 PASS Customer / Identity / Audit entity coexistence");
            assertNotNull(context.getBean(IdentityQuery.class));
            assertNotNull(context.getBean(BusinessAuditRecorder.class));
            System.out.println("E1-C02 PASS public Framework service beans");
            var repository = context.getBean(CustomerRepository.class);
            repository.saveAndFlush(new CustomerRecord(1, "fixed-preview"));
            assertEquals("fixed-preview", repository.findById(1L).orElseThrow().getDescription());
            System.out.println("E1-C03 PASS Customer Repository persistence");
            var jdbc = context.getBean(JdbcTemplate.class);
            assertEquals("public", jdbc.queryForObject("select current_schema()", String.class));
            frameworkHistory = jdbc.queryForObject("select count(*) from koiki_flyway_history where success", Integer.class);
            assertTrue(frameworkHistory >= 2);
            assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where type = 'SQL' and success", Integer.class));
            System.out.println("E1-C04 PASS same schema / separate histories / Framework-before-Customer SQL prerequisite");
            assertEquals(false, jdbc.queryForObject("select rolsuper from pg_roles where rolname=current_user", Boolean.class));
            System.out.println("E1-C05 PASS non-superuser application connection");
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            try (var client = HttpClient.newHttpClient()) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/consumer-probe")).GET().build(),
                        HttpResponse.BodyHandlers.discarding());
                assertEquals(401, response.statusCode());
            }
            System.out.println("E1-C06 PASS real Web startup / default deny");
        }
        try (var context = start(true)) {
            assertEquals("fixed-preview", context.getBean(CustomerRepository.class).findById(1L).orElseThrow().getDescription());
            var jdbc = context.getBean(JdbcTemplate.class);
            assertEquals(frameworkHistory, jdbc.queryForObject("select count(*) from koiki_flyway_history where success", Integer.class));
            assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where type = 'SQL' and success", Integer.class));
            System.out.println("E1-C07 PASS same DB restart / no repeated SQL migration / persisted data");
        }
        var failure = assertThrows(RuntimeException.class, () -> {
            try (var ignored = start(false)) {
                fail("Default scan must not be assumed to include Customer Entity when Framework EntityScan is present");
            }
        });
        StringBuilder chain = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) { chain.append(t.getMessage()).append('\n'); }
        assertTrue(chain.toString().contains("Not a managed type") && chain.toString().contains("CustomerRecord"), chain.toString());
        System.out.println("E1-C08 PASS R-05 boundary: Customer-owned explicit EntityScan required in this configuration");
    }
}
