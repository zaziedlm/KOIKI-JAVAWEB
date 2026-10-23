package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reference Session transport over the existing scoped application use cases. */
@RestController
@RequestMapping("/spa-api/v1")
public class ExpenseSessionApiController {
    private final ExpenseReadService reads;
    private final ExpenseApplicationService commands;
    private final SessionExpenseApiMapper mapper;

    ExpenseSessionApiController(ExpenseReadService reads, ExpenseApplicationService commands,
            SessionExpenseApiMapper mapper) {
        this.reads = reads;
        this.commands = commands;
        this.mapper = mapper;
    }

    @GetMapping("/expense-options")
    SessionExpenseOptionsResponse options() {
        return new SessionExpenseOptionsResponse(reads.findAvailableDepartments().stream().map(mapper::option).toList(),
                reads.findAvailableExpenseCategories().stream().map(mapper::option).toList());
    }

    @GetMapping("/expense-requests")
    SessionExpensePageResponse list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // This is transport validation; application scope remains in the use case.
        if (page < 0 || size < 1 || size > 100) {
            throw new SessionExpenseInputException();
        }
        return mapper.page(reads.findOwnRequests(page, size));
    }

    @GetMapping("/expense-requests/{id}")
    SessionExpenseDetailResponse detail(@PathVariable String id) {
        return mapper.detail(reads.findOwnRequest(id(id))
                .orElseThrow(SessionExpenseNotFoundException::new));
    }

    @PostMapping("/expense-requests")
    ResponseEntity<CreateSessionExpenseResponse> create(@Valid @RequestBody CreateSessionExpenseRequest request) {
        UUID id = commands.createDraft(request.departmentId(), Long.parseLong(request.claimedAmount()),
                mapper.toLines(request));
        return ResponseEntity.created(URI.create("/spa-api/v1/expense-requests/" + id))
                .body(new CreateSessionExpenseResponse(id));
    }

    @PostMapping("/expense-requests/{id}/submit")
    ResponseEntity<Void> submit(@PathVariable String id, @Valid @RequestBody SubmitSessionExpenseRequest request) {
        commands.submit(id(id), Long.parseLong(request.expectedVersion()));
        return ResponseEntity.noContent().build();
    }

    private static UUID id(String value) {
        if (!CanonicalUuidDeserializer.isCanonical(value)) {
            throw new SessionExpenseInputException();
        }
        return UUID.fromString(value);
    }
}
