package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.ReferencePostgreSqlTestConfiguration;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

/** Real JDBC sessions, method security, transaction, MVC and Session API over one database. */
@SpringBootTest(properties = {"koiki.identity.local-authentication.enabled=false",
        "spring.session.jdbc.initialize-schema=never", "server.servlet.session.cookie.secure=false"})
@AutoConfigureMockMvc(print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
@Import(ReferencePostgreSqlTestConfiguration.class)
@RecordApplicationEvents
class SessionExpenseApiPostgreSqlIntegrationTest {
    private static final UUID OWNER = UUID.fromString("45000000-0000-4000-8000-000000000001");
    private static final UUID OTHER = UUID.fromString("45000000-0000-4000-8000-000000000002");
    private static final UUID DEPARTMENT = UUID.fromString("45000000-0000-4000-8000-000000000011");
    private static final UUID CATEGORY = UUID.fromString("45000000-0000-4000-8000-000000000012");
    @Autowired private MockMvc mvc;
    @Autowired private JdbcClient jdbc;
    @Autowired private SessionRepository<? extends Session> sessions;
    @Autowired private ObjectMapper json;
    @Autowired private ApplicationEvents events;
    @Autowired private CacheManager caches;
    private Cookie owner = new Cookie("SESSION", "unset");

    @BeforeEach
    void prepare() {
        clean();
        Objects.requireNonNull(caches.getCache("koiki:reference:expense-category-options")).clear();
        for (UUID user : List.of(OWNER, OTHER)) {
            jdbc.sql("""
                    insert into koiki_user(user_id,email,canonical_email,status,version)
                    values (:id,:email,:email,'ACTIVE',0)
                    """).param("id", user).param("email", user + "@example.test").update();
        }
        jdbc.sql("""
                insert into kkref_department(department_id,department_code,department_name,active,version,created_at,updated_at)
                values (:id,'B1','B1 Department',true,0,now(),now())
                """).param("id", DEPARTMENT).update();
        jdbc.sql("""
                insert into kkref_expense_category(expense_category_id,expense_category_code,expense_category_name,
                active,version,created_at,updated_at) values (:id,'B1','B1 Category',true,0,now(),now())
                """).param("id", CATEGORY).update();
        jdbc.sql("""
                insert into kkref_user_department_assignment(user_id,department_id,version,created_at,updated_at)
                values (:user,:department,0,now(),now())
                """).param("user", OWNER).param("department", DEPARTMENT).update();
        owner = session(OWNER, true);
    }

    @AfterEach
    void cleanup() { clean(); }

    @Test
    void createsListsReadsAndSubmitsWithMvcDbAndAuditParity() throws Exception {
        mvc.perform(get("/spa-api/v1/expense-options").cookie(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.departments[0].id").value(DEPARTMENT.toString()));
        UUID id = create();
        mvc.perform(get("/spa-api/v1/expense-requests").cookie(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value("1"))
                .andExpect(jsonPath("$.content[0].claimedAmount").value("100"));
        mvc.perform(get("/spa-api/v1/expense-requests/" + id).cookie(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1"));
        mvc.perform(unsafe("/spa-api/v1/expense-requests/" + id + "/submit", owner)
                        .content("{\"expectedVersion\":\"1\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/expenses/" + id).cookie(owner)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUBMITTED")));
        assertState(id, "SUBMITTED:2", 1);
        assertThat(jdbc.sql("select action from koiki_audit_event where event_type='EXPENSE_WORKFLOW'")
                .query(String.class).single()).isEqualTo("SUBMIT_EXPENSE");
    }

    @Test
    void mvcCreatesDraftUsingHiddenCsrfAndSessionApiReadsIt() throws Exception {
        var form = mvc.perform(get("/expenses/new").cookie(owner)).andExpect(status().isOk()).andReturn();
        var csrf = (CsrfToken) Objects.requireNonNull(form.getRequest().getAttribute("_csrf"));
        var result = mvc.perform(post("/expenses").cookie(owner).param("_csrf", csrf.getToken())
                        .param("departmentId", DEPARTMENT.toString()).param("claimedAmount", "100")
                        .param("expenseCategoryId", CATEGORY.toString()).param("usageDate", "2026-09-12")
                        .param("description", "Taxi").param("purpose", "Visit").param("lineAmount", "100"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String location = Objects.requireNonNull(result.getResponse().getHeader("Location"));
        UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        mvc.perform(get("/spa-api/v1/expense-requests/" + id).cookie(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedAmount").value("100"));
        assertState(id, "DRAFT:1", 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2"})
    void hidesForeignAndMissingTargetsRegardlessOfVersion(String version) throws Exception {
        UUID id = create();
        Cookie other = session(OTHER, true);
        for (UUID target : List.of(id, UUID.randomUUID())) {
            mvc.perform(unsafe("/spa-api/v1/expense-requests/" + target + "/submit", other)
                            .content("{\"expectedVersion\":\"" + version + "\"}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-003"));
            mvc.perform(get("/spa-api/v1/expense-requests/" + target).cookie(other))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-003"));
        }
        assertState(id, "DRAFT:1", 0);
    }

    @Test
    void staleVersionDoesNotUpdateOrAudit() throws Exception {
        UUID id = create();
        mvc.perform(unsafe("/spa-api/v1/expense-requests/" + id + "/submit", owner)
                        .content("{\"expectedVersion\":\"0\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-005"));
        assertState(id, "DRAFT:1", 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void masterUnavailableDoesNotUpdateOrAudit(boolean department) throws Exception {
        UUID id = create();
        jdbc.sql(department ? "update kkref_department set active=false" : "update kkref_expense_category set active=false")
                .update();
        mvc.perform(unsafe("/spa-api/v1/expense-requests/" + id + "/submit", owner)
                        .content("{\"expectedVersion\":\"1\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("KOIKI-REF-EXPENSE-002"));
        assertState(id, "DRAFT:1", 0);
    }

    @Test
    void csrfAndPermissionDenialsHaveNoBusinessSideEffects() throws Exception {
        UUID id = create();
        mvc.perform(post("/spa-api/v1/expense-requests/" + id + "/submit").cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":\"1\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("KOIKI-REF-CSRF-001"));
        Cookie noPermission = session(OWNER, false);
        mvc.perform(unsafe("/spa-api/v1/expense-requests/" + id + "/submit", noPermission)
                        .content("{\"expectedVersion\":\"1\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("KOIKI-REF-SESSION-002"));
        assertState(id, "DRAFT:1", 0);
    }

    @Test
    void logoutDeletesPersistentSessionAndOldCookieIsRejected() throws Exception {
        String rawId = new String(Base64.getDecoder().decode(owner.getValue()), StandardCharsets.UTF_8);
        assertThat(sessions.findById(rawId)).isNotNull();
        var result = mvc.perform(unsafe("/spa-api/logout", owner)).andExpect(status().isNoContent()).andReturn();
        assertThat(sessions.findById(rawId)).isNull();
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(value -> value.startsWith("SESSION=;") && value.contains("Max-Age=0"))
                .anyMatch(value -> value.startsWith("XSRF-TOKEN=;") && value.contains("Max-Age=0"));
        mvc.perform(get("/spa-api/v1/expense-requests").cookie(owner))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("KOIKI-REF-SESSION-001"));
    }

    @Test
    void expiredPersistentSessionCannotUseTheApi() throws Exception {
        // The JDBC repository reconstructs Session.isExpired from last-access + interval.
        jdbc.sql("update koiki_session set last_access_time=0, expiry_time=0").update();
        mvc.perform(get("/spa-api/v1/expense-requests").cookie(owner)).andExpect(status().isUnauthorized());
        assertThat(jdbc.sql("select count(*) from kkref_expense_request").query(Long.class).single()).isZero();
    }

    private UUID create() throws Exception {
        String response = mvc.perform(unsafe("/spa-api/v1/expense-requests", owner).content("""
                {"departmentId":"%s","claimedAmount":"100","lines":[{"expenseCategoryId":"%s",
                "usageDate":"2026-09-12","description":"Taxi","purpose":"Visit","amount":"100"}]}
                """.formatted(DEPARTMENT, CATEGORY))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).path("expenseRequestId").asText());
    }

    private MockHttpServletRequestBuilder unsafe(String path, Cookie session) throws Exception {
        var token = Objects.requireNonNull(mvc.perform(get("/spa-api/csrf").cookie(session))
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN"));
        return post(path).cookie(session, new Cookie("XSRF-TOKEN", token.getValue()))
                .header("X-XSRF-TOKEN", token.getValue()).contentType(MediaType.APPLICATION_JSON);
    }

    private Cookie session(UUID user, boolean apply) {
        return session(sessions, user, apply);
    }

    private static <S extends Session> Cookie session(SessionRepository<S> repository, UUID user, boolean apply) {
        S session = repository.createSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(new TestPrincipal(
                FrameworkUserId.parse(user.toString()), apply ? Set.of("EXPENSE:APPLY") : Set.of()), "",
                apply ? List.of(new SimpleGrantedAuthority("EXPENSE:APPLY")) : List.of());
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        repository.save(session);
        return new Cookie("SESSION", Base64.getEncoder().encodeToString(session.getId().getBytes(StandardCharsets.UTF_8)));
    }

    private void assertState(UUID id, String state, long audit) {
        assertThat(jdbc.sql("select status || ':' || version from kkref_expense_request where expense_request_id=:id")
                .param("id", id).query(String.class).single()).isEqualTo(state);
        assertThat(jdbc.sql("select count(*) from kkref_expense_line where expense_request_id=:id")
                .param("id", id).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from koiki_audit_event where event_type='EXPENSE_WORKFLOW'")
                .query(Long.class).single()).isEqualTo(audit);
        assertThat(events.stream(ExpenseApprovedEvent.class).count()).isZero();
    }

    private void clean() {
        jdbc.sql("delete from koiki_session").update();
        jdbc.sql("delete from koiki_audit_event where event_type='EXPENSE_WORKFLOW'").update();
        jdbc.sql("delete from kkref_expense_line").update();
        jdbc.sql("delete from kkref_expense_request").update();
        jdbc.sql("delete from kkref_user_department_assignment").update();
        jdbc.sql("delete from kkref_expense_category").update();
        jdbc.sql("delete from kkref_department").update();
        jdbc.sql("delete from koiki_user where user_id in (:owner,:other)").param("owner", OWNER).param("other", OTHER).update();
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions)
            implements FrameworkPrincipal, Principal, Serializable {
        @Override public AuthenticationSource authenticationSource() { return AuthenticationSource.LOCAL; }
        @Override public String getName() { return userId.toString(); }
    }
}
