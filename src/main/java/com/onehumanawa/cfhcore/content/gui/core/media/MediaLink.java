package com.onehumanawa.cfhcore.content.gui.core.media;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public record MediaLink(Provider provider, String type, String id, int startSeconds) {
   public static final String VIDEO = "video";
   private static final Set<String> SPOTIFY_TYPES = Set.of("track", "album", "playlist", "episode", "show", "artist");
   private static final Set<String> YOUTUBE_PATHS = Set.of("shorts", "live", "embed", "v");
   private static final Pattern PARTS = Pattern.compile("^https?://([^/?#\\s@:]+)(/[^?#\\s]*)?(?:\\?([^#\\s]*))?", 2);
   private static final Pattern YOUTUBE_ID = Pattern.compile("^([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])");
   private static final Pattern SPOTIFY_ID = Pattern.compile("^([A-Za-z0-9]{22})(?![A-Za-z0-9])");
   private static final Pattern START = Pattern.compile("^(?:(\\d{1,3})h)?(?:(\\d{1,5})m)?(?:(\\d{1,7})s?)?");

   public static @Nullable MediaLink of(@Nullable String url) {
      if (url == null) {
         return null;
      } else {
         Matcher m = PARTS.matcher(url);
         if (!m.find()) {
            return null;
         } else {
            String host = m.group(1).toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
               host = host.substring(4);
            } else if (host.startsWith("m.")) {
               host = host.substring(2);
            }

            String[] path = (m.group(2) == null ? "" : m.group(2)).split("/");
            String query = m.group(3) == null ? "" : m.group(3);
            switch (host) {
               case "youtu.be":
                  return youtube(segment(path, 1), query);
               case "youtube.com":
               case "music.youtube.com":
               case "youtube-nocookie.com":
                  String first = segment(path, 1).toLowerCase(Locale.ROOT);
                  if (first.equals("watch")) {
                     return youtube(param(query, "v"), query);
                  }

                  return YOUTUBE_PATHS.contains(first) ? youtube(segment(path, 2), query) : null;
               case "open.spotify.com":
                  int at = 1;
                  if (segment(path, at).toLowerCase(Locale.ROOT).startsWith("intl-")) {
                     ++at;
                  }

                  if (segment(path, at).equals("embed")) {
                     ++at;
                  }

                  String type = segment(path, at).toLowerCase(Locale.ROOT);
                  Matcher id = SPOTIFY_ID.matcher(segment(path, at + 1));
                  return SPOTIFY_TYPES.contains(type) && id.find() ? new MediaLink(MediaLink.Provider.SPOTIFY, type, id.group(1), 0) : null;
               default:
                  return null;
            }
         }
      }
   }

   private static @Nullable MediaLink youtube(String id, String query) {
      Matcher m = YOUTUBE_ID.matcher(id);
      if (!m.find()) {
         return null;
      } else {
         String start = param(query, "t");
         if (start.isEmpty()) {
            start = param(query, "start");
         }

         return new MediaLink(MediaLink.Provider.YOUTUBE, "video", m.group(1), seconds(start));
      }
   }

   private static String segment(String[] path, int index) {
      return index < path.length ? path[index] : "";
   }

   private static String param(String query, String name) {
      for(String pair : query.split("&")) {
         if (pair.startsWith(name + "=")) {
            return pair.substring(name.length() + 1);
         }
      }

      return "";
   }

   private static int seconds(String start) {
      Matcher m = START.matcher(start);
      if (!start.isEmpty() && m.find()) {
         long total = number(m.group(1)) * 3600L + number(m.group(2)) * 60L + number(m.group(3));
         return (int)Math.min(total, 2147483647L);
      } else {
         return 0;
      }
   }

   private static long number(@Nullable String digits) {
      return digits == null ? 0L : Long.parseLong(digits);
   }

   public String key() {
      String var10000 = this.provider.name();
      return var10000 + ":" + this.type + ":" + this.id;
   }

   public boolean video() {
      return this.provider == MediaLink.Provider.YOUTUBE;
   }

   public String canonicalUrl() {
      String var10000;
      switch (this.provider.ordinal()) {
         case 0 -> var10000 = "https://www.youtube.com/watch?v=" + this.id + (this.startSeconds > 0 ? "&t=" + this.startSeconds + "s" : "");
         case 1 -> var10000 = "https://open.spotify.com/" + this.type + "/" + this.id;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public String embedUrl() {
      String var10000;
      switch (this.provider.ordinal()) {
         case 0 -> var10000 = "https://www.youtube.com/embed/" + this.id + "?autoplay=1" + (this.startSeconds > 0 ? "&start=" + this.startSeconds : "");
         case 1 -> var10000 = "https://open.spotify.com/embed/" + this.type + "/" + this.id;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public String oembedUrl() {
      String var10000;
      switch (this.provider.ordinal()) {
         case 0 -> var10000 = "https://www.youtube.com/oembed?format=json&url=" + encode("https://www.youtube.com/watch?v=" + this.id);
         case 1 -> var10000 = "https://open.spotify.com/oembed?url=" + encode(this.canonicalUrl());
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public @Nullable String thumbnailUrl() {
      return this.provider == MediaLink.Provider.YOUTUBE ? "https://i.ytimg.com/vi/" + this.id + "/mqdefault.jpg" : null;
   }

   private static String encode(String url) {
      return URLEncoder.encode(url, StandardCharsets.UTF_8);
   }

   public static enum Provider {
      YOUTUBE("YouTube"),
      SPOTIFY("Spotify");

      public final String label;

      private Provider(final String label) {
         this.label = label;
      }
   }
}
