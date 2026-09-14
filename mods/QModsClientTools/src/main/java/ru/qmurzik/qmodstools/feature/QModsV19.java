package ru.qmurzik.qmodstools.feature;

import ru.qmurzik.qmodstools.QModsTools;
import ru.qmurzik.qmodstools.gui.CrosshairEditorScreen;
import ru.qmurzik.qmodstools.util.DrawUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

/** Lightweight custom crosshair and inventory mouse-hotbar bridge. */
public final class QModsV19 {
    public static final String[] SHAPES={"Крест","Точка","Круг","T-образный"};
    public static final int[] COLORS={0xFFFFFF,0x55FF88,0x45D7FF,0xB060FF,0xFF5577,0xFFD24D};
    public static boolean crosshairEnabled=true,crosshairOutline=true,crosshairHitColor=true,crosshairDynamic=false,inventoryMouseSwap=true;
    public static int shape=0,size=5,gap=3,thickness=1,color=0xFFFFFF;
    private static Object minecraft;private static File file;private static boolean loaded;private static long hitUntil;
    private static Constructor<?> scaledCtor;private static Method scaledWidth,scaledHeight;
    private QModsV19(){}

    public static void tick(Object mc){minecraft=mc;load();}

    public static void onCrosshair(Object event,Object mc){
        minecraft=mc;load();if(!crosshairEnabled||event==null)return;Object type=field(event,"type");if(type==null||!String.valueOf(type).contains("CROSSHAIRS"))return;
        call(event,new String[]{"setCanceled"},true);Object player=field(mc,"field_71439_g","thePlayer");if(player==null)return;int sw=screen(mc,true),sh=screen(mc,false);if(sw<=0||sh<=0)return;
        int extra=0;if(crosshairDynamic){double mx=doubleField(player,"field_70159_w","motionX"),mz=doubleField(player,"field_70179_y","motionZ");extra=Math.min(5,(int)(Math.sqrt(mx*mx+mz*mz)*16D));if(!boolField(player,"field_70122_E","onGround"))extra+=2;}
        int c=crosshairHitColor&&System.currentTimeMillis()<hitUntil?QModsTools.config.hitColor:color;drawCross(sw/2,sh/2,gap+extra,size,thickness,c,crosshairOutline,shape);
    }

    public static void onHurt(Object event,Object mc){Object target=field(event,"entityLiving"),source=field(event,"source"),attacker=call(source,new String[]{"func_76346_g","getEntity"}),player=field(mc,"field_71439_g","thePlayer");if(attacker==player&&target!=null&&target!=player)hitUntil=System.currentTimeMillis()+220L;}

    public static void onGuiMouse(Object event,Object mc){
        minecraft=mc;load();if(!inventoryMouseSwap||event==null||mc==null)return;Object gui=field(event,"gui");if(gui==null||!inherits(gui.getClass(),"net.minecraft.client.gui.inventory.GuiContainer"))return;
        Object pressed=callStatic("org.lwjgl.input.Mouse","getEventButtonState");Object rawButton=callStatic("org.lwjgl.input.Mouse","getEventButton");if(!(pressed instanceof Boolean)||!(Boolean)pressed||!(rawButton instanceof Number))return;int button=((Number)rawButton).intValue();if(button<0)return;
        Object settings=field(mc,"field_71474_y","gameSettings"),hotbar=field(settings,"field_151456_ac","keyBindsHotbar");if(!(hotbar instanceof Object[])||((Object[])hotbar).length<9)return;Object key=((Object[])hotbar)[8],code=call(key,new String[]{"func_151463_i","getKeyCode"});if(!(code instanceof Number)||((Number)code).intValue()!=button-100)return;
        int width=intField(gui,"field_146294_l","width",0),height=intField(gui,"field_146295_m","height",0),dw=intField(mc,"field_71443_c","displayWidth",1),dh=intField(mc,"field_71440_d","displayHeight",1);Object ex=callStatic("org.lwjgl.input.Mouse","getEventX"),ey=callStatic("org.lwjgl.input.Mouse","getEventY");if(!(ex instanceof Number)||!(ey instanceof Number))return;int mx=((Number)ex).intValue()*width/Math.max(1,dw),my=height-((Number)ey).intValue()*height/Math.max(1,dh)-1;
        Object slot=call(gui,new String[]{"func_146975_c","getSlotAtPosition"},mx,my);if(slot==null)return;int slotNumber=intField(slot,"field_75222_d","slotNumber",-1);Object container=field(gui,"field_147002_h","inventorySlots");int window=intField(container,"field_75152_c","windowId",-1);Object controller=field(mc,"field_71442_b","playerController"),player=field(mc,"field_71439_g","thePlayer");if(slotNumber<0||window<0||controller==null||player==null)return;
        Object result=call(controller,new String[]{"func_78753_a","windowClick"},window,slotNumber,8,2,player);call(event,new String[]{"setCanceled"},true);
    }

    public static void openEditor(Object parent){try{Class<?> screen=Class.forName("net.minecraft.client.gui.GuiScreen");Object editor=new CrosshairEditorScreen((net.minecraft.client.gui.GuiScreen)parent);call(minecraft,new String[]{"func_147108_a","displayGuiScreen"},editor);}catch(Throwable ignored){}}
    public static void nextShape(){shape=(shape+1)%SHAPES.length;save();}
    public static void reset(){crosshairEnabled=true;crosshairOutline=true;crosshairHitColor=true;crosshairDynamic=false;inventoryMouseSwap=true;shape=0;size=5;gap=3;thickness=1;color=0xFFFFFF;save();}
    public static void save(){if(file==null)return;try{Properties p=new Properties();p.setProperty("enabled",String.valueOf(crosshairEnabled));p.setProperty("outline",String.valueOf(crosshairOutline));p.setProperty("hit_color",String.valueOf(crosshairHitColor));p.setProperty("dynamic",String.valueOf(crosshairDynamic));p.setProperty("inventory_mouse_swap",String.valueOf(inventoryMouseSwap));p.setProperty("shape",String.valueOf(shape));p.setProperty("size",String.valueOf(size));p.setProperty("gap",String.valueOf(gap));p.setProperty("thickness",String.valueOf(thickness));p.setProperty("color",Integer.toHexString(color));FileOutputStream out=new FileOutputStream(file);p.store(out,"QMods 1.9 crosshair and controls");out.close();}catch(Exception ignored){}}
    private static void load(){if(loaded)return;Object dir=field(minecraft,"field_71412_D","mcDataDir");if(!(dir instanceof File))return;file=new File(new File((File)dir,"config"),"qmods-crosshair.cfg");file.getParentFile().mkdirs();Properties p=new Properties();try{if(file.exists()){FileInputStream in=new FileInputStream(file);p.load(in);in.close();}crosshairEnabled=bool(p,"enabled",crosshairEnabled);crosshairOutline=bool(p,"outline",crosshairOutline);crosshairHitColor=bool(p,"hit_color",crosshairHitColor);crosshairDynamic=bool(p,"dynamic",crosshairDynamic);inventoryMouseSwap=bool(p,"inventory_mouse_swap",inventoryMouseSwap);shape=clamp(num(p,"shape",shape),0,3);size=clamp(num(p,"size",size),2,12);gap=clamp(num(p,"gap",gap),0,10);thickness=clamp(num(p,"thickness",thickness),1,4);try{color=Integer.parseInt(p.getProperty("color",Integer.toHexString(color)),16)&0xFFFFFF;}catch(Exception ignored){}}catch(Exception ignored){}loaded=true;}

    public static void drawCross(int x,int y,int g,int s,int t,int c,boolean outline,int form){if(outline)drawShape(x,y,g,s,t+2,0xE0000000,form);drawShape(x,y,g,s,t,0xFF000000|(c&0xFFFFFF),form);}
    private static void drawShape(int x,int y,int g,int s,int t,int c,int form){if(form==1){DrawUtil.rounded(x-t,y-t,x+t+1,y+t+1,Math.max(1,t),c);return;}if(form==2){int r=g+s;DrawUtil.rect(x-r,y-t,x-r+t,y+t+1,c);DrawUtil.rect(x+r-t+1,y-t,x+r+1,y+t+1,c);DrawUtil.rect(x-t,y-r,x+t+1,y-r+t,c);DrawUtil.rect(x-t,y+r-t+1,x+t+1,y+r+1,c);return;}DrawUtil.rect(x-g-s,y-t/2,x-g,y+(t+1)/2,c);DrawUtil.rect(x+g,y-t/2,x+g+s,y+(t+1)/2,c);DrawUtil.rect(x-t/2,y-g-s,x+(t+1)/2,y-g,c);if(form!=3)DrawUtil.rect(x-t/2,y+g,x+(t+1)/2,y+g+s,c);}
    private static int screen(Object mc,boolean width){try{if(scaledCtor==null){Class<?> c=Class.forName("net.minecraft.client.gui.ScaledResolution"),m=Class.forName("net.minecraft.client.Minecraft");scaledCtor=c.getConstructor(m);scaledWidth=find(c,new String[]{"func_78326_a","getScaledWidth"},0);scaledHeight=find(c,new String[]{"func_78328_b","getScaledHeight"},0);}Object sr=scaledCtor.newInstance(mc);return ((Number)(width?scaledWidth:scaledHeight).invoke(sr)).intValue();}catch(Exception ignored){return 0;}}
    private static boolean bool(Properties p,String k,boolean d){return Boolean.parseBoolean(p.getProperty(k,String.valueOf(d)));}private static int num(Properties p,String k,int d){try{return Integer.parseInt(p.getProperty(k,String.valueOf(d)));}catch(Exception e){return d;}}private static int clamp(int n,int a,int b){return Math.max(a,Math.min(b,n));}
    private static boolean inherits(Class<?> c,String name){for(Class<?> x=c;x!=null;x=x.getSuperclass())if(x.getName().equals(name))return true;return false;}
    private static Object field(Object o,String...names){if(o==null)return null;for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())for(String n:names)try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception ignored){}return null;}private static int intField(Object o,String a,String b,int d){Object v=field(o,a,b);return v instanceof Number?((Number)v).intValue():d;}private static double doubleField(Object o,String...n){Object v=field(o,n);return v instanceof Number?((Number)v).doubleValue():0D;}private static boolean boolField(Object o,String...n){Object v=field(o,n);return v instanceof Boolean&&(Boolean)v;}
    private static Object call(Object o,String[] names,Object...args){if(o==null)return null;for(String n:names)try{Method m=find(o.getClass(),new String[]{n},args.length);return m.invoke(o,args);}catch(Exception ignored){}return null;}private static Object callStatic(String cn,String name,Object...args){try{Method m=find(Class.forName(cn),new String[]{name},args.length);return m.invoke(null,args);}catch(Exception ignored){return null;}}
    private static Method find(Class<?> c,String[] names,int count)throws NoSuchMethodException{for(Class<?> x=c;x!=null;x=x.getSuperclass())for(String n:names)for(Method m:x.getDeclaredMethods())if(m.getName().equals(n)&&m.getParameterTypes().length==count){m.setAccessible(true);return m;}throw new NoSuchMethodException();}
}
