package org.koikifw.walkingskeleton.tier2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.koiki.walkingskeleton.architecture.KoikiModule;
import dev.koiki.walkingskeleton.architecture.ModuleTier;
import dev.koiki.walkingskeleton.architecture.PersistenceModel;
import dev.koiki.walkingskeleton.architecture.PersistenceTechnology;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;
import org.koikifw.walkingskeleton.tier2.expense.application.ExpenseUseCase;
import org.koikifw.walkingskeleton.tier2.expense.application.ExpenseUseCase.CreateExpenseCommand;
import org.koikifw.walkingskeleton.tier2.expense.application.ExpenseUseCase.ExpenseLineCommand;
import org.koikifw.walkingskeleton.tier2.expense.application.ExpenseUseCase.ExpenseResult;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseLine;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequestId;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseStatus;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.Money;
import org.koikifw.walkingskeleton.tier2.expense.domain.repository.ExpenseRequestRepository;
import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.Category;
import org.koikifw.walkingskeleton.tier2.masterdata.adapter.outbound.persistence.CategoryRepository;
import org.koikifw.walkingskeleton.tier2.masterdata.application.DeactivateCategoryUseCase;
import org.koikifw.walkingskeleton.tier2.masterdata.application.DeactivateCategoryUseCase.CategoryResult;
import org.koikifw.walkingskeleton.tier2.masterdata.domain.event.CategoryDeactivating;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.NamedInterface;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class Tier2PracticalityInfrastructureTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ExpenseRequestRepository expenseRequestRepository;

    @Autowired
    private ExpenseUseCase expenseUseCase;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DeactivateCategoryUseCase deactivateCategoryUseCase;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void startsApplicationContextWithOsivDisabled() {
        assertThat(applicationContext).isNotNull();
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class))
                .isFalse();
        assertThat(Runtime.version().feature()).isEqualTo(21);
    }

    @Test
    void declaresBusinessModuleMetadata() throws ClassNotFoundException {
        KoikiModule expense = moduleMetadata(
                "org.koikifw.walkingskeleton.tier2.expense.package-info");
        assertThat(expense.name()).isEqualTo("expense");
        assertThat(expense.tier()).isEqualTo(ModuleTier.RICH);
        assertThat(expense.persistence()).isEqualTo(PersistenceTechnology.JPA);
        assertThat(expense.persistenceModel()).isEqualTo(PersistenceModel.SHARED);

        KoikiModule masterdata = moduleMetadata(
                "org.koikifw.walkingskeleton.tier2.masterdata.package-info");
        assertThat(masterdata.name()).isEqualTo("masterdata");
        assertThat(masterdata.tier()).isEqualTo(ModuleTier.SIMPLE);
        assertThat(masterdata.persistence()).isEqualTo(PersistenceTechnology.JPA);
        assertThat(masterdata.persistenceModel()).isEqualTo(PersistenceModel.SHARED);
    }

    @Test
    void declaresOnlyMasterdataEventsAsExpenseDependency() throws ClassNotFoundException {
        NamedInterface eventBoundary = CategoryDeactivating.class.getPackage()
                .getAnnotation(NamedInterface.class);
        assertThat(eventBoundary.value()).containsExactly("events");

        ApplicationModule expenseModule = Class.forName(
                        "org.koikifw.walkingskeleton.tier2.expense.package-info")
                .getPackage()
                .getAnnotation(ApplicationModule.class);
        assertThat(expenseModule.allowedDependencies())
                .containsExactly("masterdata::events");
    }

    @Test
    void appliesApplicationOwnedFlywayMigration() {
        List<String> tableNames = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name LIKE 'ws_%'
                ORDER BY table_name
                """,
                String.class);

        assertThat(tableNames).containsExactly(
                "ws_category",
                "ws_expense_line",
                "ws_expense_request");

        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                  AND version = '1'
                """,
                Integer.class);

        assertThat(migrationCount).isEqualTo(1);
    }

    @Test
    void persistsAggregateAndKeepsLinesLazyUntilAccessedInTransaction() {
        UUID categoryId = insertCategory("travel");
        ExpenseRequest request = ExpenseRequest.draft(
                categoryId,
                "Tokyo business trip",
                Money.of(new BigDecimal("1200.00")),
                List.of(
                        ExpenseLine.of("train", Money.of(new BigDecimal("800.00"))),
                        ExpenseLine.of("bus", Money.of(new BigDecimal("400.00")))));

        ExpenseRequestId requestId = Objects.requireNonNull(
                new TransactionTemplate(transactionManager).execute(status -> {
                    ExpenseRequest saved = expenseRequestRepository.save(request);
                    entityManager.flush();
                    return saved.id();
                }));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.clear();
            ExpenseRequest restored = expenseRequestRepository.findById(requestId).orElseThrow();

            assertThat(Persistence.getPersistenceUtil().isLoaded(restored, "lines")).isFalse();
            assertThat(restored.lines())
                    .extracting(ExpenseLine::description)
                    .containsExactly("train", "bus");
            assertThat(Persistence.getPersistenceUtil().isLoaded(restored, "lines")).isTrue();
        });

        Integer persistedLineCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ws_expense_line WHERE expense_request_id = ?",
                Integer.class,
                requestId.value());
        assertThat(persistedLineCount).isEqualTo(2);
    }

    @Test
    void applicationUseCasePersistsLegalLifecycleTransitions() {
        UUID categoryId = insertCategory("lodging");
        ExpenseResult created = expenseUseCase.create(command(
                categoryId, "hotel", "10000.00", "6000.00", "4000.00"));
        assertThat(created.status()).isEqualTo("DRAFT");

        ExpenseResult submitted = expenseUseCase.submit(created.expenseRequestId());
        assertThat(submitted.status()).isEqualTo("SUBMITTED");

        ExpenseResult approved = expenseUseCase.approve(created.expenseRequestId());
        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(loadStatus(created.expenseRequestId())).isEqualTo(ExpenseStatus.APPROVED);

        ExpenseResult rejectedCandidate = expenseUseCase.create(command(
                categoryId, "taxi", "3000.00", "3000.00"));
        expenseUseCase.submit(rejectedCandidate.expenseRequestId());
        ExpenseResult rejected = expenseUseCase.reject(rejectedCandidate.expenseRequestId());
        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(loadStatus(rejected.expenseRequestId())).isEqualTo(ExpenseStatus.REJECTED);
    }

    @Test
    void failedSubmitLeavesPersistedDraftUnchanged() {
        UUID categoryId = insertCategory("supplies");
        ExpenseResult created = expenseUseCase.create(command(
                categoryId, "stationery", "1000.00", "800.00"));

        assertThatThrownBy(() -> expenseUseCase.submit(created.expenseRequestId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expense line total must equal requested amount");
        assertThat(loadStatus(created.expenseRequestId())).isEqualTo(ExpenseStatus.DRAFT);
    }

    @Test
    void generatedTierOneRepositoryPersistsCategoryDeactivation() {
        UUID categoryId = insertCategory("meals");
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        assertThat(category.active()).isTrue();

        CategoryResult result = deactivateCategoryUseCase.deactivate(categoryId);

        assertThat(result).isEqualTo(new CategoryResult(categoryId, false));
        Boolean active = jdbcTemplate.queryForObject(
                "SELECT active FROM ws_category WHERE category_id = ?",
                Boolean.class,
                categoryId);
        assertThat(active).isFalse();
    }

    private static KoikiModule moduleMetadata(String packageInfoClassName)
            throws ClassNotFoundException {
        Package modulePackage = Class.forName(packageInfoClassName).getPackage();
        return modulePackage.getAnnotation(KoikiModule.class);
    }

    private UUID insertCategory(String name) {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO ws_category (category_id, name, active) VALUES (?, ?, TRUE)",
                categoryId,
                name);
        return categoryId;
    }

    private ExpenseStatus loadStatus(UUID expenseRequestId) {
        return Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(status -> {
            entityManager.clear();
            return expenseRequestRepository.findById(ExpenseRequestId.of(expenseRequestId))
                    .orElseThrow()
                    .status();
        }));
    }

    private static CreateExpenseCommand command(
            UUID categoryId,
            String description,
            String requestedAmount,
            String... lineAmounts) {
        List<ExpenseLineCommand> lines = java.util.stream.IntStream
                .range(0, lineAmounts.length)
                .mapToObj(index -> new ExpenseLineCommand(
                        "line-" + index, new BigDecimal(lineAmounts[index])))
                .toList();
        return new CreateExpenseCommand(
                categoryId, description, new BigDecimal(requestedAmount), lines);
    }
}
