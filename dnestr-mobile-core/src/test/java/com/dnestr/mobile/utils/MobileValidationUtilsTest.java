package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileValidationUtilsTest {

    @Mock
    private WebElement element;

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void isButtonEnabledIosReadsEnabledAttribute() {
        System.setProperty("platform", "IOS");
        when(element.getAttribute("enabled")).thenReturn("true");

        assertThat(MobileValidationUtils.isButtonEnabled(element)).isTrue();
    }

    @Test
    void isButtonEnabledIosFalseWhenAttributeFalse() {
        System.setProperty("platform", "IOS");
        when(element.getAttribute("enabled")).thenReturn("false");

        assertThat(MobileValidationUtils.isButtonEnabled(element)).isFalse();
    }

    @Test
    void isButtonEnabledAndroidRequiresBothEnabledAndClickable() {
        System.setProperty("platform", "ANDROID");
        when(element.getAttribute("enabled")).thenReturn("true");
        when(element.getAttribute("clickable")).thenReturn("false");

        assertThat(MobileValidationUtils.isButtonEnabled(element)).isFalse();
    }

    @Test
    void isButtonEnabledAndroidTrueWhenBothFlagsTrue() {
        System.setProperty("platform", "ANDROID");
        when(element.getAttribute("enabled")).thenReturn("true");
        when(element.getAttribute("clickable")).thenReturn("true");

        assertThat(MobileValidationUtils.isButtonEnabled(element)).isTrue();
    }

    @Test
    void isButtonEnabledReturnsFalseOnWebDriverException() {
        System.setProperty("platform", "ANDROID");
        when(element.getAttribute("enabled")).thenThrow(new WebDriverException("session gone"));

        assertThat(MobileValidationUtils.isButtonEnabled(element)).isFalse();
    }

    @Test
    void areImagesDifferentTrueForDistinctSolidColors() throws IOException {
        byte[] red = solidColorPng(Color.RED);
        byte[] blue = solidColorPng(Color.BLUE);

        assertThat(MobileValidationUtils.areImagesDifferent(red, blue, 0.01)).isTrue();
    }

    @Test
    void areImagesDifferentFalseForIdenticalImages() throws IOException {
        byte[] green = solidColorPng(Color.GREEN);

        assertThat(MobileValidationUtils.areImagesDifferent(green, green, 0.01)).isFalse();
    }

    @Test
    void areImagesDifferentFalseWhenThresholdNotExceeded() throws IOException {
        byte[] a = solidColorPng(new Color(100, 100, 100));
        byte[] b = solidColorPng(new Color(101, 100, 100));

        assertThat(MobileValidationUtils.areImagesDifferent(a, b, 0.5)).isFalse();
    }

    @Test
    void areImagesDifferentFalseOnUnreadableImageBytes() {
        byte[] garbage = new byte[]{1, 2, 3, 4};

        assertThat(MobileValidationUtils.areImagesDifferent(garbage, garbage, 0.01)).isFalse();
    }

    private static byte[] solidColorPng(Color color) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 64, 64);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
