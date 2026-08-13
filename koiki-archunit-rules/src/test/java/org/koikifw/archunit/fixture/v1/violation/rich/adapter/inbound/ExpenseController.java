package org.koikifw.archunit.fixture.v1.violation.rich.adapter.inbound;

import org.koikifw.archunit.fixture.v1.violation.rich.application.ExpenseUseCase;
import org.koikifw.archunit.fixture.v1.violation.rich.domain.model.Expense;
import org.koikifw.archunit.fixture.v1.violation.rich.domain.repository.BadExpenseRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExpenseController {
    private final ExpenseUseCase useCase = new ExpenseUseCase();
    private BadExpenseRepository repository;

    @GetMapping("/expense")
    public Expense show(Expense input, Model model) {
        Expense expense = useCase.load();
        model.addAttribute("expense", expense);
        return expense;
    }
}
