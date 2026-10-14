package org.koikifw.buildsupport.phase4.s1fixture;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.hibernate.annotations.DynamicUpdate;
import org.koikifw.audit.*;
import org.koikifw.identity.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** C-only Application gate/model; no formal Reference implementation or authentication shortcut. */
public final class S1CStore {
    public static final UUID PERMIT=UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID PUBLICATION=UUID.fromString("00000000-0000-0000-0000-000000000100");
    public static final UUID EVENT=UUID.fromString("00000000-0000-0000-0000-000000000200");
    public static final String LISTENER="s1-c-target";
    public static final Instant NOW=Instant.parse("2026-10-06T00:00:10Z");
    public static UUID user(String name) {
        int index=List.of("issuer","closer","reader","limited","outsider","disabled").indexOf(name);
        if(index<0) throw new IllegalArgumentException(name);
        return UUID.fromString("00000000-0000-0000-0000-00000000030"+(index+1));
    }
    private final EntityManagerFactory factory;
    private final TransactionTemplate tx;
    private final IdentityQuery identity;
    private final BusinessAuditRecorder business;
    private final JdbcTemplate jdbc;
    public S1CStore(EntityManagerFactory factory,PlatformTransactionManager manager,IdentityQuery identity,
            BusinessAuditRecorder business,JdbcTemplate jdbc) {
        this.factory=factory;tx=new TransactionTemplate(manager);tx.setTimeout(10);
        this.identity=identity;this.business=business;this.jdbc=jdbc;
    }
    @TestConfiguration(proxyBeanMethods=false)
    public static class Configuration {
        @Bean Clock clock() {return Clock.fixed(NOW,ZoneOffset.UTC);}
        @Bean S1CStore store(EntityManagerFactory factory,PlatformTransactionManager manager,IdentityQuery identity,
                BusinessAuditRecorder business,JdbcTemplate jdbc) {return new S1CStore(factory,manager,identity,business,jdbc);}
    }
    public <T> T transaction(Supplier<T> work) {
        return tx.execute(status->{em().createNativeQuery("SET LOCAL lock_timeout='10s'").executeUpdate();return work.get();});
    }
    public void rollback(Runnable work) {tx.executeWithoutResult(status->{work.run();status.setRollbackOnly();});}
    public EntityManager em() {return Objects.requireNonNull(EntityManagerFactoryUtils.getTransactionalEntityManager(factory));}
    public void authorize(UUID actor,String ability,String environment,UUID publication) {
        var found=identity.findById(FrameworkUserId.parse(actor.toString()));
        if(found.isEmpty() || found.get().status()!=UserStatus.ACTIVE || !found.get().permissionCodes().contains("S1_TEST_"+ability)
                || actor.equals(user("outsider")) || !environment.equals("fixture-env") || !publication.equals(PUBLICATION)) throw new Denied();
    }
    public AuditEvent audit(UUID id,UUID actor,String action) {
        return AuditEvent.of("S1_TEST_CHANGE",AuditActor.user(actor.toString()),"S1_TEST_"+action,AuditResult.SUCCESS)
                .withResource("S1_TEST_PERMIT",id.toString());
    }
    public void issue(UUID actor,String environment,UUID publication) {
        issue(PERMIT,actor,environment,publication);
    }
    public void issue(UUID id,UUID actor,String environment,UUID publication) {
        transaction(()->{authorize(actor,"ISSUE",environment,publication);var target=publication(publication);
            var permit=new Permit(id,actor,target);em().persist(permit);em().flush();
            business.record(audit(id,actor,"ISSUED"));return null;});
    }
    public record View(UUID id,boolean closed) {}
    public View read(UUID actor,String environment,UUID publication) {
        return transaction(()->{authorize(actor,"READ",environment,publication);var permit=scoped(publication);return new View(permit.id,permit.closed!=null);});
    }
    public void close(UUID actor,String environment,UUID publication,String proof) {
        transaction(()->{authorize(actor,"CLOSE",environment,publication);
            if(!proof.equals("TEST_REVIEWED")) throw new Denied();
            var permit=scoped(publication);em().lock(permit,LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            permit.closed=NOW;permit.confirmed=actor.toString();permit.result=proof;em().flush();
            business.record(audit(PERMIT,actor,"CLOSED"));return null;});
    }
    public Permit scoped(UUID publication) {
        var rows=em().createQuery("select p from S1CPermit p where p.environment=:env and p.publication=:pub",Permit.class)
                .setParameter("env","fixture-env").setParameter("pub",publication).getResultList();
        if(rows.size()!=1) throw new Denied();return rows.getFirst();
    }
    public record Target(UUID id,UUID event,String listener,int attempt,String status) {}
    public Target publication(UUID id) {
        if(!id.equals(PUBLICATION)) throw new Denied();
        return jdbc.queryForObject("SELECT id,serialized_event::jsonb->>'eventId',listener_id,completion_attempts,status FROM event_publication WHERE id=?",
                (row,index)->new Target(UUID.fromString(row.getString(1)),UUID.fromString(row.getString(2)),row.getString(3),row.getInt(4),row.getString(5)),id);
    }
    public void consume(UUID operation) {
        var permit=scoped(PUBLICATION);UUID actor=UUID.fromString(permit.actor);
        authorize(actor,"ISSUE","fixture-env",PUBLICATION);authorize(actor,"EXECUTE","fixture-env",PUBLICATION);
        var target=publication(PUBLICATION);
        em().lock(permit,LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        if(permit.closed!=null || !NOW.isBefore(permit.expires) || em().find(Consumption.class,permit.id)!=null
                || !permit.event.equals(target.event) || !permit.listener.equals(target.listener)
                || permit.attempt!=target.attempt || !target.status.equals("FAILED")) throw new Denied();
        em().persist(new Consumption(permit.id,operation));em().flush();business.record(audit(permit.id,actor,"CONSUMED"));
    }
    public static final class Denied extends RuntimeException {public Denied(){super("S1_TEST_DENIED");}}
    // Opt-in XML registration keeps C entities out of existing context scans.
    @Table(name="permit",schema="s1c") @DynamicUpdate
    public static class Permit {
        @Id @Column(name="permit_id",updatable=false) UUID id;
        @Column(name="environment_id",updatable=false) String environment="fixture-env";
        @Column(name="publication_id",updatable=false) UUID publication;
        @Column(name="event_id",updatable=false) UUID event;
        @Column(name="listener_id",updatable=false) String listener;
        @Column(name="expected_attempt",updatable=false) int attempt;
        @Column(name="actor_id",updatable=false) String actor;
        @Column(name="reason_code",updatable=false) String reason="OWNER_TEST";
        @Column(name="issued_at",updatable=false) Instant issued=NOW.minusSeconds(10);
        @Column(name="expires_at",updatable=false) Instant expires=NOW.plusSeconds(1800);
        @Column(name="closed_at",insertable=false) Instant closed;
        @Column(name="confirmed_by",insertable=false) String confirmed;
        @Column(name="result_ref",insertable=false) String result;
        @Version @Column(name="version",insertable=false) long version;
        protected Permit() {}
        public Permit(UUID id,UUID actor,Target target) {
            this.id=id;this.actor=actor.toString();publication=target.id;event=target.event;listener=target.listener;attempt=target.attempt;
        }
    }
    @Table(name="consumption",schema="s1c")
    public static class Consumption {
        @Id @Column(name="permit_id") UUID permit;
        @Column(name="operation_id") UUID operation;
        @Column(name="worker_generation") String generation="s1-c-worker-1";
        @Column(name="consumed_at") Instant consumed=NOW;
        protected Consumption() {}
        public Consumption(UUID permit,UUID operation){this.permit=permit;this.operation=operation;}
    }
}
