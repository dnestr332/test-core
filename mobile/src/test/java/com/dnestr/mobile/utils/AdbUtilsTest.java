package com.dnestr.mobile.utils;

import com.dnestr.base.utils.ProcessUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/** adb itself is replaced: each test fixes what a command prints and checks the exact command line that was run. */
class AdbUtilsTest {

    private MockedStatic<ProcessUtils> process;

    @BeforeEach
    void setUp() {
        process = mockStatic(ProcessUtils.class);
    }

    @AfterEach
    void tearDown() {
        process.close();
    }

    private void prints(List<String> output, String... command) {
        process.when(() -> ProcessUtils.run(command)).thenReturn(output);
    }

    @Test
    void getAndroidDeviceUdidsListsOnlineDevicesOnly() {
        prints(List.of("List of devices attached", "emulator-5554\tdevice", "R58M\toffline", "emulator-5556\tdevice", ""),
                "adb", "devices");

        assertThat(AdbUtils.getAndroidDeviceUdids()).containsExactly("emulator-5554", "emulator-5556");
    }

    @Test
    void getAndroidEmulatorUdidsLeavesPhonesOut() {
        prints(List.of("emulator-5554\tdevice", "R58M\tdevice"), "adb", "devices");

        assertThat(AdbUtils.getAndroidEmulatorUdids()).containsExactly("emulator-5554");
    }

    @Test
    void getSoleAndroidDeviceUdidIsEmptyWithTwoDevices() {
        prints(List.of("emulator-5554\tdevice", "emulator-5556\tdevice"), "adb", "devices");

        assertThat(AdbUtils.getSoleAndroidDeviceUdid()).isEmpty();
    }

    @Test
    void getSoleAndroidDeviceUdidNamesTheOnlyDevice() {
        prints(List.of("emulator-5554\tdevice"), "adb", "devices");

        assertThat(AdbUtils.getSoleAndroidDeviceUdid()).contains("emulator-5554");
    }

    @Test
    void runAndroidShellTargetsTheDeviceByUdid() {
        prints(List.of("14"), "adb", "-s", "emulator-5554", "shell", "getprop", "ro.build.version.release");

        assertThat(AdbUtils.getAndroidPlatformVersion("emulator-5554")).contains("14");
    }

    @Test
    void getDeniedAndroidPermissionsReadsEachDeniedPermissionOnce() {
        prints(List.of(
                "  android.permission.CAMERA: granted=false",
                "  android.permission.ACCESS_FINE_LOCATION: granted=true",
                "  android.permission.CAMERA: granted=false, flags=[ USER_SET ]",
                "  android.permission.POST_NOTIFICATIONS: granted=false"
        ), "adb", "-s", "d", "shell", "dumpsys", "package", "com.app");

        assertThat(AdbUtils.getDeniedAndroidPermissions("d", "com.app"))
                .containsExactly("android.permission.CAMERA", "android.permission.POST_NOTIFICATIONS");
    }

    @Test
    void grantDeniedAndroidPermissionsReturnsOnlyTheOnesAdbAccepted() {
        prints(List.of("  android.permission.CAMERA: granted=false", "  android.permission.READ_LOGS: granted=false"),
                "adb", "-s", "d", "shell", "dumpsys", "package", "com.app");
        prints(List.of(), "adb", "-s", "d", "shell", "pm", "grant", "com.app", "android.permission.CAMERA");
        prints(List.of("Exception: not a changeable permission type"),
                "adb", "-s", "d", "shell", "pm", "grant", "com.app", "android.permission.READ_LOGS");

        assertThat(AdbUtils.grantDeniedAndroidPermissions("d", "com.app")).containsExactly("android.permission.CAMERA");
    }

    @Test
    void setAndroidPermissionFlagsPassesEveryFlag() {
        AdbUtils.setAndroidPermissionFlags("d", "com.app", "android.permission.CAMERA", "user-set", "user-fixed");

        process.verify(() -> ProcessUtils.run("adb", "-s", "d", "shell", "pm", "set-permission-flags", "com.app",
                "android.permission.CAMERA", "user-set", "user-fixed"));
    }

    @Test
    void getAndroidForegroundPackageReadsTheResumedActivity() {
        prints(List.of("  Display #0", "    topResumedActivity=ActivityRecord{1c u0 com.android.camera2/.CameraActivity t12}"),
                "adb", "-s", "d", "shell", "dumpsys", "activity", "activities");

        assertThat(AdbUtils.getAndroidForegroundPackage("d")).isEqualTo("com.android.camera2");
    }

    @Test
    void getAndroidAppVersionNameReadsVersionName() {
        prints(List.of("    versionCode=105 minSdk=24", "    versionName=6.2.1"), "adb", "-s", "d", "shell", "dumpsys", "package", "com.app");

        assertThat(AdbUtils.getAndroidAppVersionName("d", "com.app")).isEqualTo("6.2.1");
    }

    @Test
    void getAndroidAppVersionNameOfAMissingAppThrows() {
        prints(List.of("Unable to find package: com.app"), "adb", "-s", "d", "shell", "dumpsys", "package", "com.app");

        assertThatThrownBy(() -> AdbUtils.getAndroidAppVersionName("d", "com.app"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("com.app is not installed");
    }

    @Test
    void timeZoneIsReadFromItsPropertyAndSetThroughTheAlarmService() {
        prints(List.of("America/New_York"), "adb", "-s", "d", "shell", "getprop", "persist.sys.timezone");

        assertThat(AdbUtils.getAndroidTimeZone("d")).contains("America/New_York");
        AdbUtils.setAndroidTimeZone("d", "Asia/Tokyo");

        process.verify(() -> ProcessUtils.run("adb", "-s", "d", "shell", "service", "call", "alarm", "3", "s16", "Asia/Tokyo"));
    }

    @Test
    void setAndroidEmulatorGeoFixPassesLongitudeBeforeLatitude() {
        AdbUtils.setAndroidEmulatorGeoFix("emulator-5554", 40.6, -73.9);

        process.verify(() -> ProcessUtils.run("adb", "-s", "emulator-5554", "emu", "geo", "fix", "-73.9", "40.6"));
    }

    @Test
    void clearAndroidUiAutomator2ServerStopsBothPackagesEvenWhenOneFails() {
        process.when(() -> ProcessUtils.run("adb", "-s", "d", "shell", "am", "force-stop", "io.appium.uiautomator2.server"))
                .thenThrow(new IllegalStateException("device offline"));

        AdbUtils.clearAndroidUiAutomator2Server("d");

        process.verify(() -> ProcessUtils.run("adb", "-s", "d", "shell", "am", "force-stop", "io.appium.uiautomator2.server.test"));
    }

    @Test
    void isAndroidBootCompletedIsFalseWhileTheDeviceIsUnreachable() {
        process.when(() -> ProcessUtils.run("adb", "-s", "d", "shell", "getprop", "sys.boot_completed"))
                .thenThrow(new IllegalStateException("device not found"));

        assertThat(AdbUtils.isAndroidBootCompleted("d")).isFalse();
    }

    @Test
    void rebootAndroidDeviceWaitsUntilTheDeviceWentDownAndBootedAgain() {
        prints(List.of(), "adb", "-s", "d", "shell", "reboot");
        process.when(() -> ProcessUtils.run("adb", "-s", "d", "shell", "getprop", "sys.boot_completed"))
                .thenReturn(List.of("1"))
                .thenThrow(new IllegalStateException("device offline"))
                .thenReturn(List.of("1"));

        AdbUtils.rebootAndroidDevice("d", Duration.ofSeconds(30));

        process.verify(() -> ProcessUtils.run("adb", "-s", "d", "shell", "getprop", "sys.boot_completed"), times(3));
    }

    @Test
    void rebootAndroidDeviceThrowsWhenTheDeviceDoesNotComeBack() {
        process.when(() -> ProcessUtils.run(any(String[].class))).thenReturn(List.of("1"));

        assertThatThrownBy(() -> AdbUtils.rebootAndroidDevice("d", Duration.ZERO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not restart");
    }
}
