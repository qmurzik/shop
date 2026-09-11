package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.QModsTools;

public final class BindManager {
    private final KeyBinding[] keys = new KeyBinding[8];

    public BindManager() {
        for (int i=0;i<keys.length;i++) {
            keys[i]=new KeyBinding("QMods: текстовый бинд " + (i+1), QModsTools.config.bindKeys[i], "key.categories.qmodstools");
            ClientRegistry.registerKeyBinding(keys[i]);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getMinecraft(); if (mc.thePlayer==null || mc.currentScreen!=null) return;
        for(int i=0;i<keys.length;i++) if(keys[i].isPressed()) {
            String text=QModsTools.config.bindText[i];
            if(text!=null && !text.trim().isEmpty()) mc.thePlayer.sendChatMessage(text.trim());
        }
    }

    public void setKey(int slot,int code) {
        if(slot<0||slot>=keys.length)return;
        keys[slot].setKeyCode(code); QModsTools.config.bindKeys[slot]=code;
        KeyBinding.resetKeyBindingArrayAndHash(); Minecraft.getMinecraft().gameSettings.saveOptions();
    }

    public String keyName(int slot) { int c=keys[slot].getKeyCode(); return c==0?"НЕТ":Keyboard.getKeyName(c); }
}
