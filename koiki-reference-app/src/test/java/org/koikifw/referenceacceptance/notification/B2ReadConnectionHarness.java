package org.koikifw.referenceacceptance.notification;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.koikifw.reference.notification.application.*;
import org.koikifw.reference.notification.adapter.outbound.operational.JdbcProtectedRecoveryTargetAdapter;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.env.*;

/** Real identity, managed scope/TTL, persistence and Audit with a separate frozen source DB. */
public final class B2ReadConnectionHarness implements AutoCloseable {
    public final B2FrozenSourceProcess source;
    public final NotificationFoundationDbHarness db;
    public final ConfigurableApplicationContext context;
    private final Path assignment;
    private static final ThreadLocal<Map<String,Object>> ENV=new ThreadLocal<>();
    public B2ReadConnectionHarness() throws Exception {this(null);}
    /** Negative protocol preparation happens before Reference admission, never during a positive operation. */
    public B2ReadConnectionHarness(String negativeProvider) throws Exception {
        source=new B2FrozenSourceProcess();
        NotificationFoundationDbHarness prepared=null;
        ConfigurableApplicationContext opened=null;
        Path file=null;
        try {
            if(negativeProvider!=null) {
                if(!Set.of("UNKNOWN","FIXTURE_ACCEPTED").contains(negativeProvider)) throw new IllegalArgumentException("Negative fixture invalid");
                var attached=source.attachedDatabase();
                try(var connection=java.sql.DriverManager.getConnection(attached.getJdbcUrl(),attached.getUsername(),attached.getPassword());
                    var statement=connection.createStatement()) {
                    statement.setQueryTimeout(10);
                    List<String> columns=new ArrayList<>();
                    try(var result=statement.executeQuery("SELECT * FROM b2_read.frozen_target LIMIT 0")) {
                        for(int i=1;i<=result.getMetaData().getColumnCount();i++) {
                            String name=result.getMetaData().getColumnName(i);
                            if(!name.matches("[a-z_][a-z0-9_]*")) throw new IllegalStateException("Protocol column invalid");
                            columns.add(name.equals("provider") ? "'"+negativeProvider+"'::text AS provider" : name);
                        }
                    }
                    statement.execute("ALTER VIEW b2_read.frozen_target RENAME TO frozen_target_negative_base");
                    statement.execute("CREATE VIEW b2_read.frozen_target AS SELECT "+String.join(",",columns)+" FROM b2_read.frozen_target_negative_base");
                    statement.execute("GRANT SELECT ON b2_read.frozen_target TO b2_reader");
                }
            }
            prepared=new NotificationFoundationDbHarness(source.attachedDatabase());prepared.reset();
            prepared.clock.now=Instant.now();
            RecoveryTarget target=source.target();Instant now=prepared.clock.now;
            String grants=String.join(",",NotificationFoundationDbHarness.CAPABILITIES.stream().map(action ->
                "{\"userId\":\""+NotificationFoundationDbHarness.USER+"\",\"capability\":\"NOTIFICATION:PERMIT:"+action+"\",\"publicationId\":\""+target.publicationId()+"\"}").toList());
            String json="{\"schemaVersion\":1,\"revision\":\"test-r1\",\"environmentId\":\""+target.environmentId()+"\",\"validFrom\":\""+now.minusSeconds(1)+"\",\"validUntil\":\""+now.plusSeconds(600)+"\",\"permitTtl\":\"PT1M\",\"grants\":["+grants+"]}";
            file=Files.createTempFile("b2-managed-",".json");Files.writeString(file,json);
            Map<String,Object> props=new HashMap<>(source.settings());
            String prefix=org.koikifw.reference.notification.configuration.ManagedRecoveryConfiguration.PREFIX;
            props.putAll(Map.of(prefix+"mode","file",prefix+"path",file.toString(),prefix+"expected-sha256",ManagedRecoveryTestSupport.hash(file),prefix+"expected-revision","test-r1",prefix+"environment-id",target.environmentId()));
            ENV.set(source.environment());
            opened=prepared.open(NotificationFoundationDbHarness.Mode.PERMIT,false,props,false,FixtureEnvironment.class);
            db=prepared;context=opened;assignment=file;
            if(context.getBeansOfType(org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort.class).size()!=1)
                throw new IllegalStateException("Target registration is not unique");
        } catch(Exception|AssertionError failure) {
            if(opened!=null) opened.close();if(prepared!=null) prepared.close();if(file!=null) Files.deleteIfExists(file);source.close();throw failure;
        } finally {ENV.remove();}
    }
    public RecoveryTarget target() {return source.target();}
    public ProtectedRecoveryIssueService protectedService() {return context.getBean(ProtectedRecoveryIssueService.class);}
    public RecoveryPermitService service() {return context.getBean(RecoveryPermitService.class);}
    public JdbcProtectedRecoveryTargetAdapter adapter() {return context.getBean(JdbcProtectedRecoveryTargetAdapter.class);}
    public UUID issue() {return protectedService().issue(target(),"B2_TEST_REASON");}
    /** Finite denial/transaction observers, with the real remaining dependencies. */
    public RecoveryPermitService serviceWith(
            org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort scope,
            org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort policy,
            org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort targetPort) {
        return new RecoveryPermitService(
            context.getBean(org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository.class),
            context.getBean(org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository.class),
            context.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryIdentityPort.class),
            scope,policy,targetPort,context.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort.class),
            context.getBean(org.koikifw.audit.BusinessAuditRecorder.class),context.getBean(org.koikifw.audit.SecurityAuditRecorder.class),
            db.clock,context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
    }
    public void advance(long seconds) {db.clock.now=db.clock.now.plusSeconds(seconds);}
    @Override public void close() throws Exception {
        try {context.close();db.close();Files.deleteIfExists(assignment);} finally {source.close();}
    }
    @Configuration(proxyBeanMethods=false)
    public static class FixtureEnvironment {
        @Bean static BeanFactoryPostProcessor fixtureSecrets() {
            Map<String,Object> secrets=Map.copyOf(Objects.requireNonNull(ENV.get()));
            return factory -> {
                var environment=factory.getBean(ConfigurableEnvironment.class);
                environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,secrets));
            };
        }
    }
}
