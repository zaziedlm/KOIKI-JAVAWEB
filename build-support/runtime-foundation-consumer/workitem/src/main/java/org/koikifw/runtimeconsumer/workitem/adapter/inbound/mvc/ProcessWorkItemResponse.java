package org.koikifw.runtimeconsumer.workitem.adapter.inbound.mvc;

import java.util.UUID;

/** Immutable HTTP response for accepted asynchronous workitem processing. */
public record ProcessWorkItemResponse(UUID id, String result) {
}
