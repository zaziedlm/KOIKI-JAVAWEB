package org.koikifw.reference.expense.adapter.inbound.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseReadService;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapts the approved minimal REST surface to existing expense use cases. */
@RestController
@RequestMapping("/api/v{version:[1-9][0-9]*}/expense-requests")
public class ExpenseApiController {

    private final ExpenseReadService reads;
    private final ExpenseApplicationService commands;
    private final ExpenseApiMapper mapper;

    public ExpenseApiController(
            ExpenseReadService reads, ExpenseApplicationService commands, ExpenseApiMapper mapper) {
        this.reads = reads;
        this.commands = commands;
        this.mapper = mapper;
    }

    @GetMapping(path = "/{expenseRequestId}", version = "1")
    ExpenseDetailResponse detail(@PathVariable UUID expenseRequestId) {
        ExpenseRequestDetail detail = reads.findOwnRequest(expenseRequestId)
                .orElseThrow(ExpenseApiNotFoundException::new);
        return mapper.toResponse(detail);
    }

    @PostMapping(version = "1")
    ResponseEntity<CreateExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request) {
        UUID expenseRequestId = commands.createDraft(
                request.departmentId(), request.claimedAmount(), mapper.toLineInputs(request));
        URI location = URI.create("/api/v1/expense-requests/" + expenseRequestId);
        return ResponseEntity.created(location).body(new CreateExpenseResponse(expenseRequestId));
    }

    @PostMapping(path = "/{expenseRequestId}/submit", version = "1")
    ResponseEntity<Void> submit(
            @PathVariable UUID expenseRequestId,
            @Valid @RequestBody SubmitExpenseRequest request) {
        commands.submit(expenseRequestId, request.expectedVersion());
        return ResponseEntity.noContent().build();
    }
}
