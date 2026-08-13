package org.koikifw.archunit.fixture.v1.violation.rich.application;

import org.springframework.context.event.EventListener;

public class WrongListener {
    @EventListener
    public void on(String event) {
    }
}
