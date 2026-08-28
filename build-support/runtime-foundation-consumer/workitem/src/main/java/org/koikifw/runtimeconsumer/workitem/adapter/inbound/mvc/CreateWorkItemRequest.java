package org.koikifw.runtimeconsumer.workitem.adapter.inbound.mvc;

import jakarta.validation.constraints.NotBlank;

/** HTTP input owned by the Consumer workitem module. */
public record CreateWorkItemRequest(@NotBlank String label) {
}
