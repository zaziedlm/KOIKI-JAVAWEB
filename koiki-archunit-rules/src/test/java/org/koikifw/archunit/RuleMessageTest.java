package org.koikifw.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class RuleMessageTest {

    @Test
    void formatsTheApprovedFailureContractWithSeparateReferences() {
        RuleMessage message = RuleMessage.of(
                4,
                List.of("ADR-004", "ADR-025"),
                "module間の変更方向と初期化順序が循環する",
                "event等で依存を一方向にする");

        assertEquals(
                "[KOIKI-ARCH-004] [ADR-004] [ADR-025]" + System.lineSeparator()
                        + "影響: module間の変更方向と初期化順序が循環する" + System.lineSeparator()
                        + "修正: event等で依存を一方向にする",
                message.description());
        assertEquals(
                "違反内容: catalog -> order -> catalog",
                message.violation("catalog -> order -> catalog"));
    }

    @Test
    void exposesRuleDescriptionAndViolationDetailForArchUnitComposition() {
        RuleMessage message = RuleMessage.of(
                19,
                List.of("ADR-023", "ADR-028"),
                "view描画時の遅延loadやresponse送信後の失敗を招く",
                "transaction内でDTOへ変換する");

        assertTrue(message.description().contains("[KOIKI-ARCH-019]"));
        assertTrue(message.description().contains("影響:"));
        assertTrue(message.description().contains("修正:"));
        assertEquals("違反内容: CatalogController.show", message.violation("CatalogController.show"));
    }

    @Test
    void copiesAuthorityReferencesDefensively() {
        List<String> authorities = new ArrayList<>(List.of("ADR-022"));
        RuleMessage message = RuleMessage.of(
                1,
                authorities,
                "依存方向が崩れる",
                "Application Use Caseを介する");

        authorities.set(0, "ADR-999");

        assertTrue(message.description().contains("[ADR-022]"));
    }

    @Test
    void rejectsAllowanceAndUnknownRuleIds() {
        IllegalArgumentException allowanceFailure = assertThrows(
                IllegalArgumentException.class,
                () -> RuleMessage.of(10, List.of("ADR-025"), "impact", "correction"));
        IllegalArgumentException unknownFailure = assertThrows(
                IllegalArgumentException.class,
                () -> RuleMessage.of(99, List.of("ADR-025"), "impact", "correction"));

        assertTrue(Objects.requireNonNull(allowanceFailure.getMessage()).contains("failure rule"));
        assertTrue(Objects.requireNonNull(unknownFailure.getMessage()).contains("failure rule"));
    }

    @Test
    void rejectsMissingOrMalformedMessageParts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RuleMessage.of(1, List.of(), "impact", "correction"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RuleMessage.of(1, List.of("ADR-22"), "impact", "correction"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RuleMessage.of(1, List.of("ADR-022"), " ", "correction"));
        assertThrows(
                IllegalArgumentException.class,
                () -> RuleMessage.of(1, List.of("ADR-022"), "impact", " "));
        assertThrows(NullPointerException.class, () -> RuleMessage.of(1, null, "impact", "correction"));
        assertThrows(NullPointerException.class, () -> RuleMessage.of(1, List.of("ADR-022"), "impact", null));
        assertThrows(IllegalArgumentException.class, () -> RuleMessage.of(
                1,
                List.of("ADR-022"),
                "impact",
                "correction").violation(" "));
    }
}
