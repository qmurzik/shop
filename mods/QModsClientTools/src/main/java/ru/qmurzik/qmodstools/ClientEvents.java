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
import ru.qmurzik.qmodstools.feature.PingMeter;
import ru.qmurzik.qmodstools.feature.QModsV16;
import ru.qmurzik.qmodstools.feature.QModsV17;
import ru.qmurzik.qmodstools.feature.QModsV18;
import ru.qmurzik.qmodstools.feature.QModsV19;
import ru.qmurzik.qmodstools.feature.TrajectoryRenderer;
import ru.qmurzik.qmodstools.gui.PvPClickGui;
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
    private final PingMeter ping;

    public ClientEvents(Minecraft mc) {
        this.mc=mc; predictor=new AimPredictor(mc); trajectory=new TrajectoryRenderer(mc,predictor);
        chat=new CustomChatRenderer(mc); scoreboard=new ScoreboardRenderer(mc);aimHud=new AimHudRenderer(mc,predictor);bedWars=new BedWarsHelper(mc);
        bedWarsEvents=new BedWarsEvents(mc);combat=new CombatEffects(mc);cps=new CpsCounter();ping=new PingMeter(mc);infoHud=new InfoHud(mc,cps,ping);
        killFeed=new KillFeed(mc);translator=new IncomingTranslator(mc);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;QModsV18.optimizedTick(mc,predictor,bedWars,cps,combat,ping);QModsV16.tick(mc);
        GuiIngameForge.renderObjective=!QModsTools.config.customScoreboard;
        if(QModsTools.config.emojiReplace&&mc.currentScreen instanceof GuiChat&&!(mc.currentScreen instanceof CustomEmojiChat)){
            String initial="";try{net.minecraft.client.gui.GuiTextField f=ReflectionHelper.getPrivateValue(GuiChat.class,(GuiChat)mc.currentScreen,"field_146415_a","inputField");if(f!=null)initial=f.getText();}catch(Exception ignored){}
            mc.displayGuiScreen(new CustomEmojiChat(initial));
        }
        if(QModsTools.settingsKey.isPressed())mc.displayGuiScreen(new PvPClickGui(mc.currentScreen));
        if(QModsTools.trajectoryKey.isPressed()){QModsTools.config.trajectory=!QModsTools.config.trajectory;QModsTools.config.save();if(mc.thePlayer!=null)mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§dQMods §8• §fТраектория: "+(QModsTools.config.trajectory?"§aвключена":"§cвыключена")));}
    }

    @SubscribeEvent public void world(RenderWorldLastEvent e){trajectory.render(e);QModsV18.captureWorld(mc,e.partialTicks);}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void overlay(RenderGameOverlayEvent.Pre e) {QModsV19.onCrosshair(e,mc);if(e.type==RenderGameOverlayEvent.ElementType.ALL&&QModsTools.config.customScoreboard)GuiIngameForge.renderObjective=false;}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void chatOverlay(RenderGameOverlayEvent.Chat e){if(QModsTools.config.customChat){e.setCanceled(true);chat.render(mc.ingameGUI.getUpdateCounter());}}

    @SubscribeEvent
    public void overlayPost(RenderGameOverlayEvent.Post e){if(e.type==RenderGameOverlayEvent.ElementType.CROSSHAIRS)aimHud.render();if(e.type==RenderGameOverlayEvent.ElementType.ALL){if(QModsTools.config.customScoreboard)scoreboard.render();bedWars.render();if(QModsV18.shouldRenderInfo())infoHud.render();if(QModsV18.shouldRenderCombat(combat))combat.render();QModsV16.render(mc);}}

    @SubscribeEvent
    public void shopOverlay(GuiScreenEvent.DrawScreenEvent.Post e){bedWars.drawShop(e);}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void shopKeys(GuiScreenEvent.KeyboardInputEvent.Pre e){bedWars.shopKey(e);}

    @SubscribeEvent
    public void hurt(LivingHurtEvent e){combat.onHurt(e);QModsV17.onHurt(e,mc);QModsV19.onHurt(e,mc);}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void inventoryMouse(GuiScreenEvent.MouseInputEvent.Pre e){QModsV19.onGuiMouse(e,mc);}

    @SubscribeEvent
    public void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e){QModsV17.onSound(e);}

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent e){
        bedWars.observeChat(e);
        QModsV18.observeChat(e,mc);
        if(QModsV16.onChat(e))e.setCanceled(true);
        else translator.onChat(e);
    }
}
