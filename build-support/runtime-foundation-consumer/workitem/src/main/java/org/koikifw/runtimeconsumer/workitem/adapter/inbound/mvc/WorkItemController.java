package org.koikifw.runtimeconsumer.workitem.adapter.inbound.mvc;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned HTTP adapter for the Customer-like workitem flow. */
@RestController
@RequestMapping("/api/{version}/work-items")
public final class WorkItemController {

    private final CreateWorkItemUseCase createWorkItem;

    public WorkItemController(CreateWorkItemUseCase createWorkItem) {
        this.createWorkItem = createWorkItem;
    }

    @PostMapping(version = "1")
    public ResponseEntity<CreateWorkItemResponse> create(@Valid @RequestBody CreateWorkItemRequest request) {
        UUID id = createWorkItem.create(request.label());
        return ResponseEntity.created(URI.create("/api/1/work-items/" + id))
                .body(new CreateWorkItemResponse(id));
    }
}
