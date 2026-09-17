package org.koikifw.reference.expense.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Enables the Reference expense display-only cache. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
class ExpenseCacheConfiguration {
}
