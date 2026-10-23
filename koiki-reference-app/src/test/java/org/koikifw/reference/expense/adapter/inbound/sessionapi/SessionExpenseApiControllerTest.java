package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.reference.ReferenceApplication;
import org.koikifw.reference.expense.application.ExpenseApplicationService;
import org.koikifw.reference.expense.application.ExpenseFailure;
import org.koikifw.reference.expense.application.ExpenseOperationException;
import org.koikifw.reference.expense.application.ExpenseReadService;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestLineView;
import org.koikifw.reference.expense.application.query.ExpenseRequestListItem;
import org.koikifw.reference.expense.application.query.ExpenseRequestPage;
import org.koikifw.reference.expense.application.query.ExpenseSelectionOption;
import org.koikifw.starter.api.internal.KoikiApiAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpenseSessionApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = ReferenceApplication.class)
@ImportAutoConfiguration(KoikiApiAutoConfiguration.class)
@Import({SessionExpenseApiMapper.class, SessionExpenseApiExceptionHandler.class})
class SessionExpenseApiControllerTest {
    private static final UUID ID = UUID.fromString("45000000-0000-0000-0000-000000000001");
    @Autowired private MockMvc mvc;
    @MockitoBean private ExpenseReadService reads;
    @MockitoBean private ExpenseApplicationService commands;

    @Test
    void copiesApplicantSafeDetailWithExactDecimalStrings() throws Exception {
        when(reads.findOwnRequest(ID)).thenReturn(Optional.of(new ExpenseRequestDetail(ID, UUID.randomUUID(),
                "private-email@example.test", ID, "D", "Department", Long.MAX_VALUE, "DRAFT", null,
                9007199254740993L, Instant.EPOCH, List.of(new ExpenseRequestLineView(ID, ID, "C", "Category",
                        LocalDate.of(2026, 9, 12), "Taxi", "Visit", Long.MAX_VALUE)))));
        mvc.perform(get("/spa-api/v1/expense-requests/" + ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.claimedAmount").value("9223372036854775807"))
                .andExpect(jsonPath("$.version").value("9007199254740993"))
                .andExpect(jsonPath("$.department.id").value(ID.toString()))
                .andExpect(jsonPath("$.lines[0].expenseCategory.name").value("Category"))
                .andExpect(jsonPath("$.lines[0].amount").value("9223372036854775807"))
                .andExpect(jsonPath("$.applicantUserId").doesNotExist())
                .andExpect(content().string(not(containsString("private-email"))));
    }

    @Test
    void mapsOptionsAndPageWithoutApplicantIdentity() throws Exception {
        when(reads.findAvailableDepartments()).thenReturn(List.of(new ExpenseSelectionOption(ID, "D", "Department")));
        when(reads.findAvailableExpenseCategories()).thenReturn(List.of(new ExpenseSelectionOption(ID, "C", "Category")));
        when(reads.findOwnRequests(0, 20)).thenReturn(new ExpenseRequestPage(List.of(new ExpenseRequestListItem(
                ID, ID, "private-email@example.test", ID, "D", "Department", 100, "DRAFT", 3, Instant.EPOCH)),
                9007199254740993L, 0, 20));
        mvc.perform(get("/spa-api/v1/expense-options")).andExpect(status().isOk())
                .andExpect(jsonPath("$.departments[0].name").value("Department"))
                .andExpect(jsonPath("$.expenseCategories[0].id").value(ID.toString()));
        mvc.perform(get("/spa-api/v1/expense-requests")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value("9007199254740993"))
                .andExpect(jsonPath("$.content[0].version").value("3"))
                .andExpect(jsonPath("$.content[0].claimedAmount").value("100"))
                .andExpect(content().string(not(containsString("private-email"))));
    }

    @Test
    void createsDraftWithGeneratedLineIdAndSessionLocation() throws Exception {
        when(commands.createDraft(eq(ID), eq(Long.MAX_VALUE), anyList())).thenReturn(ID);
        mvc.perform(post("/spa-api/v1/expense-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("\"9223372036854775807\"")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/spa-api/v1/expense-requests/" + ID))
                .andExpect(jsonPath("$.expenseRequestId").value(ID.toString()));
        var lines = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(commands).createDraft(eq(ID), eq(Long.MAX_VALUE), lines.capture());
        var line = (org.koikifw.reference.expense.application.ExpenseLineInput) lines.getValue().getFirst();
        assertThat(line.expenseLineId()).isNotNull();
        assertThat(line.amount()).isEqualTo(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "9007199254740993", "9223372036854775807"})
    void submitsExactVersionWithoutRounding(String version) throws Exception {
        mvc.perform(post("/spa-api/v1/expense-requests/" + ID + "/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":\"" + version + "\"}"))
                .andExpect(status().isNoContent());
        verify(commands).submit(ID, Long.parseLong(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "9007199254740993", "null", "true", "[]", "{}", "\"\"", "\"01\"",
            "\"-1\"", "\"+1\"", "\" 1\"", "\"1.0\"", "\"1e2\"", "\"9223372036854775808\""})
    void rejectsInvalidVersionTokensBeforeUseCase(String token) throws Exception {
        mvc.perform(post("/spa-api/v1/expense-requests/" + ID + "/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":" + token + "}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "null", "\"0\"", "\"-1\"", "\"01\"", "\"1.2\"",
            "\"9223372036854775808\""})
    void rejectsInvalidAmountAtDtoBoundary(String token) throws Exception {
        mvc.perform(post("/spa-api/v1/expense-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(token)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(commands);
    }

    @Test
    void rejectsUnknownFieldAndMalformedUuidWithoutUseCase() throws Exception {
        mvc.perform(post("/spa-api/v1/expense-requests/" + ID + "/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":\"1\",\"extra\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/spa-api/v1/expense-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("\"100\"").replace(ID.toString(), "1-1-1-1-1")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/spa-api/v1/expense-requests/1-1-1-1-1")).andExpect(status().isBadRequest());
        verifyNoInteractions(commands, reads);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=x"})
    void rejectsInvalidPaging(String query) throws Exception {
        mvc.perform(get("/spa-api/v1/expense-requests?" + query)).andExpect(status().isBadRequest());
        verifyNoInteractions(reads);
    }

    @Test
    void missingReadAndUnexpectedFailureHaveSafeBodies() throws Exception {
        when(reads.findOwnRequest(ID)).thenReturn(Optional.empty());
        mvc.perform(get("/spa-api/v1/expense-requests/" + ID)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-003"));
        when(reads.findOwnRequest(ID)).thenThrow(new IllegalStateException("private-detail"));
        mvc.perform(get("/spa-api/v1/expense-requests/" + ID)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("KOIKI-INTERNAL-001"))
                .andExpect(content().string(not(containsString("private-detail"))));
    }

    @ParameterizedTest
    @CsvSource({"INVALID_INPUT,422,001", "MASTER_UNAVAILABLE,422,002", "NOT_FOUND,404,003",
            "INVALID_TRANSITION,409,004", "CONCURRENT_MODIFICATION,409,005", "CONFLICT,409,006",
            "DEPENDENCY_FAILURE,503,007"})
    void preservesBusinessFailureMeanings(ExpenseFailure failure, int status, String suffix) throws Exception {
        var exception = mock(ExpenseOperationException.class);
        when(exception.failure()).thenReturn(failure);
        doThrow(exception).when(commands).submit(ID, 1);
        mvc.perform(post("/spa-api/v1/expense-requests/" + ID + "/submit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":\"1\"}"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-" + suffix));
    }

    private static String createBody(String amount) {
        return """
                {"departmentId":"%s","claimedAmount":%s,"lines":[{"expenseCategoryId":"%s",
                "usageDate":"2026-09-12","description":"Taxi","purpose":"Visit","amount":%s}]}
                """.formatted(ID, amount, ID, amount);
    }
}
