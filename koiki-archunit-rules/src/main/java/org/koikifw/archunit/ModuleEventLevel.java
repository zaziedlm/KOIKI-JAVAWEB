package org.koikifw.archunit;

/**
 * Explicit static event-rule selection for one business module.
 * Level 0 and Level 1 both permit synchronous events and reject transactional listeners.
 * Level 2 permits the standard module listener; it does not enable or certify runtime delivery.
 */
public enum ModuleEventLevel {
    LEVEL_0,
    LEVEL_1,
    LEVEL_2
}
