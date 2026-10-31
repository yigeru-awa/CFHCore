package com.onehumanawa.cfhcore.content.gui.config.doc;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.file.TextFile;
import com.onehumanawa.cfhcore.content.gui.config.file.TomlLines;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.Humanize;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TomlDocument implements FileDocument {
   private final Path path;
   private final String modId;
   private final String idPrefix;
   private TomlLines toml = TomlLines.parse("");
   private long loadedMtime;
   private @Nullable Component notice;
   private final Map<String, OptionBinding> bindings = new LinkedHashMap();

   public TomlDocument(Path path, String modId, String fileRelativeToConfig) {
      this.path = path;
      this.modId = modId == null ? "" : modId;
      String var10001 = this.modId;
      this.idPrefix = "toml:" + var10001 + ":" + fileRelativeToConfig.replace('\\', '/') + ":";
      this.reload();
   }

   public Path path() {
      return this.path;
   }

   public Component title() {
      return Component.literal(this.path.getFileName().toString());
   }

   public String kind() {
      return "toml";
   }

   public String modId() {
      return this.modId;
   }

   public @Nullable Component notice() {
      return this.notice;
   }

   public boolean editable() {
      return this.notice == null;
   }

   public TomlLines lines() {
      return this.toml;
   }

   public void ensureFresh() {
      if (TextFile.mtime(this.path) != this.loadedMtime) {
         this.reload();
      }

   }

   public void reload() {
      this.bindings.clear();

      try {
         this.toml = Files.isRegularFile(this.path, new LinkOption[0]) ? TomlLines.parse(TextFile.read(this.path)) : TomlLines.parse("");
         this.loadedMtime = TextFile.mtime(this.path);
         this.notice = Files.isRegularFile(this.path, new LinkOption[0]) ? null : Component.translatable("cfhgui_config.editor.missing_file");
      } catch (Exception e) {
         CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot read {}: {}", this.path, e.toString());
         this.notice = Component.translatable("cfhgui_config.editor.parse_error", new Object[]{e.getMessage() == null ? e.toString() : e.getMessage()});
      }

   }

   private void save() {
      try {
         TextFile.writeAtomic(this.path, this.toml.render());
         this.loadedMtime = TextFile.mtime(this.path);
      } catch (IOException e) {
         CFHGuiConfig.LOGGER.error("[CFHGui Config] cannot write {}", this.path, e);
      }

   }

   public Optional<OptionBinding> binding(String dottedPath) {
      this.ensureFresh();
      OptionBinding cached = (OptionBinding)this.bindings.get(dottedPath);
      return cached != null ? Optional.of(cached) : this.toml.find(dottedPath).map((e) -> {
         OptionBinding b = this.build(e);
         this.bindings.put(dottedPath, b);
         return b;
      });
   }

   public List<DocSection> sections() {
      this.ensureFresh();
      Map<String, List<OptionBinding>> bySection = new LinkedHashMap();
      Map<String, TomlLines.Section> headers = new LinkedHashMap();
      bySection.put("", new ArrayList());

      for(TomlLines.Section s : this.toml.sections()) {
         headers.put(s.path(), s);
         bySection.computeIfAbsent(s.path(), (k) -> new ArrayList());
      }

      for(TomlLines.Entry e : this.toml.entries()) {
         ((List)bySection.computeIfAbsent(e.section, (k) -> new ArrayList())).add((OptionBinding)this.bindings.computeIfAbsent(e.path(), (k) -> this.build(e)));
      }

      List<DocSection> out = new ArrayList();

      for(Map.Entry<String, List<OptionBinding>> en : bySection.entrySet()) {
         String p = (String)en.getKey();
         if (!p.isEmpty() || !((List)en.getValue()).isEmpty()) {
            TomlLines.Section h = (TomlLines.Section)headers.get(p);
            String last = p.contains(".") ? p.substring(p.lastIndexOf(46) + 1) : p;
            Component title = p.isEmpty() ? Component.translatable("cfhgui_config.editor.root") : Component.literal(Humanize.key(last));
            Component comment = h != null && !h.comments().isEmpty() ? Component.literal(String.join("\n", h.comments())) : null;
            out.add(new DocSection(p, title, comment, p.isEmpty() ? 0 : p.split("\\.").length, (List)en.getValue()));
         }
      }

      return out;
   }

   private OptionBinding build(TomlLines.Entry e) {
      String var10000 = this.idPrefix;
      String id = var10000 + e.path();
      Component label = Component.literal(Humanize.key(e.key));
      List<String> desc = e.description();
      Component tip = desc.isEmpty() ? null : Component.literal(String.join("\n", desc));
      Object v = e.value;
      Binding b;
      if (e.allowed == null || e.allowed.isEmpty() || !(v instanceof String) && !(v instanceof TomlLines.Raw)) {
         if (v instanceof Boolean) {
            b = Binding.of(id, OptionType.BOOLEAN, label).getter(() -> OptionValues.asBoolean(this.current(e), false)).setter((nv) -> this.write(e, OptionValues.asBoolean(nv, false)));
         } else if (v instanceof Long) {
            b = Binding.of(id, OptionType.INT, label).getter(() -> OptionValues.asLong(this.current(e), 0L)).setter((nv) -> this.write(e, OptionValues.asLong(nv, 0L)));
            if (e.min != null && e.max != null) {
               b.range(NumberRange.of(e.min, e.max, (double)1.0F));
            }
         } else if (v instanceof Double) {
            b = Binding.of(id, OptionType.DOUBLE, label).getter(() -> OptionValues.asDouble(this.current(e), (double)0.0F)).setter((nv) -> this.write(e, OptionValues.asDouble(nv, (double)0.0F)));
            if (e.min != null && e.max != null && e.max - e.min <= (double)100000.0F) {
               b.range(NumberRange.of(e.min, e.max, (double)0.0F));
            }
         } else if (v instanceof List) {
            b = Binding.of(id, OptionType.LIST, label).getter(() -> OptionValues.asList(this.current(e))).setter((nv) -> this.write(e, OptionValues.asList(nv)));
         } else {
            label39: {
               if (v instanceof String) {
                  String s = (String)v;
                  if (looksLikeColor(s)) {
                     b = Binding.of(id, OptionType.COLOR, label).getter(() -> OptionValues.asColor(this.current(e), -1)).setter((nv) -> this.write(e, Colors.toHex(OptionValues.asColor(nv, -1))));
                     break label39;
                  }
               }

               b = Binding.of(id, OptionType.STRING, label).getter(() -> OptionValues.asString(this.current(e))).setter((nv) -> this.write(e, v instanceof TomlLines.Raw ? new TomlLines.Raw(OptionValues.asString(nv)) : OptionValues.asString(nv)));
            }
         }
      } else {
         b = Binding.of(id, OptionType.CHOICE, label).choices(Choice.ofStrings(e.allowed)).getter(() -> OptionValues.asString(this.current(e))).setter((nv) -> this.write(e, OptionValues.asString(nv)));
      }

      b.tooltip(tip).def(e.def).searchWords(String.valueOf(this.path.getFileName()) + " " + this.modId + " " + e.section);
      return b;
   }

   private static boolean looksLikeColor(String s) {
      return s.matches("#[0-9A-Fa-f]{6}") || s.matches("#[0-9A-Fa-f]{8}");
   }

   private Object current(TomlLines.Entry e) {
      this.ensureFresh();
      TomlLines.Entry live = (TomlLines.Entry)this.toml.find(e.path()).orElse(e);
      return live.value;
   }

   private void write(TomlLines.Entry e, Object value) {
      this.ensureFresh();
      if (this.toml.set(e.path(), value)) {
         this.save();
      }

   }
}
