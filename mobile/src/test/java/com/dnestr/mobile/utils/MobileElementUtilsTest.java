package com.dnestr.mobile.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.openqa.selenium.remote.RemoteWebElement;
import io.appium.java_client.remote.AppiumWebElement;
import com.dnestr.mobile.context.TestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileElementUtilsTest {

    @Mock
    private WebElement element;
    @Mock
    private WebElement other;

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void centerOfIsTheMiddleOfTheBoundingBox() {
        when(element.getRect()).thenReturn(rect(100, 200, 50, 80));

        assertThat(MobileElementUtils.centerOf(element)).isEqualTo(new Point(125, 240));
    }

    @Test
    void getIdReadsResourceIdOnAndroidAndNameOnIos() {
        System.setProperty("platform", "ANDROID");
        when(element.getAttribute("resource-id")).thenReturn("doc-row-ssn");
        assertThat(MobileElementUtils.getId(element)).isEqualTo("doc-row-ssn");

        TestContext.clearPlatform();
        System.setProperty("platform", "IOS");
        when(element.getAttribute("name")).thenReturn("doc-row-tlc");
        assertThat(MobileElementUtils.getId(element)).isEqualTo("doc-row-tlc");
    }

    @Test
    void getIdIsEmptyWhenTheElementHasNone() {
        System.setProperty("platform", "ANDROID");

        assertThat(MobileElementUtils.getId(element)).isEmpty();
    }

    @Test
    void closestToCenterYPicksTheRowOnTheCenterLine() {
        when(element.getRect()).thenReturn(rect(0, 100, 50, 40));
        when(other.getRect()).thenReturn(rect(0, 180, 50, 40));

        assertThat(MobileElementUtils.closestToCenterY(List.of(element, other), rect(0, 100, 50, 200))).contains(other);
    }

    @Test
    void closestToCenterYOfNoRowsIsEmpty() {
        assertThat(MobileElementUtils.closestToCenterY(List.of(), rect(0, 0, 10, 10))).isEmpty();
    }

    @Test
    void childTextReadsTheFirstMatchUnderTheRoot() {
        System.setProperty("platform", "ANDROID");
        By price = By.id("price");
        when(element.findElements(price)).thenReturn(List.of(other));
        when(other.getText()).thenReturn(" $12.50 ");

        assertThat(MobileElementUtils.childText(element, price)).isEqualTo("$12.50");
    }

    @Test
    void childTextIsEmptyWhenNothingMatches() {
        when(element.findElements(By.id("price"))).thenReturn(List.of());

        assertThat(MobileElementUtils.childText(element, By.id("price"))).isEmpty();
    }

    @Test
    void childTextOnIosFallsBackToTheLabel() {
        System.setProperty("platform", "IOS");
        when(element.findElements(By.id("car"))).thenReturn(List.of(other));
        when(other.getAttribute("value")).thenReturn(null);
        when(other.getAttribute("label")).thenReturn("Toyota Camry");

        assertThat(MobileElementUtils.childText(element, By.id("car"))).isEqualTo("Toyota Camry");
    }

    @Test
    void replaceValueByBackspaceDeletesThenTypes() {
        MobileElementUtils.replaceValueByBackspace(element, 4, "15");

        InOrder order = inOrder(element);
        order.verify(element).click();
        order.verify(element).sendKeys(Keys.BACK_SPACE.toString().repeat(4));
        order.verify(element).sendKeys("15");
        verify(element, never()).clear();
    }

    private static Rectangle rect(int x, int y, int width, int height) {
        return new Rectangle(new Point(x, y), new Dimension(width, height));
    }

    @Test
    void referenceIdReadsTheIdOfBothDriverElementTypes() {
        AppiumWebElement appium = mock(AppiumWebElement.class);
        RemoteWebElement remote = mock(RemoteWebElement.class);
        when(appium.getId()).thenReturn("appium-id");
        when(remote.getId()).thenReturn("remote-id");

        assertThat(MobileElementUtils.referenceId(appium)).isEqualTo("appium-id");
        assertThat(MobileElementUtils.referenceId(remote)).isEqualTo("remote-id");
    }

    @Test
    void referenceIdRefusesAnElementNotFromADriver() {
        assertThatThrownBy(() -> MobileElementUtils.referenceId(element))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a driver element");
    }
}
