package org.koikifw.archunit.fixture.gate2.business.alpha.adapter.inbound.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.koikifw.archunit.fixture.gate2.business.alpha.domain.repository.RepositoryFixtures;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.event.TransactionalEventListener;

public final class ListenerFixtures {

    private ListenerFixtures() {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @TransactionalEventListener
    public @interface MetaTransactionalListener {
    }

    public static final class TransactionalListeners {

        @TransactionalEventListener
        public void directlyTransactional(Object event) {
        }

        @MetaTransactionalListener
        public void metaTransactional(Object event) {
        }

        @ApplicationModuleListener
        public void applicationModuleListener(Object event) {
        }
    }

    public static final class CompliantListener {

        @EventListener
        public void onEvent(Object event) {
        }
    }

    public static final class DomainDependingListener {
        public RepositoryFixtures.AlphaRepository repository() {
            return null;
        }
    }
}
