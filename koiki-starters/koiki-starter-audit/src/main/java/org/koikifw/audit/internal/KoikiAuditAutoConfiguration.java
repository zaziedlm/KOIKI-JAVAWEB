package org.koikifw.audit.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/** Configures the DB-backed KOIKI Audit contract when JPA is available. */
@AutoConfiguration(afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@ConditionalOnClass({EntityManager.class, PlatformTransactionManager.class})
@ConditionalOnBean(EntityManagerFactory.class)
@EntityScan(basePackageClasses = AuditEventEntity.class)
public class KoikiAuditAutoConfiguration {

    @Bean
    AuditStore koikiAuditStore(EntityManager entityManager, ObjectProvider<Clock> clockProvider) {
        Clock clock = clockProvider.getIfAvailable(Clock::systemUTC);
        return new AuditStore(entityManager, clock);
    }

    @Bean
    BusinessAuditTransaction koikiBusinessAuditTransaction(AuditStore store) {
        return new BusinessAuditTransaction(store);
    }

    @Bean
    SecurityAuditTransaction koikiSecurityAuditTransaction(AuditStore store) {
        return new SecurityAuditTransaction(store);
    }

    @Bean
    BusinessAuditRecorder koikiBusinessAuditRecorder(BusinessAuditTransaction transaction) {
        return new DefaultBusinessAuditRecorder(transaction);
    }

    @Bean
    SecurityAuditRecorder koikiSecurityAuditRecorder(SecurityAuditTransaction transaction) {
        return new DefaultSecurityAuditRecorder(transaction);
    }
}
