package org.koikifw.archunit.fixture.gate2.ownership.consumer;

import org.koikifw.archunit.fixture.gate2.ownership.framework.sample.internal.InternalFixtures;

public final class ConsumerFixtures {

    private ConsumerFixtures() {
    }

    public static final class ConsumerApi {
    }

    public static final class DependsOnFrameworkInternal {
        public InternalFixtures.InternalType internalType() {
            return null;
        }
    }
}
