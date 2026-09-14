package ru.qmurzik.qmodstools.gui;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.feature.QModsV17;
import ru.qmurzik.qmodstools.feature.QModsV19;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.IOException;

public final class CrosshairEditorScreen extends GuiScreen {
    private final GuiScreen parent;private int dragging;
    public CrosshairEditorScreen(GuiScreen parent){this.parent=parent;}
    @Override public void drawScreen(int mx,int my,float partial){
        int w=Math.min(520,width-24),h=Math.min(350,height-24),x=(width-w)/2,y=(height-h)/2;
        DrawUtil.gradient(0,0,width,height,0xF0070910,0xF0130E1B);DrawUtil.shadow(x,y,x+w,y+h,10);DrawUtil.rounded(x,y,x+w,y+h,9,0xF311141D);DrawUtil.gradient(x,y,x+w,y+4,QModsV17.accent(),QModsV17.accent2());
        fontRendererObj.drawString("§f§lРЕДАКТОР ПРИЦЕЛА",x+22,y+19,0xFFFFFFFF,true);fontRendererObj.drawString("§7Живой предпросмотр и управление инвентарём",x+22,y+34,0xFF9994A3,false);
        int px=x+w-112,py=y+48;DrawUtil.rounded(px,py,px+82,py+82,8,0xFF0B0E15);DrawUtil.rect(px+6,py+6,px+76,py+7,0x20FFFFFF);QModsV19.drawCross(px+41,py+41,QModsV19.gap,QModsV19.size,QModsV19.thickness,QModsV19.color,QModsV19.crosshairOutline,QModsV19.shape);fontRendererObj.drawString("§8PREVIEW",px+21,py+66,0xFF777281,false);
        button(x+22,y+61,210,30,"Форма: §f"+QModsV19.SHAPES[QModsV19.shape],mx,my,true);toggle(x+22,y+101,210,"Свой прицел",QModsV19.crosshairEnabled,mx,my);
        slider(x+22,y+151,210,"Размер",QModsV19.size,2,12,mx);slider(x+22,y+197,210,"Зазор",QModsV19.gap,0,10,mx);slider(x+22,y+243,210,"Толщина",QModsV19.thickness,1,4,mx);
        toggle(x+260,y+177,210,"Чёрный контур",QModsV19.crosshairOutline,mx,my);toggle(x+260,y+210,210,"Цвет при попадании",QModsV19.crosshairHitColor,mx,my);toggle(x+260,y+243,210,"Расширение в движении",QModsV19.crosshairDynamic,mx,my);toggle(x+260,y+276,210,"Боковая кнопка → слот 9",QModsV19.inventoryMouseSwap,mx,my);
        fontRendererObj.drawString("§7Цвет",x+260,y+103,0xFFAAA6B3,false);for(int i=0;i<QModsV19.COLORS.length;i++){int cx=x+260+(i%3)*31,cy=y+116+(i/3)*28;DrawUtil.rounded(cx,cy,cx+22,cy+22,5,0xFF000000|QModsV19.COLORS[i]);if(QModsV19.color==QModsV19.COLORS[i]){DrawUtil.rect(cx-2,cy-2,cx+24,cy,0xFFFFFFFF);DrawUtil.rect(cx-2,cy+22,cx+24,cy+24,0xFFFFFFFF);}}
        button(x+22,y+h-39,90,24,"§cСбросить",mx,my,false);button(x+w-112,y+h-39,90,24,"§fГотово",mx,my,true);fontRendererObj.drawString("§8Настройки сохраняются автоматически",x+126,y+h-31,0xFF797483,false);super.drawScreen(mx,my,partial);
    }
    private void button(int x,int y,int w,int h,String text,int mx,int my,boolean accent){boolean hover=inside(mx,my,x,y,x+w,y+h);DrawUtil.rounded(x,y,x+w,y+h,5,accent?(hover?QModsV17.accent():0xFF292E3B):(hover?0xFF3A2630:0xFF242936));fontRendererObj.drawString(text,x+10,y+(h-8)/2,0xFFFFFFFF,false);}
    private void toggle(int x,int y,int w,String name,boolean on,int mx,int my){boolean hover=inside(mx,my,x,y,x+w,y+26);DrawUtil.rounded(x,y,x+w,y+26,5,hover?0xFF252A36:0xFF1B1F29);fontRendererObj.drawString("§f"+name,x+10,y+9,0xFFFFFFFF,false);int sx=x+w-37;DrawUtil.rounded(sx,y+7,sx+27,y+20,7,on?QModsV17.accent():0xFF3A3F4D);DrawUtil.rounded(on?sx+15:sx+2,y+9,on?sx+25:sx+12,y+18,5,0xFFFFFFFF);}
    private void slider(int x,int y,int w,String name,int value,int min,int max,int mx){fontRendererObj.drawString("§7"+name+" §f"+value,x,y,0xFFFFFFFF,false);int sy=y+18;DrawUtil.rounded(x,sy,x+w,sy+5,3,0xFF303543);int fill=(value-min)*(w-4)/Math.max(1,max-min);DrawUtil.rounded(x,sy,x+4+fill,sy+5,3,QModsV17.accent());DrawUtil.rounded(x+fill,sy-3,x+fill+8,sy+8,5,0xFFFFFFFF);}
    @Override protected void mouseClicked(int mx,int my,int b)throws IOException{int w=Math.min(520,width-24),h=Math.min(350,height-24),x=(width-w)/2,y=(height-h)/2;if(inside(mx,my,x+22,y+61,x+232,y+91)){QModsV19.nextShape();return;}if(inside(mx,my,x+22,y+101,x+232,y+127)){QModsV19.crosshairEnabled=!QModsV19.crosshairEnabled;save();return;}if(toggleAt(mx,my,x+260,y+177)){QModsV19.crosshairOutline=!QModsV19.crosshairOutline;save();return;}if(toggleAt(mx,my,x+260,y+210)){QModsV19.crosshairHitColor=!QModsV19.crosshairHitColor;save();return;}if(toggleAt(mx,my,x+260,y+243)){QModsV19.crosshairDynamic=!QModsV19.crosshairDynamic;save();return;}if(toggleAt(mx,my,x+260,y+276)){QModsV19.inventoryMouseSwap=!QModsV19.inventoryMouseSwap;save();return;}for(int i=0;i<QModsV19.COLORS.length;i++){int cx=x+260+(i%3)*31,cy=y+116+(i/3)*28;if(inside(mx,my,cx,cy,cx+22,cy+22)){QModsV19.color=QModsV19.COLORS[i];save();return;}}if(inside(mx,my,x+22,y+145,x+232,y+177)){dragging=1;updateSlider(mx,x+22,210);return;}if(inside(mx,my,x+22,y+191,x+232,y+223)){dragging=2;updateSlider(mx,x+22,210);return;}if(inside(mx,my,x+22,y+237,x+232,y+269)){dragging=3;updateSlider(mx,x+22,210);return;}if(inside(mx,my,x+22,y+h-39,x+112,y+h-15)){QModsV19.reset();return;}if(inside(mx,my,x+w-112,y+h-39,x+w-22,y+h-15))close();}
    @Override protected void mouseClickMove(int mx,int my,int button,long time){if(dragging==0)return;int w=Math.min(520,width-24),x=(width-w)/2;updateSlider(mx,x+22,210);}
    @Override protected void mouseReleased(int mx,int my,int state){if(dragging!=0){dragging=0;save();}}
    private void updateSlider(int mx,int x,int w){float p=Math.max(0F,Math.min(1F,(mx-x)/(float)Math.max(1,w)));if(dragging==1)QModsV19.size=2+Math.round(p*10);else if(dragging==2)QModsV19.gap=Math.round(p*10);else if(dragging==3)QModsV19.thickness=1+Math.round(p*3);}
    @Override protected void keyTyped(char ch,int key)throws IOException{if(key==Keyboard.KEY_ESCAPE||key==Keyboard.KEY_RSHIFT){close();return;}super.keyTyped(ch,key);}
    private void close(){save();mc.displayGuiScreen(parent);}private void save(){QModsV19.save();}private boolean toggleAt(int mx,int my,int x,int y){return inside(mx,my,x,y,x+210,y+26);}private boolean inside(int x,int y,int a,int b,int c,int d){return x>=a&&x<=c&&y>=b&&y<=d;}
    @Override public boolean doesGuiPauseGame(){return false;}
}
