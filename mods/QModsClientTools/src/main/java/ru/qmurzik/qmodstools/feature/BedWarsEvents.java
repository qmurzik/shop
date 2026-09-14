package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Turns MineBlaze BedWars broadcast lines (bed destroyed / final kill / game end) into big animated banners with a particle burst, like Hypixel does. */
public final class BedWarsEvents {
    private final Minecraft mc;
    private final Random rng = new Random();
    private final List<Spark> sparks = new ArrayList<Spark>();
    private String bannerText, bannerHeader; private int bannerColor; private long bannerStart, bannerUntil; private boolean bannerBig;
    private long lastGgMs;

    public BedWarsEvents(Minecraft mc) { this.mc = mc; }

    public void onChat(ClientChatReceivedEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null || e.type == 2 || !BedWarsHelper.isMatchInProgress(mc)) return;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        if (clean == null || clean.trim().isEmpty()) return;
        String low = clean.toLowerCase(Locale.ROOT).replace('ё','е');
        boolean finalKill = low.contains("final kill") || low.contains("финальн") && (low.contains("уби") || low.contains("кил") || low.contains("смерт"));
        boolean bedBroken = !finalKill && (low.contains("кроват")||low.contains("bed"))
                && (low.contains("уничтож") || low.contains("разруш") || low.contains("сломан") || low.contains("destroy"));
        boolean gameEnd = low.contains("wins the game") || low.contains("game over") || low.contains("игра завершена")
                || low.contains("игра окончена") || low.contains("победила команда") || low.contains("выиграла команда");
        if (finalKill && QModsTools.config.finalKillBanner) { show(e.message.getFormattedText(), "§lФИНАЛЬНОЕ УБИЙСТВО", 0xFF5577, true); spawnSparks(0xFF5577, 26); }
        else if (bedBroken && QModsTools.config.bedBreakBanner) { show(e.message.getFormattedText(), "§lРАЗРУШЕНИЕ КРОВАТИ", 0xFFD24D, false); spawnSparks(0xFFD24D, 16); }
        if (gameEnd) {
            spawnConfetti();
            if (QModsTools.config.autoGg && System.currentTimeMillis() - lastGgMs > 60000L) {
                lastGgMs = System.currentTimeMillis();
                mc.thePlayer.sendChatMessage(QModsTools.config.autoGgText);
            }
        }
    }

    private void show(String text, String header, int color, boolean big) {
        bannerText = text; bannerHeader = header; bannerColor = color; bannerBig = big;
        bannerStart = System.currentTimeMillis(); bannerUntil = bannerStart + 3200L;
    }

    private void spawnSparks(int color, int count) { for (int i = 0; i < count; i++) sparks.add(new Spark(color)); }

    private void spawnConfetti() {
        int[] palette = {0xB060FF, 0xFF5C9A, 0x45D7FF, 0x56E39F, 0xFFB84D};
        for (int i = 0; i < 60; i++) sparks.add(new Spark(palette[rng.nextInt(palette.length)]));
    }

    public void tick() {
        long now = System.currentTimeMillis();
        Iterator<Spark> it = sparks.iterator(); while (it.hasNext()) if (now - it.next().start > 1100L) it.remove();
    }

    public void render() {
        renderSparks();
        if (bannerText == null || mc.currentScreen != null) return;
        long now = System.currentTimeMillis(); if (now > bannerUntil) { bannerText = null; return; }
        float in = Math.min(1F, (now - bannerStart) / 220F), out = Math.min(1F, (bannerUntil - now) / 260F);
        float scale = (bannerBig ? 1.35F : 1.05F) * (0.85F + 0.15F * in);
        int alpha = (int) (255 * Math.min(in, out));
        ScaledResolution sr = new ScaledResolution(mc); int cx = sr.getScaledWidth() / 2, cy = sr.getScaledHeight() / 4;
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

    private void renderSparks() {
        if (sparks.isEmpty() || mc.currentScreen != null) return;
        ScaledResolution sr = new ScaledResolution(mc); long now = System.currentTimeMillis();
        for (Spark s : sparks) {
            float t = (now - s.start) / 1100F; if (t > 1) continue;
            float px = (s.x + s.vx * t) * sr.getScaledWidth();
            float py = (s.y + s.vy * t + 0.5F * t * t) * sr.getScaledHeight();
            int a = (int) (235 * (1 - t)); int size = 3;
            DrawUtil.rect((int) px - size, (int) py - size, (int) px + size, (int) py + size, ColorUtil.argb(s.color, a));
        }
    }

    private final class Spark {
        final float x, y, vx, vy; final int color; final long start = System.currentTimeMillis();
        Spark(int color) {
            this.color = color; x = 0.5F; y = 0.25F;
            float ang = (float) (rng.nextDouble() * Math.PI * 2); float speed = 0.12F + (float) rng.nextDouble() * 0.28F;
            vx = (float) Math.cos(ang) * speed; vy = (float) Math.sin(ang) * speed - 0.12F;
        }
    }
}
