package com.onehumanawa.cfhcore.content.gui.core.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class GifSearch {
   private static final String KLIPY = "https://api.klipy.com/v2/";

   public static boolean hasKey() {
      return !ownKey().isEmpty() || !proxy().isEmpty();
   }

   private static String ownKey() {
      String k = MediaConfig.get().tenorApiKey;
      return k == null ? "" : k.trim();
   }

   private static String proxy() {
      String p = MediaConfig.get().tenorProxy;
      if (p != null && !p.isBlank()) {
         return p.trim().endsWith("/") ? p.trim() : p.trim() + "/";
      } else {
         return "";
      }
   }

   public static void search(String query, Consumer<Result> callback) {
      run("search", "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8), callback);
   }

   public static void trending(Consumer<Result> callback) {
      run("featured", "", callback);
   }

   private static void run(String endpoint, String query, Consumer<Result> callback) {
      String key = ownKey();
      String proxy = proxy();
      if (key.isEmpty() && proxy.isEmpty()) {
         callback.accept(new Result(GifSearch.Result.State.NO_KEY, List.of()));
      } else {
         boolean viaProxy = key.isEmpty();
         Thread worker = new Thread(() -> {
            Result result;
            try {
               String var10000 = query.isEmpty() ? "" : query + "&";
               String params = var10000 + "limit=30&media_filter=gif,tinygif&contentfilter=medium";
               String url = viaProxy ? proxy + endpoint + "?" + params : "https://api.klipy.com/v2/" + endpoint + "?" + params + "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
               HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
               conn.setConnectTimeout(8000);
               conn.setReadTimeout(10000);
               conn.setRequestProperty("User-Agent", MediaConfig.get().userAgent);
               InputStream in = conn.getInputStream();

               try {
                  JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                  JsonArray results = root.getAsJsonArray("results");
                  List<String> urls = new ArrayList(results == null ? 0 : results.size());
                  if (results != null) {
                     for(int i = 0; i < results.size(); ++i) {
                        JsonObject formats = results.get(i).getAsJsonObject().getAsJsonObject("media_formats");
                        if (formats != null) {
                           JsonObject gif = formats.has("gif") ? formats.getAsJsonObject("gif") : (formats.has("tinygif") ? formats.getAsJsonObject("tinygif") : null);
                           if (gif != null && gif.has("url")) {
                              urls.add(gif.get("url").getAsString());
                           }
                        }
                     }
                  }

                  result = new Result(GifSearch.Result.State.OK, urls);
               } catch (Throwable var18) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (in != null) {
                  in.close();
               }
            } catch (Exception e) {
               CFHGui.LOGGER.warn("[CFHGui] gif search failed{}: {}", viaProxy ? " (through " + proxy + ")" : "", e.toString());
               result = new Result(viaProxy ? GifSearch.Result.State.UNAVAILABLE : GifSearch.Result.State.ERROR, List.of());
            }

            Result done = result;
            Minecraft.getInstance().execute(() -> callback.accept(done));
         }, "cfhgui-gifsearch");
         worker.setDaemon(true);
         worker.start();
      }
   }

   private GifSearch() {
   }

   public static record Result(State state, List<String> urls) {
      public static enum State {
         NO_KEY,
         ERROR,
         UNAVAILABLE,
         OK;
      }
   }
}
