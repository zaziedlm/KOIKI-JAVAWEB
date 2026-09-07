package org.koikifw.identity.internal;

import java.text.Normalizer;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.SecurityAuditRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.crypto.password.PasswordEncoder;

final class KoikiLocalAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(KoikiLocalAuthenticationProvider.class);
    private static final String GENERIC_MESSAGE = "Bad credentials";

    private final DaoAuthenticationProvider delegate;
    private final IdentityUserDetailsService userDetailsService;
    private final LoginAttemptStore attemptStore;
    private final @Nullable SourceFingerprintFactory sourceFingerprintFactory;
    private final SecurityAuditRecorder auditRecorder;
    private final IdentityAuthenticationProperties properties;

    KoikiLocalAuthenticationProvider(
            IdentityUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            LoginAttemptStore attemptStore,
            @Nullable SourceFingerprintFactory sourceFingerprintFactory,
            SecurityAuditRecorder auditRecorder,
            IdentityAuthenticationProperties properties) {
        this.userDetailsService = userDetailsService;
        this.attemptStore = attemptStore;
        this.sourceFingerprintFactory = sourceFingerprintFactory;
        this.auditRecorder = auditRecorder;
        this.properties = properties;
        this.delegate = new DaoAuthenticationProvider(userDetailsService);
        this.delegate.setPasswordEncoder(passwordEncoder);
        this.delegate.setUserDetailsPasswordService(userDetailsService);
        this.delegate.setHideUserNotFoundExceptions(true);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String presentedIdentifier = authentication.getName();
        SourceFingerprint source = sourceFingerprint(authentication);
        if (source != null && isSourceBlocked(source)) {
            auditFailure(Optional.empty(), "SOURCE_BLOCKED");
            throw genericFailure();
        }

        final String normalizedPassword;
        try {
            normalizedPassword = normalizePassword(authentication.getCredentials());
        } catch (BadCredentialsException exception) {
            rejectFailure(presentedIdentifier, source);
            throw genericFailure();
        }
        UsernamePasswordAuthenticationToken request =
                UsernamePasswordAuthenticationToken.unauthenticated(presentedIdentifier, normalizedPassword);
        request.setDetails(authentication.getDetails());

        final Authentication result;
        try {
            result = delegate.authenticate(request);
        } catch (AuthenticationException exception) {
            rejectFailure(presentedIdentifier, source);
            throw genericFailure();
        } catch (RuntimeException exception) {
            auditFailure(Optional.empty(), "DEPENDENCY_FAILURE");
            throw genericFailure();
        }
        if (!(result.getPrincipal() instanceof KoikiIdentityUserDetails principal)
                || !principal.authenticationAllowed()) {
            erase(result);
            rejectFailure(presentedIdentifier, source);
            throw genericFailure();
        }

        try {
            attemptStore.recordSuccess(principal.userId().value());
            auditRecorder.record(AuditEvent.of(
                            "IDENTITY_LOGIN", AuditActor.user(principal.userId().toString()), "LOGIN", AuditResult.SUCCESS)
                    .withSubject(principal.userId().toString()));
            erase(result);
            return result;
        } catch (RuntimeException exception) {
            erase(result);
            logger.error("KOIKI-ID-AUTH-001: local authentication completion failed", exception);
            throw genericFailure();
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean isSourceBlocked(SourceFingerprint source) {
        try {
            return attemptStore.isSourceBlocked(source);
        } catch (RuntimeException exception) {
            auditFailure(Optional.empty(), "DEPENDENCY_FAILURE");
            logger.error("KOIKI-ID-AUTH-004: login attempt protection failed", exception);
            throw genericFailure();
        }
    }

    private @Nullable SourceFingerprint sourceFingerprint(Authentication authentication) {
        if (properties.getLoginAttempt().getSourceProtection()
                == IdentityAuthenticationProperties.SourceProtection.EXTERNAL) {
            return null;
        }
        SourceFingerprintFactory factory = sourceFingerprintFactory;
        if (factory == null) {
            throw genericFailure();
        }
        try {
            return factory.from(authentication);
        } catch (RuntimeException exception) {
            logger.error("KOIKI-ID-AUTH-002: trusted authentication source is unavailable", exception);
            throw genericFailure();
        }
    }

    private String normalizePassword(@Nullable Object credentials) {
        String value = credentials instanceof String string ? string : "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        if (normalized.codePointCount(0, normalized.length())
                > properties.getPassword().getMaximumLength()) {
            throw genericFailure();
        }
        return normalized;
    }

    private void rejectFailure(String presentedIdentifier, @Nullable SourceFingerprint source) {
        Optional<AccountProtectionState> account = Optional.empty();
        final LoginFailureState state;
        try {
            account = userDetailsService.protectionState(presentedIdentifier);
            state = attemptStore.recordFailure(account.orElse(null), source);
        } catch (RuntimeException exception) {
            auditFailure(account, "PROTECTION_STATE_FAILURE");
            logger.error("KOIKI-ID-AUTH-004: login attempt protection failed", exception);
            throw genericFailure();
        }
        String reason = state.accountLockedNow()
                ? "ACCOUNT_LOCKED"
                : state.sourceBlockedNow() ? "SOURCE_BLOCKED" : "BAD_CREDENTIALS";
        auditFailure(account, reason);
    }

    private void auditFailure(Optional<AccountProtectionState> account, String reason) {
        AuditEvent event = AuditEvent.of(
                        "IDENTITY_LOGIN", AuditActor.anonymous(), "LOGIN", AuditResult.FAILURE)
                .withReason(reason);
        if (account.isPresent()) {
            event = event.withSubject(account.orElseThrow().userId().toString());
        }
        try {
            auditRecorder.record(event);
        } catch (AuditRecordingException exception) {
            logger.error("KOIKI-ID-AUTH-003: security audit recording failed");
        }
    }

    private static void erase(Authentication authentication) {
        if (authentication instanceof CredentialsContainer credentials) {
            credentials.eraseCredentials();
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CredentialsContainer credentials) {
            credentials.eraseCredentials();
        }
    }

    private static BadCredentialsException genericFailure() {
        return new BadCredentialsException(GENERIC_MESSAGE);
    }
}
