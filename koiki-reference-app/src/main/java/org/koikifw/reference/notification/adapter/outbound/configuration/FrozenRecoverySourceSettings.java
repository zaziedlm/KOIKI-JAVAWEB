package org.koikifw.reference.notification.adapter.outbound.configuration;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Startup-fixed local fixture settings. Secrets have no printable representation. */
public final class FrozenRecoverySourceSettings {
    private static final Pattern URL=Pattern.compile("jdbc:postgresql://127\\.0\\.0\\.1:([0-9]{1,5})/([a-z][a-z0-9_]{0,62})");
    private final String environment,source,jarHash,jdbcUrl,username,password;
    private final UUID run;
    private final long revision;
    public FrozenRecoverySourceSettings(String environment,String run,String source,String jarHash,long revision,
                                        Map<String,String> secretEnvironment) {
        try {
            if(!environment.matches("[a-z0-9-]{1,64}") || !source.matches("[a-z0-9-]{1,128}")
                    || !jarHash.matches("[0-9a-f]{64}") || revision<1) throw invalid();
            String url=required(secretEnvironment,"B2_SOURCE_JDBC_URL");
            var match=URL.matcher(url);
            if(!match.matches() || Integer.parseInt(match.group(1))<1 || Integer.parseInt(match.group(1))>65535) throw invalid();
            String user=required(secretEnvironment,"B2_SOURCE_READER_USERNAME");
            String secret=required(secretEnvironment,"B2_SOURCE_READER_PASSWORD");
            if(!user.equals("b2_reader") || secret.length()>128 || secret.codePoints().anyMatch(Character::isISOControl)) throw invalid();
            this.environment=environment;this.run=UUID.fromString(run);this.source=source;this.jarHash=jarHash;
            this.revision=revision;this.jdbcUrl=url+"?connectTimeout=10&socketTimeout=10";this.username=user;this.password=secret;
        } catch(RuntimeException failure) {throw invalid();}
    }
    private static String required(Map<String,String> values,String key) {
        String value=values.get(key);if(value==null || value.isBlank()) throw invalid();return value;
    }
    public String environment() {return environment;}
    public UUID run() {return run;}
    public String source() {return source;}
    public String jarHash() {return jarHash;}
    public long revision() {return revision;}
    public String jdbcUrl() {return jdbcUrl;}
    public String username() {return username;}
    public String password() {return password;}
    @Override public String toString() {return "FrozenRecoverySourceSettings[REDACTED]";}
    private static IllegalArgumentException invalid() {return new IllegalArgumentException("Frozen recovery source invalid");}
}
