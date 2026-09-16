package org.koikifw.reference.expense.adapter.inbound.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
    "koiki.identity.local-authentication.enabled=false",
    "spring.session.jdbc.initialize-schema=never",
    "koiki.reference.api.bearer.enabled=true",
    "koiki.reference.api.bearer.issuer=https://issuer.example.test",
    "koiki.reference.api.bearer.audience=koiki-reference-api"
})
@AutoConfigureMockMvc
@Import(ReferencePostgreSqlTestConfiguration.class)
class ExpenseApiPostgreSqlIntegrationTest {

    private static final UUID APPLICANT_ID = id(1);
    private static final UUID OUTSIDER_ID = id(2);
    private static final UUID DEPARTMENT_ID = id(11);
    private static final UUID CATEGORY_ID = id(12);
    private static final UUID ROLE_ID = id(21);
    private static final UUID PERMISSION_ID = id(22);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void prepare() {
        cleanData();
        insertIdentity(APPLICANT_ID, "applicant@example.test", "ACTIVE");
        insertIdentity(OUTSIDER_ID, "outsider@example.test", "ACTIVE");
        jdbc.sql("insert into koiki_role(role_id, role_code) values (:id, 'EXPENSE_APPLICANT')")
                .param("id", ROLE_ID)
                .update();
        jdbc.sql("""
                        insert into koiki_permission(permission_id, permission_code)
                        values (:id, 'EXPENSE:APPLY')
                        """)
                .param("id", PERMISSION_ID)
                .update();
        jdbc.sql("""
                        insert into koiki_role_permission(role_id, permission_id)
                        values (:roleId, :permissionId)
                        """)
                .param("roleId", ROLE_ID)
                .param("permissionId", PERMISSION_ID)
                .update();
        assignRole(APPLICANT_ID);
        assignRole(OUTSIDER_ID);
        jdbc.sql("""
                        insert into kkref_department
                            (department_id, department_code, department_name,
                             active, version, created_at, updated_at)
                        values (:id, 'FINANCE', 'Finance', true, 0, now(), now())
                        """)
                .param("id", DEPARTMENT_ID)
                .update();
        jdbc.sql("""
                        insert into kkref_expense_category
                            (expense_category_id, expense_category_code, expense_category_name,
                             active, version, created_at, updated_at)
                        values (:id, 'TRAVEL', 'Travel', true, 0, now(), now())
                        """)
                .param("id", CATEGORY_ID)
                .update();
        jdbc.sql("""
                        insert into kkref_user_department_assignment
                            (user_id, department_id, version, created_at, updated_at)
                        values (:userId, :departmentId, 0, now(), now())
                        """)
                .param("userId", APPLICANT_ID)
                .param("departmentId", DEPARTMENT_ID)
                .update();

        when(jwtDecoder.decode("applicant-token")).thenReturn(jwt(APPLICANT_ID, "expense.apply"));
        when(jwtDecoder.decode("outsider-token")).thenReturn(jwt(OUTSIDER_ID, "expense.apply"));
        when(jwtDecoder.decode("no-scope-token")).thenReturn(jwt(APPLICANT_ID, "expense.read"));
        when(jwtDecoder.decode("unknown-user-token")).thenReturn(jwt(id(99), "expense.apply"));
        when(jwtDecoder.decode("invalid-token"))
                .thenThrow(new BadJwtException("invalid fixture token"));
    }

    @AfterEach
    void cleanUp() {
        cleanData();
    }

    @Test
    void createsReadsAndSubmitsThroughBearerWithDatabaseAndAuditParity() throws Exception {
        String response = mvc.perform(post("/api/v1/expense-requests")
                        .header(AUTHORIZATION, "Bearer applicant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                        "/api/v1/expense-requests/")))
                .andExpect(jsonPath("$.expenseRequestId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID requestId = UUID.fromString(
                objectMapper.readTree(response).path("expenseRequestId").asText());

        assertThat(jdbc.sql("""
                        select status || ':' || version
                          from kkref_expense_request
                         where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(String.class)
                .single())
                .isEqualTo("DRAFT:1");
        assertThat(jdbc.sql("""
                        select expense_line_id
                          from kkref_expense_line
                         where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(UUID.class)
                .single())
                .isNotNull();

        mvc.perform(get("/api/v1/expense-requests/{id}", requestId)
                        .header(AUTHORIZATION, "Bearer applicant-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.department.code").value("FINANCE"))
                .andExpect(jsonPath("$.lines[0].expenseCategoryCode").value("TRAVEL"))
                .andExpect(jsonPath("$.applicantUserId").doesNotExist())
                .andExpect(jsonPath("$.applicantEmail").doesNotExist());

        mvc.perform(post("/api/v1/expense-requests/{id}/submit", requestId)
                        .header(AUTHORIZATION, "Bearer applicant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}"))
                .andExpect(status().isNoContent());

        assertThat(jdbc.sql("""
                        select status || ':' || version
                          from kkref_expense_request
                         where expense_request_id = :id
                        """)
                .param("id", requestId)
                .query(String.class)
                .single())
                .isEqualTo("SUBMITTED:2");
        assertThat(jdbc.sql("""
                        select action
                          from koiki_audit_event
                         where event_type = 'EXPENSE_WORKFLOW'
                           and resource_id = :id
                        """)
                .param("id", requestId.toString())
                .query(String.class)
                .list())
                .containsExactly("SUBMIT_EXPENSE");
    }

    @Test
    void keepsOwnScopeAndRollsBackStaleAndUnavailableMasterFailures() throws Exception {
        UUID requestId = createDraft();

        mvc.perform(get("/api/v1/expense-requests/{id}", requestId)
                        .header(AUTHORIZATION, "Bearer outsider-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-003"));

        mvc.perform(post("/api/v1/expense-requests/{id}/submit", requestId)
                        .header(AUTHORIZATION, "Bearer applicant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/expense-requests/{id}/submit", requestId)
                        .header(AUTHORIZATION, "Bearer applicant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-005"));

        jdbc.sql("update kkref_expense_category set active = false where expense_category_id = :id")
                .param("id", CATEGORY_ID)
                .update();
        mvc.perform(post("/api/v1/expense-requests")
                        .header(AUTHORIZATION, "Bearer applicant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-002"));

        assertThat(jdbc.sql("select count(*) from kkref_expense_request")
                .query(Long.class)
                .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("""
                        select count(*)
                          from koiki_audit_event
                         where event_type = 'EXPENSE_WORKFLOW'
                        """)
                .query(Long.class)
                .single())
                .isEqualTo(1);
    }

    @Test
    void rejectsMissingInvalidCookieUnknownUserAndMissingExactScope() throws Exception {
        mvc.perform(get("/api/v1/expense-requests/{id}", id(31)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-AUTH-001"));
        mvc.perform(get("/api/v1/expense-requests/{id}", id(31))
                        .header(AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-AUTH-001"));
        mvc.perform(get("/api/v1/expense-requests/{id}", id(31))
                        .header(AUTHORIZATION, "Bearer unknown-user-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-AUTH-001"));

        MockHttpSession browserSession = new MockHttpSession();
        browserSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(
                        "browser-user",
                        "",
                        List.of(new SimpleGrantedAuthority("EXPENSE:APPLY")))));
        mvc.perform(get("/api/v1/expense-requests/{id}", id(31)).session(browserSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-AUTH-001"));

        mvc.perform(get("/api/v1/expense-requests/{id}", id(31))
                        .header(AUTHORIZATION, "Bearer no-scope-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("KOIKI-REF-AUTH-002"));
    }

    private UUID createDraft() throws Exception {
        String response = mvc.perform(post("/api/v1/expense-requests")
                        .header(AUTHORIZATION, "Bearer applicant-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(response)
                .path("expenseRequestId")
                .asText());
    }

    private String validCreateBody() {
        return """
                {
                  "departmentId": "%s",
                  "claimedAmount": 1200,
                  "lines": [{
                    "expenseCategoryId": "%s",
                    "usageDate": "2026-09-12",
                    "description": "Train",
                    "purpose": "Customer visit",
                    "amount": 1200
                  }]
                }
                """.formatted(DEPARTMENT_ID, CATEGORY_ID);
    }

    private void insertIdentity(UUID userId, String email, String status) {
        jdbc.sql("""
                        insert into koiki_user(user_id, email, canonical_email, status, version)
                        values (:userId, :email, :email, :status, 0)
                        """)
                .param("userId", userId)
                .param("email", email)
                .param("status", status)
                .update();
    }

    private void assignRole(UUID userId) {
        jdbc.sql("insert into koiki_user_role(user_id, role_id) values (:userId, :roleId)")
                .param("userId", userId)
                .param("roleId", ROLE_ID)
                .update();
    }

    private void cleanData() {
        jdbc.sql("delete from koiki_audit_event where event_type = 'EXPENSE_WORKFLOW'").update();
        jdbc.sql("delete from kkref_expense_line").update();
        jdbc.sql("delete from kkref_expense_request").update();
        jdbc.sql("delete from kkref_user_department_assignment").update();
        jdbc.sql("delete from kkref_expense_category").update();
        jdbc.sql("delete from kkref_department").update();
        jdbc.sql("delete from koiki_role_permission where role_id = :id")
                .param("id", ROLE_ID)
                .update();
        jdbc.sql("delete from koiki_user_role where role_id = :id")
                .param("id", ROLE_ID)
                .update();
        jdbc.sql("delete from koiki_permission where permission_id = :id")
                .param("id", PERMISSION_ID)
                .update();
        jdbc.sql("delete from koiki_role where role_id = :id")
                .param("id", ROLE_ID)
                .update();
        jdbc.sql("delete from koiki_user where user_id in (:applicant, :outsider)")
                .param("applicant", APPLICANT_ID)
                .param("outsider", OUTSIDER_ID)
                .update();
    }

    private static Jwt jwt(UUID userId, String scope) {
        Instant now = Instant.now();
        return new Jwt(
                "redacted-fixture-token",
                now.minusSeconds(1),
                now.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", "https://issuer.example.test",
                        "sub", "opaque-subject",
                        "aud", List.of("koiki-reference-api"),
                        "token_use", "access",
                        "scope", scope,
                        "koiki_user_id", userId.toString()));
    }

    private static UUID id(long suffix) {
        return new UUID(0x3500000000000000L, suffix);
    }
}
