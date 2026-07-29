package com.dnestr.mobile.appium;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.Socket;

import static com.dnestr.core.logs.LogStyles.*;

@Slf4j
public class AppiumLocalServer {

    private AppiumDriverLocalService service;

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

    public void stopLocalServer() {
        if (service != null && service.isRunning()) {
            log.info("{} Stopping Appium server…", INFO_SHORT);
            service.stop();
            log.info("{} Appium server stopped{}", OK_SHORT, RESET);
        }
    }

    public boolean isServerRunning(int port) {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
