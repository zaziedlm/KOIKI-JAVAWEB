package org.koikifw.archunit.fixture.negative.internal.customer;

import org.koikifw.archunit.fixture.negative.internal.framework.sample.internal.FrameworkSecret;

public final class InternalConsumer {

    public FrameworkSecret secret() {
        return new FrameworkSecret();
    }
}
