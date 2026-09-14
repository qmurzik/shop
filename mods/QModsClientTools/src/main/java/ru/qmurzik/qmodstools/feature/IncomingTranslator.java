package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import ru.qmurzik.qmodstools.QModsTools;

/** Auto-translates foreign incoming chat lines to Russian, adding a compact indented follow-up line rather than rewriting the original. */
public final class IncomingTranslator {
    private final Minecraft mc;
    private long lastRequestMs;

    public IncomingTranslator(Minecraft mc) { this.mc = mc; }

    public void onChat(ClientChatReceivedEvent e) {
        if (!QModsTools.config.translateIncoming || mc.thePlayer == null || e.type == 2) return;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        if (clean == null) return;
        String stripped = messageBody(clean.trim());
        if (stripped.isEmpty() || stripped.length() > 200) return;
        if (stripped.startsWith("↳")) return;
        if (hasCyrillic(stripped) || !hasLetter(stripped)) return;
        long now = System.currentTimeMillis();
        if (now - lastRequestMs < 400L) return;
        lastRequestMs = now;
        ChatTranslator.translate(stripped, "auto", "ru", new ChatTranslator.Callback() {
            @Override public void onResult(String translated) {
                if (translated == null || mc.thePlayer == null) return;
                if (translated.trim().equalsIgnoreCase(stripped.trim())) return;
                mc.thePlayer.addChatMessage(new ChatComponentText("§8↳ §7" + translated));
            }
        });
    }

    private static boolean hasCyrillic(String s) { for (int i = 0; i < s.length(); i++) { char c = s.charAt(i); if (c >= 0x0400 && c <= 0x04FF) return true; } return false; }
    private static boolean hasLetter(String s) { for (int i = 0; i < s.length(); i++) if (Character.isLetter(s.charAt(i))) return true; return false; }
    private static String messageBody(String s) {
        int colon=s.indexOf(':');
        if(colon>0&&colon+1<s.length()&&colon<48)return s.substring(colon+1).trim();
        return s;
    }
}
