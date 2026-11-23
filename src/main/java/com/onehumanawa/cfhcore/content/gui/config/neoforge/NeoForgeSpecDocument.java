package com.onehumanawa.cfhcore.content.gui.config.neoforge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.doc.DocSection;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.Humanize;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.RestartType;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeSpecDocument implements FileDocument {
   private final ModConfig config;
   private final ModConfigSpec spec;
   private final String idPrefix;

   NeoForgeSpecDocument(ModConfig config, ModConfigSpec spec) {
      this.config = config;
      this.spec = spec;
      String var10001 = config.getModId();
      this.idPrefix = "toml:" + var10001 + ":" + config.getFileName() + ":";
   }

   public Path path() {
      try {
         Path p = this.config.getFullPath();
         if (p != null) {
            return p;
         }
      } catch (Throwable var2) {
      }

      return CFHGuiPlatform.get().configDir().resolve(this.config.getFileName());
   }

   public Component title() {
      return Component.literal(this.config.getFileName());
   }

   public String kind() {
      return "spec";
   }

   public String modId() {
      return this.config.getModId();
   }

   public void reload() {
   }

   public boolean editable() {
      return this.spec.isLoaded();
   }

   public @Nullable Component notice() {
      return this.spec.isLoaded() ? null : Component.translatable("cfhgui_config.editor.not_loaded", new Object[]{this.config.getType().name().toLowerCase(Locale.ROOT)});
   }

   public List<DocSection> sections() {
      List<DocSection> out = new ArrayList();
      if (!this.spec.isLoaded()) {
         return out;
      } else {
         try {
            this.walk(this.spec.getSpec(), this.spec.getValues(), new ArrayList(), 0, out);
         } catch (Throwable t) {
            CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot walk {}: {}", this.config.getFileName(), t.toString());
         }

         return out;
      }
   }

   private void walk(UnmodifiableConfig specNode, UnmodifiableConfig valuesNode, List<String> path, int depth, List<DocSection> out) {
      List<OptionBinding> leaves = new ArrayList();
      List<Runnable> nested = new ArrayList();

      for(UnmodifiableConfig.Entry e : specNode.entrySet()) {
         String key = e.getKey();
         Object raw = e.getRawValue();
         List<String> childPath = new ArrayList(path);
         childPath.add(key);
         if (raw instanceof UnmodifiableConfig sub) {
            Object vals = valuesNode.get(List.of(key));
            if (vals instanceof UnmodifiableConfig subVals) {
               nested.add(() -> this.walk(sub, subVals, childPath, depth + 1, out));
            }
         } else if (raw instanceof ModConfigSpec.ValueSpec vs) {
            Object cvRaw = valuesNode.get(List.of(key));
            if (cvRaw instanceof ModConfigSpec.ConfigValue<?> cv) {
               OptionBinding b = this.binding(childPath, key, vs, cv);
               if (b != null) {
                  leaves.add(b);
               }
            }
         }
      }

      Component comment = null;
      Component title;
      if (path.isEmpty()) {
         title = Component.translatable("cfhgui_config.editor.root");
      } else {
         String tk = this.spec.getLevelTranslationKey(path);
         title = tk != null && I18n.exists(tk) ? Component.translatable(tk) : Component.literal(Humanize.key((String)path.get(path.size() - 1)));
         String c = this.spec.getLevelComment(path);
         if (c != null && !c.isBlank()) {
            comment = Component.literal(c);
         }
      }

      if (!leaves.isEmpty() || depth == 0) {
         out.add(new DocSection(String.join(".", path), title, comment, depth, leaves));
      }

      for(Runnable r : nested) {
         r.run();
      }

   }

   private @Nullable OptionBinding binding(List<String> path, String key, ModConfigSpec.ValueSpec vs, ModConfigSpec.ConfigValue<?> cv) {
      String var10000 = this.idPrefix;
      String id = var10000 + String.join(".", path);
      String tk = vs.getTranslationKey();
      Component label = tk != null && I18n.exists(tk) ? Component.translatable(tk) : Component.literal(Humanize.key(key));
      Component tip = null;
      if (tk != null && I18n.exists(tk + ".tooltip")) {
         tip = Component.translatable(tk + ".tooltip");
      } else if (vs.getComment() != null && !vs.getComment().isBlank()) {
         tip = Component.literal(vs.getComment());
      }

      boolean restart = vs.restartType() != RestartType.NONE;
      ModConfigSpec.Range<?> range = safeRange(vs);
      Binding b;
      if (cv instanceof ModConfigSpec.BooleanValue bv) {
         Binding var38 = Binding.of(id, OptionType.BOOLEAN, label);
         Objects.requireNonNull(bv);
         b = var38.getter(bv::get).setter((v) -> this.write(cv, OptionValues.asBoolean(v, false)));
      } else if (cv instanceof ModConfigSpec.IntValue iv) {
         Binding var39 = Binding.of(id, OptionType.INT, label);
         Objects.requireNonNull(iv);
         b = var39.getter(iv::get).setter((v) -> this.write(cv, OptionValues.asInt(v, 0)));
         if (range != null) {
            Comparable var20 = range.getMin();
            if (var20 instanceof Number) {
               Number lo = (Number)var20;
               var20 = range.getMax();
               if (var20 instanceof Number) {
                  Number hi = (Number)var20;
                  b.range(NumberRange.of(lo.doubleValue(), hi.doubleValue(), (double)1.0F));
               }
            }
         }
      } else if (cv instanceof ModConfigSpec.LongValue lv) {
         Binding var40 = Binding.of(id, OptionType.INT, label);
         Objects.requireNonNull(lv);
         b = var40.getter(lv::get).setter((v) -> this.write(cv, OptionValues.asLong(v, 0L)));
         if (range != null) {
            Comparable var33 = range.getMin();
            if (var33 instanceof Number) {
               Number lo = (Number)var33;
               var33 = range.getMax();
               if (var33 instanceof Number) {
                  Number hi = (Number)var33;
                  b.range(NumberRange.of(lo.doubleValue(), hi.doubleValue(), (double)1.0F));
               }
            }
         }
      } else if (cv instanceof ModConfigSpec.DoubleValue dv) {
         Binding var41 = Binding.of(id, OptionType.DOUBLE, label);
         Objects.requireNonNull(dv);
         b = var41.getter(dv::get).setter((v) -> this.write(cv, OptionValues.asDouble(v, (double)0.0F)));
         if (range != null) {
            Comparable var35 = range.getMin();
            if (var35 instanceof Number) {
               Number lo = (Number)var35;
               var35 = range.getMax();
               if (var35 instanceof Number) {
                  Number hi = (Number)var35;
                  b.range(NumberRange.of(lo.doubleValue(), hi.doubleValue(), (double)0.0F));
               }
            }
         }
      } else if (cv instanceof ModConfigSpec.EnumValue<?> ev) {
         Class var42;
         if (vs.getClazz() != null && vs.getClazz().isEnum()) {
            var42 = vs.getClazz();
         } else {
            Object var37 = ev.get();
            if (var37 instanceof Enum) {
               Enum<?> en = (Enum)var37;
               var42 = en.getDeclaringClass();
            } else {
               var42 = null;
            }
         }

         Class<?> clazz = var42;
         if (clazz == null) {
            return null;
         }

         b = Binding.of(id, OptionType.CHOICE, label).choices(Choice.ofEnum(clazz)).getter(() -> {
            Object v = cv.get();
            String var43;
            if (v instanceof Enum<?> en) {
               var43 = en.name();
            } else {
               var43 = String.valueOf(v);
            }

            return var43;
         }).setter((v) -> {
            String name = OptionValues.asString(v);

            for(Object c : clazz.getEnumConstants()) {
               if (((Enum)c).name().equals(name)) {
                  this.write(cv, c);
                  return;
               }
            }

         });
      } else {
         Object cur = cv.get();
         if (cur instanceof List) {
            b = Binding.of(id, OptionType.LIST, label).getter(() -> OptionValues.asList(cv.get())).setter((v) -> {
               Object sample = sampleElement(vs, cv);
               List<Object> out = new ArrayList();

               for(String s : OptionValues.asList(v)) {
                  out.add(coerce(s, sample));
               }

               this.write(cv, out);
            });
         } else if (cur instanceof Boolean) {
            Binding var43 = Binding.of(id, OptionType.BOOLEAN, label);
            Objects.requireNonNull(cv);
            b = var43.getter(cv::get).setter((v) -> this.write(cv, OptionValues.asBoolean(v, false)));
         } else if (!(cur instanceof Integer) && !(cur instanceof Long)) {
            if (cur instanceof Number) {
               Binding var45 = Binding.of(id, OptionType.DOUBLE, label);
               Objects.requireNonNull(cv);
               b = var45.getter(cv::get).setter((v) -> this.write(cv, OptionValues.asDouble(v, (double)0.0F)));
            } else {
               label139: {
                  if (cur instanceof String) {
                     String s = (String)cur;
                     if (s.matches("#[0-9A-Fa-f]{6}") || s.matches("#[0-9A-Fa-f]{8}")) {
                        b = Binding.of(id, OptionType.COLOR, label).getter(() -> OptionValues.asColor(cv.get(), -1)).setter((v) -> this.write(cv, Colors.toHex(OptionValues.asColor(v, -1))));
                        break label139;
                     }
                  }

                  if (cur instanceof String) {
                     b = Binding.of(id, OptionType.STRING, label).getter(() -> OptionValues.asString(cv.get())).setter((v) -> this.write(cv, OptionValues.asString(v)));
                  } else {
                     b = Binding.of(id, OptionType.INFO, label).getter(() -> String.valueOf(cv.get()));
                  }
               }
            }
         } else {
            Binding var44 = Binding.of(id, OptionType.INT, label);
            Objects.requireNonNull(cv);
            b = var44.getter(cv::get).setter((v) -> this.write(cv, cur instanceof Integer ? OptionValues.asInt(v, 0) : OptionValues.asLong(v, 0L)));
         }
      }

      Object def = null;

      try {
         def = cv.getDefault();
      } catch (Throwable var21) {
      }

      if (def instanceof Enum<?> en) {
         def = en.name();
      }

      Binding var46 = b.tooltip(tip).def(def).restart(restart);
      String var10001 = this.config.getFileName();
      return var46.searchWords(var10001 + " " + this.config.getModId() + " " + String.join(" ", path));
   }

   private static ModConfigSpec.@Nullable Range<?> safeRange(ModConfigSpec.ValueSpec vs) {
      try {
         return vs.getRange();
      } catch (Throwable var2) {
         return null;
      }
   }

   private static @Nullable Object sampleElement(ModConfigSpec.ValueSpec vs, ModConfigSpec.ConfigValue<?> cv) {
      try {
         Object cur = cv.get();
         if (cur instanceof List<?> l) {
            if (!l.isEmpty()) {
               return l.get(0);
            }
         }

         Object def = cv.getDefault();
         if (def instanceof List<?> l) {
            if (!l.isEmpty()) {
               return l.get(0);
            }
         }

         if (vs instanceof ModConfigSpec.ListValueSpec lvs) {
            if (lvs.getNewElementSupplier() != null) {
               return lvs.getNewElementSupplier().get();
            }
         }
      } catch (Throwable var5) {
      }

      return null;
   }

   private static Object coerce(String s, @Nullable Object sample) {
      try {
         if (sample instanceof Boolean) {
            return Boolean.parseBoolean(s.trim());
         }

         if (sample instanceof Integer) {
            return Integer.parseInt(s.trim());
         }

         if (sample instanceof Long) {
            return Long.parseLong(s.trim());
         }

         if (sample instanceof Double || sample instanceof Float) {
            return Double.parseDouble(s.trim());
         }

         if (sample instanceof Enum<?> en) {
            for(Object c : en.getDeclaringClass().getEnumConstants()) {
               if (((Enum)c).name().equalsIgnoreCase(s.trim())) {
                  return c;
               }
            }
         }
      } catch (NumberFormatException var7) {
      }

      return s;
   }

   private void write(ModConfigSpec.ConfigValue raw, Object value) {
      raw.set(value);

      try {
         this.spec.save();
      } catch (Throwable t) {
         CFHGuiConfig.LOGGER.warn("[CFHGui Config] {} could not be saved: {}", this.config.getFileName(), t.toString());
      }

   }
}
