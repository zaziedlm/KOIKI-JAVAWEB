package org.koikifw.archunit.fixture.gate2.ownership.framework.api;

import org.koikifw.archunit.fixture.gate2.ownership.consumer.ConsumerFixtures;

public final class FrameworkFixtures {

    private FrameworkFixtures() {
    }

    public static final class DependsOnConsumer {
        public ConsumerFixtures.ConsumerApi consumerApi() {
            return null;
        }
    }
}
