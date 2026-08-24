package org.koikifw.archunit.fixture.gate3.business.rich.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RichModel {

    @Id
    private Long id;

    public RichModel() {
    }

    public RichModel(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
