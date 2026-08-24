package org.koikifw.archunit.fixture.gate2.business.alpha.application;

import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;

public final class MisplacedListener {

    @EventListener
    public void directListener(Object event) {
    }

    @ApplicationModuleListener
    public void applicationModuleListener(Object event) {
    }
}
