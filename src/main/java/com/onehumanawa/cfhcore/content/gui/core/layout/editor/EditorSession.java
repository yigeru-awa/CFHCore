package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutStore;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

final class EditorSession {
   private static final int UNDO_CAP = 100;
   static final List<ScreenLayout.Element> CLIPBOARD = new ArrayList();
   private static @Nullable EditorSession resumable;
   final Screen screen;
   final String layoutId;
   private ScreenLayout working;
   private String savedJson;
   private final Deque<ScreenLayout> undo = new ArrayDeque();
   private final Deque<ScreenLayout> redo = new ArrayDeque();
   private @Nullable String lastKey;
   private long lastKeyMs;
   private int revision;
   private int dirtyRevision = -1;
   private boolean dirtyCached;
   private boolean refreshing;
   List<String> selection = List.of();
   boolean showProps = true;
   boolean showLayers;
   boolean grid;
   boolean snap = true;

   EditorSession(Screen screen) {
      this.screen = screen;
      this.layoutId = LayoutApplier.layoutId(screen);
      this.working = LayoutStore.get(this.layoutId);
      normalize(this.working, this.layoutId);
      this.savedJson = this.diskJson();
   }

   ScreenLayout layout() {
      return this.working;
   }

   static String json(ScreenLayout l) {
      return JsonConfig.GSON.toJson(l);
   }

   private String diskJson() {
      ScreenLayout disk = null;
      Path f = LayoutStore.fileFor(this.layoutId);
      if (Files.isRegularFile(f, new LinkOption[0])) {
         try {
            disk = (ScreenLayout)JsonConfig.GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), ScreenLayout.class);
         } catch (Exception var4) {
         }
      }

      if (disk == null) {
         disk = new ScreenLayout();
      }

      normalize(disk, this.layoutId);
      return json(disk);
   }

   void touch() {
      ++this.revision;
   }

   boolean dirty() {
      if (this.dirtyRevision != this.revision) {
         this.dirtyRevision = this.revision;

         try {
            this.dirtyCached = !json(this.working).equals(this.savedJson);
         } catch (Exception var2) {
            this.dirtyCached = true;
         }
      }

      return this.dirtyCached;
   }

   void pushUndo(@Nullable String coalesceKey) {
      long now = Clock.nowMs();
      if (coalesceKey != null && coalesceKey.equals(this.lastKey) && now - this.lastKeyMs < 1500L) {
         this.lastKeyMs = now;
         this.touch();
      } else {
         this.lastKey = coalesceKey;
         this.lastKeyMs = now;
         this.undo.push(this.working.copy());

         while(this.undo.size() > 100) {
            this.undo.removeLast();
         }

         this.redo.clear();
         this.touch();
      }
   }

   boolean canUndo() {
      return !this.undo.isEmpty();
   }

   boolean canRedo() {
      return !this.redo.isEmpty();
   }

   boolean undo() {
      if (this.undo.isEmpty()) {
         return false;
      } else {
         this.redo.push(this.working.copy());
         assign(this.working, (ScreenLayout)this.undo.pop());
         this.lastKey = null;
         this.touch();
         return true;
      }
   }

   boolean redo() {
      if (this.redo.isEmpty()) {
         return false;
      } else {
         this.undo.push(this.working.copy());
         assign(this.working, (ScreenLayout)this.redo.pop());
         this.lastKey = null;
         this.touch();
         return true;
      }
   }

   void save() {
      LayoutStore.save(this.layoutId, this.working);
      this.savedJson = json(this.working);
      this.touch();
   }

   void discard() {
      LayoutStore.invalidate();
      this.working = LayoutStore.get(this.layoutId);
      normalize(this.working, this.layoutId);
      this.undo.clear();
      this.redo.clear();
      this.lastKey = null;
      this.savedJson = this.diskJson();
      this.reapply();
   }

   void reapply() {
      this.refreshing = true;

      try {
         LayoutApplier.refresh(this.screen);
      } catch (Exception e) {
         CFHGui.LOGGER.error("[CFHGui] editor: applying layout {} failed", this.layoutId, e);
      } finally {
         this.refreshing = false;
      }

      this.touch();
   }

   boolean refreshing() {
      return this.refreshing;
   }

   void replaceWith(ScreenLayout other) {
      this.pushUndo((String)null);
      assign(this.working, other);
      normalize(this.working, this.layoutId);
      this.touch();
   }

   String newElementId(String base) {
      String b = base == null ? "element" : base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
      b = b.replaceAll("_\\d+$", "");
      if (b.isEmpty()) {
         b = "element";
      }

      Set<String> used = new HashSet();

      for(ScreenLayout.Element e : this.working.elements) {
         used.add(e.id);
      }

      int n = 1;

      while(true) {
         String id = b + "_" + n;
         if (!used.contains(id)) {
            return id;
         }

         ++n;
      }
   }

   boolean idInUse(String id, ScreenLayout.@Nullable Element except) {
      for(ScreenLayout.Element e : this.working.elements) {
         if (e != except && e.id.equals(id)) {
            return true;
         }
      }

      return false;
   }

   ScreenLayout.Element newElement(ElementType type) {
      ScreenLayout.Element e = new ScreenLayout.Element();
      e.type = type.id();
      e.id = this.newElementId(type.id().substring(type.id().indexOf(58) + 1));
      int[] d = type.defaultSize();
      e.place = new ScreenLayout.Placement("CENTER", 0, 0, d[0], d[1]);

      for(ActionType.Arg a : type.props()) {
         e.props.put(a.key(), a.defaultValue() == null ? "" : a.defaultValue());
      }

      return e;
   }

   static LinkedHashMap<String, String> defaultArgs(ActionType type) {
      LinkedHashMap<String, String> m = new LinkedHashMap();

      for(ActionType.Arg a : type.args()) {
         m.put(a.key(), a.defaultValue() == null ? "" : a.defaultValue());
      }

      return m;
   }

   static void setResumable(@Nullable EditorSession s) {
      resumable = s;
   }

   static @Nullable EditorSession takeResumable(Screen screen) {
      EditorSession s = resumable;
      resumable = null;
      return s != null && s.screen == screen ? s : null;
   }

   static void clearResumable(EditorSession s) {
      if (resumable == s) {
         resumable = null;
      }

   }

   static boolean parked(String layoutId) {
      EditorSession s = resumable;
      return s != null && s.layoutId.equals(layoutId);
   }

   static void normalize(ScreenLayout l, String id) {
      if (l.hidden == null) {
         l.hidden = new ArrayList();
      }

      l.hidden.removeIf(Objects::isNull);
      if (l.moved == null) {
         l.moved = new LinkedHashMap();
      }

      l.moved.values().removeIf(Objects::isNull);

      for(ScreenLayout.Placement p : l.moved.values()) {
         if (p.anchor == null) {
            p.anchor = "TOP_LEFT";
         }
      }

      if (l.elements == null) {
         l.elements = new ArrayList();
      }

      l.elements.removeIf(Objects::isNull);
      Set<String> ids = new HashSet();

      for(ScreenLayout.Element e : l.elements) {
         if (e.type == null || e.type.isBlank()) {
            e.type = "cfhgui:button";
         }

         if (e.place == null) {
            e.place = new ScreenLayout.Placement();
         }

         if (e.place.anchor == null) {
            e.place.anchor = "TOP_LEFT";
         }

         if (e.props == null) {
            e.props = new LinkedHashMap();
         }

         e.props.values().removeIf(Objects::isNull);
         if (e.actions == null) {
            e.actions = new ArrayList();
         }

         e.actions.removeIf(Objects::isNull);

         for(ScreenLayout.Action a : e.actions) {
            if (a.type == null) {
               a.type = "";
            }

            if (a.args == null) {
               a.args = new LinkedHashMap();
            }

            a.args.values().removeIf(Objects::isNull);
         }

         if (e.id == null || e.id.isBlank() || !ids.add(e.id)) {
            String base = e.type.substring(e.type.indexOf(58) + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            int n = 1;

            while(true) {
               String cand = base + "_" + n;
               if (ids.add(cand)) {
                  e.id = cand;
                  break;
               }

               ++n;
            }
         }
      }

      if (l.background != null) {
         if (l.background.kind == null) {
            l.background.kind = "default";
         }

         if (l.background.value == null) {
            l.background.value = "";
         }

         if ("default".equalsIgnoreCase(l.background.kind.trim())) {
            l.background = null;
         }
      }

      l.screen = id;
      if (l.version <= 0) {
         l.version = 1;
      }

   }

   static void assign(ScreenLayout target, ScreenLayout src) {
      target.version = src.version;
      target.screen = src.screen;
      target.hidden.clear();
      if (src.hidden != null) {
         target.hidden.addAll(src.hidden);
      }

      target.moved.clear();
      if (src.moved != null) {
         src.moved.forEach((k, v) -> {
            if (v != null) {
               target.moved.put(k, v.copy());
            }

         });
      }

      target.elements.clear();
      if (src.elements != null) {
         for(ScreenLayout.Element e : src.elements) {
            if (e != null) {
               target.elements.add(e.copy());
            }
         }
      }

      target.background = copyBackground(src.background);
   }

   static ScreenLayout.@Nullable Background copyBackground(ScreenLayout.@Nullable Background b) {
      if (b == null) {
         return null;
      } else {
         ScreenLayout.Background c = new ScreenLayout.Background();
         c.kind = b.kind;
         c.value = b.value;
         c.dim = b.dim;
         return c;
      }
   }
}
