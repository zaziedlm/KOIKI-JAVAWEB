package org.koikifw.buildsupport.phase4.rulefixture.business.alpha.adapter.inbound.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.adapter.outbound.external.ExternalSender;
import org.koikifw.buildsupport.phase4.rulefixture.business.alpha.application.SendUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.event.TransactionalEventListener;

public final class ListenerCases {

    private ListenerCases() {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @TransactionalEventListener
    public @interface MetaTransactional {
    }

    public static final class Synchronous {
        @EventListener
        public void onEvent(Object event) {
        }
    }

    public static final class Transactional {
        @TransactionalEventListener
        public void onEvent(Object event) {
        }
    }

    public static final class MetaTransactionalCase {
        @MetaTransactional
        public void onEvent(Object event) {
        }
    }

    public static final class ModuleListener {
        @ApplicationModuleListener
        public void onEvent(Object event) {
        }
    }

    public static final class DirectExternalCall {
        private final ExternalSender sender;

        public DirectExternalCall(ExternalSender sender) {
            this.sender = sender;
        }

        @EventListener
        public void onEvent(Object event) {
            sender.send();
        }
    }

    public static final class IndirectExternalCall {
        private final SendUseCase useCase;

        public IndirectExternalCall(SendUseCase useCase) {
            this.useCase = useCase;
        }

        @EventListener
        public void onEvent(Object event) {
            useCase.send();
        }
    }
}
