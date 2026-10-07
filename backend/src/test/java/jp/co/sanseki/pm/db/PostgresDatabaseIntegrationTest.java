package jp.co.sanseki.pm.db;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 本番想定の PostgreSQL（Testcontainers）で同じテストを実行する。Docker が使えない環境ではスキップされる。 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresDatabaseIntegrationTest extends AbstractDatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
