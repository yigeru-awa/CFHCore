package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

final class EditorCanvas {
   private static final int DRAG_THRESHOLD = 3;
   private static final int GUIDE_TOLERANCE = 4;
   private final Screen screen;
   private final EditorSession session;
   private final Listener listener;
   private final EditorOverlay overlay;
   List<EditorItem> items = new ArrayList();
   Map<String, EditorItem> byId = new HashMap();
   final List<String> selection = new ArrayList();
   @Nullable String hover;
   boolean grid = true;
   boolean snap = true;
   private Mode mode;
   private double pressX;
   private double pressY;
   private int handle;
   private boolean pressShift;
   private boolean fixedSaid;
   private @Nullable String pressedId;
   private final Map<String, Rect> startRects;
   private @Nullable Rect band;
   private final List<Integer> guidesV;
   private final List<Integer> guidesH;

   EditorCanvas(Screen screen, EditorSession session, EditorOverlay overlay, Listener listener) {
      this.mode = EditorCanvas.Mode.NONE;
      this.handle = -1;
      this.startRects = new LinkedHashMap();
      this.guidesV = new ArrayList();
      this.guidesH = new ArrayList();
      this.screen = screen;
      this.session = session;
      this.overlay = overlay;
      this.listener = listener;
   }

   void rebind() {
      try {
         this.items = EditorItems.bind(this.screen, this.session.layout());
      } catch (Exception e) {
         CFHGui.LOGGER.error("[CFHGui] editor: binding items failed", e);
         this.items = new ArrayList();
      }

      this.byId = new HashMap();

      for(EditorItem it : this.items) {
         this.byId.put(it.id, it);
      }

      this.selection.removeIf((id) -> !this.byId.containsKey(id));
      if (this.hover != null && !this.byId.containsKey(this.hover)) {
         this.hover = null;
      }

   }

   @Nullable EditorItem primary() {
      return this.selection.isEmpty() ? null : (EditorItem)this.byId.get(this.selection.get(this.selection.size() - 1));
   }

   @Nullable EditorItem itemOf(ScreenLayout.Element e) {
      for(EditorItem it : this.items) {
         if (it.element == e) {
            return it;
         }
      }

      return null;
   }

   @Nullable EditorItem itemOfKey(String key) {
      return (EditorItem)this.byId.get("w:" + key);
   }

   List<EditorItem> selected() {
      List<EditorItem> out = new ArrayList();

      for(String id : this.selection) {
         EditorItem it = (EditorItem)this.byId.get(id);
         if (it != null) {
            out.add(it);
         }
      }

      return out;
   }

   private List<EditorItem> selectedMovable() {
      List<EditorItem> out = new ArrayList();

      for(EditorItem it : this.selected()) {
         if (it.movable()) {
            out.add(it);
         }
      }

      return out;
   }

   void select(String id, boolean additive) {
      if (this.byId.containsKey(id)) {
         if (!additive) {
            this.selection.clear();
         }

         this.selection.remove(id);
         this.selection.add(id);
         this.listener.selectionChanged();
      }
   }

   void toggleSelect(String id) {
      if (this.selection.remove(id)) {
         this.listener.selectionChanged();
      } else {
         this.select(id, true);
      }
   }

   void clearSelection() {
      if (!this.selection.isEmpty()) {
         this.selection.clear();
         this.listener.selectionChanged();
      }
   }

   void selectAll() {
      this.selection.clear();

      for(EditorItem it : this.items) {
         if (it.hittable()) {
            this.selection.add(it.id);
         }
      }

      this.listener.selectionChanged();
   }

   void selectNext(int dir) {
      List<EditorItem> hit = new ArrayList();

      for(EditorItem it : this.items) {
         if (it.hittable()) {
            hit.add(it);
         }
      }

      if (!hit.isEmpty()) {
         EditorItem cur = this.primary();
         int i = cur == null ? -1 : hit.indexOf(cur);
         i = ((i + dir) % hit.size() + hit.size()) % hit.size();
         this.select(((EditorItem)hit.get(i)).id, false);
      }
   }

   @Nullable EditorItem hitTest(double mx, double my) {
      for(int i = this.items.size() - 1; i >= 0; --i) {
         EditorItem it = (EditorItem)this.items.get(i);
         if (it.hittable() && it.rect.contains(mx, my)) {
            return it;
         }
      }

      return null;
   }

   private int handleAt(double mx, double my) {
      if (this.selection.size() != 1) {
         return -1;
      } else {
         EditorItem it = this.primary();
         if (it != null && it.hittable() && it.movable()) {
            int[][] pts = EditorStyle.handlePoints(it.rect);

            for(int i = 0; i < pts.length; ++i) {
               if (Math.abs(mx - (double)pts[i][0]) <= (double)3.0F && Math.abs(my - (double)pts[i][1]) <= (double)3.0F) {
                  return i;
               }
            }

            return -1;
         } else {
            return -1;
         }
      }
   }

   void mouseClicked(double mx, double my, int button) {
      if (button == 1) {
         this.contextMenu(mx, my);
      } else if (button == 0) {
         this.pressX = mx;
         this.pressY = my;
         this.guidesV.clear();
         this.guidesH.clear();
         this.startRects.clear();
         this.pressShift = EditorKeys.shift();
         int h = this.handleAt(mx, my);
         if (h >= 0) {
            EditorItem it = this.primary();
            if (it != null) {
               this.mode = EditorCanvas.Mode.RESIZE;
               this.handle = h;
               this.pressedId = it.id;
               this.startRects.put(it.id, it.rect);
               this.session.pushUndo((String)null);
            }
         } else {
            EditorItem hit = this.hitTest(mx, my);
            if (hit == null) {
               if (!this.pressShift) {
                  this.clearSelection();
               }

               this.mode = EditorCanvas.Mode.BAND;
               this.band = null;
            } else {
               if (this.pressShift) {
                  if (this.selection.contains(hit.id)) {
                     this.selection.remove(hit.id);
                     this.listener.selectionChanged();
                     this.mode = EditorCanvas.Mode.NONE;
                     return;
                  }

                  this.selection.add(hit.id);
               } else if (!this.selection.contains(hit.id)) {
                  this.selection.clear();
                  this.selection.add(hit.id);
               } else {
                  this.selection.remove(hit.id);
                  this.selection.add(hit.id);
               }

               this.listener.selectionChanged();
               this.mode = EditorCanvas.Mode.PRESS;
               this.pressedId = hit.id;
               this.fixedSaid = false;

               for(EditorItem it : this.selectedMovable()) {
                  if (it.hittable()) {
                     this.startRects.put(it.id, it.rect);
                  }
               }

            }
         }
      }
   }

   void mouseDragged(double mx, double my) {
      switch (this.mode.ordinal()) {
         case 1:
            if (Math.abs(mx - this.pressX) < (double)3.0F && Math.abs(my - this.pressY) < (double)3.0F) {
               return;
            }

            if (this.startRects.isEmpty()) {
               EditorItem pressed = this.pressedId == null ? null : (EditorItem)this.byId.get(this.pressedId);
               if (!this.fixedSaid && pressed != null) {
                  this.fixedSaid = true;
                  CFHGuiToasts.show(EditorText.t(pressed.locked() ? "toast.locked" : "toast.fixed"), pressed.name(), Icon.LOCK);
               }

               return;
            }

            this.mode = EditorCanvas.Mode.MOVE;
            this.session.pushUndo((String)null);
            this.dragMove(mx, my);
            break;
         case 2:
            this.dragMove(mx, my);
            break;
         case 3:
            this.dragResize(mx, my);
            break;
         case 4:
            int x0 = (int)Math.min(this.pressX, mx);
            int y0 = (int)Math.min(this.pressY, my);
            int x1 = (int)Math.max(this.pressX, mx);
            int y1 = (int)Math.max(this.pressY, my);
            this.band = Rect.of(x0, y0, x1 - x0, y1 - y0);
      }

   }

   void mouseReleased(double mx, double my) {
      switch (this.mode.ordinal()) {
         case 2:
         case 3:
            for(Map.Entry<String, Rect> en : this.startRects.entrySet()) {
               EditorItem it = (EditorItem)this.byId.get(en.getKey());
               if (it != null) {
                  this.commitRect(it, it.rect, this.mode == EditorCanvas.Mode.RESIZE, (Rect)en.getValue());
               }
            }

            this.overlay.applyLive();
            break;
         case 4:
            if (this.band != null && (this.band.w() > 2 || this.band.h() > 2)) {
               if (!this.pressShift) {
                  this.selection.clear();
               }

               for(EditorItem it : this.items) {
                  if (it.hittable() && intersects(it.rect, this.band)) {
                     this.selection.remove(it.id);
                     this.selection.add(it.id);
                  }
               }

               this.listener.selectionChanged();
            }

            this.band = null;
      }

      this.mode = EditorCanvas.Mode.NONE;
      this.handle = -1;
      this.startRects.clear();
      this.guidesV.clear();
      this.guidesH.clear();
   }

   boolean dragging() {
      return this.mode == EditorCanvas.Mode.MOVE || this.mode == EditorCanvas.Mode.RESIZE;
   }

   private static boolean intersects(Rect a, Rect b) {
      return a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y();
   }

   private void dragMove(double mx, double my) {
      double dx = mx - this.pressX;
      double dy = my - this.pressY;
      if (EditorKeys.shift()) {
         if (Math.abs(dx) > Math.abs(dy)) {
            dy = (double)0.0F;
         } else {
            dx = (double)0.0F;
         }
      }

      int ndx = (int)Math.round(dx);
      int ndy = (int)Math.round(dy);
      Rect ref = this.pressedId != null && this.startRects.containsKey(this.pressedId) ? (Rect)this.startRects.get(this.pressedId) : (Rect)this.startRects.values().iterator().next();
      int[] adj = this.snapAdjust(ref.move(ndx, ndy), true, true, true, true, this.startRects.keySet());
      ndx += adj[0];
      ndy += adj[1];

      for(Map.Entry<String, Rect> en : this.startRects.entrySet()) {
         EditorItem it = (EditorItem)this.byId.get(en.getKey());
         if (it != null) {
            this.applyLive(it, ((Rect)en.getValue()).move(ndx, ndy));
         }
      }

   }

   private void dragResize(double mx, double my) {
      EditorItem it = this.primary();
      if (it != null && this.startRects.containsKey(it.id)) {
         Rect s = (Rect)this.startRects.get(it.id);
         int dx = (int)Math.round(mx - this.pressX);
         int dy = (int)Math.round(my - this.pressY);
         int x = s.x();
         int y = s.y();
         int r = s.right();
         int b = s.bottom();
         boolean left = this.handle == 0 || this.handle == 6 || this.handle == 7;
         boolean right = this.handle == 2 || this.handle == 3 || this.handle == 4;
         boolean top = this.handle == 0 || this.handle == 1 || this.handle == 2;
         boolean bottom = this.handle == 4 || this.handle == 5 || this.handle == 6;
         if (left) {
            x += dx;
         }

         if (right) {
            r += dx;
         }

         if (top) {
            y += dy;
         }

         if (bottom) {
            b += dy;
         }

         Rect cand = Rect.of(Math.min(x, r), Math.min(y, b), Math.abs(r - x), Math.abs(b - y));
         int[] adj = this.snapAdjust(cand, left, right, top, bottom, Set.of(it.id));
         if (left) {
            x += adj[0];
         } else if (right) {
            r += adj[0];
         }

         if (top) {
            y += adj[1];
         } else if (bottom) {
            b += adj[1];
         }

         if (r - x < 2) {
            if (left) {
               x = r - 2;
            } else {
               r = x + 2;
            }
         }

         if (b - y < 2) {
            if (top) {
               y = b - 2;
            } else {
               b = y + 2;
            }
         }

         this.applyLive(it, Rect.of(x, y, r - x, b - y));
      }
   }

   private void applyLive(EditorItem it, Rect r) {
      it.rect = r;
      if (it.widget != null) {
         it.widget.setX(r.x());
         it.widget.setY(r.y());
         it.widget.setWidth(Math.max(1, r.w()));
         it.widget.setHeight(Math.max(1, r.h()));
      }

   }

   private int[] snapAdjust(Rect r, boolean left, boolean right, boolean top, boolean bottom, Set<String> exclude) {
      this.guidesV.clear();
      this.guidesH.clear();
      if (EditorKeys.alt()) {
         return new int[]{0, 0};
      } else {
         int sw = this.screen.width;
         int sh = this.screen.height;
         List<Integer> vs = new ArrayList(List.of(0, sw / 2, sw));
         List<Integer> hs = new ArrayList(List.of(0, sh / 2, sh));

         for(EditorItem it : this.items) {
            if (!exclude.contains(it.id) && it.hittable()) {
               Rect o = it.rect;
               vs.add(o.x());
               vs.add(o.x() + o.w() / 2);
               vs.add(o.right());
               hs.add(o.y());
               hs.add(o.y() + o.h() / 2);
               hs.add(o.bottom());
            }
         }

         boolean moveX = left && right;
         boolean moveY = top && bottom;
         int[] edgesX = moveX ? new int[]{r.x(), r.x() + r.w() / 2, r.right()} : (left ? new int[]{r.x()} : (right ? new int[]{r.right()} : new int[0]));
         int[] edgesY = moveY ? new int[]{r.y(), r.y() + r.h() / 2, r.bottom()} : (top ? new int[]{r.y()} : (bottom ? new int[]{r.bottom()} : new int[0]));
         int adjX = best(vs, edgesX);
         int adjY = best(hs, edgesY);
         int gridStep = Math.max(1, CFHGui.config().devSnap);
         if (adjX == Integer.MIN_VALUE) {
            adjX = this.snap && edgesX.length > 0 ? snapTo(edgesX[0], gridStep) - edgesX[0] : 0;
         }

         if (adjY == Integer.MIN_VALUE) {
            adjY = this.snap && edgesY.length > 0 ? snapTo(edgesY[0], gridStep) - edgesY[0] : 0;
         }

         for(int e : edgesX) {
            if (vs.contains(e + adjX)) {
               this.guidesV.add(e + adjX);
            }
         }

         for(int e : edgesY) {
            if (hs.contains(e + adjY)) {
               this.guidesH.add(e + adjY);
            }
         }

         return new int[]{adjX, adjY};
      }
   }

   private static int best(List<Integer> lines, int[] edges) {
      int best = Integer.MIN_VALUE;

      for(int line : lines) {
         for(int e : edges) {
            int d = line - e;
            if (Math.abs(d) <= 4 && (best == Integer.MIN_VALUE || Math.abs(d) < Math.abs(best))) {
               best = d;
            }
         }
      }

      return best;
   }

   private static int snapTo(int v, int step) {
      return Math.round((float)v / (float)step) * step;
   }

   Anchor currentAnchor(EditorItem it) {
      if (it.element != null) {
         return Anchor.parse(it.element.place.anchor, Anchor.TOP_LEFT);
      } else {
         ScreenLayout.Placement p = it.key == null ? null : (ScreenLayout.Placement)this.session.layout().moved.get(it.key);
         return p != null ? Anchor.parse(p.anchor, Anchor.TOP_LEFT) : Anchor.nearest(it.rect.centerX(), it.rect.centerY(), this.screen.width, this.screen.height);
      }
   }

   boolean anchorIsAuto(EditorItem it, Rect at) {
      if (it.element != null || it.key != null && this.session.layout().moved.containsKey(it.key)) {
         return this.currentAnchor(it) == Anchor.nearest(at.centerX(), at.centerY(), this.screen.width, this.screen.height);
      } else {
         return true;
      }
   }

   void commitRect(EditorItem it, Rect r, boolean resized, Rect before) {
      int sw = this.screen.width;
      int sh = this.screen.height;
      Anchor a = this.anchorIsAuto(it, before) ? Anchor.nearest(r.centerX(), r.centerY(), sw, sh) : this.currentAnchor(it);
      this.writePlacement(it, r, a, resized);
   }

   private void writePlacement(EditorItem it, Rect r, Anchor a, boolean resized) {
      ScreenLayout.Placement p;
      if (it.element != null) {
         p = it.element.place;
      } else {
         if (it.key == null) {
            return;
         }

         p = (ScreenLayout.Placement)this.session.layout().moved.computeIfAbsent(it.key, (k) -> new ScreenLayout.Placement());
      }

      p.anchor = a.name();
      p.x = a.offsetX(this.screen.width, r.x(), r.w());
      p.y = a.offsetY(this.screen.height, r.y(), r.h());
      if (resized || p.w > 0) {
         p.w = r.w();
      }

      if (resized || p.h > 0) {
         p.h = r.h();
      }

      it.rect = r;
   }

   void setAnchor(EditorItem it, @Nullable Anchor anchor) {
      this.session.pushUndo((String)null);
      Anchor a = anchor != null ? anchor : Anchor.nearest(it.rect.centerX(), it.rect.centerY(), this.screen.width, this.screen.height);
      this.writePlacement(it, it.rect, a, false);
      this.overlay.applyLive();
   }

   void setGeometry(EditorItem it, Rect r, boolean resized, String undoKey) {
      this.session.pushUndo(undoKey);
      this.commitRect(it, r, resized, it.rect);
      this.overlay.applyLive();
   }

   void nudge(int dx, int dy) {
      List<EditorItem> sel = this.selectedMovable();
      if (!sel.isEmpty()) {
         this.session.pushUndo("nudge");

         for(EditorItem it : sel) {
            this.commitRect(it, it.rect.move(dx, dy), false, it.rect);
         }

         this.overlay.applyLive();
      }
   }

   void align(Align a) {
      List<EditorItem> sel = this.selectedMovable();
      if (!sel.isEmpty()) {
         Rect b;
         if (sel.size() >= 2) {
            int x0 = Integer.MAX_VALUE;
            int y0 = Integer.MAX_VALUE;
            int x1 = Integer.MIN_VALUE;
            int y1 = Integer.MIN_VALUE;

            for(EditorItem it : sel) {
               x0 = Math.min(x0, it.rect.x());
               y0 = Math.min(y0, it.rect.y());
               x1 = Math.max(x1, it.rect.right());
               y1 = Math.max(y1, it.rect.bottom());
            }

            b = Rect.of(x0, y0, x1 - x0, y1 - y0);
         } else {
            b = Rect.of(0, 0, this.screen.width, this.screen.height);
         }

         this.session.pushUndo((String)null);

         for(EditorItem it : sel) {
            Rect r = it.rect;
            Rect var10000;
            switch (a.ordinal()) {
               case 0 -> var10000 = r.withX(b.x());
               case 1 -> var10000 = r.withX(b.x() + (b.w() - r.w()) / 2);
               case 2 -> var10000 = r.withX(b.right() - r.w());
               case 3 -> var10000 = r.withY(b.y());
               case 4 -> var10000 = r.withY(b.y() + (b.h() - r.h()) / 2);
               case 5 -> var10000 = r.withY(b.bottom() - r.h());
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            Rect n = var10000;
            this.commitRect(it, n, false, r);
         }

         this.overlay.applyLive();
      }
   }

   void setVisible(EditorItem it, boolean visible) {
      this.session.pushUndo((String)null);
      if (it.element != null) {
         it.element.visible = visible;
      } else if (it.key != null) {
         if (visible) {
            this.session.layout().hidden.remove(it.key);
         } else if (!this.session.layout().hidden.contains(it.key)) {
            this.session.layout().hidden.add(it.key);
         }
      }

      this.overlay.applyLive();
   }

   void setLocked(EditorItem it, boolean locked) {
      if (it.element != null) {
         this.session.pushUndo((String)null);
         it.element.locked = locked;
         this.overlay.applyLive();
      }
   }

   void resetVanilla(EditorItem it) {
      if (it.key != null) {
         this.session.pushUndo((String)null);
         this.session.layout().moved.remove(it.key);
         this.session.layout().hidden.remove(it.key);
         this.overlay.applyLive();
      }
   }

   void deleteSelected() {
      List<EditorItem> sel = this.selected();
      if (!sel.isEmpty()) {
         this.session.pushUndo((String)null);

         for(EditorItem it : sel) {
            if (it.element != null) {
               this.session.layout().elements.remove(it.element);
            } else if (it.key != null && !this.session.layout().hidden.contains(it.key)) {
               this.session.layout().hidden.add(it.key);
            }
         }

         this.selection.clear();
         this.overlay.applyStructural();
         this.listener.selectionChanged();
      }
   }

   void addElement(ElementType type) {
      this.session.pushUndo((String)null);
      ScreenLayout.Element e = this.session.newElement(type);
      this.session.layout().elements.add(e);
      this.selection.clear();
      this.selection.add("e:" + e.id);
      this.overlay.applyStructural();
      this.listener.selectionChanged();
   }

   boolean renameElement(EditorItem it, String newId) {
      if (it.element != null && !newId.isBlank() && !this.session.idInUse(newId, it.element)) {
         this.session.pushUndo("rename:" + System.identityHashCode(it.element));
         it.element.id = newId;
         int i = this.selection.indexOf(it.id);
         if (i >= 0) {
            this.selection.set(i, "e:" + newId);
         }

         this.overlay.applyLive();
         return true;
      } else {
         return false;
      }
   }

   void copy() {
      List<ScreenLayout.Element> out = new ArrayList();

      for(EditorItem it : this.orderedSelectedElements()) {
         out.add(it.element.copy());
      }

      if (!out.isEmpty()) {
         EditorSession.CLIPBOARD.clear();
         EditorSession.CLIPBOARD.addAll(out);
      }
   }

   void paste() {
      if (!EditorSession.CLIPBOARD.isEmpty()) {
         this.pasteElements(EditorSession.CLIPBOARD);
      }
   }

   void duplicate() {
      List<ScreenLayout.Element> src = new ArrayList();

      for(EditorItem it : this.orderedSelectedElements()) {
         src.add(it.element);
      }

      if (!src.isEmpty()) {
         this.pasteElements(src);
      }
   }

   private List<EditorItem> orderedSelectedElements() {
      List<EditorItem> out = new ArrayList();

      for(EditorItem it : this.items) {
         if (it.element != null && this.selection.contains(it.id)) {
            out.add(it);
         }
      }

      return out;
   }

   private void pasteElements(List<ScreenLayout.Element> src) {
      this.session.pushUndo((String)null);
      this.selection.clear();

      for(ScreenLayout.Element s : src) {
         ScreenLayout.Element e = s.copy();
         e.id = this.session.newElementId(s.id);
         ScreenLayout.Placement var10000 = e.place;
         var10000.x += 8;
         var10000 = e.place;
         var10000.y += 8;
         this.session.layout().elements.add(e);
         this.selection.add("e:" + e.id);
      }

      this.overlay.applyStructural();
      this.listener.selectionChanged();
   }

   void reorderElement(EditorItem it, int newIndex) {
      if (it.element != null) {
         List<ScreenLayout.Element> els = this.session.layout().elements;
         int cur = els.indexOf(it.element);
         if (cur >= 0) {
            int target = Math.max(0, Math.min(els.size() - 1, newIndex));
            if (target != cur) {
               this.session.pushUndo((String)null);
               els.remove(cur);
               els.add(target, it.element);
               this.overlay.applyStructural();
            }
         }
      }
   }

   void zOrder(EditorItem it, int delta, boolean extreme) {
      if (it.element != null) {
         List<ScreenLayout.Element> els = this.session.layout().elements;
         int cur = els.indexOf(it.element);
         if (cur >= 0) {
            this.reorderElement(it, extreme ? (delta > 0 ? els.size() - 1 : 0) : cur + delta);
         }
      }
   }

   private void contextMenu(double mx, double my) {
      EditorItem hit = this.hitTest(mx, my);
      List<MenuPopup.Item> menu = new ArrayList();
      if (hit != null) {
         if (!this.selection.contains(hit.id)) {
            this.select(hit.id, false);
         }

         boolean custom = hit.element != null;
         MutableComponent var10001 = EditorText.t("menu.properties");
         Icon var10002 = Icon.SLIDERS;
         EditorOverlay var10003 = this.overlay;
         Objects.requireNonNull(var10003);
         menu.add(MenuPopup.Item.of(var10001, var10002, var10003::showProperties));
         if (custom) {
            menu.add(MenuPopup.Item.of(EditorText.t("menu.duplicate"), Icon.DUPLICATE, this::duplicate));
         }

         menu.add(MenuPopup.Item.of(EditorText.t("menu.copy"), Icon.COPY, this::copy));
         menu.add(EditorSession.CLIPBOARD.isEmpty() ? MenuPopup.Item.disabled(EditorText.t("menu.paste"), Icon.IMPORT) : MenuPopup.Item.of(EditorText.t("menu.paste"), Icon.IMPORT, this::paste));
         menu.add(MenuPopup.Item.sep());
         if (custom) {
            menu.add(MenuPopup.Item.of(EditorText.t("menu.to_front"), Icon.ARROW_UP, () -> this.zOrder(hit, 1, true)));
            menu.add(MenuPopup.Item.of(EditorText.t("menu.forward"), Icon.CHEVRON_UP, () -> this.zOrder(hit, 1, false)));
            menu.add(MenuPopup.Item.of(EditorText.t("menu.backward"), Icon.CHEVRON_DOWN, () -> this.zOrder(hit, -1, false)));
            menu.add(MenuPopup.Item.of(EditorText.t("menu.to_back"), Icon.ARROW_DOWN, () -> this.zOrder(hit, -1, true)));
            menu.add(MenuPopup.Item.sep());
            menu.add(MenuPopup.Item.of(hit.locked() ? EditorText.t("menu.unlock") : EditorText.t("menu.lock"), hit.locked() ? Icon.UNLOCK : Icon.LOCK, () -> this.setLocked(hit, !hit.locked())));
         }

         if (custom) {
            menu.add(MenuPopup.Item.of(EditorText.t("menu.hide"), Icon.EYE_OFF, () -> this.setVisible(hit, false)));
         } else {
            menu.add(MenuPopup.Item.of(EditorText.t("menu.reset_widget"), Icon.REFRESH, () -> this.resetVanilla(hit)));
         }

         menu.add(MenuPopup.Item.danger(EditorText.t(custom ? "menu.delete" : "menu.delete_widget"), Icon.TRASH, this::deleteSelected));
      } else {
         menu.add(MenuPopup.Item.of(EditorText.t("menu.add"), Icon.PLUS, () -> this.overlay.openAddPalette((int)mx, (int)my)));
         menu.add(EditorSession.CLIPBOARD.isEmpty() ? MenuPopup.Item.disabled(EditorText.t("menu.paste"), Icon.IMPORT) : MenuPopup.Item.of(EditorText.t("menu.paste"), Icon.IMPORT, this::paste));
         menu.add(MenuPopup.Item.of(EditorText.t("menu.select_all"), Icon.GRID, this::selectAll));
         menu.add(MenuPopup.Item.sep());
         MutableComponent var8 = EditorText.t("menu.background");
         Icon var9 = Icon.IMAGE;
         EditorOverlay var10 = this.overlay;
         Objects.requireNonNull(var10);
         menu.add(MenuPopup.Item.of(var8, var9, var10::showBackground));
      }

      CFHGuiContextMenu.open(mx, my, menu);
   }

   void render(GuiGraphics g, int mouseX, int mouseY, boolean mouseOverChrome) {
      int sw = this.screen.width;
      int sh = this.screen.height;
      if (this.grid) {
         EditorStyle.grid(g, sw, sh, CFHGui.config().devSnap);
      }

      if (!this.dragging()) {
         for(EditorItem it : this.items) {
            if (it.widget != null) {
               it.rect = Rect.of(it.widget.getX(), it.widget.getY(), it.widget.getWidth(), it.widget.getHeight());
            }
         }
      }

      if (!mouseOverChrome && this.mode != EditorCanvas.Mode.MOVE && this.mode != EditorCanvas.Mode.RESIZE && this.mode != EditorCanvas.Mode.BAND) {
         EditorItem h = this.hitTest((double)mouseX, (double)mouseY);
         this.hover = h == null ? null : h.id;
      } else {
         this.hover = null;
      }

      int phase = EditorStyle.dashPhase();
      int accent = EditorStyle.accent();

      for(String id : this.selection) {
         EditorItem it = (EditorItem)this.byId.get(id);
         if (it != null) {
            Rect r = it.rect;
            if (!it.hittable()) {
               EditorStyle.dashedRect(g, r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, EditorStyle.ghost(), 0);
            } else {
               EditorStyle.dashedRect(g, r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, accent, phase);
               if (it.locked()) {
                  Icons.draw(g, Icon.LOCK, r.right() - 9, r.y() + 1, 8, accent);
               }
            }
         }
      }

      if (this.selection.size() == 1) {
         EditorItem it = this.primary();
         if (it != null && it.hittable() && it.movable()) {
            EditorStyle.handles(g, it.rect);
         }
      }

      if (this.hover != null && !this.selection.contains(this.hover)) {
         EditorItem it = (EditorItem)this.byId.get(this.hover);
         if (it != null) {
            CFHGuiDraw.outline(g, it.rect.x() - 1, it.rect.y() - 1, it.rect.w() + 2, it.rect.h() + 2, EditorStyle.hoverOutline(), 0);
         }
      }

      if (this.hover != null) {
         EditorItem it = (EditorItem)this.byId.get(this.hover);
         if (it != null) {
            MutableComponent var10000;
            if (it.fixed()) {
               var10000 = EditorText.t("label.fixed");
            } else {
               int var30 = it.rect.w();
               var10000 = Component.literal(var30 + "×" + it.rect.h());
            }

            Component size = var10000;
            Component label = Component.empty().append(it.name()).append("  ").append(size.copy().withStyle((s) -> s.withColor(EditorStyle.muted() & 16777215)));
            int ly = it.rect.y() - 14 < 24 ? it.rect.bottom() + 2 : it.rect.y() - 14;
            EditorStyle.labelPill(g, label, Math.max(2, Math.min(it.rect.x(), sw - 60)), ly);
         }
      }

      int guide = EditorStyle.guide();

      for(int x : this.guidesV) {
         CFHGuiDraw.vline(g, x, 0, sh, guide);
      }

      for(int y : this.guidesH) {
         CFHGuiDraw.hline(g, 0, y, sw, guide);
      }

      if ((this.mode == EditorCanvas.Mode.MOVE || this.mode == EditorCanvas.Mode.RESIZE) && this.pressedId != null) {
         EditorItem it = (EditorItem)this.byId.get(this.pressedId);
         if (it != null) {
            Rect r = it.rect;
            int var31 = r.x();
            Component txt = Component.literal(var31 + ", " + r.y() + "   " + r.w() + "×" + r.h());
            EditorStyle.labelPill(g, txt, Math.min(r.right() + 4, sw - 70), Math.min(r.bottom() + 2, sh - 14));
         }
      }

      if (this.mode == EditorCanvas.Mode.BAND && this.band != null) {
         g.fill(this.band.x(), this.band.y(), this.band.right(), this.band.bottom(), EditorStyle.bandFill());
         CFHGuiDraw.outline(g, this.band.x(), this.band.y(), Math.max(1, this.band.w()), Math.max(1, this.band.h()), EditorStyle.hoverOutline(), 0);
      }

   }

   static enum Align {
      LEFT,
      CENTER,
      RIGHT,
      TOP,
      MIDDLE,
      BOTTOM;
   }

   private static enum Mode {
      NONE,
      PRESS,
      MOVE,
      RESIZE,
      BAND;
   }

   interface Listener {
      void selectionChanged();

      void itemsChanged(boolean var1);
   }
}
