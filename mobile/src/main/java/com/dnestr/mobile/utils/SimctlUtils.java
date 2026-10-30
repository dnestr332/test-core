package com.dnestr.mobile.utils;

import com.dnestr.base.utils.ProcessUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** iOS simulator control through {@code xcrun simctl}, which needs Xcode's command-line tools. */
public final class SimctlUtils {

    private static final Pattern RUNTIME = Pattern.compile("^-- iOS ([\\d.]+) --$");
    private static final Pattern BOOTED = Pattern.compile("^\\s+(.+?) \\(([0-9A-F-]{36})\\) \\(Booted\\)\\s*$");
    private static final Pattern SHORT_VERSION = Pattern.compile("CFBundleShortVersionString\\s*=\\s*\"?([^\";]+)\"?;");

    private SimctlUtils() {}

    /** A simulator as {@code simctl} lists it, with the iOS version of its runtime. */
    public record IosSimulator(String udid, String name, String platformVersion) {}

    /** The booted iOS simulators, with the iOS version of the runtime each runs. */
    public static List<IosSimulator> getBootedIosSimulators() {
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
    public static Optional<String> getIosSimulatorAppVersion(String udid, String bundleId) {
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
    public static void sendIosSimulatorPush(String udid, String bundleId, String payloadJson) {
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

    private static void deleteQuietly(Path file) {
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // a leftover temp file is harmless
        }
    }
}
