package ru.qmurzik.qmodstools.util;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

public final class DrawUtil {
    private static final GradientGui GRADIENT = new GradientGui();
    private DrawUtil() {}

    public static void rect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void gradient(int left, int top, int right, int bottom, int topColor, int bottomColor) {
        GRADIENT.draw(left, top, right, bottom, topColor, bottomColor);
    }

    public static void rounded(int left, int top, int right, int bottom, int radius, int color) {
        if (right <= left || bottom <= top) return;
        radius = Math.max(1, Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2)));
        rect(left + radius, top, right - radius, bottom, color);
        rect(left, top + radius, right, bottom - radius, color);
        for (int y = 0; y < radius; y++) {
            double dy = radius - y - 0.5;
            int dx = radius - (int)Math.sqrt(Math.max(0, radius * radius - dy * dy));
            rect(left + dx, top + y, right - dx, top + y + 1, color);
            rect(left + dx, bottom - y - 1, right - dx, bottom - y, color);
        }
    }

    public static void shadow(int left, int top, int right, int bottom, int radius) {
        for (int i = radius; i > 0; i--) {
            int a = Math.max(2, 18 - i * 3);
            rounded(left - i, top - i, right + i, bottom + i, Math.max(3, radius), a << 24);
        }
    }

    public static void beginHudScale(float scale, float x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
    }

    public static void endHudScale() { GlStateManager.popMatrix(); }

    private static final class GradientGui extends Gui {
        void draw(int left,int top,int right,int bottom,int topColor,int bottomColor){drawGradientRect(left,top,right,bottom,topColor,bottomColor);}
    }
}
