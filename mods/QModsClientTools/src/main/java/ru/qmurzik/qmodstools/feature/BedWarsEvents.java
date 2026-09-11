package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.util.Locale;

/** Turns MineBlaze BedWars broadcast lines (bed destroyed / final kill) into a big animated banner, like Hypixel does. */
public final class BedWarsEvents {
    private final Minecraft mc;
    private String bannerText, bannerHeader; private int bannerColor; private long bannerStart, bannerUntil; private boolean bannerBig;

    public BedWarsEvents(Minecraft mc) { this.mc = mc; }

    public void onChat(ClientChatReceivedEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null || e.type != 0 || !BedWarsHelper.isMineBlazeBedWars(mc)) return;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        if (clean == null || clean.trim().isEmpty()) return;
        String low = clean.toLowerCase(Locale.ROOT);
        boolean finalKill = low.contains("final kill") || (low.contains("финальн") && (low.contains("уби") || low.contains("удар")));
        boolean bedBroken = !finalKill && low.contains("кроват") && (low.contains("уничтож") || low.contains("разруш") || low.contains("сломан"));
        if (finalKill && QModsTools.config.finalKillBanner) show(e.message.getFormattedText(), "§lФИНАЛЬНОЕ УБИЙСТВО", 0xFF5577, true);
        else if (bedBroken && QModsTools.config.bedBreakBanner) show(e.message.getFormattedText(), "§lРАЗРУШЕНИЕ КРОВАТИ", 0xFFD24D, false);
    }

    private void show(String text, String header, int color, boolean big) {
        bannerText = text; bannerHeader = header; bannerColor = color; bannerBig = big;
        bannerStart = System.currentTimeMillis(); bannerUntil = bannerStart + 3200L;
    }

    public void render() {
        if (bannerText == null || mc.currentScreen != null) return;
        long now = System.currentTimeMillis(); if (now > bannerUntil) { bannerText = null; return; }
        float in = Math.min(1F, (now - bannerStart) / 220F), out = Math.min(1F, (bannerUntil - now) / 260F);
        float scale = (bannerBig ? 1.35F : 1.05F) * (0.85F + 0.15F * in);
        int alpha = (int) (255 * Math.min(in, out));
        ScaledResolution sr = new ScaledResolution(mc); int cx = sr.getScaledWidth() / 2, cy = sr.getScaledHeight() / 3;
        DrawUtil.beginHudScale(scale, cx, cy);
        int w = mc.fontRendererObj.getStringWidth(bannerText), hw = mc.fontRendererObj.getStringWidth(bannerHeader);
        int boxW = Math.max(w, hw) + 28, boxX = -boxW / 2, boxY = -20;
        DrawUtil.shadow(boxX, boxY, boxX + boxW, boxY + 40, 6);
        DrawUtil.rounded(boxX, boxY, boxX + boxW, boxY + 40, 6, ColorUtil.argb(0x12151D, Math.min(220, alpha)));
        DrawUtil.rect(boxX, boxY, boxX + boxW, boxY + 3, ColorUtil.argb(bannerColor, alpha));
        mc.fontRendererObj.drawString(bannerHeader, -hw / 2, boxY + 8, ColorUtil.argb(0xFFFFFF, alpha), true);
        mc.fontRendererObj.drawString(bannerText, -w / 2, boxY + 22, ColorUtil.argb(0xFFFFFF, alpha), true);
        DrawUtil.endHudScale();
    }
}
