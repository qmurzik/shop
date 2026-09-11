package ru.qmurzik.qmodstools.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class CosmeticRenderer {
    private CosmeticRenderer(){}

    public static void install(){
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        Map<String, RenderPlayer> skins = manager.getSkinMap();
        RenderPlayer normal = new LocalPlayerRenderer(manager, false);
        RenderPlayer slim = new LocalPlayerRenderer(manager, true);
        try {
            skins.put("default", normal);
            skins.put("slim", slim);
        } catch (UnsupportedOperationException e) {
            // Some environments (e.g. OptiFine) expose the skin map through an
            // unmodifiable view. Replace the backing field with a mutable copy instead.
            Map<String, RenderPlayer> mutable = new HashMap<String, RenderPlayer>(skins);
            mutable.put("default", normal);
            mutable.put("slim", slim);
            if (!replaceSkinMap(manager, skins, mutable)) {
                throw e;
            }
        }
    }

    private static boolean replaceSkinMap(RenderManager manager, Map<String, RenderPlayer> oldMap, Map<String, RenderPlayer> newMap) {
        for (Class<?> type = manager.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                try {
                    if (field.get(manager) == oldMap) {
                        field.set(manager, newMap);
                        return true;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return false;
    }
}
