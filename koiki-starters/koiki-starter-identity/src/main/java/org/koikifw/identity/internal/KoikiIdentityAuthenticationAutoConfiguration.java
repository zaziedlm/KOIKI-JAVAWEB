package org.koikifw.identity.internal;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import org.koikifw.audit.SecurityAuditRecorder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

/** Configures opt-in local password authentication and persistent attempt protection. */
@AutoConfiguration(after = KoikiIdentityAutoConfiguration.class)
@ConditionalOnClass({DaoAuthenticationProvider.class, JdbcClient.class})
@ConditionalOnProperty(name = "koiki.identity.local-authentication.enabled", havingValue = "true")
@EnableConfigurationProperties(IdentityAuthenticationProperties.class)
public class KoikiIdentityAuthenticationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    PasswordEncoder koikiIdentityPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    IdentityUserDetailsService koikiIdentityUserDetailsService(
            EntityManager entityManager, ObjectProvider<Clock> clockProvider) {
        return new IdentityUserDetailsService(
                entityManager, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    LoginAttemptStore koikiIdentityLoginAttemptStore(
            JdbcClient jdbcClient,
            PlatformTransactionManager transactionManager,
            IdentityAuthenticationProperties properties,
            ObjectProvider<Clock> clockProvider) {
        properties.validate();
        return new LoginAttemptStore(
                jdbcClient,
                transactionManager,
                properties,
                clockProvider.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    @ConditionalOnProperty(
            name = "koiki.identity.login-attempt.source-protection",
            havingValue = "APPLICATION",
            matchIfMissing = true)
    SourceFingerprintFactory koikiIdentitySourceFingerprintFactory(
            IdentityAuthenticationProperties properties) {
        properties.validate();
        return new SourceFingerprintFactory(properties);
    }

    @Bean
    AuthenticationProvider koikiIdentityAuthenticationProvider(
            IdentityUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            LoginAttemptStore attemptStore,
            ObjectProvider<SourceFingerprintFactory> sourceFingerprintFactory,
            SecurityAuditRecorder auditRecorder,
            IdentityAuthenticationProperties properties) {
        return new KoikiLocalAuthenticationProvider(
                userDetailsService,
                passwordEncoder,
                attemptStore,
                sourceFingerprintFactory.getIfAvailable(),
                auditRecorder,
                properties);
    }
}
