package ru.qmurzik.qmodstools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import ru.qmurzik.qmodstools.feature.AimPredictor;
import ru.qmurzik.qmodstools.feature.BedWarsEvents;
import ru.qmurzik.qmodstools.feature.BedWarsHelper;
import ru.qmurzik.qmodstools.feature.CombatEffects;
import ru.qmurzik.qmodstools.feature.CpsCounter;
import ru.qmurzik.qmodstools.feature.IncomingTranslator;
import ru.qmurzik.qmodstools.feature.KillFeed;
import ru.qmurzik.qmodstools.feature.TrajectoryRenderer;
import ru.qmurzik.qmodstools.gui.GuiSettings;
import ru.qmurzik.qmodstools.gui.CustomEmojiChat;
import ru.qmurzik.qmodstools.hud.CustomChatRenderer;
import ru.qmurzik.qmodstools.hud.InfoHud;
import ru.qmurzik.qmodstools.hud.ScoreboardRenderer;
import ru.qmurzik.qmodstools.hud.AimHudRenderer;

public final class ClientEvents {
    private final Minecraft mc;
    private final AimPredictor predictor;
    private final TrajectoryRenderer trajectory;
    private final CustomChatRenderer chat;
    private final ScoreboardRenderer scoreboard;
    private final AimHudRenderer aimHud;
    private final BedWarsHelper bedWars;
    private final BedWarsEvents bedWarsEvents;
    private final CombatEffects combat;
    private final CpsCounter cps;
    private final InfoHud infoHud;
    private final KillFeed killFeed;
    private final IncomingTranslator translator;

    public ClientEvents(Minecraft mc) {
        this.mc=mc; predictor=new AimPredictor(mc); trajectory=new TrajectoryRenderer(mc,predictor);
        chat=new CustomChatRenderer(mc); scoreboard=new ScoreboardRenderer(mc);aimHud=new AimHudRenderer(mc,predictor);bedWars=new BedWarsHelper(mc);
        bedWarsEvents=new BedWarsEvents(mc);combat=new CombatEffects(mc);cps=new CpsCounter();infoHud=new InfoHud(mc,cps);
        killFeed=new KillFeed(mc);translator=new IncomingTranslator(mc);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return; predictor.tick();predictor.updateForCurrentBow();predictor.applySmoothAim();bedWars.tick();cps.tick();combat.tick();killFeed.tick();bedWarsEvents.tick();
        GuiIngameForge.renderObjective=!QModsTools.config.customScoreboard;
        if(QModsTools.config.emojiReplace&&mc.currentScreen instanceof GuiChat&&!(mc.currentScreen instanceof CustomEmojiChat)){
            String initial="";try{net.minecraft.client.gui.GuiTextField f=ReflectionHelper.getPrivateValue(GuiChat.class,(GuiChat)mc.currentScreen,"field_146415_a","inputField");if(f!=null)initial=f.getText();}catch(Exception ignored){}
            mc.displayGuiScreen(new CustomEmojiChat(initial));
        }
        if(QModsTools.settingsKey.isPressed())mc.displayGuiScreen(new GuiSettings(mc.currentScreen));
        if(QModsTools.trajectoryKey.isPressed()){QModsTools.config.trajectory=!QModsTools.config.trajectory;QModsTools.config.save();if(mc.thePlayer!=null)mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§dQMods §8• §fТраектория: "+(QModsTools.config.trajectory?"§aвключена":"§cвыключена")));}
    }

    @SubscribeEvent public void world(RenderWorldLastEvent e){trajectory.render(e);}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void overlay(RenderGameOverlayEvent.Pre e) {if(e.type==RenderGameOverlayEvent.ElementType.ALL&&QModsTools.config.customScoreboard)GuiIngameForge.renderObjective=false;}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void chatOverlay(RenderGameOverlayEvent.Chat e){if(QModsTools.config.customChat){e.setCanceled(true);chat.render(mc.ingameGUI.getUpdateCounter());}}

    @SubscribeEvent
    public void overlayPost(RenderGameOverlayEvent.Post e){if(e.type==RenderGameOverlayEvent.ElementType.CROSSHAIRS)aimHud.render();if(e.type==RenderGameOverlayEvent.ElementType.ALL){if(QModsTools.config.customScoreboard)scoreboard.render();bedWars.render();infoHud.render();combat.render();bedWarsEvents.render();killFeed.render();}}

    @SubscribeEvent
    public void shopOverlay(GuiScreenEvent.DrawScreenEvent.Post e){bedWars.drawShop(e);}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void shopKeys(GuiScreenEvent.KeyboardInputEvent.Pre e){bedWars.shopKey(e);}

    @SubscribeEvent
    public void hurt(LivingHurtEvent e){combat.onHurt(e);}

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e){bedWarsEvents.onChat(e);killFeed.onChat(e);translator.onChat(e);}
}
