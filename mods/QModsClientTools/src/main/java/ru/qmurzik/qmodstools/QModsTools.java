package ru.qmurzik.qmodstools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.command.CommandQTools;
import ru.qmurzik.qmodstools.feature.BindManager;
import ru.qmurzik.qmodstools.cosmetic.CosmeticRenderer;

@Mod(modid = QModsTools.MOD_ID, name = QModsTools.NAME, version = QModsTools.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", acceptableRemoteVersions = "*", clientSideOnly = true)
public class QModsTools {
    public static final String MOD_ID = "qmodstools";
    public static final String NAME = "QMods Client Tools";
    public static final String VERSION = "1.5.1";
    public static Config config;
    public static KeyBinding settingsKey;
    public static KeyBinding trajectoryKey;
    public static KeyBinding rejoinKey;
    public static BindManager binds;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Config(event.getSuggestedConfigurationFile());
        config.load();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        settingsKey = new KeyBinding("key.qmodstools.settings", Keyboard.KEY_RSHIFT, "key.categories.qmodstools");
        trajectoryKey = new KeyBinding("key.qmodstools.trajectory", Keyboard.KEY_P, "key.categories.qmodstools");
        rejoinKey = new KeyBinding("key.qmodstools.rejoin", Keyboard.KEY_NONE, "key.categories.qmodstools");
        ClientRegistry.registerKeyBinding(settingsKey);
        ClientRegistry.registerKeyBinding(trajectoryKey);
        ClientRegistry.registerKeyBinding(rejoinKey);
        CosmeticRenderer.install();

        binds = new BindManager();
        ClientEvents events = new ClientEvents(Minecraft.getMinecraft());
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance().bus().register(events);
        FMLCommonHandler.instance().bus().register(binds);
        ClientCommandHandler.instance.registerCommand(new CommandQTools());
    }
}
