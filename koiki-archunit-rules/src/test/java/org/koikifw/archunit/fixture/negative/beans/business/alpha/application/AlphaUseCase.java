package org.koikifw.archunit.fixture.negative.beans.business.alpha.application;

import org.koikifw.archunit.fixture.negative.beans.business.beta.application.BetaUseCase;

public final class AlphaUseCase {

    private final BetaUseCase betaUseCase;

    public AlphaUseCase(BetaUseCase betaUseCase) {
        this.betaUseCase = betaUseCase;
    }

    public void execute() {
        betaUseCase.execute();
    }
}
