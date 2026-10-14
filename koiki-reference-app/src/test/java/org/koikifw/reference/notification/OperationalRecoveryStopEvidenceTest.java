package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.koikifw.referenceacceptance.notification.OperationalRecoveryTestSupport.*;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.MutableClock;

class OperationalRecoveryStopEvidenceTest {
    final MutableClock clock = new MutableClock();
    final RecoveryTarget target = target();
    final Model model = new Model(clock, target);
    final StopReceipt receipt = model.stop(target);
    boolean valid() { return model.validStop(receipt, target); }
    @Test void S01RequiresStoppedDrainedAndControlledGeneration() { assertThat(valid()).isTrue(); model.controlGeneration++; assertThat(valid()).isFalse(); }
    @Test void S02RejectsAliveOrUndrainedProcess() { model.processStopped = false; assertThat(valid()).isFalse(); model.processStopped = true; model.drained = false; assertThat(valid()).isFalse(); }
    @Test void S03RejectsOtherProcessOrGeneration() { model.process = "other"; assertThat(valid()).isFalse(); model.process = receipt.process(); model.generation = "next"; assertThat(valid()).isFalse(); }
    @Test void S04RejectsDeployTargetAndIssuerMismatch() {
        model.deploy = "other"; assertThat(valid()).isFalse(); model.deploy = receipt.deploy();
        model.issuer = "untrusted"; assertThat(valid()).isFalse(); model.issuer = receipt.issuer();
        assertThat(model.validStop(receipt, target())).isFalse();
    }
    @Test void S05RejectsExpiryEquality() { clock.now = FROM.plusSeconds(60); assertThat(valid()).isFalse(); }
    @Test void S06RevokesAfterRestart() { model.restarted = true; assertThat(valid()).isFalse(); }
    @Test void S07RejectsTransferAndControlLoss() { model.transferred = true; assertThat(valid()).isFalse(); model.transferred = false; model.controlAvailable = false; assertThat(valid()).isFalse(); }
    @Test void S08WorkerLockCannotReplaceStopReceipt() {
        model.recoveryLockHeld = true;
        assertThat(model.recoveryLockHeld).isTrue();
        var forged = new StopReceipt(target, receipt.process(), receipt.generation(), receipt.deploy(), receipt.issuer(), 1, false, false, window());
        assertThat(model.validStop(forged, target)).isFalse(); // No recovery lock input can turn this into stopped evidence.
        var changed = new StopReceipt(target, receipt.process(), "tampered", receipt.deploy(), receipt.issuer(), 1, true, true, window());
        assertThat(model.validStop(changed, target)).isFalse();
    }
}
