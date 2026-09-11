package ru.qmurzik.qmodstools.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import ru.qmurzik.qmodstools.QModsTools;

public final class QModsCapeLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final ResourceLocation CAPE=new ResourceLocation("qmodstools","textures/cosmetic/qmods_cape.png");
    private final RenderPlayer renderer;
    public QModsCapeLayer(RenderPlayer renderer){this.renderer=renderer;}

    @Override public void doRenderLayer(AbstractClientPlayer player,float limbSwing,float limbSwingAmount,float partialTicks,float age,float headYaw,float headPitch,float scale){
        if(player!=Minecraft.getMinecraft().thePlayer||!QModsTools.config.localQModsCape||player.isInvisible())return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(CAPE);GlStateManager.pushMatrix();GlStateManager.translate(0,0,0.125F);
        double dx=lerp(player.prevChasingPosX,player.chasingPosX,partialTicks)-lerp(player.prevPosX,player.posX,partialTicks);
        double dy=lerp(player.prevChasingPosY,player.chasingPosY,partialTicks)-lerp(player.prevPosY,player.posY,partialTicks);
        double dz=lerp(player.prevChasingPosZ,player.chasingPosZ,partialTicks)-lerp(player.prevPosZ,player.posZ,partialTicks);
        float yaw=player.prevRenderYawOffset+(player.renderYawOffset-player.prevRenderYawOffset)*partialTicks;
        double sin=MathHelper.sin(yaw*(float)Math.PI/180F),cos=-MathHelper.cos(yaw*(float)Math.PI/180F);
        float lift=(float)dy*10F;lift=MathHelper.clamp_float(lift,-6F,32F);
        float back=(float)(dx*sin+dz*cos)*100F;back=MathHelper.clamp_float(back,0F,150F);
        float side=(float)(dx*cos-dz*sin)*100F;side=MathHelper.clamp_float(side,-20F,20F);
        float camera=player.prevCameraYaw+(player.cameraYaw-player.prevCameraYaw)*partialTicks;
        lift+=MathHelper.sin((player.prevDistanceWalkedModified+(player.distanceWalkedModified-player.prevDistanceWalkedModified)*partialTicks)*6F)*32F*camera;
        if(player.isSneaking())lift+=25F;
        GlStateManager.rotate(6F+back/2F+lift,1,0,0);GlStateManager.rotate(side/2F,0,0,1);GlStateManager.rotate(-side/2F,0,1,0);GlStateManager.rotate(180F,0,1,0);
        renderer.getMainModel().renderCape(0.0625F);GlStateManager.popMatrix();
    }
    private double lerp(double a,double b,float p){return a+(b-a)*p;}
    @Override public boolean shouldCombineTextures(){return false;}
}
