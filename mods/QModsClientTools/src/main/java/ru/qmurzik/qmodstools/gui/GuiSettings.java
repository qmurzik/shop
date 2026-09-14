package ru.qmurzik.qmodstools.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.qmurzik.qmodstools.Config;
import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GuiSettings extends GuiScreen {
    private final GuiScreen parent;
    private final String[] tabs={"Лук и упреждение","Красивый чат","Scoreboard","Текстовые бинды","MineBlaze BW","BW Автоматика","Бой и HUD","Внешность","Производительность"};
    private final String[] tabIcons={"➶","✦","★","•","♦","◉","☠","☺","○"};
    private final int[] palette={0xB060FF,0xFF5C9A,0x45D7FF,0x56E39F,0xFFB84D,0xFFFFFF};
    private static final int[] CONTENT_BOTTOM={325,455,340,0,360,425,300,285,280};
    private static final int VP_TOP=70;
    private int tab=0,bindScroll=0,captureSlot=-1,captureSpecial=0;
    private float barY=75,barTarget=75,scroll=0;private long lastFrameMs=System.currentTimeMillis();
    private final List<GuiTextField> bindFields=new ArrayList<GuiTextField>();
    private GuiTextField ggTextField,baseAlertPrefixField,baseAlertMessageField;

    public GuiSettings(GuiScreen parent){this(parent,0);}
    public GuiSettings(GuiScreen parent,int startTab){this.parent=parent;this.tab=Math.max(0,Math.min(8,startTab));}

    @Override public void initGui(){
        Keyboard.enableRepeatEvents(true);
        int fw=Math.max(160,width-320);
        ggTextField=new GuiTextField(200,fontRendererObj,158,0,fw,18);ggTextField.setMaxStringLength(64);ggTextField.setText(QModsTools.config.autoGgText);
        baseAlertPrefixField=new GuiTextField(201,fontRendererObj,158,0,Math.max(80,fw/3),18);baseAlertPrefixField.setMaxStringLength(16);baseAlertPrefixField.setText(QModsTools.config.baseAlertPrefix);
        baseAlertMessageField=new GuiTextField(202,fontRendererObj,158,0,fw,18);baseAlertMessageField.setMaxStringLength(96);baseAlertMessageField.setText(QModsTools.config.baseAlertMessage);
        rebuildFields();
    }
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);syncFields();syncAutomationFields();QModsTools.config.save();}
    private void syncAutomationFields(){QModsTools.config.autoGgText=ggTextField.getText();QModsTools.config.baseAlertPrefix=baseAlertPrefixField.getText();QModsTools.config.baseAlertMessage=baseAlertMessageField.getText();}

    private void rebuildFields(){bindFields.clear();if(tab!=3)return;int cx=132,contentW=width-cx-20;for(int i=0;i<8;i++){GuiTextField f=new GuiTextField(i,fontRendererObj,cx+18,82+i*34-bindScroll*34,Math.max(100,contentW-116),20);f.setMaxStringLength(160);f.setText(QModsTools.config.bindText[i]==null?"":QModsTools.config.bindText[i]);bindFields.add(f);}}
    private void syncFields(){for(int i=0;i<bindFields.size();i++)QModsTools.config.bindText[i]=bindFields.get(i).getText();}

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks){
        long now=System.currentTimeMillis();float dt=Math.min(60,now-lastFrameMs);lastFrameMs=now;
        drawDefaultBackground();DrawUtil.gradient(0,0,width,height,0xEC0B0C12,0xF0161120);
        DrawUtil.shadow(12,12,width-12,height-12,7);DrawUtil.rounded(12,12,width-12,height-12,7,0xEC12151E);
        DrawUtil.gradient(12,12,width-12,60,0xFF282039,0xFF151824);
        int pulse=(int)(24*Math.sin(now/450.0));DrawUtil.rect(12,58,width-12,60,ColorUtil.argb(ColorUtil.brighten(accent(),Math.max(0,pulse)),225));
        fontRendererObj.drawString("§lQMODS CLIENT TOOLS",28,27,0xFFFFFFFF,true);fontRendererObj.drawString("§7Forge 1.8.9  •  лёгкий клиентский HUD",28,43,0xFFB8B3C7,false);
        barTarget=75+tab*31;barY+=(barTarget-barY)*Math.min(1F,dt/110F);
        DrawUtil.rounded(18,(int)barY-4,21,(int)barY+16,2,ColorUtil.argb(accent(),255));
        int sy=75;for(int i=0;i<tabs.length;i++){boolean active=i==tab;boolean hover=isHover(mouseX,mouseY,18,sy-5,124,sy+17);if(active)DrawUtil.rounded(22,sy-5,121,sy+17,4,ColorUtil.argb(accent(),72));else if(hover)DrawUtil.rounded(22,sy-5,121,sy+17,4,0x2AFFFFFF);fontRendererObj.drawString((active?"§f":hover?"§7":"§8")+tabIcons[i]+" §r"+(active?"§f":"§7")+tabs[i],28,sy+2,active?0xFFFFFFFF:0xFFAAA6B3,false);sy+=31;}
        DrawUtil.rect(128,70,129,height-26,0x30FFFFFF);
        int vpBottom=height-26;int cmy=cy(mouseY);
        if(tab!=3){
            ScaledResolution sr=new ScaledResolution(mc);int factor=sr.getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(148*factor,mc.displayHeight-vpBottom*factor,(width-148-24)*factor,(vpBottom-VP_TOP)*factor);
            GlStateManager.pushMatrix();GlStateManager.translate(0,-scroll,0);
        }
        switch(tab){case 0:drawArcher(mouseX,cmy);break;case 1:drawChat(mouseX,cmy);break;case 2:drawScoreboard(mouseX,cmy);break;case 3:drawBinds(mouseX,mouseY);break;case 4:drawMineBlaze(mouseX,cmy);break;case 5:drawAutomation(mouseX,cmy);break;case 6:drawCombat(mouseX,cmy);break;case 7:drawCosmetics(mouseX,cmy);break;default:drawPerformance(mouseX,cmy);}
        if(tab!=3){GlStateManager.popMatrix();GL11.glDisable(GL11.GL_SCISSOR_TEST);drawScrollbar(vpBottom);}
        fontRendererObj.drawString("§f/qtools §7— резервное открытие меню",28,height-31,0xFF9A95A5,false);
        int mk=width-151,my=height-39;DrawUtil.rounded(mk,my,width-24,my+22,4,ColorUtil.argb(captureSpecial==1?accent():0x292D39,230));drawCenteredString(fontRendererObj,captureSpecial==1?"НАЖМИ КЛАВИШУ":"Меню: "+keyName(QModsTools.settingsKey),mk+(width-24-mk)/2,my+7,0xFFFFFFFF);
        super.drawScreen(mouseX,mouseY,partialTicks);
    }

    private int cy(int my){return tab==3?my:(int)(my+scroll);}
    private int maxScroll(){int bottom=CONTENT_BOTTOM[tab];int visible=(height-26)-VP_TOP;return Math.max(0,bottom-visible);}
    private void drawScrollbar(int vpBottom){int max=maxScroll();if(max<=0)return;int trackH=vpBottom-VP_TOP;float visibleFrac=trackH/(float)(trackH+max);int thumbH=Math.max(20,(int)(trackH*visibleFrac));int thumbY=VP_TOP+(int)((trackH-thumbH)*(scroll/max));DrawUtil.rounded(width-19,VP_TOP,width-15,vpBottom,2,0x20FFFFFF);DrawUtil.rounded(width-19,thumbY,width-15,thumbY+thumbH,2,ColorUtil.argb(accent(),200));}

    private void drawArcher(int mx,int my){title("Точный выстрел","Маркер упреждения + плавное доведение при натяжении");int y=102;toggle("Траектория полёта","Реальная физика и место столкновения",QModsTools.config.trajectory,y,mx,my);y+=36;toggle("Маркер упреждения","Показывать заметный указатель около прицела",QModsTools.config.aimGuide,y,mx,my);y+=36;toggle("Мягкое доведение","Плавно вести прицел к рассчитанной точке",QModsTools.config.aimAssist,y,mx,my);y+=36;toggle("Не целиться в союзников","Важно для командного BedWars",QModsTools.config.ignoreTeammates,y,mx,my);y+=39;slider("Сила доведения",QModsTools.config.aimStrength,.05,.65,y,"%.2f");y+=34;slider("Дальность цели",QModsTools.config.aimRange,12,128,y,"%.0f блоков");}
    private void drawChat(int mx,int my){title("Красивый чат","Темы, плавное появление, время и текстовые эмодзи");int y=102;toggle("Свой рендер чата","Заменяет стандартный фон и строки",QModsTools.config.customChat,y,mx,my);y+=36;cycle("Тема",themeName(QModsTools.config.chatTheme),y);y+=34;slider("Прозрачность",QModsTools.config.chatAlpha,0,255,y,"%.0f");y+=34;toggle("Плавное появление","Новые сообщения мягко выезжают",QModsTools.config.chatAnimations,y,mx,my);y+=36;toggle("Время сообщений","Добавить метку HH:mm",QModsTools.config.chatTimestamps,y,mx,my);y+=36;toggle("Эмодзи из символов","Заменять :) <3 :D ;) :P :( ^^",QModsTools.config.emojiReplace,y,mx,my);y+=39;colorRow("Акцент",QModsTools.config.chatAccent,y);y+=32;
        int mid=(148+width-34)/2;smallToggle("Подсветка упоминаний",QModsTools.config.chatMentionHighlight,148,y,mid-5,mx,my);smallToggle("Автоперевод входящих → RU",QModsTools.config.translateIncoming,mid+5,y,width-34,mx,my);y+=36;
        toggle("Перевод исходящих (Ctrl+Enter)","Напиши сообщение и вместо Enter нажми Ctrl+Enter — переведёт и отправит",QModsTools.config.translateOutgoing,y,mx,my);y+=36;
        cycle("Язык перевода",langName(QModsTools.config.translateTargetLang),y);}
    private void drawScoreboard(int mx,int my){title("Оформление scoreboard","Стекло, неон, карточки или минимализм");int y=102;toggle("Свой scoreboard","Красивый блок вместо стандартного",QModsTools.config.customScoreboard,y,mx,my);y+=36;cycle("Тема",themeName(QModsTools.config.scoreboardTheme),y);y+=34;slider("Прозрачность",QModsTools.config.scoreboardAlpha,0,255,y,"%.0f");y+=34;slider("Масштаб",QModsTools.config.scoreboardScale,.65,1.5,y,"%.2fx");y+=34;cycle("Сторона",QModsTools.config.scoreboardSide==1?"Справа":"Слева",y);y+=34;toggle("Очки справа","Показывать красные числа",QModsTools.config.scoreboardNumbers,y,mx,my);y+=38;colorRow("Акцент",QModsTools.config.scoreboardAccent,y);}
    private void drawPerformance(int mx,int my){title("Производительность","Настройки для Intel HD 4000 и слабого процессора");int y=105;toggle("Лёгкий режим","Меньше сегментов и эффектов без потери физики",QModsTools.config.lowPower,y,mx,my);y+=42;slider("Частота расчёта",QModsTools.config.calculationInterval,1,6,y,"раз в %.0f тик(а)");y+=40;slider("Сегменты траектории",QModsTools.config.maxSteps,40,320,y,"%.0f");y+=45;info("Рекомендуется для твоего ноутбука: лёгкий режим, расчёт раз в 2 тика, 140–180 сегментов.",y);}
    private void drawMineBlaze(int mx,int my){title("MineBlaze BedWars","HUD, магазин и безопасный быстрый перезаход");int mid=(148+width-34)/2;smallToggle("BedWars HUD",QModsTools.config.bedWarsHud,148,102,mid-5,mx,my);smallToggle("Помощник магазина",QModsTools.config.shopHelper,mid+5,102,width-34,mx,my);smallToggle("Тревога о кровати",QModsTools.config.bedAlert,148,138,mid-5,mx,my);smallToggle("Вкладки 1–9",QModsTools.config.quickShopKeys,mid+5,138,width-34,mx,my);smallToggle("Чистый scoreboard",QModsTools.config.hideMineBlazeOrderNumbers,148,174,mid-5,mx,my);smallToggle("Автоперезаход",QModsTools.config.autoVoidRejoin,mid+5,174,width-34,mx,my);smallToggle("Баннер разрушения кровати",QModsTools.config.bedBreakBanner,148,210,mid-5,mx,my);smallToggle("Баннер финального убийства",QModsTools.config.finalKillBanner,mid+5,210,width-34,mx,my);slider("Высота бездны",QModsTools.config.voidRejoinY,-40,80,254,"Y %.0f");slider("Задержка /rejoin",QModsTools.config.rejoinDelayMs,500,3000,290,"%.0f мс");int x=158,y=329;DrawUtil.rounded(x,y,x+190,y+24,4,0xFF292D39);fontRendererObj.drawString("§fРучной перезаход",x+9,y+8,0xFFFFFFFF,false);int bx=x+198;DrawUtil.rounded(bx,y,bx+100,y+24,4,ColorUtil.argb(captureSpecial==2?accent():0x343845,230));drawCenteredString(fontRendererObj,captureSpecial==2?"НАЖМИ...":keyName(QModsTools.rejoinKey),bx+50,y+8,0xFFFFFFFF);}
    private void drawAutomation(int mx,int my){title("BW Автоматика","Килл-фид, авто-GG, таймеры генераторов и оповещение о вторжении");int mid=(148+width-34)/2;smallToggle("Килл-фид",QModsTools.config.killFeed,148,102,mid-5,mx,my);smallToggle("Авто-GG по окончании игры",QModsTools.config.autoGg,mid+5,102,width-34,mx,my);smallToggle("Таймеры генераторов",QModsTools.config.genTimers,148,138,mid-5,mx,my);smallToggle("Подсказка по ресурсам",QModsTools.config.shopReminder,mid+5,138,width-34,mx,my);toggle("Оповещение о вторжении","Срабатывает только когда матч реально идёт (не в лобби) — шлёт сообщение, если враг рядом с твоей базой",QModsTools.config.baseAlert,174,mx,my);slider("Интервал алмазного генератора",QModsTools.config.diamondGenInterval,5,120,213,"%.0fс");slider("Интервал изумрудного генератора",QModsTools.config.emeraldGenInterval,5,180,247,"%.0fс");slider("Радиус оповещения о базе",QModsTools.config.baseAlertRadius,5,60,281,"%.0f блоков");
        int fw=Math.max(160,width-320);
        textRow("Текст авто-GG",ggTextField,315,fw);textRow("Префикс командного чата (напр. !)",baseAlertPrefixField,351,Math.max(80,fw/3));textRow("Текст оповещения о базе",baseAlertMessageField,387,fw);}
    private void textRow(String label,GuiTextField field,int y,int w){fontRendererObj.drawString("§f"+label,158,y,0xFFFFFFFF,false);field.yPosition=y+13;DrawUtil.rounded(154,y+11,158+w+4,y+31,4,0x501F2330);field.drawTextBox();}
    private void drawCombat(int mx,int my){title("Бой и HUD","Пинг, счётчик кликов, координаты и эффекты попаданий в PvP");int mid=(148+width-34)/2;smallToggle("Пинг на экране",QModsTools.config.pingHud,148,102,mid-5,mx,my);smallToggle("Счётчик CPS",QModsTools.config.cpsHud,mid+5,102,width-34,mx,my);smallToggle("Координаты + FPS",QModsTools.config.coordsHud,148,138,mid-5,mx,my);smallToggle("Частицы удара",QModsTools.config.hitParticles,mid+5,138,width-34,mx,my);smallToggle("Маркер попадания",QModsTools.config.hitMarker,148,174,mid-5,mx,my);smallToggle("Индикатор урона",QModsTools.config.damageIndicator,mid+5,174,width-34,mx,my);smallToggle("Звук крита",QModsTools.config.critEffects,148,210,mid-5,mx,my);info("Маркер попадания — вспышка на прицеле при своём ударе, рядом всплывает нанесённый урон. Индикатор урона — метка вокруг прицела, показывающая, с какой стороны прилетело.",250);}
    private void drawCosmetics(int mx,int my){title("Локальная внешность","Скин и плащ видны только на твоём клиенте");int y=105;toggle("Скин Киры","Чёрно-фиолетовый образ и фиолетовые глаза",QModsTools.config.localKiraSkin,y,mx,my);y+=42;toggle("Плащ QMods","Чёрный плащ с фиолетово-голубым знаком",QModsTools.config.localQModsCape,y,mx,my);y+=48;cycle("Цветовой вариант",variantName(QModsTools.config.cosmeticVariant),y);y+=38;info("Посмотреть можно от третьего лица клавишей F5. Другие игроки продолжат видеть твой обычный серверный скин.",y);}
    private void drawBinds(int mx,int my){title("Текстовые бинды","Нажал клавишу — сообщение или команда отправились в чат");int top=78,bottom=height-45;for(int i=0;i<bindFields.size();i++){int y=82+i*34-bindScroll*34;if(y<top||y+22>bottom)continue;GuiTextField f=bindFields.get(i);f.yPosition=y;DrawUtil.rounded(140,y-3,width-34,y+24,4,0x501F2330);fontRendererObj.drawString("§d"+(i+1),136,y+6,0xFFFFFFFF,true);f.drawTextBox();int bx=width-126;DrawUtil.rounded(bx,y,bx+82,y+20,4,ColorUtil.argb(captureSlot==i?accent():0x2A2E3B,captureSlot==i?190:220));String key=captureSlot==i?"НАЖМИ...":QModsTools.binds.keyName(i);drawCenteredString(fontRendererObj,key,bx+41,y+6,0xFFFFFFFF);}
        fontRendererObj.drawString("§7Колесо мыши — остальные слоты • пример: §f/i love qmods §7или §f/hub",146,height-40,0xFFAAA6B3,false);
    }

    private void title(String a,String b){fontRendererObj.drawString("§f§l"+a,148,76,0xFFFFFFFF,true);fontRendererObj.drawString("§7"+b,148,89,0xFFAAA6B3,false);}
    private void toggle(String name,String desc,boolean on,int y,int mx,int my){int x=148;DrawUtil.rounded(x,y,x+Math.max(260,width-x-34),y+31,5,isHover(mx,my,x,y,width-34,y+31)?0x45272B38:0x30212731);fontRendererObj.drawString("§f"+name,x+10,y+6,0xFFFFFFFF,false);fontRendererObj.drawString("§8"+desc,x+10,y+18,0xFF878291,false);int bx=width-76;DrawUtil.rounded(bx,y+7,bx+34,y+23,8,on?ColorUtil.argb(accent(),230):0xFF3A3E49);DrawUtil.rounded(on?bx+20:bx+2,y+9,on?bx+32:bx+14,y+21,6,0xFFFFFFFF);}
    private void smallToggle(String name,boolean on,int x1,int y,int x2,int mx,int my){DrawUtil.rounded(x1,y,x2,y+29,5,isHover(mx,my,x1,y,x2,y+29)?0x45272B38:0x30212731);fontRendererObj.drawString("§f"+name,x1+9,y+10,0xFFFFFFFF,false);int bx=x2-39;DrawUtil.rounded(bx,y+7,bx+31,y+22,8,on?ColorUtil.argb(accent(),230):0xFF3A3E49);DrawUtil.rounded(on?bx+18:bx+2,y+9,on?bx+29:bx+13,y+20,6,0xFFFFFFFF);}
    private void cycle(String name,String value,int y){fontRendererObj.drawString("§f"+name,158,y+8,0xFFFFFFFF,false);int x=width-170;DrawUtil.rounded(x,y,x+126,y+24,4,0xFF292D39);drawCenteredString(fontRendererObj,"§d‹  §f"+value+"  §d›",x+63,y+8,0xFFFFFFFF);}
    private void slider(String name,double value,double min,double max,int y,String fmt){fontRendererObj.drawString("§f"+name,158,y+3,0xFFFFFFFF,false);int x=158,w=Math.max(100,width-x-155),yy=y+18;DrawUtil.rounded(x,yy,x+w,yy+5,2,0xFF343845);int fill=(int)(w*(value-min)/(max-min));DrawUtil.rounded(x,yy,x+fill,yy+5,2,ColorUtil.argb(accent(),230));DrawUtil.rounded(x+fill-3,yy-2,x+fill+4,yy+8,4,0xFFFFFFFF);fontRendererObj.drawString(String.format(fmt,value),x+w+12,y+13,0xFFCBC7D2,false);}
    private void colorRow(String name,int selected,int y){fontRendererObj.drawString("§f"+name,158,y+5,0xFFFFFFFF,false);int x=246;for(int c:palette){DrawUtil.rounded(x,y,x+22,y+22,5,0xFF000000|c);if(c==selected){DrawUtil.rect(x+4,y+19,x+18,y+21,0xFFFFFFFF);}x+=30;}}
    private void info(String text,int y){DrawUtil.rounded(148,y,width-36,y+44,5,ColorUtil.argb(accent(),35));fontRendererObj.drawSplitString("§7"+text,160,y+10,width-215,0xFFCBC7D2);}

    @Override protected void mouseClicked(int mx,int my,int button)throws IOException{super.mouseClicked(mx,my,button);if(button!=0)return;if(isHover(mx,my,width-151,height-39,width-24,height-17)){captureSpecial=1;return;}int sy=70;for(int i=0;i<tabs.length;i++){if(isHover(mx,my,18,sy,124,sy+25)){syncFields();syncAutomationFields();tab=i;captureSlot=-1;captureSpecial=0;scroll=0;rebuildFields();return;}sy+=31;}if(tab==3){for(int i=0;i<bindFields.size();i++){GuiTextField f=bindFields.get(i);f.mouseClicked(mx,my,button);int y=f.yPosition;if(isHover(mx,my,width-126,y,width-44,y+20)){captureSlot=i;return;}}return;}
        if(my<VP_TOP||my>height-26)return;int cmy=cy(my);
        if(tab==5){ggTextField.mouseClicked(mx,cmy,button);baseAlertPrefixField.mouseClicked(mx,cmy,button);baseAlertMessageField.mouseClicked(mx,cmy,button);}
        int rejoinX=356;if(tab==4&&isHover(mx,cmy,rejoinX,329,rejoinX+100,353)){captureSpecial=2;return;}handleContentClick(mx,cmy);}

    private void handleContentClick(int mx,int my){Config c=QModsTools.config;if(tab==0){if(hit(mx,my,102))c.trajectory=!c.trajectory;else if(hit(mx,my,138))c.aimGuide=!c.aimGuide;else if(hit(mx,my,174))c.aimAssist=!c.aimAssist;else if(hit(mx,my,210))c.ignoreTeammates=!c.ignoreTeammates;else if(sliderHit(mx,my,249))c.aimStrength=(float)slide(mx,.05,.65);else if(sliderHit(mx,my,283))c.aimRange=slide(mx,12,128);}
        else if(tab==1){if(hit(mx,my,102))c.customChat=!c.customChat;else if(isHover(mx,my,width-170,138,width-44,162))c.chatTheme=(c.chatTheme+1)%6;else if(sliderHit(mx,my,172))c.chatAlpha=(int)slide(mx,0,255);else if(hit(mx,my,206))c.chatAnimations=!c.chatAnimations;else if(hit(mx,my,242))c.chatTimestamps=!c.chatTimestamps;else if(hit(mx,my,278))c.emojiReplace=!c.emojiReplace;else if(my>=317&&my<=341)c.chatAccent=pickColor(mx);else{int mid=(148+width-34)/2;if(isHover(mx,my,148,349,mid-5,378))c.chatMentionHighlight=!c.chatMentionHighlight;else if(isHover(mx,my,mid+5,349,width-34,378))c.translateIncoming=!c.translateIncoming;else if(hit(mx,my,385))c.translateOutgoing=!c.translateOutgoing;else if(isHover(mx,my,width-170,421,width-44,445))c.translateTargetLang=nextLang(c.translateTargetLang);}}
        else if(tab==2){if(hit(mx,my,102))c.customScoreboard=!c.customScoreboard;else if(isHover(mx,my,width-170,138,width-44,162))c.scoreboardTheme=(c.scoreboardTheme+1)%6;else if(sliderHit(mx,my,172))c.scoreboardAlpha=(int)slide(mx,0,255);else if(sliderHit(mx,my,206))c.scoreboardScale=(float)slide(mx,.65,1.5);else if(isHover(mx,my,width-170,240,width-44,264))c.scoreboardSide=1-c.scoreboardSide;else if(hit(mx,my,274))c.scoreboardNumbers=!c.scoreboardNumbers;else if(my>=312&&my<=336)c.scoreboardAccent=pickColor(mx);}
        else if(tab==4){int mid=(148+width-34)/2;if(isHover(mx,my,148,102,mid-5,131))c.bedWarsHud=!c.bedWarsHud;else if(isHover(mx,my,mid+5,102,width-34,131))c.shopHelper=!c.shopHelper;else if(isHover(mx,my,148,138,mid-5,167))c.bedAlert=!c.bedAlert;else if(isHover(mx,my,mid+5,138,width-34,167))c.quickShopKeys=!c.quickShopKeys;else if(isHover(mx,my,148,174,mid-5,203))c.hideMineBlazeOrderNumbers=!c.hideMineBlazeOrderNumbers;else if(isHover(mx,my,mid+5,174,width-34,203))c.autoVoidRejoin=!c.autoVoidRejoin;else if(isHover(mx,my,148,210,mid-5,239))c.bedBreakBanner=!c.bedBreakBanner;else if(isHover(mx,my,mid+5,210,width-34,239))c.finalKillBanner=!c.finalKillBanner;else if(sliderHit(mx,my,254))c.voidRejoinY=(int)Math.round(slide(mx,-40,80));else if(sliderHit(mx,my,290))c.rejoinDelayMs=(int)Math.round(slide(mx,500,3000));}
        else if(tab==5){int mid=(148+width-34)/2;if(isHover(mx,my,148,102,mid-5,131))c.killFeed=!c.killFeed;else if(isHover(mx,my,mid+5,102,width-34,131))c.autoGg=!c.autoGg;else if(isHover(mx,my,148,138,mid-5,167))c.genTimers=!c.genTimers;else if(isHover(mx,my,mid+5,138,width-34,167))c.shopReminder=!c.shopReminder;else if(hit(mx,my,174))c.baseAlert=!c.baseAlert;else if(sliderHit(mx,my,213))c.diamondGenInterval=(int)Math.round(slide(mx,5,120));else if(sliderHit(mx,my,247))c.emeraldGenInterval=(int)Math.round(slide(mx,5,180));else if(sliderHit(mx,my,281))c.baseAlertRadius=(int)Math.round(slide(mx,5,60));}
        else if(tab==6){int mid=(148+width-34)/2;if(isHover(mx,my,148,102,mid-5,131))c.pingHud=!c.pingHud;else if(isHover(mx,my,mid+5,102,width-34,131))c.cpsHud=!c.cpsHud;else if(isHover(mx,my,148,138,mid-5,167))c.coordsHud=!c.coordsHud;else if(isHover(mx,my,mid+5,138,width-34,167))c.hitParticles=!c.hitParticles;else if(isHover(mx,my,148,174,mid-5,203))c.hitMarker=!c.hitMarker;else if(isHover(mx,my,mid+5,174,width-34,203))c.damageIndicator=!c.damageIndicator;else if(isHover(mx,my,148,210,mid-5,239))c.critEffects=!c.critEffects;}
        else if(tab==7){if(hit(mx,my,105))c.localKiraSkin=!c.localKiraSkin;else if(hit(mx,my,147))c.localQModsCape=!c.localQModsCape;else if(isHover(mx,my,width-170,195,width-44,219))c.cosmeticVariant=(c.cosmeticVariant+1)%3;}
        else if(tab==8){if(hit(mx,my,105))c.lowPower=!c.lowPower;else if(sliderHit(mx,my,147))c.calculationInterval=(int)Math.round(slide(mx,1,6));else if(sliderHit(mx,my,187))c.maxSteps=(int)Math.round(slide(mx,40,320));}c.save();}

    @Override protected void mouseClickMove(int mx,int my,int button,long time){if(button!=0)return;if(my<VP_TOP||my>height-26)return;my=cy(my);Config c=QModsTools.config;if(tab==0){if(sliderHit(mx,my,249))c.aimStrength=(float)slide(mx,.05,.65);else if(sliderHit(mx,my,283))c.aimRange=slide(mx,12,128);}else if(tab==1&&sliderHit(mx,my,172))c.chatAlpha=(int)slide(mx,0,255);else if(tab==2){if(sliderHit(mx,my,172))c.scoreboardAlpha=(int)slide(mx,0,255);else if(sliderHit(mx,my,206))c.scoreboardScale=(float)slide(mx,.65,1.5);}else if(tab==4){if(sliderHit(mx,my,254))c.voidRejoinY=(int)Math.round(slide(mx,-40,80));else if(sliderHit(mx,my,290))c.rejoinDelayMs=(int)Math.round(slide(mx,500,3000));}else if(tab==5){if(sliderHit(mx,my,213))c.diamondGenInterval=(int)Math.round(slide(mx,5,120));else if(sliderHit(mx,my,247))c.emeraldGenInterval=(int)Math.round(slide(mx,5,180));else if(sliderHit(mx,my,281))c.baseAlertRadius=(int)Math.round(slide(mx,5,60));}else if(tab==8){if(sliderHit(mx,my,147))c.calculationInterval=(int)Math.round(slide(mx,1,6));else if(sliderHit(mx,my,187))c.maxSteps=(int)Math.round(slide(mx,40,320));}}
    @Override protected void keyTyped(char ch,int key)throws IOException{if(captureSpecial>0){if(key==Keyboard.KEY_ESCAPE){captureSpecial=0;return;}int code=key==Keyboard.KEY_BACK||key==Keyboard.KEY_DELETE?0:key;if(captureSpecial==1)QModsTools.settingsKey.setKeyCode(code);else QModsTools.rejoinKey.setKeyCode(code);net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();mc.gameSettings.saveOptions();captureSpecial=0;return;}if(captureSlot>=0){if(key==Keyboard.KEY_ESCAPE){captureSlot=-1;return;}QModsTools.binds.setKey(captureSlot,key==Keyboard.KEY_BACK||key==Keyboard.KEY_DELETE?0:key);captureSlot=-1;QModsTools.config.save();return;}if(tab==3){for(GuiTextField f:bindFields)f.textboxKeyTyped(ch,key);}if(tab==5){ggTextField.textboxKeyTyped(ch,key);baseAlertPrefixField.textboxKeyTyped(ch,key);baseAlertMessageField.textboxKeyTyped(ch,key);}if(key==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(parent);return;}super.keyTyped(ch,key);}
    @Override public void handleMouseInput()throws IOException{super.handleMouseInput();int d=Mouse.getEventDWheel();if(d==0)return;if(tab==3){syncFields();bindScroll=Math.max(0,Math.min(3,bindScroll+(d<0?1:-1)));rebuildFields();}else{scroll=MathHelper.clamp_float(scroll+(d<0?20F:-20F),0,maxScroll());}}

    private boolean hit(int mx,int my,int y){return isHover(mx,my,148,y,width-34,y+31);}
    private boolean sliderHit(int mx,int my,int y){return isHover(mx,my,150,y+8,width-38,y+30);}
    private double slide(int mx,double min,double max){int x=158,w=Math.max(100,width-x-155);double f=Math.max(0,Math.min(1,(mx-x)/(double)w));return min+(max-min)*f;}
    private int pickColor(int mx){int x=246;for(int c:palette){if(mx>=x&&mx<=x+22)return c;x+=30;}return accent();}
    private boolean isHover(int mx,int my,int x1,int y1,int x2,int y2){return mx>=x1&&mx<=x2&&my>=y1&&my<=y2;}
    private int accent(){return tab==2?QModsTools.config.scoreboardAccent:QModsTools.config.chatAccent;}
    private String themeName(int i){return new String[]{"Стекло","Неон","Карточки","Минимал","Сакура","QMods"}[Math.max(0,Math.min(5,i))];}
    private static final String[] LANG_CODES={"en","uk","tr","de","es"};
    private static final String[] LANG_NAMES={"Английский","Украинский","Турецкий","Немецкий","Испанский"};
    private String langName(String code){for(int i=0;i<LANG_CODES.length;i++)if(LANG_CODES[i].equals(code))return LANG_NAMES[i];return code;}
    private String nextLang(String code){for(int i=0;i<LANG_CODES.length;i++)if(LANG_CODES[i].equals(code))return LANG_CODES[(i+1)%LANG_CODES.length];return LANG_CODES[0];}
    private String variantName(int v){return new String[]{"Обычный","Ледяной","Огненный"}[Math.max(0,Math.min(2,v))];}
    private String keyName(net.minecraft.client.settings.KeyBinding key){int c=key.getKeyCode();return c==0?"НЕТ":Keyboard.getKeyName(c);}
    @Override public boolean doesGuiPauseGame(){return false;}
}
