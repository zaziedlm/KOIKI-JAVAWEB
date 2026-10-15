package org.koikifw.reference.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;

class ExpenseApprovedEventContractTest {
    @Test
    @SuppressWarnings("NullAway") // Intentional invalid-null constructor inputs for K01.
    void requiresOnlyImmutableValidApprovalValues() {
        UUID id = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> new ExpenseApprovedEvent(null, id, id, 3, 1200));
        assertThrows(NullPointerException.class, () -> new ExpenseApprovedEvent(id, null, id, 3, 1200));
        assertThrows(NullPointerException.class, () -> new ExpenseApprovedEvent(id, id, null, 3, 1200));
        assertThrows(IllegalArgumentException.class, () -> new ExpenseApprovedEvent(id, id, id, 0, 1200));
        assertThrows(IllegalArgumentException.class, () -> new ExpenseApprovedEvent(id, id, id, 3, 0));
        assertTrue(ExpenseApprovedEvent.class.isRecord());
        assertTrue(Arrays.stream(ExpenseApprovedEvent.class.getRecordComponents())
                .allMatch(component -> Set.of(UUID.class, long.class).contains(component.getType())));
    }

    @Test
    void keepsBusinessPayloadAndKeyStableAndSeparatesOtherApprovals() {
        UUID expense = UUID.randomUUID();
        UUID applicant = UUID.randomUUID();
        var first = new ExpenseApprovedEvent(UUID.randomUUID(), expense, applicant, 3, 1200);
        var same = new ExpenseApprovedEvent(UUID.randomUUID(), expense, applicant, 3, 1200);
        assertEquals(first.notificationKey(), same.notificationKey());
        assertEquals(first.canonicalPayload(), same.canonicalPayload());
        assertEquals(first.payloadSha256(), same.payloadSha256());
        assertNotEquals(first.eventId(), same.eventId());
        assertNotEquals(first.notificationKey(), new ExpenseApprovedEvent(
                UUID.randomUUID(), UUID.randomUUID(), applicant, 3, 1200).notificationKey());
        assertNotEquals(first.notificationKey(), new ExpenseApprovedEvent(
                UUID.randomUUID(), expense, applicant, 4, 1200).notificationKey());
        var conflict = new ExpenseApprovedEvent(UUID.randomUUID(), expense, applicant, 3, 1300);
        assertEquals(first.notificationKey(), conflict.notificationKey());
        assertNotEquals(first.payloadSha256(), conflict.payloadSha256());
    }
}
