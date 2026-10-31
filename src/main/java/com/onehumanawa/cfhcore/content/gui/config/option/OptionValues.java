package com.onehumanawa.cfhcore.content.gui.config.option;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class OptionValues {
   public static @Nullable Object normalize(@Nullable Object v) {
      if (v == null) {
         return null;
      } else if (v instanceof Number) {
         Number n = (Number)v;
         return n.doubleValue();
      } else if (v instanceof Enum) {
         Enum<?> e = (Enum)v;
         return e.name();
      } else if (!(v instanceof List)) {
         return v;
      } else {
         List<?> l = (List)v;
         List<String> out = new ArrayList(l.size());

         for(Object o : l) {
            out.add(String.valueOf(o));
         }

         return out;
      }
   }

   public static boolean asBoolean(@Nullable Object v, boolean fallback) {
      if (v instanceof Boolean b) {
         return b;
      } else if (v instanceof Number n) {
         return n.doubleValue() != (double)0.0F;
      } else {
         if (v instanceof String s) {
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equals("1")) {
               return true;
            }

            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off") || s.equals("0")) {
               return false;
            }
         }

         return fallback;
      }
   }

   public static double asDouble(@Nullable Object v, double fallback) {
      if (v instanceof Number n) {
         return n.doubleValue();
      } else if (v instanceof Boolean b) {
         return b ? (double)1.0F : (double)0.0F;
      } else {
         if (v instanceof String s) {
            try {
               return Double.parseDouble(s.trim());
            } catch (NumberFormatException var5) {
            }
         }

         return fallback;
      }
   }

   public static int asInt(@Nullable Object v, int fallback) {
      if (v instanceof Number n) {
         return n.intValue();
      } else if (v instanceof Boolean b) {
         return b ? 1 : 0;
      } else {
         if (v instanceof String s) {
            try {
               return (int)Math.round(Double.parseDouble(s.trim()));
            } catch (NumberFormatException var4) {
            }
         }

         return fallback;
      }
   }

   public static long asLong(@Nullable Object v, long fallback) {
      if (v instanceof Number n) {
         return n.longValue();
      } else {
         if (v instanceof String s) {
            try {
               return Math.round(Double.parseDouble(s.trim()));
            } catch (NumberFormatException var5) {
            }
         }

         return fallback;
      }
   }

   public static String asString(@Nullable Object v) {
      if (v == null) {
         return "";
      } else if (v instanceof Enum) {
         Enum<?> e = (Enum)v;
         return e.name();
      } else {
         if (v instanceof Double) {
            Double d = (Double)v;
            if (d == Math.rint(d) && Math.abs(d) < 1.0E15) {
               return Long.toString(d.longValue());
            }
         }

         return String.valueOf(v);
      }
   }

   public static List<String> asList(@Nullable Object v) {
      List<String> out = new ArrayList();
      if (v instanceof List) {
         for(Object o : (List)v) {
            out.add(String.valueOf(o));
         }
      } else if (v instanceof Object[]) {
         Object[] arr = (Object[])v;

         for(Object o : arr) {
            out.add(String.valueOf(o));
         }
      } else if (v instanceof String) {
         String s = (String)v;
         if (!s.isEmpty()) {
            for(String p : s.split("\\s*,\\s*")) {
               if (!p.isEmpty()) {
                  out.add(p);
               }
            }
         }
      }

      return out;
   }

   public static int asColor(@Nullable Object v, int fallback) {
      if (v instanceof Number n) {
         return n.intValue() | -16777216;
      } else {
         if (v instanceof String s) {
            int c = Colors.fromHex(s, Integer.MIN_VALUE);
            if (c != Integer.MIN_VALUE) {
               return c;
            }

            try {
               return (int)Long.parseLong(s.trim()) | -16777216;
            } catch (NumberFormatException var5) {
            }
         }

         return fallback;
      }
   }

   public static Component defaultText(OptionBinding b, @Nullable Object v) {
      if (v == null) {
         return Component.literal("-");
      } else {
         switch (b.type()) {
            case BOOLEAN:
               return Component.translatable(asBoolean(v, false) ? "options.on" : "options.off");
            case CHOICE:
               String id = asString(v);

               for(Choice c : b.choices()) {
                  if (c.id().equals(id)) {
                     return c.label();
                  }
               }

               return Component.literal(Humanize.enumName(id));
            case INT:
               return Component.literal(Long.toString(asLong(v, 0L)));
            case DOUBLE:
               return Component.literal(formatDouble(asDouble(v, (double)0.0F)));
            case COLOR:
               return Component.literal(Colors.toHex(asColor(v, 0)));
            case LIST:
               return Component.literal(asList(v).size() + " items");
            default:
               return Component.literal(asString(v));
         }
      }
   }

   public static String formatDouble(double d) {
      if (d == Math.rint(d) && Math.abs(d) < (double)1.0E9F) {
         return String.format(Locale.ROOT, "%.1f", d);
      } else {
         String s = String.format(Locale.ROOT, "%.3f", d);
         return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", ".0") : s;
      }
   }

   public static JsonElement toJson(OptionBinding b, @Nullable Object v) {
      if (v == null) {
         return JsonNull.INSTANCE;
      } else {
         Object var10000;
         switch (b.type()) {
            case BOOLEAN:
               var10000 = new JsonPrimitive(asBoolean(v, false));
               break;
            case CHOICE:
            default:
               var10000 = new JsonPrimitive(asString(v));
               break;
            case INT:
               var10000 = new JsonPrimitive(asLong(v, 0L));
               break;
            case DOUBLE:
               var10000 = new JsonPrimitive(asDouble(v, (double)0.0F));
               break;
            case COLOR:
               var10000 = new JsonPrimitive(Colors.toHex(asColor(v, 0)));
               break;
            case LIST:
               JsonArray arr = new JsonArray();

               for(String s : asList(v)) {
                  arr.add(s);
               }

               var10000 = arr;
         }

         return (JsonElement)var10000;
      }
   }

   public static @Nullable Object fromJson(OptionBinding b, @Nullable JsonElement e) {
      if (e != null && !e.isJsonNull()) {
         Object var10000;
         switch (b.type()) {
            case BOOLEAN:
               var10000 = e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : asBoolean(e.getAsString(), false);
               break;
            case CHOICE:
            default:
               var10000 = e.isJsonPrimitive() ? e.getAsString() : e.toString();
               break;
            case INT:
               var10000 = e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsNumber().longValue() : asLong(e.getAsString(), 0L);
               break;
            case DOUBLE:
               var10000 = e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsDouble() : asDouble(e.getAsString(), (double)0.0F);
               break;
            case COLOR:
               var10000 = asColor(e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : e.getAsString(), -1);
               break;
            case LIST:
               List<String> out = new ArrayList();
               if (e.isJsonArray()) {
                  for(JsonElement x : e.getAsJsonArray()) {
                     out.add(x.isJsonPrimitive() ? x.getAsString() : x.toString());
                  }
               } else {
                  out.addAll(asList(e.getAsString()));
               }

               var10000 = out;
         }

         return var10000;
      } else {
         return null;
      }
   }

   private OptionValues() {
   }
}
