package org.koikifw.buildsupport.phase4.s1fixture;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Consumer;
import org.koikifw.audit.*;
import org.koikifw.identity.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.ConfigurableWebApplicationContext;

/** Real local Provider/Session/filter stack in a mock Servlet context; no mock authentication. */
public final class S1CWebFixture {
    private S1CWebFixture() {}
    public static void run(String url,boolean check,Consumer<ConfigurableWebApplicationContext> action) {
        new WebApplicationContextRunner().withUserConfiguration(Configuration.class)
                .withPropertyValues("s1c.mode="+(check?"check":"web"),"spring.datasource.url="+url,
                        "spring.datasource.username="+(check?"s1c_check":"s1c_web"),"spring.datasource.password=s1-fixture-only",
                        "spring.datasource.hikari.maximum-pool-size=4","spring.datasource.hikari.minimum-idle=0",
                        "spring.datasource.hikari.connection-timeout=10000","spring.datasource.hikari.connection-init-sql=SET statement_timeout='10s'",
                        "spring.jpa.hibernate.ddl-auto=validate","spring.jpa.open-in-view=false",
                        "spring.jpa.mapping-resources=s1-additional/orm-web-mode.xml","spring.flyway.enabled=false",
                        "spring.session.jdbc.initialize-schema=never","spring.session.jdbc.table-name=koiki_session",
                        "spring.session.jdbc.cleanup-cron=-","server.servlet.session.cookie.http-only=true",
                        "server.servlet.session.cookie.secure=true","server.servlet.session.cookie.same-site=lax",
                        "koiki.identity.local-authentication.enabled=true",
                        "koiki.identity.login-attempt.source-protection=APPLICATION",
                        "koiki.identity.login-attempt.source-hmac-key-id=s1-c-test-only",
                        "koiki.identity.login-attempt.source-hmac-key="+Base64.getEncoder().encodeToString(new byte[32]),
                        "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                        "spring.modulith.republish-outstanding-events-on-restart=false",
                        "spring.modulith.moments.enabled=false",
                        "spring.autoconfigure.exclude=org.springframework.modulith.events.config.EventPublicationAutoConfiguration,org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration")
                .run(context->{
                    if(context.getStartupFailure()!=null) throw new IllegalStateException("C Web startup failed",context.getStartupFailure());
                    var actual=(ConfigurableWebApplicationContext)context.getSourceApplicationContext();
                    if(!actual.getBeansOfType(CompromisedPasswordChecker.class).isEmpty()
                            || !actual.getBeansOfType(IdentityAdministration.class).isEmpty()
                            || actual.containsBean("eventPublicationRegistry")) throw new IllegalStateException("Unexpected C Web capability");
                    action.accept(actual);
                });
    }
    public static MockMvc mvc(ConfigurableWebApplicationContext context) {
        // Exactly one real Session filter before the one real Security chain.
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSessionRepositoryFilter",Filter.class)).apply(springSecurity()).build();
    }
    @TestConfiguration(proxyBeanMethods=false) @EnableAutoConfiguration @Import(S1CStore.Configuration.class)
    public static class Configuration {
        @Bean DefaultCookieSerializer cookieSerializer() {
            var serializer=new DefaultCookieSerializer();serializer.setUseHttpOnlyCookie(true);serializer.setUseSecureCookie(true);serializer.setSameSite("Lax");return serializer;
        }
        @Bean @Order(0) SecurityFilterChain fixtureChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/s1-test/**","/login","/logout");
            http.authorizeHttpRequests(requests->requests.requestMatchers("/login","/logout").permitAll().anyRequest().authenticated());
            http.formLogin(form->form.usernameParameter("email").defaultSuccessUrl("/s1-test/home",true));
            http.csrf(withDefaults());http.headers(withDefaults());return http.build();
        }
        @Bean ReadController readController(S1CStore store,SecurityAuditRecorder audit) {return new ReadController(store,audit);}
        @Bean @ConditionalOnProperty(name="s1c.mode",havingValue="web") Actions actions(S1CStore store) {return new Actions(store);}
        @Bean @ConditionalOnProperty(name="s1c.mode",havingValue="web") UpdateController updateController(Actions actions,SecurityAuditRecorder audit) {return new UpdateController(actions,audit);}
    }
    public static final class Actions {
        private final S1CStore store;
        Actions(S1CStore store){this.store=store;}
        void issue(UUID actor,String env,UUID pub){store.issue(actor,env,pub);}
        void close(UUID actor,String env,UUID pub,String proof){store.close(actor,env,pub,proof);}
    }
    static UUID actor(Authentication authentication) {
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) throw new S1CStore.Denied();
        return principal.userId().value();
    }
    static ResponseEntity<String> denied(SecurityAuditRecorder audit,UUID actor) {
        audit.record(AuditEvent.of("S1_TEST_DENIAL",AuditActor.user(actor.toString()),"S1_TEST_ACCESS",AuditResult.FAILURE).withReason("S1_TEST_DENIED"));
        return ResponseEntity.status(403).body("S1_TEST_DENIED");
    }
    // TestConfiguration keeps the Controller stereotype out of existing Probe component scans.
    @TestConfiguration(proxyBeanMethods=false) @RestController
    public static class ReadController {
        private final S1CStore store;private final SecurityAuditRecorder audit;
        ReadController(S1CStore store,SecurityAuditRecorder audit){this.store=store;this.audit=audit;}
        @GetMapping("/s1-test/home") public String home(HttpServletRequest request) {
            return "CSRF:"+((CsrfToken)request.getAttribute(CsrfToken.class.getName())).getToken();
        }
        @GetMapping("/s1-test/read") public ResponseEntity<?> read(Authentication authentication,
                @RequestParam(defaultValue="fixture-env") String environment,
                @RequestParam(defaultValue="00000000-0000-0000-0000-000000000100") UUID publication) {
            UUID actor=actor(authentication);
            try {return ResponseEntity.ok(store.read(actor,environment,publication));}
            catch(S1CStore.Denied failure){return denied(audit,actor);}
        }
    }
    @TestConfiguration(proxyBeanMethods=false) @RestController
    public static class UpdateController {
        private final Actions actions;private final SecurityAuditRecorder audit;
        UpdateController(Actions actions,SecurityAuditRecorder audit){this.actions=actions;this.audit=audit;}
        @PostMapping("/s1-test/issue") public ResponseEntity<String> issue(Authentication authentication,HttpServletRequest request,
                @RequestParam(defaultValue="fixture-env") String environment,
                @RequestParam(defaultValue="00000000-0000-0000-0000-000000000100") UUID publication) {
            if(request.getParameterMap().containsKey("actorId"))return ResponseEntity.badRequest().body("S1_TEST_INVALID_INPUT");
            UUID actor=actor(authentication);
            try {actions.issue(actor,environment,publication);return ResponseEntity.ok("ISSUED");}
            catch(S1CStore.Denied failure){return denied(audit,actor);}
        }
        @PostMapping("/s1-test/close") public ResponseEntity<String> close(Authentication authentication,HttpServletRequest request,
                @RequestParam(defaultValue="fixture-env") String environment,
                @RequestParam(defaultValue="00000000-0000-0000-0000-000000000100") UUID publication,
                @RequestParam(defaultValue="TEST_REVIEWED") String proof) {
            if(request.getParameterMap().containsKey("actorId"))return ResponseEntity.badRequest().body("S1_TEST_INVALID_INPUT");
            UUID actor=actor(authentication);
            try {actions.close(actor,environment,publication,proof);return ResponseEntity.ok("CLOSED");}
            catch(S1CStore.Denied failure){return denied(audit,actor);}
        }
    }
}
