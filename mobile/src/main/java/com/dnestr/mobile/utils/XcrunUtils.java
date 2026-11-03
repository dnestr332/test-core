package com.dnestr.mobile.utils;

import com.dnestr.base.utils.ProcessUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * iOS device state without an Appium session, through Xcode's command-line tools: {@code xcrun simctl} for a
 * simulator, {@code xcrun devicectl} (Xcode 15 or later) for a real iPhone.
 */
@UtilityClass
public class XcrunUtils {

    private final Pattern RUNTIME = Pattern.compile("^-- iOS ([\\d.]+) --$");
    private final Pattern BOOTED = Pattern.compile("^\\s+(.+?) \\(([0-9A-F-]{36})\\) \\(Booted\\)\\s*$");
    private final Duration BOOT_TIMEOUT = Duration.ofMinutes(3);
    private final Duration DEVICECTL_TIMEOUT = Duration.ofSeconds(60);
    private final Pattern SHORT_VERSION = Pattern.compile("CFBundleShortVersionString\\s*=\\s*\"?([^\";]+)\"?;");

    /** A simulator as {@code simctl} lists it, with the iOS version of its runtime. */
    public record IosSimulator(String udid, String name, String platformVersion) {}

    /** The booted iOS simulators, with the iOS version of the runtime each runs. */
    public List<IosSimulator> getBootedIosSimulators() {
        List<IosSimulator> simulators = new ArrayList<>();
        String version = null;
        for (String line : ProcessUtils.run("xcrun", "simctl", "list", "devices")) {
            if (line.startsWith("--")) {
                Matcher runtime = RUNTIME.matcher(line.trim());
                version = runtime.matches() ? runtime.group(1) : null;
                continue;
            }
            Matcher device = BOOTED.matcher(line);
            if (version != null && device.matches()) {
                simulators.add(new IosSimulator(device.group(2), device.group(1), version));
            }
        }
        return simulators;
    }

    /** The version ({@code CFBundleShortVersionString}) of the app installed on the simulator; empty when it is not installed. */
    public Optional<String> getIosSimulatorAppVersion(String udid, String bundleId) {
        for (String line : ProcessUtils.run("xcrun", "simctl", "appinfo", udid, bundleId)) {
            Matcher version = SHORT_VERSION.matcher(line);
            if (version.find()) return Optional.of(version.group(1).trim());
        }
        return Optional.empty();
    }

    /**
     * Delivers a push notification to the app on the simulator, as APNs would. {@code payloadJson} is the APNs payload,
     * e.g. {@code {"aps":{"alert":{"title":"Documents","body":"Please review"}}}}.
     *
     * @throws IllegalStateException if simctl does not report the notification as sent
     */
    public void sendIosSimulatorPush(String udid, String bundleId, String payloadJson) {
        Path payload = null;
        try {
            payload = Files.createTempFile("push", ".apns");
            Files.writeString(payload, payloadJson, StandardCharsets.UTF_8);
            List<String> output = ProcessUtils.run("xcrun", "simctl", "push", udid, bundleId, payload.toString());
            if (output.stream().noneMatch(line -> line.contains("Notification sent"))) {
                throw new IllegalStateException("simctl did not send the push to %s: %s".formatted(bundleId, String.join(" ", output)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not write the push payload", e);
        } finally {
            deleteQuietly(payload);
        }
    }

    /** Shuts the simulator down and boots it again, waiting until it has booted. An Appium session on it does not survive. */
    public void rebootIosSimulator(String udid) {
        ProcessUtils.run("xcrun", "simctl", "shutdown", udid);
        ProcessUtils.run("xcrun", "simctl", "boot", udid);
        ProcessUtils.run(BOOT_TIMEOUT, "xcrun", "simctl", "bootstatus", udid, "-b");
    }

    /** Moves the simulator's simulated location, as the Features > Location menu does. */
    public void setIosSimulatorLocation(String udid, double latitude, double longitude) {
        ProcessUtils.run("xcrun", "simctl", "location", udid, "set", String.format(Locale.ROOT, "%f,%f", latitude, longitude));
    }

    /** Grants the app a privacy service, e.g. {@code location} or {@code location-always}, with no system prompt. */
    public void grantIosSimulatorPrivacy(String udid, String service, String bundleId) {
        ProcessUtils.run("xcrun", "simctl", "privacy", udid, "grant", service, bundleId);
    }

    public void revokeIosSimulatorPrivacy(String udid, String service, String bundleId) {
        ProcessUtils.run("xcrun", "simctl", "privacy", udid, "revoke", service, bundleId);
    }

    /**
     * Leaves the app signed out with no stored data, as an Android data clear does: its data container is emptied and
     * the keychain, where an app keeps its session, is reset. Call with the app terminated; privacy grants are kept.
     *
     * @throws IllegalStateException if the app has no data container or the keychain reset reports an error
     */
    public void clearIosSimulatorAppData(String udid, String bundleId) {
        List<String> output = ProcessUtils.run("xcrun", "simctl", "get_app_container", udid, bundleId, "data");
        Path container = output.isEmpty() ? null : Path.of(output.getFirst().strip());
        if (container == null || !Files.isDirectory(container)) {
            throw new IllegalStateException("No data container for %s on %s: %s".formatted(bundleId, udid, output));
        }
        for (String dir : List.of("Documents", "Library", "tmp")) {
            deleteContents(container.resolve(dir));
        }
        List<String> reset = ProcessUtils.run("xcrun", "simctl", "keychain", udid, "reset");
        if (reset.stream().anyMatch(line -> !line.isBlank())) {
            throw new IllegalStateException("Could not reset the keychain on %s: %s".formatted(udid, reset));
        }
    }

    private void deleteContents(Path dir) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(dir)).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not empty " + dir, e);
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // a leftover temp file is harmless
        }
    }

    /** Whether {@code bundleId} is installed on the real iPhone {@code udid}. @throws IllegalStateException if devicectl's answer cannot be read */
    public boolean isIosDeviceAppInstalled(String udid, String bundleId) {
        Path json = null;
        try {
            json = Files.createTempFile("devicectl-apps", ".json");
            ProcessUtils.run(DEVICECTL_TIMEOUT, "xcrun", "devicectl", "device", "info", "apps",
                    "--device", udid, "--bundle-id", bundleId, "--quiet", "--json-output", json.toString());
            return Pattern.compile("\"bundleIdentifier\"\\s*:\\s*\"" + Pattern.quote(bundleId) + "\"")
                    .matcher(Files.readString(json)).find();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the apps devicectl lists on " + udid, e);
        } finally {
            deleteQuietly(json);
        }
    }
}
