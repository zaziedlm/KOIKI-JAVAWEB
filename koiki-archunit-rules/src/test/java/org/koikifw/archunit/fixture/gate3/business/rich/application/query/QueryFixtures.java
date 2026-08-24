package org.koikifw.archunit.fixture.gate3.business.rich.application.query;

public final class QueryFixtures {

    private QueryFixtures() {
    }

    public static OwnedReadModel ownedReadModel() {
        return new OwnedReadModel("value");
    }

    public record OwnedReadModel(String value) {
    }
}
