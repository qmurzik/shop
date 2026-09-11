package ru.qmurzik.qmodstools;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public final class Config {
    private final Configuration cfg;

    public boolean trajectory = true;
    public boolean trajectoryLanding = true;
    public boolean targetPrediction = true;
    public boolean targetPlayersOnly = true;
    public boolean targetOutline = true;
    public boolean aimGuide = true;
    public boolean aimAssist = true;
    public boolean ignoreTeammates = true;
    public float aimStrength = 0.22F;
    public float aimMaxStep = 3.5F;
    public int trajectoryColor = 0xB060FF;
    public int hitColor = 0x55FF88;
    public int missColor = 0xFF5577;
    public int maxSteps = 180;
    public int calculationInterval = 2;
    public double aimRange = 72.0;
    public double aimFov = 28.0;

    public boolean customChat = true;
    public int chatTheme = 0;
    public int chatAlpha = 150;
    public int chatAccent = 0xB060FF;
    public boolean chatShadow = true;
    public boolean chatTimestamps = false;
    public boolean chatAnimations = true;
    public boolean emojiReplace = true;
    public boolean emojiOutgoing = true;
    public int chatX = 2;
    public int chatYOffset = 0;

    public boolean customScoreboard = true;
    public int scoreboardTheme = 0;
    public int scoreboardAlpha = 155;
    public int scoreboardAccent = 0xB060FF;
    public boolean scoreboardNumbers = true;
    public boolean scoreboardShadow = true;
    public int scoreboardSide = 1;
    public int scoreboardYOffset = 0;
    public float scoreboardScale = 1.0F;

    public boolean lowPower = true;
    public boolean debugAim = false;
    public boolean mineBlazeMode = true;
    public boolean bedWarsHud = true;
    public boolean bedAlert = true;
    public boolean shopHelper = true;
    public boolean quickShopKeys = true;
    public boolean hideMineBlazeOrderNumbers = true;
    public boolean autoVoidRejoin = true;
    public int voidRejoinY = 15;
    public int rejoinDelayMs = 1000;
    public boolean localKiraSkin = true;
    public boolean localQModsCape = true;
    public final String[] bindText = new String[8];
    public final int[] bindKeys = new int[8];

    public Config(File file) { this.cfg = new Configuration(file); }

    public void load() {
        cfg.load();
        trajectory = bool("archer", "trajectory", trajectory);
        trajectoryLanding = bool("archer", "landing_marker", trajectoryLanding);
        targetPrediction = bool("archer", "motion_prediction", targetPrediction);
        targetPlayersOnly = bool("archer", "players_only", targetPlayersOnly);
        targetOutline = bool("archer", "target_outline", targetOutline);
        aimGuide = bool("archer", "aim_guide", aimGuide);
        aimAssist = bool("archer", "aim_assist", aimAssist);
        ignoreTeammates = bool("archer", "ignore_teammates", ignoreTeammates);
        aimStrength = (float) decimal("archer", "aim_strength", aimStrength, 0.05, 0.65);
        aimMaxStep = (float) decimal("archer", "aim_max_step", aimMaxStep, 0.5, 12.0);
        trajectoryColor = color("archer", "trajectory_color", trajectoryColor);
        hitColor = color("archer", "hit_color", hitColor);
        missColor = color("archer", "miss_color", missColor);
        maxSteps = integer("archer", "max_steps", maxSteps, 40, 320);
        calculationInterval = integer("archer", "calculation_interval", calculationInterval, 1, 6);
        aimRange = decimal("archer", "aim_range", aimRange, 12, 128);
        aimFov = decimal("archer", "aim_fov", aimFov, 5, 90);

        customChat = bool("chat", "enabled", customChat);
        chatTheme = integer("chat", "theme", chatTheme, 0, 5);
        chatAlpha = integer("chat", "alpha", chatAlpha, 0, 255);
        chatAccent = color("chat", "accent", chatAccent);
        chatShadow = bool("chat", "text_shadow", chatShadow);
        chatTimestamps = bool("chat", "timestamps", chatTimestamps);
        chatAnimations = bool("chat", "animations", chatAnimations);
        emojiReplace = bool("chat", "emoji_replace", emojiReplace);
        emojiOutgoing = bool("chat", "emoji_outgoing", emojiOutgoing);
        chatX = integer("chat", "x", chatX, 0, 500);
        chatYOffset = integer("chat", "y_offset", chatYOffset, -300, 300);

        customScoreboard = bool("scoreboard", "enabled", customScoreboard);
        scoreboardTheme = integer("scoreboard", "theme", scoreboardTheme, 0, 5);
        scoreboardAlpha = integer("scoreboard", "alpha", scoreboardAlpha, 0, 255);
        scoreboardAccent = color("scoreboard", "accent", scoreboardAccent);
        scoreboardNumbers = bool("scoreboard", "numbers", scoreboardNumbers);
        scoreboardShadow = bool("scoreboard", "text_shadow", scoreboardShadow);
        scoreboardSide = integer("scoreboard", "side", scoreboardSide, 0, 1);
        scoreboardYOffset = integer("scoreboard", "y_offset", scoreboardYOffset, -300, 300);
        scoreboardScale = (float) decimal("scoreboard", "scale", scoreboardScale, 0.65, 1.5);
        lowPower = bool("performance", "low_power", lowPower);
        debugAim = bool("performance", "debug_aim", debugAim);
        mineBlazeMode = bool("mineblaze", "enabled", mineBlazeMode);
        bedWarsHud = bool("mineblaze", "bedwars_hud", bedWarsHud);
        bedAlert = bool("mineblaze", "bed_alert", bedAlert);
        shopHelper = bool("mineblaze", "shop_helper", shopHelper);
        quickShopKeys = bool("mineblaze", "quick_shop_keys", quickShopKeys);
        hideMineBlazeOrderNumbers = bool("mineblaze", "hide_order_numbers", hideMineBlazeOrderNumbers);
        autoVoidRejoin = bool("mineblaze", "auto_void_rejoin", autoVoidRejoin);
        voidRejoinY = integer("mineblaze", "void_rejoin_y", voidRejoinY, -40, 80);
        rejoinDelayMs = integer("mineblaze", "rejoin_delay_ms", rejoinDelayMs, 500, 3000);
        localKiraSkin = bool("cosmetics", "kira_skin", localKiraSkin);
        localQModsCape = bool("cosmetics", "qmods_cape", localQModsCape);
        int schema = integer("general", "schema_version", 0, 0, 99);
        if (schema < 2) quickShopKeys = true;

        for (int i = 0; i < 8; i++) {
            bindText[i] = cfg.getString("text_" + (i + 1), "binds", i == 0 ? "/hub" : "", "Chat text or command");
            bindKeys[i] = cfg.getInt("key_" + (i + 1), "binds", i == 0 ? 23 : 0, 0, 255, "LWJGL key code");
        }
        save();
    }

    public void save() {
        set("archer", "trajectory", trajectory); set("archer", "landing_marker", trajectoryLanding);
        set("archer", "motion_prediction", targetPrediction); set("archer", "players_only", targetPlayersOnly);
        set("archer", "target_outline", targetOutline); set("archer", "trajectory_color", hex(trajectoryColor));
        set("archer", "aim_guide", aimGuide); set("archer", "aim_assist", aimAssist);
        set("archer", "ignore_teammates", ignoreTeammates); set("archer", "aim_strength", aimStrength); set("archer", "aim_max_step", aimMaxStep);
        set("archer", "hit_color", hex(hitColor)); set("archer", "miss_color", hex(missColor));
        set("archer", "max_steps", maxSteps); set("archer", "calculation_interval", calculationInterval);
        set("archer", "aim_range", aimRange); set("archer", "aim_fov", aimFov);
        set("chat", "enabled", customChat); set("chat", "theme", chatTheme); set("chat", "alpha", chatAlpha);
        set("chat", "accent", hex(chatAccent)); set("chat", "text_shadow", chatShadow);
        set("chat", "timestamps", chatTimestamps); set("chat", "animations", chatAnimations);
        set("chat", "emoji_replace", emojiReplace); set("chat", "emoji_outgoing", emojiOutgoing);
        set("chat", "x", chatX); set("chat", "y_offset", chatYOffset);
        set("scoreboard", "enabled", customScoreboard); set("scoreboard", "theme", scoreboardTheme);
        set("scoreboard", "alpha", scoreboardAlpha); set("scoreboard", "accent", hex(scoreboardAccent));
        set("scoreboard", "numbers", scoreboardNumbers); set("scoreboard", "text_shadow", scoreboardShadow);
        set("scoreboard", "side", scoreboardSide); set("scoreboard", "y_offset", scoreboardYOffset);
        set("scoreboard", "scale", scoreboardScale); set("performance", "low_power", lowPower);
        set("performance", "debug_aim", debugAim);
        set("mineblaze", "enabled", mineBlazeMode); set("mineblaze", "bedwars_hud", bedWarsHud);
        set("mineblaze", "bed_alert", bedAlert); set("mineblaze", "shop_helper", shopHelper);
        set("mineblaze", "quick_shop_keys", quickShopKeys); set("mineblaze", "hide_order_numbers", hideMineBlazeOrderNumbers);
        set("mineblaze", "auto_void_rejoin", autoVoidRejoin); set("mineblaze", "void_rejoin_y", voidRejoinY);
        set("mineblaze", "rejoin_delay_ms", rejoinDelayMs); set("cosmetics", "kira_skin", localKiraSkin);
        set("cosmetics", "qmods_cape", localQModsCape); set("general", "schema_version", 2);
        for (int i = 0; i < 8; i++) { set("binds", "text_" + (i + 1), bindText[i] == null ? "" : bindText[i]); set("binds", "key_" + (i + 1), bindKeys[i]); }
        cfg.save();
    }

    private boolean bool(String c, String k, boolean d) { return cfg.getBoolean(k, c, d, ""); }
    private int integer(String c, String k, int d, int min, int max) { return cfg.getInt(k, c, d, min, max, ""); }
    private double decimal(String c, String k, double d, double min, double max) { return cfg.get(c, k, d).setMinValue(min).setMaxValue(max).getDouble(d); }
    private int color(String c, String k, int d) { try { return Integer.parseInt(cfg.getString(k, c, hex(d), "RGB hex").replace("#", ""), 16) & 0xFFFFFF; } catch (Exception e) { return d; } }
    private String hex(int c) { return String.format("%06X", c & 0xFFFFFF); }
    private void set(String c, String k, Object v) { cfg.get(c, k, String.valueOf(v)).set(String.valueOf(v)); }
}
