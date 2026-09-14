package ru.qmurzik.qmodstools.feature;

public final class EmojiReplacer {
    private EmojiReplacer() {}
    public static String replace(String s) {
        if (s == null) return "";
        // Minecraft 1.8.9's bundled font does not contain modern emoji and renders many
        // Unicode symbols as boxes or random-looking glyphs. Keep the server-safe ASCII
        // emoticon intact and only tint it locally in our custom chat.
        return s.replace(">:(", "§c>:(§r").replace(":'(", "§b:'(§r").replace(":')", "§e:')§r")
                .replace("o_O", "§do_O§r").replace("O_o", "§dO_o§r")
                .replace("<3", "§d<3§r").replace(":*", "§c:*§r")
                .replace(":-)", "§e:-)§r").replace(":)", "§e:)§r")
                .replace(":D", "§e:D§r").replace("xD", "§exD§r").replace("XD", "§eXD§r")
                .replace(";)", "§6;)§r").replace("B)", "§6B)§r")
                .replace(":P", "§d:P§r").replace(":p", "§d:p§r")
                .replace(":-(", "§7:-(§r").replace(":(", "§7:(§r")
                .replace(":O", "§b:O§r").replace(":o", "§b:o§r")
                .replace(":/", "§7:/§r").replace(":|", "§7:|§r").replace(":3", "§d:3§r")
                .replace("^^", "§a^^§r");
    }

    public static String outgoing(String s) {
        // Never send formatting codes or unsupported Unicode to the server.
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }
}
