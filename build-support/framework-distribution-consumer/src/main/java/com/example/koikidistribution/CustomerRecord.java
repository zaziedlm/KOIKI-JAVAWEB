package com.example.koikidistribution;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_record")
public class CustomerRecord {
    @Id
    private long id;
    private String description = "";

    protected CustomerRecord() {}

    public CustomerRecord(long id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
