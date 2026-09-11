package ru.qmurzik.qmodstools.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.feature.AimPredictor;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

public final class AimHudRenderer {
    private final Minecraft mc;
    private final AimPredictor predictor;
    public AimHudRenderer(Minecraft mc,AimPredictor predictor){this.mc=mc;this.predictor=predictor;}

    public void render(){
        if(!QModsTools.config.aimGuide||mc.thePlayer==null)return;
        AimPredictor.Solution s=predictor.getSolution();if(s==null)return;
        ScaledResolution sr=new ScaledResolution(mc);int cx=sr.getScaledWidth()/2,cy=sr.getScaledHeight()/2;
        float dyaw=MathHelper.wrapAngleTo180_float(s.yaw-mc.thePlayer.rotationYaw);
        float dpitch=MathHelper.wrapAngleTo180_float(s.pitch-mc.thePlayer.rotationPitch);
        float fov=Math.max(30F,mc.gameSettings.fovSetting);
        int dx=(int)(dyaw*sr.getScaledWidth()/fov);
        int dy=(int)(dpitch*sr.getScaledHeight()/fov);
        int limitX=cx-18,limitY=cy-18;dx=MathHelper.clamp_int(dx,-limitX,limitX);dy=MathHelper.clamp_int(dy,-limitY,limitY);
        int x=cx+dx,y=cy+dy;boolean locked=Math.abs(dyaw)<1.15F&&Math.abs(dpitch)<1.15F;
        int color=locked?QModsTools.config.hitColor:QModsTools.config.trajectoryColor;
        DrawUtil.rounded(x-7,y-1,x+8,y+2,1,ColorUtil.argb(color,230));
        DrawUtil.rounded(x-1,y-7,x+2,y+8,1,ColorUtil.argb(color,230));
        DrawUtil.rect(x-10,y-10,x-5,y-9,ColorUtil.argb(color,190));DrawUtil.rect(x-10,y-10,x-9,y-5,ColorUtil.argb(color,190));
        DrawUtil.rect(x+5,y-10,x+10,y-9,ColorUtil.argb(color,190));DrawUtil.rect(x+9,y-10,x+10,y-5,ColorUtil.argb(color,190));
        DrawUtil.rect(x-10,y+9,x-5,y+10,ColorUtil.argb(color,190));DrawUtil.rect(x-10,y+5,x-9,y+10,ColorUtil.argb(color,190));
        DrawUtil.rect(x+5,y+9,x+10,y+10,ColorUtil.argb(color,190));DrawUtil.rect(x+9,y+5,x+10,y+10,ColorUtil.argb(color,190));
        String name=s.target.getDisplayName().getFormattedText();String info=(locked?"§aГОТОВО":"§dУПРЕЖДЕНИЕ")+" §8• §f"+name+" §8• §7"+String.format("%.2f c",s.flightTicks/20D);
        int w=mc.fontRendererObj.getStringWidth(info)+12;int bx=cx-w/2,by=cy+25;
        DrawUtil.rounded(bx,by,bx+w,by+17,5,0xB012151D);DrawUtil.rect(bx,by,bx+w,by+2,ColorUtil.argb(color,210));
        mc.fontRendererObj.drawString(info,bx+6,by+5,0xFFFFFFFF,true);
    }
}
