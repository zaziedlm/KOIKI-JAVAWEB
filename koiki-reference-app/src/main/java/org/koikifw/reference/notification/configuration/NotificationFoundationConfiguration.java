package org.koikifw.reference.notification.configuration;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.Optional;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.reference.notification.adapter.outbound.identity.PublicRecoveryIdentityAdapter;
import org.koikifw.reference.notification.adapter.outbound.persistence.JpaRecoveryConsumptionAdapter;
import org.koikifw.reference.notification.adapter.outbound.persistence.JpaRecoveryPermitAdapter;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryIdentityPort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository;
import org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.koikifw.reference.notification.domain.model.RecoveryPermit;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Explicit foundation registration. Unconnected operational ports deny every required operation. */
@Configuration(proxyBeanMethods = false)
@Conditional(NotificationFoundationConfiguration.Enabled.class)
@EntityScan(basePackageClasses = RecoveryPermit.class)
@Import(ManagedRecoveryConfiguration.class)
public class NotificationFoundationConfiguration {
    @Bean
    RecoveryPermitRepository notificationPermits(EntityManager entityManager) {
        return new JpaRecoveryPermitAdapter(entityManager);
    }

    @Bean
    RecoveryConsumptionRepository notificationConsumptions(EntityManager entityManager) {
        return new JpaRecoveryConsumptionAdapter(entityManager);
    }

    @Bean
    RecoveryIdentityPort notificationIdentities(IdentityQuery query) {
        return new PublicRecoveryIdentityAdapter(query);
    }

    @Bean
    @ConditionalOnMissingBean(RecoveryScopePort.class)
    @Conditional(ManagedRecoveryConfiguration.DisabledMode.class)
    RecoveryScopePort notificationUnconnectedScope() {
        return (actor, capability, environment, publication) -> RecoveryScopePort.Decision.UNAVAILABLE;
    }

    @Bean
    @ConditionalOnMissingBean(RecoveryTtlPolicyPort.class)
    @Conditional(ManagedRecoveryConfiguration.DisabledMode.class)
    RecoveryTtlPolicyPort notificationUnconnectedTtl() { return target -> Optional.empty(); }

    @Bean
    @ConditionalOnMissingBean(RecoveryTargetPort.class)
    RecoveryTargetPort notificationUnconnectedTarget() { return (environment, publication) -> Optional.empty(); }

    @Bean
    @ConditionalOnMissingBean(RecoveryEvidencePort.class)
    RecoveryEvidencePort notificationUnconnectedEvidence() {
        return new RecoveryEvidencePort() {
            @Override public Optional<ConsumptionProof> consumption(java.util.UUID permit,
                    org.koikifw.reference.notification.application.query.RecoveryTarget target,
                    java.util.UUID operation, String worker) { return Optional.empty(); }
            @Override public Optional<ClosureProof> closure(java.util.UUID permit,
                    org.koikifw.reference.notification.application.query.RecoveryTarget target,
                    java.util.UUID actor, String result) { return Optional.empty(); }
        };
    }

    @Bean
    RecoveryPermitService notificationPermitService(RecoveryPermitRepository permits,
            RecoveryConsumptionRepository consumptions, RecoveryIdentityPort identities,
            RecoveryScopePort scope, RecoveryTtlPolicyPort ttl, RecoveryTargetPort target,
            RecoveryEvidencePort proof, BusinessAuditRecorder business, SecurityAuditRecorder security,
            ObjectProvider<Clock> clocks, PlatformTransactionManager manager) {
        return new RecoveryPermitService(permits, consumptions, identities, scope, ttl, target, proof,
                business, security, clocks.getIfAvailable(Clock::systemUTC), manager);
    }

    static final class Enabled implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(
                    "koiki.reference.notification.foundation.enabled", "false");
            if (!value.equals("true") && !value.equals("false")) {
                throw new IllegalArgumentException("Invalid notification foundation enabled property");
            }
            return value.equals("true");
        }
    }
}
