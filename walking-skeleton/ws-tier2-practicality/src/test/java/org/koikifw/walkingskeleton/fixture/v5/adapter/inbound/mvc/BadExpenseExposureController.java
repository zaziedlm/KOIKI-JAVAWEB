package org.koikifw.walkingskeleton.fixture.v5.adapter.inbound.mvc;

import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class BadExpenseExposureController {

    private final BadExpenseExposureSource source;

    public BadExpenseExposureController(BadExpenseExposureSource source) {
        this.source = source;
    }

    @GetMapping("/test/architecture/entity-exposure")
    public String detail(Model model) {
        ExpenseRequest expenseRequest = source.load();
        model.addAttribute("expense", expenseRequest);
        return "test/entity-exposure";
    }
}
