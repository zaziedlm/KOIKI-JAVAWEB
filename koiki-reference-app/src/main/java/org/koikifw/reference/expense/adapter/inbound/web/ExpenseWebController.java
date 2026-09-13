package org.koikifw.reference.expense.adapter.inbound.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseReadService;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Adapts the authorized expense use cases to full-page Thymeleaf journeys. */
@Controller
public class ExpenseWebController {

    private static final int PAGE_SIZE = 20;
    private final ExpenseReadService reads;
    private final ExpenseApplicationService commands;

    public ExpenseWebController(ExpenseReadService reads, ExpenseApplicationService commands) {
        this.reads = reads;
        this.commands = commands;
    }

    @GetMapping("/expenses")
    String ownRequests(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", reads.findOwnRequests(page, PAGE_SIZE));
        model.addAttribute("context", "mine");
        model.addAttribute("title", "自分の経費申請");
        return "expense/list";
    }

    @GetMapping("/expenses/approvals")
    String approvalQueue(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", reads.findApprovalQueue(page, PAGE_SIZE));
        model.addAttribute("context", "approvals");
        model.addAttribute("title", "承認対象の経費申請");
        return "expense/list";
    }

    @GetMapping("/expenses/accounting")
    String accountingQueue(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("result", reads.findAccountingQueue(page, PAGE_SIZE));
        model.addAttribute("context", "accounting");
        model.addAttribute("title", "精算対象の経費申請");
        return "expense/list";
    }

    @GetMapping("/expenses/new")
    String newDraft(Model model) {
        populateOptions(model);
        model.addAttribute("form", ExpenseDraftForm.empty());
        model.addAttribute("editing", false);
        return "expense/form";
    }

    @PostMapping("/expenses")
    String createDraft(
            @Valid @ModelAttribute("form") ExpenseDraftForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.completeForCreate()) {
            populateOptions(model);
            model.addAttribute("editing", false);
            return "expense/form";
        }
        UUID id = commands.createDraft(
                form.requiredDepartmentId(), form.requiredClaimedAmount(), List.of(form.toLineInput()));
        redirectAttributes.addFlashAttribute("notice", "経費申請を下書き保存しました。");
        return "redirect:/expenses/" + id;
    }

    @GetMapping("/expenses/{expenseRequestId}")
    String ownDetail(@PathVariable UUID expenseRequestId, Model model) {
        return detail(reads.findOwnRequest(expenseRequestId).orElseThrow(ExpenseWebController::notFound),
                "mine", model);
    }

    @GetMapping("/expenses/{expenseRequestId}/edit")
    String editDraft(@PathVariable UUID expenseRequestId, Model model) {
        ExpenseRequestDetail detail = reads.findOwnRequest(expenseRequestId)
                .orElseThrow(ExpenseWebController::notFound);
        if (!"DRAFT".equals(detail.status()) || detail.lines().size() != 1) {
            throw notFound();
        }
        populateOptions(model);
        model.addAttribute("form", ExpenseDraftForm.from(detail));
        model.addAttribute("editing", true);
        model.addAttribute("expenseRequestId", expenseRequestId);
        return "expense/form";
    }

    @PostMapping("/expenses/{expenseRequestId}/edit")
    String updateDraft(
            @PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute("form") ExpenseDraftForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.completeForEdit()) {
            populateOptions(model);
            model.addAttribute("editing", true);
            model.addAttribute("expenseRequestId", expenseRequestId);
            return "expense/form";
        }
        commands.editDraft(
                expenseRequestId,
                form.requiredExpectedVersion(),
                form.requiredClaimedAmount(),
                List.of(form.toLineInput()));
        redirectAttributes.addFlashAttribute("notice", "下書きを更新しました。");
        return "redirect:/expenses/" + expenseRequestId;
    }

    @GetMapping("/expenses/approvals/{expenseRequestId}")
    String approvalDetail(@PathVariable UUID expenseRequestId, Model model) {
        return detail(reads.findApprovalRequest(expenseRequestId)
                .orElseThrow(ExpenseWebController::notFound), "approvals", model);
    }

    @GetMapping("/expenses/accounting/{expenseRequestId}")
    String accountingDetail(@PathVariable UUID expenseRequestId, Model model) {
        return detail(reads.findAccountingRequest(expenseRequestId)
                .orElseThrow(ExpenseWebController::notFound), "accounting", model);
    }

    @PostMapping("/expenses/{expenseRequestId}/submit")
    String submit(@PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute ExpenseVersionForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        requireValid(bindingResult, form.valid());
        commands.submit(expenseRequestId, form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費申請を提出しました。");
        return "redirect:/expenses/" + expenseRequestId;
    }

    @PostMapping("/expenses/{expenseRequestId}/reedit")
    String reedit(@PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute ExpenseVersionForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        requireValid(bindingResult, form.valid());
        commands.beginReedit(expenseRequestId, form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "申請を再編集可能な下書きへ戻しました。");
        return "redirect:/expenses/" + expenseRequestId;
    }

    @PostMapping("/expenses/approvals/{expenseRequestId}/approve")
    String approve(@PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute ExpenseVersionForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        requireValid(bindingResult, form.valid());
        commands.approve(expenseRequestId, form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費申請を承認しました。");
        return "redirect:/expenses/approvals";
    }

    @PostMapping("/expenses/approvals/{expenseRequestId}/reject")
    String reject(@PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute ExpenseDecisionForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        requireValid(bindingResult, form.valid());
        commands.reject(expenseRequestId, form.requiredReason(), form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費申請を却下しました。");
        return "redirect:/expenses/approvals";
    }

    @PostMapping("/expenses/approvals/{expenseRequestId}/return")
    String returnForRework(@PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute ExpenseDecisionForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        requireValid(bindingResult, form.valid());
        commands.returnForRework(expenseRequestId, form.requiredReason(), form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費申請を差し戻しました。");
        return "redirect:/expenses/approvals";
    }

    @PostMapping("/expenses/accounting/{expenseRequestId}/settle")
    String settle(@PathVariable UUID expenseRequestId,
            @Valid @ModelAttribute ExpenseVersionForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        requireValid(bindingResult, form.valid());
        commands.settle(expenseRequestId, form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費申請を精算済みにしました。");
        return "redirect:/expenses/accounting";
    }

    private String detail(ExpenseRequestDetail detail, String context, Model model) {
        model.addAttribute("request", detail);
        model.addAttribute("context", context);
        return "expense/detail";
    }

    private void populateOptions(Model model) {
        model.addAttribute("departments", reads.findAvailableDepartments());
        model.addAttribute("categories", reads.findAvailableExpenseCategories());
    }

    private static void requireValid(BindingResult bindingResult, boolean complete) {
        if (bindingResult.hasErrors() || !complete) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid expense form.");
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense request was not found.");
    }
}
