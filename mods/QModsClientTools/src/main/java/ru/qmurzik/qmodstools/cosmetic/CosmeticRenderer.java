package ru.qmurzik.qmodstools.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class CosmeticRenderer {
    private CosmeticRenderer(){}

    public static void install(){
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        RenderPlayer normal = new LocalPlayerRenderer(manager, false);
        RenderPlayer slim = new LocalPlayerRenderer(manager, true);

        Field skinMapField = findSkinMapField(manager.getClass());
        if (skinMapField == null) {
            // Couldn't locate the backing field (unexpected mapping/obfuscation);
            // fall back to the public getter, which is what worked before OptiFine
            // started wrapping it in an unmodifiable view.
            putSkins(manager.getSkinMap(), normal, slim, null, null);
            return;
        }

        Map<String, RenderPlayer> raw;
        try {
            //noinspection unchecked
            raw = (Map<String, RenderPlayer>) skinMapField.get(manager);
        } catch (IllegalAccessException e) {
            putSkins(manager.getSkinMap(), normal, slim, null, null);
            return;
        }
        putSkins(raw, normal, slim, manager, skinMapField);
    }

    private static void putSkins(Map<String, RenderPlayer> skins, RenderPlayer normal, RenderPlayer slim,
                                  RenderManager manager, Field skinMapField) {
        try {
            skins.put("default", normal);
            skins.put("slim", slim);
        } catch (UnsupportedOperationException e) {
            // The map itself (not just a getter view of it) turned out to be
            // unmodifiable. Swap it out for a mutable copy holding the same data.
            if (manager == null || skinMapField == null) throw e;
            Map<String, RenderPlayer> mutable = new HashMap<String, RenderPlayer>(skins);
            mutable.put("default", normal);
            mutable.put("slim", slim);
            try {
                skinMapField.set(manager, mutable);
            } catch (IllegalAccessException ex) {
                throw e;
            }
        }
    }

    /** Finds the RenderManager field declared as Map&lt;String, RenderPlayer&gt;, independent of its name/obfuscation. */
    private static Field findSkinMapField(Class<?> managerClass) {
        for (Class<?> type = managerClass; type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                Type generic = field.getGenericType();
                if (!(generic instanceof ParameterizedType)) continue;
                Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
                if (args.length == 2 && args[0] == String.class
                        && args[1] instanceof Class
                        && RenderPlayer.class.isAssignableFrom((Class<?>) args[1])) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }
}
