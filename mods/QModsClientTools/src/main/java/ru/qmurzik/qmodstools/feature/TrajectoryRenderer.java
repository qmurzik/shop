package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public final class TrajectoryRenderer {
    private final Minecraft mc;
    private final AimPredictor predictor;
    private final List<Vec3> points = new ArrayList<Vec3>(200);
    private MovingObjectPosition impact;
    private Entity hitEntity;
    private int lastCalc = -100;
    private double launchSpeed;

    public TrajectoryRenderer(Minecraft mc, AimPredictor predictor) { this.mc = mc; this.predictor = predictor; }

    public void render(RenderWorldLastEvent event) {
        if (!QModsTools.config.trajectory || mc.thePlayer == null || mc.theWorld == null || !isDrawingBow()) return;
        if (mc.thePlayer.ticksExisted - lastCalc >= QModsTools.config.calculationInterval) calculate();
        if (points.size() < 2) return;
        double rx = mc.getRenderManager().viewerPosX, ry = mc.getRenderManager().viewerPosY, rz = mc.getRenderManager().viewerPosZ;
        int color = hitEntity != null ? QModsTools.config.hitColor : QModsTools.config.trajectoryColor;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D(); GlStateManager.enableBlend(); GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableDepth(); GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH); GL11.glLineWidth(QModsTools.config.lowPower ? 1.6F : 2.4F);
        Tessellator tess = Tessellator.getInstance(); WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i); float alpha = 0.92F - 0.42F * i / Math.max(1, points.size() - 1);
            wr.pos(p.xCoord-rx, p.yCoord-ry, p.zCoord-rz).color(ColorUtil.r(color), ColorUtil.g(color), ColorUtil.b(color), alpha).endVertex();
        }
        tess.draw();
        if (QModsTools.config.trajectoryLanding) drawImpact(rx, ry, rz, color);
        drawAimMarker(rx, ry, rz);
        GL11.glDisable(GL11.GL_LINE_SMOOTH); GlStateManager.depthMask(true); GlStateManager.enableDepth();
        GlStateManager.enableTexture2D(); GlStateManager.disableBlend(); GlStateManager.popMatrix();
    }

    private void calculate() {
        lastCalc = mc.thePlayer.ticksExisted; points.clear(); impact = null; hitEntity = null;
        int use = mc.thePlayer.getItemInUse().getMaxItemUseDuration() - mc.thePlayer.getItemInUseCount();
        float charge = use / 20.0F; charge = (charge * charge + charge * 2.0F) / 3.0F; if (charge > 1) charge = 1;
        launchSpeed = charge * 3.0D;
        if (launchSpeed < 0.12D) return;
        float yaw = mc.thePlayer.rotationYaw, pitch = mc.thePlayer.rotationPitch;
        double px = mc.thePlayer.posX - Math.cos(Math.toRadians(yaw)) * 0.16D;
        double py = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 0.1D;
        double pz = mc.thePlayer.posZ - Math.sin(Math.toRadians(yaw)) * 0.16D;
        double mx = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * launchSpeed + mc.thePlayer.motionX;
        double my = -Math.sin(Math.toRadians(pitch)) * launchSpeed + (mc.thePlayer.onGround ? 0 : mc.thePlayer.motionY);
        double mz = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * launchSpeed + mc.thePlayer.motionZ;
        points.add(new Vec3(px, py, pz));
        int steps = Math.min(QModsTools.config.maxSteps, QModsTools.config.lowPower ? 140 : 320);
        for (int i = 0; i < steps; i++) {
            Vec3 from = new Vec3(px,py,pz), to = new Vec3(px+mx,py+my,pz+mz);
            MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(from, to, false, true, false);
            Vec3 end = blockHit == null ? to : blockHit.hitVec;
            Entity entity = findEntityHit(from, end);
            if (entity != null) { hitEntity = entity; end = closestHit(from, end, entity); }
            px=end.xCoord; py=end.yCoord; pz=end.zCoord; points.add(end);
            if (entity != null || blockHit != null) { impact = entity != null ? new MovingObjectPosition(entity, end) : blockHit; break; }
            boolean water = mc.theWorld.isMaterialInBB(new AxisAlignedBB(px-.05,py-.05,pz-.05,px+.05,py+.05,pz+.05), net.minecraft.block.material.Material.water);
            double drag = water ? 0.6D : 0.99D; mx*=drag; my*=drag; mz*=drag; my-=0.05D;
            if (py < 0) break;
        }
    }

    private Entity findEntityHit(Vec3 from, Vec3 to) {
        AxisAlignedBB area = new AxisAlignedBB(from.xCoord,from.yCoord,from.zCoord,to.xCoord,to.yCoord,to.zCoord).expand(1,1,1);
        Entity best = null; double bestDist = Double.MAX_VALUE;
        for (Object raw : mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, area)) {
            Entity e=(Entity)raw; if (!e.canBeCollidedWith() || e == mc.thePlayer.ridingEntity) continue;
            AxisAlignedBB box=e.getEntityBoundingBox().expand(0.3D,0.3D,0.3D); MovingObjectPosition mop=box.calculateIntercept(from,to);
            if (mop != null) { double d=from.squareDistanceTo(mop.hitVec); if (d<bestDist) { bestDist=d; best=e; } }
        }
        return best;
    }

    private Vec3 closestHit(Vec3 from, Vec3 to, Entity e) {
        MovingObjectPosition mop=e.getEntityBoundingBox().expand(.3,.3,.3).calculateIntercept(from,to); return mop==null?to:mop.hitVec;
    }

    private void drawImpact(double rx,double ry,double rz,int color) {
        if (points.isEmpty()) return; Vec3 p=points.get(points.size()-1); double s=.22D;
        Tessellator t=Tessellator.getInstance(); WorldRenderer w=t.getWorldRenderer(); GL11.glLineWidth(2F);
        w.begin(GL11.GL_LINES,DefaultVertexFormats.POSITION_COLOR);
        line(w,p.xCoord-rx-s,p.yCoord-ry,p.zCoord-rz,p.xCoord-rx+s,p.yCoord-ry,p.zCoord-rz,color);
        line(w,p.xCoord-rx,p.yCoord-ry-s,p.zCoord-rz,p.xCoord-rx,p.yCoord-ry+s,p.zCoord-rz,color);
        line(w,p.xCoord-rx,p.yCoord-ry,p.zCoord-rz-s,p.xCoord-rx,p.yCoord-ry,p.zCoord-rz+s,color); t.draw();
    }

    private void drawAimMarker(double rx,double ry,double rz) {
        AimPredictor.Solution s=predictor.getSolution(); if (s==null) return;
        Vec3 eye=mc.thePlayer.getPositionEyes(1F); Vec3 p=eye.addVector(s.direction.xCoord*10,s.direction.yCoord*10,s.direction.zCoord*10);
        int c=QModsTools.config.hitColor; double d=.15;
        Tessellator t=Tessellator.getInstance(); WorldRenderer w=t.getWorldRenderer(); GL11.glLineWidth(2.2F);
        w.begin(GL11.GL_LINE_LOOP,DefaultVertexFormats.POSITION_COLOR);
        vertex(w,p.xCoord-rx,p.yCoord-ry+d,p.zCoord-rz,c); vertex(w,p.xCoord-rx+d,p.yCoord-ry,p.zCoord-rz,c);
        vertex(w,p.xCoord-rx,p.yCoord-ry-d,p.zCoord-rz,c); vertex(w,p.xCoord-rx-d,p.yCoord-ry,p.zCoord-rz,c); t.draw();
    }

    private void line(WorldRenderer w,double x1,double y1,double z1,double x2,double y2,double z2,int c) { vertex(w,x1,y1,z1,c); vertex(w,x2,y2,z2,c); }
    private void vertex(WorldRenderer w,double x,double y,double z,int c) { w.pos(x,y,z).color(ColorUtil.r(c),ColorUtil.g(c),ColorUtil.b(c),.95F).endVertex(); }
    private boolean isDrawingBow() { return mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse()!=null && mc.thePlayer.getItemInUse().getItem() instanceof ItemBow; }
}
