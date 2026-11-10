package com.dnestr.mobile.utils;

import com.dnestr.base.utils.ProcessUtils;
import com.dnestr.mobile.utils.XcrunUtils.IosSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.Locale;
import java.time.Duration;
import org.mockito.InOrder;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;

/** xcrun itself is replaced: each test fixes what simctl prints. */
class XcrunUtilsTest {

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

            assertThat(XcrunUtils.getBootedIosSimulators()).containsExactly(
                    new IosSimulator("11111111-2222-3333-4444-555555555555", "iPhone 15", "17.5"),
                    new IosSimulator("FFFFFFFF-0000-1111-2222-333333333333", "iPhone 16 Pro", "18.0"));
        }
    }

    @Test
    void getIosSimulatorAppVersionReadsTheShortVersionString() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "appinfo", "udid", "com.app")).thenReturn(List.of(
                    "{", "    CFBundleIdentifier = \"com.app\";", "    CFBundleShortVersionString = \"6.2.1\";", "}"));

            assertThat(XcrunUtils.getIosSimulatorAppVersion("udid", "com.app")).contains("6.2.1");
        }
    }

    @Test
    void getIosSimulatorAppVersionIsEmptyWhenTheAppIsNotInstalled() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "appinfo", "udid", "com.app")).thenReturn(List.of(
                    "An error was encountered processing the command (domain=NSPOSIXErrorDomain, code=2)"));
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "get_app_container", "udid", "com.app", "app"))
                    .thenReturn(List.of("An error was encountered processing the command (domain=NSPOSIXErrorDomain, code=2)"));

            assertThat(XcrunUtils.getIosSimulatorAppVersion("udid", "com.app")).isEmpty();
        }
    }

    /** What Xcode 26.5's simctl printed on CI for an installed, running app: every key but the version. */
    @Test
    void getIosSimulatorAppVersionReadsTheInfoPlistWhenAppinfoLeavesTheVersionOut() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "appinfo", "udid", "com.app")).thenReturn(List.of(
                    "{", "    CFBundleIdentifier = \"com.app\";", "    CFBundleName = App;", "    CFBundleVersion = 1;", "}"));
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "get_app_container", "udid", "com.app", "app"))
                    .thenReturn(List.of("/sim/Bundle/Application/X/App.app"));
            process.when(() -> ProcessUtils.run("/usr/libexec/PlistBuddy", "-c", "Print CFBundleShortVersionString",
                    "/sim/Bundle/Application/X/App.app/Info.plist")).thenReturn(List.of("5.0.0"));

            assertThat(XcrunUtils.getIosSimulatorAppVersion("udid", "com.app")).contains("5.0.0");
        }
    }

    @Test
    void getIosSimulatorAppVersionIsEmptyWhenTheInfoPlistHasNoVersion() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "appinfo", "udid", "com.app")).thenReturn(List.of("{", "}"));
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "get_app_container", "udid", "com.app", "app"))
                    .thenReturn(List.of("/sim/App.app"));
            process.when(() -> ProcessUtils.run("/usr/libexec/PlistBuddy", "-c", "Print CFBundleShortVersionString",
                    "/sim/App.app/Info.plist")).thenReturn(List.of("Print: Entry, \":CFBundleShortVersionString\", Does Not Exist"));

            assertThat(XcrunUtils.getIosSimulatorAppVersion("udid", "com.app")).isEmpty();
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

            XcrunUtils.sendIosSimulatorPush("udid", "com.app", "{\"aps\":{\"alert\":\"Hi\"}}");

            assertThat(payloads).containsExactly("{\"aps\":{\"alert\":\"Hi\"}}");
        }
    }

    @Test
    void sendIosSimulatorPushReportsWhatSimctlSaidWhenItFails() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run(eq("xcrun"), eq("simctl"), eq("push"), eq("udid"), eq("com.app"), anyString()))
                    .thenReturn(List.of("Invalid device: udid"));

            assertThatThrownBy(() -> XcrunUtils.sendIosSimulatorPush("udid", "com.app", "{}"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid device: udid");
        }
    }

    @Test
    void rebootIosSimulatorShutsDownBootsAndWaitsForTheBoot() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            XcrunUtils.rebootIosSimulator("udid");

            InOrder order = inOrder(ProcessUtils.class);
            order.verify(process, () -> ProcessUtils.run("xcrun", "simctl", "shutdown", "udid"));
            order.verify(process, () -> ProcessUtils.run("xcrun", "simctl", "boot", "udid"));
            order.verify(process, () -> ProcessUtils.run(any(Duration.class), eq("xcrun"), eq("simctl"), eq("bootstatus"), eq("udid"), eq("-b")));
        }
    }

    @Test
    void setIosSimulatorLocationWritesCoordinatesWithADotWhateverTheLocale() {
        Locale before = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            XcrunUtils.setIosSimulatorLocation("udid", 40.712776, -74.005974);

            process.verify(() -> ProcessUtils.run("xcrun", "simctl", "location", "udid", "set", "40.712776,-74.005974"));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void grantAndRevokeIosSimulatorPrivacyNameTheServiceAndTheApp() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            XcrunUtils.grantIosSimulatorPrivacy("udid", "location-always", "com.app");
            XcrunUtils.revokeIosSimulatorPrivacy("udid", "location", "com.app");

            process.verify(() -> ProcessUtils.run("xcrun", "simctl", "privacy", "udid", "grant", "location-always", "com.app"));
            process.verify(() -> ProcessUtils.run("xcrun", "simctl", "privacy", "udid", "revoke", "location", "com.app"));
        }
    }

    @Test
    void clearIosSimulatorAppDataEmptiesTheDataFoldersAndResetsTheKeychain(@TempDir Path container) throws Exception {
        Files.createDirectories(container.resolve("Library/Preferences"));
        Files.writeString(container.resolve("Library/Preferences/app.plist"), "session");
        Files.createDirectories(container.resolve("Documents"));
        Files.writeString(container.resolve("Documents/cache.json"), "{}");
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "get_app_container", "udid", "com.app", "data"))
                    .thenReturn(List.of(container.toString()));
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "keychain", "udid", "reset")).thenReturn(List.of());

            XcrunUtils.clearIosSimulatorAppData("udid", "com.app");

            assertThat(container.resolve("Library")).isEmptyDirectory();
            assertThat(container.resolve("Documents")).isEmptyDirectory();
            process.verify(() -> ProcessUtils.run("xcrun", "simctl", "keychain", "udid", "reset"));
        }
    }

    @Test
    void clearIosSimulatorAppDataRefusesAnAppWithNoDataContainer() {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run("xcrun", "simctl", "get_app_container", "udid", "com.app", "data"))
                    .thenReturn(List.of("An error was encountered processing the command"));

            assertThatThrownBy(() -> XcrunUtils.clearIosSimulatorAppData("udid", "com.app"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No data container for com.app");
        }
    }

    // devicectl: each test writes the JSON devicectl would leave in its --json-output file
    private boolean installedWhenDeviceWrites(String json) {
        try (MockedStatic<ProcessUtils> process = mockStatic(ProcessUtils.class)) {
            process.when(() -> ProcessUtils.run(any(Duration.class), eq("xcrun"), eq("devicectl"), eq("device"), eq("info"),
                    eq("apps"), eq("--device"), eq("udid"), eq("--bundle-id"), eq("com.app"), eq("--quiet"), eq("--json-output"),
                    anyString())).thenAnswer(call -> {
                Files.writeString(Path.of(call.<String>getArgument(12)), json);
                return List.of();
            });
            return XcrunUtils.isIosDeviceAppInstalled("udid", "com.app");
        }
    }

    @Test
    void isIosDeviceAppInstalledFindsTheAppInTheList() {
        assertThat(installedWhenDeviceWrites(
                "{\"result\":{\"apps\":[{\"bundleIdentifier\" : \"com.app\",\"name\":\"App\"}]}}")).isTrue();
    }

    @Test
    void isIosDeviceAppInstalledIsFalseForAnEmptyList() {
        assertThat(installedWhenDeviceWrites("{\"result\":{\"apps\":[]}}")).isFalse();
    }

    @Test
    void isIosDeviceAppInstalledDoesNotTakeAnotherAppWhoseIdStartsTheSame() {
        assertThat(installedWhenDeviceWrites("{\"result\":{\"apps\":[{\"bundleIdentifier\":\"com.app.widget\"}]}}")).isFalse();
    }
}
