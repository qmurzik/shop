package ru.qmurzik.qmodstools.gui;

import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Keyboard;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.feature.ChatTranslator;
import ru.qmurzik.qmodstools.feature.EmojiReplacer;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.IOException;

public final class CustomEmojiChat extends GuiChat {
    private static final String[] EMOJIS={":)","<3",":D",";)",":P",":(","^^",":O",":/",":|",":*","xD","B)",":3",":'(","o_O"};
    private volatile boolean translating=false;
    public CustomEmojiChat(String initial){super(initial==null?"":initial);}

    @Override public void sendChatMessage(String msg,boolean addToChat){
        if(QModsTools.config.emojiReplace&&QModsTools.config.emojiOutgoing&&!msg.startsWith("/"))msg=EmojiReplacer.outgoing(msg);
        super.sendChatMessage(msg,addToChat);
    }

    @Override protected void keyTyped(char typedChar,int keyCode)throws IOException{
        if(translating){if(keyCode==Keyboard.KEY_ESCAPE)super.keyTyped(typedChar,keyCode);return;}
        boolean alt=Keyboard.isKeyDown(Keyboard.KEY_LMENU)||Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        boolean enter=keyCode==Keyboard.KEY_RETURN||keyCode==Keyboard.KEY_NUMPADENTER;
        if(alt&&enter&&QModsTools.config.translateOutgoing){
            final String text=inputField.getText();
            if(text!=null&&!text.trim().isEmpty()&&!text.startsWith("/")){
                translating=true;
                ChatTranslator.translate(text,"auto",QModsTools.config.translateTargetLang,new ChatTranslator.Callback(){
                    @Override public void onResult(String translated){
                        translating=false;
                        if(mc.currentScreen!=CustomEmojiChat.this)return;
                        sendChatMessage(translated!=null?translated:text,true);
                        mc.displayGuiScreen(null);
                    }
                });
                return;
            }
        }
        super.keyTyped(typedChar,keyCode);
    }

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        super.drawScreen(mouseX,mouseY,partialTicks);
        int cols=8,x=5,y=height-62,w=28*cols+8,h=47;
        DrawUtil.shadow(x,y,x+w,y+h,4);DrawUtil.rounded(x,y,x+w,y+h,4,ColorUtil.argb(0x12151D,220));
        DrawUtil.rect(x,y,x+3,y+h,ColorUtil.argb(QModsTools.config.chatAccent,240));
        for(int i=0;i<EMOJIS.length;i++){int bx=x+6+(i%cols)*28,by=y+3+(i/cols)*21;boolean hover=mouseX>=bx&&mouseX<bx+24&&mouseY>=by&&mouseY<by+18;DrawUtil.rounded(bx,by,bx+24,by+18,4,hover?ColorUtil.argb(QModsTools.config.chatAccent,160):0xFF292D39);drawCenteredString(fontRendererObj,EMOJIS[i],bx+12,by+5,0xFFFFFFFF);}
        fontRendererObj.drawString(translating?"§dПереводим...":"§7нажми, чтобы вставить  §8•  §7Alt+Enter — перевести и отправить",x+w+7,y+18,0xFFAAA6B3,false);
    }

    @Override protected void mouseClicked(int mouseX,int mouseY,int button)throws IOException{
        if(button==0){int cols=8,x=5,y=height-62;for(int i=0;i<EMOJIS.length;i++){int bx=x+6+(i%cols)*28,by=y+3+(i/cols)*21;if(mouseX>=bx&&mouseX<bx+24&&mouseY>=by&&mouseY<by+18){inputField.writeText(EMOJIS[i]+" ");inputField.setFocused(true);return;}}}
        super.mouseClicked(mouseX,mouseY,button);
    }
}
