package ru.qmurzik.qmodstools.feature;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import ru.qmurzik.qmodstools.QModsTools;

import java.util.Locale;

/** Keeps BedWars combat events in the dedicated HUD instead of duplicating them in normal chat. */
public final class ChatSuppression {
    private static final String[] KILL_WORDS = {"убил", "убила", "убит", "убита", "зарезал", "застрелил", "взорвал",
            "сжег", "сгорел", "утонул", "погиб", "разбился", "сбросил", "скинул", "столкнул", "бездна",
            "убийство", "killed", "slain", "shot", "blew up", "burned", "drowned", "fell", "void", "eliminated", "final kill"};

    private ChatSuppression() {}

    public static boolean shouldHide(ClientChatReceivedEvent event) {
        if (event == null || event.message == null || event.type == 2 || !BedWarsHelper.isMatchInProgress(net.minecraft.client.Minecraft.getMinecraft())) return false;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (clean == null) return false;
        String low = clean.toLowerCase(Locale.ROOT).replace('ё', 'е');
        boolean finalKill = low.contains("final kill") || low.contains("финальн") && (low.contains("уби") || low.contains("кил") || low.contains("смерт"));
        boolean bedBroken = !finalKill && (low.contains("кроват") || low.contains("bed"))
                && (low.contains("уничтож") || low.contains("разруш") || low.contains("сломан") || low.contains("destroy"));
        if (finalKill && QModsTools.config.finalKillBanner) return true;
        if (bedBroken && QModsTools.config.bedBreakBanner) return true;
        if (!QModsTools.config.killFeed) return false;
        for (String word : KILL_WORDS) if (low.contains(word)) return true;
        return false;
    }
}
