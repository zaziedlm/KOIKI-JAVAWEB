package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.koikifw.session.SessionCleanup;
import org.koikifw.session.SessionCleanupException;
import org.koikifw.session.SessionCleanupResult;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

class SessionCleanupContractTest {

    private static final String[] APPROVED_VALUES = {
        "spring.session.jdbc.initialize-schema=never",
        "spring.session.jdbc.table-name=koiki_session",
        "spring.session.jdbc.cleanup-cron=-",
        "server.servlet.session.cookie.http-only=true"
    };

    @Test
    void exposesOnlyTheApprovedVendorNeutralContractShape() {
        assertThat(SessionCleanup.class.isInterface()).isTrue();
        assertThat(SessionCleanupResult.values())
                .containsExactly(SessionCleanupResult.COMPLETED, SessionCleanupResult.CONTENDED);
        assertThat(Modifier.isFinal(SessionCleanupException.class.getModifiers())).isTrue();
        assertThat(new SessionCleanupException())
                .hasMessage("KOIKI expired session cleanup failed")
                .hasNoCause();
    }

    @Test
    void completesCleanupAndExplicitlyUnlocksOnTheDedicatedConnection() throws Exception {
        JdbcIndexedSessionRepository repository = mock(JdbcIndexedSessionRepository.class);
        JdbcMocks jdbc = jdbcMocks(true, true);

        runWith(jdbc.dataSource(), repository, cleanup ->
                assertThat(cleanup.cleanUpExpiredSessions())
                        .isEqualTo(SessionCleanupResult.COMPLETED));

        verify(repository).cleanUpExpiredSessions();
        verify(jdbc.connection()).close();
        verify(jdbc.unlockStatement()).executeQuery();
    }

    @Test
    void returnsImmediatelyWithoutCleanupWhenAnotherProcessOwnsTheLock() throws Exception {
        JdbcIndexedSessionRepository repository = mock(JdbcIndexedSessionRepository.class);
        JdbcMocks jdbc = jdbcMocks(false, false);

        runWith(jdbc.dataSource(), repository, cleanup ->
                assertThat(cleanup.cleanUpExpiredSessions())
                        .isEqualTo(SessionCleanupResult.CONTENDED));

        verify(repository, never()).cleanUpExpiredSessions();
        verify(jdbc.connection()).close();
        verify(jdbc.unlockStatement(), never()).executeQuery();
    }

    @Test
    void convertsDatastoreFailureToTheFixedSafeFailure() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        JdbcIndexedSessionRepository repository = mock(JdbcIndexedSessionRepository.class);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("jdbc:postgresql://secret-host/private-database"));

        runWith(dataSource, repository, cleanup ->
                assertThatThrownBy(cleanup::cleanUpExpiredSessions)
                        .isExactlyInstanceOf(SessionCleanupException.class)
                        .hasMessage("KOIKI expired session cleanup failed")
                        .hasMessageNotContaining("postgresql")
                        .hasNoCause());
        verify(repository, never()).cleanUpExpiredSessions();
    }

    @Test
    void explicitlyUnlocksAndClosesAfterCleanupFailure() throws Exception {
        JdbcIndexedSessionRepository repository = mock(JdbcIndexedSessionRepository.class);
        doThrow(new IllegalStateException("fixture cleanup detail"))
                .when(repository)
                .cleanUpExpiredSessions();
        JdbcMocks jdbc = jdbcMocks(true, true);

        runWith(jdbc.dataSource(), repository, cleanup ->
                assertThatThrownBy(cleanup::cleanUpExpiredSessions)
                        .isExactlyInstanceOf(SessionCleanupException.class)
                        .hasMessage("KOIKI expired session cleanup failed")
                        .hasMessageNotContaining("fixture")
                        .hasNoCause());

        verify(jdbc.unlockStatement()).executeQuery();
        verify(jdbc.connection()).close();
    }

    private void runWith(
            DataSource dataSource,
            JdbcIndexedSessionRepository repository,
            java.util.function.Consumer<SessionCleanup> assertion) {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        KoikiSessionJdbcAutoConfiguration.class))
                .withBean(DataSource.class, () -> dataSource)
                .withBean(JdbcIndexedSessionRepository.class, () -> repository)
                .withPropertyValues(APPROVED_VALUES)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SessionCleanup.class);
                    assertion.accept(context.getBean(SessionCleanup.class));
                });
    }

    private JdbcMocks jdbcMocks(boolean lockAcquired, boolean unlockCompleted)
            throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement tryLockStatement = mock(PreparedStatement.class);
        PreparedStatement unlockStatement = mock(PreparedStatement.class);
        ResultSet tryLockResult = booleanResult(lockAcquired);
        ResultSet unlockResult = booleanResult(unlockCompleted);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString()))
                .thenReturn(tryLockStatement, unlockStatement);
        when(tryLockStatement.executeQuery()).thenReturn(tryLockResult);
        when(unlockStatement.executeQuery()).thenReturn(unlockResult);
        return new JdbcMocks(dataSource, connection, unlockStatement);
    }

    private ResultSet booleanResult(boolean value) throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.next()).thenReturn(true);
        when(result.getBoolean(1)).thenReturn(value);
        return result;
    }

    private record JdbcMocks(
            DataSource dataSource,
            Connection connection,
            PreparedStatement unlockStatement) {}
}
