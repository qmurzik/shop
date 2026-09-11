package ru.qmurzik.qmodstools.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
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
}
