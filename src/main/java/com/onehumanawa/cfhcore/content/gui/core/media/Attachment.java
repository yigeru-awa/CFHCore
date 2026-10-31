package com.onehumanawa.cfhcore.content.gui.core.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public record Attachment(Kind kind, String id, String url, int durationMs, String name, @Nullable MediaLink link) {
   public static volatile boolean embedLinks = true;
   public static volatile int thumbRows = 6;
   public static final int MAX_PER_MESSAGE = 3;
   public static final int MAX_NAME = 40;
   private static final Pattern TOKEN = Pattern.compile("\\[(voice|image)(?: (\\d+):(\\d{2}))? #([0-9a-f]{8})\\]");
   private static final Pattern FILE_TOKEN = Pattern.compile("\\[file ([^\\[\\]#]{1,60}?) #([0-9a-f]{8})\\]");
   private static final Pattern IMAGE_URL = Pattern.compile("https?://\\S+?\\.(?:png|jpe?g|gif|webp)(?:\\?\\S*)?(?=\\s|$)", 2);
   private static final Pattern VIDEO_FILE = Pattern.compile("https?://\\S+?\\.(?:mp4|webm|mov)(?:\\?\\S*)?", 2);
   private static final Pattern ANY_URL = Pattern.compile("https?://[^\\s<>\"']+", 2);

   public Attachment(Kind kind, String id, String url, int durationMs) {
      this(kind, id, url, durationMs, "", (MediaLink)null);
   }

   public Attachment(Kind kind, String id, String url, int durationMs, String name) {
      this(kind, id, url, durationMs, name, (MediaLink)null);
   }

   public int rows() {
      int var10000;
      switch (this.kind.ordinal()) {
         case 0:
            var10000 = 2;
            break;
         case 1:
         case 2:
            var10000 = Math.max(3, Math.min(12, thumbRows));
            break;
         case 3:
            var10000 = 3;
            break;
         case 4:
            var10000 = 2;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public boolean isGif() {
      return this.url != null && this.url.toLowerCase(Locale.ROOT).contains(".gif");
   }

   public boolean isBlob() {
      return this.kind == Attachment.Kind.VOICE || this.kind == Attachment.Kind.IMAGE_BLOB || this.kind == Attachment.Kind.FILE_BLOB;
   }

   public boolean isImage() {
      return this.kind == Attachment.Kind.IMAGE_BLOB || this.kind == Attachment.Kind.IMAGE_URL;
   }

   public static List<Attachment> parse(String message) {
      return parse(message, embedLinks);
   }

   public static List<Attachment> parse(String message, boolean links) {
      List<Attachment> out = new ArrayList(2);
      if (message != null && !message.isEmpty()) {
         boolean voice;
         int duration;
         for(Matcher t = TOKEN.matcher(message); t.find() && out.size() < 3; out.add(new Attachment(voice ? Attachment.Kind.VOICE : Attachment.Kind.IMAGE_BLOB, t.group(4), (String)null, duration))) {
            voice = t.group(1).equals("voice");
            duration = 0;
            if (t.group(2) != null) {
               try {
                  duration = (Integer.parseInt(t.group(2)) * 60 + Integer.parseInt(t.group(3))) * 1000;
               } catch (NumberFormatException var7) {
               }
            }
         }

         Matcher f = FILE_TOKEN.matcher(message);

         while(f.find() && out.size() < 3) {
            out.add(new Attachment(Attachment.Kind.FILE_BLOB, f.group(2), (String)null, 0, f.group(1).trim()));
         }

         if (links) {
            Matcher img = IMAGE_URL.matcher(message);

            while(img.find() && out.size() < 3) {
               out.add(new Attachment(Attachment.Kind.IMAGE_URL, idForUrl(img.group()), img.group(), 0));
            }
         }

         Matcher url = ANY_URL.matcher(message);

         while(url.find() && out.size() < 3) {
            MediaLink link = MediaLink.of(url.group());
            if (link != null || VIDEO_FILE.matcher(url.group()).matches()) {
               out.add(new Attachment(Attachment.Kind.VIDEO_URL, idForUrl(url.group()), url.group(), 0, "", link));
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static String token(Kind kind, String id, int durationMs) {
      return token(kind, id, durationMs, "");
   }

   public static String token(Kind kind, String id, int durationMs, String name) {
      switch (kind.ordinal()) {
         case 0:
            int totalSec = Math.round((float)durationMs / 1000.0F);
            return "[voice %d:%02d #%s]".formatted(totalSec / 60, totalSec % 60, id);
         case 4:
            return "[file %s #%s]".formatted(safeName(name), id);
         default:
            return "[image #%s]".formatted(id);
      }
   }

   public static String safeName(String name) {
      String n = name == null ? "file" : name.replaceAll("[\\[\\]#\\r\\n\\t]", "_").trim();
      if (n.isEmpty()) {
         n = "file";
      }

      if (n.length() > 40) {
         int dot = n.lastIndexOf(46);
         String ext = dot > 0 && n.length() - dot <= 8 ? n.substring(dot) : "";
         String var10000 = n.substring(0, Math.max(1, 40 - ext.length() - 1));
         n = var10000 + "~" + ext;
      }

      return n;
   }

   public static String idForUrl(String url) {
      return "%08x".formatted(url.hashCode());
   }

   public static List<String> links(String text) {
      List<String> out = new ArrayList(1);
      if (text == null) {
         return out;
      } else {
         Matcher m = ANY_URL.matcher(text);

         while(m.find()) {
            out.add(m.group());
         }

         return out;
      }
   }

   public static boolean hasAny(String text) {
      return text != null && (TOKEN.matcher(text).find() || FILE_TOKEN.matcher(text).find() || ANY_URL.matcher(text).find());
   }

   public static enum Kind {
      VOICE,
      IMAGE_BLOB,
      IMAGE_URL,
      VIDEO_URL,
      FILE_BLOB;
   }
}
