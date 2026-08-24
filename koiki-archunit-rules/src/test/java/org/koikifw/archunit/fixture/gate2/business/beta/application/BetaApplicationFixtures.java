package org.koikifw.archunit.fixture.gate2.business.beta.application;

import org.koikifw.archunit.fixture.gate2.business.alpha.application.ApplicationFixtures;

public final class BetaApplicationFixtures {

    private BetaApplicationFixtures() {
    }

    public static final class BetaUseCase {
    }

    public static final class DependsOnAlphaApplication {
        public ApplicationFixtures.AlphaUseCase alphaUseCase() {
            return null;
        }
    }
}
