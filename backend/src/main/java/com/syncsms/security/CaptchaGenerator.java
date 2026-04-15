package com.syncsms.security;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * @description 简易验证码生成器（PNG base64）
 */
public class CaptchaGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String randomCode(int len) {
        Random r = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static String renderPngBase64(String code) {
        int w = 140;
        int h = 44;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 251));
            g.fillRect(0, 0, w, h);

            // noise lines
            Random r = new Random();
            for (int i = 0; i < 8; i++) {
                g.setColor(new Color(r.nextInt(180), r.nextInt(180), r.nextInt(180)));
                int x1 = r.nextInt(w);
                int y1 = r.nextInt(h);
                int x2 = r.nextInt(w);
                int y2 = r.nextInt(h);
                g.drawLine(x1, y1, x2, y2);
            }

            g.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g.getFontMetrics();
            int totalW = fm.stringWidth(code);
            int startX = (w - totalW) / 2;
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();

            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                g.setColor(new Color(30 + r.nextInt(120), 30 + r.nextInt(120), 30 + r.nextInt(120)));
                int x = startX + fm.charWidth(c) * i;
                int y = baseline + (r.nextInt(7) - 3);
                g.drawString(String.valueOf(c), x, y);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败");
        }
    }
}

