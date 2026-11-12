// Evidence-only probe for docs/development/phase4-consumer-feedback-web-fragment-session-logout-owner-review-20261112.md.
// Not part of any build. Compiled ad hoc against the koiki-reference-app test classpath (scratch worktree of f6ff36d).
import static org.mockito.Mockito.mock;
import javax.sql.DataSource;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.transaction.PlatformTransactionManager;

public class LogoutCustomizerOrderProbe {
    public static void main(String[] args) {
        String[] props = {
            "spring.session.jdbc.initialize-schema=never",
            "spring.session.jdbc.table-name=koiki_session",
            "spring.session.jdbc.cleanup-cron=-",
            "server.servlet.session.cookie.http-only=true",
            "server.servlet.session.cookie.secure=true",
            "server.servlet.session.cookie.same-site=lax"};
        WebApplicationContextRunner base = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                SessionAutoConfiguration.class, JdbcSessionAutoConfiguration.class,
                KoikiSessionJdbcAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(JdbcIndexedSessionRepository.class, () -> mock(JdbcIndexedSessionRepository.class))
            .withPropertyValues(props);
        base.run(ctx -> report("A: Boot session auto-config, no app CookieSerializer", ctx));
        base.withBean("appCookieSerializer", CookieSerializer.class, DefaultCookieSerializer::new)
            .run(ctx -> report("B: app declares CookieSerializer (Reference pattern)", ctx));
    }
    static void report(String label, org.springframework.context.ApplicationContext ctx) {
        if (ctx instanceof org.springframework.boot.test.context.assertj.AssertableWebApplicationContext a
                && a.getStartupFailure() != null) {
            System.out.println(label + " -> STARTUP FAILED: " + a.getStartupFailure());
            return;
        }
        System.out.println(label);
        System.out.println("  CookieSerializer beans      = " + java.util.Arrays.toString(ctx.getBeanNamesForType(CookieSerializer.class)));
        System.out.println("  koikiSessionLogoutHandler   = " + ctx.containsBean("koikiSessionLogoutHandler"));
        System.out.println("  koikiSessionLogoutCustomizer= " + ctx.containsBean("koikiSessionLogoutCustomizer"));
    }
}
