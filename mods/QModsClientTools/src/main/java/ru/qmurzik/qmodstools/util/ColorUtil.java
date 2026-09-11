package ru.qmurzik.qmodstools.util;

public final class ColorUtil {
    private ColorUtil() {}
    public static int argb(int rgb, int alpha) { return ((alpha & 255) << 24) | (rgb & 0xFFFFFF); }
    public static float r(int rgb) { return ((rgb >> 16) & 255) / 255.0F; }
    public static float g(int rgb) { return ((rgb >> 8) & 255) / 255.0F; }
    public static float b(int rgb) { return (rgb & 255) / 255.0F; }
    public static int brighten(int rgb, int amount) {
        int r = Math.min(255, ((rgb >> 16) & 255) + amount);
        int g = Math.min(255, ((rgb >> 8) & 255) + amount);
        int b = Math.min(255, (rgb & 255) + amount);
        return (r << 16) | (g << 8) | b;
    }
}
