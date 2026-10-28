package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.locators.SystemLocators;
import com.dnestr.mobile.waits.DriverWait;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemActionsTest {

    @Mock
    private ElementActions elementActions;
    @Mock
    private DriverWait driverWait;
    @Mock
    private AppiumDriver driver;

    private SystemActions systemActions;

    @BeforeEach
    void setUp() {
        systemActions = new SystemActions(elementActions, driverWait, driver);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void tapAlertButtonOnAndroidClicksTheDialogButton() {
        System.setProperty("platform", "ANDROID");

        systemActions.tapAlertButton("Yes");

        verify(elementActions).click(SystemLocators.androidAlertButton("Yes"));
        verifyNoInteractions(driver);
    }

    @Test
    void tapAlertButtonOnIosWaitsForTheAlertThenAcceptsByLabel() {
        System.setProperty("platform", "IOS");

        systemActions.tapAlertButton("Yes");

        InOrder order = inOrder(driverWait, driver);
        order.verify(driverWait).waitForAlert();
        order.verify(driver).executeScript("mobile: alert", Map.of("action", "accept", "buttonLabel", "Yes"));
    }

    @Test
    void answerPermissionPromptOnAndroidUsesTheDialogButtons() {
        System.setProperty("platform", "ANDROID");

        systemActions.answerPermissionPrompt(true);
        systemActions.answerPermissionPrompt(false);

        verify(elementActions).click(SystemLocators.androidPermissionAllowButton());
        verify(elementActions).click(SystemLocators.androidPermissionDenyButton());
    }

    @Test
    void answerPermissionPromptOnIosPrefersWhileUsingTheApp() {
        System.setProperty("platform", "IOS");
        when(driver.executeScript("mobile: alert", Map.of("action", "getButtons")))
                .thenReturn(List.of("Allow Once", "Allow While Using App", "Don’t Allow"));

        systemActions.answerPermissionPrompt(true);

        verify(driver).executeScript("mobile: alert", Map.of("action", "accept", "buttonLabel", "Allow While Using App"));
    }

    @Test
    void answerPermissionPromptOnIosDeniesWithEitherApostrophe() {
        System.setProperty("platform", "IOS");
        when(driver.executeScript("mobile: alert", Map.of("action", "getButtons"))).thenReturn(List.of("Don't Allow", "OK"));

        systemActions.answerPermissionPrompt(false);

        verify(driver).executeScript("mobile: alert", Map.of("action", "accept", "buttonLabel", "Don't Allow"));
    }

    @Test
    void answerPermissionPromptOnIosNamesTheButtonsWhenNoneFits() {
        System.setProperty("platform", "IOS");
        when(driver.executeScript("mobile: alert", Map.of("action", "getButtons"))).thenReturn(List.of("Settings"));

        assertThatThrownBy(() -> systemActions.answerPermissionPrompt(true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No allow button among [Settings]");
    }

    @Test
    void tapNotificationOnAndroidOpensTheShadeFirst() {
        System.setProperty("platform", "ANDROID");

        systemActions.tapNotification("New ride");

        InOrder order = inOrder(driver, elementActions);
        order.verify(driver).executeScript("mobile: openNotifications", Map.of());
        order.verify(elementActions).click(SystemLocators.androidNotification("New ride"));
    }

    @Test
    void tapNotificationOnIosSwipesNotificationCenterDownFirst() {
        System.setProperty("platform", "IOS");
        WebDriver.Options options = mock(WebDriver.Options.class);
        WebDriver.Window window = mock(WebDriver.Window.class);
        when(driver.manage()).thenReturn(options);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(new Dimension(400, 800));

        systemActions.tapNotification("New ride");

        InOrder order = inOrder(driver, elementActions);
        order.verify(driver).perform(any());
        order.verify(elementActions).click(SystemLocators.iosNotification("New ride"));
        verify(driver, never()).executeScript(eq("mobile: openNotifications"), any());
    }

    @Test
    void takeAndroidEmulatorCameraPhotoShootsUntilDoneShowsThenConfirms() {
        System.setProperty("platform", "ANDROID");

        systemActions.takeAndroidEmulatorCameraPhoto(3);

        InOrder order = inOrder(elementActions);
        order.verify(elementActions).clickUntilVisible(SystemLocators.androidCameraShutterButton(),
                SystemLocators.androidCameraDoneButton(), 3);
        order.verify(elementActions).click(SystemLocators.androidCameraDoneButton());
    }

    @Test
    void takeAndroidEmulatorCameraPhotoRefusesIos() {
        System.setProperty("platform", "IOS");

        assertThatThrownBy(() -> systemActions.takeAndroidEmulatorCameraPhoto(3))
                .isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(elementActions);
    }
}
