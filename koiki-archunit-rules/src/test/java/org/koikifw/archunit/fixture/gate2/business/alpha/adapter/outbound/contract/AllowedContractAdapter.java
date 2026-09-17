package org.koikifw.archunit.fixture.gate2.business.alpha.adapter.outbound.contract;

import org.koikifw.archunit.fixture.gate2.business.beta.contract.BetaAvailabilityQuery;

public final class AllowedContractAdapter {

    private final BetaAvailabilityQuery availability;

    public AllowedContractAdapter(BetaAvailabilityQuery availability) {
        this.availability = availability;
    }

    public boolean load() {
        return availability.isAvailable();
    }
}
