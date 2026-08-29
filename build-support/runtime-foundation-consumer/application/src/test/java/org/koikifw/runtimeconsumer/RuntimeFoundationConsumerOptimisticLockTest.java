package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.koikifw.runtimeconsumer.workreview.domain.model.WorkReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(RuntimePostgreSqlTestConfiguration.class)
class RuntimeFoundationConsumerOptimisticLockTest {

    @Autowired
    private CreateWorkItemUseCase createWorkItem;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void usesIdentifierIdentityWithAnUninitializedHibernateProxy() {
        UUID workItemId = createWorkItem.create("proxy-identity-probe");
        WorkReview expected = new WorkReview(workItemId, "same identity");
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            WorkReview reference = entityManager.getReference(WorkReview.class, workItemId);

            assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(reference)).isFalse();
            assertThat(expected).isEqualTo(reference);
            assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(reference)).isFalse();
            assertThat(reference).isEqualTo(expected);
            assertThat(reference.hashCode()).isEqualTo(expected.hashCode());
        } finally {
            entityManager.close();
        }
    }

    @Test
    void preventsAStaleReviewTransitionFromOverwritingTheCommittedDecision() {
        UUID workItemId = createWorkItem.create("optimistic-lock-probe");
        EntityManager first = entityManagerFactory.createEntityManager();
        EntityManager stale = entityManagerFactory.createEntityManager();

        try {
            first.getTransaction().begin();
            stale.getTransaction().begin();
            WorkReview firstReview = first.find(WorkReview.class, workItemId);
            WorkReview staleReview = stale.find(WorkReview.class, workItemId);

            firstReview.approve();
            first.getTransaction().commit();

            staleReview.reject();
            assertThatThrownBy(() -> stale.getTransaction().commit())
                    .isInstanceOf(RollbackException.class)
                    .hasCauseInstanceOf(OptimisticLockException.class);
        } finally {
            rollbackIfActive(first);
            rollbackIfActive(stale);
            first.close();
            stale.close();
        }

        String status = jdbcClient.sql("""
                        select status
                        from kkbiz_work_review
                        where work_item_id = :workItemId
                        """)
                .param("workItemId", workItemId)
                .query(String.class)
                .single();
        Long version = jdbcClient.sql("""
                        select version
                        from kkbiz_work_review
                        where work_item_id = :workItemId
                        """)
                .param("workItemId", workItemId)
                .query(Long.class)
                .single();

        assertThat(status).isEqualTo("APPROVED");
        assertThat(version).isOne();
    }

    private static void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }
}
