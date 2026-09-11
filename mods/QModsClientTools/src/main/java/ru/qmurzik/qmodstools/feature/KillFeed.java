package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Compact kill feed for MineBlaze BedWars: reuses the server's own death-broadcast chat lines instead of trying to re-parse who/whom/weapon. */
public final class KillFeed {
    private static final String[] KEYWORDS = {"убил", "зарезал", "застрелил", "взорвал", "сжёг", "утонул", "убит",
            "killed", "slain", "shot", "blew up", "burned", "drowned", "fell"};
    private final Minecraft mc;
    private final List<Entry> entries = new ArrayList<Entry>();

    public KillFeed(Minecraft mc) { this.mc = mc; }

    public void onChat(ClientChatReceivedEvent e) {
        if (!QModsTools.config.killFeed || mc.thePlayer == null || mc.theWorld == null || e.type != 0 || !BedWarsHelper.isMatchInProgress(mc)) return;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        if (clean == null) return;
        String low = clean.toLowerCase(Locale.ROOT);
        for (String kw : KEYWORDS) if (low.contains(kw)) { add(e.message.getFormattedText()); return; }
    }

    private void add(String text) {
        entries.add(new Entry(text));
        while (entries.size() > 5) entries.remove(0);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) if (now - it.next().start > 6000L) it.remove();
    }

    public void render() {
        if (entries.isEmpty() || mc.currentScreen != null) return;
        int x = 6, y = 6; long now = System.currentTimeMillis();
        for (Entry en : entries) {
            float age = now - en.start;
            float alpha = age < 5500 ? 1F : Math.max(0F, 1F - (age - 5500) / 500F);
            int w = mc.fontRendererObj.getStringWidth(en.text) + 12;
            DrawUtil.rounded(x, y, x + w, y + 12, 3, ColorUtil.argb(0x12151D, (int) (200 * alpha)));
            mc.fontRendererObj.drawString(en.text, x + 6, y + 2, ColorUtil.argb(0xFFFFFF, (int) (255 * alpha)), true);
            y += 15;
        }
    }

    private static final class Entry { final String text; final long start = System.currentTimeMillis(); Entry(String text) { this.text = text; } }
}
