package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class StatFmt {
   private static String localeCode = "";
   private static Locale locale;
   private static Locale integerLocale;
   private static NumberFormat integer;
   private static Locale decimalLocale;
   private static final NumberFormat[] DECIMALS;

   public static Locale locale() {
      String code;
      try {
         code = Minecraft.getInstance().getLanguageManager().getSelected();
      } catch (RuntimeException var5) {
         code = "en_us";
      }

      if (code == null) {
         code = "en_us";
      }

      if (!code.equals(localeCode)) {
         localeCode = code;
         String[] parts = code.split("_", 2);

         Locale made;
         try {
            Locale.Builder builder = (new Locale.Builder()).setLanguage(parts[0]);
            if (parts.length == 2) {
               builder.setRegion(parts[1].toUpperCase(Locale.ROOT));
            }

            made = builder.build();
         } catch (RuntimeException var4) {
            made = Locale.ENGLISH;
         }

         locale = made;
      }

      return locale;
   }

   public static String count(long n) {
      Locale l = locale();
      if (l != integerLocale) {
         integerLocale = l;
         integer = NumberFormat.getIntegerInstance(l);
      }

      return integer.format(n);
   }

   public static String compact(long n) {
      long abs = Math.abs(n);
      if (abs < 10000L) {
         return count(n);
      } else if (abs < 1000000L) {
         return I18n.get("cfhgui.stats.compact.thousand", new Object[]{decimal((double)n / (double)1000.0F, abs < 100000L ? 1 : 0, false)});
      } else {
         return abs < 1000000000L ? I18n.get("cfhgui.stats.compact.million", new Object[]{decimal((double)n / (double)1000000.0F, abs < 100000000L ? 2 : 1, false)}) : I18n.get("cfhgui.stats.compact.billion", new Object[]{decimal((double)n / (double)1.0E9F, 2, false)});
      }
   }

   public static String decimal(double value, int digits, boolean fixed) {
      Locale l = locale();
      if (l != decimalLocale) {
         decimalLocale = l;
         Arrays.fill(DECIMALS, null);
      }

      int slot = digits * 2 + (fixed ? 1 : 0);
      boolean kept = slot >= 0 && slot < DECIMALS.length;
      NumberFormat f = kept ? DECIMALS[slot] : null;
      if (f == null) {
         f = NumberFormat.getNumberInstance(l);
         f.setMaximumFractionDigits(digits);
         f.setMinimumFractionDigits(fixed ? digits : 0);
         if (kept) {
            DECIMALS[slot] = f;
         }
      }

      return f.format(value);
   }

   public static String time(long ticks) {
      long s = Math.max(0L, ticks) / 20L;
      long min = s / 60L;
      long h = min / 60L;
      long d = h / 24L;
      long y = d / 365L;
      if (y > 0L) {
         return I18n.get("cfhgui.stats.time.yd", new Object[]{count(y), d % 365L});
      } else if (d > 0L) {
         return I18n.get("cfhgui.stats.time.dh", new Object[]{count(d), h % 24L});
      } else if (h > 0L) {
         return I18n.get("cfhgui.stats.time.hm", new Object[]{h, min % 60L});
      } else {
         return min > 0L ? I18n.get("cfhgui.stats.time.ms", new Object[]{min, s % 60L}) : I18n.get("cfhgui.stats.time.s", new Object[]{s});
      }
   }

   public static String timeExact(long ticks) {
      long s = Math.max(0L, ticks) / 20L;
      return I18n.get("cfhgui.stats.time.exact", new Object[]{count(s / 3600L), s / 60L % 60L, s % 60L, count(ticks)});
   }

   public static String distance(long cm) {
      double m = (double)Math.max(0L, cm) / (double)100.0F;
      if (m < (double)1.0F) {
         return I18n.get("cfhgui.stats.distance.cm", new Object[]{count(Math.max(0L, cm))});
      } else if (m < (double)10.0F) {
         return I18n.get("cfhgui.stats.distance.m", new Object[]{decimal(m, 1, false)});
      } else if (m < (double)1000.0F) {
         return I18n.get("cfhgui.stats.distance.m", new Object[]{count(Math.round(m))});
      } else {
         double km = m / (double)1000.0F;
         return km < (double)100.0F ? I18n.get("cfhgui.stats.distance.km", new Object[]{decimal(km, 1, true)}) : I18n.get("cfhgui.stats.distance.km", new Object[]{count(Math.round(km))});
      }
   }

   public static String distanceExact(long cm) {
      return I18n.get("cfhgui.stats.distance.exact", new Object[]{decimal((double)Math.max(0L, cm) / (double)100.0F, 2, true)});
   }

   public static String hearts(long tenths) {
      double hearts = (double)tenths / (double)20.0F;
      return Math.abs(hearts) < (double)100.0F ? decimal(hearts, 1, false) : count(Math.round(hearts));
   }

   public static Shown of(StatKind kind, long value, @Nullable ResourceLocation id) {
      switch (kind) {
         case TIME:
            return new Shown(time(value), timeExact(value), false);
         case DISTANCE:
            return new Shown(distance(value), distanceExact(value), false);
         case TENTHS:
            if (isDamage(id)) {
               return new Shown(hearts(value), I18n.get("cfhgui.stats.hearts.exact", new Object[]{decimal((double)value / (double)10.0F, 1, true), decimal((double)value / (double)20.0F, 2, false)}), true);
            }

            return new Shown(decimal((double)value / (double)10.0F, 1, true), decimal((double)value / (double)10.0F, 1, true), false);
         case RATIO:
            return new Shown(decimal((double)value / (double)100.0F, 2, true), decimal((double)value / (double)100.0F, 2, true), false);
         case OWN:
            if (id != null) {
               String own = StatKind.own(id, value);
               return new Shown(own, own, false);
            }

            return new Shown(count(value), count(value), false);
         default:
            return new Shown(count(value), count(value), false);
      }
   }

   public static Shown plain(long value) {
      String text = count(value);
      return new Shown(text, text, false);
   }

   public static Shown words(String text, String exact) {
      return new Shown(text, exact, false);
   }

   public static boolean isDamage(@Nullable ResourceLocation id) {
      return id != null && "minecraft".equals(id.getNamespace()) && id.getPath().startsWith("damage_");
   }

   private StatFmt() {
   }

   static {
      locale = Locale.ROOT;
      integerLocale = Locale.ROOT;
      integer = NumberFormat.getIntegerInstance(Locale.ROOT);
      decimalLocale = Locale.ROOT;
      DECIMALS = new NumberFormat[6];
   }

   public static record Shown(String text, String exact, boolean hearts) {
      public String spoken() {
         return this.hearts ? I18n.get("cfhgui.stats.hearts", new Object[]{this.text}) : this.text;
      }
   }
}
