package org.koikifw.identity.internal;

import java.time.Duration;
import java.util.Base64;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("koiki.identity")
class IdentityAuthenticationProperties {

    enum SourceProtection {
        APPLICATION,
        EXTERNAL
    }

    static class LocalAuthentication {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean value) {
            this.enabled = value;
        }
    }

    static class Password {
        private int maximumLength = 128;

        public int getMaximumLength() {
            return maximumLength;
        }

        public void setMaximumLength(int value) {
            this.maximumLength = value;
        }
    }

    static class LoginAttempt {
        private int accountThreshold = 5;
        private Duration accountWindow = Duration.ofMinutes(15);
        private Duration accountLockDuration = Duration.ofMinutes(30);
        private SourceProtection sourceProtection = SourceProtection.APPLICATION;
        private int sourceThreshold = 100;
        private Duration sourceWindow = Duration.ofMinutes(15);
        private Duration sourceBlockDuration = Duration.ofMinutes(15);
        private Duration retention = Duration.ofHours(24);
        private @Nullable String sourceHmacKeyId;
        private @Nullable String sourceHmacKey;

        public int getAccountThreshold() {
            return accountThreshold;
        }

        public void setAccountThreshold(int value) {
            this.accountThreshold = value;
        }

        public Duration getAccountWindow() {
            return accountWindow;
        }

        public void setAccountWindow(Duration value) {
            this.accountWindow = value;
        }

        public Duration getAccountLockDuration() {
            return accountLockDuration;
        }

        public void setAccountLockDuration(Duration value) {
            this.accountLockDuration = value;
        }

        public SourceProtection getSourceProtection() {
            return sourceProtection;
        }

        public void setSourceProtection(SourceProtection value) {
            this.sourceProtection = value;
        }

        public int getSourceThreshold() {
            return sourceThreshold;
        }

        public void setSourceThreshold(int value) {
            this.sourceThreshold = value;
        }

        public Duration getSourceWindow() {
            return sourceWindow;
        }

        public void setSourceWindow(Duration value) {
            this.sourceWindow = value;
        }

        public Duration getSourceBlockDuration() {
            return sourceBlockDuration;
        }

        public void setSourceBlockDuration(Duration value) {
            this.sourceBlockDuration = value;
        }

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration value) {
            this.retention = value;
        }

        public @Nullable String getSourceHmacKeyId() {
            return sourceHmacKeyId;
        }

        public void setSourceHmacKeyId(@Nullable String value) {
            this.sourceHmacKeyId = value;
        }

        public @Nullable String getSourceHmacKey() {
            return sourceHmacKey;
        }

        public void setSourceHmacKey(@Nullable String value) {
            this.sourceHmacKey = value;
        }
    }

    private final LocalAuthentication localAuthentication = new LocalAuthentication();
    private final Password password = new Password();
    private final LoginAttempt loginAttempt = new LoginAttempt();

    public LocalAuthentication getLocalAuthentication() {
        return localAuthentication;
    }

    public Password getPassword() {
        return password;
    }

    public LoginAttempt getLoginAttempt() {
        return loginAttempt;
    }

    void validate() {
        requireRange(password.maximumLength, 64, 1024, "password.maximum-length");
        requireRange(loginAttempt.accountThreshold, 3, 10, "login-attempt.account-threshold");
        requireRange(loginAttempt.sourceThreshold, 10, 10_000, "login-attempt.source-threshold");
        requireDuration(loginAttempt.accountWindow, "login-attempt.account-window");
        requireDuration(loginAttempt.accountLockDuration, "login-attempt.account-lock-duration");
        requireDuration(loginAttempt.sourceWindow, "login-attempt.source-window");
        requireDuration(loginAttempt.sourceBlockDuration, "login-attempt.source-block-duration");
        if (loginAttempt.retention.compareTo(Duration.ofDays(30)) > 0
                || loginAttempt.retention.compareTo(loginAttempt.accountWindow) < 0
                || loginAttempt.retention.compareTo(loginAttempt.accountLockDuration) < 0
                || loginAttempt.retention.compareTo(loginAttempt.sourceWindow) < 0
                || loginAttempt.retention.compareTo(loginAttempt.sourceBlockDuration) < 0) {
            throw invalid("login-attempt.retention");
        }
        if (loginAttempt.sourceProtection == SourceProtection.APPLICATION) {
            requireHmacConfiguration();
        }
    }

    byte[] decodedSourceKey() {
        String configured = loginAttempt.sourceHmacKey;
        if (configured == null) {
            throw invalid("login-attempt.source-hmac-key");
        }
        try {
            return Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException exception) {
            throw invalid("login-attempt.source-hmac-key");
        }
    }

    String requiredSourceKeyId() {
        String configured = loginAttempt.sourceHmacKeyId;
        if (configured == null) {
            throw invalid("login-attempt.source-hmac-key-id");
        }
        return configured;
    }

    private void requireHmacConfiguration() {
        String keyId = loginAttempt.sourceHmacKeyId;
        if (keyId == null || keyId.isBlank() || keyId.length() > 100 || !keyId.equals(keyId.trim())) {
            throw invalid("login-attempt.source-hmac-key-id");
        }
        if (decodedSourceKey().length < 32) {
            throw invalid("login-attempt.source-hmac-key");
        }
    }

    private static void requireRange(int value, int minimum, int maximum, String property) {
        if (value < minimum || value > maximum) {
            throw invalid(property);
        }
    }

    private static void requireDuration(Duration value, String property) {
        if (value.compareTo(Duration.ofMinutes(1)) < 0
                || value.compareTo(Duration.ofHours(24)) > 0) {
            throw invalid(property);
        }
    }

    private static IllegalStateException invalid(String property) {
        return new IllegalStateException("Invalid KOIKI Identity setting: " + property);
    }
}
