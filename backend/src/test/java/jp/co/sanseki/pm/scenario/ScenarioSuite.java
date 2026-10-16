package jp.co.sanseki.pm.scenario;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;

/** シナリオを DynamicTest 化し、target/scenario-report/{screen}.md に結果を出力する。 */
final class ScenarioSuite {

    private ScenarioSuite() {
    }

    static Stream<DynamicTest> tests(String screen, String baseUrl) throws IOException {
        ScenarioRunner runner = new ScenarioRunner(baseUrl);
        List<ScenarioRunner.Scenario> scenarios = ScenarioRunner.load(screen);
        assertTrue(!scenarios.isEmpty(), "シナリオがありません: test/scenarios/" + screen);
        List<ScenarioRunner.ScenarioResult> results = new ArrayList<>();
        return Stream.concat(
                scenarios.stream().map(s -> DynamicTest.dynamicTest(s.id() + " " + s.title(), () -> {
                    ScenarioRunner.ScenarioResult r = runner.run(s);
                    results.add(r);
                    StringBuilder msg = new StringBuilder();
                    r.steps().stream().filter(st -> !st.passed())
                            .forEach(st -> msg.append(st.name()).append(": ").append(st.detail()).append('\n'));
                    assertTrue(r.passed(), msg.toString());
                })),
                Stream.of(DynamicTest.dynamicTest("report", () -> {
                    Path dir = Path.of("target", "scenario-report");
                    Files.createDirectories(dir);
                    String report = ScenarioRunner.report(screen, results);
                    Files.writeString(dir.resolve(screen + ".md"), report, StandardCharsets.UTF_8);
                    System.out.println(report);
                })));
    }
}
