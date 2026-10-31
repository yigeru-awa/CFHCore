package com.onehumanawa.cfhcore.content.gui.menu.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.minecraft.network.chat.Component;

public final class Fmt {
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
   private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("d MMM yyyy");

   public static Component ago(long epochMs) {
      if (epochMs <= 0L) {
         return Component.translatable("cfhgui.time.never");
      } else {
         long d = Math.max(0L, System.currentTimeMillis() - epochMs);
         long s = d / 1000L;
         long m = s / 60L;
         long h = m / 60L;
         long days = h / 24L;
         if (s < 45L) {
            return Component.translatable("cfhgui.time.just_now");
         } else if (m < 60L) {
            return Component.translatable("cfhgui.time.minutes_ago", new Object[]{m});
         } else if (h < 24L) {
            return Component.translatable("cfhgui.time.hours_ago", new Object[]{h});
         } else if (days < 7L) {
            return Component.translatable("cfhgui.time.days_ago", new Object[]{days});
         } else {
            return days < 30L ? Component.translatable("cfhgui.time.weeks_ago", new Object[]{days / 7L}) : Component.literal(DATE_SHORT.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())));
         }
      }
   }

   public static String date(long epochMs) {
      return epochMs <= 0L ? "-" : DATE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
   }

   public static String duration(long ms) {
      long s = Math.max(0L, ms / 1000L);
      long m = s / 60L;
      long h = m / 60L;
      if (h > 0L) {
         return "%dh %02dm".formatted(h, m % 60L);
      } else {
         return m > 0L ? "%dm %02ds".formatted(m, s % 60L) : s + "s";
      }
   }

   public static String bytes(long bytes) {
      if (bytes < 0L) {
         return "?";
      } else if (bytes < 1024L) {
         return bytes + " B";
      } else {
         double kb = (double)bytes / (double)1024.0F;
         if (kb < (double)1024.0F) {
            return "%.0f KB".formatted(kb);
         } else {
            double mb = kb / (double)1024.0F;
            return mb < (double)1024.0F ? "%.1f MB".formatted(mb) : "%.2f GB".formatted(mb / (double)1024.0F);
         }
      }
   }

   public static int pingBars(long ping) {
      if (ping < 0L) {
         return 0;
      } else if (ping < 150L) {
         return 5;
      } else if (ping < 300L) {
         return 4;
      } else if (ping < 600L) {
         return 3;
      } else {
         return ping < 1000L ? 2 : 1;
      }
   }

   private Fmt() {
   }
}
