package org.koikifw.reference.externalcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.ExternalCheckService;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {"koiki.identity.local-authentication.enabled=false",
        "spring.session.jdbc.initialize-schema=never", "koiki.reference.external-check.enabled=true",
        "koiki.api.http.read-timeout=1500ms"})
@Import(ReferencePostgreSqlTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExternalCheckContextPostgreSqlTest {
    private static final ExternalRateFixture PROVIDER = provider();
    @Autowired private ExternalCheckService service;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private HikariDataSource pool;
    private static final Query QUERY = new Query("USD", "JPY", LocalDate.of(2026, 10, 22));

    private static ExternalRateFixture provider() {
        try { return new ExternalRateFixture(); }
        catch (java.io.IOException failure) { throw new IllegalStateException("Could not start owned fixture", failure); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("koiki.reference.external-check.provider-base-uri", () -> PROVIDER.baseUri().toString());
    }

    @BeforeEach void authenticate() { authenticateCurrentThread(); PROVIDER.mode = ExternalRateFixture.Mode.NORMAL; }

    private static void authenticateCurrentThread() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "external-fixture", "unused", List.of(new SimpleGrantedAuthority("EXTERNALCHECK:USE"))));
    }

    @org.junit.jupiter.api.AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }
    @AfterAll static void stopProvider() { PROVIDER.close(); }

    @Test void i01LookupDoesNotStartABusinessTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(service.lookup(QUERY).rate()).isEqualTo("150.12345678");
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
    }

    @Test void i02ExistingTransactionIsRejectedBeforeProviderArrival() {
        int before = PROVIDER.requests.get();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertThatThrownBy(() -> service.lookup(QUERY)).isInstanceOfSatisfying(ExternalCheckFailure.class,
                    failure -> assertThat(failure.kind()).isEqualTo(ExternalCheckFailure.Kind.INVALID_CONTEXT));
        });
        assertThat(PROVIDER.requests.get()).isEqualTo(before);
        assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
    }

    @Test void i03DelayedHttpDoesNotHoldTheDatabasePool() throws Exception {
        PROVIDER.mode = ExternalRateFixture.Mode.HOLD;
        PROVIDER.entered = new java.util.concurrent.CountDownLatch(1);
        var worker = Executors.newSingleThreadExecutor();
        try {
            var response = worker.submit(() -> {
                authenticateCurrentThread();
                try { return service.lookup(QUERY); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(PROVIDER.entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
            try (var connection = pool.getConnection(); var statement = connection.createStatement();
                    var rows = statement.executeQuery("select 1")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(1);
            }
            PROVIDER.release.countDown();
            assertThat(response.get(2, TimeUnit.SECONDS).rate()).isEqualTo("150.12345678");
        } finally {
            PROVIDER.release.countDown(); worker.shutdownNow();
            assertThat(worker.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        }
    }
}
