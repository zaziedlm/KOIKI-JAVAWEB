package org.koikifw.reference.expense.adapter.inbound.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.ReferenceApplication;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseFailure;
import org.koikifw.reference.expense.application.ExpenseOperationException;
import org.koikifw.reference.expense.application.ExpenseReadService;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestLineView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.koikifw.starter.api.internal.KoikiApiAutoConfiguration;

@WebMvcTest(ExpenseApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = ReferenceApplication.class)
@ImportAutoConfiguration(KoikiApiAutoConfiguration.class)
@Import({ExpenseApiMapper.class, ExpenseApiExceptionHandler.class})
class ExpenseApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseReadService reads;

    @MockitoBean
    private ExpenseApplicationService commands;

    @Test
    void returnsApplicantSafeScopedDetailForCanonicalVersion() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        when(reads.findOwnRequest(requestId)).thenReturn(Optional.of(new ExpenseRequestDetail(
                requestId,
                applicantId,
                "must-not-leak@example.test",
                departmentId,
                "SALES",
                "Sales",
                1200,
                "DRAFT",
                null,
                3,
                Instant.parse("2026-09-15T01:23:45Z"),
                List.of(new ExpenseRequestLineView(
                        lineId,
                        categoryId,
                        "TRAVEL",
                        "Travel",
                        LocalDate.of(2026, 9, 15),
                        "train fare",
                        "customer visit",
                        1200)))));

        mockMvc.perform(get("/api/v1/expense-requests/{id}", requestId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.expenseRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.department.departmentId").value(departmentId.toString()))
                .andExpect(jsonPath("$.department.code").value("SALES"))
                .andExpect(jsonPath("$.decisionReason").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.lines[0].expenseLineId").value(lineId.toString()))
                .andExpect(content().string(not(containsString(applicantId.toString()))))
                .andExpect(content().string(not(containsString("must-not-leak@example.test"))));
    }

    @Test
    void createsDraftWithServerGeneratedLineIdAndLocation() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(commands.createDraft(eq(departmentId), eq(1200L), anyList())).thenReturn(requestId);

        mockMvc.perform(post("/api/v1/expense-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": "%s",
                                  "claimedAmount": 1200,
                                  "lines": [{
                                    "expenseCategoryId": "%s",
                                    "usageDate": "2026-09-15",
                                    "description": "train fare",
                                    "purpose": "customer visit",
                                    "amount": 1200
                                  }]
                                }
                                """.formatted(departmentId, categoryId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/expense-requests/" + requestId))
                .andExpect(jsonPath("$.expenseRequestId").value(requestId.toString()));

        var lines = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(commands).createDraft(eq(departmentId), eq(1200L), lines.capture());
        Object input = lines.getValue().getFirst();
        org.assertj.core.api.Assertions.assertThat(input)
                .isInstanceOf(org.koikifw.reference.expense.application.ExpenseLineInput.class);
        var line = (org.koikifw.reference.expense.application.ExpenseLineInput) input;
        org.assertj.core.api.Assertions.assertThat(line.expenseLineId()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(line.expenseCategoryId()).isEqualTo(categoryId);
    }

    @Test
    void submitsExpectedVersionWithoutResponseBody() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/expense-requests/{id}/submit", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":3}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(commands).submit(requestId, 3L);
    }

    @Test
    void rejectsInvalidRepresentationBeforeCallingUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/expense-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("KOIKI-VALIDATION-001"));

        mockMvc.perform(post("/api/v1/expense-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KOIKI-JSON-001"));

        verifyNoInteractions(commands);
    }

    @Test
    void rejectsUnsupportedAndNonCanonicalVersions() throws Exception {
        mockMvc.perform(get("/api/v2/expense-requests/{id}", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("KOIKI-HTTP-400"));

        mockMvc.perform(get("/api/1/expense-requests/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/expense-requests/{id}/unknown", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void mapsScopedNotFoundAndKnownBusinessFailuresToStableCodes() throws Exception {
        UUID missing = UUID.randomUUID();
        when(reads.findOwnRequest(missing)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/expense-requests/{id}", missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-003"))
                .andExpect(jsonPath("$.detail").value("Expense request was not found."));

        UUID stale = UUID.randomUUID();
        ExpenseOperationException conflict = mock(ExpenseOperationException.class);
        when(conflict.failure()).thenReturn(ExpenseFailure.CONCURRENT_MODIFICATION);
        doThrow(conflict).when(commands).submit(stale, 1L);

        mockMvc.perform(post("/api/v1/expense-requests/{id}/submit", stale)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-005"))
                .andExpect(content().string(not(containsString("ExpenseOperationException"))));
    }
}
