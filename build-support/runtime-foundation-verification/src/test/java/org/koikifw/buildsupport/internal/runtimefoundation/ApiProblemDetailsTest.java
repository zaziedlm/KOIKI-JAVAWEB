package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = ApiProblemDetailsTest.TestApplication.class)
class ApiProblemDetailsTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void createClient() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void returnsProblemDetailsForRequestValidation() throws Exception {
        mockMvc.perform(post("/api/1/problem-fixture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed."))
                .andExpect(jsonPath("$.instance").value("/api/1/problem-fixture"))
                .andExpect(jsonPath("$.code").value("KOIKI-VALIDATION-001"))
                .andExpect(jsonPath("$.violations[0].field").value("label"))
                .andExpect(jsonPath("$.violations[0].message").isString());
    }

    @Test
    void mapsMalformedAndStrictJacksonInputWithoutLeakingParserDetails() throws Exception {
        mockMvc.perform(post("/api/1/problem-fixture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("KOIKI-JSON-001"))
                .andExpect(jsonPath("$.detail").value("Request body is not valid JSON."))
                .andExpect(content().string(not(containsString("Unexpected end-of-input"))));

        mockMvc.perform(post("/api/1/problem-fixture")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"accepted\",\"internalProbe\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("KOIKI-JSON-001"))
                .andExpect(content().string(not(containsString("internalProbe"))));
    }

    @Test
    void mapsDirectJacksonAndUnexpectedExceptionsWithoutInternalInformation() throws Exception {
        mockMvc.perform(get("/api/1/problem-fixture/jackson"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("KOIKI-JSON-002"))
                .andExpect(jsonPath("$.detail").value("JSON processing failed."))
                .andExpect(content().string(not(containsString("Unexpected end-of-input"))));

        mockMvc.perform(get("/api/1/problem-fixture/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("KOIKI-INTERNAL-001"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("fixture-secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void preservesApplicationOwnedSpringErrorResponseCode() throws Exception {
        mockMvc.perform(get("/api/1/problem-fixture/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FIXTURE-CONFLICT-001"))
                .andExpect(jsonPath("$.detail").value("The fixture request conflicts with current state."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/missing", "/wrapped-missing"})
    void preservesAnnotatedHttpStatusWithoutSecurity(String path) throws Exception {
        assertFalse(ClassUtils.isPresent(
                "org.springframework.security.core.AuthenticationException", getClass().getClassLoader()));
        mockMvc.perform(get("/api/1/problem-fixture" + path))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("fixture-private-detail"))));
    }

    @Test
    void preservesCustomErrorResponseStatusHeadersAndBody() throws Exception {
        mockMvc.perform(get("/api/1/problem-fixture/throttled"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "9"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("KOIKI-HTTP-429"))
                .andExpect(jsonPath("$.detail").value("Application capacity is exhausted."))
                .andExpect(content().string(not(containsString("fixture-private-detail"))));
    }

    @Test
    void preservesWrappedResponseStatusException() throws Exception {
        mockMvc.perform(get("/api/1/problem-fixture/wrapped-status"))
                .andExpect(status().isGone())
                .andExpect(content().string(not(containsString("fixture-private-detail"))));
    }

    record FixtureRequest(@NotBlank String label) {
    }

    @RestController
    @RequestMapping("/api/{version}/problem-fixture")
    static class FixtureController {

        private final JsonMapper jsonMapper;

        FixtureController(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
        }

        @PostMapping(version = "1")
        void accept(@Valid @RequestBody FixtureRequest request) {
        }

        @GetMapping(path = "/jackson", version = "1")
        void failWithJackson() {
            jsonMapper.readTree("{");
        }

        @GetMapping(path = "/unexpected", version = "1")
        void failUnexpectedly() {
            throw new IllegalStateException("fixture-secret");
        }

        @GetMapping(path = "/conflict", version = "1")
        void conflict() {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "The fixture request conflicts with current state.");
            problem.setProperty("code", "FIXTURE-CONFLICT-001");
            throw new ErrorResponseException(HttpStatus.CONFLICT, problem, null);
        }

        @GetMapping(path = "/missing", version = "1")
        void missing() {
            throw new MissingException();
        }

        @GetMapping(path = "/wrapped-missing", version = "1")
        void wrappedMissing() {
            throw new IllegalStateException("fixture-private-detail", new MissingException());
        }

        @GetMapping(path = "/throttled", version = "1")
        void throttled() {
            throw new CapacityException();
        }

        @GetMapping(path = "/wrapped-status", version = "1")
        void wrappedStatus() {
            throw new IllegalStateException("fixture-private-detail",
                    new ResponseStatusException(HttpStatus.GONE, "The resource is no longer available."));
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class MissingException extends RuntimeException {
        MissingException() {
            super("fixture-private-detail");
        }
    }

    static class CapacityException extends RuntimeException implements ErrorResponse {
        private final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Application capacity is exhausted.");

        CapacityException() {
            super("fixture-private-detail");
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, "9");
            return headers;
        }

        @Override
        public ProblemDetail getBody() {
            return problem;
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(FixtureController.class)
    static class TestApplication {
    }
}
