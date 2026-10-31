package com.onehumanawa.cfhcore.content.gui.core.media;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public final class LinkPreviews {
   private static final int MAX = 128;
   private static final int TIMEOUT_MS = 5000;
   private static final int MAX_ANSWER_BYTES = 65536;
   private static final int MAX_TEXT = 160;
   private static final Map<String, Preview> PREVIEWS = new LinkedHashMap<String, Preview>(64, 0.75F, true) {
      protected boolean removeEldestEntry(Map.Entry<String, Preview> eldest) {
         return this.size() > 128;
      }
   };

   public static @Nullable Preview get(MediaLink link) {
      if (!Attachment.embedLinks) {
         return null;
      } else {
         synchronized(PREVIEWS) {
            return (Preview)PREVIEWS.computeIfAbsent(link.key(), (k) -> {
               Preview fresh = new Preview();
               MediaCache.background(() -> ask(link, fresh));
               return fresh;
            });
         }
      }
   }

   private static void ask(MediaLink link, Preview p) {
      try {
         HttpURLConnection conn = (HttpURLConnection)URI.create(link.oembedUrl()).toURL().openConnection();
         conn.setConnectTimeout(5000);
         conn.setReadTimeout(5000);
         conn.setRequestProperty("User-Agent", MediaConfig.get().userAgent);
         conn.setRequestProperty("Accept", "application/json");
         int code = conn.getResponseCode();
         if (code != 200) {
            throw new IOException("HTTP " + code);
         }

         InputStream in = conn.getInputStream();

         JsonObject answer;
         try {
            answer = JsonParser.parseString(new String(in.readNBytes(65536), StandardCharsets.UTF_8)).getAsJsonObject();
         } catch (Throwable var9) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (in != null) {
            in.close();
         }

         String title = words(answer, "title");
         if (title.isEmpty()) {
            throw new IOException("no title");
         }

         p.title = title;
         p.author = words(answer, "author_name");
         String picture = link.thumbnailUrl();
         if (picture == null) {
            picture = string(answer, "thumbnail_url");
         }

         if (picture.startsWith("https://")) {
            p.picture = new Attachment(Attachment.Kind.IMAGE_URL, Attachment.idForUrl(picture), picture, 0);
         }

         p.status = MediaCache.Status.READY;
      } catch (Exception e) {
         CFHGui.LOGGER.debug("[CFHGui] link preview failed for {}: {}", link.canonicalUrl(), e.toString());
         p.status = MediaCache.Status.FAILED;
      }

   }

   private static String string(JsonObject answer, String key) {
      JsonElement e = answer.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
   }

   private static String words(JsonObject answer, String key) {
      String plain = ChatFormatting.stripFormatting(string(answer, key));
      String line = (plain == null ? "" : plain).replaceAll("\\s+", " ").trim();
      return line.length() > 160 ? line.substring(0, 160) : line;
   }

   private LinkPreviews() {
   }

   public static final class Preview {
      private volatile MediaCache.Status status;
      private volatile String title;
      private volatile String author;
      private volatile @Nullable Attachment picture;

      public Preview() {
         this.status = MediaCache.Status.LOADING;
         this.title = "";
         this.author = "";
      }

      public String title() {
         return this.title;
      }

      public String author() {
         return this.author;
      }

      public MediaCache.@Nullable Entry picture() {
         Attachment p = this.picture;
         return p == null ? null : MediaCache.get(p);
      }

      public boolean complete() {
         if (this.status != MediaCache.Status.READY) {
            return false;
         } else {
            MediaCache.Entry e = this.picture();
            return e != null && e.ready() && !e.frames.isEmpty();
         }
      }

      public boolean wordsOnly() {
         if (this.status != MediaCache.Status.READY) {
            return false;
         } else {
            MediaCache.Entry e = this.picture();
            return e == null || e.status == MediaCache.Status.FAILED;
         }
      }
   }
}
