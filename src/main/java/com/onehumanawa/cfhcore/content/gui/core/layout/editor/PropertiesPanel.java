package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutStore;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.Actions;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

final class PropertiesPanel {
   private final EditorOverlay ed;
   private final CFHGuiScrollPanel panel = new CFHGuiScrollPanel(0, 0, 10, 10);
   private Rect bounds = Rect.of(0, 0, 10, 10);
   private boolean refreshing;
   private @Nullable EditorTextField fx;
   private @Nullable EditorTextField fy;
   private @Nullable EditorTextField fw;
   private @Nullable EditorTextField fh;
   private @Nullable CFHGuiDropdown<String> anchorDd;
   private ScreenLayout.@Nullable Element shownElement;
   private @Nullable String shownKey;
   private int shownCount = -1;
   private Icon titleIcon;
   private boolean titleIconShown;

   PropertiesPanel(EditorOverlay ed) {
      this.titleIcon = Icon.PANEL;
      this.ed = ed;
      this.panel.padding(4).scrollStep(30);
   }

   void setBounds(Rect r) {
      boolean changed = r.w() != this.bounds.w() || r.h() != this.bounds.h();
      this.bounds = r;
      this.panel.setX(r.x());
      this.panel.setY(r.y() + 2);
      this.panel.setWidth(r.w());
      this.panel.setHeight(r.h() - 2);
      if (changed) {
         this.rebuild();
      }

   }

   Rect bounds() {
      return this.bounds;
   }

   boolean contains(double mx, double my) {
      return this.bounds.contains(mx, my);
   }

   boolean hasFocus() {
      return this.panel.getFocused() != null;
   }

   boolean textInputFocused() {
      GuiEventListener f = this.panel.getFocused();
      if (f instanceof EditBox eb) {
         return eb.isFocused();
      } else if (f instanceof MultiLineField m) {
         return m.isFocused();
      } else if (f instanceof ColorRow c) {
         return c.textFocused();
      } else {
         return false;
      }
   }

   boolean singleLineFocused() {
      boolean var10000;
      label26: {
         GuiEventListener f = this.panel.getFocused();
         if (f instanceof EditBox eb) {
            if (eb.isFocused()) {
               break label26;
            }
         }

         if (f instanceof ColorRow c) {
            if (c.textFocused()) {
               break label26;
            }
         }

         var10000 = false;
         return var10000;
      }

      var10000 = true;
      return var10000;
   }

   void clearFocus() {
      this.panel.setFocused((GuiEventListener)null);
   }

   void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      EditorStyle.panel(g, this.bounds.x(), this.bounds.y(), this.bounds.w(), this.bounds.h());
      this.panel.render(g, mouseX, mouseY, partialTick);
      if (this.titleIconShown && this.panel.scrollAmount() < (double)4.0F) {
         Icons.draw(g, this.titleIcon, this.bounds.x() + 6, this.bounds.y() + 7 - (int)Math.round(this.panel.scrollAmount()), 12, EditorStyle.accent());
      }

   }

   boolean mouseClicked(double mx, double my, int button) {
      boolean r = this.panel.mouseClicked(mx, my, button);
      if (!r) {
         this.panel.setFocused((GuiEventListener)null);
      }

      return r;
   }

   void mouseReleased(double mx, double my, int button) {
      this.panel.mouseReleased(mx, my, button);
   }

   void mouseDragged(double mx, double my, int button, double dx, double dy) {
      this.panel.mouseDragged(mx, my, button, dx, dy);
   }

   boolean mouseScrolled(double mx, double my, double sx, double sy) {
      return this.panel.mouseScrolled(mx, my, sx, sy);
   }

   boolean keyPressed(int key, int scan, int mods) {
      return this.panel.keyPressed(key, scan, mods);
   }

   boolean charTyped(char c, int mods) {
      return this.panel.charTyped(c, mods);
   }

   void rebuild() {
      double scroll = this.panel.scrollAmount();
      this.panel.clear();
      this.fx = this.fy = this.fw = this.fh = null;
      this.anchorDd = null;
      this.shownElement = null;
      this.shownKey = null;
      this.titleIconShown = false;
      EditorCanvas c = this.ed.canvas();
      Rows rows = new Rows(Math.max(40, this.bounds.w() - 18));
      this.shownCount = c.selection.size();

      try {
         if (c.selection.isEmpty()) {
            this.buildScreen(rows);
         } else if (c.selection.size() > 1) {
            this.buildMulti(rows, c.selected());
         } else {
            EditorItem it = c.primary();
            if (it == null) {
               this.buildScreen(rows);
            } else if (it.element != null) {
               this.buildElement(rows, it.element, it.type);
            } else {
               this.buildVanilla(rows, it);
            }
         }
      } catch (Exception e) {
         CFHGui.LOGGER.error("[CFHGui] editor: properties panel failed", e);
         rows.caption(Component.literal(e.toString()));
      }

      this.panel.setContentHeight(rows.y + 6);
      this.panel.snapScroll(Math.min(scroll, (double)this.panel.maxScroll()));
   }

   void refreshGeometry() {
      EditorItem it;
      boolean var10000;
      label88: {
         label87: {
            EditorCanvas c = this.ed.canvas();
            it = c.selection.size() == 1 ? c.primary() : null;
            if (it != null && c.selection.size() == this.shownCount) {
               if (it.element != null) {
                  if (it.element == this.shownElement) {
                     break label87;
                  }
               } else if (it.key != null && it.key.equals(this.shownKey)) {
                  break label87;
               }
            }

            var10000 = false;
            break label88;
         }

         var10000 = true;
      }

      boolean same = var10000;
      if (!same) {
         this.rebuild();
      } else {
         this.refreshing = true;

         try {
            Rect r = it.rect;
            setIfIdle(this.fx, r.x());
            setIfIdle(this.fy, r.y());
            setIfIdle(this.fw, r.w());
            setIfIdle(this.fh, r.h());
            if (this.anchorDd != null) {
               this.anchorDd.setValue(this.anchorName(it));
            }
         } finally {
            this.refreshing = false;
         }

      }
   }

   private static void setIfIdle(@Nullable EditorTextField f, int v) {
      if (f != null && !f.isFocused()) {
         String s = Integer.toString(v);
         if (!s.equals(f.getValue())) {
            f.setValue(s);
         }

      }
   }

   private String anchorName(EditorItem it) {
      if (it.element != null) {
         return it.element.place.anchor == null ? "AUTO" : it.element.place.anchor.toUpperCase(Locale.ROOT);
      } else {
         ScreenLayout.Placement p = it.key == null ? null : (ScreenLayout.Placement)this.ed.session().layout().moved.get(it.key);
         return p != null && p.anchor != null ? p.anchor.toUpperCase(Locale.ROOT) : "AUTO";
      }
   }

   private void buildElement(Rows rows, ScreenLayout.Element e, @Nullable ElementType type) {
      this.shownElement = e;
      EditorCanvas c = this.ed.canvas();
      Supplier<EditorItem> cur = () -> c.itemOf(e);
      rows.title((Component)(type != null ? type.label() : Component.literal(e.type)), type != null ? type.icon() : Icon.QUESTION);
      rows.caption(EditorText.t("props.custom_element"));
      rows.caption(EditorText.t("props.id"));
      EditorTextField idField = (EditorTextField)rows.add((new EditorTextField(0, 0, rows.w, EditorText.t("props.id"))).text(e.id), 20);
      idField.onChange((v) -> {
         if (!this.refreshing) {
            String nv = v.trim();
            boolean ok = nv.matches("[A-Za-z0-9_.-]+") && (nv.equals(e.id) || !this.ed.session().idInUse(nv, e));
            idField.setInvalid(!ok);
            EditorItem it = (EditorItem)cur.get();
            if (ok && it != null && !nv.equals(e.id)) {
               c.renameElement(it, nv);
            }

         }
      });
      this.anchorRow(rows, cur);
      this.geometryRows(rows, cur);
      rows.pair(new CFHGuiToggle(0, 0, 10, EditorText.t("props.visible"), e.visible, (v) -> {
         EditorItem it = (EditorItem)cur.get();
         if (it != null && !this.refreshing) {
            c.setVisible(it, v);
         }

      }), new CFHGuiToggle(0, 0, 10, EditorText.t("props.locked"), e.locked, (v) -> {
         EditorItem it = (EditorItem)cur.get();
         if (it != null && !this.refreshing) {
            c.setLocked(it, v);
         }

      }), 20);
      if (type != null && !type.props().isEmpty()) {
         rows.separator(EditorText.t("props.section_props"));

         for(ActionType.Arg arg : type.props()) {
            if (EditorForms.captioned(arg.kind())) {
               rows.caption(arg.label());
            }

            String key = arg.key();
            rows.add(EditorForms.field(0, 0, rows.w, arg, (String)e.props.getOrDefault(key, arg.defaultValue()), (v) -> {
               if (!this.refreshing) {
                  this.ed.session().pushUndo("prop:" + e.id + ":" + key);
                  e.props.put(key, v);
                  this.ed.applyLive();
               }
            }), EditorForms.height(arg.kind()));
         }
      }

      if (type == null || type.clickable()) {
         this.buildActions(rows, e);
      }

      rows.separator(EditorText.t("props.section_element"));
      MutableComponent var10007 = EditorText.t("props.duplicate");
      Objects.requireNonNull(c);
      CFHGuiButton var10001 = (new CFHGuiButton(0, 0, 10, 18, var10007, c::duplicate)).icon(Icon.DUPLICATE).variant(CFHGuiButton.Variant.GHOST);
      MutableComponent var10008 = EditorText.t("props.delete");
      Objects.requireNonNull(c);
      rows.pair(var10001, (new CFHGuiButton(0, 0, 10, 18, var10008, c::deleteSelected)).icon(Icon.TRASH).variant(CFHGuiButton.Variant.DANGER), 18);
   }

   private void buildActions(Rows rows, ScreenLayout.Element e) {
      rows.separator(EditorText.t("props.section_actions"));
      List<ActionType> all = Actions.all();

      for(int i = 0; i < e.actions.size(); ++i) {
         int idx = i;
         ScreenLayout.Action act = (ScreenLayout.Action)e.actions.get(i);
         ActionType cur = (ActionType)Actions.get(act.type).orElse(null);
         int y = rows.y;
         int ddW = rows.w - 66;
         CFHGuiDropdown<ActionType> dd = new CFHGuiDropdown<ActionType>(0, 0, ddW, all, cur, (a) -> a.label(), (a) -> {
            if (!this.refreshing && a != null) {
               this.ed.session().pushUndo((String)null);
               act.type = a.id();
               act.args = EditorSession.defaultArgs(a);
               this.ed.applyStructural();
            }
         });
         CFHGuiScrollPanel var10000 = this.panel;
         Objects.requireNonNull(rows);
         var10000.add(dd, 6, y);
         CFHGuiIconButton up = new CFHGuiIconButton(0, 0, 20, Icon.ARROW_UP, EditorText.t("props.action_up"), () -> this.moveAction(e, idx, -1));
         up.enabled(i > 0);
         var10000 = this.panel;
         Objects.requireNonNull(rows);
         var10000.add(up, 6 + ddW + 2, y);
         CFHGuiIconButton down = new CFHGuiIconButton(0, 0, 20, Icon.ARROW_DOWN, EditorText.t("props.action_down"), () -> this.moveAction(e, idx, 1));
         down.enabled(i < e.actions.size() - 1);
         var10000 = this.panel;
         Objects.requireNonNull(rows);
         var10000.add(down, 6 + ddW + 24, y);
         var10000 = this.panel;
         CFHGuiIconButton var10001 = new CFHGuiIconButton(0, 0, 20, Icon.TRASH, EditorText.t("props.action_remove"), () -> {
            this.ed.session().pushUndo((String)null);
            if (idx < e.actions.size()) {
               e.actions.remove(idx);
            }

            this.ed.applyStructural();
         });
         Objects.requireNonNull(rows);
         var10000.add(var10001, 6 + ddW + 46, y);
         rows.y += 24;
         if (cur == null) {
            rows.caption(EditorText.t("props.unknown_action", act.type));
         } else {
            for(ActionType.Arg arg : cur.args()) {
               if (EditorForms.captioned(arg.kind())) {
                  rows.caption(arg.label());
               }

               String key = arg.key();
               rows.add(EditorForms.field(0, 0, rows.w, arg, (String)act.args.getOrDefault(key, arg.defaultValue()), (v) -> {
                  if (!this.refreshing) {
                     this.ed.session().pushUndo("act:" + e.id + ":" + idx + ":" + key);
                     act.args.put(key, v);
                     this.ed.applyLive();
                  }
               }), EditorForms.height(arg.kind()));
            }
         }

         rows.gap(2);
      }

      rows.add((new CFHGuiButton(0, 0, rows.w, 18, EditorText.t("props.add_action"), () -> {
         this.ed.session().pushUndo((String)null);
         ActionType first = (ActionType)Actions.get("cfhgui:open_screen").orElse(all.isEmpty() ? null : (ActionType)all.get(0));
         if (first != null) {
            e.actions.add(new ScreenLayout.Action(first.id(), EditorSession.defaultArgs(first)));
            this.ed.applyStructural();
         }
      })).icon(Icon.PLUS).variant(CFHGuiButton.Variant.GHOST).leftAligned(), 18);
   }

   private void moveAction(ScreenLayout.Element e, int idx, int delta) {
      int to = idx + delta;
      if (idx >= 0 && idx < e.actions.size() && to >= 0 && to < e.actions.size()) {
         this.ed.session().pushUndo((String)null);
         ScreenLayout.Action a = (ScreenLayout.Action)e.actions.remove(idx);
         e.actions.add(to, a);
         this.ed.applyStructural();
      }
   }

   private void buildVanilla(Rows rows, EditorItem item) {
      this.shownKey = item.key;
      EditorCanvas c = this.ed.canvas();
      String key = item.key == null ? "" : item.key;
      Supplier<EditorItem> cur = () -> c.itemOfKey(key);
      rows.title(item.name(), item.icon());
      rows.caption((Component)(item.fixed() ? item.typeName() : EditorText.t("props.vanilla_widget", item.typeName())));
      rows.caption(Component.literal(key));
      if (item.fixed()) {
         rows.note(EditorText.t(item.isPart() ? "props.part_hint" : "props.left_out_hint"));
      } else if (item.backdrop) {
         rows.note(EditorText.t("props.backdrop_hint"));
      } else {
         this.anchorRow(rows, cur);
         this.geometryRows(rows, cur);
      }

      rows.add(new CFHGuiToggle(0, 0, rows.w, EditorText.t("props.visible"), item.visible(), (v) -> {
         EditorItem it = (EditorItem)cur.get();
         if (it != null && !this.refreshing) {
            c.setVisible(it, v);
         }

      }), 20);
      rows.gap(2);
      CFHGuiButton var10001 = (new CFHGuiButton(0, 0, 10, 18, EditorText.t("props.reset_widget"), () -> {
         EditorItem it = (EditorItem)cur.get();
         if (it != null) {
            c.resetVanilla(it);
         }

      })).icon(Icon.REFRESH).variant(CFHGuiButton.Variant.GHOST);
      MutableComponent var10008 = EditorText.t("props.delete");
      Objects.requireNonNull(c);
      rows.pair(var10001, (new CFHGuiButton(0, 0, 10, 18, var10008, c::deleteSelected)).icon(Icon.TRASH).variant(CFHGuiButton.Variant.DANGER), 18);
   }

   private void anchorRow(Rows rows, Supplier<EditorItem> cur) {
      rows.caption(EditorText.t("props.anchor"));
      List<String> opts = new ArrayList();
      opts.add("AUTO");

      for(Anchor a : Anchor.values()) {
         opts.add(a.name());
      }

      EditorItem it0 = (EditorItem)cur.get();
      String curA = it0 == null ? "AUTO" : this.anchorName(it0);
      this.anchorDd = new CFHGuiDropdown<String>(0, 0, rows.w, opts, opts.contains(curA) ? curA : "AUTO", (s) -> "AUTO".equals(s) ? EditorText.t("props.anchor_auto") : Component.literal(pretty(s)), (s) -> {
         if (!this.refreshing) {
            EditorItem it = (EditorItem)cur.get();
            if (it != null) {
               this.ed.canvas().setAnchor(it, "AUTO".equals(s) ? null : Anchor.parse(s, Anchor.TOP_LEFT));
            }

         }
      });
      rows.add(this.anchorDd, 20);
   }

   private void geometryRows(Rows rows, Supplier<EditorItem> cur) {
      EditorItem it0 = (EditorItem)cur.get();
      Rect r = it0 == null ? Rect.of(0, 0, 0, 0) : it0.rect;
      int half = rows.w / 2 - 2;
      rows.caption2(EditorText.t("props.x"), EditorText.t("props.y"));
      this.fx = (new EditorTextField(0, 0, half, EditorText.t("props.x"))).integer().text(Integer.toString(r.x()));
      this.fy = (new EditorTextField(0, 0, half, EditorText.t("props.y"))).integer().text(Integer.toString(r.y()));
      rows.pair(this.fx, this.fy, 20);
      rows.caption2(EditorText.t("props.w"), EditorText.t("props.h"));
      this.fw = (new EditorTextField(0, 0, half, EditorText.t("props.w"))).integer().text(Integer.toString(r.w()));
      this.fh = (new EditorTextField(0, 0, half, EditorText.t("props.h"))).integer().text(Integer.toString(r.h()));
      rows.pair(this.fw, this.fh, 20);
      this.fx.onChange((v) -> this.geometry(cur, v, 0));
      this.fy.onChange((v) -> this.geometry(cur, v, 1));
      this.fw.onChange((v) -> this.geometry(cur, v, 2));
      this.fh.onChange((v) -> this.geometry(cur, v, 3));
   }

   private void geometry(Supplier<EditorItem> cur, String v, int which) {
      if (!this.refreshing) {
         EditorItem it = (EditorItem)cur.get();
         if (it != null && !it.locked()) {
            int n;
            try {
               n = Integer.parseInt(v.trim());
            } catch (NumberFormatException var8) {
               return;
            }

            Rect r = it.rect;
            Rect var10000;
            switch (which) {
               case 0 -> var10000 = r.withX(n);
               case 1 -> var10000 = r.withY(n);
               case 2 -> var10000 = r.withW(Math.max(1, n));
               default -> var10000 = r.withH(Math.max(1, n));
            }

            Rect nr = var10000;
            if (!nr.equals(r)) {
               this.ed.canvas().setGeometry(it, nr, which >= 2, "geom:" + it.id + ":" + which);
            }
         }
      }
   }

   private static String pretty(String anchor) {
      String s = anchor.toLowerCase(Locale.ROOT).replace('_', ' ');
      char var10000 = Character.toUpperCase(s.charAt(0));
      return var10000 + s.substring(1);
   }

   private void buildMulti(Rows rows, List<EditorItem> sel) {
      EditorCanvas c = this.ed.canvas();
      rows.title(EditorText.t("props.n_selected", sel.size()), Icon.LAYERS);
      rows.caption(EditorText.t("props.align_hint"));
      int y = rows.y;
      EditorCanvas.Align[] aligns = EditorCanvas.Align.values();
      Icon[] icons = new Icon[]{Icon.ALIGN_LEFT, Icon.ALIGN_CENTER, Icon.ALIGN_RIGHT, Icon.ALIGN_TOP, Icon.ALIGN_MIDDLE, Icon.ALIGN_BOTTOM};
      int step = Math.max(20, rows.w / 6);

      for(int i = 0; i < aligns.length; ++i) {
         EditorCanvas.Align a = aligns[i];
         CFHGuiScrollPanel var10000 = this.panel;
         Icon var10006 = icons[i];
         String var10007 = a.name();
         CFHGuiIconButton var10001 = new CFHGuiIconButton(0, 0, 20, var10006, EditorText.t("props.align." + var10007.toLowerCase(Locale.ROOT)), () -> c.align(a));
         Objects.requireNonNull(rows);
         var10000.add(var10001, 6 + i * step, y);
      }

      rows.y += 24;
      rows.separator(EditorText.t("props.section_element"));
      CFHGuiButton var10 = (new CFHGuiButton(0, 0, 10, 18, EditorText.t("props.hide"), () -> {
         for(EditorItem it : c.selected()) {
            c.setVisible(it, false);
         }

      })).icon(Icon.EYE_OFF).variant(CFHGuiButton.Variant.GHOST);
      MutableComponent var10008 = EditorText.t("props.delete");
      Objects.requireNonNull(c);
      rows.pair(var10, (new CFHGuiButton(0, 0, 10, 18, var10008, c::deleteSelected)).icon(Icon.TRASH).variant(CFHGuiButton.Variant.DANGER), 18);
      int var10005 = rows.w;
      MutableComponent var11 = EditorText.t("props.duplicate");
      Objects.requireNonNull(c);
      rows.add((new CFHGuiButton(0, 0, var10005, 18, var11, c::duplicate)).icon(Icon.DUPLICATE).variant(CFHGuiButton.Variant.GHOST).leftAligned(), 18);
   }

   private void buildScreen(Rows rows) {
      EditorSession s = this.ed.session();
      ScreenLayout l = s.layout();
      String id = s.layoutId;
      rows.title(Component.literal(id.startsWith("custom:") ? id.substring(7) : ScreenIds.display(id)), Icon.MONITOR);
      rows.caption(Component.literal(id));
      rows.caption(Component.literal(LayoutStore.fileFor(id).getFileName().toString()));
      rows.separator(EditorText.t("props.section_background"));
      if (this.ed.screen() instanceof LayoutAware) {
         rows.note(EditorText.t("props.bg_scenery"));
      }

      rows.caption(EditorText.t("props.bg_kind"));
      List<String> kinds = List.of("default", "color", "image", "panorama", "none");
      String kind = l.background != null && l.background.kind != null ? l.background.kind.toLowerCase(Locale.ROOT) : "default";
      rows.add(new CFHGuiDropdown<String>(0, 0, rows.w, kinds, kinds.contains(kind) ? kind : "default", (k) -> EditorText.t("props.bg." + k), (k) -> {
         if (!this.refreshing) {
            s.pushUndo((String)null);
            if ("default".equals(k)) {
               l.background = null;
            } else {
               if (l.background == null) {
                  l.background = new ScreenLayout.Background();
                  l.background.value = "color".equals(k) ? "#161615" : "";
               }

               l.background.kind = k;
            }

            LayoutBackground.forgetImages();
            this.ed.applyStructural();
         }
      }), 20);
      if (l.background != null) {
         ScreenLayout.Background bg = l.background;
         switch (kind) {
            case "color":
               rows.caption(EditorText.t("props.bg_color"));
               rows.add(new ColorRow(0, 0, rows.w, bg.value, (v) -> {
                  if (!this.refreshing) {
                     s.pushUndo("bg:color");
                     bg.value = v;
                  }
               }), 20);
               break;
            case "image":
               rows.note(EditorText.t("props.bg_image"));
               rows.add((new EditorTextField(0, 0, rows.w, EditorText.t("props.bg_image"))).text(bg.value).placeholder(Component.literal("config/cfhgui/images/bg.png")).onChange((v) -> {
                  if (!this.refreshing) {
                     s.pushUndo("bg:image");
                     bg.value = v.trim();
                     LayoutBackground.forgetImages();
                  }
               }), 20);
               rows.add(new CFHGuiToggle(0, 0, rows.w, EditorText.t("props.bg_dim"), bg.dim, (v) -> {
                  if (!this.refreshing) {
                     s.pushUndo((String)null);
                     bg.dim = v;
                  }
               }), 20);
               break;
            case "panorama":
               rows.add(new CFHGuiToggle(0, 0, rows.w, EditorText.t("props.bg_gradient"), bg.dim, (v) -> {
                  if (!this.refreshing) {
                     s.pushUndo((String)null);
                     bg.dim = v;
                  }
               }), 20);
         }
      }

      rows.separator(EditorText.t("props.section_layout"));
      rows.caption(EditorText.t("props.stats", l.elements.size(), l.moved.size(), l.hidden.size()));
      rows.add((new CFHGuiButton(0, 0, rows.w, 18, EditorText.t("toolbar.add"), () -> this.ed.openAddPalette(this.bounds.x() + 6, this.bounds.y() + 40))).icon(Icon.PLUS).variant(CFHGuiButton.Variant.GHOST).leftAligned(), 18);
      int var10005 = rows.w;
      MutableComponent var10007 = EditorText.t("toolbar.reset");
      EditorOverlay var10008 = this.ed;
      Objects.requireNonNull(var10008);
      rows.add((new CFHGuiButton(0, 0, var10005, 18, var10007, var10008::resetScreen)).icon(Icon.REFRESH).variant(CFHGuiButton.Variant.DANGER).leftAligned(), 18);
   }

   private final class Rows {
      final int x = 6;
      final int w;
      int y = 6;

      Rows(final int w) {
         this.w = w;
      }

      <T extends AbstractWidget> T add(T widget, int height) {
         if (widget instanceof EditorTextField) {
            PropertiesPanel.this.panel.add(widget, 12, this.y + 5);
         } else {
            PropertiesPanel.this.panel.add(widget, 6, this.y);
         }

         this.y += height + 4;
         return widget;
      }

      void pair(AbstractWidget a, AbstractWidget b, int height) {
         int half = this.w / 2 - 2;
         this.placeAt(a, 6, this.y, half);
         this.placeAt(b, 6 + half + 4, this.y, half);
         this.y += height + 4;
      }

      private void placeAt(AbstractWidget wd, int px, int py, int width) {
         if (wd instanceof EditorTextField) {
            PropertiesPanel.this.panel.add(wd, px + 6, py + 5);
         } else {
            wd.setWidth(width);
            PropertiesPanel.this.panel.add(wd, px, py);
         }

      }

      void title(Component text, @Nullable Icon icon) {
         int off = icon != null ? 16 : 0;
         PropertiesPanel.this.titleIcon = icon != null ? icon : Icon.PANEL;
         PropertiesPanel.this.titleIconShown = icon != null;
         PropertiesPanel.this.panel.add((new CFHGuiLabel(0, 0, this.w - off, text)).style(CFHGuiLabel.Style.TITLE), 6 + off, this.y);
         this.y += 14;
      }

      void caption(Component text) {
         PropertiesPanel.this.panel.add((new CFHGuiLabel(0, 0, this.w, text)).style(CFHGuiLabel.Style.CAPTION), 6, this.y);
         this.y += 11;
      }

      void note(Component text) {
         CFHGuiLabel label = (new CFHGuiLabel(0, 0, this.w, text)).style(CFHGuiLabel.Style.CAPTION).wrap(true);
         PropertiesPanel.this.panel.add(label, 6, this.y);
         this.y += label.getHeight() + 3;
      }

      void caption2(Component a, Component b) {
         int half = this.w / 2 - 2;
         PropertiesPanel.this.panel.add((new CFHGuiLabel(0, 0, half, a)).style(CFHGuiLabel.Style.CAPTION), 6, this.y);
         PropertiesPanel.this.panel.add((new CFHGuiLabel(0, 0, half, b)).style(CFHGuiLabel.Style.CAPTION), 6 + half + 4, this.y);
         this.y += 11;
      }

      void separator(Component text) {
         this.y += 2;
         PropertiesPanel.this.panel.add(new CFHGuiSeparator(0, 0, this.w, text), 6, this.y);
         this.y += 14;
      }

      void gap(int px) {
         this.y += px;
      }
   }
}
