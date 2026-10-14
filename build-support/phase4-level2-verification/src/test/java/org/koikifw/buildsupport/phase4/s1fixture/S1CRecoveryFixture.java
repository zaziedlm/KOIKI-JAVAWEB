package org.koikifw.buildsupport.phase4.s1fixture;

import static org.koikifw.buildsupport.phase4.s1fixture.S1CStore.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

/** C-only explicit replay and durable operational HOLD; no registry or sender replacement. */
public final class S1CRecoveryFixture {
    private S1CRecoveryFixture() {}
    public record Event(UUID eventId) {}
    public record Operation(UUID permit,UUID publication,UUID operation,UUID event,String listener,int attempt,String generation) {}
    public record Evidence(String permit,String publication,String operation,String event,String listener,int attempt,
            String generation,String started,String outcome) {}
    public static final ThreadLocal<Operation> CURRENT=new ThreadLocal<>();
    public static void run(String url,Consumer<ConfigurableApplicationContext> action,String... overrides) {
        runner(url,overrides).run(context->{
            if(context.getStartupFailure()!=null) throw new IllegalStateException("C recovery startup failed",context.getStartupFailure());
            action.accept(context.getSourceApplicationContext());
        });
    }
    public static ApplicationContextRunner runner(String url,String... overrides) {
        return new ApplicationContextRunner().withUserConfiguration(Configuration.class).withPropertyValues(
                "s1c.mode=recovery","s1c.permit="+PERMIT,"s1c.publication="+PUBLICATION,
                "s1c.operation="+S1CDatabase.OPERATION,"spring.datasource.url="+url,
                "spring.datasource.username=s1c_recovery","spring.datasource.password=s1-fixture-only",
                "spring.datasource.hikari.maximum-pool-size=4","spring.datasource.hikari.minimum-idle=0",
                "spring.datasource.hikari.connection-timeout=10000","spring.datasource.hikari.connection-init-sql=SET statement_timeout='10s'",
                "spring.jpa.hibernate.ddl-auto=validate","spring.jpa.mapping-resources=s1-additional/orm-web-mode.xml",
                "spring.flyway.enabled=false","spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "spring.modulith.events.completion-mode=UPDATE","spring.modulith.republish-outstanding-events-on-restart=false",
                "spring.modulith.events.staleness.published=0s","spring.modulith.events.staleness.processing=0s",
                "spring.modulith.events.staleness.resubmitted=0s","spring.modulith.moments.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration,org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration")
                .withPropertyValues(overrides);
    }
    public static final class Probe {
        public final List<Operation> sends=new CopyOnWriteArrayList<>();
        public final List<String> threads=new CopyOnWriteArrayList<>();
        private final S1CStore store;private final JdbcTemplate jdbc;
        Probe(S1CStore store,JdbcTemplate jdbc){this.store=store;this.jdbc=jdbc;}
        public void handle(Event event) {
            Operation op=CURRENT.get();if(op==null || !op.event.equals(event.eventId()) || !op.listener.equals(LISTENER))throw new Denied();
            var target=store.publication(op.publication);
            if(!target.event().equals(op.event) || !target.listener().equals(op.listener) || target.attempt()!=op.attempt+1)throw new Denied();
            Integer count=jdbc.queryForObject("SELECT count(*) FROM s1c.consumption c JOIN s1c.permit p USING(permit_id) WHERE c.permit_id=? AND c.operation_id=? AND c.worker_generation=? AND p.closed_at IS NULL AND p.expires_at>?",
                    Integer.class,op.permit,op.operation,op.generation,java.time.OffsetDateTime.ofInstant(NOW,java.time.ZoneOffset.UTC));
            if(count==null || count!=1)throw new Denied();
            var permit=store.scoped(op.publication);
            store.authorize(UUID.fromString(permit.actor),"EXECUTE","fixture-env",op.publication);
            sends.add(op);threads.add(Thread.currentThread().getName());
        }
    }
    public static class Listener {
        private final Probe probe;Listener(Probe probe){this.probe=probe;}
        @ApplicationModuleListener(id="s1-c-target") public void handle(Event event){probe.handle(event);}
    }
    public static final class Gate {
        private final S1CStore store;private final IncompleteEventPublications registry;
        private final boolean resumed;
        public final Operation operation;
        Gate(S1CStore store,IncompleteEventPublications registry,Environment env) {
            this.store=store;this.registry=registry;
            resumed=env.getProperty("s1c.resume",Boolean.class,false);
            if(!"recovery".equals(env.getProperty("s1c.mode")))throw new Denied();
            UUID permit=UUID.fromString(env.getRequiredProperty("s1c.permit"));
            UUID publication=UUID.fromString(env.getRequiredProperty("s1c.publication"));
            UUID op=UUID.fromString(env.getRequiredProperty("s1c.operation"));
            if(!permit.equals(PERMIT) || !publication.equals(PUBLICATION))throw new Denied();
            var target=store.publication(publication);
            operation=new Operation(permit,publication,op,target.event(),target.listener(),target.attempt(),"s1-c-worker-1");
        }
        public void preserve(Path file) {
            var op=operation;
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file,JsonMapper.builder().build().writeValueAsString(new Evidence(op.permit.toString(),op.publication.toString(),
                        op.operation.toString(),op.event.toString(),op.listener,op.attempt,op.generation,NOW.toString(),"UNKNOWN")),StandardOpenOption.CREATE_NEW);
            } catch(Exception failure){throw new IllegalStateException("Cannot preserve C evidence",failure);}
        }
        public String resume(Path file) {
            try {
                Evidence proof=JsonMapper.builder().build().readValue(Files.readString(file),Evidence.class);
                var op=operation;
                if(!proof.permit.equals(op.permit.toString()) || !proof.publication.equals(op.publication.toString())
                        || !proof.operation.equals(op.operation.toString()) || !proof.event.equals(op.event.toString())
                        || !proof.listener.equals(op.listener) || proof.attempt!=op.attempt || !proof.generation.equals(op.generation)
                        || !proof.started.equals(NOW.toString()) || !proof.outcome.equals("UNKNOWN"))throw new Denied();
                return "HOLD";
            } catch(Exception failure){throw new Denied();}
        }
        public void execute(Path file) {
            if(resumed)throw new Denied();
            // Existing or unavailable proof always refuses; it is never inferred safe from consumption absence.
            preserve(file);
            store.transaction(()->{store.consume(operation.operation);return null;});
            store.transaction(()->{var permit=store.scoped(operation.publication);
                store.authorize(UUID.fromString(permit.actor),"EXECUTE","fixture-env",operation.publication);
                if(permit.closed!=null || !NOW.isBefore(permit.expires))throw new Denied();return null;});
            CURRENT.set(operation);
            try {registry.resubmitIncompletePublications(p->p.getIdentifier().equals(operation.publication)
                    && p.getStatus()==EventPublication.Status.FAILED && p.getCompletionAttempts()==operation.attempt
                    && p.getEvent() instanceof Event e && e.eventId().equals(operation.event));}
            finally {CURRENT.remove();}
        }
    }
    @TestConfiguration(proxyBeanMethods=false) @EnableAutoConfiguration @EnableAsync @Import(S1CStore.Configuration.class)
    public static class Configuration {
        @Bean Probe probe(S1CStore store,JdbcTemplate jdbc){return new Probe(store,jdbc);}
        @Bean Listener listener(Probe probe){return new Listener(probe);}
        @Bean Gate gate(S1CStore store,IncompleteEventPublications registry,Environment env){return new Gate(store,registry,env);}
        @Bean("taskExecutor") ThreadPoolTaskExecutor executor() {
            var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(1);executor.setMaxPoolSize(1);executor.setQueueCapacity(20);
            executor.setThreadNamePrefix("s1-c-real-worker-");executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(10);
            executor.setTaskDecorator(task->{Operation captured=CURRENT.get();return ()->{Operation previous=CURRENT.get();
                try{if(captured==null)CURRENT.remove();else CURRENT.set(captured);task.run();}
                finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}};});return executor;
        }
    }
}
