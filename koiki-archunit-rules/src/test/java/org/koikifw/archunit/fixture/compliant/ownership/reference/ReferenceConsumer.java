package org.koikifw.archunit.fixture.compliant.ownership.reference;

import org.koikifw.archunit.fixture.compliant.ownership.framework.api.FrameworkApi;

public final class ReferenceConsumer {

    public String use(FrameworkApi api) {
        return api.value();
    }
}
