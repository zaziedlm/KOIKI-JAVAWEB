package org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.inbound.event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.outbound.Outbound;
import org.koikifw.archunit.fixture.levelselection.business.alpha.application.UseCases;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class Listeners {
    @Retention(RetentionPolicy.RUNTIME)
    @EventListener
    public @interface Sync { }
    @Retention(RetentionPolicy.RUNTIME)
    @TransactionalEventListener
    public @interface Raw { }
    @Retention(RetentionPolicy.RUNTIME)
    @ApplicationModuleListener
    public @interface Wrapped { }
    @Retention(RetentionPolicy.RUNTIME)
    @Transactional(propagation = Propagation.REQUIRED)
    public @interface OverrideTransaction { }
    @Retention(RetentionPolicy.RUNTIME)
    @Async("differentExecutor")
    public @interface OverrideAsync { }

    public static class Synchronous {
        @EventListener public void handle(Object event) { }
    }
    public static class Standard {
        @ApplicationModuleListener public void handle(Object event) { }
    }
    public static class RawDirect {
        @TransactionalEventListener public void handle(Object event) { }
    }
    public static class RawMeta {
        @Raw public void handle(Object event) { }
    }
    public static class WrappedModule {
        @Wrapped public void handle(Object event) { }
    }
    public static class MethodTransaction {
        @ApplicationModuleListener @Transactional(propagation = Propagation.REQUIRED)
        public void handle(Object event) { }
    }
    public static class MethodAsync {
        @ApplicationModuleListener @Async("differentExecutor")
        public void handle(Object event) { }
    }
    public static class AnnotationPropagation {
        @ApplicationModuleListener(propagation = Propagation.REQUIRED)
        public void handle(Object event) { }
    }
    @Transactional
    public static class ClassTransaction {
        @ApplicationModuleListener public void handle(Object event) { }
    }
    @OverrideTransaction
    public static class ClassMetaTransaction {
        @ApplicationModuleListener public void handle(Object event) { }
    }
    @OverrideAsync
    public static class ClassMetaAsync {
        @ApplicationModuleListener public void handle(Object event) { }
    }
    public static class MethodMetaTransaction {
        @ApplicationModuleListener @OverrideTransaction public void handle(Object event) { }
    }
    public static class Mixed {
        @ApplicationModuleListener @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void handle(Object event) { }
    }
    public static class DirectOutbound {
        @EventListener public void handle(Object event) { new Outbound().send(); }
    }
    public static class MetaOutbound {
        @Sync public void handle(Object event) { new Outbound().send(); }
    }
    public static class RawOutbound {
        @TransactionalEventListener public void handle(Object event) { new Outbound().send(); }
    }
    public static class StandardOutbound {
        @ApplicationModuleListener public void handle(Object event) { new Outbound().send(); }
    }
    public static class ApplicationRoute {
        @EventListener public void handle(Object event) { new UseCases.Plain().handle(event); }
    }
    public static class IndirectRoute {
        @EventListener public void handle(Object event) { new UseCases.Indirect().handle(event); }
    }
}
