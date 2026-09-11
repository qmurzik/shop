package ru.qmurzik.qmodstools.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.feature.CpsCounter;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

public final class InfoHud {
    private final Minecraft mc;
    private final CpsCounter cps;

    public InfoHud(Minecraft mc, CpsCounter cps) { this.mc = mc; this.cps = cps; }

    public void render() {
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        ScaledResolution sr = new ScaledResolution(mc); int y = 6;
        if (QModsTools.config.pingHud) {
            int ping = ping(); int color = ping < 0 ? 0xAAAAAA : ping < 80 ? 0x55FF88 : ping < 180 ? 0xFFD24D : 0xFF5577;
            y = badge(sr, String.format("§f%d §7мс", Math.max(0, ping)), y, color);
        }
        if (QModsTools.config.cpsHud) y = badge(sr, String.format("§f%d §7/ §f%d §8КЛИК/С", cps.leftCps(), cps.rightCps()), y, 0x45D7FF);
        if (QModsTools.config.coordsHud) {
            y = badge(sr, String.format("§f%.0f §7 %.0f §7 %.0f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), y, 0x56E39F);
            y = badge(sr, String.format("§f%d §7FPS", Minecraft.getDebugFPS()), y, 0xFFB84D);
        }
    }

    private int ping() {
        NetworkPlayerInfo info = mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    private int badge(ScaledResolution sr, String text, int y, int color) {
        int w = mc.fontRendererObj.getStringWidth(text) + 16, x = sr.getScaledWidth() - w - 6;
        DrawUtil.rounded(x, y, x + w, y + 15, 4, 0xB012151D);
        DrawUtil.rect(x, y, x + 3, y + 15, ColorUtil.argb(color, 225));
        mc.fontRendererObj.drawString(text, x + 8, y + 4, 0xFFFFFFFF, true);
        return y + 17;
    }
}
