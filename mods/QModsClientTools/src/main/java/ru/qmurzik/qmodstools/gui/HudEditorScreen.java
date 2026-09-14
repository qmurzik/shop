package ru.qmurzik.qmodstools.gui;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.feature.QModsV16;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.IOException;

public final class HudEditorScreen extends GuiScreen {
    private int dragging=-1,offX,offY;
    @Override public void drawScreen(int mx,int my,float partial){DrawUtil.rect(0,0,width,height,0xA0090B11);QModsV16.renderEditorPreview(width,height);for(int i=0;i<4;i++){int x=QModsV16.layoutX[i]*width/100,y=QModsV16.layoutY[i]*height/100;String s="§d"+(i+1)+" §f"+QModsV16.WIDGET_NAMES[i]+" §7"+QModsV16.layoutScale[i]+"%";fontRendererObj.drawString(s,Math.max(3,x-(i==2?70:0)),Math.max(3,y-11),0xFFFFFFFF,true);}drawCenteredString(fontRendererObj,"§f§lРЕДАКТОР HUD",width/2,8,0xFFFFFFFF);drawCenteredString(fontRendererObj,"§7ЛКМ — перемещение  •  ПКМ — масштаб  •  R — сброс  •  Esc — сохранить",width/2,height-14,0xFFFFFFFF);super.drawScreen(mx,my,partial);}
    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{int hit=QModsV16.widgetAt(mx,my,width,height);if(hit<0)return;if(button==0){dragging=hit;offX=mx-QModsV16.layoutX[hit]*width/100;offY=my-QModsV16.layoutY[hit]*height/100;}else if(button==1){int s=QModsV16.layoutScale[hit]+10;QModsV16.layoutScale[hit]=s>140?70:s;QModsV16.saveLayout();}}
    @Override protected void mouseClickMove(int mx,int my,int button,long time){if(dragging<0||button!=0)return;QModsV16.layoutX[dragging]=Math.max(0,Math.min(100,(mx-offX)*100/Math.max(1,width)));QModsV16.layoutY[dragging]=Math.max(2,Math.min(92,(my-offY)*100/Math.max(1,height)));}
    @Override protected void mouseReleased(int mx,int my,int state){if(dragging>=0){dragging=-1;QModsV16.saveLayout();}}
    @Override protected void keyTyped(char ch,int key)throws IOException{if(key==Keyboard.KEY_R){QModsV16.resetLayout();return;}if(key==Keyboard.KEY_ESCAPE){QModsV16.saveLayout();QModsV16.closeEditor();return;}super.keyTyped(ch,key);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
