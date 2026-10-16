package jp.co.sanseki.pm.scenario;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 起動済みのアプリ（scenario プロファイル）に対してシナリオを実行する。
 * mvn test -Dtest=LiveScenarioTest -Dscenario.baseUrl=http://localhost:8080 [-Dscenario.screen=pm01]
 */
@EnabledIfSystemProperty(named = "scenario.baseUrl", matches = ".+")
class LiveScenarioTest {

    @TestFactory
    Stream<DynamicTest> live() throws IOException {
        return ScenarioSuite.tests(System.getProperty("scenario.screen", "pm01"), System.getProperty("scenario.baseUrl"));
    }
}
