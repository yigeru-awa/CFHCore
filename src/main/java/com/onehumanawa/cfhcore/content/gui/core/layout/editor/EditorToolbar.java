package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

final class EditorToolbar {
   private static final int SIZE = 20;
   private static final int GAP = 2;
   private static final int SEP = 5;
   private final EditorOverlay ed;
   private final List<CFHGuiIconButton> left = new ArrayList();
   private final List<Runnable> leftActions = new ArrayList();
   private final List<Icon> leftIcons = new ArrayList();
   private final List<Integer> separatorsAfter = List.of(0, 2, 4, 7);
   private final List<CFHGuiIconButton> right = new ArrayList();
   private final List<AbstractWidget> placed = new ArrayList();
   private final List<Integer> separatorX = new ArrayList();
   private final CFHGuiIconButton undoB;
   private final CFHGuiIconButton redoB;
   private final CFHGuiIconButton gridB;
   private final CFHGuiIconButton snapB;
   private final CFHGuiIconButton layersB;
   private final CFHGuiIconButton propsB;
   private final CFHGuiIconButton previewB;
   private final CFHGuiIconButton saveB;
   private final CFHGuiIconButton moreB;
   private int width;
   private int visibleLeft;
   private int labelX;
   private int labelW;
   private @Nullable AbstractWidget pressed;

   EditorToolbar(EditorOverlay ed) {
      this.ed = ed;
      this.add(Icon.PLUS, "toolbar.add", () -> ed.openAddPalette(4, 24));
      Icon var10002 = Icon.UNDO;
      Objects.requireNonNull(ed);
      this.undoB = this.add(var10002, "toolbar.undo", ed::undo);
      var10002 = Icon.REDO;
      Objects.requireNonNull(ed);
      this.redoB = this.add(var10002, "toolbar.redo", ed::redo);
      var10002 = Icon.GRID;
      Objects.requireNonNull(ed);
      this.gridB = this.add(var10002, "toolbar.grid", ed::toggleGrid);
      var10002 = Icon.SNAP;
      Objects.requireNonNull(ed);
      this.snapB = this.add(var10002, "toolbar.snap", ed::toggleSnap);
      var10002 = Icon.LAYERS;
      Objects.requireNonNull(ed);
      this.layersB = this.add(var10002, "toolbar.layers", ed::toggleLayers);
      var10002 = Icon.SLIDERS;
      Objects.requireNonNull(ed);
      this.propsB = this.add(var10002, "toolbar.properties", ed::toggleProps);
      var10002 = Icon.EYE;
      Objects.requireNonNull(ed);
      this.previewB = this.add(var10002, "toolbar.preview", ed::togglePreview);
      Icon var10001 = Icon.IMAGE;
      Objects.requireNonNull(ed);
      this.add(var10001, "toolbar.background", ed::showBackground);
      var10001 = Icon.REFRESH;
      Objects.requireNonNull(ed);
      this.add(var10001, "toolbar.reset", ed::resetScreen);
      var10001 = Icon.EXPORT;
      Objects.requireNonNull(ed);
      this.add(var10001, "toolbar.export", ed::exportLayout);
      var10001 = Icon.IMPORT;
      Objects.requireNonNull(ed);
      this.add(var10001, "toolbar.import", ed::importLayout);
      var10001 = Icon.MONITOR;
      Objects.requireNonNull(ed);
      this.add(var10001, "toolbar.new_screen", ed::newCustomScreen);
      Icon var10006 = Icon.SAVE;
      MutableComponent var10007 = EditorText.t("toolbar.save");
      Objects.requireNonNull(ed);
      this.saveB = new CFHGuiIconButton(0, 0, 20, var10006, var10007, ed::save);
      this.right.add(this.saveB);
      this.right.add(new CFHGuiIconButton(0, 0, 20, Icon.EXIT, EditorText.t("toolbar.exit"), () -> ed.requestExit((Runnable)null)));
      this.moreB = new CFHGuiIconButton(0, 0, 20, Icon.DOTS, EditorText.t("toolbar.more"), this::openMore);
   }

   private CFHGuiIconButton add(Icon icon, String key, Runnable action) {
      CFHGuiIconButton b = new CFHGuiIconButton(0, 0, 20, icon, EditorText.t(key), action);
      this.left.add(b);
      this.leftActions.add(action);
      this.leftIcons.add(icon);
      return b;
   }

   private int leftWidth(int n) {
      int w = n * 22;

      for(int s : this.separatorsAfter) {
         if (s < n - 1) {
            w += 5;
         }
      }

      return w;
   }

   void relayout(int width) {
      this.width = width;
      int y = 2;
      this.placed.clear();
      this.separatorX.clear();
      int rightW = this.right.size() * 22;
      int rx = width - 4;

      for(int i = this.right.size() - 1; i >= 0; --i) {
         rx -= 20;
         ((CFHGuiIconButton)this.right.get(i)).setX(rx);
         ((CFHGuiIconButton)this.right.get(i)).setY(2);
         rx -= 2;
      }

      int avail = width - 8 - rightW - 64;
      this.visibleLeft = this.left.size();
      if (this.leftWidth(this.visibleLeft) > avail) {
         for(int var11 = avail - 22; this.visibleLeft > 1 && this.leftWidth(this.visibleLeft) > var11; --this.visibleLeft) {
         }
      }

      int x = 4;

      for(int i = 0; i < this.visibleLeft; ++i) {
         CFHGuiIconButton b = (CFHGuiIconButton)this.left.get(i);
         b.setX(x);
         b.setY(2);
         this.placed.add(b);
         x += 22;
         if (this.separatorsAfter.contains(i) && i < this.visibleLeft - 1) {
            this.separatorX.add(x + 1);
            x += 5;
         }
      }

      if (this.visibleLeft < this.left.size()) {
         this.moreB.setX(x);
         this.moreB.setY(2);
         this.placed.add(this.moreB);
         x += 22;
      }

      this.labelX = x + 4;
      this.labelW = width - rightW - 8 - this.labelX;
      this.placed.addAll(this.right);
   }

   private void openMore() {
      List<MenuPopup.Item> items = new ArrayList();

      for(int i = this.visibleLeft; i < this.left.size(); ++i) {
         items.add(MenuPopup.Item.of(((CFHGuiIconButton)this.left.get(i)).getMessage(), (Icon)this.leftIcons.get(i), (Runnable)this.leftActions.get(i)));
      }

      Popups.open(new MenuPopup(this.moreB.getX(), 24, items, 140));
   }

   boolean contains(double mx, double my) {
      return my >= (double)0.0F && my < (double)24.0F && mx >= (double)0.0F && mx < (double)this.width;
   }

   void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      EditorSession s = this.ed.session();
      this.undoB.enabled(s.canUndo());
      this.redoB.enabled(s.canRedo());
      this.gridB.toggled(this.ed.canvas().grid);
      this.snapB.toggled(this.ed.canvas().snap);
      this.layersB.toggled(this.ed.showLayers());
      this.propsB.toggled(this.ed.showProps());
      this.previewB.toggled(false);
      this.saveB.enabled(true);
      EditorStyle.panel(g, 0, 0, this.width, 24);

      for(int sx : this.separatorX) {
         CFHGuiDraw.vline(g, sx, 5, 14, Colors.withAlpha(EditorStyle.panelBorder(), 192));
      }

      for(AbstractWidget w : this.placed) {
         w.render(g, mouseX, mouseY, partialTick);
      }

      if (this.labelW > 30) {
         String id = s.layoutId;
         String name = id.startsWith("custom:") ? id.substring(7) : ScreenIds.display(id);
         Component label = Component.literal(name + (s.dirty() ? " •" : ""));
         int nw = Math.min(CFHGuiDraw.width(label), this.labelW);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(label, this.labelW), this.labelX, 8, EditorStyle.text(), EditorStyle.vanilla());
         int rest = this.labelW - nw - 6;
         if (rest > 40) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Component.literal(id), rest), this.labelX + nw + 6, 8, EditorStyle.dim(), false);
         }
      }

   }

   boolean mouseClicked(double mx, double my, int button) {
      this.pressed = null;

      for(AbstractWidget w : this.placed) {
         if (w.mouseClicked(mx, my, button)) {
            this.pressed = w;
            return true;
         }
      }

      return false;
   }

   void mouseReleased(double mx, double my, int button) {
      if (this.pressed != null) {
         this.pressed.mouseReleased(mx, my, button);
      }

      this.pressed = null;
   }
}
