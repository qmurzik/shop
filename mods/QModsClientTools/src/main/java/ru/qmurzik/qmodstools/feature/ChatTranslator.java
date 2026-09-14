package ru.qmurzik.qmodstools.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Keyless translator with two independent providers, timeouts and a small cache. */
public final class ChatTranslator {
    public interface Callback { void onResult(String translated); }
    private static final Map<String,String> CACHE=new LinkedHashMap<String,String>(64,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,String> eldest){return size()>64;}
    };
    private ChatTranslator() {}

    public static void translate(final String text, final String from, final String to, final Callback callback) {
        final Minecraft mc=Minecraft.getMinecraft();
        final String source=normalizeLanguage(from,text),target=normalizeTarget(to);
        final String key=source+'>'+target+':'+text;
        synchronized(CACHE){if(CACHE.containsKey(key)){final String cached=CACHE.get(key);mc.addScheduledTask(new Runnable(){public void run(){callback.onResult(cached);}});return;}}
        Thread t=new Thread("QMods-Translate"){
            @Override public void run(){
                String result=google(text,source,target);
                if(result==null)result=myMemory(text,source,target);
                if(result!=null){result=result.trim();if(!result.isEmpty())synchronized(CACHE){CACHE.put(key,result);}}
                final String delivered=result;
                mc.addScheduledTask(new Runnable(){public void run(){callback.onResult(delivered);}});
            }
        };
        t.setDaemon(true);t.start();
    }

    private static String google(String text,String from,String to){
        HttpURLConnection conn=null;
        try{
            String query="client=gtx&sl="+from+"&tl="+to+"&dt=t&q="+URLEncoder.encode(text,"UTF-8");
            conn=open("https://translate.googleapis.com/translate_a/single?"+query);
            JsonArray segments=new JsonParser().parse(read(conn)).getAsJsonArray().get(0).getAsJsonArray();
            StringBuilder out=new StringBuilder();
            for(JsonElement seg:segments){JsonArray part=seg.getAsJsonArray();if(part.size()>0&&!part.get(0).isJsonNull())out.append(part.get(0).getAsString());}
            return out.length()==0?null:out.toString();
        }catch(Exception ignored){return null;}finally{if(conn!=null)conn.disconnect();}
    }

    private static String myMemory(String text,String from,String to){
        HttpURLConnection conn=null;
        try{
            String query="q="+URLEncoder.encode(text,"UTF-8")+"&langpair="+from+"%7C"+to;
            conn=open("https://api.mymemory.translated.net/get?"+query);
            JsonObject root=new JsonParser().parse(read(conn)).getAsJsonObject();
            JsonObject data=root.getAsJsonObject("responseData");
            if(data==null||data.get("translatedText")==null)return null;
            String result=data.get("translatedText").getAsString();
            return decodeEntities(result);
        }catch(Exception ignored){return null;}finally{if(conn!=null)conn.disconnect();}
    }

    private static HttpURLConnection open(String address)throws Exception{
        HttpURLConnection conn=(HttpURLConnection)new URL(address).openConnection();
        conn.setRequestMethod("GET");conn.setConnectTimeout(5500);conn.setReadTimeout(6500);
        conn.setUseCaches(false);conn.setRequestProperty("User-Agent","QMods/1.9.1 Minecraft/1.8.9");
        conn.setRequestProperty("Accept","application/json");conn.setRequestProperty("Accept-Charset","UTF-8");
        int code=conn.getResponseCode();if(code<200||code>=300)throw new java.io.IOException("HTTP "+code);
        return conn;
    }

    private static String read(HttpURLConnection conn)throws Exception{
        InputStream in=conn.getInputStream();BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
        StringBuilder sb=new StringBuilder();String line;while((line=reader.readLine())!=null)sb.append(line);reader.close();return sb.toString();
    }
    private static String normalizeLanguage(String lang,String text){if(lang!=null&&!"auto".equalsIgnoreCase(lang))return lang.toLowerCase();return hasCyrillic(text)?"ru":"en";}
    private static String normalizeTarget(String lang){return lang==null||lang.trim().isEmpty()?"en":lang.trim().toLowerCase();}
    private static boolean hasCyrillic(String s){for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c>=0x0400&&c<=0x04FF)return true;}return false;}
    private static String decodeEntities(String s){return s.replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");}
}
