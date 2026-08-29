package org.koikifw.runtimeconsumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RuntimePostgreSqlTestConfiguration.class)
class RuntimeFoundationConsumerSmokeTest {

    @Autowired
    private CreateWorkItemUseCase createWorkItem;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void startsAndInvokesCustomerOwnedBusinessModule() {
        assertNotNull(createWorkItem.create("CP4 customer-like work item"));
        Long stored = jdbcClient.sql("select count(*) from kkbiz_work_item where label = :label")
                .param("label", "CP4 customer-like work item")
                .query(Long.class)
                .single();
        org.junit.jupiter.api.Assertions.assertEquals(1L, stored);
    }
}
