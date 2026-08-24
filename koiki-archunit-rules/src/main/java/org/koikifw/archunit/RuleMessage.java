package org.koikifw.archunit;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Message metadata shared by package-private failure rules. */
final class RuleMessage {

    private static final Set<Integer> FAILURE_RULE_IDS = Set.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 24,
            28, 38, 39);

    private final int ruleId;
    private final List<String> authorities;
    private final String impact;
    private final String correction;

    private RuleMessage(
            int ruleId,
            List<String> authorities,
            String impact,
            String correction) {
        this.ruleId = ruleId;
        this.authorities = authorities;
        this.impact = impact;
        this.correction = correction;
    }

    static RuleMessage of(
            int ruleId,
            @Nullable List<String> authorities,
            @Nullable String impact,
            @Nullable String correction) {
        if (!FAILURE_RULE_IDS.contains(ruleId)) {
            throw new IllegalArgumentException(
                    "ruleId must identify a Phase 1a failure rule");
        }

        List<String> authorityCopy = List.copyOf(Objects.requireNonNull(
                authorities,
                "authorities must not be null"));
        if (authorityCopy.isEmpty() || authorityCopy.stream().anyMatch(RuleMessage::isInvalidAuthority)) {
            throw new IllegalArgumentException(
                    "authorities must contain ADR-nnn or section references");
        }

        return new RuleMessage(
                ruleId,
                authorityCopy,
                requireText("impact", impact),
                requireText("correction", correction));
    }

    String description() {
        return header()
                + System.lineSeparator()
                + "影響: " + impact
                + System.lineSeparator()
                + "修正: " + correction;
    }

    String violation(@Nullable String detail) {
        return "違反内容: " + requireText("detail", detail);
    }

    private String header() {
        String references = authorities.stream()
                .map(authority -> "[" + authority + "]")
                .collect(Collectors.joining(" "));
        return "[KOIKI-ARCH-%03d] %s".formatted(ruleId, references);
    }

    private static boolean isInvalidAuthority(String authority) {
        return !(authority.matches("ADR-\\d{3}")
                || (authority.startsWith("§") && authority.length() > 1));
    }

    private static String requireText(String parameterName, @Nullable String value) {
        String text = Objects.requireNonNull(
                value,
                parameterName + " must not be null");
        if (text.isBlank()) {
            throw new IllegalArgumentException(parameterName + " must not be blank");
        }
        return text;
    }
}
