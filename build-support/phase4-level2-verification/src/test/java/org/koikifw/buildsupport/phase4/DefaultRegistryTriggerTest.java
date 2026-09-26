package org.koikifw.buildsupport.phase4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ProbeApplication.class, properties = "probe.direct-listener.enabled=true")
class DefaultRegistryTriggerTest extends RegistryTriggerSelectionSupport {

    @Test
    void bothTransactionalListenersHavePublicationsByDefault() throws InterruptedException {
        assertPublicationCount(2);
    }
}
