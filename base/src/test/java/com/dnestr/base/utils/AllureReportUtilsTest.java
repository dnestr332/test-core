package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AllureReportUtilsTest {

    @Test
    void writeEnvironmentWritesThePlatformTitleAndDate(@TempDir Path dir) throws Exception {
        AllureReportUtils.writeEnvironment("ANDROID", dir.resolve("allure-results"));

        assertThat(Files.readAllLines(dir.resolve("allure-results/environment.properties"))).containsExactly(
                "Platform=ANDROID", "ReportTitle=ANDROID REPORT", "ExecutionDate=" + LocalDate.now());
    }

    @Test
    void writeExecutorWritesJsonNamedAfterThePlatform(@TempDir Path dir) throws Exception {
        AllureReportUtils.writeExecutor("IOS \"sim\"", dir);

        assertThat(Files.readString(dir.resolve("executor.json")))
                .contains("\"name\": \"IOS \\\"sim\\\" REPORT\"", "\"type\": \"local\"", "\"buildOrder\": 1");
    }

    @Test
    void aDirectoryThatCannotBeCreatedIsOnlyLogged(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("taken"), "a file, not a directory");

        assertThatCode(() -> AllureReportUtils.writeEnvironment("ANDROID", file)).doesNotThrowAnyException();
    }
}
