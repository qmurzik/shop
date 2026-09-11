package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Local-only PvP feedback: hit particles, a hit marker on the crosshair, a damage popup and a directional "who hit me" indicator. */
public final class CombatEffects {
    private final Minecraft mc;
    private final Random rng = new Random();
    private final List<Popup> popups = new ArrayList<Popup>();
    private final List<Indicator> indicators = new ArrayList<Indicator>();
    private long hitMarkerUntil, damageFlashUntil;
    private boolean hitMarkerCrit;

    public CombatEffects(Minecraft mc) { this.mc = mc; }

    public void onHurt(LivingHurtEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        Entity attacker = e.source.getEntity();
        if (attacker == mc.thePlayer && e.entityLiving != mc.thePlayer) onLocalHit(e);
        else if (e.entityLiving == mc.thePlayer) onLocalDamaged(attacker);
    }

    private void onLocalHit(LivingHurtEvent e) {
        boolean crit = mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater();
        if (QModsTools.config.hitParticles) spawnHitParticles(e.entityLiving, crit);
        if (QModsTools.config.hitMarker) { hitMarkerUntil = System.currentTimeMillis() + 220L; hitMarkerCrit = crit; }
        if (crit && QModsTools.config.critEffects) mc.thePlayer.playSound("random.orb", 0.5F, 1.6F);
        popups.add(new Popup(String.format("-%.1f", e.ammount), crit));
    }

    private void onLocalDamaged(Entity attacker) {
        damageFlashUntil = System.currentTimeMillis() + 380L;
        if (!QModsTools.config.damageIndicator || attacker == null || attacker == mc.thePlayer) return;
        double dx = attacker.posX - mc.thePlayer.posX, dz = attacker.posZ - mc.thePlayer.posZ;
        float angle = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90D);
        indicators.add(new Indicator(angle));
    }

    private void spawnHitParticles(EntityLivingBase target, boolean crit) {
        double x = target.posX, y = target.posY + target.height * 0.6D, z = target.posZ;
        int color = crit ? 0xFFD24D : QModsTools.config.hitColor;
        float r = ColorUtil.r(color), g = ColorUtil.g(color), b = ColorUtil.b(color);
        int count = QModsTools.config.lowPower ? 4 : 8;
        for (int i = 0; i < count; i++) {
            double ox = (rng.nextDouble() - 0.5) * target.width, oy = rng.nextDouble() * target.height * 0.6, oz = (rng.nextDouble() - 0.5) * target.width;
            mc.theWorld.spawnParticle(EnumParticleTypes.REDSTONE, x + ox, y + oy, z + oz, r, g, b);
        }
        if (crit) for (int i = 0; i < 3; i++)
            mc.theWorld.spawnParticle(EnumParticleTypes.CRIT, x + (rng.nextDouble() - 0.5) * target.width, y, z + (rng.nextDouble() - 0.5) * target.width, 0, 0, 0);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        Iterator<Popup> pit = popups.iterator(); while (pit.hasNext()) if (now - pit.next().start > 650L) pit.remove();
        Iterator<Indicator> iit = indicators.iterator(); while (iit.hasNext()) if (now - iit.next().start > 1000L) iit.remove();
    }

    public void render() {
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        ScaledResolution sr = new ScaledResolution(mc); int cx = sr.getScaledWidth() / 2, cy = sr.getScaledHeight() / 2;
        if (now < damageFlashUntil) {
            float a = (damageFlashUntil - now) / 380F; int alpha = (int) (90 * a); int c = ColorUtil.argb(0xFF3355, alpha + 90);
            DrawUtil.rect(0, 0, sr.getScaledWidth(), 4, c); DrawUtil.rect(0, sr.getScaledHeight() - 4, sr.getScaledWidth(), sr.getScaledHeight(), c);
            DrawUtil.rect(0, 0, 4, sr.getScaledHeight(), c); DrawUtil.rect(sr.getScaledWidth() - 4, 0, sr.getScaledWidth(), sr.getScaledHeight(), c);
        }
        if (QModsTools.config.hitMarker && now < hitMarkerUntil) {
            float t = (hitMarkerUntil - now) / 220F; int a = (int) (255 * t); int s = (int) (6 + 6 * (1 - t));
            int c = ColorUtil.argb(hitMarkerCrit ? 0xFFD24D : 0xFFFFFF, a);
            DrawUtil.rect(cx - s, cy - 1, cx + s, cy + 1, c); DrawUtil.rect(cx - 1, cy - s, cx + 1, cy + s, c);
        }
        for (Popup p : popups) {
            float t = (now - p.start) / 650F; if (t > 1) continue;
            int a = (int) (255 * (1 - t)); int yy = cy - 24 - (int) (14 * t);
            int color = p.crit ? 0xFFD24D : 0xFFFFFF; int w = mc.fontRendererObj.getStringWidth(p.text);
            mc.fontRendererObj.drawString(p.text, cx - w / 2, yy, ColorUtil.argb(color, a), true);
        }
        for (Indicator ind : indicators) {
            float t = (now - ind.start) / 1000F; if (t > 1) continue;
            int a = (int) (230 * (1 - t));
            float relative = MathHelper.wrapAngleTo180_float(ind.angle - mc.thePlayer.rotationYaw);
            double rad = Math.toRadians(relative); int radius = Math.min(sr.getScaledWidth(), sr.getScaledHeight()) / 2 - 30;
            int ax = cx + (int) (Math.sin(rad) * radius), ay = cy - (int) (Math.cos(rad) * radius);
            DrawUtil.rounded(ax - 5, ay - 5, ax + 5, ay + 5, 3, ColorUtil.argb(0xFF5577, a));
        }
    }

    private static final class Popup { final String text; final boolean crit; final long start = System.currentTimeMillis(); Popup(String text, boolean crit) { this.text = text; this.crit = crit; } }
    private static final class Indicator { final float angle; final long start = System.currentTimeMillis(); Indicator(float angle) { this.angle = angle; } }
}
