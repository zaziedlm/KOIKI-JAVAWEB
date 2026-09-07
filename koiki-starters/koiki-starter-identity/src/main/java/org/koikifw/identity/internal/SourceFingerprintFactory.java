package org.koikifw.identity.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

final class SourceFingerprintFactory {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String keyId;
    private final byte[] key;

    SourceFingerprintFactory(IdentityAuthenticationProperties properties) {
        this.keyId = properties.requiredSourceKeyId();
        this.key = properties.decodedSourceKey();
    }

    SourceFingerprint from(Authentication authentication) {
        Object details = authentication.getDetails();
        if (!(details instanceof WebAuthenticationDetails webDetails)) {
            throw new IllegalStateException("A trusted remote address is required for local authentication.");
        }
        String normalizedAddress = normalizeAddress(webDetails.getRemoteAddress());
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return new SourceFingerprint(
                    keyId, mac.doFinal(normalizedAddress.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Source protection is unavailable.", exception);
        }
    }

    private static String normalizeAddress(String value) {
        if (!value.matches("[0-9A-Fa-f:.]+")) {
            throw new IllegalStateException("A trusted remote address is required for local authentication.");
        }
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("A trusted remote address is required for local authentication.");
        }
    }
}
