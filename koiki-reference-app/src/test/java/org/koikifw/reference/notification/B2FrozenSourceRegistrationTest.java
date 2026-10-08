package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import jakarta.persistence.EntityManager;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.koikifw.audit.*;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.reference.notification.configuration.*;
import org.koikifw.reference.notification.adapter.outbound.configuration.FrozenRecoverySourceSettings;
import org.koikifw.reference.notification.adapter.outbound.operational.JdbcProtectedRecoveryTargetAdapter;
import org.koikifw.reference.notification.application.*;
import org.koikifw.reference.notification.application.port.outbound.*;
import org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.*;
import org.springframework.transaction.PlatformTransactionManager;

/** Registration-only branches use no live supplier; positive save/read is independently exercised. */
class B2FrozenSourceRegistrationTest {
    @TempDir Path directory;
    private static final String P=FrozenRecoverySourceConfiguration.PREFIX;
    private static final String SECRET=UUID.randomUUID().toString();
    private ApplicationContextRunner base(Map<String,Object> secrets) {
        return new ApplicationContextRunner().withUserConfiguration(NotificationFoundationConfiguration.class)
            .withBean(EntityManager.class,()->mock(EntityManager.class)).withBean(IdentityQuery.class,()->mock(IdentityQuery.class))
            .withBean(BusinessAuditRecorder.class,()->mock(BusinessAuditRecorder.class)).withBean(SecurityAuditRecorder.class,()->mock(SecurityAuditRecorder.class))
            .withBean(PlatformTransactionManager.class,()->mock(PlatformTransactionManager.class))
            .withPropertyValues("koiki.reference.notification.foundation.enabled=true")
            .withInitializer(c -> c.getEnvironment().getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,secrets)));
    }
    private Map<String,Object> secrets() {return Map.of("B2_SOURCE_JDBC_URL","jdbc:postgresql://127.0.0.1:5432/test","B2_SOURCE_READER_USERNAME","b2_reader","B2_SOURCE_READER_PASSWORD",SECRET);}
    private ApplicationContextRunner selected(Map<String,Object> secrets) {
        return base(secrets).withPropertyValues(P+"mode=tooling-jdbc-v1",P+"environment-id=test-environment",P+"run-id="+UUID.randomUUID(),
            P+"source-id=test-source",P+"expected-jar-sha256="+"a".repeat(64),P+"expected-revision=1");
    }
    @Test void C01_defaultDisabled() {
        base(Map.of()).run(c -> {assertThat(c).hasNotFailed();assertThat(c).doesNotHaveBean(RecoveryIssueProtectionPort.class);
            assertThat(c).doesNotHaveBean(ProtectedRecoveryIssueService.class);assertThat(c.getBean(RecoveryTargetPort.class).current("test",UUID.randomUUID())).isEmpty();});
    }
    @Test void C02_explicitSingleAdapterAndEmptyEvidence() {
        selected(secrets()).run(c -> {assertThat(c).hasNotFailed();assertThat(c.getBeansOfType(RecoveryTargetPort.class)).hasSize(1);
            assertThat(c.getBean(RecoveryTargetPort.class)).isSameAs(c.getBean(RecoveryIssueProtectionPort.class)).isInstanceOf(JdbcProtectedRecoveryTargetAdapter.class);
            var target=ManagedRecoveryTestSupport.target();var evidence=c.getBean(RecoveryEvidencePort.class);
            assertThat(evidence.consumption(UUID.randomUUID(),target,UUID.randomUUID(),"generation")).isEmpty();
            assertThat(evidence.closure(UUID.randomUUID(),target,UUID.randomUUID(),"result")).isEmpty();});
    }
    @Test void C03_unknownModeRejects() {base(secrets()).withPropertyValues(P+"mode=unknown").run(c -> assertThat(c).hasFailed());}
    @Test void C04_missingSettingsReject() {
        base(secrets()).withPropertyValues(P+"mode=tooling-jdbc-v1").run(c -> assertThat(c).hasFailed());
        selected(Map.of()).run(c -> assertThat(c).hasFailed());
    }
    @Test void C05_badLoopbackAndNonEnvironmentSecretsReject() {
        var bad=new HashMap<>(secrets());bad.put("B2_SOURCE_JDBC_URL","jdbc:postgresql://localhost:5432/test");
        selected(bad).run(c -> assertThat(c).hasFailed());
        selected(Map.of()).withPropertyValues("B2_SOURCE_JDBC_URL=jdbc:postgresql://127.0.0.1:5432/test","B2_SOURCE_READER_USERNAME=b2_reader","B2_SOURCE_READER_PASSWORD="+SECRET)
            .run(c -> assertThat(c).hasFailed());
    }
    @Test void C06_foundationDisabled() {selected(secrets()).withPropertyValues("koiki.reference.notification.foundation.enabled=false")
        .run(c -> {assertThat(c).hasNotFailed();assertThat(c).doesNotHaveBean(RecoveryTargetPort.class);assertThat(c).doesNotHaveBean(ProtectedRecoveryIssueService.class);});}
    @Test void C07_managedScopeTtlRemainIndependent() throws Exception {
        Path file=ManagedRecoveryTestSupport.file(directory,ManagedRecoveryTestSupport.json(ManagedRecoveryTestSupport.grant("ISSUE")));
        String prefix=ManagedRecoveryConfiguration.PREFIX;
        selected(secrets()).withPropertyValues(prefix+"mode=file",prefix+"path="+file,prefix+"expected-sha256="+ManagedRecoveryTestSupport.hash(file),prefix+"expected-revision=test-r1",prefix+"environment-id=test-environment")
            .run(c -> {assertThat(c).hasNotFailed();assertThat(c.getBean(RecoveryScopePort.class)).isNotInstanceOf(JdbcProtectedRecoveryTargetAdapter.class);
                assertThat(c.getBean(RecoveryTtlPolicyPort.class)).isNotInstanceOf(JdbcProtectedRecoveryTargetAdapter.class);});
    }
    @Test void C08_noSecretOrExternalEntrypointExposure() {
        selected(secrets()).run(c -> {assertThat(c).hasNotFailed();assertThat(c.getBean(FrozenRecoverySourceSettings.class).toString()).doesNotContain(SECRET,"jdbc:");
            assertThat(c.getBeansWithAnnotation(org.springframework.stereotype.Controller.class)).isEmpty();
            assertThat(Arrays.stream(ProtectedRecoveryIssueService.class.getMethods()).map(java.lang.reflect.Method::getReturnType))
                .noneMatch(type -> type.isAnnotationPresent(jakarta.persistence.Entity.class));});
    }
}
