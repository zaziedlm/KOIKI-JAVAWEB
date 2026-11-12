// Evidence-only probe for docs/development/phase4-consumer-feedback-web-fragment-session-logout-owner-review-20261112.md.
// Not part of any build. Compiled ad hoc against the koiki-reference-app test classpath (scratch worktree of f6ff36d).
import static org.mockito.Mockito.mock;
import java.lang.reflect.Field;
import java.util.List;
import javax.sql.DataSource;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.PlatformTransactionManager;

public class LogoutHandlerDuplicationProbe {
    @Configuration(proxyBeanMethods = false) @EnableWebSecurity
    static class ExplicitChain {
        @Bean SecurityFilterChain chain(HttpSecurity http, Customizer<HttpSecurity> koikiSessionLogoutCustomizer) throws Exception {
            koikiSessionLogoutCustomizer.customize(http);
            return http.authorizeHttpRequests(a -> a.anyRequest().authenticated()).build();
        }
    }
    @Configuration(proxyBeanMethods = false) @EnableWebSecurity
    static class PlainChain {
        @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(a -> a.anyRequest().authenticated()).build();
        }
    }
    public static void main(String[] args) {
        WebApplicationContextRunner base = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class, JdbcSessionAutoConfiguration.class, KoikiSessionJdbcAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(JdbcIndexedSessionRepository.class, () -> mock(JdbcIndexedSessionRepository.class))
            .withPropertyValues("spring.session.jdbc.initialize-schema=never","spring.session.jdbc.table-name=koiki_session","spring.session.jdbc.cleanup-cron=-","server.servlet.session.cookie.http-only=true");
        base.withUserConfiguration(ExplicitChain.class).run(c -> report("explicit customize(http)", c));
        base.withUserConfiguration(PlainChain.class).run(c -> report("no explicit call", c));
    }
    static void report(String label, org.springframework.boot.test.context.assertj.AssertableWebApplicationContext c) throws Exception {
        if (c.getStartupFailure() != null) { System.out.println(label + " FAILED " + c.getStartupFailure()); return; }
        LogoutHandler koiki = c.getBean("koikiSessionLogoutHandler", LogoutHandler.class);
        for (SecurityFilterChain ch : c.getBeansOfType(SecurityFilterChain.class).values()) {
            for (var f : ch.getFilters()) if (f instanceof LogoutFilter lf) {
                Field h = LogoutFilter.class.getDeclaredField("handler"); h.setAccessible(true);
                CompositeLogoutHandler comp = (CompositeLogoutHandler) h.get(lf);
                Field l = CompositeLogoutHandler.class.getDeclaredField("logoutHandlers"); l.setAccessible(true);
                List<?> hs = (List<?>) l.get(comp);
                long n = hs.stream().filter(x -> x == koiki).count();
                System.out.println(label + ": koikiSessionLogoutHandler occurrences in LogoutFilter = " + n + " of " + hs.size());
            }
        }
    }
}
