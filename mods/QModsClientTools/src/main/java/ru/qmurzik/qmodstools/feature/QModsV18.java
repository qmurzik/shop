package ru.qmurzik.qmodstools.feature;

import ru.qmurzik.qmodstools.QModsTools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/** Central low-allocation scheduler and cached MineBlaze match state for 1.8.0. */
public final class QModsV18 {
    private static Object minecraft, observedWorld;
    private static int lastBowTick=Integer.MIN_VALUE,lastBedTick=Integer.MIN_VALUE,lastPingTick=Integer.MIN_VALUE,lastCombatTick=Integer.MIN_VALUE,lastCaptureTick=Integer.MIN_VALUE;
    private static boolean bowWasActive,matchActive,ownBedKnown,ownBedAlive=true,pingWasEnabled;
    private static String ownTeam="Не определена",ownTeamKey="";
    private QModsV18(){}

    public static void optimizedTick(Object mc,Object predictor,Object bedWars,Object cps,Object combat,Object ping){
        minecraft=mc;QModsV19.tick(mc);Object player=field(mc,"field_71439_g","thePlayer"),world=field(mc,"field_71441_e","theWorld");
        if(world!=observedWorld){observedWorld=world;lastBedTick=Integer.MIN_VALUE;matchActive=false;ownBedKnown=false;ownBedAlive=true;ownTeam="Не определена";ownTeamKey="";}
        int tick=intField(player,"field_70173_aa","ticksExisted",0);
        boolean bowFeatures=QModsTools.config.trajectory||QModsTools.config.targetPrediction||QModsTools.config.aimAssist||QModsTools.config.aimGuide;
        boolean bow=player!=null&&world!=null&&bowFeatures&&isDrawingBow(player);
        int bowInterval=QModsTools.config.lowPower?Math.max(4,QModsTools.config.calculationInterval):Math.max(2,QModsTools.config.calculationInterval);
        if(bow&&tick-lastBowTick>=bowInterval){lastBowTick=tick;call(predictor,"tick");call(predictor,"updateForCurrentBow");}
        else if(!bow&&bowWasActive)call(predictor,"updateForCurrentBow");
        bowWasActive=bow;if(bow)call(predictor,"applySmoothAim");

        int bedInterval=QModsTools.config.lowPower?2:1;
        boolean bedWarsOn=QModsTools.config.mineBlazeMode;
        if(!bedWarsOn)matchActive=false;
        if(bedWarsOn&&(player==null||world==null||tick-lastBedTick>=bedInterval)){lastBedTick=tick;call(bedWars,"tick");}
        if(bedWarsOn&&player!=null&&world!=null&&tick%10==0)refreshMatchState(mc);
        if(QModsTools.config.cpsHud)call(cps,"tick");
        if(player==null||tick-lastCombatTick>=5){lastCombatTick=tick;call(combat,"tick");}
        boolean pingEnabled=QModsTools.config.pingHud;
        if(pingEnabled&&(tick-lastPingTick>=20||!pingWasEnabled)){lastPingTick=tick;call(ping,"tick");}
        else if(!pingEnabled&&pingWasEnabled)call(ping,"stopWorker");
        pingWasEnabled=pingEnabled;
    }

    public static void captureWorld(Object mc,float partial){
        Object player=field(mc,"field_71439_g","thePlayer");int tick=intField(player,"field_70173_aa","ticksExisted",-1);
        if(!matchActive||player==null){QModsV16.clearTntPoints();return;}
        if(tick==lastCaptureTick)return;lastCaptureTick=tick;QModsV16.captureWorld(mc,partial);
    }

    public static boolean shouldRenderInfo(){return QModsTools.config.pingHud||QModsTools.config.cpsHud||QModsTools.config.coordsHud;}
    public static boolean shouldRenderCombat(Object combat){
        long now=System.currentTimeMillis();return longField(combat,"hitMarkerUntil",0)>now||longField(combat,"damageFlashUntil",0)>now||!empty(field(combat,"popups"))||!empty(field(combat,"indicators"));
    }
    public static void renderInfo(Object info){if(shouldRenderInfo())call(info,"render");}
    public static void renderCombat(Object combat){if(shouldRenderCombat(combat))call(combat,"render");}

    public static void observeChat(Object event,Object mc){
        minecraft=mc;if(!QModsTools.config.mineBlazeMode)return;Object message=field(event,"message");if(message==null||intField(event,"type",0)==2)return;
        String clean=normalize(stringCall(message,"func_150260_c","getUnformattedText"));if(clean.isEmpty())return;
        boolean bed=containsBedEvent(clean),start=clean.contains("игра нач")||clean.contains("game start")||clean.contains("защитите свою кровать"),lobby=clean.contains("возвращение в лобби")||clean.contains("waiting for players")||clean.contains("ожидание игроков");
        if(start||bed||clean.contains("final kill")||clean.contains("финальн"))matchActive=true;
        if(lobby)matchActive=false;
        if(bed&&isOwnBedMessage(clean)){ownBedKnown=true;ownBedAlive=false;}
    }

    public static boolean isMatchActive(){return matchActive;}
    public static boolean isOwnBedKnown(){return ownBedKnown;}
    public static boolean isOwnBedAlive(){return !ownBedKnown||ownBedAlive;}
    public static String getOwnTeam(){return ownTeam;}

    private static void refreshMatchState(Object mc){
        Object world=field(mc,"field_71441_e","theWorld"),player=field(mc,"field_71439_g","thePlayer");if(world==null||player==null){matchActive=false;return;}
        Object state=callStatic("ru.qmurzik.qmodstools.feature.BedWarsHelper","isMatchInProgress",mc);if(state instanceof Boolean)matchActive=(Boolean)state;
        Object board=call(world,"func_96441_U","getScoreboard");String playerName=stringCall(player,"func_70005_c_","getName");Object team=call(board,new String[]{"func_96509_i","getPlayersTeam"},playerName);
        if(team!=null){String reg=stringCall(team,"func_96661_b","getRegisteredName");String prefix=normalize(stringCall(team,"func_96668_e","getColorPrefix","getPrefix"));Object format=call(team,"func_178775_l","getChatFormat");int color=intValue(call(format,"func_175746_b","getColorIndex"),-1);ownTeamKey=normalize(reg+" "+prefix+" "+colorName(color)+" "+colorStem(color));String display=colorName(color);ownTeam=display.isEmpty()?(reg.isEmpty()?"Не определена":reg):display;}
        Object objective=objective(board,team);Object scores=call(board,new String[]{"func_96534_i","getSortedScores"},objective);if(!(scores instanceof Iterable))return;
        Boolean bed=null;
        for(Object score:(Iterable<?>)scores){String name=stringCall(score,"func_96653_e","getPlayerName");Object lineTeam=call(board,new String[]{"func_96509_i","getPlayersTeam"},name);Object formatted=callStatic("net.minecraft.scoreboard.ScorePlayerTeam","func_96667_a",lineTeam,name);String line=normalize(String.valueOf(formatted));if(line.isEmpty())continue;if(isOwnTeamLine(line)){if(line.contains("✘")||line.contains("×")||line.contains(" x ")||line.contains("destroyed")||line.contains("сломана"))bed=false;else if(line.contains("✔")||line.contains("✓")||line.contains(" bed")||line.contains("кровать"))bed=true;}}
        if(bed!=null){ownBedKnown=true;ownBedAlive=bed;}
    }

    private static boolean isOwnTeamLine(String line){if(line.contains("(вы)")||line.contains("(you)"))return true;if(ownTeamKey.isEmpty())return false;for(String token:ownTeamKey.split(" "))if(token.length()>2&&line.contains(token))return true;return false;}
    private static boolean isOwnBedMessage(String line){if(line.contains("ваша кроват")||line.contains("your bed"))return true;if(line.contains("(вы)")||line.contains("(you)"))return true;return isOwnTeamLine(line);}
    private static boolean containsBedEvent(String s){return (s.contains("кроват")||s.contains("bed"))&&(s.contains("уничтож")||s.contains("разруш")||s.contains("сломан")||s.contains("destroy"));}
    private static Object objective(Object board,Object team){Object format=call(team,"func_178775_l","getChatFormat");int color=intValue(call(format,"func_175746_b","getColorIndex"),-1);Object o=color>=0?call(board,new String[]{"func_96539_a","getObjectiveInDisplaySlot"},3+color):null;return o!=null?o:call(board,new String[]{"func_96539_a","getObjectiveInDisplaySlot"},1);}
    private static String colorName(int c){switch(c){case 4:case 12:return "Красные";case 1:case 9:return "Синие";case 2:case 10:return "Зелёные";case 14:return "Жёлтые";case 11:case 3:return "Голубые";case 15:return "Белые";case 13:case 5:return "Розовые";case 7:case 8:return "Серые";default:return "";}}
    private static String colorStem(int c){switch(c){case 4:case 12:return "красн";case 1:case 9:return "син";case 2:case 10:return "зелен";case 14:return "желт";case 11:case 3:return "голуб";case 15:return "бел";case 13:case 5:return "розов";case 7:case 8:return "сер";default:return "";}}
    private static boolean isDrawingBow(Object player){if(!boolCall(player,"func_71039_bw","isUsingItem"))return false;Object stack=call(player,"func_71011_bu","getItemInUse"),item=call(stack,"func_77973_b","getItem");return item!=null&&item.getClass().getName().endsWith("ItemBow");}
    private static boolean empty(Object o){return !(o instanceof java.util.Collection)||((java.util.Collection<?>)o).isEmpty();}
    private static String normalize(String s){return s==null?"":s.replaceAll("§[0-9A-FK-ORa-fk-or]","").replace(' ',' ').replace('ё','е').trim().toLowerCase(Locale.ROOT);}
    private static Object field(Object o,String...names){if(o==null)return null;for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())for(String n:names)try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception ignored){}return null;}
    private static int intField(Object o,String n,int d){Object v=field(o,n);return intValue(v,d);}private static int intField(Object o,String a,String b,int d){Object v=field(o,a,b);return intValue(v,d);}private static int intValue(Object v,int d){return v instanceof Number?((Number)v).intValue():d;}private static long longField(Object o,String n,long d){Object v=field(o,n);return v instanceof Number?((Number)v).longValue():d;}
    private static Object call(Object o,String...names){return call(o,names,new Object[0]);}private static Object call(Object o,String[] names,Object...args){if(o==null)return null;for(String n:names)try{Method m=find(o.getClass(),n,args.length);m.setAccessible(true);return m.invoke(o,args);}catch(Exception ignored){}return null;}
    private static Object callStatic(String cn,String name,Object...args){try{Method m=find(Class.forName(cn),name,args.length);m.setAccessible(true);return m.invoke(null,args);}catch(Exception ignored){return null;}}
    private static Method find(Class<?> c,String n,int count)throws NoSuchMethodException{for(Class<?> x=c;x!=null;x=x.getSuperclass())for(Method m:x.getDeclaredMethods())if(m.getName().equals(n)&&m.getParameterTypes().length==count)return m;throw new NoSuchMethodException(n);}
    private static String stringCall(Object o,String...names){Object v=call(o,names);return v==null?"":String.valueOf(v);}private static boolean boolCall(Object o,String...names){Object v=call(o,names);return v instanceof Boolean&&(Boolean)v;}
}
