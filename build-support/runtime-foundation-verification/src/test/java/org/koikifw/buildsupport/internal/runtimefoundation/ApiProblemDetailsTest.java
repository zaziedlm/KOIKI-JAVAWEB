package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(FixtureController.class)
    static class TestApplication {
    }
}
