package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class MobileValidationUtils {

    private MobileValidationUtils() {}

    public static boolean isButtonEnabled(WebElement element) {
        try {
            if (TestContext.isIos()) {
                return Boolean.parseBoolean(element.getAttribute("enabled"));
            } else {
                String enabled = element.getAttribute("enabled");
                String clickable = element.getAttribute("clickable");

                boolean isEnabled = "true".equalsIgnoreCase(enabled);
                boolean isClickable = "true".equalsIgnoreCase(clickable);

                return isEnabled && isClickable;
            }
        } catch (WebDriverException e) {
            return false;
        }
    }

    public static boolean areImagesDifferent(byte[] a, byte[] b, double threshold) {
        try {
            BufferedImage ia = ImageIO.read(new ByteArrayInputStream(a));
            BufferedImage ib = ImageIO.read(new ByteArrayInputStream(b));
            if (ia == null || ib == null) return false;

            int w = 64, h = 64;
            BufferedImage ra = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            BufferedImage rb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

            Graphics2D g1 = ra.createGraphics();
            g1.drawImage(ia, 0, 0, w, h, null);
            g1.dispose();
            Graphics2D g2 = rb.createGraphics();
            g2.drawImage(ib, 0, 0, w, h, null);
            g2.dispose();

            long sumSq = 0L;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int ca = ra.getRGB(x, y);
                    int cb = rb.getRGB(x, y);
                    int ar = (ca >> 16) & 255, ag = (ca >> 8) & 255, ab = ca & 255;
                    int br = (cb >> 16) & 255, bg = (cb >> 8) & 255, bb = cb & 255;
                    int dr = ar - br, dg = ag - bg, db = ab - bb;
                    sumSq += (long) dr * dr + (long) dg * dg + (long) db * db;
                }
            }

            double mse = sumSq / (double) (w * h * 3 * 255 * 255);
            return mse > threshold;
        } catch (Exception e) {
            return false;
        }
    }
}
