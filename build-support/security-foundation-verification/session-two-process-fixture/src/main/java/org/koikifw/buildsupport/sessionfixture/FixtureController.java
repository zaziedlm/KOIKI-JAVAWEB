package org.koikifw.buildsupport.sessionfixture;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.koikifw.identity.FrameworkPrincipal;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class FixtureController {

    private static final String BOOTSTRAP_KEY_PROPERTY = "koiki.fixture.bootstrap-key";

    private final JdbcClient jdbcClient;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    FixtureController(
            JdbcClient jdbcClient, PasswordEncoder passwordEncoder, Environment environment) {
        this.jdbcClient = jdbcClient;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @GetMapping("/fixture/readiness")
    String readiness() {
        return "B3-4-ready";
    }

    @PostMapping("/fixture/setup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void setup(
            @RequestHeader("X-Koiki-Fixture-Key") String bootstrapKey,
            @RequestParam UUID userId,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam UUID roleId,
            @RequestParam String roleCode,
            @RequestParam UUID permissionId,
            @RequestParam String permissionCode) {
        requireBootstrapKey(bootstrapKey);

        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user(user_id, email, canonical_email, status)
                        VALUES (:userId, :email, :email, 'ACTIVE')
                        ON CONFLICT (user_id) DO NOTHING
                        """)
                .param("userId", userId)
                .param("email", email)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_password_credential(user_id, encoded_password)
                        VALUES (:userId, :encodedPassword)
                        ON CONFLICT (user_id) DO NOTHING
                        """)
                .param("userId", userId)
                .param("encodedPassword", passwordEncoder.encode(password))
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_role(role_id, role_code)
                        VALUES (:roleId, :roleCode)
                        ON CONFLICT (role_id) DO NOTHING
                        """)
                .param("roleId", roleId)
                .param("roleCode", roleCode)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_permission(permission_id, permission_code)
                        VALUES (:permissionId, :permissionCode)
                        ON CONFLICT (permission_id) DO NOTHING
                        """)
                .param("permissionId", permissionId)
                .param("permissionCode", permissionCode)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user_role(user_id, role_id)
                        VALUES (:userId, :roleId)
                        ON CONFLICT DO NOTHING
                        """)
                .param("userId", userId)
                .param("roleId", roleId)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_role_permission(role_id, permission_id)
                        VALUES (:roleId, :permissionId)
                        ON CONFLICT DO NOTHING
                        """)
                .param("roleId", roleId)
                .param("permissionId", permissionId)
                .update();
    }

    @PostMapping("/fixture/setup-external")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void setupExternal(
            @RequestHeader("X-Koiki-Fixture-Key") String bootstrapKey,
            @RequestParam UUID linkId,
            @RequestParam UUID userId,
            @RequestParam String issuer,
            @RequestParam String subject) {
        requireBootstrapKey(bootstrapKey);
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_external_identity_link(link_id, user_id, issuer, subject)
                        VALUES (:linkId, :userId, :issuer, :subject)
                        """)
                .param("linkId", linkId)
                .param("userId", userId)
                .param("issuer", issuer)
                .param("subject", subject)
                .update();
    }

    @PostMapping("/fixture/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void reset(@RequestHeader("X-Koiki-Fixture-Key") String bootstrapKey) {
        requireBootstrapKey(bootstrapKey);
        jdbcClient.sql("DELETE FROM koiki_session_attributes").update();
        jdbcClient.sql("DELETE FROM koiki_session").update();
        jdbcClient.sql("DELETE FROM koiki_login_attempt").update();
        jdbcClient.sql("DELETE FROM koiki_external_identity_link").update();
        jdbcClient.sql("DELETE FROM koiki_role_permission").update();
        jdbcClient.sql("DELETE FROM koiki_user_role").update();
        jdbcClient.sql("DELETE FROM koiki_password_credential").update();
        jdbcClient.sql("DELETE FROM koiki_user").update();
        jdbcClient.sql("DELETE FROM koiki_permission").update();
        jdbcClient.sql("DELETE FROM koiki_role").update();
        jdbcClient.sql("DELETE FROM koiki_audit_event").update();
    }

    @GetMapping("/fixture/session")
    String session(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return principal.userId() + "|" + String.join(",", principal.permissions().stream()
                .sorted()
                .toList());
    }

    private void requireBootstrapKey(String presentedKey) {
        String requiredKey = environment.getRequiredProperty(BOOTSTRAP_KEY_PROPERTY);
        if (!MessageDigest.isEqual(
                presentedKey.getBytes(StandardCharsets.UTF_8),
                requiredKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
