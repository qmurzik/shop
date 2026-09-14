package ru.qmurzik.qmodstools.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.*;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BedWarsHelper {
    private static final Pattern NUMBER=Pattern.compile("(\\d+)");
    private static Object observedWorld;
    private static long chatMatchEvidenceUntilMs;
    private final Minecraft mc;
    private boolean active,ownBed=true,knownBed;
    private int startedTick=-1,alertUntil=-1,beds,finals,kills;
    private boolean rejoinPending;
    private long rejoinAtMs,rejoinDeadlineMs,lastRejoinMs;
    private Vec3 basePos;
    private long lastBaseAlertMs;
    private long baseThreatUntilMs;
    private String baseThreatName="враг";

    public BedWarsHelper(Minecraft mc){this.mc=mc;}

    public void tick(){
        processPendingRejoin();
        if(QModsTools.rejoinKey!=null&&QModsTools.rejoinKey.isPressed()&&isMineBlazeServer())startRejoin("ручной бинд");
        boolean now=isMatchInProgress(mc);if(!now){active=false;startedTick=-1;knownBed=false;basePos=null;return;}
        Scoreboard b=mc.theWorld.getScoreboard();ScoreObjective o=objective(b,mc);if(o==null)return;
        if(!active){active=true;startedTick=mc.thePlayer.ticksExisted;beds=finals=kills=0;knownBed=false;basePos=new Vec3(mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ);}
        Boolean currentBed=null;boolean sawStats=false;
        for(Score s:b.getSortedScores(o)){
            String line=EnumChatFormatting.getTextWithoutFormattingCodes(ScorePlayerTeam.formatPlayerName(b.getPlayersTeam(s.getPlayerName()),s.getPlayerName()));if(line==null)continue;
            String low=line.toLowerCase(Locale.ROOT);
            if(low.contains("сломано кроватей")){beds=number(line,beds);sawStats=true;}
            else if(low.contains("финальных убийств")){finals=number(line,finals);sawStats=true;}
            else if(low.contains("убийств")){kills=number(line,kills);sawStats=true;}
            if(low.contains("(вы)"))currentBed=!(line.contains("✘")||line.contains("×")||low.contains(" x "));
        }
        if(sawStats)chatMatchEvidenceUntilMs=Math.max(chatMatchEvidenceUntilMs,System.currentTimeMillis()+2500L);
        if(currentBed!=null){if(knownBed&&ownBed&&!currentBed&&QModsTools.config.bedAlert)alertUntil=mc.thePlayer.ticksExisted+120;ownBed=currentBed;knownBed=true;}
        if(QModsTools.config.autoVoidRejoin&&shouldVoidRejoin())startRejoin("падение в бездну");
        int elapsed=mc.thePlayer.ticksExisted-startedTick;
        if(QModsTools.config.baseAlert&&basePos!=null&&elapsed>200&&mc.thePlayer.ticksExisted%20==0)checkBaseAlert(b);
    }

    /** True once a real match (not the shared pre-game lobby) is confirmed running, per the current scoreboard.
     *  Deliberately does NOT require isMineBlazeBedWars() - that heuristic depends on the objective title
     *  literally containing "bedwars", which isn't guaranteed to match this server's actual wording/styling,
     *  and would silently disable match detection (and everything built on it, incl. auto-rejoin) if it doesn't. */
    public static boolean isMatchInProgress(Minecraft mc){
        if(mc==null||mc.theWorld==null||mc.thePlayer==null)return false;
        if(observedWorld!=mc.theWorld){observedWorld=mc.theWorld;chatMatchEvidenceUntilMs=0L;}
        Scoreboard b=mc.theWorld.getScoreboard();ScoreObjective o=objective(b,mc);if(o==null)return false;
        for(Score s:b.getSortedScores(o)){
            String low=normalize(ScorePlayerTeam.formatPlayerName(b.getPlayersTeam(s.getPlayerName()),s.getPlayerName()));
            if(isMatchScoreLine(low))return true;
        }
        return System.currentTimeMillis()<chatMatchEvidenceUntilMs;
    }

    /** Must run before feature chat handlers: the broadcast itself can be the first reliable match evidence. */
    public void observeChat(ClientChatReceivedEvent e){
        if(e==null||e.message==null||e.type==2||mc.theWorld==null||mc.thePlayer==null)return;
        String low=normalize(e.message.getUnformattedText());if(low.isEmpty())return;
        boolean matchEvent=(low.contains("кроват")&&(low.contains("уничтож")||low.contains("разруш")||low.contains("сломан")))
                ||low.contains("final kill")||low.contains("финальн")
                ||low.contains("игра нач")||low.contains("game start")||low.contains("защитите свою кровать");
        if(matchEvent)chatMatchEvidenceUntilMs=System.currentTimeMillis()+15000L;
        if(low.contains("возвращение в лобби")||low.contains("waiting for players")||low.contains("ожидание игроков"))chatMatchEvidenceUntilMs=0L;
    }

    private void checkBaseAlert(Scoreboard b){
        long now=System.currentTimeMillis();if(now-lastBaseAlertMs<45000L)return;
        ScorePlayerTeam myTeam=b.getPlayersTeam(mc.thePlayer.getName());
        for(Object raw:mc.theWorld.playerEntities){
            EntityPlayer p=(EntityPlayer)raw;if(p==mc.thePlayer)continue;
            ScorePlayerTeam theirTeam=b.getPlayersTeam(p.getName());
            // Only skip when both are confirmed on the SAME team - if team data isn't available
            // on this server (both null), fall back to just distance so the alert still works.
            if(sameTeam(myTeam,theirTeam)||sameNameColor(mc.thePlayer,p))continue;
            double dx=p.posX-basePos.xCoord,dy=p.posY-basePos.yCoord,dz=p.posZ-basePos.zCoord;
            if(Math.sqrt(dx*dx+dy*dy+dz*dz)<=QModsTools.config.baseAlertRadius){
                lastBaseAlertMs=now;baseThreatUntilMs=now+4500L;baseThreatName=p.getDisplayName().getFormattedText();
                mc.thePlayer.playSound("random.orb",0.8F,0.65F);return;
            }
        }
    }

    public void render(){
        if(!QModsTools.config.mineBlazeMode||!QModsTools.config.bedWarsHud||!active||mc.currentScreen!=null)return;
        ScaledResolution sr=new ScaledResolution(mc);int tick=mc.thePlayer.ticksExisted;int seconds=Math.max(0,(tick-startedTick)/20);
        String stats=String.format("§6BEDWARS §8• §f%02d:%02d §8• §dКровати §f%d §8• §cФиналы §f%d §8• §7Убийства §f%d",seconds/60,seconds%60,beds,finals,kills);
        int sw=mc.fontRendererObj.getStringWidth(stats)+16,x=sr.getScaledWidth()/2-sw/2;
        DrawUtil.shadow(x,7,x+sw,27,4);DrawUtil.rounded(x,7,x+sw,27,4,0xC012151D);DrawUtil.rect(x,7,x+sw,9,ColorUtil.argb(0xB060FF,220));mc.fontRendererObj.drawString(stats,x+8,13,0xFFFFFFFF,true);
        List<String> labelList=new ArrayList<String>();List<Integer> amountList=new ArrayList<Integer>();
        labelList.add("§fFe");amountList.add(count(Items.iron_ingot));
        labelList.add("§6Au");amountList.add(count(Items.gold_ingot));
        labelList.add("§b♦");amountList.add(count(Items.diamond));
        labelList.add("§a✦");amountList.add(count(Items.emerald));
        int arrowIdx=labelList.size();labelList.add("§f➶");amountList.add(count(Items.arrow));
        int blockIdx=labelList.size();labelList.add("§f▦");amountList.add(countBlocks());
        if(QModsTools.config.genTimers){int secs=Math.max(0,(tick-startedTick)/20);
            labelList.add("§b⏱♦");amountList.add(QModsTools.config.diamondGenInterval-(secs%QModsTools.config.diamondGenInterval));
            labelList.add("§a⏱✦");amountList.add(QModsTools.config.emeraldGenInterval-(secs%QModsTools.config.emeraldGenInterval));
        }
        int total=labelList.size()*45,gx=sr.getScaledWidth()/2-total/2,gy=31;
        for(int i=0;i<labelList.size();i++){int bx=gx+i*45;int amount=amountList.get(i);
            boolean low=QModsTools.config.shopReminder&&((i==arrowIdx&&amount<QModsTools.config.lowArrowThreshold)||(i==blockIdx&&amount<QModsTools.config.lowBlockThreshold));
            DrawUtil.rounded(bx,gy,bx+41,gy+17,4,low?0xCC5A1620:0xAD171A22);
            mc.fontRendererObj.drawString(labelList.get(i)+" "+(low?"§c":"§f")+amount,bx+6,gy+5,0xFFFFFFFF,true);}
        if(alertUntil>tick){String warn="§c§lКРОВАТЬ СЛОМАНА §8• §fвозрождения больше нет";int ww=mc.fontRendererObj.getStringWidth(warn)+20,wx=sr.getScaledWidth()/2-ww/2,wy=55;DrawUtil.shadow(wx,wy,wx+ww,wy+24,5);DrawUtil.rounded(wx,wy,wx+ww,wy+24,5,0xD8240D14);DrawUtil.rect(wx,wy,wx+ww,wy+3,0xFFFF3F62);mc.fontRendererObj.drawString(warn,wx+10,wy+8,0xFFFFFFFF,true);}
        if(baseThreatUntilMs>System.currentTimeMillis()){
            String warn="§c§lВРАГ У БАЗЫ §8• §f"+baseThreatName;int ww=mc.fontRendererObj.getStringWidth(warn)+20,wx=sr.getScaledWidth()/2-ww/2,wy=alertUntil>tick?83:55;
            DrawUtil.shadow(wx,wy,wx+ww,wy+24,5);DrawUtil.rounded(wx,wy,wx+ww,wy+24,5,0xDF240D14);DrawUtil.gradient(wx,wy,wx+ww,wy+3,0xFFFF355D,0xFFFFA34D);mc.fontRendererObj.drawString(warn,wx+10,wy+8,0xFFFFFFFF,true);
        }
        if(rejoinPending){long left=Math.max(0,rejoinAtMs-System.currentTimeMillis());String text="§dQMods §8• §f/rejoin через §d"+String.format("%.1f",left/1000D)+"с";int w=mc.fontRendererObj.getStringWidth(text)+16,rx=sr.getScaledWidth()/2-w/2,ry=55;DrawUtil.rounded(rx,ry,rx+w,ry+19,5,0xD012151D);mc.fontRendererObj.drawString(text,rx+8,ry+6,0xFFFFFFFF,true);}
    }

    public void drawShop(GuiScreenEvent.DrawScreenEvent.Post e){
        if(!QModsTools.config.mineBlazeMode||!QModsTools.config.shopHelper||!(e.gui instanceof GuiChest)||!isShop((GuiChest)e.gui))return;
        GuiChest gui=(GuiChest)e.gui;int left,top;try{left=ReflectionHelper.getPrivateValue(GuiContainer.class,gui,"field_147003_i","guiLeft");top=ReflectionHelper.getPrivateValue(GuiContainer.class,gui,"field_147009_r","guiTop");}catch(Exception ex){return;}
        ContainerChest container=(ContainerChest)gui.inventorySlots;int shopSize=container.getLowerChestInventory().getSizeInventory();
        Cost hovered=null;ItemStack hoveredStack=null;
        for(int i=0;i<Math.min(shopSize,container.inventorySlots.size());i++){Slot slot=(Slot)container.inventorySlots.get(i);if(!slot.getHasStack())continue;int x=left+slot.xDisplayPosition,y=top+slot.yDisplayPosition;
            if(i<9&&QModsTools.config.quickShopKeys){DrawUtil.rounded(x-2,y-3,x+6,y+6,3,0xE012151D);mc.fontRendererObj.drawString("§f"+(i+1),x,y-2,0xFFFFFFFF,true);}
            Cost cost=cost(slot.getStack());if(cost==null)continue;int have=count(cost.item);boolean ok=have>=cost.amount;int c=ok?0x55FF88:0xFF496A;
            DrawUtil.rect(x-2,y-2,x+18,y+18,ColorUtil.argb(c,45));DrawUtil.rect(x-2,y-2,x+18,y,ColorUtil.argb(c,245));DrawUtil.rect(x-2,y+16,x+18,y+18,ColorUtil.argb(c,245));DrawUtil.rect(x-2,y,x,y+16,ColorUtil.argb(c,245));DrawUtil.rect(x+16,y,x+18,y+16,ColorUtil.argb(c,245));
            DrawUtil.rounded(x+10,y-4,x+20,y+6,4,ColorUtil.argb(c,245));mc.fontRendererObj.drawString(ok?"§f✓":"§f×",x+12,y-3,0xFFFFFFFF,true);
            if(e.mouseX>=x-2&&e.mouseX<=x+18&&e.mouseY>=y-2&&e.mouseY<=y+18){hovered=cost;hoveredStack=slot.getStack();}
        }
        String hint=QModsTools.config.quickShopKeys?"§f1–9 §7— вкладки  •  §a✓ хватает  §8•  §c× не хватает":"§a✓ хватает ресурсов  §8•  §c× не хватает";int w=mc.fontRendererObj.getStringWidth(hint)+16,x=left+(176-w)/2,y=top-19;DrawUtil.shadow(x,y,x+w,y+16,4);DrawUtil.rounded(x,y,x+w,y+16,4,0xE012151D);DrawUtil.gradient(x,y,x+w,y+2,0xE0B060FF,0xA045D7FF);mc.fontRendererObj.drawString(hint,x+8,y+4,0xFFFFFFFF,true);
        if(hovered!=null&&hoveredStack!=null){int have=count(hovered.item);boolean ok=have>=hovered.amount;String name=hoveredStack.getDisplayName();String status=(ok?"§aМОЖНО КУПИТЬ":"§cНЕ ХВАТАЕТ")+" §8• §f"+have+"/"+hovered.amount+" "+hovered.label;int pw=Math.max(mc.fontRendererObj.getStringWidth(name),mc.fontRendererObj.getStringWidth(status))+18,px=left+88-pw/2,py=top-43;DrawUtil.shadow(px,py,px+pw,py+34,5);DrawUtil.rounded(px,py,px+pw,py+34,5,0xEE11141C);DrawUtil.rect(px,py,px+pw,py+3,ColorUtil.argb(ok?0x55FF88:0xFF496A,240));mc.fontRendererObj.drawString(name,px+9,py+7,0xFFFFFFFF,true);mc.fontRendererObj.drawString(status,px+9,py+20,0xFFFFFFFF,true);}
    }

    public void shopKey(GuiScreenEvent.KeyboardInputEvent.Pre e){
        if(!QModsTools.config.mineBlazeMode||!QModsTools.config.quickShopKeys||!(e.gui instanceof GuiChest)||!Keyboard.getEventKeyState()||!isShop((GuiChest)e.gui))return;
        int key=Keyboard.getEventKey();if(key<Keyboard.KEY_1||key>Keyboard.KEY_9)return;GuiChest gui=(GuiChest)e.gui;ContainerChest c=(ContainerChest)gui.inventorySlots;int index=key-Keyboard.KEY_1;if(index<c.getLowerChestInventory().getSizeInventory()&&index<c.inventorySlots.size()){Slot slot=(Slot)c.inventorySlots.get(index);mc.playerController.windowClick(c.windowId,slot.slotNumber,0,0,mc.thePlayer);e.setCanceled(true);}
    }

    public static boolean isMineBlazeBedWars(Minecraft mc){
        if(mc==null||mc.theWorld==null||mc.thePlayer==null)return false;String ip=mc.getCurrentServerData()==null?"":mc.getCurrentServerData().serverIP.toLowerCase(Locale.ROOT);ScoreObjective o=objective(mc.theWorld.getScoreboard(),mc);String title=o==null?"":EnumChatFormatting.getTextWithoutFormattingCodes(o.getDisplayName());return (ip.contains("mineblaze")&&(title!=null&&title.toLowerCase(Locale.ROOT).contains("bedwars")))||(title!=null&&title.toLowerCase(Locale.ROOT).contains("bedwars")&&containsMineBlaze(mc.theWorld.getScoreboard(),o));
    }
    private static boolean containsMineBlaze(Scoreboard b,ScoreObjective o){if(o==null)return false;for(Score s:b.getSortedScores(o)){String line=EnumChatFormatting.getTextWithoutFormattingCodes(s.getPlayerName());if(line!=null&&line.toLowerCase(Locale.ROOT).contains("mineblaze"))return true;}return false;}
    private static boolean isMatchScoreLine(String low){return low.contains("сломано")&&low.contains("кроват")||low.contains("финальн")&&low.contains("убий")||low.contains("kills:")||low.contains("beds broken");}
    private static String normalize(String s){String clean=EnumChatFormatting.getTextWithoutFormattingCodes(s);return clean==null?"":clean.replace('\u00A0',' ').replace('ё','е').trim().toLowerCase(Locale.ROOT);}
    private static boolean sameTeam(ScorePlayerTeam a,ScorePlayerTeam b){return a!=null&&b!=null&&(a==b||a.getRegisteredName().equals(b.getRegisteredName()));}
    private static boolean sameNameColor(EntityPlayer a,EntityPlayer b){
        String aa=a.getDisplayName().getFormattedText(),bb=b.getDisplayName().getFormattedText();
        int ca=firstColor(aa),cb=firstColor(bb);return ca>=0&&ca==cb;
    }
    private static int firstColor(String s){
        if(s==null)return -1;for(int i=0;i+1<s.length();i++)if(s.charAt(i)=='§'){
            int c="0123456789abcdef".indexOf(Character.toLowerCase(s.charAt(i+1)));if(c>=0)return c;
        }return -1;
    }
    private static ScoreObjective objective(Scoreboard b,Minecraft mc){ScorePlayerTeam t=b.getPlayersTeam(mc.thePlayer.getName());if(t!=null&&t.getChatFormat().getColorIndex()>=0){ScoreObjective o=b.getObjectiveInDisplaySlot(3+t.getChatFormat().getColorIndex());if(o!=null)return o;}return b.getObjectiveInDisplaySlot(1);}
    private boolean isShop(GuiChest gui){String n=((ContainerChest)gui.inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();return n!=null&&n.toLowerCase(Locale.ROOT).contains("магазин");}
    private boolean isMineBlazeServer(){return mc.getCurrentServerData()!=null&&mc.getCurrentServerData().serverIP.toLowerCase(Locale.ROOT).contains("mineblaze");}
    private boolean shouldVoidRejoin(){if(mc.thePlayer==null||mc.theWorld==null||mc.thePlayer.capabilities.isFlying||mc.thePlayer.onGround||mc.thePlayer.motionY>=-0.045D||mc.thePlayer.fallDistance<3F||mc.thePlayer.posY>=QModsTools.config.voidRejoinY)return false;return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,mc.thePlayer.getEntityBoundingBox().offset(0,-6,0).expand(.2,0,.2)).isEmpty();}
    private void startRejoin(String reason){long now=System.currentTimeMillis();if(rejoinPending||now-lastRejoinMs<12000L||mc.thePlayer==null)return;lastRejoinMs=now;rejoinPending=true;rejoinAtMs=now+QModsTools.config.rejoinDelayMs;rejoinDeadlineMs=now+9000L;mc.thePlayer.sendChatMessage("/leave");mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§dQMods §8• §fБыстрый перезаход: §7"+reason));}
    private void processPendingRejoin(){if(!rejoinPending)return;long now=System.currentTimeMillis();if(now>rejoinDeadlineMs){rejoinPending=false;return;}if(now>=rejoinAtMs&&mc.thePlayer!=null){mc.thePlayer.sendChatMessage("/rejoin");mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("§dQMods §8• §fОтправлена команда §d/rejoin"));rejoinPending=false;}}
    private int number(String s,int fallback){Matcher m=NUMBER.matcher(s);return m.find()?Integer.parseInt(m.group(1)):fallback;}
    private int count(Item item){int n=0;for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null&&s.getItem()==item)n+=s.stackSize;return n;}
    private int countBlocks(){int n=0;for(ItemStack s:mc.thePlayer.inventory.mainInventory)if(s!=null&&s.getItem() instanceof ItemBlock)n+=s.stackSize;return n;}
    private Cost cost(ItemStack stack){List<String> tip=stack.getTooltip(mc.thePlayer,false);for(String raw:tip){String s=EnumChatFormatting.getTextWithoutFormattingCodes(raw);if(s==null)continue;s=s.toLowerCase(Locale.ROOT);Matcher m=NUMBER.matcher(s);if(!m.find())continue;int n=Integer.parseInt(m.group(1));if(s.contains("желез"))return new Cost(Items.iron_ingot,n,"железа");if(s.contains("золот"))return new Cost(Items.gold_ingot,n,"золота");if(s.contains("алмаз"))return new Cost(Items.diamond,n,"алмазов");if(s.contains("изумруд"))return new Cost(Items.emerald,n,"изумрудов");}return null;}
    private static final class Cost{final Item item;final int amount;final String label;Cost(Item item,int amount,String label){this.item=item;this.amount=amount;this.label=label;}}
}
