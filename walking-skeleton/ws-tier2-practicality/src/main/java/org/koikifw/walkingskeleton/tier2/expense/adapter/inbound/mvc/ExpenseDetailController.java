package org.koikifw.walkingskeleton.tier2.expense.adapter.inbound.mvc;

import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.expense.application.ExpenseDetailQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/expenses")
public class ExpenseDetailController {

    private final ExpenseDetailQuery expenseDetailQuery;

    public ExpenseDetailController(ExpenseDetailQuery expenseDetailQuery) {
        this.expenseDetailQuery = expenseDetailQuery;
    }

    @GetMapping("/{expenseRequestId}")
    public String detail(@PathVariable UUID expenseRequestId, Model model) {
        model.addAttribute("expense", expenseDetailQuery.find(expenseRequestId));
        return "expense/detail";
    }
}
