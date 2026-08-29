package org.koikifw.performance.fixture.adapter.inbound.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.UUID;
import org.koikifw.performance.fixture.application.CreatePerformanceItemUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Identical HTTP workload used by the bare and KOIKI application variants. */
@RestController
@RequestMapping("/performance/{version}")
public final class PerformanceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceController.class);

    private final CreatePerformanceItemUseCase createItem;

    public PerformanceController(CreatePerformanceItemUseCase createItem) {
        this.createItem = createItem;
    }

    @GetMapping(path = "/ready", version = "1")
    public ReadyResponse ready() {
        return new ReadyResponse("ready");
    }

    @GetMapping(path = "/success", version = "1")
    public SuccessResponse success() {
        LOGGER.atInfo()
                .addKeyValue("operation", "performanceHttpSuccess")
                .log("performance fixture request");
        return new SuccessResponse("ok");
    }

    @PostMapping(path = "/validate", version = "1")
    public SuccessResponse validate(@Valid @RequestBody PerformanceRequest request) {
        return new SuccessResponse(request.label());
    }

    @PostMapping(path = "/items", version = "1")
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody PerformanceRequest request) {
        UUID id = createItem.create(request.label());
        return ResponseEntity.created(URI.create("/performance/1/items/" + id))
                .body(new ItemResponse(id));
    }

    public record PerformanceRequest(@NotBlank String label) { }

    public record ReadyResponse(String status) { }

    public record SuccessResponse(String value) { }

    public record ItemResponse(UUID id) { }
}
