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
            @RequestParam UUID permissionId) {
        requireBootstrapKey(bootstrapKey);

        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user(user_id, email, canonical_email, status)
                        VALUES (:userId, :email, :email, 'ACTIVE')
                        """)
                .param("userId", userId)
                .param("email", email)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_password_credential(user_id, encoded_password)
                        VALUES (:userId, :encodedPassword)
                        """)
                .param("userId", userId)
                .param("encodedPassword", passwordEncoder.encode(password))
                .update();
        jdbcClient.sql("INSERT INTO koiki_role(role_id, role_code) VALUES (:roleId, 'B3_READER')")
                .param("roleId", roleId)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_permission(permission_id, permission_code)
                        VALUES (:permissionId, 'ORDER:READ')
                        """)
                .param("permissionId", permissionId)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_user_role(user_id, role_id)
                        VALUES (:userId, :roleId)
                        """)
                .param("userId", userId)
                .param("roleId", roleId)
                .update();
        jdbcClient.sql(
                        """
                        INSERT INTO koiki_role_permission(role_id, permission_id)
                        VALUES (:roleId, :permissionId)
                        """)
                .param("roleId", roleId)
                .param("permissionId", permissionId)
                .update();
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
