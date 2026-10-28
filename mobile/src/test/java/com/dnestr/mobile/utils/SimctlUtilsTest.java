package com.dnestr.mobile.utils;

import com.dnestr.base.utils.ProcessUtils;
import com.dnestr.mobile.utils.SimctlUtils.IosSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/** xcrun itself is replaced: each test fixes what simctl prints. */
class SimctlUtilsTest {

    @Test
    void getBootedIosSimulatorsReadsBootedDevicesWithTheirRuntimeVersion() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "list", "devices")).thenReturn(List.of(
                    "== Devices ==",
                    "-- iOS 17.5 --",
                    "    iPhone 15 (11111111-2222-3333-4444-555555555555) (Booted) ",
                    "    iPhone SE (3rd generation) (AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE) (Shutdown) ",
                    "-- watchOS 10.5 --",
                    "    Apple Watch (99999999-8888-7777-6666-555555555555) (Booted) ",
                    "-- iOS 18.0 --",
                    "    iPhone 16 Pro (FFFFFFFF-0000-1111-2222-333333333333) (Booted) "
            ));

            assertThat(SimctlUtils.getBootedIosSimulators()).containsExactly(
                    new IosSimulator("11111111-2222-3333-4444-555555555555", "iPhone 15", "17.5"),
                    new IosSimulator("FFFFFFFF-0000-1111-2222-333333333333", "iPhone 16 Pro", "18.0"));
        }
    }

    @Test
    void getIosSimulatorAppVersionReadsTheShortVersionString() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "appinfo", "udid", "com.app")).thenReturn(List.of(
                    "{", "    CFBundleIdentifier = \"com.app\";", "    CFBundleShortVersionString = \"6.2.1\";", "}"));

            assertThat(SimctlUtils.getIosSimulatorAppVersion("udid", "com.app")).contains("6.2.1");
        }
    }

    @Test
    void getIosSimulatorAppVersionIsEmptyWhenTheAppIsNotInstalled() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "appinfo", "udid", "com.app")).thenReturn(List.of(
                    "An error was encountered processing the command (domain=NSPOSIXErrorDomain, code=2)"));

            assertThat(SimctlUtils.getIosSimulatorAppVersion("udid", "com.app")).isEmpty();
        }
    }

    @Test
    void sendIosSimulatorPushPassesThePayloadAsAFile() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            List<String> payloads = new ArrayList<>();
            process.when(() -> ProcessUtils.run(eq("xcrun"), eq("simctl"), eq("push"), eq("udid"), eq("com.app"), anyString()))
                    .thenAnswer(call -> {
                        payloads.add(Files.readString(Path.of(call.<String>getArgument(5))));
                        return List.of("Notification sent to 'com.app'");
                    });

            SimctlUtils.sendIosSimulatorPush("udid", "com.app", "{\"aps\":{\"alert\":\"Hi\"}}");

            assertThat(payloads).containsExactly("{\"aps\":{\"alert\":\"Hi\"}}");
        }
    }

    @Test
    void sendIosSimulatorPushReportsWhatSimctlSaidWhenItFails() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run(eq("xcrun"), eq("simctl"), eq("push"), eq("udid"), eq("com.app"), anyString()))
                    .thenReturn(List.of("Invalid device: udid"));

            assertThatThrownBy(() -> SimctlUtils.sendIosSimulatorPush("udid", "com.app", "{}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid device: udid");
        }
    }
}
