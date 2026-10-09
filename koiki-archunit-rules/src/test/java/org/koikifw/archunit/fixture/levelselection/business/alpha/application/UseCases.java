package org.koikifw.archunit.fixture.levelselection.business.alpha.application;

import org.koikifw.archunit.fixture.levelselection.business.alpha.adapter.outbound.Outbound;
import org.springframework.modulith.events.ApplicationModuleListener;

public class UseCases {
    public static class Plain {
        public void handle(Object event) { }
    }
    public static class Indirect {
        public void handle(Object event) { new Outbound().send(); }
    }
    public static class Misplaced {
        @ApplicationModuleListener
        public void handle(Object event) { }
    }
}
