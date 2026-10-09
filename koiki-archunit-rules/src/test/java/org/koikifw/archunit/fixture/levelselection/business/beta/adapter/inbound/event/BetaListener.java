package org.koikifw.archunit.fixture.levelselection.business.beta.adapter.inbound.event;

import org.springframework.modulith.events.ApplicationModuleListener;

public class BetaListener {
    @ApplicationModuleListener
    public void handle(Object event) { }
}
