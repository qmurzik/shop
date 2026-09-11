package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import ru.qmurzik.qmodstools.QModsTools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AimPredictor {
    private static final double DRAG = 0.99D;
    private static final double GRAVITY = 0.05D;
    private final Minecraft mc;
    private final Map<Integer, Track> tracks = new HashMap<Integer, Track>();
    private Solution solution;
    private int lockedTargetId = -1;

    public AimPredictor(Minecraft mc) { this.mc = mc; }

    public void tick() {
        if (mc.theWorld == null || mc.thePlayer == null) { tracks.clear(); solution = null; return; }
        int now = mc.thePlayer.ticksExisted;
        for (Object raw : mc.theWorld.loadedEntityList) {
            if (!(raw instanceof EntityLivingBase) || raw == mc.thePlayer) continue;
            EntityLivingBase entity = (EntityLivingBase)raw;
            Track tr = tracks.get(entity.getEntityId());
            if (tr == null) { tr = new Track(entity.posX, entity.posY, entity.posZ, now); tracks.put(entity.getEntityId(), tr); continue; }
            if (tr.tick != now) {
                double vx = entity.posX - tr.x, vy = entity.posY - tr.y, vz = entity.posZ - tr.z;
                double mix = entity instanceof EntityPlayer ? 0.55D : 0.72D;
                tr.vx = tr.vx * mix + vx * (1.0D - mix);
                tr.vy = tr.vy * mix + vy * (1.0D - mix);
                tr.vz = tr.vz * mix + vz * (1.0D - mix);
                if (entity.onGround && Math.abs(tr.vy) < 0.08D) tr.vy = 0;
                tr.x = entity.posX; tr.y = entity.posY; tr.z = entity.posZ; tr.tick = now;
            }
        }
        Iterator<Map.Entry<Integer, Track>> it = tracks.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue().tick > 40) it.remove();
    }

    public Solution update(double launchSpeed) {
        solution = null;
        if (!QModsTools.config.targetPrediction || launchSpeed < 0.12D || mc.thePlayer == null) return null;
        EntityLivingBase target = selectTarget();
        if (target == null) return null;
        Track tr = tracks.get(target.getEntityId());
        double tvx = tr == null ? target.posX - target.prevPosX : tr.vx;
        double tvy = tr == null ? target.posY - target.prevPosY : tr.vy;
        double tvz = tr == null ? target.posZ - target.prevPosZ : tr.vz;
        double speedCap = 0.9D;
        double h = Math.sqrt(tvx * tvx + tvz * tvz);
        if (h > speedCap) { tvx *= speedCap / h; tvz *= speedCap / h; }
        tvy = MathHelper.clamp_double(tvy, -0.6D, 0.6D);

        Vec3 start = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 0.1D, mc.thePlayer.posZ);
        Vec3 base = new Vec3(mc.thePlayer.motionX, mc.thePlayer.onGround ? 0 : mc.thePlayer.motionY, mc.thePlayer.motionZ);
        double targetY = target.posY + target.height * 0.62D;
        double lastF = Double.NaN, lastT = 0.5D;
        double bestT = -1, bestAbs = Double.MAX_VALUE;
        boolean rooted = false;
        for (double t = 0.5D; t <= 160.0D; t += 0.25D) {
            double f = requiredSpeed(start, base, target.posX + tvx * t, targetY + tvy * t, target.posZ + tvz * t, t) - launchSpeed;
            double af = Math.abs(f);
            if (af < bestAbs) { bestAbs = af; bestT = t; }
            if (!Double.isNaN(lastF) && lastF > 0 && f <= 0) {
                double lo = lastT, hi = t;
                for (int n = 0; n < 18; n++) {
                    double mid = (lo + hi) * 0.5D;
                    double mf = requiredSpeed(start, base, target.posX + tvx * mid, targetY + tvy * mid, target.posZ + tvz * mid, mid) - launchSpeed;
                    if (mf > 0) lo = mid; else hi = mid;
                }
                bestT = (lo + hi) * 0.5D;
                bestAbs = Math.abs(requiredSpeed(start, base, target.posX + tvx * bestT, targetY + tvy * bestT, target.posZ + tvz * bestT, bestT) - launchSpeed);
                rooted = true;
                break;
            }
            lastF = f; lastT = t;
        }
        if (bestT < 0 || (!rooted && bestAbs > Math.max(0.18D, launchSpeed * 0.12D))) return null;
        double tx = target.posX + tvx * bestT, ty = targetY + tvy * bestT, tz = target.posZ + tvz * bestT;
        Vec3 velocity = requiredVelocity(start, base, tx, ty, tz, bestT);
        double required = velocity.lengthVector();
        if (required < 0.001D || required > launchSpeed * 1.13D) return null;
        velocity = velocity.normalize();
        float yaw = (float)(Math.atan2(-velocity.xCoord, velocity.zCoord) * 180.0D / Math.PI);
        float pitch = (float)(-Math.asin(MathHelper.clamp_double(velocity.yCoord, -1, 1)) * 180.0D / Math.PI);
        solution = new Solution(target, new Vec3(tx, ty, tz), velocity, yaw, pitch, bestT);
        lockedTargetId = target.getEntityId();
        return solution;
    }

    public void updateForCurrentBow() {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem() || mc.thePlayer.getItemInUse() == null || !(mc.thePlayer.getItemInUse().getItem() instanceof ItemBow)) {
            solution = null; lockedTargetId = -1; return;
        }
        int use = mc.thePlayer.getItemInUse().getMaxItemUseDuration() - mc.thePlayer.getItemInUseCount();
        float charge = use / 20.0F; charge = (charge * charge + charge * 2.0F) / 3.0F; if (charge > 1) charge = 1;
        update(charge * 3.0D);
    }

    public void applySmoothAim() {
        if (!QModsTools.config.aimAssist || solution == null || mc.currentScreen != null) return;
        float dyaw = MathHelper.wrapAngleTo180_float(solution.yaw - mc.thePlayer.rotationYaw);
        float dpitch = MathHelper.wrapAngleTo180_float(solution.pitch - mc.thePlayer.rotationPitch);
        float max = QModsTools.config.aimMaxStep;
        float strength = QModsTools.config.aimStrength;
        float yawStep = MathHelper.clamp_float(dyaw * strength, -max, max);
        float pitchStep = MathHelper.clamp_float(dpitch * strength, -max, max);
        if (Math.abs(dyaw) < 0.08F) yawStep = dyaw;
        if (Math.abs(dpitch) < 0.08F) pitchStep = dpitch;
        mc.thePlayer.rotationYaw += yawStep;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchStep, -89.5F, 89.5F);
    }

    public Solution getSolution() { return solution; }

    private EntityLivingBase selectTarget() {
        EntityLivingBase best = null;
        double bestAngle = QModsTools.config.aimFov;
        double bestDistance = Double.MAX_VALUE;
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = mc.thePlayer.getLook(1.0F).normalize();
        for (Object raw : mc.theWorld.loadedEntityList) {
            if (!(raw instanceof EntityLivingBase) || raw == mc.thePlayer) continue;
            if (QModsTools.config.targetPlayersOnly && !(raw instanceof EntityPlayer)) continue;
            EntityLivingBase e = (EntityLivingBase)raw;
            if (e.isDead || e.getHealth() <= 0 || e.isInvisible()) continue;
            if (QModsTools.config.ignoreTeammates && mc.thePlayer.isOnSameTeam(e)) continue;
            double distance = mc.thePlayer.getDistanceToEntity(e);
            if (distance > QModsTools.config.aimRange) continue;
            if (!hasLineOfSight(eye, e)) continue;
            Vec3 to = new Vec3(e.posX - eye.xCoord, e.posY + e.height * 0.62D - eye.yCoord, e.posZ - eye.zCoord).normalize();
            double angle = Math.toDegrees(Math.acos(MathHelper.clamp_double(look.dotProduct(to), -1, 1)));
            if (angle < bestAngle - 0.08D || (Math.abs(angle - bestAngle) <= 0.08D && distance < bestDistance)) {
                bestAngle = angle; bestDistance = distance; best = e;
            }
        }
        return best;
    }

    private boolean hasLineOfSight(Vec3 eye, EntityLivingBase e) {
        double[] heights = {0.82D, 0.60D, 0.28D};
        for (double h : heights) {
            Vec3 point = new Vec3(e.posX, e.posY + e.height * h, e.posZ);
            if (mc.theWorld.rayTraceBlocks(eye, point, false, true, false) == null) return true;
        }
        return false;
    }

    private static double requiredSpeed(Vec3 start, Vec3 base, double tx, double ty, double tz, double t) {
        return requiredVelocity(start, base, tx, ty, tz, t).lengthVector();
    }

    private static Vec3 requiredVelocity(Vec3 start, Vec3 base, double tx, double ty, double tz, double t) {
        double s = (1.0D - Math.pow(DRAG, t)) / (1.0D - DRAG);
        double gravityDrop = GRAVITY / (1.0D - DRAG) * (t - s);
        return new Vec3((tx - start.xCoord) / s - base.xCoord,
                (ty - start.yCoord + gravityDrop) / s - base.yCoord,
                (tz - start.zCoord) / s - base.zCoord);
    }

    private static final class Track {
        double x, y, z, vx, vy, vz; int tick;
        Track(double x, double y, double z, int tick) { this.x=x; this.y=y; this.z=z; this.tick=tick; }
    }

    public static final class Solution {
        public final EntityLivingBase target;
        public final Vec3 predictedTarget, direction;
        public final float yaw, pitch;
        public final double flightTicks;
        Solution(EntityLivingBase target, Vec3 predictedTarget, Vec3 direction, float yaw, float pitch, double flightTicks) {
            this.target=target; this.predictedTarget=predictedTarget; this.direction=direction; this.yaw=yaw; this.pitch=pitch; this.flightTicks=flightTicks;
        }
    }
}
