package org.koikifw.architecture;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class KoikiModuleContractTest {

    @Test
    void declaresPackageRuntimeContractWithoutInheritance() {
        Target target = KoikiModule.class.getAnnotation(Target.class);
        Retention retention = KoikiModule.class.getAnnotation(Retention.class);

        assertArrayEquals(new java.lang.annotation.ElementType[] {PACKAGE}, target.value());
        assertEquals(RUNTIME, retention.value());
        assertTrue(KoikiModule.class.isAnnotationPresent(Documented.class));
        assertFalse(KoikiModule.class.isAnnotationPresent(Inherited.class));
    }

    @Test
    void requiresAllApprovedAttributesWithoutDefaults() {
        Map<String, Method> attributes = Arrays.stream(KoikiModule.class.getDeclaredMethods())
                .collect(Collectors.toUnmodifiableMap(Method::getName, Function.identity()));

        assertEquals(Map.of(
                "name", String.class,
                "tier", ModuleTier.class,
                "persistence", PersistenceTechnology.class,
                "persistenceModel", PersistenceModel.class),
                attributes.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getReturnType())));
        attributes.values().forEach(attribute -> assertNull(attribute.getDefaultValue()));
    }

    @Test
    void exposesOnlyApprovedEnumConstants() {
        assertArrayEquals(new ModuleTier[] {ModuleTier.SIMPLE, ModuleTier.RICH}, ModuleTier.values());
        assertArrayEquals(
                new PersistenceTechnology[] {PersistenceTechnology.JPA, PersistenceTechnology.MYBATIS},
                PersistenceTechnology.values());
        assertArrayEquals(new PersistenceModel[] {PersistenceModel.SHARED}, PersistenceModel.values());
    }
}
