package dev.koiki.walkingskeleton.smoke.lib;

/**
 * Minimal library class used only to verify Reactor / Parent / BOM / NullAway wiring.
 */
public final class GreetingService {

    public String greeting(String name) {
        return "KOIKI Walking Skeleton: hello " + name;
    }
}
