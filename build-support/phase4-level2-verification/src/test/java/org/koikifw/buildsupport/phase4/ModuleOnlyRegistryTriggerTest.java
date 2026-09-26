package org.koikifw.buildsupport.phase4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = ProbeApplication.class,
        properties = {
            "probe.direct-listener.enabled=true",
            "spring.modulith.events.registry-trigger-annotation="
                    + "org.springframework.modulith.events.ApplicationModuleListener"
        })
class ModuleOnlyRegistryTriggerTest extends RegistryTriggerSelectionSupport {

    @Test
    void directListenerRunsWithoutDurablePublicationWhenOnlyModuleListenerIsSelected()
            throws InterruptedException {
        assertPublicationCount(1);
    }
}
