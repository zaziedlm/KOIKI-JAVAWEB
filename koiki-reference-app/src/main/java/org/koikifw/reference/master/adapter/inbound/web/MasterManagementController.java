package org.koikifw.reference.master.adapter.inbound.web;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.koikifw.reference.master.application.MasterAdministration;
import org.koikifw.reference.master.application.MasterCatalogQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Adapts Reference master administration to full-page MVC forms. */
@Controller
public class MasterManagementController {

    private static final int PAGE_SIZE = 20;
    private final MasterCatalogQuery query;
    private final MasterAdministration administration;

    public MasterManagementController(
            MasterCatalogQuery query, MasterAdministration administration) {
        this.query = query;
        this.administration = administration;
    }

    @GetMapping("/master/departments")
    String departments(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        populateDepartments(model, search, page);
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new MasterCreateForm(null, null));
        }
        return "master/departments";
    }

    @PostMapping("/master/departments")
    String createDepartment(
            @Valid @ModelAttribute("createForm") MasterCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.valid()) {
            populateDepartments(model, "", 0);
            return "master/departments";
        }
        administration.createDepartment(form.requiredCode(), form.requiredName());
        redirectAttributes.addFlashAttribute("notice", "部門を登録しました。");
        return "redirect:/master/departments";
    }

    @PostMapping("/master/departments/{departmentId}/rename")
    String renameDepartment(
            @PathVariable UUID departmentId,
            @Valid @ModelAttribute MasterRenameForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.valid()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "master/invalid-form";
        }
        administration.renameDepartment(departmentId, form.requiredName(), form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "部門名を更新しました。");
        return "redirect:/master/departments";
    }

    @PostMapping("/master/departments/{departmentId}/deactivate")
    String deactivateDepartment(
            @PathVariable UUID departmentId,
            @Valid @ModelAttribute MasterVersionForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.valid()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "master/invalid-form";
        }
        administration.deactivateDepartment(departmentId, form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "部門を廃止しました。");
        return "redirect:/master/departments";
    }

    @GetMapping("/master/expense-categories")
    String expenseCategories(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        populateCategories(model, search, page);
        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new MasterCreateForm(null, null));
        }
        return "master/expense-categories";
    }

    @PostMapping("/master/expense-categories")
    String createExpenseCategory(
            @Valid @ModelAttribute("createForm") MasterCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.valid()) {
            populateCategories(model, "", 0);
            return "master/expense-categories";
        }
        administration.createExpenseCategory(form.requiredCode(), form.requiredName());
        redirectAttributes.addFlashAttribute("notice", "経費科目を登録しました。");
        return "redirect:/master/expense-categories";
    }

    @PostMapping("/master/expense-categories/{categoryId}/rename")
    String renameExpenseCategory(
            @PathVariable UUID categoryId,
            @Valid @ModelAttribute MasterRenameForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.valid()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "master/invalid-form";
        }
        administration.renameExpenseCategory(categoryId, form.requiredName(), form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費科目名を更新しました。");
        return "redirect:/master/expense-categories";
    }

    @PostMapping("/master/expense-categories/{categoryId}/deactivate")
    String deactivateExpenseCategory(
            @PathVariable UUID categoryId,
            @Valid @ModelAttribute MasterVersionForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !form.valid()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "master/invalid-form";
        }
        administration.deactivateExpenseCategory(categoryId, form.requiredVersion());
        redirectAttributes.addFlashAttribute("notice", "経費科目を廃止しました。");
        return "redirect:/master/expense-categories";
    }

    private void populateDepartments(Model model, String search, int page) {
        model.addAttribute("result", query.findDepartments(search, page, PAGE_SIZE));
        model.addAttribute("search", search);
    }

    private void populateCategories(Model model, String search, int page) {
        model.addAttribute("result", query.findExpenseCategories(search, page, PAGE_SIZE));
        model.addAttribute("search", search);
    }
}
