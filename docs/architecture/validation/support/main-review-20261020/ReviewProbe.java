package org.koikifw.review;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Review-only, no DB/network/server. Compiled alongside the reviewed source
 * files; this is a reproducer of current behavior, not a regression acceptance.
 */
public class ReviewProbe {
    @RestController
    public static class Endpoints {
        @GetMapping("/denied") public String denied() { throw new AccessDeniedException("probe"); }
        @GetMapping("/unauthenticated") public String unauthenticated() { throw new BadCredentialsException("probe"); }
        @GetMapping("/missing") public String missing() { throw new Missing(); }
        @GetMapping({"/", "/login", "/expenses", "/expenses/list"})
        public String mvc() { return "ok"; }
    }
    @RestController
    public static class ApiEndpoint {
        @GetMapping(path = "/api/{version}/probe", version = "1")
        public String api() { return "ok"; }
    }
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public static class Missing extends RuntimeException {}
    public static class Versions extends ApiVersionConfigurer {
        ApiVersionStrategy strategy() { return getApiVersionStrategy(); }
    }
    private static Object instance(String name) throws Exception {
        var constructor = Class.forName(name).getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
    private static void status(MockMvc mvc, String label, String path, int expected) throws Exception {
        int actual = mvc.perform(get(path)).andReturn().getResponse().getStatus();
        System.out.println("REVIEW " + label + " " + path + " status=" + actual);
        if (actual != expected) throw new AssertionError(label + " expected " + expected);
    }
    public static void main(String[] args) throws Exception {
        Object advice = instance("org.koikifw.starter.api.internal.KoikiProblemDetailsExceptionHandler");
        MockMvc errors = MockMvcBuilders.standaloneSetup(new Endpoints()).setControllerAdvice(advice).build();
        status(errors, "global-advice", "/denied", 500);
        status(errors, "global-advice", "/unauthenticated", 500);
        status(errors, "global-advice", "/missing", 500);
        MockMvc plain = MockMvcBuilders.standaloneSetup(new Endpoints()).build();
        status(plain, "without-advice", "/missing", 404);

        var config = instance("org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration");
        var factory = config.getClass().getDeclaredMethod("referenceCookieSerializer");
        factory.setAccessible(true);
        CookieSerializer serializer = (CookieSerializer) factory.invoke(config);
        for (boolean secure : new boolean[]{false, true}) {
            var request = new MockHttpServletRequest();
            request.setSecure(secure);
            var response = new MockHttpServletResponse();
            serializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, "review-fixture"));
            String cookie = response.getHeader("Set-Cookie");
            boolean hasSecure = cookie != null && cookie.contains("; Secure");
            System.out.println("REVIEW reference-cookie requestSecure=" + secure + " cookieSecure=" + hasSecure);
            if (hasSecure != secure) throw new AssertionError("Unexpected serializer behavior");
        }
        Versions versions = new Versions();
        versions.usePathSegment(1).setVersionRequired(true).detectSupportedVersions(false).addSupportedVersions("1");
        MockMvc global = MockMvcBuilders.standaloneSetup(new Endpoints(), new ApiEndpoint())
                .setApiVersionStrategy(versions.strategy()).setControllerAdvice(advice).build();
        status(global, "global-version", "/", 400);
        status(global, "global-version", "/login", 400);
        status(global, "global-version", "/expenses/list", 400);
        status(global, "global-version", "/api/1/probe", 200);
        Versions scoped = new Versions();
        scoped.usePathSegment(1, p -> p.pathWithinApplication().value().startsWith("/api/"))
                .setVersionRequired(false).detectSupportedVersions(false).addSupportedVersions("1");
        MockMvc reference = MockMvcBuilders.standaloneSetup(new Endpoints(), new ApiEndpoint())
                .setApiVersionStrategy(scoped.strategy()).setControllerAdvice(advice).build();
        status(reference, "reference-version", "/login", 200);
        status(reference, "reference-version", "/api/1/probe", 200);
        cookieBootContext(serializer);
        expenseScope();
        architectureImports();
        System.out.println("REVIEW completed");
    }

    private static void cookieBootContext(CookieSerializer serializer) {
        new org.springframework.boot.test.context.runner.WebApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.session.autoconfigure.SessionAutoConfiguration.class))
                .withPropertyValues("server.servlet.session.cookie.secure=true",
                        "server.servlet.session.cookie.same-site=strict")
                .withBean(CookieSerializer.class, () -> serializer)
                .run(context -> {
                    if (context.getStartupFailure() != null) throw new AssertionError(context.getStartupFailure());
                    var response = new MockHttpServletResponse();
                    context.getBean(CookieSerializer.class).writeCookieValue(
                            new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, "review-fixture"));
                    String cookie = response.getHeader("Set-Cookie");
                    boolean secure = cookie.contains("; Secure");
                    boolean strict = cookie.contains("SameSite=Strict");
                    System.out.println("REVIEW boot-cookie configuredSecure=true configuredSameSite=Strict"
                            + " actualSecure=" + secure + " actualStrict=" + strict);
                    if (secure || strict) throw new AssertionError("Behavior changed");
                });
    }

    private static void expenseScope() {
        var owner = java.util.UUID.randomUUID();
        var other = java.util.UUID.randomUUID();
        var id = java.util.UUID.randomUUID();
        var day = java.time.LocalDate.of(2026, 10, 20);
        var request = org.koikifw.reference.expense.domain.model.ExpenseRequest.createDraft(
                id, owner, java.util.UUID.randomUUID(),
                new org.koikifw.reference.expense.domain.model.Money(1),
                java.util.List.of(org.koikifw.reference.expense.domain.model.ExpenseLine.create(
                        java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), day, "probe", "probe",
                        new org.koikifw.reference.expense.domain.model.Money(1), day)),
                day, java.time.Instant.parse("2026-10-20T00:00:00Z"));
        var repoType = org.koikifw.reference.expense.domain.repository.ExpenseRequestRepository.class;
        var repository = (org.koikifw.reference.expense.domain.repository.ExpenseRequestRepository)
                java.lang.reflect.Proxy.newProxyInstance(repoType.getClassLoader(), new Class[]{repoType},
                        (proxy, method, arguments) -> {
                            if (method.getName().equals("findById")) return java.util.Optional.of(request);
                            throw new AssertionError("Mutation or unexpected repository use");
                        });
        var master = new org.koikifw.reference.expense.application.port.outbound.MasterAvailabilityPort() {
            public boolean isActiveDepartment(java.util.UUID value) { throw new AssertionError(); }
            public boolean isActiveExpenseCategory(java.util.UUID value) { throw new AssertionError(); }
            public boolean isUserAssignedToActiveDepartment(org.koikifw.identity.FrameworkUserId user,
                    java.util.UUID value) { throw new AssertionError(); }
        };
        var service = new org.koikifw.reference.expense.application.ExpenseApplicationService(
                repository, master, (user, department) -> false, event -> { throw new AssertionError(); });
        var principal = new org.koikifw.identity.FrameworkPrincipal() {
            public org.koikifw.identity.FrameworkUserId userId() {
                return org.koikifw.identity.FrameworkUserId.parse(other.toString());
            }
            public org.koikifw.identity.AuthenticationSource authenticationSource() { return null; }
            public java.util.Set<String> permissions() { return java.util.Set.of("EXPENSE:APPLY"); }
        };
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, java.util.List.of()));
        try {
            for (long version : new long[]{request.version(), request.version() + 1}) {
                try {
                    service.submit(id, version);
                    throw new AssertionError("Unexpected success");
                } catch (org.koikifw.reference.expense.application.ExpenseOperationException ex) {
                    var expected = version == request.version()
                            ? org.koikifw.reference.expense.application.ExpenseFailure.NOT_FOUND
                            : org.koikifw.reference.expense.application.ExpenseFailure.CONCURRENT_MODIFICATION;
                    System.out.println("REVIEW other-owner expectedVersion=" + version + " failure=" + ex.failure());
                    if (ex.failure() != expected) throw new AssertionError("Behavior changed");
                }
            }
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    private static void architectureImports() {
        var all = new com.tngtech.archunit.core.importer.ClassFileImporter()
                .importPackages("org.koikifw.reference");
        long tests = all.stream().filter(c -> c.getSource().map(s ->
                s.getUri().toString().contains("test-classes")).orElse(false)).count();
        var production = new com.tngtech.archunit.core.importer.ClassFileImporter()
                .withImportOption(com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.koikifw.reference");
        var rules = org.koikifw.archunit.KoikiArchitectureRules.businessModuleRules(
                "org.koikifw.reference", java.util.Map.of("notification", org.koikifw.archunit.ModuleEventLevel.LEVEL_2));
        System.out.println("REVIEW arch-import all=" + all.size() + " testClasses=" + tests
                + " production=" + production.size());
        System.out.println("REVIEW arch-rules allViolations=" + rules.evaluate(all).getFailureReport().getDetails().size()
                + " productionViolations=" + rules.evaluate(production).getFailureReport().getDetails().size());
    }
}
