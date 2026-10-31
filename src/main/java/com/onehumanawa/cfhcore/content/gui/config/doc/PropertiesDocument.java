package com.onehumanawa.cfhcore.content.gui.config.doc;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.file.PropertiesLines;
import com.onehumanawa.cfhcore.content.gui.config.file.TextFile;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.Humanize;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
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

public final class PropertiesDocument implements FileDocument {
   private final Path path;
   private final String modId;
   private final String idPrefix;
   private PropertiesLines props = PropertiesLines.parse("");
   private long loadedMtime;
   private @Nullable Component notice;
   private final Map<String, OptionBinding> bindings = new LinkedHashMap();

   public PropertiesDocument(Path path, String modId, String fileRelativeToGame) {
      this.path = path;
      this.modId = modId == null ? "" : modId;
      this.idPrefix = "props:" + fileRelativeToGame.replace('\\', '/') + ":";
      this.reload();
   }

   public Path path() {
      return this.path;
   }

   public Component title() {
      return Component.literal(this.path.getFileName().toString());
   }

   public String kind() {
      return "properties";
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

   public void ensureFresh() {
      if (TextFile.mtime(this.path) != this.loadedMtime) {
         this.reload();
      }

   }

   public void reload() {
      this.bindings.clear();

      try {
         this.props = Files.isRegularFile(this.path, new LinkOption[0]) ? PropertiesLines.parse(TextFile.read(this.path)) : PropertiesLines.parse("");
         this.loadedMtime = TextFile.mtime(this.path);
         this.notice = Files.isRegularFile(this.path, new LinkOption[0]) ? null : Component.translatable("cfhgui_config.editor.missing_file");
      } catch (Exception e) {
         CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot read {}: {}", this.path, e.toString());
         this.notice = Component.translatable("cfhgui_config.editor.parse_error", new Object[]{String.valueOf(e.getMessage())});
      }

   }

   private void save() {
      try {
         TextFile.writeAtomic(this.path, this.props.render());
         this.loadedMtime = TextFile.mtime(this.path);
      } catch (IOException e) {
         CFHGuiConfig.LOGGER.error("[CFHGui Config] cannot write {}", this.path, e);
      }

   }

   public Optional<OptionBinding> binding(String key) {
      this.ensureFresh();
      OptionBinding cached = (OptionBinding)this.bindings.get(key);
      return cached != null ? Optional.of(cached) : this.props.find(key).map((e) -> {
         OptionBinding b = this.build(e);
         this.bindings.put(key, b);
         return b;
      });
   }

   public List<DocSection> sections() {
      this.ensureFresh();
      List<OptionBinding> all = new ArrayList();

      for(PropertiesLines.Entry e : this.props.entries()) {
         all.add((OptionBinding)this.bindings.computeIfAbsent(e.key, (k) -> this.build(e)));
      }

      return List.of(new DocSection("", Component.translatable("cfhgui_config.editor.root"), (Component)null, 0, all));
   }

   private OptionBinding build(PropertiesLines.Entry e) {
      String id = this.idPrefix + e.key;
      Component label = Component.literal(Humanize.key(e.key));
      List<String> desc = e.description();
      Component tip = desc.isEmpty() ? null : Component.literal(String.join("\n", desc));
      String v = e.value.trim();
      Binding b;
      if (e.allowed != null) {
         b = Binding.of(id, OptionType.CHOICE, label).choices(Choice.ofStrings(e.allowed)).getter(() -> this.current(e)).setter((nv) -> this.write(e, OptionValues.asString(nv)));
      } else if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
         if (v.matches("[+-]?\\d+")) {
            b = Binding.of(id, OptionType.INT, label).getter(() -> OptionValues.asLong(this.current(e), 0L)).setter((nv) -> this.write(e, Long.toString(OptionValues.asLong(nv, 0L))));
         } else if (v.matches("[+-]?(\\d+\\.\\d*|\\.\\d+|\\d+)([eE][+-]?\\d+)?")) {
            b = Binding.of(id, OptionType.DOUBLE, label).getter(() -> OptionValues.asDouble(this.current(e), (double)0.0F)).setter((nv) -> this.write(e, OptionValues.formatDouble(OptionValues.asDouble(nv, (double)0.0F))));
         } else {
            b = Binding.of(id, OptionType.STRING, label).getter(() -> this.current(e)).setter((nv) -> this.write(e, OptionValues.asString(nv)));
         }
      } else {
         b = Binding.of(id, OptionType.BOOLEAN, label).getter(() -> OptionValues.asBoolean(this.current(e), false)).setter((nv) -> this.write(e, Boolean.toString(OptionValues.asBoolean(nv, false))));
      }

      Binding var10000 = b.tooltip(tip);
      String var10001 = String.valueOf(this.path.getFileName());
      return var10000.searchWords(var10001 + " " + this.modId);
   }

   private String current(PropertiesLines.Entry e) {
      this.ensureFresh();
      return (String)this.props.find(e.key).map((x) -> x.value).orElse(e.value);
   }

   private void write(PropertiesLines.Entry e, String value) {
      this.ensureFresh();
      if (this.props.set(e.key, value)) {
         this.save();
      }

   }
}
