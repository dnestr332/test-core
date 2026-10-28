package com.dnestr.base.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Writes the files an Allure report reads from its results directory to fill in its Environment and Executor
 * widgets. Plain file output: Allure itself is not needed. A failure is logged, not thrown, since the report is
 * still usable without them.
 */
@Slf4j
public final class AllureReportUtils {

    private AllureReportUtils() {}

    /** {@code environment.properties}: the platform, a report title and today's date. */
    public static void writeEnvironment(String platform, Path resultsDir) {
        String properties = """
                Platform=%1$s
                ReportTitle=%1$s REPORT
                ExecutionDate=%2$s
                """.formatted(platform, LocalDate.now());
        write(resultsDir.resolve("environment.properties"), properties);
    }

    /** {@code executor.json}, naming the report after {@code platform}. */
    public static void writeExecutor(String platform, Path resultsDir) {
        String json = """
                {
                  "name": "%1$s REPORT",
                  "type": "local",
                  "reportName": "%1$s REPORT",
                  "buildOrder": 1
                }""".formatted(platform.replace("\\", "\\\\").replace("\"", "\\\""));
        write(resultsDir.resolve("executor.json"), json);
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            log.info("Wrote {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not write {}: {}", file, e.getMessage());
        }
    }
}
