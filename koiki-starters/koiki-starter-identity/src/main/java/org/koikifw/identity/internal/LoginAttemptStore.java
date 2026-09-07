package org.koikifw.identity.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

final class LoginAttemptStore {

    private final JdbcClient jdbcClient;
    private final IdentityAuthenticationProperties properties;
    private final Clock clock;
    private final TransactionTemplate transaction;

    LoginAttemptStore(
            JdbcClient jdbcClient,
            PlatformTransactionManager transactionManager,
            IdentityAuthenticationProperties properties,
            Clock clock) {
        this.jdbcClient = jdbcClient;
        this.properties = properties;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    boolean isSourceBlocked(SourceFingerprint source) {
        OffsetDateTime now = databaseTime();
        Optional<Boolean> blocked = jdbcClient
                .sql(
                        """
                        SELECT COALESCE(blocked_until > :now, false)
                        FROM koiki_login_attempt
                        WHERE scope = 'SOURCE'
                          AND source_key_id = :keyId
                          AND source_fingerprint = :fingerprint
                        """)
                .param("keyId", source.keyId())
                .param("fingerprint", source.value())
                .param("now", now)
                .query(Boolean.class)
                .optional();
        return blocked.orElse(false);
    }

    LoginFailureState recordFailure(
            @Nullable AccountProtectionState account, @Nullable SourceFingerprint source) {
        LoginFailureState result = transaction.execute(status -> {
            OffsetDateTime now = databaseTime();
            boolean accountLocked = account != null
                    && account.eligibleForAccountAttempt()
                    && recordAccountFailure(account.userId(), now);
            boolean sourceBlocked = source != null && recordSourceFailure(source, now);
            return new LoginFailureState(accountLocked, sourceBlocked);
        });
        if (result == null) {
            throw new IllegalStateException("Identity protection transaction did not complete.");
        }
        return result;
    }

    void recordSuccess(UUID userId) {
        transaction.executeWithoutResult(status -> {
            OffsetDateTime now = databaseTime();
            jdbcClient.sql("DELETE FROM koiki_login_attempt WHERE scope = 'ACCOUNT' AND user_id = :userId")
                    .param("userId", userId)
                    .update();
            jdbcClient.sql(
                            """
                            UPDATE koiki_password_credential
                            SET locked_until = NULL,
                                version = version + 1,
                                updated_at = :now
                            WHERE user_id = :userId
                              AND locked_until IS NOT NULL
                              AND locked_until <= :now
                            """)
                    .param("now", now)
                    .param("userId", userId)
                    .update();
        });
    }

    private boolean recordAccountFailure(UUID userId, OffsetDateTime now) {
        int count = jdbcClient
                .sql(
                        """
                        INSERT INTO koiki_login_attempt(
                            attempt_id, scope, user_id, failure_count, window_started_at,
                            last_failed_at, created_at, updated_at)
                        VALUES (:attemptId, 'ACCOUNT', :userId, 1, :now, :now, :now, :now)
                        ON CONFLICT (user_id) WHERE scope = 'ACCOUNT'
                        DO UPDATE SET
                            failure_count = CASE
                                WHEN koiki_login_attempt.window_started_at <= :windowFloor THEN 1
                                ELSE koiki_login_attempt.failure_count + 1
                            END,
                            window_started_at = CASE
                                WHEN koiki_login_attempt.window_started_at <= :windowFloor THEN :now
                                ELSE koiki_login_attempt.window_started_at
                            END,
                            blocked_until = CASE
                                WHEN koiki_login_attempt.window_started_at <= :windowFloor THEN NULL
                                ELSE koiki_login_attempt.blocked_until
                            END,
                            last_failed_at = :now,
                            updated_at = :now
                        RETURNING failure_count
                        """)
                .param("attemptId", UUID.randomUUID())
                .param("userId", userId)
                .param("now", now)
                .param("windowFloor", now.minus(properties.getLoginAttempt().getAccountWindow()))
                .query(Integer.class)
                .single();
        if (count < properties.getLoginAttempt().getAccountThreshold()) {
            return false;
        }
        OffsetDateTime lockedUntil = now.plus(properties.getLoginAttempt().getAccountLockDuration());
        int locked = jdbcClient
                .sql(
                        """
                        UPDATE koiki_password_credential
                        SET locked_until = :lockedUntil,
                            version = version + 1,
                            updated_at = :now
                        WHERE user_id = :userId
                          AND (locked_until IS NULL OR locked_until <= :now)
                        """)
                .param("lockedUntil", lockedUntil)
                .param("now", now)
                .param("userId", userId)
                .update();
        if (locked == 1) {
            jdbcClient.sql(
                            """
                            UPDATE koiki_login_attempt
                            SET blocked_until = :lockedUntil, updated_at = :now
                            WHERE scope = 'ACCOUNT' AND user_id = :userId
                            """)
                    .param("lockedUntil", lockedUntil)
                    .param("now", now)
                    .param("userId", userId)
                    .update();
        }
        return locked == 1;
    }

    private boolean recordSourceFailure(SourceFingerprint source, OffsetDateTime now) {
        int count = jdbcClient
                .sql(
                        """
                        INSERT INTO koiki_login_attempt(
                            attempt_id, scope, source_key_id, source_fingerprint, failure_count,
                            window_started_at, last_failed_at, created_at, updated_at)
                        VALUES (:attemptId, 'SOURCE', :keyId, :fingerprint, 1, :now, :now, :now, :now)
                        ON CONFLICT (source_key_id, source_fingerprint) WHERE scope = 'SOURCE'
                        DO UPDATE SET
                            failure_count = CASE
                                WHEN koiki_login_attempt.window_started_at <= :windowFloor THEN 1
                                ELSE koiki_login_attempt.failure_count + 1
                            END,
                            window_started_at = CASE
                                WHEN koiki_login_attempt.window_started_at <= :windowFloor THEN :now
                                ELSE koiki_login_attempt.window_started_at
                            END,
                            blocked_until = CASE
                                WHEN koiki_login_attempt.window_started_at <= :windowFloor THEN NULL
                                ELSE koiki_login_attempt.blocked_until
                            END,
                            last_failed_at = :now,
                            updated_at = :now
                        RETURNING failure_count
                        """)
                .param("attemptId", UUID.randomUUID())
                .param("keyId", source.keyId())
                .param("fingerprint", source.value())
                .param("now", now)
                .param("windowFloor", now.minus(properties.getLoginAttempt().getSourceWindow()))
                .query(Integer.class)
                .single();
        if (count < properties.getLoginAttempt().getSourceThreshold()) {
            return false;
        }
        OffsetDateTime blockedUntil = now.plus(properties.getLoginAttempt().getSourceBlockDuration());
        int blocked = jdbcClient
                .sql(
                        """
                        UPDATE koiki_login_attempt
                        SET blocked_until = :blockedUntil, updated_at = :now
                        WHERE scope = 'SOURCE'
                          AND source_key_id = :keyId
                          AND source_fingerprint = :fingerprint
                          AND (blocked_until IS NULL OR blocked_until <= :now)
                        """)
                .param("blockedUntil", blockedUntil)
                .param("now", now)
                .param("keyId", source.keyId())
                .param("fingerprint", source.value())
                .update();
        return blocked == 1;
    }

    private OffsetDateTime databaseTime() {
        return clock.instant().atOffset(ZoneOffset.UTC);
    }
}
