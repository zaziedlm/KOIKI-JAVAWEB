package com.example.koikidistribution;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

/** Customer-owned explicit scan, without importing any Framework internal types. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "consumer.explicit-entity-scan", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackageClasses = CustomerRecord.class)
class CustomerEntityConfiguration {}
