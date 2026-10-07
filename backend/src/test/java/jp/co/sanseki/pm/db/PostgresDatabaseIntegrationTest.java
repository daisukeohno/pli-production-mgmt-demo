package jp.co.sanseki.pm.db;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 本番想定の PostgreSQL（Testcontainers）で同じテストを実行する。Docker が使えない環境ではスキップされる。
 * Docker Hub の匿名 pull 制限（429）を避けるため、公式イメージのミラー（mirror.gcr.io）から取得する。
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresDatabaseIntegrationTest extends AbstractDatabaseIntegrationTest {

    static final DockerImageName POSTGRES_IMAGE = DockerImageName
            .parse("mirror.gcr.io/library/postgres:16-alpine")
            .asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);
}
