package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.mock.web.MockHttpSession;

class LocalSessionAuthorizationTest {

    private static final String PERMISSION_READ = "fixture:read";
    private static final String VALID_PASSWORD = "fixture-local-passphrase";
    private static final String READER_EMAIL = "reader@fixture.invalid";
    private static final String NO_PERMISSION_EMAIL = "limited@fixture.invalid";
    private static final String DISABLED_EMAIL = "disabled@fixture.invalid";
    private static final String LOCKED_EMAIL = "locked@fixture.invalid";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KoikiSecurityAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class))
            .withUserConfiguration(LocalSessionFixtureConfiguration.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canonicalEmailAndCorrectPasswordEstablishSessionWithFixationProtection() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);
            MockHttpSession initialSession = new MockHttpSession(context.getServletContext());
            String initialSessionId = initialSession.getId();

            MvcResult login = mvc.perform(post("/login")
                            .session(initialSession)
                            .with(csrf())
                            .param("email", "  READER@FIXTURE.INVALID  ")
                            .param("password", VALID_PASSWORD))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andReturn();

            HttpSession authenticatedSession = login.getRequest().getSession(false);
            assertThat(authenticatedSession).isNotNull();
            assertThat(authenticatedSession.getId()).isNotEqualTo(initialSessionId);

            mvc.perform(get("/fixture/local/read").session((MockHttpSession) authenticatedSession))
                    .andExpect(status().isOk())
                    .andExpect(content().string("fixture-protected"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        });
    }

    @Test
    void loginRequiresCsrfAndKeepsSecurityHeadersEnabled() {
        runner.run(context -> mockMvc(context)
                .perform(post("/login")
                        .param("email", READER_EMAIL)
                        .param("password", VALID_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY")));
    }

    @Test
    void unknownDisabledLockedAndBadPasswordHaveOneExternalFailureShape() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);

            LoginFailure expected = failedLogin(mvc, "unknown@fixture.invalid", VALID_PASSWORD);
            assertThat(failedLogin(mvc, DISABLED_EMAIL, VALID_PASSWORD)).isEqualTo(expected);
            assertThat(failedLogin(mvc, LOCKED_EMAIL, VALID_PASSWORD)).isEqualTo(expected);
            assertThat(failedLogin(mvc, READER_EMAIL, "wrong-fixture-passphrase")).isEqualTo(expected);
            assertThat(expected.location()).isEqualTo("/login?error");
            assertThat(expected.body()).doesNotContain("unknown", "disabled", "locked", VALID_PASSWORD);
        });
    }

    @Test
    void anonymousAndInsufficientPermissionCannotReachProtectedOperation() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);
            ProtectedFixtureService service = context.getBean(ProtectedFixtureService.class);

            mvc.perform(get("/fixture/local/read"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));

            MockHttpSession limitedSession = login(mvc, NO_PERMISSION_EMAIL);
            int invocationCount = service.invocationCount();
            mvc.perform(get("/fixture/local/read").session(limitedSession))
                    .andExpect(status().isForbidden());
            assertThat(service.invocationCount()).isEqualTo(invocationCount);
        });
    }

    @Test
    void methodSecurityRejectsControllerBypassAndOnlyExactPermissionAllowsInvocation() {
        runner.run(context -> {
            ProtectedFixtureService service = context.getBean(ProtectedFixtureService.class);
            int invocationCount = service.invocationCount();

            assertThatThrownBy(service::read)
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
            assertThat(service.invocationCount()).isEqualTo(invocationCount);

            authenticate("fixture-user", "FIXTURE:READ");
            assertThatThrownBy(service::read).isInstanceOf(AccessDeniedException.class);
            assertThat(service.invocationCount()).isEqualTo(invocationCount);

            authenticate("fixture-user", "fixture:read:extra");
            assertThatThrownBy(service::read).isInstanceOf(AccessDeniedException.class);
            assertThat(service.invocationCount()).isEqualTo(invocationCount);

            authenticate("fixture-user", "fixture:unknown");
            assertThatThrownBy(service::read).isInstanceOf(AccessDeniedException.class);
            assertThat(service.invocationCount()).isEqualTo(invocationCount);

            authenticate("fixture-user", PERMISSION_READ);
            assertThat(service.read()).isEqualTo("fixture-protected");
            assertThat(service.invocationCount()).isEqualTo(invocationCount + 1);
        });
    }

    @Test
    void duplicateCanonicalEmailFailsFixtureConstruction() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        List<FixtureIdentity> duplicates = List.of(
                new FixtureIdentity("user@fixture.invalid", encoder.encode(VALID_PASSWORD), true, true, Set.of()),
                new FixtureIdentity(" USER@FIXTURE.INVALID ", encoder.encode(VALID_PASSWORD), true, true, Set.of()));

        assertThatThrownBy(() -> new FixtureIdentityStore(duplicates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate canonical fixture identity");
    }

    private static MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static MockHttpSession login(MockMvc mvc, String email) throws Exception {
        MvcResult result = mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", VALID_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static LoginFailure failedLogin(MockMvc mvc, String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return new LoginFailure(
                result.getResponse().getStatus(),
                result.getResponse().getHeader(HttpHeaders.LOCATION),
                result.getResponse().getContentAsString(),
                result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
    }

    private static void authenticate(String principal, String authority) {
        Authentication authentication = new TestingAuthenticationToken(principal, "not-a-credential", authority);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private record LoginFailure(int status, String location, String body, Collection<String> cookies) {}

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class LocalSessionFixtureConfiguration {

        @Bean
        PasswordEncoder fixturePasswordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        FixtureIdentityStore fixtureIdentityStore(PasswordEncoder encoder) {
            String encodedPassword = encoder.encode(VALID_PASSWORD);
            return new FixtureIdentityStore(List.of(
                    new FixtureIdentity(READER_EMAIL, encodedPassword, true, true, Set.of("reader")),
                    new FixtureIdentity(NO_PERMISSION_EMAIL, encodedPassword, true, true, Set.of("READER")),
                    new FixtureIdentity(DISABLED_EMAIL, encodedPassword, false, true, Set.of("reader")),
                    new FixtureIdentity(LOCKED_EMAIL, encodedPassword, true, false, Set.of("reader"))));
        }

        @Bean
        @Order(1)
        SecurityFilterChain localSessionSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/login", "/fixture/local/**");
            http.authorizeHttpRequests(requests -> requests
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/fixture/local/read").hasAuthority(PERMISSION_READ)
                    .anyRequest().denyAll());
            http.formLogin(form -> form
                    .usernameParameter("email")
                    .loginProcessingUrl("/login")
                    .permitAll());
            return http.build();
        }

        @Bean
        ProtectedFixtureService protectedFixtureService() {
            return new ProtectedFixtureService();
        }

        @Bean
        FixtureController fixtureController(ProtectedFixtureService service) {
            return new FixtureController(service);
        }
    }

    static final class FixtureIdentityStore implements UserDetailsService {

        private final Map<String, FixtureIdentity> identities;

        private FixtureIdentityStore(List<FixtureIdentity> fixtureIdentities) {
            identities = new LinkedHashMap<>();
            fixtureIdentities.forEach(identity -> {
                FixtureIdentity previous = identities.put(canonicalize(identity.email()), identity);
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate canonical fixture identity");
                }
            });
        }

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            FixtureIdentity identity = identities.get(canonicalize(email));
            if (identity == null) {
                throw new UsernameNotFoundException("Fixture identity was not found");
            }
            Collection<SimpleGrantedAuthority> authorities = identity.roleNames().stream()
                    .map(FixtureRole::fromExternalName)
                    .flatMap(Collection::stream)
                    .flatMap(role -> role.permissions().stream())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toUnmodifiableSet());
            return User.withUsername(canonicalize(identity.email()))
                    .password(identity.encodedPassword())
                    .disabled(!identity.enabled())
                    .accountLocked(!identity.accountNonLocked())
                    .authorities(authorities)
                    .build();
        }

        private static String canonicalize(String email) {
            String canonical = email.trim().toLowerCase(Locale.ROOT);
            if (canonical.isEmpty() || canonical.chars().anyMatch(character -> character > 0x7f)) {
                throw new UsernameNotFoundException("Fixture identity was not found");
            }
            return canonical;
        }
    }

    private record FixtureIdentity(
            String email,
            String encodedPassword,
            boolean enabled,
            boolean accountNonLocked,
            Set<String> roleNames) {}

    private enum FixtureRole {
        READER;

        static Collection<FixtureRole> fromExternalName(String roleName) {
            return roleName.equals("reader") ? Set.of(READER) : Set.of();
        }

        Set<String> permissions() {
            return Set.of(PERMISSION_READ);
        }
    }

    static class ProtectedFixtureService {

        private final AtomicInteger invocations = new AtomicInteger();

        @PreAuthorize("hasAuthority('fixture:read')")
        public String read() {
            invocations.incrementAndGet();
            return "fixture-protected";
        }

        int invocationCount() {
            return invocations.get();
        }
    }

    @RestController
    static class FixtureController {

        private final ProtectedFixtureService service;

        FixtureController(ProtectedFixtureService service) {
            this.service = service;
        }

        @GetMapping("/fixture/local/read")
        String read() {
            return service.read();
        }
    }
}
