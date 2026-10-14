package org.koikifw.buildsupport.phase4.s1fixture;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/** Test-only mode composition. Counters are in-memory probes, not an actual registry or Web security. */
public final class S1ModeFixture {
    private S1ModeFixture() {}
    public record Snapshot(int sends, int registryWrites, int attempts) {}
    public record DispatchRequested() {}
    public record Options(String mode, UUID permit, UUID publication, UUID operation) {}

    public static class State {
        private int sends;
        private int registryWrites;
        private int attempts = 1;
        public int runnerStarts;
        public Snapshot snapshot() { return new Snapshot(sends, registryWrites, attempts); }
    }
    public static class Sender {
        private final State state;
        public Sender(State state) { this.state = state; }
        public void send() { state.sends++; state.registryWrites++; state.attempts++; }
    }
    public static class NormalListener {
        private final Sender sender;
        public NormalListener(Sender sender) { this.sender = sender; }
        @EventListener
        public void on(DispatchRequested event) { sender.send(); }
    }
    public static class Inspection {
        private final State state;
        public Inspection(State state) { this.state = state; }
        public Snapshot read() { return state.snapshot(); }
    }
    public static class RecoveryRunner implements ApplicationRunner {
        private final Options options;
        private final State state;
        public RecoveryRunner(Options options, State state) { this.options = options; this.state = state; }
        @Override
        public void run(ApplicationArguments arguments) {
            // Startup prepares the explicit target only. Permission/consumption/send is a separate gate.
            if (options.permit() == null || options.publication() == null || options.operation() == null) {
                throw new IllegalStateException("Missing recovery target");
            }
            state.runnerStarts++;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({Normal.class, Check.class, Recovery.class})
    public static class Configuration {
        @Bean
        Options options(Environment environment) {
            String mode = environment.getProperty("s1.mode", "");
            if (!mode.equals("normal") && !mode.equals("check") && !mode.equals("recovery")) {
                throw new IllegalArgumentException("Unknown s1.mode: " + mode);
            }
            if (!mode.equals("recovery")) { return new Options(mode, null, null, null); }
            return new Options(mode, required(environment, "s1.permit-id"),
                    required(environment, "s1.publication-id"), required(environment, "s1.operation-id"));
        }
        private static UUID required(Environment environment, String key) {
            String value = environment.getProperty(key, "");
            if (value.isBlank()) { throw new IllegalArgumentException("Missing " + key); }
            try {
                UUID id = UUID.fromString(value);
                if (!id.toString().equalsIgnoreCase(value)) { throw new IllegalArgumentException("Noncanonical UUID"); }
                return id;
            } catch (IllegalArgumentException error) { throw new IllegalArgumentException("Invalid " + key, error); }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "s1.mode", havingValue = "normal")
    public static class Normal {
        @Bean Sender sender(Options options, State state) { return new Sender(state); }
        @Bean NormalListener normalListener(Sender sender) { return new NormalListener(sender); }
    }
    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "s1.mode", havingValue = "check")
    public static class Check {
        @Bean Inspection inspection(Options options, State state) { return new Inspection(state); }
    }
    @TestConfiguration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "s1.mode", havingValue = "recovery")
    public static class Recovery {
        @Bean Sender sender(Options options, State state) { return new Sender(state); }
        @Bean RecoveryRunner recoveryRunner(Options options, State state) { return new RecoveryRunner(options, state); }
    }
}
