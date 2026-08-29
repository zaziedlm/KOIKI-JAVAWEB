package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItem;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemRepository;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({RuntimePostgreSqlTestConfiguration.class,
        RuntimeFoundationConsumerTransactionTest.RollbackProbeConfiguration.class})
class RuntimeFoundationConsumerTransactionTest {

    @Autowired
    private CreateWorkItemUseCase createWorkItem;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void rollsBackDatabaseWriteWhenProcessingFailsAfterRepositorySave() {
        assertThatThrownBy(() -> createWorkItem.create("rollback-probe"))
                .isInstanceOf(RollbackProbeException.class);

        Long stored = jdbcClient.sql("select count(*) from kkbiz_work_item where label = :label")
                .param("label", "rollback-probe")
                .query(Long.class)
                .single();
        assertThat(stored).isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RollbackProbeConfiguration {

        @Bean
        @Primary
        WorkItemRepository rollbackProbeRepository(
                @Qualifier("workItemRepository") WorkItemRepository delegate) {
            return new WorkItemRepository() {
                @Override
                public Optional<WorkItem> findById(UUID id) {
                    return delegate.findById(id);
                }

                @Override
                public WorkItem save(WorkItem workItem) {
                    delegate.save(workItem);
                    throw new RollbackProbeException();
                }
            };
        }
    }

    private static final class RollbackProbeException extends RuntimeException {
    }
}
