package ru.qmurzik.qmodstools.feature;

public final class EmojiReplacer {
    private EmojiReplacer() {}
    public static String replace(String s) {
        if (s == null) return "";
        return s.replace(">:(", "§c☠§r").replace(":'(", "§b☂§r").replace(":')", "§e☺§r")
                .replace("o_O", "§d◉_◉§r").replace("O_o", "§d◉_◉§r")
                .replace("<3", "§d❤§r").replace(":*", "§c♥§r")
                .replace(":-)", "§e☺§r").replace(":)", "§e☺§r")
                .replace(":D", "§e☻§r").replace("xD", "§e☻§r").replace("XD", "§e☻§r")
                .replace(";)", "§6✦§r").replace("B)", "§6☀§r")
                .replace(":P", "§d♪§r").replace(":p", "§d♪§r")
                .replace(":-(", "§7☹§r").replace(":(", "§7☹§r")
                .replace(":O", "§b○§r").replace(":o", "§b○§r")
                .replace(":/", "§7◔§r").replace(":|", "§7•§r").replace(":3", "§d♬§r")
                .replace("^^", "§a★§r");
    }

    public static String outgoing(String s) {
        return replace(s).replaceAll("§[0-9a-fk-or]", "");
    }
}
