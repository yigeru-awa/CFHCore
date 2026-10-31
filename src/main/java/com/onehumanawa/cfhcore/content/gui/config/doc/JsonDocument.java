package com.onehumanawa.cfhcore.content.gui.config.doc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.file.JsonDoc;
import com.onehumanawa.cfhcore.content.gui.config.file.TextFile;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Humanize;
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

public final class JsonDocument implements FileDocument {
   private final Path path;
   private final String modId;
   private final String idPrefix;
   private JsonDoc doc = JsonDoc.empty();
   private long loadedMtime;
   private @Nullable Component notice;
   private boolean editable = true;
   private final Map<String, OptionBinding> bindings = new LinkedHashMap();

   public JsonDocument(Path path, String modId, String fileRelativeToGame) {
      this.path = path;
      this.modId = modId == null ? "" : modId;
      this.idPrefix = "json:" + fileRelativeToGame.replace('\\', '/') + ":";
      this.reload();
   }

   public Path path() {
      return this.path;
   }

   public Component title() {
      return Component.literal(this.path.getFileName().toString());
   }

   public String kind() {
      return "json";
   }

   public String modId() {
      return this.modId;
   }

   public @Nullable Component notice() {
      return this.notice;
   }

   public boolean editable() {
      return this.editable;
   }

   public JsonDoc doc() {
      return this.doc;
   }

   public void ensureFresh() {
      if (TextFile.mtime(this.path) != this.loadedMtime) {
         this.reload();
      }

   }

   public void reload() {
      this.bindings.clear();
      this.editable = true;

      try {
         if (!Files.isRegularFile(this.path, new LinkOption[0])) {
            this.doc = JsonDoc.empty();
            this.notice = Component.translatable("cfhgui_config.editor.missing_file");
            this.editable = false;
         } else {
            this.doc = JsonDoc.parse(TextFile.read(this.path));
            this.notice = this.doc.hadComments() ? Component.translatable("cfhgui_config.editor.comments_lost") : null;
         }

         this.loadedMtime = TextFile.mtime(this.path);
      } catch (Exception e) {
         CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot parse {}: {}", this.path, e.toString());
         this.doc = JsonDoc.empty();
         this.notice = Component.translatable("cfhgui_config.editor.parse_error", new Object[]{String.valueOf(e.getMessage())});
         this.editable = false;
      }

   }

   private void save() {
      try {
         TextFile.writeAtomic(this.path, this.doc.render());
         this.loadedMtime = TextFile.mtime(this.path);
      } catch (IOException e) {
         CFHGuiConfig.LOGGER.error("[CFHGui Config] cannot write {}", this.path, e);
      }

   }

   public Optional<OptionBinding> binding(String pointer) {
      this.ensureFresh();
      OptionBinding cached = (OptionBinding)this.bindings.get(pointer);
      if (cached != null) {
         return Optional.of(cached);
      } else {
         JsonElement el = this.doc.get(pointer);
         if (el != null && !el.isJsonObject() && (!el.isJsonArray() || isPrimitiveArray(el.getAsJsonArray()))) {
            List<String> toks = JsonDoc.tokens(pointer);
            String key = toks.isEmpty() ? "" : (String)toks.get(toks.size() - 1);
            OptionBinding b = this.build(pointer, key, el);
            this.bindings.put(pointer, b);
            return Optional.of(b);
         } else {
            return Optional.empty();
         }
      }
   }

   public List<DocSection> sections() {
      this.ensureFresh();
      List<DocSection> out = new ArrayList();
      if (this.doc.root().isJsonObject()) {
         this.walk("", Component.translatable("cfhgui_config.editor.root"), this.doc.root().getAsJsonObject(), 0, out);
      } else if (this.doc.root().isJsonArray()) {
         this.walkArray("", Component.translatable("cfhgui_config.editor.root"), this.doc.root().getAsJsonArray(), 0, out);
      }

      return out;
   }

   private void walk(String pointer, Component title, JsonObject obj, int depth, List<DocSection> out) {
      List<OptionBinding> leaves = new ArrayList();
      List<Runnable> nested = new ArrayList();

      for(Map.Entry<String, JsonElement> e : obj.entrySet()) {
         String p = pointer + "/" + ((String)e.getKey()).replace("~", "~0").replace("/", "~1");
         JsonElement v = (JsonElement)e.getValue();
         if (v.isJsonObject()) {
            nested.add(() -> this.walk(p, Component.literal(Humanize.key((String)e.getKey())), v.getAsJsonObject(), depth + 1, out));
         } else if (v.isJsonArray() && !isPrimitiveArray(v.getAsJsonArray())) {
            nested.add(() -> this.walkArray(p, Component.literal(Humanize.key((String)e.getKey())), v.getAsJsonArray(), depth + 1, out));
         } else {
            leaves.add((OptionBinding)this.bindings.computeIfAbsent(p, (k) -> this.build(p, (String)e.getKey(), v)));
         }
      }

      if (!leaves.isEmpty() || depth == 0) {
         out.add(new DocSection(pointer, title, (Component)null, depth, leaves));
      }

      for(Runnable r : nested) {
         r.run();
      }

   }

   private void walkArray(String pointer, Component title, JsonArray arr, int depth, List<DocSection> out) {
      for(int i = 0; i < arr.size(); ++i) {
         JsonElement v = arr.get(i);
         String p = pointer + "/" + i;
         int at = i;
         String var10000 = title.getString();
         Component t = Component.literal(var10000 + " [" + i + "]");
         if (v.isJsonObject()) {
            this.walk(p, t, v.getAsJsonObject(), depth + 1, out);
         } else if (v.isJsonArray()) {
            this.walkArray(p, t, v.getAsJsonArray(), depth + 1, out);
         } else {
            out.add(new DocSection(p, t, (Component)null, depth + 1, List.of((OptionBinding)this.bindings.computeIfAbsent(p, (k) -> this.build(p, "[" + at + "]", v)))));
         }
      }

   }

   private static boolean isPrimitiveArray(JsonArray arr) {
      for(JsonElement e : arr) {
         if (!e.isJsonPrimitive() && !e.isJsonNull()) {
            return false;
         }
      }

      return true;
   }

   private OptionBinding build(String pointer, String key, JsonElement el) {
      String id = this.idPrefix + pointer;
      Component label = Component.literal(Humanize.key(key));
      Binding b;
      if (el.isJsonArray()) {
         b = Binding.of(id, OptionType.LIST, label).getter(() -> OptionValues.asList(listOf(this.current(pointer)))).setter((nv) -> {
            JsonArray a = new JsonArray();
            JsonElement cur = this.current(pointer);
            JsonElement sample = cur != null && cur.isJsonArray() && !cur.getAsJsonArray().isEmpty() ? cur.getAsJsonArray().get(0) : null;

            for(String s : OptionValues.asList(nv)) {
               a.add(coerceLike(s, sample));
            }

            this.write(pointer, a);
         });
      } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
         b = Binding.of(id, OptionType.BOOLEAN, label).getter(() -> {
            JsonElement c = this.current(pointer);
            return c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isBoolean() ? c.getAsBoolean() : OptionValues.asBoolean(str(c), false);
         }).setter((nv) -> this.write(pointer, new JsonPrimitive(OptionValues.asBoolean(nv, false))));
      } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
         boolean integer = isIntegerText(el.getAsJsonPrimitive().getAsString());
         if (integer) {
            b = Binding.of(id, OptionType.INT, label).getter(() -> OptionValues.asLong(str(this.current(pointer)), 0L)).setter((nv) -> this.write(pointer, new JsonPrimitive(OptionValues.asLong(nv, 0L))));
         } else {
            b = Binding.of(id, OptionType.DOUBLE, label).getter(() -> OptionValues.asDouble(str(this.current(pointer)), (double)0.0F)).setter((nv) -> this.write(pointer, new JsonPrimitive(OptionValues.asDouble(nv, (double)0.0F))));
         }
      } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() && el.getAsString().matches("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")) {
         b = Binding.of(id, OptionType.COLOR, label).getter(() -> OptionValues.asColor(str(this.current(pointer)), -1)).setter((nv) -> this.write(pointer, new JsonPrimitive(Colors.toHex(OptionValues.asColor(nv, -1)))));
      } else {
         b = Binding.of(id, OptionType.STRING, label).getter(() -> str(this.current(pointer))).setter((nv) -> this.write(pointer, new JsonPrimitive(OptionValues.asString(nv))));
      }

      String var10001 = String.valueOf(this.path.getFileName());
      return b.searchWords(var10001 + " " + this.modId + " " + pointer.replace('/', ' '));
   }

   private static boolean isIntegerText(String s) {
      return s != null && s.matches("[+-]?\\d+");
   }

   private static JsonElement coerceLike(String s, @Nullable JsonElement sample) {
      if (sample != null && sample.isJsonPrimitive()) {
         JsonPrimitive p = sample.getAsJsonPrimitive();
         if (p.isBoolean()) {
            return new JsonPrimitive(OptionValues.asBoolean(s, false));
         }

         if (p.isNumber()) {
            try {
               return isIntegerText(s.trim()) ? new JsonPrimitive(Long.parseLong(s.trim())) : new JsonPrimitive(Double.parseDouble(s.trim()));
            } catch (NumberFormatException var4) {
            }
         }
      }

      return new JsonPrimitive(s);
   }

   private static List<String> listOf(@Nullable JsonElement e) {
      List<String> out = new ArrayList();
      if (e != null && e.isJsonArray()) {
         for(JsonElement x : e.getAsJsonArray()) {
            out.add(x.isJsonPrimitive() ? x.getAsString() : x.toString());
         }
      }

      return out;
   }

   private static @Nullable String str(@Nullable JsonElement e) {
      if (e != null && !e.isJsonNull()) {
         return e.isJsonPrimitive() ? e.getAsString() : e.toString();
      } else {
         return null;
      }
   }

   private @Nullable JsonElement current(String pointer) {
      this.ensureFresh();
      return this.doc.get(pointer);
   }

   private void write(String pointer, JsonElement value) {
      this.ensureFresh();
      if (this.doc.set(pointer, value)) {
         this.save();
      }

   }
}
