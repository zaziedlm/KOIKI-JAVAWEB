package com.example.koikiconsumer.c2;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.session.SessionCleanup;
import org.koikifw.session.SessionCleanupResult;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Root-reactor-external executable Consumer used only by the P2-C2 harness. */
@SpringBootApplication
public class C2ConsumerApplication {

    /** Starts one isolated PostgreSQL probe and exits after externally observable completion. */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(C2ConsumerApplication.class);
        boolean webProbe = Arrays.asList(args).contains("--koiki.consumer.c2.web-probe=true");
        if (webProbe) {
            application.setWebApplicationType(WebApplicationType.SERVLET);
            application.run(args);
            return;
        }
        application.setWebApplicationType(WebApplicationType.NONE);

        ConfigurableApplicationContext context = null;
        int exitCode = 1;
        try {
            context = application.run(args);
            FrameworkUserId actorId = FrameworkUserId.parse(UUID.randomUUID().toString());
            FrameworkPrincipal principal = new ProbePrincipal(actorId);
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, java.util.List.of()));

            String syntheticEmail = "c2-" + UUID.randomUUID() + "@" + "invalid.example";
            context.getBean(C2PublicApiProbe.class).run(syntheticEmail);
            SessionCleanupResult cleanup = context.getBean(SessionCleanup.class)
                    .cleanUpExpiredSessions();
            if (cleanup != SessionCleanupResult.COMPLETED) {
                throw new IllegalStateException("The Session cleanup boundary was contended.");
            }
            System.out.println("P2-C2-C2-3-CONSUMER-SUCCEEDED");
            exitCode = 0;
        } finally {
            SecurityContextHolder.clearContext();
            if (context != null) {
                context.close();
            }
        }
        System.exit(exitCode);
    }

    private record ProbePrincipal(FrameworkUserId userId) implements FrameworkPrincipal {

        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }

        @Override
        public Set<String> permissions() {
            return Set.of();
        }
    }
}
