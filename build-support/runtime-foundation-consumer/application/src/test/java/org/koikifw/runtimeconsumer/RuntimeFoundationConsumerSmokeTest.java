package org.koikifw.runtimeconsumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RuntimeFoundationConsumerSmokeTest {

    @Autowired
    private CreateWorkItemUseCase createWorkItem;

    @Test
    void startsAndInvokesCustomerOwnedBusinessModule() {
        assertNotNull(createWorkItem.create("CP1 customer-like work item"));
    }
}
