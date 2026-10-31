package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.ToDoubleFunction;

public final class StartupTimeline {
   public static final String FILE = "earlywindow-times.properties";
   public static final String WINDOW = "window";
   public static final String HANDOVER = "handover";
   public static final String RELOAD = "reload";
   public static final String END = "end";
   private static final String MARK = "cfhgui.startup.mark.";
   private static final String SAVED = "cfhgui.startup.saved";
   private static final double NEWEST = (double)0.5F;
   private static final double CREEP = (double)0.25F;
   private static final double EASE = 0.12;
   private static final double CAP = 0.99;
   private static final double CLOSE = 0.015;
   private static final Map<String, Double> TYPICAL = Map.of("window", 0.03, "handover", 0.18, "reload", 0.6);
   private static final long TYPICAL_END = 60000L;
   private final String[] keys;
   private final double[] expected;
   private final double end;
   private final boolean learnt;
   private int at = -2;
   private double anchorX;
   private double anchorP;
   private double stretch = (double)1.0F;
   private double x;
   private long last = -1L;
   private long ended = -1L;
   private double endFrom;
   private float shown;

   private StartupTimeline(Map<String, Long> stages, long end, boolean learnt) {
      List<Map.Entry<String, Long>> sorted = new ArrayList();

      for(Map.Entry<String, Long> e : stages.entrySet()) {
         if ((Long)e.getValue() > 0L && (Long)e.getValue() < end && !"end".equals(e.getKey())) {
            sorted.add(e);
         }
      }

      sorted.sort(Entry.comparingByValue());
      List<Map.Entry<String, Long>> kept = new ArrayList();
      long before = 0L;

      for(Map.Entry<String, Long> e : sorted) {
         boolean main = "window".equals(e.getKey()) || "handover".equals(e.getKey()) || "reload".equals(e.getKey());
         if (main || !((double)((Long)e.getValue() - before) < 0.015 * (double)end)) {
            kept.add(e);
            before = (Long)e.getValue();
         }
      }

      this.keys = new String[kept.size()];
      this.expected = new double[kept.size()];

      for(int i = 0; i < this.keys.length; ++i) {
         this.keys[i] = (String)((Map.Entry)kept.get(i)).getKey();
         this.expected[i] = (double)(Long)((Map.Entry)kept.get(i)).getValue();
      }

      this.end = (double)end;
      this.learnt = learnt;
   }

   public static StartupTimeline read(java.nio.file.Path dir) {
      Properties p = load(dir);
      Map<String, Long> stages = new HashMap();
      long end = -1L;

      for(String name : p.stringPropertyNames()) {
         if (name.startsWith("stage.")) {
            long v = number(p.getProperty(name));
            if (v > 0L) {
               stages.put(name.substring(6), v);
            }
         }
      }

      end = number(p.getProperty("end"));
      if (end <= 0L) {
         long early = number(p.getProperty("early"));
         long game = number(p.getProperty("game"));
         if (early >= 500L && game >= 500L) {
            long window = Math.max(1000L, Math.round((double)early * (Double)TYPICAL.get("window")));
            stages.clear();
            stages.put("window", window);
            stages.put("reload", window + early);
            end = window + early + Math.max(game, Math.round((double)(window + early) * 0.6));
            if (end <= 3600000L) {
               return new StartupTimeline(stages, end, false);
            }
         }
      }

      if (end >= 1000L && end <= 3600000L && !stages.isEmpty()) {
         return new StartupTimeline(stages, end, true);
      } else {
         Map<String, Long> typical = new HashMap();
         TYPICAL.forEach((k, share) -> typical.put(k, Math.round(share * (double)60000.0F)));
         return new StartupTimeline(typical, 60000L, false);
      }
   }

   public boolean learnt() {
      return this.learnt;
   }

   public float progress() {
      return this.progress(StartupClock.elapsedMs(), (ToDoubleFunction)null);
   }

   public float progress(long now, ToDoubleFunction<String> counters) {
      if (this.at == -2) {
         this.start(now);
      }

      this.anchorNewest();
      this.advance(Math.max(0L, now - this.last));
      this.last = now;
      if (!this.learnt && counters != null && this.at >= 0) {
         double count = counters.applyAsDouble(this.keys[this.at]);
         if (count > (double)0.0F) {
            this.x = Math.max(this.x, this.anchorX + Math.min((double)1.0F, count) * 0.9 * (this.next() - this.anchorX));
         }
      }

      double p = this.anchorP + ((double)1.0F - this.anchorP) * clamp((this.x - this.anchorX) / Math.max((double)1.0F, this.end - this.anchorX));
      if (marked("end") >= 0L) {
         if (this.ended < 0L) {
            this.ended = now;
            this.endFrom = Math.max((double)this.shown, Math.min(p, 0.99));
         }

         p = (double)1.0F - ((double)1.0F - this.endFrom) * Math.exp((double)(-(now - this.ended)) / (double)150.0F);
         if (p > 0.999) {
            p = (double)1.0F;
         }
      } else {
         p = Math.min(p, 0.99);
      }

      this.shown = Math.max(this.shown, (float)p);
      return this.shown;
   }

   public boolean ended() {
      return marked("end") >= 0L;
   }

   public static int percent(float progress, boolean over) {
      int pct = (int)Math.floor((double)(Math.max(0.0F, Math.min(1.0F, progress)) * 100.0F + 0.5F));
      return over ? pct : Math.min(99, pct);
   }

   private void start(long now) {
      int best = -1;
      long began = 0L;

      for(int i = 0; i < this.keys.length; ++i) {
         long a = marked(this.keys[i]);
         if (a >= 0L) {
            best = i;
            began = a;
         }
      }

      this.at = best;
      if (best < 0) {
         this.anchorX = (double)0.0F;
         this.anchorP = (double)0.0F;
         this.stretch = (double)1.0F;
         this.x = (double)0.0F;
      } else {
         this.anchorX = this.expected[best];
         this.stretch = this.pace(began, this.anchorX);
         this.anchorP = clamp((double)began / ((double)began + (this.end - this.anchorX) * this.stretch));
         this.x = this.anchorX;
      }

      this.advance(Math.max(0L, now - began));
      this.last = now;
   }

   private void anchorNewest() {
      for(int i = this.keys.length - 1; i > this.at; --i) {
         long a = marked(this.keys[i]);
         if (a >= 0L) {
            this.anchorP = clamp(this.anchorP + ((double)1.0F - this.anchorP) * clamp((this.x - this.anchorX) / Math.max((double)1.0F, this.end - this.anchorX)));
            this.at = i;
            this.anchorX = this.expected[i];
            this.stretch = this.pace(a, this.anchorX);
            this.x = this.anchorX;
            return;
         }
      }

   }

   private double pace(long began, double expectedAt) {
      if (!(expectedAt < (double)1.0F) && began >= 1L) {
         double ratio = (double)began / expectedAt;
         if (!this.learnt) {
            return Math.max(0.15, Math.min((double)25.0F, ratio));
         } else {
            double trust = 0.3 + 0.6 * Math.min((double)1.0F, expectedAt / this.end);
            return (double)1.0F + trust * (Math.max(0.4, Math.min((double)3.0F, ratio)) - (double)1.0F);
         }
      } else {
         return (double)1.0F;
      }
   }

   private void advance(long ms) {
      while(ms > 0L) {
         long step = Math.min(ms, 25L);
         ms -= step;
         this.x = Math.min(this.end, this.x + (double)step * this.speed() / this.stretch);
      }

   }

   private double next() {
      return this.at + 1 < this.keys.length ? this.expected[this.at + 1] : this.end;
   }

   private double speed() {
      double next = this.next();
      double toEnd = clamp((this.end - this.x) / Math.max((double)1.0F, 0.12 * (this.end - this.anchorX)));
      double nearEnd = toEnd * toEnd * toEnd;
      if (this.x >= next) {
         return Math.min((double)0.25F, nearEnd);
      } else if (next >= this.end) {
         return nearEnd;
      } else {
         double left = (next - this.x) / Math.max((double)1.0F, next - this.anchorX);
         double slowing = left >= 0.12 ? (double)1.0F : (double)0.25F + (double)0.75F * left / 0.12;
         return Math.min(slowing, nearEnd);
      }
   }

   public static boolean mark(String stage) {
      return mark(stage, StartupClock.elapsedMs());
   }

   public static synchronized boolean mark(String stage, long ms) {
      String key = "cfhgui.startup.mark." + stage;
      if (System.getProperty(key) != null) {
         return false;
      } else {
         System.setProperty(key, Long.toString(Math.max(0L, ms)));
         return true;
      }
   }

   public static long marked(String stage) {
      String v = System.getProperty("cfhgui.startup.mark." + stage);
      if (v == null) {
         return -1L;
      } else {
         long n = number(v);
         return n >= 0L ? n : -1L;
      }
   }

   public static String key(String kind, String text) {
      StringBuilder b = (new StringBuilder(kind)).append(':');
      boolean gap = false;
      boolean digits = false;

      for(char c : (text == null ? "" : text.trim().toLowerCase(Locale.ROOT)).toCharArray()) {
         if (c >= 'a' && c <= 'z') {
            if (gap && b.charAt(b.length() - 1) != ':') {
               b.append('_');
            }

            b.append(c);
            digits = false;
            gap = false;
         } else if (c >= '0' && c <= '9') {
            if (!digits) {
               if (gap && b.charAt(b.length() - 1) != ':') {
                  b.append('_');
               }

               b.append('#');
            }

            digits = true;
            gap = false;
         } else {
            gap = true;
            digits = false;
         }

         if (b.length() >= kind.length() + 49) {
            break;
         }
      }

      return b.toString();
   }

   public static String save(java.nio.file.Path dir) {
      long end;
      synchronized(StartupTimeline.class) {
         end = marked("end");
         if (end < 1000L || System.getProperty("cfhgui.startup.saved") != null) {
            return null;
         }

         System.setProperty("cfhgui.startup.saved", "true");
      }

      Map<String, Long> now = new TreeMap();

      for(String name : System.getProperties().stringPropertyNames()) {
         if (name.startsWith("cfhgui.startup.mark.")) {
            String stage = name.substring("cfhgui.startup.mark.".length());
            long v = marked(stage);
            if (v > 0L && v < end && !"end".equals(stage) && stage.matches("[a-z0-9_:#.]+")) {
               now.put(stage, v);
            }
         }
      }

      Properties old = load(dir);
      long endAt = blend(number(old.getProperty("end")), end);
      Map<String, Long> stages = new LinkedHashMap();

      for(Map.Entry<String, Long> e : now.entrySet()) {
         long before = number(old.getProperty("stage." + (String)e.getKey()));
         stages.put((String)e.getKey(), before > 0L ? blend(before, (Long)e.getValue()) : Math.round((double)(Long)e.getValue() * ((double)endAt / (double)end)));
      }

      if (!now.containsKey("window")) {
         for(String name : old.stringPropertyNames()) {
            long v = number(old.getProperty(name));
            if (name.startsWith("stage.") && v > 0L && v < endAt) {
               stages.putIfAbsent(name.substring(6), v);
            }
         }
      }

      long runs = Math.max(0L, number(old.getProperty("runs"))) + 1L;
      StringBuilder out = (new StringBuilder()).append("# When each stage of the game's start began, in milliseconds from the process start: an average over the\n").append("# launches of this game folder (the newest counts half). Written by CFHGui when a start is over; its start-up\n").append("# window and loading screen show the next start against it. Delete the file to start the history again.\n").append("runs=").append(runs).append('\n').append("end").append('=').append(endAt).append('\n');
      stages.entrySet().stream().sorted(Entry.comparingByValue()).forEach((ex) -> out.append("stage.").append((String)ex.getKey()).append('=').append(ex.getValue()).append('\n'));
      long later = number(old.getProperty("laterReload"));
      if (later > 0L) {
         out.append("laterReload=").append(later).append('\n');
      }

      Long window = (Long)stages.get("window");
      Long reload = (Long)stages.get("reload");
      if (reload != null) {
         out.append("early=").append(Math.max(500L, reload - (window != null ? window : 0L))).append('\n').append("game=").append(Math.max(500L, endAt - reload)).append('\n');
      }

      return write(dir, out.toString());
   }

   public static long expectedReload(java.nio.file.Path dir) {
      long v = number(load(dir).getProperty("laterReload"));
      return v >= 200L && v <= 3600000L ? v : -1L;
   }

   public static String saveReload(java.nio.file.Path dir, long ms) {
      if (ms >= 200L && ms <= 3600000L) {
         Properties old = load(dir);
         long v = blend(number(old.getProperty("laterReload")), ms);
         StringBuilder out = new StringBuilder();

         try {
            BufferedReader in = Files.newBufferedReader(dir.resolve("earlywindow-times.properties"), StandardCharsets.UTF_8);

            String line;
            try {
               while((line = in.readLine()) != null) {
                  if (!line.startsWith("laterReload=")) {
                     out.append(line).append('\n');
                  }
               }
            } catch (Throwable var11) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (in != null) {
               in.close();
            }
         } catch (IOException var12) {
         }

         out.append("laterReload=").append(v).append('\n');
         return write(dir, out.toString());
      } else {
         return null;
      }
   }

   private static long blend(long old, long sample) {
      return old > 0L ? Math.round((double)old + (double)0.5F * (double)(sample - old)) : sample;
   }

   private static Properties load(java.nio.file.Path dir) {
      Properties p = new Properties();

      try {
         BufferedReader in = Files.newBufferedReader(dir.resolve("earlywindow-times.properties"), StandardCharsets.UTF_8);

         try {
            p.load(in);
         } catch (Throwable var6) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (in != null) {
            in.close();
         }
      } catch (RuntimeException | IOException var7) {
      }

      return p;
   }

   private static String write(java.nio.file.Path dir, String text) {
      try {
         Files.createDirectories(dir);
         java.nio.file.Path tmp = dir.resolve("earlywindow-times.properties.tmp");
         Files.writeString(tmp, text, StandardCharsets.UTF_8);

         try {
            Files.move(tmp, dir.resolve("earlywindow-times.properties"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var4) {
            Files.move(tmp, dir.resolve("earlywindow-times.properties"), StandardCopyOption.REPLACE_EXISTING);
         }

         return null;
      } catch (RuntimeException | IOException e) {
         return ((Exception)e).toString();
      }
   }

   private static long number(String v) {
      if (v == null) {
         return -1L;
      } else {
         try {
            return Long.parseLong(v.trim());
         } catch (NumberFormatException var2) {
            return -1L;
         }
      }
   }

   private static double clamp(double v) {
      return Math.max((double)0.0F, Math.min((double)1.0F, v));
   }
}
