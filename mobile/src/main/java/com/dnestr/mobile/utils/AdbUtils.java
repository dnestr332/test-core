package com.dnestr.mobile.utils;

import com.dnestr.base.utils.ProcessUtils;
import com.dnestr.base.utils.WaitUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Android device control through {@code adb}, which must be on the {@code PATH}. It works without an Appium session,
 * so it also covers setup before one starts and a device that just rebooted. Every call names the device by udid:
 * adb refuses to pick one itself when several are attached.
 */
@Slf4j
public final class AdbUtils {

    private static final Pattern DENIED_PERMISSION = Pattern.compile("(android\\.permission\\.[A-Z_]+): granted=false");
    private static final Pattern RESUMED_ACTIVITY = Pattern.compile("(?:topResumedActivity|mResumedActivity).* u0 ([\\w.]+)/");
    private static final List<String> UIAUTOMATOR2_PACKAGES = List.of("io.appium.uiautomator2.server", "io.appium.uiautomator2.server.test");

    private AdbUtils() {}

    //region DEVICES
    /** The udids of the attached Android devices and emulators that are online. */
    public static List<String> getAndroidDeviceUdids() {
        return ProcessUtils.run("adb", "devices").stream()
                .map(String::trim)
                .filter(line -> line.matches("\\S+\\s+device"))
                .map(line -> line.split("\\s+")[0])
                .toList();
    }

    /** The online Android emulators' udids. */
    public static List<String> getAndroidEmulatorUdids() {
        return getAndroidDeviceUdids().stream().filter(udid -> udid.startsWith("emulator-")).toList();
    }

    /** The only attached Android device, when there is exactly one. */
    public static Optional<String> getSoleAndroidDeviceUdid() {
        List<String> devices = getAndroidDeviceUdids();
        return devices.size() == 1 ? Optional.of(devices.getFirst()) : Optional.empty();
    }

    /** The device's Android version, e.g. {@code 14}. */
    public static Optional<String> getAndroidPlatformVersion(String udid) {
        return firstNonBlank(runAndroidShell(udid, "getprop", "ro.build.version.release"));
    }
    //endregion

    //region COMMANDS
    /** Runs {@code adb -s <udid> shell <args>} and returns its output lines. */
    public static List<String> runAndroidShell(String udid, String... args) {
        return runOnAndroidDevice(udid, "shell", args);
    }

    /** Runs {@code adb -s <udid> emu <args>} on the emulator console, which works while no Appium session does. */
    public static List<String> runAndroidEmulatorConsole(String udid, String... args) {
        return runOnAndroidDevice(udid, "emu", args);
    }

    private static List<String> runOnAndroidDevice(String udid, String subcommand, String... args) {
        List<String> command = new ArrayList<>(List.of("adb", "-s", udid, subcommand));
        command.addAll(List.of(args));
        return ProcessUtils.run(command.toArray(String[]::new));
    }
    //endregion

    //region PERMISSIONS
    /** Grants a runtime permission to the app, as if the user had allowed it. */
    public static void grantAndroidPermission(String udid, String appPackage, String permission) {
        runAndroidShell(udid, "pm", "grant", appPackage, permission);
    }

    /** Revokes a runtime permission from the app; Android kills the app if it is running. */
    public static void revokeAndroidPermission(String udid, String appPackage, String permission) {
        runAndroidShell(udid, "pm", "revoke", appPackage, permission);
    }

    /** Sets permission flags, e.g. {@code user-set} and {@code user-fixed}, which a "don't ask again" refusal sets. */
    public static void setAndroidPermissionFlags(String udid, String appPackage, String permission, String... flags) {
        runAndroidShell(udid, withArgs(List.of("pm", "set-permission-flags", appPackage, permission), flags));
    }

    /** Clears permission flags set by {@link #setAndroidPermissionFlags}, so the app may ask for the permission again. */
    public static void clearAndroidPermissionFlags(String udid, String appPackage, String permission, String... flags) {
        runAndroidShell(udid, withArgs(List.of("pm", "clear-permission-flags", appPackage, permission), flags));
    }

    /** The runtime permissions the app declares but does not hold. */
    public static List<String> getDeniedAndroidPermissions(String udid, String appPackage) {
        List<String> denied = new ArrayList<>();
        for (String line : runAndroidShell(udid, "dumpsys", "package", appPackage)) {
            Matcher matcher = DENIED_PERMISSION.matcher(line);
            if (matcher.find() && !denied.contains(matcher.group(1))) {
                denied.add(matcher.group(1));
            }
        }
        return denied;
    }

    /**
     * Grants every runtime permission the app does not hold and returns the ones granted; one adb refuses is logged and
     * skipped. Appium's {@code autoGrantPermissions} does not re-grant after the data wipe it performs each session
     * with {@code noReset=false}, which leaves a permission dialog owning the window.
     */
    public static List<String> grantDeniedAndroidPermissions(String udid, String appPackage) {
        List<String> granted = new ArrayList<>();
        for (String permission : getDeniedAndroidPermissions(udid, appPackage)) {
            String error = String.join(" ", runAndroidShell(udid, "pm", "grant", appPackage, permission)).trim();
            if (error.isEmpty()) {
                granted.add(permission);
            } else {
                log.warn("Could not grant {}: {}", permission, error);
            }
        }
        return granted;
    }
    //endregion

    //region APPS
    /** The package of the app in the foreground, read from the resumed activity; empty when none is resumed. */
    public static String getAndroidForegroundPackage(String udid) {
        for (String line : runAndroidShell(udid, "dumpsys", "activity", "activities")) {
            Matcher matcher = RESUMED_ACTIVITY.matcher(line);
            if (matcher.find()) return matcher.group(1);
        }
        return "";
    }

    /**
     * The installed app's version name.
     *
     * @throws IllegalStateException if the app is not installed
     */
    public static String getAndroidAppVersionName(String udid, String appPackage) {
        return runAndroidShell(udid, "dumpsys", "package", appPackage).stream()
                .map(String::trim)
                .filter(line -> line.startsWith("versionName="))
                .map(line -> line.substring("versionName=".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(appPackage + " is not installed on " + udid));
    }

    /** Stops the app and its background work at once, without the app being told. */
    public static void forceStopAndroidApp(String udid, String appPackage) {
        runAndroidShell(udid, "am", "force-stop", appPackage);
    }

    /**
     * Force-stops a wedged UiAutomator2 server; Appium starts a fresh one with the next session. Failures are only
     * logged, since this runs on a device that is already misbehaving.
     */
    public static void clearAndroidUiAutomator2Server(String udid) {
        for (String appPackage : UIAUTOMATOR2_PACKAGES) {
            try {
                forceStopAndroidApp(udid, appPackage);
            } catch (RuntimeException e) {
                log.warn("Could not force-stop {}: {}", appPackage, e.getMessage());
            }
        }
    }
    //endregion

    //region DEVICE STATE
    /** Turns the device's location services on or off. */
    public static void setAndroidLocationServices(String udid, boolean enabled) {
        runAndroidShell(udid, "cmd", "location", "set-location-enabled", String.valueOf(enabled));
    }

    /** The device's time zone id, e.g. {@code America/New_York}; empty when it has none set. */
    public static Optional<String> getAndroidTimeZone(String udid) {
        return firstNonBlank(runAndroidShell(udid, "getprop", "persist.sys.timezone"));
    }

    /** Sets the device's time zone, e.g. {@code America/New_York}, as the system settings would. */
    public static void setAndroidTimeZone(String udid, String zoneId) {
        runAndroidShell(udid, "service", "call", "alarm", "3", "s16", zoneId);
    }

    /** Moves an emulator's GPS fix through its console, so it works with no Appium session. */
    public static void setAndroidEmulatorGeoFix(String udid, double latitude, double longitude) {
        runAndroidEmulatorConsole(udid, "geo", "fix", String.valueOf(longitude), String.valueOf(latitude));
    }

    /**
     * Reboots the device and waits until it has gone down and finished booting again. An Appium session on it does
     * not survive.
     *
     * @throws IllegalStateException if it is not back within {@code timeout}
     */
    public static void rebootAndroidDevice(String udid, Duration timeout) {
        try {
            runAndroidShell(udid, "reboot");
        } catch (IllegalStateException droppedWithTheDevice) {
            log.info("adb returned no result for the reboot, as the device went away: {}", droppedWithTheDevice.getMessage());
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        boolean wentDown = false;
        while (System.nanoTime() < deadline) {
            boolean booted = isAndroidBootCompleted(udid);
            if (wentDown && booted) return;
            wentDown = wentDown || !booted;
            WaitUtils.sleepSeconds(3);
        }
        throw new IllegalStateException("%s did not restart within %s".formatted(udid, timeout));
    }

    /** Whether the device reports its boot as complete; {@code false} while adb cannot reach it. */
    public static boolean isAndroidBootCompleted(String udid) {
        try {
            return firstNonBlank(runAndroidShell(udid, "getprop", "sys.boot_completed")).filter("1"::equals).isPresent();
        } catch (IllegalStateException deviceAway) {
            return false;
        }
    }
    //endregion

    private static Optional<String> firstNonBlank(List<String> lines) {
        return lines.stream().map(String::trim).filter(line -> !line.isEmpty()).findFirst();
    }

    private static String[] withArgs(List<String> head, String... tail) {
        List<String> args = new ArrayList<>(head);
        args.addAll(List.of(tail));
        return args.toArray(String[]::new);
    }
}
