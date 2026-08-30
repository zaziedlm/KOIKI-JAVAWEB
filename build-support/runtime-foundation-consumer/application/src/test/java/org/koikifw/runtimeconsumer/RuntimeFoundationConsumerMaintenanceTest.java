package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock.LockHandle;
import org.koikifw.runtimeconsumer.workitem.application.usecase.RunWorkItemMaintenanceUseCase;
import org.koikifw.runtimeconsumer.workitem.application.usecase.WorkItemMaintenanceResult;
import org.koikifw.runtimeconsumer.workitem.application.usecase.WorkItemMaintenanceTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "koiki.consumer.mode=maintenance")
@Import(RuntimePostgreSqlTestConfiguration.class)
class RuntimeFoundationConsumerMaintenanceTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WorkItemExecutionLock executionLock;

    @Autowired
    private RunWorkItemMaintenanceUseCase useCase;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void resetMaintenanceRows() {
        jdbcClient.sql("delete from kkbiz_work_item_maintenance").update();
        jdbcClient.sql("""
                        insert into kkbiz_work_item_maintenance (task_key)
                        values (:primary), (:secondary)
                        """)
                .param("primary", WorkItemMaintenanceTask.PRIMARY.externalKey())
                .param("secondary", WorkItemMaintenanceTask.SECONDARY.externalKey())
                .update();
    }

    @Test
    void usesANonWebContextAndKeepsSessionLockUntilClosed() {
        assertThat(applicationContext).isNotInstanceOf(WebApplicationContext.class);

        LockHandle first = executionLock.tryAcquire(
                        WorkItemMaintenanceTask.PRIMARY.lockNamespace(),
                        WorkItemMaintenanceTask.PRIMARY.lockId())
                .orElseThrow();
        try {
            assertThat(executionLock.tryAcquire(
                            WorkItemMaintenanceTask.PRIMARY.lockNamespace(),
                            WorkItemMaintenanceTask.PRIMARY.lockId()))
                    .isEmpty();
            LockHandle differentTask = executionLock.tryAcquire(
                            WorkItemMaintenanceTask.SECONDARY.lockNamespace(),
                            WorkItemMaintenanceTask.SECONDARY.lockId())
                    .orElseThrow();
            differentTask.close();
        } finally {
            first.close();
        }

        LockHandle reacquired = executionLock.tryAcquire(
                        WorkItemMaintenanceTask.PRIMARY.lockNamespace(),
                        WorkItemMaintenanceTask.PRIMARY.lockId())
                .orElseThrow();
        reacquired.close();
    }

    @Test
    void recordsExactlyOneSideEffectAndEmitsExecutionCorrelation(CapturedOutput output) {
        WorkItemMaintenanceResult result = useCase.run(WorkItemMaintenanceTask.PRIMARY);

        assertThat(result.outcome()).isEqualTo(WorkItemMaintenanceResult.Outcome.SUCCEEDED);
        assertThat(executionCount(WorkItemMaintenanceTask.PRIMARY)).isEqualTo(1L);
        List<JsonNode> lifecycle = maintenanceLogs(output);
        assertThat(lifecycle).extracting(node -> node.path("result").asText())
                .containsSubsequence("started", "acquired", "succeeded");
        JsonNode succeeded = lifecycle.getLast();
        assertThat(succeeded.path("executionId").asText())
                .isEqualTo(result.executionId().toString());
        assertThat(succeeded.path("lockOwner").asText())
                .isEqualTo(result.executionId().toString());
        assertThat(succeeded.has("requestId")).isFalse();
    }

    @Test
    void skipsWhenContendedAndReleasesAfterTaskFailure() {
        LockHandle held = executionLock.tryAcquire(
                        WorkItemMaintenanceTask.PRIMARY.lockNamespace(),
                        WorkItemMaintenanceTask.PRIMARY.lockId())
                .orElseThrow();
        try {
            WorkItemMaintenanceResult contended = useCase.run(WorkItemMaintenanceTask.PRIMARY);
            assertThat(contended.outcome())
                    .isEqualTo(WorkItemMaintenanceResult.Outcome.CONTENDED);
            assertThat(executionCount(WorkItemMaintenanceTask.PRIMARY)).isZero();
        } finally {
            held.close();
        }

        jdbcClient.sql("delete from kkbiz_work_item_maintenance where task_key = :taskKey")
                .param("taskKey", WorkItemMaintenanceTask.PRIMARY.externalKey())
                .update();
        WorkItemMaintenanceResult failed = useCase.run(WorkItemMaintenanceTask.PRIMARY);
        assertThat(failed.outcome()).isEqualTo(WorkItemMaintenanceResult.Outcome.FAILED);

        jdbcClient.sql("insert into kkbiz_work_item_maintenance (task_key) values (:taskKey)")
                .param("taskKey", WorkItemMaintenanceTask.PRIMARY.externalKey())
                .update();
        assertThat(useCase.run(WorkItemMaintenanceTask.PRIMARY).outcome())
                .isEqualTo(WorkItemMaintenanceResult.Outcome.SUCCEEDED);
    }

    private long executionCount(WorkItemMaintenanceTask task) {
        return jdbcClient.sql("""
                        select execution_count
                        from kkbiz_work_item_maintenance
                        where task_key = :taskKey
                        """)
                .param("taskKey", task.externalKey())
                .query(Long.class)
                .single();
    }

    private List<JsonNode> maintenanceLogs(CapturedOutput output) {
        return output.getOut().lines()
                .filter(line -> line.startsWith("{"))
                .map(jsonMapper::readTree)
                .filter(Objects::nonNull)
                .filter(node -> "work item maintenance lifecycle"
                        .equals(node.path("message").asText()))
                .toList();
    }
}
