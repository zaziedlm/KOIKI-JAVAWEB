package org.koikifw.archunit.fixture.compliant.ownership.customer;

import org.koikifw.archunit.fixture.compliant.ownership.framework.api.FrameworkApi;

public final class CustomerConsumer {

    public String use(FrameworkApi api) {
        return api.value();
    }
}
