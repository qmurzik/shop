package ru.qmurzik.qmodstools.gui;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import ru.qmurzik.qmodstools.feature.QModsV17;
import ru.qmurzik.qmodstools.feature.QModsV19;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.IOException;
import java.util.Locale;

/** Lightweight card grid inspired by PvP clients. No blur, shaders or texture allocations. */
public final class PvPClickGui extends GuiScreen {
    /** Maps each module card (category, index) to the legacy GuiSettings tab that holds its detailed options. */
    private static final int[][] SETTINGS_TAB = {
            {0, 0, 6, 6, 6},
            {4, 4, 4, 5, 5},
            {2, 1, 5, 6, 6},
            {1, 1, 1},
            {7, 7},
            {8},
            {6}
    };
    private final GuiScreen parent;
    private int category=-1,scrollRow;
    private boolean searchFocused,categoryOpen;
    private String search="";
    private int lastColumns=5,lastRows,lastVisibleRows=2;

    public PvPClickGui(GuiScreen p){parent=p;}

    @Override public void drawScreen(int mx,int my,float partial){
        int w=Math.min(820,width-20),h=Math.min(440,height-20),x=(width-w)/2,y=(height-h)/2;
        int wheel=Mouse.getDWheel();if(wheel!=0&&!categoryOpen){scrollRow+=wheel<0?1:-1;clampScroll();}
        DrawUtil.rect(0,0,width,height,0x72000000);DrawUtil.shadow(x,y,x+w,y+h,10);DrawUtil.rounded(x,y,x+w,y+h,8,0xF20A0D13);
        DrawUtil.rect(x+1,y+52,x+w-1,y+h-1,0xF30A0C12);

        int contentX=x+15,contentY=y+64,contentW=w-30,contentH=h-76;
        int cols=w>=730?5:w>=570?4:3,gap=8,cardW=(contentW-gap*(cols-1))/cols,cardH=132;
        int count=visibleCount(),rows=(count+cols-1)/cols,visibleRows=Math.max(1,(contentH+gap)/(cardH+gap));
        lastColumns=cols;lastRows=rows;lastVisibleRows=visibleRows;clampScroll();
        int visible=0;
        for(int c=0;c<QModsV17.CATEGORIES.length;c++)for(int i=0;i<QModsV17.moduleCount(c);i++)if(matches(c,i)){
            int row=visible/cols,col=visible%cols,screenRow=row-scrollRow;visible++;
            if(screenRow<0||screenRow>=visibleRows)continue;
            int bx=contentX+col*(cardW+gap),by=contentY+screenRow*(cardH+gap);
            card(bx,by,cardW,cardH,c,i,mx,my);
        }
        if(count==0){center("§7Ничего не найдено",x+w/2,y+h/2-5,0xFF9995A2);center("§8Попробуй другой запрос",x+w/2,y+h/2+11,0xFF77737F);}
        if(rows>visibleRows){int trackY=contentY,trackH=contentH-2;DrawUtil.rounded(x+w-7,trackY,x+w-4,trackY+trackH,2,0xFF20242D);int thumb=Math.max(24,trackH*visibleRows/rows),range=Math.max(1,trackH-thumb),max=Math.max(1,rows-visibleRows);int ty=trackY+range*scrollRow/max;DrawUtil.rounded(x+w-7,ty,x+w-4,ty+thumb,2,QModsV17.accent());}

        // Header is drawn last so scrolling cards never overlap it.
        DrawUtil.rounded(x,y,x+w,y+53,8,0xFA171B24);DrawUtil.rect(x,y+45,x+w,y+53,0xFA171B24);DrawUtil.gradient(x,y,x+w,y+3,QModsV17.accent(),QModsV17.accent2());
        fontRendererObj.drawString("§f§lQMODS",x+17,y+18,0xFFFFFFFF,true);fontRendererObj.drawString("§8PVP UI",x+18,y+32,0xFF777481,false);
        int searchX=x+112,searchW=Math.min(190,Math.max(126,w-610));DrawUtil.rounded(searchX,y+14,searchX+searchW,y+40,5,searchFocused?0xFF252B38:0xFF20242E);
        DrawUtil.rect(searchX+10,y+21,searchX+15,y+22,searchFocused?QModsV17.accent():0xFF737986);DrawUtil.rect(searchX+9,y+22,searchX+10,y+27,searchFocused?QModsV17.accent():0xFF737986);DrawUtil.rect(searchX+15,y+22,searchX+16,y+27,searchFocused?QModsV17.accent():0xFF737986);DrawUtil.rect(searchX+11,y+27,searchX+18,y+28,searchFocused?QModsV17.accent():0xFF737986);DrawUtil.rect(searchX+17,y+28,searchX+19,y+30,searchFocused?QModsV17.accent():0xFF737986);
        String shown=search.isEmpty()?(searchFocused?"§7Введи название...":"§8Поиск модулей"):"§f"+trim(search,20);fontRendererObj.drawString(shown,searchX+24,y+23,0xFFFFFFFF,false);if(searchFocused&&(System.currentTimeMillis()/450L)%2==0)fontRendererObj.drawString("§f_",searchX+25+Math.min(108,search.length()*6),y+23,0xFFFFFFFF,false);
        int hudX=searchX+searchW+8;headerButton(hudX,y+14,82,26,"HUD Editor",mx,my);headerButton(hudX+90,y+14,76,26,"Прицел",mx,my);
        int dropX=x+w-190;DrawUtil.rounded(dropX,y+14,dropX+146,y+40,5,0xFF20242E);fontRendererObj.drawString("§7"+categoryName(),dropX+11,y+23,0xFFFFFFFF,false);fontRendererObj.drawString(categoryOpen?"§b^":"§bv",dropX+132,y+22,0xFFFFFFFF,false);
        DrawUtil.rounded(x+w-35,y+14,x+w-12,y+39,5,inside(mx,my,x+w-35,y+14,x+w-12,y+39)?0xFF2A303C:0xFF20242E);fontRendererObj.drawString("§c§lx",x+w-27,y+22,0xFFFFFFFF,false);
        DrawUtil.rounded(x+8,y+16,x+11,y+37,2,QModsV17.accent());

        if(categoryOpen){
            int dh=(QModsV17.CATEGORIES.length+1)*22;DrawUtil.shadow(dropX,y+44,dropX+146,y+46+dh,5);DrawUtil.rounded(dropX,y+44,dropX+146,y+46+dh,5,0xFF171B24);
            for(int i=-1;i<QModsV17.CATEGORIES.length;i++){int iy=y+47+(i+1)*22;boolean on=i==category,hover=inside(mx,my,dropX+3,iy-2,dropX+143,iy+18);if(on||hover)DrawUtil.rounded(dropX+3,iy-2,dropX+143,iy+18,3,on?ColorUtil.argb(QModsV17.accent(),75):0xFF222731);fontRendererObj.drawString((on?"§f":"§7")+(i<0?"ВСЕ МОДУЛИ":QModsV17.CATEGORIES[i]),dropX+10,iy+4,0xFFFFFFFF,false);}
        }
        super.drawScreen(mx,my,partial);
    }

    private void card(int x,int y,int w,int h,int cat,int item,int mx,int my){
        boolean hover=inside(mx,my,x,y,x+w,y+h),on=QModsV17.moduleEnabled(cat,item);int bg=hover?0xFF141923:0xFF0E1219;
        DrawUtil.rounded(x,y,x+w,y+h,4,bg);DrawUtil.rect(x,y,x+w,y+1,hover?ColorUtil.argb(QModsV17.accent(),150):0xFF202631);DrawUtil.rect(x,y,x+1,y+h,0xFF202631);DrawUtil.rect(x+w-1,y,x+w,y+h,0xFF202631);DrawUtil.rect(x,y+h-1,x+w,y+h,0xFF202631);
        String name=trim(QModsV17.moduleName(cat,item),Math.max(13,(w-43)/6));fontRendererObj.drawString("§f"+name,x+8,y+10,0xFFFFFFFF,false);
        int sx=x+w-31;DrawUtil.rounded(sx,y+8,sx+23,y+20,7,on?QModsV17.accent():0xFF343A46);DrawUtil.rounded(on?sx+13:sx+2,y+10,on?sx+21:sx+10,y+18,5,on?0xFFFFFFFF:0xFF9095A0);
        drawIcon(x+w/2,y+68,cat,item,on);
        String desc=trim(QModsV17.moduleDesc(cat,item),Math.max(15,(w-15)/6));fontRendererObj.drawString("§8"+desc,x+8,y+h-18,0xFF7F8490,false);
        boolean gearHover=inside(mx,my,x+w-15,y+h-15,x+w-6,y+h-6);DrawUtil.rounded(x+w-15,y+h-15,x+w-6,y+h-6,5,gearHover?QModsV17.accent():(on?QModsV17.accent2():0xFF26303A));fontRendererObj.drawString("§f§li",x+w-12,y+h-14,0xFFFFFFFF,false);
    }

    /** Small vector-like icons made from rectangle calls: zero textures and allocations. */
    private void drawIcon(int cx,int cy,int cat,int variant,boolean on){
        int a=on?QModsV17.accent():0xFF6A4DA1,b=on?QModsV17.accent2():0xFF346D87;
        DrawUtil.rounded(cx-30,cy-28,cx+30,cy+28,24,ColorUtil.argb(a,on?24:13));
        if(cat==0){
            glowRect(cx-3,cy-3,cx+4,cy+4,a);glowRect(cx-25,cy-1,cx-8,cy+2,b);glowRect(cx+8,cy-1,cx+25,cy+2,b);glowRect(cx-1,cy-25,cx+2,cy-8,a);glowRect(cx-1,cy+8,cx+2,cy+25,a);
        }else if(cat==1){
            glowRect(cx-24,cy-2,cx+23,cy+13,a);glowRect(cx-23,cy-13,cx-5,cy-2,b);glowRect(cx-26,cy-7,cx-23,cy+22,b);glowRect(cx+20,cy+10,cx+24,cy+22,a);
        }else if(cat==2){
            outline(cx-25,cy-19,cx+25,cy+14,a);glowRect(cx-3,cy+14,cx+4,cy+22,b);glowRect(cx-13,cy+21,cx+14,cy+24,b);glowRect(cx-17,cy-10,cx+14,cy-7,ColorUtil.argb(b,210));
        }else if(cat==3){
            outline(cx-25,cy-19,cx+25,cy+14,a);glowRect(cx-17,cy-8,cx+17,cy-5,b);glowRect(cx-17,cy,cx+10,cy+3,b);glowRect(cx-14,cy+14,cx-22,cy+22,a);
        }else if(cat==4){
            DrawUtil.rounded(cx-11,cy-23,cx+11,cy-2,11,ColorUtil.argb(a,80));outline(cx-11,cy-23,cx+11,cy-2,a);DrawUtil.rounded(cx-23,cy+3,cx+23,cy+23,10,ColorUtil.argb(b,65));outline(cx-23,cy+3,cx+23,cy+23,b);
        }else if(cat==5){
            glowRect(cx-25,cy+13,cx-18,cy+24,b);glowRect(cx-12,cy+4,cx-5,cy+24,a);glowRect(cx+1,cy-6,cx+8,cy+24,b);glowRect(cx+14,cy-20,cx+21,cy+24,a);
        }else{
            glowRect(cx-24,cy-8,cx-11,cy+9,b);glowRect(cx-11,cy-15,cx-7,cy+16,a);glowRect(cx-7,cy-15,cx+4,cy-10,a);glowRect(cx-7,cy+11,cx+4,cy+16,a);glowRect(cx+10,cy-10,cx+13,cy+11,b);glowRect(cx+19,cy-17,cx+22,cy+18,a);
        }
        if((variant&1)==1){DrawUtil.rounded(cx+25,cy-24,cx+29,cy-20,2,ColorUtil.argb(b,210));DrawUtil.rounded(cx+29,cy-17,cx+32,cy-14,2,ColorUtil.argb(a,180));}
    }

    private void outline(int l,int t,int r,int b,int c){glowRect(l,t,r,t+3,c);glowRect(l,b-3,r,b,c);glowRect(l,t,l+3,b,c);glowRect(r-3,t,r,b,c);}
    private void glowRect(int l,int t,int r,int b,int c){DrawUtil.rect(l-1,t-1,r+1,b+1,ColorUtil.argb(c,35));DrawUtil.rect(l,t,r,b,c);}
    private void headerButton(int x,int y,int w,int h,String text,int mx,int my){boolean hover=inside(mx,my,x,y,x+w,y+h);DrawUtil.rounded(x,y,x+w,y+h,5,hover?0xFF2A303C:0xFF20242E);fontRendererObj.drawString("§7"+text,x+9,y+9,0xFFFFFFFF,false);}

    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{
        int w=Math.min(820,width-20),h=Math.min(440,height-20),x=(width-w)/2,y=(height-h)/2;
        int searchX=x+112,searchW=Math.min(190,Math.max(126,w-610)),hudX=searchX+searchW+8,dropX=x+w-190;
        if(inside(mx,my,x+w-35,y+14,x+w-12,y+39)){close();return;}
        if(categoryOpen){for(int i=-1;i<QModsV17.CATEGORIES.length;i++){int iy=y+47+(i+1)*22;if(inside(mx,my,dropX+3,iy-2,dropX+143,iy+18)){category=i;categoryOpen=false;scrollRow=0;return;}}categoryOpen=false;return;}
        if(inside(mx,my,dropX,y+14,dropX+146,y+40)){categoryOpen=!categoryOpen;searchFocused=false;return;}
        if(inside(mx,my,searchX,y+14,searchX+searchW,y+40)){searchFocused=true;categoryOpen=false;return;}searchFocused=false;
        if(inside(mx,my,hudX,y+14,hudX+82,y+40)){mc.displayGuiScreen(new HudEditorScreen());return;}
        if(inside(mx,my,hudX+90,y+14,hudX+166,y+40)){QModsV19.openEditor(this);return;}
        int contentX=x+15,contentY=y+64,contentW=w-30,gap=8,cardW=(contentW-gap*(lastColumns-1))/lastColumns,cardH=132,visible=0;
        for(int c=0;c<QModsV17.CATEGORIES.length;c++)for(int i=0;i<QModsV17.moduleCount(c);i++)if(matches(c,i)){
            int row=visible/lastColumns,col=visible%lastColumns,screenRow=row-scrollRow;visible++;
            if(screenRow<0||screenRow>=lastVisibleRows)continue;
            int bx=contentX+col*(cardW+gap),by=contentY+screenRow*(cardH+gap);
            if(inside(mx,my,bx+cardW-15,by+cardH-15,bx+cardW-6,by+cardH-6)){openSettings(c,i);return;}
            if(inside(mx,my,bx,by,bx+cardW,by+cardH)){QModsV17.toggle(c,i);return;}
        }
    }

    private void openSettings(int cat,int item){
        int tab=cat<SETTINGS_TAB.length&&item<SETTINGS_TAB[cat].length?SETTINGS_TAB[cat][item]:0;
        QModsV17.save();mc.displayGuiScreen(new GuiSettings(this,tab));
    }

    @Override protected void keyTyped(char ch,int key)throws IOException{
        if(searchFocused){if(key==14&&search.length()>0){search=search.substring(0,search.length()-1);scrollRow=0;return;}if(key==28){searchFocused=false;return;}if(!Character.isISOControl(ch)&&search.length()<28){search+=ch;scrollRow=0;return;}}
        if(key==Keyboard.KEY_ESCAPE||key==Keyboard.KEY_RSHIFT){close();return;}super.keyTyped(ch,key);
    }
    private int visibleCount(){int n=0;for(int c=0;c<QModsV17.CATEGORIES.length;c++)for(int i=0;i<QModsV17.moduleCount(c);i++)if(matches(c,i))n++;return n;}
    private boolean matches(int c,int i){if(category>=0&&category!=c)return false;if(search.isEmpty())return true;String q=search.toLowerCase(Locale.ROOT);return QModsV17.moduleName(c,i).toLowerCase(Locale.ROOT).contains(q)||QModsV17.moduleDesc(c,i).toLowerCase(Locale.ROOT).contains(q);}
    private void clampScroll(){scrollRow=Math.max(0,Math.min(scrollRow,Math.max(0,lastRows-lastVisibleRows)));}
    private String categoryName(){return category<0?"ВСЕ МОДУЛИ":QModsV17.CATEGORIES[category];}
    private void close(){QModsV17.save();mc.displayGuiScreen(parent);}
    private void center(String s,int x,int y,int c){fontRendererObj.drawString(s,x-visibleLength(s)*3,y,c,false);}
    private int visibleLength(String s){int n=0;for(int i=0;i<s.length();i++){if(s.charAt(i)=='§'&&i+1<s.length()){i++;continue;}n++;}return n;}
    private String trim(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-3))+"...";}
    private boolean inside(int x,int y,int a,int b,int c,int d){return x>=a&&x<=c&&y>=b&&y<=d;}
    @Override public boolean doesGuiPauseGame(){return false;}
}
