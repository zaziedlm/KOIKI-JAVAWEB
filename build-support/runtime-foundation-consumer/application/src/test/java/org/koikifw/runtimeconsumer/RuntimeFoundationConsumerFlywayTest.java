package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(RuntimePostgreSqlTestConfiguration.class)
class RuntimeFoundationConsumerFlywayTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void keepsOwnerHistoriesSeparateAndRunsKoikiBeforeCustomer() {
        List<String> koikiVersions = jdbcClient.sql("""
                        select version
                        from koiki_flyway_history
                        where success
                        order by installed_rank
                        """)
                .query((resultSet, rowNumber) -> Objects.requireNonNull(resultSet.getString("version")))
                .list();
        List<String> customerVersions = jdbcClient.sql("""
                        select version
                        from flyway_schema_history
                        where success
                        order by installed_rank
                        """)
                .query((resultSet, rowNumber) -> Objects.requireNonNull(resultSet.getString("version")))
                .list();
        String orderMarker = jdbcClient.sql("""
                        select marker
                        from kkbiz_migration_order_probe
                        where id = 1
                        """)
                .query(String.class)
                .single();

        assertThat(koikiVersions).containsExactly("1");
        assertThat(customerVersions).containsExactly("0", "1", "2");
        assertThat(orderMarker).isEqualTo("koiki-before-customer");
    }
}
