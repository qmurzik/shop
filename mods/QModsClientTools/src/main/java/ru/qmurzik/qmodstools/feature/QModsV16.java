package ru.qmurzik.qmodstools.feature;

import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.gui.HudEditorScreen;
import ru.qmurzik.qmodstools.util.ColorUtil;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Features introduced in 1.6.0. Reflection keeps the add-on compatible with both MCP and SRG runtime names. */
public final class QModsV16 {
    public static final String[] WIDGET_NAMES={"Команды","Киллфид","Баннеры","Статистика"};
    public static final int[] layoutX={82,2,50,2},layoutY={16,28,25,4},layoutScale={100,100,100,100};
    private static final String[] KILL_WORDS={"убил","убила","убит","убита","зарезал","застрелил","взорвал","сжег","сгорел","утонул","погиб","разбился","сбросил","скинул","столкнул","бездна","killed","slain","shot","blew up","burned","drowned","fell","void","eliminated","final kill"};
    private static final ArrayList<FeedEntry> feed=new ArrayList<FeedEntry>();
    private static final ArrayDeque<Banner> banners=new ArrayDeque<Banner>();
    private static final ArrayList<TntPoint> tntPoints=new ArrayList<TntPoint>();
    private static final ArrayList<String> cachedTeamLines=new ArrayList<String>();
    private static final FloatBuffer modelBuffer=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer(),projectionBuffer=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer(),windowBuffer=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final IntBuffer viewportBuffer=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();
    private static Object minecraft,lastWorld; private static boolean keyWasDown,match,scoreboardBefore=true,layoutLoaded,statsLoaded,ownBedLost;
    private static long matchStarted,ignoreMatchUntil,matchMissingSince; private static int matchKills,matchFinals,matchBeds,totalMatches,totalWins,totalKills,totalFinals,totalBeds,lastTeamCacheTick=-100;
    private static String matchTeam="Не определена";private static Method glGetFloat,glGetInteger,gluProject,keyDownMethod,scaledWidthMethod,scaledHeightMethod,fontDrawMethod,fontWidthMethod;private static Constructor<?> scaledResolutionCtor;private static Object cachedFont;
    private static File layoutFile,historyFile;

    private QModsV16(){}

    public static void tick(Object mc){
        minecraft=mc;ensureFiles(mc);QModsV17.tick(mc);boolean down=keyDown(65);if(down&&!keyWasDown)openEditor(mc);keyWasDown=down;
        long time=System.currentTimeMillis();Object world=field(mc,"field_71441_e","theWorld");boolean now=QModsV18.isMatchActive()&&time>=ignoreMatchUntil;
        if(now&&!match){match=true;matchStarted=time;matchKills=matchFinals=matchBeds=0;ownBedLost=false;matchMissingSince=0;matchTeam=QModsV18.getOwnTeam();scoreboardBefore=QModsTools.config.customScoreboard;QModsTools.config.customScoreboard=false;}
        if(match){if(QModsV18.isOwnBedKnown()&&!QModsV18.isOwnBedAlive())ownBedLost=true;if("Не определена".equals(matchTeam)&&!"Не определена".equals(QModsV18.getOwnTeam()))matchTeam=QModsV18.getOwnTeam();if(ownBedLost&&playerDead(mc)){finishMatch(false,"final_death");now=false;}else if(!now){if(matchMissingSince==0)matchMissingSince=time;long wait=ownBedLost?800L:3000L;if(time-matchMissingSince>=wait){if(ownBedLost)finishMatch(false,world==null||world!=lastWorld?"left":"eliminated");else stopTracking();}}else matchMissingSince=0;}
        lastWorld=world;
        for(int i=feed.size()-1;i>=0;i--)if(time-feed.get(i).time>6500L)feed.remove(i);
        while(banners.size()>5)banners.removeLast();
    }

    public static boolean onChat(Object event){
        Object message=field(event,"message");int type=intField(event,"type",0);if(message==null||type==2||(!match&&!QModsV18.isMatchActive()))return false;
        String clean=strip(stringCall(message,"func_150260_c","getUnformattedText"));if(clean.isEmpty())return false;
        String formatted=stringCall(message,"func_150254_d","getFormattedText");if(formatted.isEmpty())formatted=clean;
        String low=clean.toLowerCase(Locale.ROOT).replace('ё','е');boolean finalKill=low.contains("final kill")||low.contains("финальн")&&(low.contains("уби")||low.contains("кил")||low.contains("смерт"));
        boolean bed=!finalKill&&(low.contains("кроват")||low.contains("bed"))&&(low.contains("уничтож")||low.contains("разруш")||low.contains("сломан")||low.contains("destroy"));
        boolean kill=containsAny(low,KILL_WORDS)&&!bed;
        if(bed&&QModsTools.config.bedBreakBanner)enqueue("РАЗРУШЕНИЕ КРОВАТИ",formatted,0xFFD24D,false);
        if(finalKill&&QModsTools.config.finalKillBanner)enqueue("ФИНАЛЬНОЕ УБИЙСТВО",formatted,0xFF5577,true);
        if(kill&&QModsTools.config.killFeed)addFeed(formatted,deathIcon(low),finalKill);
        String me=playerName(minecraft).toLowerCase(Locale.ROOT);boolean mine=!me.isEmpty()&&low.contains(me);
        if(mine&&kill){matchKills++;if(finalKill)matchFinals++;}if(mine&&bed)matchBeds++;
        boolean end=low.contains("wins the game")||low.contains("game over")||low.contains("игра завершена")||low.contains("игра окончена")||low.contains("победила команда")||low.contains("выиграла команда");
        if(QModsV18.isOwnBedKnown()&&!QModsV18.isOwnBedAlive())ownBedLost=true;
        if(end&&match){boolean win=low.contains(me)||low.contains("победа")||low.contains("victory");finishMatch(win,"server_end");}
        return bed&&QModsTools.config.bedBreakBanner||kill&&QModsTools.config.killFeed;
    }

    public static void render(Object mc){
        minecraft=mc;boolean hud=match&&QModsTools.config.bedWarsHud,feedVisible=match&&QModsTools.config.killFeed&&!feed.isEmpty(),bannerVisible=!banners.isEmpty();if(!hud&&!feedVisible&&!bannerVisible&&tntPoints.isEmpty()&&!QModsV17.hasResult())return;int sw=screen(true),sh=screen(false);if(sw<=0||sh<=0)return;
        if(hud&&!QModsV17.isConcentrating()){renderTeams(sw,sh,false);renderStats(sw,sh,false);}if(feedVisible)renderFeed(sw,sh,false);if(bannerVisible)renderBanner(sw,sh,false);if(hud)renderTnt(sw,sh);QModsV17.render(mc,sw,sh);
    }

    public static void renderEditorPreview(int sw,int sh){renderTeams(sw,sh,true);renderFeed(sw,sh,true);renderBannerPreview(sw,sh);renderStats(sw,sh,true);}

    public static void captureWorld(Object mc,float partial){
        tntPoints.clear();if(!match||!QModsTools.config.bedWarsHud)return;Object world=field(mc,"field_71441_e","theWorld");Object entities=field(world,"field_72996_f","loadedEntityList");if(!(entities instanceof Iterable))return;
        try{
            if(glGetFloat==null){Class<?> gl=Class.forName("org.lwjgl.opengl.GL11");glGetFloat=gl.getMethod("glGetFloat",int.class,FloatBuffer.class);glGetInteger=gl.getMethod("glGetInteger",int.class,IntBuffer.class);gluProject=Class.forName("org.lwjgl.util.glu.GLU").getMethod("gluProject",float.class,float.class,float.class,FloatBuffer.class,FloatBuffer.class,IntBuffer.class,FloatBuffer.class);}
            modelBuffer.clear();projectionBuffer.clear();viewportBuffer.clear();glGetFloat.invoke(null,2982,modelBuffer);glGetFloat.invoke(null,2983,projectionBuffer);glGetInteger.invoke(null,2978,viewportBuffer);
            int sw=screen(true),sh=screen(false),dw=intField(mc,"field_71443_c",sw),dh=intField(mc,"field_71440_d",sh);
            for(Object e:(Iterable<?>)entities){if(e==null||!e.getClass().getName().endsWith("EntityTNTPrimed"))continue;int fuse=intField(e,"field_70516_a","fuse",-1);if(fuse<0)continue;float x=(float)lerp(doubleField(e,"field_70142_S","lastTickPosX"),doubleField(e,"field_70165_t","posX"),partial),y=(float)lerp(doubleField(e,"field_70137_T","lastTickPosY"),doubleField(e,"field_70163_u","posY"),partial)+1.15F,z=(float)lerp(doubleField(e,"field_70136_U","lastTickPosZ"),doubleField(e,"field_70161_v","posZ"),partial);windowBuffer.clear();boolean ok=(Boolean)gluProject.invoke(null,x,y,z,modelBuffer,projectionBuffer,viewportBuffer,windowBuffer);if(ok&&windowBuffer.get(2)>=0&&windowBuffer.get(2)<=1)tntPoints.add(new TntPoint(windowBuffer.get(0)*sw/dw,sh-windowBuffer.get(1)*sh/dh,fuse));}
        }catch(Throwable ignored){}
    }
    public static void clearTntPoints(){if(!tntPoints.isEmpty())tntPoints.clear();}

    public static void resetLayout(){layoutX[0]=82;layoutY[0]=16;layoutX[1]=2;layoutY[1]=28;layoutX[2]=50;layoutY[2]=25;layoutX[3]=2;layoutY[3]=4;for(int i=0;i<4;i++)layoutScale[i]=100;saveLayout();}
    public static void saveLayout(){if(layoutFile==null)return;try{FileWriter w=new FileWriter(layoutFile);for(int i=0;i<4;i++)w.write(layoutX[i]+","+layoutY[i]+","+layoutScale[i]+"\n");w.close();}catch(Exception ignored){}}
    public static int widgetAt(int mx,int my,int sw,int sh){for(int i=3;i>=0;i--){int x=layoutX[i]*sw/100,y=layoutY[i]*sh/100,w=i==0?150:i==1?185:i==2?230:145,h=i==0?92:i==1?75:i==2?43:50;float s=layoutScale[i]/100F;if(mx>=x-w*(i==2?.5:0)&&mx<=x+(i==2?w*.5:w)*s&&my>=y&&my<=y+h*s)return i;}return -1;}

    private static void renderTeams(int sw,int sh,boolean preview){List<String> filtered;if(preview)filtered=previewTeams();else{int tick=intField(field(minecraft,"field_71439_g","thePlayer"),"field_70173_aa","ticksExisted",0);if(tick-lastTeamCacheTick>=10||cachedTeamLines.isEmpty()){lastTeamCacheTick=tick;cachedTeamLines.clear();for(String s:scoreLines(minecraft)){String low=strip(s).toLowerCase(Locale.ROOT);if(isTeamLine(low))cachedTeamLines.add(s);}}filtered=cachedTeamLines;}if(filtered.isEmpty())return;int idx=0,x=layoutX[0]*sw/100,y=layoutY[0]*sh/100;float scale=layoutScale[0]/100F;DrawUtil.beginHudScale(scale,x,y);int h=20+filtered.size()*12;DrawUtil.shadow(0,0,145,h,5);DrawUtil.rounded(0,0,145,h,5,0xD0141720);DrawUtil.gradient(0,0,145,3,0xFFB060FF,0xFF45D7FF);draw("§f§lЖИВЫЕ КОМАНДЫ",8,7,0xFFFFFFFF);for(String line:filtered){String clean=strip(line);boolean dead=clean.contains("✘")||clean.contains("×")||clean.matches(".*\\b0\\b.*");draw((dead?"§8":"§f")+(dead?"✘ ":"● ")+trim(clean,22),8,20+idx++*12,0xFFFFFFFF);}DrawUtil.endHudScale();}
    private static void renderFeed(int sw,int sh,boolean preview){if(feed.isEmpty()&&!preview)return;int x=layoutX[1]*sw/100,y=layoutY[1]*sh/100;float scale=layoutScale[1]/100F;DrawUtil.beginHudScale(scale,x,y);List<FeedEntry> list=feed;if(preview&&list.isEmpty()){list=new ArrayList<FeedEntry>();list.add(new FeedEntry("§cRed §7→ §aGreen","⚔",false));list.add(new FeedEntry("§dYou §7→ §eEnemy","★",true));}long now=System.currentTimeMillis();int row=0;for(FeedEntry e:list){float a=preview?1F:Math.min(1F,(6500-(now-e.time))/500F);if(a<=0)continue;int w=Math.min(182,textWidth(e.icon+" "+e.text)+14);int color=e.fin?0xFF3657:0x151922;DrawUtil.rounded(0,row*16,w,row*16+14,4,ColorUtil.argb(color,(int)(215*a)));DrawUtil.rect(0,row*16,3,row*16+14,ColorUtil.argb(e.fin?0xFF5577:0xB060FF,(int)(255*a)));draw("§f"+e.icon+" §r"+trim(strip(e.text),27),7,row*16+3,ColorUtil.argb(0xFFFFFF,(int)(255*a)));row++;}DrawUtil.endHudScale();}
    private static void renderBanner(int sw,int sh,boolean preview){Banner b=banners.peek();if(b==null)return;long now=System.currentTimeMillis();if(now-b.start>3000L){banners.poll();Banner next=banners.peek();if(next!=null)next.start=now;return;}drawBanner(sw,sh,b.header,b.text,b.color,b.big);}
    private static void renderBannerPreview(int sw,int sh){drawBanner(sw,sh,"РАЗРУШЕНИЕ КРОВАТИ","Красная кровать разрушена!",0xFFD24D,false);}
    private static void drawBanner(int sw,int sh,String header,String text,int color,boolean big){int x=layoutX[2]*sw/100,y=layoutY[2]*sh/100;float scale=layoutScale[2]/100F*(big?1.08F:1F);DrawUtil.beginHudScale(scale,x,y);int w=Math.min(220,Math.max(textWidth(header),textWidth(text))+26);DrawUtil.shadow(-w/2,0,w/2,40,6);DrawUtil.rounded(-w/2,0,w/2,40,6,0xDC12151D);DrawUtil.gradient(-w/2,0,w/2,3,ColorUtil.argb(color,255),ColorUtil.argb(0xB060FF,180));center("§l"+header,0,8,0xFFFFFFFF);center(trim(strip(text),34),0,23,0xFFFFFFFF);DrawUtil.endHudScale();}
    private static void renderStats(int sw,int sh,boolean preview){ensureStats();int x=layoutX[3]*sw/100,y=layoutY[3]*sh/100;float scale=layoutScale[3]/100F;DrawUtil.beginHudScale(scale,x,y);DrawUtil.shadow(0,0,140,44,4);DrawUtil.rounded(0,0,140,44,4,0xC8141720);draw("§d§lQMODS §fСТАТИСТИКА",7,6,0xFFFFFFFF);draw("§7Матчи §f"+totalMatches+"  §7Победы §a"+totalWins,7,19,0xFFFFFFFF);draw("§7Киллы §f"+totalKills+"  §7Финалы §c"+totalFinals+"  §7Кровати §d"+totalBeds,7,31,0xFFFFFFFF);DrawUtil.endHudScale();}
    private static void renderTnt(int sw,int sh){for(TntPoint p:tntPoints){int x=(int)p.x,y=(int)p.y;String s=String.format(Locale.ROOT,"§c§lTNT §f%.1fс",p.fuse/20F);int w=textWidth(s)+10;DrawUtil.rounded(x-w/2,y-7,x+w/2,y+7,4,0xD0200D12);DrawUtil.rect(x-w/2,y-7,x+w/2,y-5,0xFFFF3D58);center(s,x,y-3,0xFFFFFFFF);}}

    private static void addFeed(String text,String icon,boolean fin){feed.add(new FeedEntry(text,icon,fin));while(feed.size()>5)feed.remove(0);}
    private static void enqueue(String h,String t,int c,boolean big){for(Banner b:banners)if(b.text.equals(t))return;Banner b=new Banner(h,t,c,big);if(banners.isEmpty())b.start=System.currentTimeMillis();banners.add(b);}
    private static String deathIcon(String s){if(s.contains("final" )||s.contains("финальн"))return "★";if(s.contains("shot")||s.contains("застрел"))return "➶";if(s.contains("взор")||s.contains("blew"))return "✹";if(s.contains("сгор")||s.contains("burn"))return "♨";if(s.contains("бездн")||s.contains("void")||s.contains("fell")||s.contains("сброс")||s.contains("скинул"))return "⇩";return "⚔";}
    private static boolean containsAny(String s,String[] words){for(String w:words)if(s.contains(w))return true;return false;}
    private static boolean isTeamLine(String s){return (s.contains("красн")||s.contains("син")||s.contains("зелен")||s.contains("желт")||s.contains("голуб")||s.contains("бел")||s.contains("розов")||s.contains("сер")||s.contains("red")||s.contains("blue")||s.contains("green")||s.contains("yellow")||s.contains("aqua")||s.contains("white")||s.contains("pink")||s.contains("gray"))&&(s.contains("✔")||s.contains("✘")||s.contains("×")||s.matches(".*\\b[0-9]\\b.*"));}
    private static List<String> previewTeams(){ArrayList<String> a=new ArrayList<String>();a.add("Красные ✔ 3");a.add("Синие ✔ 2");a.add("Зелёные ✘ 1");a.add("Жёлтые ✔ 4");return a;}
    private static List<String> scoreLines(Object mc){ArrayList<String> out=new ArrayList<String>();try{Object world=field(mc,"field_71441_e","theWorld"),board=call(world,new String[]{"func_96441_U","getScoreboard"});Object myTeam=call(board,new String[]{"func_96509_i","getPlayersTeam"},new Class[]{String.class},playerName(mc)),format=call(myTeam,new String[]{"func_178775_l","getChatFormat"}),color=call(format,new String[]{"func_175746_b","getColorIndex"});int slot=color instanceof Number?3+((Number)color).intValue():1;Object objective=call(board,new String[]{"func_96539_a","getObjectiveInDisplaySlot"},new Class[]{int.class},slot);if(objective==null)objective=call(board,new String[]{"func_96539_a","getObjectiveInDisplaySlot"},new Class[]{int.class},1);Object scores=call(board,new String[]{"func_96534_i","getSortedScores"},new Class[]{Class.forName("net.minecraft.scoreboard.ScoreObjective")},objective);if(scores instanceof Iterable)for(Object score:(Iterable<?>)scores){String name=stringCall(score,"func_96653_e","getPlayerName");Object team=call(board,new String[]{"func_96509_i","getPlayersTeam"},new Class[]{String.class},name);String line=(String)callStatic("net.minecraft.scoreboard.ScorePlayerTeam",new String[]{"func_96667_a","formatPlayerName"},new Class[]{Class.forName("net.minecraft.scoreboard.Team"),String.class},team,name);if(line!=null)out.add(line);}}catch(Throwable ignored){}return out;}
    private static void finishMatch(boolean win,String reason){if(!match)return;int duration=(int)Math.max(0,(System.currentTimeMillis()-matchStarted)/1000);totalMatches++;if(win)totalWins++;totalKills+=matchKills;totalFinals+=matchFinals;totalBeds+=matchBeds;try{ensureFiles(minecraft);boolean fresh=!historyFile.exists();FileWriter w=new FileWriter(historyFile,true);if(fresh)w.write("date,duration_seconds,win,kills,finals,beds,team,end_reason\n");w.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+","+duration+","+(win?1:0)+","+matchKills+","+matchFinals+","+matchBeds+","+csv(matchTeam)+","+csv(reason)+"\n");w.close();}catch(Exception ignored){}QModsV17.onMatchFinished(win,duration,matchKills,matchFinals,matchBeds);ignoreMatchUntil=System.currentTimeMillis()+15000L;stopTracking();}
    private static void stopTracking(){match=false;matchMissingSince=0;ownBedLost=false;cachedTeamLines.clear();tntPoints.clear();QModsTools.config.customScoreboard=scoreboardBefore;}
    private static boolean playerDead(Object mc){Object p=field(mc,"field_71439_g","thePlayer");if(p==null)return false;Object dead=field(p,"field_70128_L","isDead");if(dead instanceof Boolean&&(Boolean)dead)return true;Object health=call(p,new String[]{"func_110143_aJ","getHealth"});return health instanceof Number&&((Number)health).floatValue()<=0F;}
    private static String csv(String s){return s==null?"":s.replace(',', ' ').replace('\n',' ').replace('\r',' ');}
    private static void ensureFiles(Object mc){if(layoutLoaded)return;File dir=(File)field(mc,"field_71412_D","mcDataDir");if(dir==null)return;File cfg=new File(dir,"config");cfg.mkdirs();layoutFile=new File(cfg,"qmods-hud-layout.cfg");historyFile=new File(cfg,"qmods-match-history.csv");layoutLoaded=true;loadLayout();ensureStats();}
    private static void loadLayout(){if(layoutFile==null||!layoutFile.exists())return;try{BufferedReader r=new BufferedReader(new FileReader(layoutFile));for(int i=0;i<4;i++){String s=r.readLine();if(s==null)break;String[] p=s.split(",");layoutX[i]=clamp(Integer.parseInt(p[0]),0,100);layoutY[i]=clamp(Integer.parseInt(p[1]),0,100);layoutScale[i]=clamp(Integer.parseInt(p[2]),70,140);}r.close();}catch(Exception ignored){}}
    private static void ensureStats(){if(statsLoaded)return;statsLoaded=true;if(historyFile==null||!historyFile.exists())return;try{BufferedReader r=new BufferedReader(new FileReader(historyFile));String s;while((s=r.readLine())!=null){String[] p=s.split(",");if(p.length<6||p[0].equals("date"))continue;totalMatches++;totalWins+=Integer.parseInt(p[2]);totalKills+=Integer.parseInt(p[3]);totalFinals+=Integer.parseInt(p[4]);totalBeds+=Integer.parseInt(p[5]);}r.close();}catch(Exception ignored){}}
    private static void openEditor(Object mc){try{Class<?> gui=Class.forName("net.minecraft.client.gui.GuiScreen");call(mc,new String[]{"func_147108_a","displayGuiScreen"},new Class[]{gui},new HudEditorScreen());}catch(Throwable ignored){}}
    public static void closeEditor(){try{Class<?> gui=Class.forName("net.minecraft.client.gui.GuiScreen");call(minecraft,new String[]{"func_147108_a","displayGuiScreen"},new Class[]{gui},new Object[]{null});}catch(Throwable ignored){}}
    private static boolean keyDown(int code){try{if(keyDownMethod==null)keyDownMethod=Class.forName("org.lwjgl.input.Keyboard").getMethod("isKeyDown",int.class);return (Boolean)keyDownMethod.invoke(null,code);}catch(Throwable ignored){return false;}}
    private static int screen(boolean width){try{if(scaledResolutionCtor==null){Class<?> c=Class.forName("net.minecraft.client.gui.ScaledResolution");scaledResolutionCtor=c.getConstructor(Class.forName("net.minecraft.client.Minecraft"));scaledWidthMethod=findAny(c,new String[]{"func_78326_a","getScaledWidth"},0);scaledHeightMethod=findAny(c,new String[]{"func_78328_b","getScaledHeight"},0);}Object sr=scaledResolutionCtor.newInstance(minecraft);return ((Number)(width?scaledWidthMethod:scaledHeightMethod).invoke(sr)).intValue();}catch(Throwable ignored){return 0;}}
    private static Object font(){if(cachedFont==null)cachedFont=field(minecraft,"field_71466_p","fontRendererObj");return cachedFont;}
    private static void draw(String text,int x,int y,int color){Object f=font();if(f==null)return;try{if(fontDrawMethod==null)fontDrawMethod=findAny(f.getClass(),new String[]{"func_175065_a","drawString"},5);fontDrawMethod.invoke(f,text,(float)x,(float)y,color,true);}catch(Exception ignored){}}
    private static void center(String text,int x,int y,int color){draw(text,x-textWidth(text)/2,y,color);}
    private static int textWidth(String text){Object f=font();if(f==null)return text.length()*6;try{if(fontWidthMethod==null)fontWidthMethod=findAny(f.getClass(),new String[]{"func_78256_a","getStringWidth"},1);return ((Number)fontWidthMethod.invoke(f,text)).intValue();}catch(Exception ignored){return text.length()*6;}}
    private static String playerName(Object mc){Object p=field(mc,"field_71439_g","thePlayer");return stringCall(p,"func_70005_c_","getName");}
    private static String strip(String s){return s==null?"":s.replaceAll("§[0-9A-FK-ORa-fk-or]","").replace('\u00A0',' ').trim();}
    private static String trim(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-1))+"…";}
    private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private static double lerp(double a,double b,float t){return a+(b-a)*t;}
    private static Object field(Object o,String...names){if(o==null)return null;for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())for(String n:names)try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception ignored){}return null;}
    private static int intField(Object o,String n,int d){Object v=field(o,n);return v instanceof Number?((Number)v).intValue():d;}private static int intField(Object o,String a,String b,int d){Object v=field(o,a,b);return v instanceof Number?((Number)v).intValue():d;}private static double doubleField(Object o,String...n){Object v=field(o,n);return v instanceof Number?((Number)v).doubleValue():0;}
    private static Object call(Object o,String[] names,Object...args){return call(o,names,new Class[0],args);}private static Object call(Object o,String[] names,Class<?>[] types,Object...args){if(o==null)return null;for(String n:names)try{Method m=findMethod(o.getClass(),n,types.length);m.setAccessible(true);return m.invoke(o,args);}catch(Exception ignored){}return null;}
    private static Object callStatic(String cn,String[] names,Class<?>[] types,Object...args)throws Exception{Class<?> c=Class.forName(cn);for(String n:names)try{Method m=findMethod(c,n,types.length);m.setAccessible(true);return m.invoke(null,args);}catch(Exception ignored){}return null;}
    private static Method findAny(Class<?> c,String[] names,int count)throws NoSuchMethodException{for(String n:names)try{Method m=findMethod(c,n,count);m.setAccessible(true);return m;}catch(Exception ignored){}throw new NoSuchMethodException();}
    private static Method findMethod(Class<?> c,String name,int count)throws NoSuchMethodException{for(Class<?> x=c;x!=null;x=x.getSuperclass())for(Method m:x.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==count)return m;throw new NoSuchMethodException(name);}
    private static String stringCall(Object o,String...names){Object v=call(o,names);return v==null?"":String.valueOf(v);}
    private static final class FeedEntry{final String text,icon;final boolean fin;final long time=System.currentTimeMillis();FeedEntry(String t,String i,boolean f){text=t;icon=i;fin=f;}}
    private static final class Banner{final String header,text;final int color;final boolean big;long start;Banner(String h,String t,int c,boolean b){header=h;text=t;color=c;big=b;}}
    private static final class TntPoint{final float x,y;final int fuse;TntPoint(float a,float b,int f){x=a;y=b;fuse=f;}}
}
