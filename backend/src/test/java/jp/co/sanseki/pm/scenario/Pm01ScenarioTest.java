package jp.co.sanseki.pm.scenario;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * test/scenarios/pm01 の全シナリオを、PostgreSQL（Testcontainers）＋起動したアプリに対して実 HTTP で実行する。
 * 全件合格が PM01 移行の完了条件。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("scenario")
@Testcontainers
class Pm01ScenarioTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("mirror.gcr.io/library/postgres:16-alpine").asCompatibleSubstituteFor("postgres"));

    @LocalServerPort
    int port;

    @TestFactory
    Stream<DynamicTest> pm01() throws IOException {
        return ScenarioSuite.tests("pm01", "http://localhost:" + port);
    }
}
