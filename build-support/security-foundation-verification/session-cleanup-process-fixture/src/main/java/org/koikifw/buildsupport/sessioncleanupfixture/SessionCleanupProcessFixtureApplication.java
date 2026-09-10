package org.koikifw.buildsupport.sessioncleanupfixture;

import org.koikifw.session.SessionCleanup;
import org.koikifw.session.SessionCleanupException;
import org.koikifw.session.SessionCleanupResult;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Non-distributed non-web executable fixture for the Phase 2 B3-5 process boundary. */
@SpringBootApplication
public class SessionCleanupProcessFixtureApplication {

    /** Runs one cleanup attempt and maps its public result to the approved process exit. */
    public static void main(String[] args) {
        SpringApplication application =
                new SpringApplication(SessionCleanupProcessFixtureApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        int exitCode = 1;
        ConfigurableApplicationContext context = null;
        try {
            context = application.run(args);
            SessionCleanupResult result = context.getBean(SessionCleanup.class)
                    .cleanUpExpiredSessions();
            exitCode = result == SessionCleanupResult.COMPLETED ? 0 : 10;
        } catch (SessionCleanupException exception) {
            System.err.println(exception.getMessage());
        } finally {
            if (context != null) {
                context.close();
            }
        }
        System.exit(exitCode);
    }
}
