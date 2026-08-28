package org.koikifw.runtimeconsumer.workitem.adapter.inbound.mvc;

import java.util.UUID;

/** HTTP output that does not expose the persistence model. */
public record CreateWorkItemResponse(UUID id) {
}
