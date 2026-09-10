package org.koikifw.identity.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.UserSessionInvalidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Configures the Framework-owned Identity read contract when JPA is available. */
@AutoConfiguration(
        afterName = {
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.koikifw.audit.internal.KoikiAuditAutoConfiguration"
        })
@ConditionalOnClass(EntityManager.class)
@ConditionalOnBean(EntityManagerFactory.class)
@EntityScan(basePackageClasses = IdentityUserEntity.class)
@EnableConfigurationProperties(IdentityAuthenticationProperties.class)
public class KoikiIdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdentityQuery.class)
    IdentityQuery koikiIdentityQuery(EntityManager entityManager) {
        return new DefaultIdentityQuery(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    PasswordEncoder koikiIdentityPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(IdentityAdministration.class)
    @ConditionalOnBean({
        BusinessAuditRecorder.class,
        SecurityAuditRecorder.class,
        UserSessionInvalidator.class,
        CompromisedPasswordChecker.class
    })
    IdentityAdministration koikiIdentityAdministration(
            EntityManager entityManager,
            PasswordEncoder passwordEncoder,
            CompromisedPasswordChecker compromisedPasswordChecker,
            BusinessAuditRecorder businessAuditRecorder,
            SecurityAuditRecorder securityAuditRecorder,
            UserSessionInvalidator sessionInvalidator,
            IdentityAuthenticationProperties properties,
            ObjectProvider<Clock> clockProvider) {
        properties.validatePasswordPolicy();
        return new DefaultIdentityAdministration(
                entityManager,
                clockProvider.getIfAvailable(Clock::systemUTC),
                passwordEncoder,
                compromisedPasswordChecker,
                businessAuditRecorder,
                securityAuditRecorder,
                sessionInvalidator,
                properties);
    }
}
