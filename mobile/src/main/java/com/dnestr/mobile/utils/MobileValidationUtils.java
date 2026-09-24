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

    /**
     * Whether {@code element} is enabled and interactable, platform-appropriately: on iOS, the
     * {@code enabled} accessibility attribute alone; on Android, both {@code enabled} AND
     * {@code clickable} (a native Android view can report enabled while still not being tappable,
     * e.g. it's covered or its clickable flag is explicitly off). Returns {@code false} rather than
     * throwing if the element can't be inspected (e.g. it went stale mid-check).
     */
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

    /**
     * Whether two screenshots (raw image bytes) look meaningfully different: both are downscaled to
     * a 64×64 thumbnail, then compared by mean-squared pixel error normalized to {@code [0, 1]}
     * against {@code threshold} — a coarse perceptual diff, not a pixel-exact comparison, so minor
     * rendering noise (anti-aliasing, animation timing) doesn't register as a difference. Returns
     * {@code false} (not different) if either image fails to decode, or on any other error.
     */
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
