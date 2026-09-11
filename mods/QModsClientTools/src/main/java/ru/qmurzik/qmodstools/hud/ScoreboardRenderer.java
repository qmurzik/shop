package ru.qmurzik.qmodstools.hud;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.*;
import net.minecraft.util.EnumChatFormatting;
import ru.qmurzik.qmodstools.Config;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;
import ru.qmurzik.qmodstools.feature.BedWarsHelper;

import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.Locale;

public final class ScoreboardRenderer {
    private final Minecraft mc;
    public ScoreboardRenderer(Minecraft mc){this.mc=mc;}

    public void render() {
        if(mc.theWorld==null)return; Scoreboard board=mc.theWorld.getScoreboard();
        ScoreObjective objective=null; ScorePlayerTeam team=board.getPlayersTeam(mc.thePlayer.getName());
        if(team!=null){int color=team.getChatFormat().getColorIndex();if(color>=0)objective=board.getObjectiveInDisplaySlot(3+color);}
        if(objective==null)objective=board.getObjectiveInDisplaySlot(1);if(objective==null)return;
        Collection<Score> scores=board.getSortedScores(objective);List<Score> filtered=Lists.newArrayList(Iterables.filter(scores,s->s.getPlayerName()!=null&&!s.getPlayerName().startsWith("#")));
        if(QModsTools.config.mineBlazeMode&&BedWarsHelper.isMineBlazeBedWars(mc)){
            Iterator<Score> it=filtered.iterator();while(it.hasNext()){Score s=it.next();String raw=ScorePlayerTeam.formatPlayerName(board.getPlayersTeam(s.getPlayerName()),s.getPlayerName());String clean=EnumChatFormatting.getTextWithoutFormattingCodes(raw);if(clean!=null){String low=clean.toLowerCase(Locale.ROOT);if(low.contains("сломано кроватей")||low.contains("финальных убийств")||low.contains("убийств:"))it.remove();}}
        }
        if(filtered.size()>15)filtered=Lists.newArrayList(Iterables.skip(filtered,filtered.size()-15));
        Config c=QModsTools.config;boolean showNumbers=c.scoreboardNumbers&&!(c.mineBlazeMode&&c.hideMineBlazeOrderNumbers&&BedWarsHelper.isMineBlazeBedWars(mc));int max=mc.fontRendererObj.getStringWidth(objective.getDisplayName());
        for(Score s:filtered){ScorePlayerTeam st=board.getPlayersTeam(s.getPlayerName());String name=ScorePlayerTeam.formatPlayerName(st,s.getPlayerName());String value="§c"+s.getScorePoints();max=Math.max(max,mc.fontRendererObj.getStringWidth(name)+(showNumbers?mc.fontRendererObj.getStringWidth(value)+5:0));}
        int w=max+14,h=(filtered.size()+1)*10+8;ScaledResolution sr=new ScaledResolution(mc);float scale=c.scoreboardScale;
        int anchorX=c.scoreboardSide==1?sr.getScaledWidth()-4:4;int x=c.scoreboardSide==1?(int)(anchorX/scale)-w:(int)(anchorX/scale);int y=(int)((sr.getScaledHeight()/2+c.scoreboardYOffset)/scale)-h/2;
        DrawUtil.beginHudScale(scale,0,0);int bg=ColorUtil.argb(themeBackground(c.scoreboardTheme),c.scoreboardAlpha);
        if(c.scoreboardTheme==0){DrawUtil.shadow(x,y,x+w,y+h,5);DrawUtil.rounded(x,y,x+w,y+h,5,bg);DrawUtil.gradient(x,y,x+w,y+3,ColorUtil.argb(ColorUtil.brighten(c.scoreboardAccent,45),220),ColorUtil.argb(c.scoreboardAccent,95));}
        else if(c.scoreboardTheme==1){DrawUtil.shadow(x,y,x+w,y+h,5);DrawUtil.rounded(x,y,x+w,y+h,5,bg);DrawUtil.rect(c.scoreboardSide==1?x+w-3:x,y,c.scoreboardSide==1?x+w:x+3,y+h,ColorUtil.argb(c.scoreboardAccent,235));}
        else if(c.scoreboardTheme==2){DrawUtil.rounded(x,y,x+w,y+13,4,ColorUtil.argb(c.scoreboardAccent,205));}
        else if(c.scoreboardTheme==4){DrawUtil.shadow(x,y,x+w,y+h,6);DrawUtil.rounded(x,y,x+w,y+h,6,bg);DrawUtil.gradient(x,y,x+w,y+4,0xF0FF77B5,0x80FFD5E7);}
        else if(c.scoreboardTheme==5){DrawUtil.shadow(x,y,x+w,y+h,5);DrawUtil.rounded(x,y,x+w,y+h,5,bg);DrawUtil.gradient(x,y,x+w,y+3,0xE0B060FF,0x9045D7FF);DrawUtil.rect(c.scoreboardSide==1?x+w-2:x,y+3,c.scoreboardSide==1?x+w:x+2,y+h,0xFF45D7FF);}
        int titleX=x+(w-mc.fontRendererObj.getStringWidth(objective.getDisplayName()))/2;mc.fontRendererObj.drawString(objective.getDisplayName(),titleX,y+3,0xFFFFFFFF,c.scoreboardShadow);
        int row=0;for(Score s:filtered){int yy=y+16+row*10;ScorePlayerTeam st=board.getPlayersTeam(s.getPlayerName());String name=ScorePlayerTeam.formatPlayerName(st,s.getPlayerName());String val="§c"+s.getScorePoints();
            if(c.scoreboardTheme==2)DrawUtil.rounded(x,yy-2,x+w,yy+8,3,ColorUtil.argb(themeBackground(c.scoreboardTheme),c.scoreboardAlpha));
            else if(c.scoreboardTheme!=3)DrawUtil.rect(x+3,yy-2,x+w-3,yy+8,(Math.min(28,c.scoreboardAlpha)<<24));
            mc.fontRendererObj.drawString(name,x+6,yy,0xFFFFFFFF,c.scoreboardShadow);if(showNumbers)mc.fontRendererObj.drawString(val,x+w-6-mc.fontRendererObj.getStringWidth(val),yy,0xFFFFFFFF,c.scoreboardShadow);row++;}
        DrawUtil.endHudScale();
    }

    private int themeBackground(int t){switch(t){case 1:return 0x10091C;case 2:return 0x18131F;case 3:return 0;case 4:return 0x2B1522;case 5:return 0x0D1020;default:return 0x11141C;}}
}
