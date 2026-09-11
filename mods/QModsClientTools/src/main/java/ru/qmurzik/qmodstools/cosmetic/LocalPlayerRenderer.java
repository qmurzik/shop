package ru.qmurzik.qmodstools.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import ru.qmurzik.qmodstools.QModsTools;

import java.util.Iterator;

public final class LocalPlayerRenderer extends RenderPlayer {
    private static final ResourceLocation KIRA=new ResourceLocation("qmodstools","textures/cosmetic/kira.png");

    public LocalPlayerRenderer(RenderManager manager,boolean slim){
        super(manager,slim);
        Iterator<LayerRenderer<AbstractClientPlayer>> it=layerRenderers.iterator();
        while(it.hasNext())if(it.next() instanceof net.minecraft.client.renderer.entity.layers.LayerCape)it.remove();
        addLayer(new SelectiveCapeLayer(this));
        addLayer(new QModsCapeLayer(this));
    }

    @Override protected ResourceLocation getEntityTexture(AbstractClientPlayer player){
        if(player==Minecraft.getMinecraft().thePlayer&&QModsTools.config.localKiraSkin)return KIRA;
        return super.getEntityTexture(player);
    }

    @Override public void doRender(AbstractClientPlayer entity,double x,double y,double z,float yaw,float partialTicks){
        boolean tinted=entity==Minecraft.getMinecraft().thePlayer&&QModsTools.config.localKiraSkin&&QModsTools.config.cosmeticVariant!=0;
        if(tinted){float[] c=tint(QModsTools.config.cosmeticVariant);GlStateManager.color(c[0],c[1],c[2],1F);}
        super.doRender(entity,x,y,z,yaw,partialTicks);
        if(tinted)GlStateManager.color(1F,1F,1F,1F);
    }

    private static float[] tint(int variant){
        switch(variant){case 1:return new float[]{0.55F,0.78F,1F};case 2:return new float[]{1F,0.5F,0.5F};default:return new float[]{1F,1F,1F};}
    }
}
