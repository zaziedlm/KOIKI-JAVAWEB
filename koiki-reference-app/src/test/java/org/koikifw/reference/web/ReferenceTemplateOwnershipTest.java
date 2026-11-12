package org.koikifw.referenceacceptance.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.HomeController;
import org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration;
import org.koikifw.starter.api.internal.KoikiApiAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The Web MVC Starter ships only the shared head; the Reference owns its navigation header. */
@WebMvcTest(HomeController.class)
@ContextConfiguration(classes = org.koikifw.reference.ReferenceApplication.class)
@ImportAutoConfiguration(KoikiApiAutoConfiguration.class)
@Import({ReferenceSecurityConfiguration.class, ReferenceTemplateOwnershipTest.SessionTestConfiguration.class})
class ReferenceTemplateOwnershipTest {

    private static final List<String> REFERENCE_ROUTES = List.of(
            "/expenses", "/master/departments", "/master/expense-categories", "/identity/users");

    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "referenceCompromisedPasswordChecker")
    private CompromisedPasswordChecker compromisedPasswordChecker;

    @Test
    void starterFragmentsProvideOnlyTheSharedHead() throws IOException {
        Resource[] starter = resources.getResources("classpath*:templates/koiki/fragments.html");

        assertThat(starter).hasSize(1);
        String content = read(starter[0]);
        assertThat(content).contains("th:fragment=\"head(title)\"");
        assertThat(content).doesNotContain("th:fragment=\"header\"").doesNotContain("KOIKI Reference");
        REFERENCE_ROUTES.forEach(route -> assertThat(content).doesNotContain("@{" + route + "}"));
    }

    @Test
    void referenceTemplatesUseTheReferenceOwnedHeader() throws IOException {
        Resource[] templates = resources.getResources("classpath*:templates/**/*.html");
        int referenceHeaders = 0;

        for (Resource template : templates) {
            String content = read(template);
            assertThat(content).as(template.getDescription()).doesNotContain("koiki/fragments :: header");
            if (content.contains("reference/fragments :: header")) {
                referenceHeaders++;
            }
        }
        assertThat(referenceHeaders).isEqualTo(13);
    }

    @Test
    void rendersTheReferenceHeaderOnTheHomePage() throws Exception {
        mockMvc.perform(get("/").with(user("authenticated")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<strong>KOIKI Reference</strong>"),
                        containsString("aria-label=\"主要メニュー\""),
                        containsString("href=\"/expenses\""),
                        containsString("href=\"/identity/users\""),
                        containsString("/koiki-web/koiki.css"))));
    }

    private static String read(Resource resource) throws IOException {
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    static class SessionTestConfiguration {

        @Bean
        Customizer<HttpSecurity> koikiSessionLogoutCustomizer() {
            return http -> { };
        }
    }
}
