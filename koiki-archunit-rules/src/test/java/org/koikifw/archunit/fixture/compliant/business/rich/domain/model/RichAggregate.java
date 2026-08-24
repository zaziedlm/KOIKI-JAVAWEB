package org.koikifw.archunit.fixture.compliant.business.rich.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RichAggregate {

    @Id
    private Long id;

    private String name;

    protected RichAggregate() {
        id = 0L;
        name = "";
    }

    public RichAggregate(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String newName) {
        name = newName;
    }
}
