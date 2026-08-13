package org.koikifw.walkingskeleton.tier2test.mvc;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test/lazy-expenses")
public class LazyExpenseExposureController {

    private final LazyExpenseEntityQuery query;

    public LazyExpenseExposureController(LazyExpenseEntityQuery query) {
        this.query = query;
    }

    @GetMapping("/{expenseRequestId}")
    public String detail(@PathVariable UUID expenseRequestId, Model model) {
        model.addAttribute("expense", query.find(expenseRequestId));
        return "test/lazy-expense";
    }
}
