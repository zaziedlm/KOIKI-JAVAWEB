package org.koikifw.identity.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.koikifw.identity.IdentityQuery;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;

/** Configures the Framework-owned Identity read contract when JPA is available. */
@AutoConfiguration(afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@ConditionalOnClass(EntityManager.class)
@ConditionalOnBean(EntityManagerFactory.class)
@EntityScan(basePackageClasses = IdentityUserEntity.class)
public class KoikiIdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdentityQuery.class)
    IdentityQuery koikiIdentityQuery(EntityManager entityManager) {
        return new DefaultIdentityQuery(entityManager);
    }
}
