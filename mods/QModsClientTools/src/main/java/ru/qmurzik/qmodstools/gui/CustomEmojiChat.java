package ru.qmurzik.qmodstools.gui;

import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.feature.EmojiReplacer;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.IOException;

public final class CustomEmojiChat extends GuiChat {
    private static final String[] EMOJIS={":)","<3",":D",";)",":P",":(","^^",":O",":/",":|",":*","xD","B)",":3",":'(","o_O"};
    public CustomEmojiChat(String initial){super(initial==null?"":initial);}

    @Override public void sendChatMessage(String msg,boolean addToChat){
        if(QModsTools.config.emojiReplace&&QModsTools.config.emojiOutgoing&&!msg.startsWith("/"))msg=EmojiReplacer.outgoing(msg);
        super.sendChatMessage(msg,addToChat);
    }

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        super.drawScreen(mouseX,mouseY,partialTicks);
        int cols=8,x=5,y=height-62,w=28*cols+8,h=47;
        DrawUtil.shadow(x,y,x+w,y+h,4);DrawUtil.rounded(x,y,x+w,y+h,4,ColorUtil.argb(0x12151D,220));
        DrawUtil.rect(x,y,x+3,y+h,ColorUtil.argb(QModsTools.config.chatAccent,240));
        for(int i=0;i<EMOJIS.length;i++){int bx=x+6+(i%cols)*28,by=y+3+(i/cols)*21;boolean hover=mouseX>=bx&&mouseX<bx+24&&mouseY>=by&&mouseY<by+18;DrawUtil.rounded(bx,by,bx+24,by+18,4,hover?ColorUtil.argb(QModsTools.config.chatAccent,160):0xFF292D39);drawCenteredString(fontRendererObj,EMOJIS[i],bx+12,by+5,0xFFFFFFFF);}
        fontRendererObj.drawString("§7нажми, чтобы вставить",x+w+7,y+18,0xFFAAA6B3,false);
    }

    @Override protected void mouseClicked(int mouseX,int mouseY,int button)throws IOException{
        if(button==0){int cols=8,x=5,y=height-62;for(int i=0;i<EMOJIS.length;i++){int bx=x+6+(i%cols)*28,by=y+3+(i/cols)*21;if(mouseX>=bx&&mouseX<bx+24&&mouseY>=by&&mouseY<by+18){inputField.writeText(EMOJIS[i]+" ");inputField.setFocused(true);return;}}}
        super.mouseClicked(mouseX,mouseY,button);
    }
}
