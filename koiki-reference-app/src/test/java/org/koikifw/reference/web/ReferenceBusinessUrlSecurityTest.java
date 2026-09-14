package org.koikifw.referenceacceptance.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.HomeController;
import org.koikifw.reference.expense.adapter.inbound.web.ExpenseWebController;
import org.koikifw.reference.expense.adapter.inbound.web.ExpenseWebExceptionHandler;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseReadService;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestLineView;
import org.koikifw.reference.expense.application.query.ExpenseRequestPage;
import org.koikifw.reference.expense.application.query.ExpenseSelectionOption;
import org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration;
import org.koikifw.reference.master.adapter.inbound.web.MasterManagementController;
import org.koikifw.reference.master.adapter.inbound.web.MasterWebExceptionHandler;
import org.koikifw.reference.master.application.MasterAdministration;
import org.koikifw.reference.master.application.MasterCatalogQuery;
import org.koikifw.reference.master.application.MasterFailure;
import org.koikifw.reference.master.application.MasterOperationException;
import org.koikifw.reference.master.application.dto.DepartmentPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({HomeController.class, MasterManagementController.class, ExpenseWebController.class})
@ContextConfiguration(classes = org.koikifw.reference.ReferenceApplication.class)
@Import({
        ReferenceSecurityConfiguration.class,
        MasterWebExceptionHandler.class,
        ExpenseWebExceptionHandler.class,
        ReferenceBusinessUrlSecurityTest.SessionTestConfiguration.class
})
class ReferenceBusinessUrlSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterCatalogQuery masterQuery;

    @MockitoBean
    private MasterAdministration masterAdministration;

    @MockitoBean
    private ExpenseReadService expenseReads;

    @MockitoBean
    private ExpenseApplicationService expenseCommands;

    @MockitoBean(name = "referenceCompromisedPasswordChecker")
    private CompromisedPasswordChecker compromisedPasswordChecker;

    @Test
    void redirectsUnauthenticatedBusinessRequestToLogin() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void preservesTheFormLoginRedirectForUnauthenticatedHtmxRequests() throws Exception {
        mockMvc.perform(get("/master/departments")
                        .header("HX-Request", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void servesSharedCssFromWebMvcStarterWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/koiki-web/koiki.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/css")))
                .andExpect(content().string(containsString(":root")));
    }

    @Test
    void servesHtmxIntegrationResourcesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/koiki-web/koiki-htmx.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("text/javascript")))
                .andExpect(content().string(containsString("htmx:configRequest")))
                .andExpect(content().string(containsString("htmx:beforeSwap")))
                .andExpect(content().string(containsString("responseUrl.endsWith(\"/login\")")))
                .andExpect(content().string(containsString("koiki:htmx:afterSwap")));

        mockMvc.perform(get("/webjars/htmx.org/2.0.10/dist/htmx.min.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("htmx")));
    }

    @Test
    void rejectsMasterRequestWithoutMasterPermission() throws Exception {
        mockMvc.perform(get("/master/departments").with(user("applicant")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(masterQuery);
    }

    @Test
    void rendersFullMasterPageWithSharedResourceAndCsrf() throws Exception {
        when(masterQuery.findDepartments("", 0, 20))
                .thenReturn(new DepartmentPage(List.of(), 0, 0, 20));

        mockMvc.perform(get("/master/departments")
                        .with(user("master").authorities(() -> "MASTER:ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ja\"")))
                .andExpect(content().string(containsString("/koiki-web/koiki.css")))
                .andExpect(content().string(containsString("/webjars/htmx.org/2.0.10/dist/htmx.min.js")))
                .andExpect(content().string(containsString("/koiki-web/koiki-htmx.js")))
                .andExpect(content().string(containsString("name=\"_csrf_header\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("hx-trigger=\"input changed delay:350ms, submit\"")))
                .andExpect(content().string(containsString("hx-push-url=\"true\"")))
                .andExpect(content().string(containsString("hx-indicator=\"#department-loading\"")))
                .andExpect(content().string(containsString("部門管理")));
    }

    @Test
    void rendersOnlyDepartmentResultsForHtmxSearchAndPaging() throws Exception {
        when(masterQuery.findDepartments("SALES", 1, 20))
                .thenReturn(new DepartmentPage(List.of(), 21, 1, 20));

        mockMvc.perform(get("/master/departments")
                        .header("HX-Request", "true")
                        .param("search", "SALES")
                        .param("page", "1")
                        .with(user("master").authorities(() -> "MASTER:ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"department-results\"")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")))
                .andExpect(content().string(containsString("hx-push-url=\"true\"")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("部門登録"))));

        verify(masterQuery).findDepartments("SALES", 1, 20);
    }

    @Test
    void rejectsMasterMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/master/departments")
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .param("code", "SALES")
                        .param("name", "営業部"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(masterAdministration);
    }

    @Test
    void rejectsHtmxMasterMutationWithoutCsrfUsingSanitizedPartialError() throws Exception {
        mockMvc.perform(post("/master/departments")
                        .header("HX-Request", "true")
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .param("code", "SALES")
                        .param("name", "営業部"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(containsString("この操作は許可されていません。")))
                .andExpect(content().string(not(containsString("InvalidCsrfTokenException"))));
        verifyNoInteractions(masterAdministration);
    }

    @Test
    void validatesMasterFormBeforeCallingUseCase() throws Exception {
        when(masterQuery.findDepartments("", 0, 20))
                .thenReturn(new DepartmentPage(List.of(), 0, 0, 20));

        mockMvc.perform(post("/master/departments")
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .with(csrf())
                        .param("code", "invalid code")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("must match")))
                .andExpect(content().string(containsString("must not be blank")));
        verifyNoInteractions(masterAdministration);
    }

    @Test
    void rendersOnlyCreateFragmentForHtmxValidationFailure() throws Exception {
        mockMvc.perform(post("/master/departments")
                        .header("HX-Request", "true")
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .with(csrf())
                        .param("code", "invalid code")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"department-create\"")))
                .andExpect(content().string(containsString("must match")))
                .andExpect(content().string(containsString("must not be blank")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("department-results"))));
        verifyNoInteractions(masterAdministration);
    }

    @Test
    void redirectsSuccessfulHtmxCreateWithoutRenderingAFullPage() throws Exception {
        mockMvc.perform(post("/master/departments")
                        .header("HX-Request", "true")
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .with(csrf())
                        .param("code", "SALES")
                        .param("name", "営業部"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("HX-Redirect", "/master/departments"))
                .andExpect(content().string(""));

        verify(masterAdministration).createDepartment("SALES", "営業部");
    }

    @Test
    void rendersSanitizedHtmxNotFoundAndUnexpectedErrorsAsFragments() throws Exception {
        UUID departmentId = UUID.randomUUID();
        doThrow(new MasterOperationException(MasterFailure.NOT_FOUND))
                .when(masterAdministration)
                .renameDepartment(eq(departmentId), eq("更新名"), eq(1L));

        mockMvc.perform(post("/master/departments/{id}/rename", departmentId)
                        .header("HX-Request", "true")
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .with(csrf())
                        .param("name", "更新名")
                        .param("expectedVersion", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("data-koiki-focus")))
                .andExpect(content().string(containsString("対象のマスターは存在しません。")))
                .andExpect(content().string(not(containsString(departmentId.toString()))))
                .andExpect(content().string(not(containsString("<html"))));

        when(masterQuery.findDepartments("FAIL", 0, 20))
                .thenThrow(new IllegalStateException("sensitive dependency detail"));
        mockMvc.perform(get("/master/departments")
                        .header("HX-Request", "true")
                        .param("search", "FAIL")
                        .with(user("master").authorities(() -> "MASTER:ADMIN")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("予期しないエラーが発生しました。")))
                .andExpect(content().string(not(containsString("sensitive dependency detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void rendersExpenseFormAndInvokesExistingCreateUseCase() throws Exception {
        UUID departmentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(expenseReads.findAvailableDepartments()).thenReturn(List.of(
                new ExpenseSelectionOption(departmentId, "SALES", "営業部")));
        when(expenseReads.findAvailableExpenseCategories()).thenReturn(List.of(
                new ExpenseSelectionOption(categoryId, "TRAVEL", "旅費交通費")));
        when(expenseCommands.createDraft(
                        org.mockito.ArgumentMatchers.eq(departmentId),
                        org.mockito.ArgumentMatchers.eq(1000L),
                        org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(requestId);

        mockMvc.perform(get("/expenses/new").with(user("applicant")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("経費申請作成")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        mockMvc.perform(post("/expenses")
                        .with(user("applicant"))
                        .with(csrf())
                        .param("departmentId", departmentId.toString())
                        .param("claimedAmount", "1000")
                        .param("expenseCategoryId", categoryId.toString())
                        .param("usageDate", java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString())
                        .param("description", "顧客訪問")
                        .param("purpose", "商談")
                        .param("lineAmount", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/expenses/" + requestId));

        verify(expenseCommands).createDraft(
                org.mockito.ArgumentMatchers.eq(departmentId),
                org.mockito.ArgumentMatchers.eq(1000L),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void permitsAuthenticatedExpenseListAndDoesNotRequireBusinessPermissionAtUrlLayer()
            throws Exception {
        when(expenseReads.findOwnRequests(0, 20))
                .thenReturn(new ExpenseRequestPage(List.of(), 0, 0, 20));

        mockMvc.perform(get("/expenses").with(user("authenticated")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("自分の経費申請")));
    }

    @Test
    void rendersScopedExpenseDetailWithActionsAndCsrf() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ExpenseRequestDetail detail = new ExpenseRequestDetail(
                requestId,
                applicantId,
                "applicant@example.test",
                departmentId,
                "SALES",
                "営業部",
                1000L,
                "DRAFT",
                null,
                3L,
                Instant.parse("2026-09-13T00:00:00Z"),
                List.of(new ExpenseRequestLineView(
                        UUID.randomUUID(),
                        categoryId,
                        "TRAVEL",
                        "旅費交通費",
                        LocalDate.of(2026, 9, 13),
                        "顧客訪問",
                        "商談",
                        1000L)));
        when(expenseReads.findOwnRequest(requestId)).thenReturn(java.util.Optional.of(detail));

        mockMvc.perform(get("/expenses/{id}", requestId).with(user("applicant")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("applicant@example.test")))
                .andExpect(content().string(containsString("SALES 営業部")))
                .andExpect(content().string(containsString("TRAVEL 旅費交通費")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("提出")));
    }

    @Test
    void rendersSanitizedNotFoundPageForMissingScopedExpense() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(expenseReads.findOwnRequest(requestId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/expenses/{id}", requestId).with(user("applicant")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("申請が存在しないか、閲覧・操作できません。")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(requestId.toString()))));
    }

    @Test
    void returnsBadRequestForInvalidInlineMasterMutation() throws Exception {
        mockMvc.perform(post("/master/departments/{id}/rename", UUID.randomUUID())
                        .with(user("master").authorities(() -> "MASTER:ADMIN"))
                        .with(csrf())
                        .param("name", "")
                        .param("expectedVersion", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("入力内容を確認してください")));
        verifyNoInteractions(masterAdministration);
    }

    static class SessionTestConfiguration {

        @Bean
        Customizer<HttpSecurity> koikiSessionLogoutCustomizer() {
            return http -> { };
        }
    }
}
