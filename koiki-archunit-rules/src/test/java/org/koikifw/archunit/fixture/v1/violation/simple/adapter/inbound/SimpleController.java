package org.koikifw.archunit.fixture.v1.violation.simple.adapter.inbound;

import org.koikifw.archunit.fixture.v1.violation.simple.adapter.outbound.SimpleOutbound;

public class SimpleController {
    private final SimpleOutbound outbound = new SimpleOutbound();
}
