package org.koikifw.buildsupport.phase4.s1fixture;

import static org.koikifw.buildsupport.phase4.s1fixture.S1CStore.*;

import java.sql.*;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** One bounded disposable DB; admin is restricted to test setup/observation, never runtime. */
public final class S1CDatabase implements AutoCloseable {
    public static final String PASSWORD="s1-c-test-passphrase";
    public static final UUID OPERATION=UUID.fromString("00000000-0000-0000-0000-000000000010");
    public static final UUID OTHER=UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID UNRELATED=UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final String IMAGE="postgres@sha256:18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73";
    private final PostgreSQLContainer database=new PostgreSQLContainer(IMAGE)
            .withCommand("postgres","-c","max_connections=16")
            .withCreateContainerCmdModifier(command->command.getHostConfig().withMemory(1024L*1024*1024).withNanoCPUs(1_000_000_000L));
    private final String encoded=PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(PASSWORD);
    public void start() {
        database.start();
        try(var connection=admin()) {
            for(String name:List.of("db/migration/koiki/V2026090300__create_koiki_audit.sql",
                    "db/migration/koiki/V2026090301__create_koiki_identity.sql",
                    "db/migration/koiki/V2026090701__create_koiki_session.sql",
                    "org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql")) {
                var resource=new ClassPathResource(name);
                if(!resource.getURL().toString().startsWith("jar:")) throw new IllegalStateException("C must use artifact SQL");
                System.out.println("S1-C schema="+resource.getURL());ScriptUtils.executeSqlScript(connection,resource);
            }
            ScriptUtils.executeSqlScript(connection,new ClassPathResource("s1-additional/permit-web-mode.sql"));
            if(database.getContainerInfo().getHostConfig().getMemory()!=1024L*1024*1024
                    || database.getContainerInfo().getHostConfig().getNanoCPUs()!=1_000_000_000L) throw new IllegalStateException("C DB budget");
            System.out.println("S1-C container="+database.getContainerId());
        } catch(Exception | Error failure) {database.stop();throw new IllegalStateException("C DB setup failed",failure);}
    }
    public String url(){return database.getJdbcUrl();}
    public Connection admin() throws SQLException {return DriverManager.getConnection(url(),database.getUsername(),database.getPassword());}
    public void sql(String... sqls) {
        try(var connection=admin();var statement=connection.createStatement()) {for(String sql:sqls)statement.execute(sql);}
        catch(SQLException failure){throw new IllegalStateException(failure);}
    }
    public void seed() {
        sql("TRUNCATE s1c.consumption,s1c.permit,koiki_audit_event,event_publication,koiki_session,koiki_session_attributes,koiki_login_attempt",
                "TRUNCATE koiki_user,koiki_role,koiki_permission CASCADE");
        try(var connection=admin()) {
            int index=0;
            for(String code:List.of("ISSUE","READ","EXECUTE","CLOSE")) {
                try(var statement=connection.prepareStatement("INSERT INTO koiki_permission(permission_id,permission_code) VALUES (?,?)")) {
                    statement.setObject(1,UUID.fromString("00000000-0000-0000-0000-00000000050"+(++index)));statement.setString(2,"S1_TEST_"+code);statement.executeUpdate();
                }
            }
            for(String name:List.of("issuer","closer","reader","limited","outsider","disabled")) {
                UUID id=user(name),role=UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                try(var statement=connection.prepareStatement("INSERT INTO koiki_user(user_id,email,canonical_email,status) VALUES (?,?,?,?)")) {
                    statement.setObject(1,id);statement.setString(2,name+"@fixture.invalid");statement.setString(3,name+"@fixture.invalid");statement.setString(4,name.equals("disabled")?"DISABLED":"ACTIVE");statement.executeUpdate();
                }
                try(var statement=connection.prepareStatement("INSERT INTO koiki_password_credential(user_id,encoded_password) VALUES (?,?)")) {
                    statement.setObject(1,id);statement.setString(2,encoded);statement.executeUpdate();
                }
                sql("INSERT INTO koiki_role(role_id,role_code) VALUES ('"+role+"','S1_C_"+name.toUpperCase(Locale.ROOT)+"')",
                        "INSERT INTO koiki_user_role VALUES ('"+id+"','"+role+"')");
                List<String> permissions=switch(name) {
                    case "issuer","disabled"->List.of("ISSUE","READ","EXECUTE");
                    case "closer"->List.of("CLOSE","READ");
                    case "reader"->List.of("READ");
                    case "outsider"->List.of("ISSUE","READ","EXECUTE","CLOSE");
                    default->List.of();
                };
                for(String code:permissions) sql("INSERT INTO koiki_role_permission SELECT '"+role+"',permission_id FROM koiki_permission WHERE permission_code='S1_TEST_"+code+"'");
            }
            for(UUID publication:List.of(PUBLICATION,OTHER,UNRELATED)) {
                UUID event=publication.equals(UNRELATED)?UUID.fromString("00000000-0000-0000-0000-000000000201"):EVENT;
                try(var statement=connection.prepareStatement("INSERT INTO event_publication(id,listener_id,event_type,serialized_event,publication_date,status,completion_attempts) VALUES (?,?,?,?,?,'FAILED',1)")) {
                    statement.setObject(1,publication);statement.setString(2,publication.equals(OTHER)?"s1-c-other":LISTENER);
                    statement.setString(3,"org.koikifw.buildsupport.phase4.s1fixture.S1CRecoveryFixture$Event");
                    statement.setString(4,"{\"eventId\":\""+event+"\"}");statement.setObject(5,java.time.OffsetDateTime.ofInstant(NOW.minusSeconds(10),java.time.ZoneOffset.UTC));statement.executeUpdate();
                }
            }
            try(var statement=connection.prepareStatement("INSERT INTO s1c.permit(permit_id,environment_id,publication_id,event_id,listener_id,expected_attempt,actor_id,reason_code,issued_at,expires_at) VALUES (?,'fixture-env',?,?,?,1,?,'OWNER_TEST',?,?)")) {
                statement.setObject(1,PERMIT);statement.setObject(2,PUBLICATION);statement.setObject(3,EVENT);statement.setString(4,LISTENER);
                statement.setString(5,user("issuer").toString());statement.setObject(6,java.time.OffsetDateTime.ofInstant(NOW.minusSeconds(10),java.time.ZoneOffset.UTC));
                statement.setObject(7,java.time.OffsetDateTime.ofInstant(NOW.plusSeconds(1800),java.time.ZoneOffset.UTC));statement.executeUpdate();
            }
        } catch(SQLException failure){throw new IllegalStateException(failure);}
    }
    public String observe(String sql) {
        try(var connection=DriverManager.getConnection(url(),"s1c_observer","s1-fixture-only");var statement=connection.createStatement();var rows=statement.executeQuery(sql)) {
            if(!rows.next())throw new IllegalStateException("No observer row");return rows.getString(1);
        } catch(SQLException failure){throw new IllegalStateException(failure);}
    }
    public int count(String table){return Integer.parseInt(observe("SELECT count(*) FROM "+table));}
    public String snapshot() {
        return observe("SELECT COALESCE(string_agg(row_to_json(p)::text,'|' ORDER BY permit_id),'') FROM s1c.permit p")
                +observe("SELECT COALESCE(string_agg(row_to_json(c)::text,'|' ORDER BY permit_id),'') FROM s1c.consumption c")
                +observe("SELECT string_agg(row_to_json(e)::text,'|' ORDER BY id) FROM event_publication e");
    }
    public String refused(String role,String sql) {
        try(var connection=DriverManager.getConnection(url(),role,"s1-fixture-only");var statement=connection.createStatement()) {
            statement.execute(sql);throw new IllegalStateException("Unexpected DB permission");
        } catch(SQLException failure){System.out.println("S1-C DB denial role="+role+" state="+failure.getSQLState());return failure.getSQLState();}
    }
    @Override public void close(){database.stop();}
}
