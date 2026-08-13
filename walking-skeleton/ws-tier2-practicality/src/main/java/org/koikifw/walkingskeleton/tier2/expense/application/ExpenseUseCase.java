package org.koikifw.walkingskeleton.tier2.expense.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseLine;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequestId;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.Money;
import org.koikifw.walkingskeleton.tier2.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseUseCase {

    private final ExpenseRequestRepository repository;

    public ExpenseUseCase(ExpenseRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ExpenseResult create(CreateExpenseCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        List<ExpenseLine> lines = command.lines().stream()
                .map(line -> ExpenseLine.of(line.description(), Money.of(line.amount())))
                .toList();
        ExpenseRequest expenseRequest = ExpenseRequest.draft(
                command.categoryId(),
                command.description(),
                Money.of(command.requestedAmount()),
                lines);
        repository.save(expenseRequest);
        return toResult(expenseRequest);
    }

    @Transactional
    public ExpenseResult submit(UUID expenseRequestId) {
        ExpenseRequest expenseRequest = load(expenseRequestId);
        expenseRequest.submit();
        return toResult(expenseRequest);
    }

    @Transactional
    public ExpenseResult approve(UUID expenseRequestId) {
        ExpenseRequest expenseRequest = load(expenseRequestId);
        expenseRequest.approve();
        return toResult(expenseRequest);
    }

    @Transactional
    public ExpenseResult reject(UUID expenseRequestId) {
        ExpenseRequest expenseRequest = load(expenseRequestId);
        expenseRequest.reject();
        return toResult(expenseRequest);
    }

    private ExpenseRequest load(UUID expenseRequestId) {
        return repository.findById(ExpenseRequestId.of(expenseRequestId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "expense request not found: " + expenseRequestId));
    }

    private static ExpenseResult toResult(ExpenseRequest expenseRequest) {
        return new ExpenseResult(
                expenseRequest.id().value(), expenseRequest.status().name());
    }

    public record CreateExpenseCommand(
            UUID categoryId,
            String description,
            BigDecimal requestedAmount,
            List<ExpenseLineCommand> lines) {

        public CreateExpenseCommand {
            lines = List.copyOf(lines);
        }
    }

    public record ExpenseLineCommand(String description, BigDecimal amount) {
    }

    public record ExpenseResult(UUID expenseRequestId, String status) {
    }
}
