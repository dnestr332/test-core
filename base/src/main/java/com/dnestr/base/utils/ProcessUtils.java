package com.dnestr.base.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a command-line tool (adb, xcrun) and returns what it printed. */
public final class ProcessUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);

    private ProcessUtils() {}

    /** {@link #run(Duration, String...)} with a 20 second timeout. */
    public static List<String> run(String... command) {
        return run(DEFAULT_TIMEOUT, command);
    }

    /**
     * Runs {@code command} and returns its output lines, standard error merged into standard output. The exit code is
     * not checked: tools such as adb report failures in their output. The output goes to a temporary file, so a tool
     * that hangs with its output still open is timed out too.
     *
     * @throws IllegalStateException if the tool cannot be started, does not finish within {@code timeout} (it is then
     *                               killed), or the thread is interrupted
     */
    public static List<String> run(Duration timeout, String... command) {
        String commandLine = String.join(" ", command);
        Path output = null;
        try {
            output = Files.createTempFile("process-output", ".txt");
            Process process;
            try {
                process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            } catch (IOException e) {
                throw new IllegalStateException("Could not run `%s`. Is %s on PATH?".formatted(commandLine, command[0]), e);
            }
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("`%s` timed out after %s".formatted(commandLine, timeout));
            }
            return Files.readAllLines(output, StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted running `%s`".formatted(commandLine), e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the output of `%s`".formatted(commandLine), e);
        } finally {
            deleteQuietly(output);
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
