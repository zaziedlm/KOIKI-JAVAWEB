package com.example.koikidistribution;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerRecord, Long> {}
