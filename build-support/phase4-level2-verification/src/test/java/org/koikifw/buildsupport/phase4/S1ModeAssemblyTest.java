package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.s1fixture.S1ModeFixture;
import org.koikifw.buildsupport.phase4.s1fixture.S1ModeFixture.State;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** L4: real Boot startup of a test-only mode contract; no Web server, Security or database. */
@Timeout(600)
class S1ModeAssemblyTest {
    private static final List<String> IDS = List.of(
            "--s1.permit-id=00000000-0000-0000-0000-000000000001",
            "--s1.publication-id=00000000-0000-0000-0000-000000000100",
            "--s1.operation-id=00000000-0000-0000-0000-000000000010");

    @Test
    void l4_01_normalStartupDoesNotSendButExplicitDispatchWorks() {
        var state = new State();
        var before = state.snapshot();
        try (var context = start(state, "normal", List.of())) {
            assertTrue(context.containsBean("normalListener"));
            assertTrue(context.containsBean("sender"));
            assertFalse(context.containsBean("inspection"));
            assertFalse(context.containsBean("recoveryRunner"));
            assertEquals(before, state.snapshot());
            context.publishEvent(new S1ModeFixture.DispatchRequested());
            assertEquals(new S1ModeFixture.Snapshot(1, 1, 2), state.snapshot(), "Positive control proves the sender is active");
        }
        assertEquals(new S1ModeFixture.Snapshot(1, 1, 2), state.snapshot(), "Context close must not send again");
    }

    @Test
    void l4_02_checkHasNoSendingBeansAndInspectionAndCloseDoNotWrite() {
        var state = new State();
        var before = state.snapshot();
        try (var context = start(state, "check", List.of())) {
            assertTrue(context.containsBean("inspection"));
            assertTrue(context.getBeansOfType(S1ModeFixture.Sender.class).isEmpty());
            assertTrue(context.getBeansOfType(S1ModeFixture.NormalListener.class).isEmpty());
            assertTrue(context.getBeansOfType(ApplicationRunner.class).isEmpty());
            assertEquals(before, state.snapshot());
            assertEquals(before, context.getBean(S1ModeFixture.Inspection.class).read());
            context.publishEvent(new S1ModeFixture.DispatchRequested());
            assertEquals(before, state.snapshot(), "Check mode cannot send even when dispatch event arrives");
        }
        assertEquals(before, state.snapshot());
        assertEquals(0, state.runnerStarts);
    }

    @Test
    void l4_03_recoveryHasExplicitTargetRunnerButStartupDoesNotSend() {
        var state = new State();
        var before = state.snapshot();
        try (var context = start(state, "recovery", IDS)) {
            assertTrue(context.containsBean("recoveryRunner"));
            assertTrue(context.containsBean("sender"));
            assertFalse(context.containsBean("normalListener"));
            assertFalse(context.containsBean("inspection"));
            assertEquals(1, context.getBeansOfType(ApplicationRunner.class).size());
            assertEquals(1, state.runnerStarts, "Boot really invoked ApplicationRunner");
            var options = context.getBean(S1ModeFixture.Options.class);
            assertEquals("00000000-0000-0000-0000-000000000100", options.publication().toString());
            context.publishEvent(new S1ModeFixture.DispatchRequested());
            assertEquals(before, state.snapshot());
        }
        assertEquals(before, state.snapshot());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "Recovery"})
    void l4_04_missingOrUnknownModeFailsClosed(String mode) {
        var state = new State();
        var before = state.snapshot();
        RuntimeException error = assertThrows(RuntimeException.class, () -> start(state, mode, IDS));
        assertTrue(messages(error).contains("Unknown s1.mode"));
        assertEquals(before, state.snapshot());
        assertEquals(0, state.runnerStarts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"permit", "publication", "operation"})
    void l4_05_missingRecoveryIdFailsBeforeRunner(String id) {
        var state = new State();
        var before = state.snapshot();
        List<String> arguments = IDS.stream().filter(value -> !value.startsWith("--s1." + id + "-id=")).toList();
        RuntimeException error = assertThrows(RuntimeException.class, () -> start(state, "recovery", arguments));
        assertTrue(messages(error).contains("Missing s1." + id + "-id"));
        assertEquals(before, state.snapshot());
        assertEquals(0, state.runnerStarts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"permit", "publication", "operation"})
    void l4_06_invalidRecoveryIdFailsBeforeRunner(String id) {
        var state = new State();
        var before = state.snapshot();
        List<String> arguments = IDS.stream().map(value -> value.startsWith("--s1." + id + "-id=")
                ? "--s1." + id + "-id=not-a-uuid" : value).toList();
        RuntimeException error = assertThrows(RuntimeException.class, () -> start(state, "recovery", arguments));
        assertTrue(messages(error).contains("Invalid s1." + id + "-id"));
        assertEquals(before, state.snapshot());
        assertEquals(0, state.runnerStarts);
    }

    private static ConfigurableApplicationContext start(State state, String mode, List<String> ids) {
        var arguments = new ArrayList<>(ids);
        arguments.add("--s1.mode=" + mode);
        arguments.add("--spring.config.location=optional:classpath:s1-minimum/unused.properties");
        arguments.add("--spring.main.banner-mode=off");
        var context = new SpringApplicationBuilder(S1ModeFixture.Configuration.class)
                .web(WebApplicationType.NONE)
                .initializers(application -> application.getBeanFactory().registerSingleton("state", state))
                .run(arguments.toArray(String[]::new));
        System.out.println("S1-L4 mode=" + mode + " beans=" + Arrays.toString(context.getBeanDefinitionNames())
                + " snapshot=" + state.snapshot() + " runnerStarts=" + state.runnerStarts);
        return context;
    }

    private static String messages(Throwable error) {
        var messages = new StringBuilder();
        for (Throwable cause = error; cause != null; cause = cause.getCause()) { messages.append(cause.getMessage()).append('\n'); }
        return messages.toString();
    }
}
