package ru.qmurzik.qmodstools.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import ru.qmurzik.qmodstools.Config;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.feature.EmojiReplacer;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CustomChatRenderer {
    private final Minecraft mc;
    private final Map<ChatLine,String> times=new IdentityHashMap<ChatLine,String>();
    private final SimpleDateFormat clock=new SimpleDateFormat("HH:mm");

    public CustomChatRenderer(Minecraft mc){this.mc=mc;}

    @SuppressWarnings("unchecked")
    public void render(int updateCounter) {
        if (mc.gameSettings.chatVisibility == net.minecraft.entity.player.EntityPlayer.EnumChatVisibility.HIDDEN) return;
        GuiNewChat gui=mc.ingameGUI.getChatGUI();
        List<ChatLine> lines;
        int scroll=0;
        try {
            lines=ReflectionHelper.getPrivateValue(GuiNewChat.class,gui,"field_146253_i","drawnChatLines");
            Integer s=ReflectionHelper.getPrivateValue(GuiNewChat.class,gui,"field_146250_j","scrollPos"); if(s!=null)scroll=s;
        } catch(Exception e){return;}
        if(lines==null||lines.isEmpty())return;
        Config c=QModsTools.config; boolean open=mc.currentScreen instanceof GuiChat;
        float scale=mc.gameSettings.chatScale; int maxLines=getLineCount(open); int width=getWidth();
        int visible=0;
        for(int i=scroll;i<lines.size()&&visible<maxLines;i++){
            int age=updateCounter-lines.get(i).getUpdatedCounter(); if(age<200||open)visible++;
        }
        if(visible==0)return;
        ScaledResolution sr=new ScaledResolution(mc); int baseY=sr.getScaledHeight()-40+c.chatYOffset;
        int x=c.chatX; int panelW=(int)Math.ceil(width/scale)+8; int panelH=visible*9+7;
        GlStateManager.pushMatrix(); GlStateManager.translate(x,baseY,0); GlStateManager.scale(scale,scale,1);
        int bg=ColorUtil.argb(themeBackground(c.chatTheme),c.chatAlpha);
        if(c.chatTheme==0){DrawUtil.shadow(-2,-panelH,panelW,3,4);DrawUtil.rounded(-2,-panelH,panelW,3,4,bg);DrawUtil.gradient(-2,-panelH,panelW,-panelH+2,ColorUtil.argb(ColorUtil.brighten(c.chatAccent,40),210),ColorUtil.argb(c.chatAccent,80));}
        else if(c.chatTheme==1){DrawUtil.shadow(-3,-panelH,panelW+1,3,5);DrawUtil.rounded(-3,-panelH,panelW+1,3,5,bg);DrawUtil.rect(-3,-panelH,-1,3,ColorUtil.argb(c.chatAccent,230));DrawUtil.rect(-1,-panelH,panelW+1,-panelH+1,ColorUtil.argb(c.chatAccent,150));}
        else if(c.chatTheme==4){DrawUtil.shadow(-2,-panelH,panelW,3,6);DrawUtil.rounded(-2,-panelH,panelW,3,6,bg);DrawUtil.gradient(-2,-panelH,panelW,-panelH+3,0xEFFF78B5,0x70FFD5E7);}
        else if(c.chatTheme==5){DrawUtil.shadow(-3,-panelH,panelW+1,3,5);DrawUtil.rounded(-3,-panelH,panelW+1,3,5,bg);DrawUtil.gradient(-3,-panelH,-1,3,0xFFB060FF,0xFF45D7FF);DrawUtil.gradient(-1,-panelH,panelW+1,-panelH+2,0xD0B060FF,0x5045D7FF);}
        int row=0;
        for(int i=scroll;i<lines.size()&&row<maxLines;i++){
            ChatLine line=lines.get(i);int age=updateCounter-line.getUpdatedCounter();if(age>=200&&!open)continue;
            double fade=open?1.0:1.0-age/200.0;fade*=10;fade=MathHelper.clamp_double(fade,0,1);fade*=fade;
            int alpha=(int)(255*fade*mc.gameSettings.chatOpacity);if(alpha<4)continue;
            int y=-9*(row+1);int slide=c.chatAnimations&&!open?(int)((1-fade)*-12):0;
            if(c.chatTheme==2)DrawUtil.rounded(slide,y-1,panelW,y+8,3,ColorUtil.argb(themeBackground(c.chatTheme),Math.min(alpha,c.chatAlpha)));
            else if(c.chatTheme==3){}
            else DrawUtil.rect(slide-1,y-1,panelW,y+8,(Math.min(alpha,38)<<24));
            String text=line.getChatComponent().getFormattedText();if(c.emojiReplace)text=EmojiReplacer.replace(text);
            if(c.chatTimestamps){String tm=times.get(line);if(tm==null){tm=clock.format(new Date());times.put(line,tm);}text="§8["+tm+"] §r"+text;}
            if(c.chatMentionHighlight&&mc.thePlayer!=null&&isMention(text))DrawUtil.rect(slide-1,y-1,panelW,y+8,ColorUtil.argb(c.chatAccent,Math.min(70,alpha)));
            int textColor=(alpha<<24)|0xFFFFFF;mc.fontRendererObj.drawString(text,slide+2,y,textColor,c.chatShadow);row++;
        }
        GlStateManager.popMatrix();
    }

    private boolean isMention(String text){String clean=EnumChatFormatting.getTextWithoutFormattingCodes(text);return clean!=null&&clean.toLowerCase(Locale.ROOT).contains(mc.thePlayer.getName().toLowerCase(Locale.ROOT));}
    private int getLineCount(boolean open){float h=open?mc.gameSettings.chatHeightFocused:mc.gameSettings.chatHeightUnfocused;return MathHelper.floor_float((h*160F+20F)/9F);}
    private int getWidth(){return MathHelper.floor_float(mc.gameSettings.chatWidth*280F+40F);}
    private int themeBackground(int theme){switch(theme){case 1:return 0x100B1D;case 2:return 0x17121F;case 3:return 0;case 4:return 0x2A1521;case 5:return 0x0E1020;default:return 0x11131A;}}
}
