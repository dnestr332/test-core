package com.dnestr.mobile.appium;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.Socket;

import static com.dnestr.base.logs.LogStyles.*;

/**
 * Manages a local Appium server process (as opposed to connecting to an already-running/remote
 * one) — for a test run that needs to launch its own Appium instance rather than assume one is
 * already listening. Suppresses the server's own log output (redirected to a no-op stream) since
 * this module does its own step logging.
 */
@Slf4j
public class AppiumLocalServer {

    private AppiumDriverLocalService service;

    /** Starts a local Appium server on {@code port}, unless one is already listening there ({@link #isServerRunning}), in which case this is a no-op. */
    public void startLocalServer(int port) {
        if (isServerRunning(port)) {
            log.info("{} Appium server already running on {}", INFO_SHORT, port);
            return;
        }

        log.info("{} Starting Appium server on {}…", INFO_SHORT, port);
        service = AppiumDriverLocalService.buildService(
                new AppiumServiceBuilder()
                        .withIPAddress("127.0.0.1")
                        .usingPort(port)
                        .withArgument(() -> "--log-level", "error")
                        .withLogOutput(new OutputStream() {
                            @Override
                            public void write(int b) {
                            }
                        })
        );

        service.start();
        log.info("{} Appium server started on {}", OK_SHORT, port);
    }

    /** Stops the server started by {@link #startLocalServer}, if one is running; otherwise does nothing. */
    public void stopLocalServer() {
        if (service != null && service.isRunning()) {
            log.info("{} Stopping Appium server…", INFO_SHORT);
            service.stop();
            log.info("{} Appium server stopped{}", OK_SHORT, RESET);
        }
    }

    /** Whether something is already listening on {@code 127.0.0.1:port}, checked via a raw TCP connect attempt. */
    public boolean isServerRunning(int port) {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
