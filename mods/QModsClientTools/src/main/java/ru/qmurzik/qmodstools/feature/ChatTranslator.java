package ru.qmurzik.qmodstools.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Free, keyless translation via Google's public "translate_a/single" endpoint. Runs off-thread, delivers the result back on the client thread. */
public final class ChatTranslator {
    public interface Callback { void onResult(String translated); }
    private ChatTranslator() {}

    public static void translate(final String text, final String from, final String to, final Callback callback) {
        final Minecraft mc = Minecraft.getMinecraft();
        Thread t = new Thread("QMods-Translate") {
            @Override public void run() {
                final String result = fetch(text, from, to);
                mc.addScheduledTask(new Runnable() { public void run() { callback.onResult(result); } });
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private static String fetch(String text, String from, String to) {
        try {
            String query = "client=gtx&sl=" + from + "&tl=" + to + "&dt=t&q=" + URLEncoder.encode(text, "UTF-8");
            URL url = new URL("https://translate.googleapis.com/translate_a/single?" + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000); conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JsonElement root = new JsonParser().parse(sb.toString());
            JsonArray segments = root.getAsJsonArray().get(0).getAsJsonArray();
            StringBuilder out = new StringBuilder();
            for (JsonElement seg : segments) {
                JsonArray part = seg.getAsJsonArray();
                if (part.size() > 0 && !part.get(0).isJsonNull()) out.append(part.get(0).getAsString());
            }
            return out.length() > 0 ? out.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
