package ru.qmurzik.qmodstools.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import ru.qmurzik.qmodstools.QModsTools;

/** Keeps normal capes for everybody, but avoids drawing two capes on the local player. */
public final class SelectiveCapeLayer extends LayerCape {
    public SelectiveCapeLayer(RenderPlayer renderer) { super(renderer); }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                              float partialTicks, float age, float headYaw, float headPitch, float scale) {
        if (player == Minecraft.getMinecraft().thePlayer && QModsTools.config.localQModsCape) return;
        super.doRenderLayer(player, limbSwing, limbSwingAmount, partialTicks, age, headYaw, headPitch, scale);
    }
}
