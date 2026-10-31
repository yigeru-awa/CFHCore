package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CustomScreen;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementTypes;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutStore;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class EditorOverlay implements EditorCanvas.Listener {
   private final Screen screen;
   private final EditorSession session;
   private final EditorCanvas canvas;
   private final EditorToolbar toolbar;
   private final PropertiesPanel props;
   private final LayersPanel layers;
   private boolean showProps = true;
   private boolean showLayers;
   private boolean preview;
   private boolean exiting;
   private boolean broken;
   private @Nullable Object pressTarget;
   private int lastW = -1;
   private int lastH = -1;
   private long lastErrorMs;
   private int errorCount;

   public EditorOverlay(Screen screen) {
      this.screen = screen;
      EditorSession resumed = EditorSession.takeResumable(screen);
      this.session = resumed != null ? resumed : new EditorSession(screen);
      this.canvas = new EditorCanvas(screen, this.session, this, this);
      this.toolbar = new EditorToolbar(this);
      this.props = new PropertiesPanel(this);
      this.layers = new LayersPanel(this);

      try {
         this.canvas.grid = CFHGui.config().devGrid;
         this.canvas.snap = true;
         this.showLayers = screen.width >= 800;
         if (resumed != null) {
            this.canvas.grid = resumed.grid;
            this.canvas.snap = resumed.snap;
            this.showProps = resumed.showProps;
            this.showLayers = resumed.showLayers;
            this.canvas.selection.addAll(resumed.selection);
         }

         this.canvas.rebind();
         this.relayout();
         this.props.rebuild();
         this.layers.refresh();
      } catch (Exception e) {
         CFHGui.LOGGER.error("[CFHGui] editor could not start on {}", this.session.layoutId, e);
      }

   }

   public Screen screen() {
      return this.screen;
   }

   EditorSession session() {
      return this.session;
   }

   EditorCanvas canvas() {
      return this.canvas;
   }

   boolean showLayers() {
      return this.showLayers;
   }

   boolean showProps() {
      return this.showProps;
   }

   public boolean hidesScreenMouse() {
      return !this.preview;
   }

   public void onInit() {
      try {
         this.relayout();
         this.canvas.rebind();
         this.props.rebuild();
         this.layers.refresh();
      } catch (Exception e) {
         this.fail(e);
      }

   }

   public void close() {
      if (!this.exiting) {
         try {
            this.session.selection = new ArrayList(this.canvas.selection);
            this.session.showProps = this.showProps;
            this.session.showLayers = this.showLayers;
            this.session.grid = this.canvas.grid;
            this.session.snap = this.canvas.snap;
            if (!this.session.dirty()) {
               return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != this.screen) {
               CFHGuiToasts.show(EditorText.t("toast.unsaved_kept"), Component.literal(this.session.layoutId), Icon.WARNING);
               return;
            }

            Popups.closeAll();
            EditorSession.setResumable(this.session);
            askSaveDiscard(() -> {
               this.session.save();
               EditorSession.clearResumable(this.session);
               this.toastSaved();
            }, () -> {
               this.session.discard();
               EditorSession.clearResumable(this.session);
            }, () -> LayoutEditor.start(this.screen));
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] editor close failed", e);
         }

      }
   }

   private void relayout() {
      this.lastW = this.screen.width;
      this.lastH = this.screen.height;
      this.toolbar.relayout(this.screen.width);
      int top = 24;
      int h = this.screen.height - 24;
      this.layers.setBounds(Rect.of(0, 24, Math.min(150, this.screen.width / 3), h));
      int pw = Math.min(186, this.screen.width / 2);
      this.props.setBounds(Rect.of(this.screen.width - pw, 24, pw, h));
   }

   private void fail(Exception e) {
      ++this.errorCount;
      long now = Clock.nowMs();
      if (now - this.lastErrorMs > 2000L) {
         this.lastErrorMs = now;
         CFHGui.LOGGER.error("[CFHGui] layout editor error ({} so far)", this.errorCount, e);
      }

      if (this.errorCount >= 8 && !this.broken) {
         this.broken = true;
         CFHGuiToasts.show(EditorText.t("toast.editor_failed"), Component.literal(e.toString()), Icon.ERROR);
         this.exiting = true;
         LayoutEditor.stop();
      }

   }

   public void selectionChanged() {
      this.props.rebuild();
      this.layers.refresh();
   }

   public void itemsChanged(boolean structural) {
      if (structural) {
         this.props.rebuild();
      } else {
         this.props.refreshGeometry();
      }

      this.layers.refresh();
   }

   void applyLive() {
      this.session.reapply();
      this.canvas.rebind();
      this.itemsChanged(false);
   }

   void applyStructural() {
      this.session.reapply();
      this.canvas.rebind();
      this.itemsChanged(true);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (!this.broken) {
         try {
            if (this.screen.width != this.lastW || this.screen.height != this.lastH) {
               this.onInit();
            }

            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 150.0F);
            if (this.preview) {
               Component hint = EditorText.t("preview.hint");
               int w = CFHGuiDraw.width(hint) + 10;
               CFHGuiDraw.pixelRound(g, this.screen.width - w - 4, this.screen.height - 16, w, 12, Colors.withAlpha(EditorStyle.panelFill(), 144), EditorStyle.radius() > 0 ? 2 : 0);
               g.drawString(CFHGuiDraw.font(), hint, this.screen.width - w + 1, this.screen.height - 14, Colors.withAlpha(EditorStyle.muted(), 192), false);
               g.pose().popPose();
               return;
            }

            this.canvas.render(g, mouseX, mouseY, this.chromeContains((double)mouseX, (double)mouseY));
            boolean dragging = this.canvas.dragging();
            if (this.showLayers && !dragging) {
               this.layers.render(g, mouseX, mouseY, partialTick);
            }

            if (this.showProps && !dragging) {
               this.props.render(g, mouseX, mouseY, partialTick);
            }

            this.toolbar.render(g, mouseX, mouseY, partialTick);
            g.pose().popPose();
         } catch (Exception e) {
            this.fail(e);
         }

      }
   }

   private boolean chromeContains(double mx, double my) {
      return this.toolbar.contains(mx, my) || this.showLayers && this.layers.contains(mx, my) || this.showProps && this.props.contains(mx, my);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.broken) {
         return false;
      } else {
         try {
            if (this.preview) {
               this.preview = false;
               return true;
            }

            this.pressTarget = null;
            if (this.toolbar.contains(mouseX, mouseY)) {
               this.props.clearFocus();
               this.toolbar.mouseClicked(mouseX, mouseY, button);
               this.pressTarget = this.toolbar;
               return true;
            }

            if (this.showProps && this.props.contains(mouseX, mouseY)) {
               this.props.mouseClicked(mouseX, mouseY, button);
               this.pressTarget = this.props;
               return true;
            }

            if (this.showLayers && this.layers.contains(mouseX, mouseY)) {
               this.props.clearFocus();
               this.layers.mouseClicked(mouseX, mouseY, button);
               this.pressTarget = this.layers;
               return true;
            }

            this.props.clearFocus();
            this.canvas.mouseClicked(mouseX, mouseY, button);
            this.pressTarget = this.canvas;
         } catch (Exception e) {
            this.fail(e);
         }

         return true;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.broken) {
         return false;
      } else {
         try {
            Object t = this.pressTarget;
            this.pressTarget = null;
            if (t == this.toolbar) {
               this.toolbar.mouseReleased(mouseX, mouseY, button);
            } else if (t == this.props) {
               this.props.mouseReleased(mouseX, mouseY, button);
            } else if (t == this.layers) {
               this.layers.mouseReleased(mouseX, mouseY);
            } else if (t == this.canvas) {
               this.canvas.mouseReleased(mouseX, mouseY);
            }
         } catch (Exception e) {
            this.fail(e);
         }

         return true;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.broken) {
         return false;
      } else {
         try {
            if (this.pressTarget == this.props) {
               this.props.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            } else if (this.pressTarget == this.layers) {
               this.layers.mouseDragged(mouseX, mouseY);
            } else if (this.pressTarget == this.canvas) {
               this.canvas.mouseDragged(mouseX, mouseY);
            }
         } catch (Exception e) {
            this.fail(e);
         }

         return true;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (!this.broken && !this.preview) {
         try {
            if (this.showProps && this.props.contains(mouseX, mouseY)) {
               this.props.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            } else if (this.showLayers && this.layers.contains(mouseX, mouseY)) {
               this.layers.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
         } catch (Exception e) {
            this.fail(e);
         }

         return true;
      } else {
         return !this.broken;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.broken) {
         return false;
      } else {
         try {
            if (this.preview) {
               this.preview = false;
               return true;
            }

            if (keyCode >= 290 && keyCode <= 301) {
               return false;
            }

            if (this.showProps && this.props.textInputFocused()) {
               if (keyCode != 256 && (!EditorKeys.isEnter(keyCode) || !this.props.singleLineFocused())) {
                  this.props.keyPressed(keyCode, scanCode, modifiers);
                  return true;
               }

               this.props.clearFocus();
               return true;
            }

            if (this.showProps && this.props.hasFocus() && this.props.keyPressed(keyCode, scanCode, modifiers)) {
               return true;
            }

            boolean ctrl = EditorKeys.ctrl();
            boolean shift = EditorKeys.shift();
            switch (keyCode) {
               case 65:
                  if (ctrl) {
                     this.canvas.selectAll();
                  }
                  break;
               case 67:
                  if (ctrl) {
                     this.canvas.copy();
                  }
                  break;
               case 68:
                  if (ctrl) {
                     this.canvas.duplicate();
                  }
                  break;
               case 71:
                  if (!ctrl) {
                     this.toggleGrid();
                  }
                  break;
               case 76:
                  if (!ctrl) {
                     this.toggleLayers();
                  }
                  break;
               case 80:
                  if (!ctrl) {
                     this.togglePreview();
                  }
                  break;
               case 83:
                  if (ctrl) {
                     this.save();
                  }
                  break;
               case 86:
                  if (ctrl) {
                     this.canvas.paste();
                  }
                  break;
               case 89:
                  if (ctrl) {
                     this.redo();
                  }
                  break;
               case 90:
                  if (ctrl) {
                     if (shift) {
                        this.redo();
                     } else {
                        this.undo();
                     }
                  }
                  break;
               case 256:
                  if (!this.canvas.selection.isEmpty()) {
                     this.canvas.clearSelection();
                  } else {
                     this.requestExit((Runnable)null);
                  }
                  break;
               case 258:
                  this.canvas.selectNext(shift ? -1 : 1);
                  break;
               case 259:
               case 261:
                  this.canvas.deleteSelected();
                  break;
               case 262:
                  this.canvas.nudge(shift ? 8 : 1, 0);
                  break;
               case 263:
                  this.canvas.nudge(shift ? -8 : -1, 0);
                  break;
               case 264:
                  this.canvas.nudge(0, shift ? 8 : 1);
                  break;
               case 265:
                  this.canvas.nudge(0, shift ? -8 : -1);
            }
         } catch (Exception e) {
            this.fail(e);
         }

         return true;
      }
   }

   public boolean charTyped(char c, int modifiers) {
      if (this.broken) {
         return false;
      } else {
         try {
            if (this.preview) {
               this.preview = false;
               return true;
            }

            if (this.showProps && this.props.textInputFocused()) {
               this.props.charTyped(c, modifiers);
            }
         } catch (Exception e) {
            this.fail(e);
         }

         return true;
      }
   }

   void undo() {
      if (this.session.undo()) {
         this.session.reapply();
         this.canvas.rebind();
         this.itemsChanged(true);
      }

   }

   void redo() {
      if (this.session.redo()) {
         this.session.reapply();
         this.canvas.rebind();
         this.itemsChanged(true);
      }

   }

   void save() {
      this.session.save();
      this.toastSaved();
   }

   public void saveNow() {
      this.save();
   }

   public void harnessPick(String key, boolean show) {
      EditorItem it = this.canvas.itemOfKey(key);
      if (it == null) {
         CFHGui.LOGGER.warn("[CFHGui] editor: nothing with the key {} on {}", key, this.session.layoutId);
      } else {
         if (show) {
            this.canvas.setVisible(it, true);
         } else {
            this.canvas.select(it.id, false);
         }

      }
   }

   private void toastSaved() {
      CFHGuiToasts.show(EditorText.t("toast.saved"), Component.literal(LayoutStore.fileFor(this.session.layoutId).getFileName().toString()), Icon.SAVE);
   }

   void toggleGrid() {
      this.canvas.grid = !this.canvas.grid;
      boolean g = this.canvas.grid;

      try {
         CFHGui.configFile().update((c) -> c.devGrid = g);
      } catch (Exception var3) {
      }

   }

   void toggleSnap() {
      this.canvas.snap = !this.canvas.snap;
   }

   void toggleLayers() {
      this.showLayers = !this.showLayers;
   }

   void toggleProps() {
      this.showProps = !this.showProps;
      if (this.showProps) {
         this.props.rebuild();
      }

   }

   void togglePreview() {
      this.preview = !this.preview;
      if (this.preview) {
         Popups.closeAll();
      }

   }

   void showProperties() {
      this.showProps = true;
      this.props.rebuild();
   }

   void showBackground() {
      this.canvas.clearSelection();
      this.showProps = true;
      this.props.rebuild();
   }

   void openAddPalette(int x, int y) {
      List<MenuPopup.Item> items = new ArrayList();

      for(ElementType type : ElementTypes.all()) {
         items.add(MenuPopup.Item.of(type.label(), type.icon(), () -> {
            this.canvas.addElement(type);
            this.showProperties();
         }));
      }

      if (items.isEmpty()) {
         items.add(MenuPopup.Item.disabled(EditorText.t("palette.empty"), (Icon)null));
      }

      Popups.open(new MenuPopup(x, y, items, 130));
   }

   void resetScreen() {
      CFHGuiModal.confirmDanger(EditorText.t("reset.title"), EditorText.t("reset.body"), EditorText.t("reset.ok"), () -> {
         this.session.replaceWith(new ScreenLayout());
         this.canvas.selection.clear();
         this.applyStructural();
      });
   }

   void exportLayout() {
      try {
         Minecraft.getInstance().keyboardHandler.setClipboard(EditorSession.json(this.session.layout()));
         CFHGuiToasts.show(EditorText.t("toast.exported"), Component.literal(this.session.layoutId), Icon.EXPORT);
      } catch (Exception e) {
         CFHGuiToasts.show(EditorText.t("toast.export_failed"), Component.literal(e.toString()), Icon.ERROR);
      }

   }

   void importLayout() {
      ScreenLayout parsed = null;

      try {
         String json = Minecraft.getInstance().keyboardHandler.getClipboard();
         if (json != null && !json.isBlank()) {
            parsed = (ScreenLayout)JsonConfig.GSON.fromJson(json, ScreenLayout.class);
         }
      } catch (Exception var3) {
      }

      if (parsed == null) {
         CFHGuiToasts.show(EditorText.t("toast.import_invalid"), (Component)null, Icon.WARNING);
      } else {
         ScreenLayout imported = parsed;
         CFHGuiModal.confirm(EditorText.t("import.title"), EditorText.t("import.body"), EditorText.t("import.ok"), () -> {
            this.session.replaceWith(imported);
            this.canvas.selection.clear();
            this.applyStructural();
            CFHGuiToasts.show(EditorText.t("toast.imported"), Component.literal(this.session.layoutId), Icon.IMPORT);
         });
      }
   }

   void newCustomScreen() {
      List<String> existing = EditorForms.customScreens();
      Component body = existing.isEmpty() ? EditorText.t("new_screen.body") : EditorText.t("new_screen.body_existing", String.join(", ", existing));
      CFHGuiModal.prompt(EditorText.t("new_screen.title"), body, "", (raw) -> {
         String id = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
         if (!id.matches("[a-z0-9_-]{1,32}")) {
            CFHGuiToasts.show(EditorText.t("toast.bad_id"), EditorText.t("toast.bad_id_body"), Icon.WARNING);
         } else {
            this.requestExit(() -> {
               Minecraft mc = Minecraft.getInstance();
               mc.setScreen(new CustomScreen(id, this.screen));
               if (mc.screen instanceof CustomScreen) {
                  LayoutEditor.start(mc.screen);
               }

            });
         }
      });
   }

   void requestExit(@Nullable Runnable then) {
      Runnable finish = () -> {
         this.exiting = true;
         LayoutEditor.stop();
         if (then != null) {
            then.run();
         }

      };
      if (!this.session.dirty()) {
         finish.run();
      } else {
         askSaveDiscard(() -> {
            this.session.save();
            this.toastSaved();
            finish.run();
         }, () -> {
            this.session.discard();
            finish.run();
         }, (Runnable)null);
      }
   }

   private static void askSaveDiscard(Runnable save, Runnable discard, @Nullable Runnable cancel) {
      (new CFHGuiModal(EditorText.t("exit.title"), EditorText.t("exit.body"), Icon.SAVE)).button(EditorText.t("exit.cancel"), CFHGuiButton.Variant.SECONDARY, cancel).button(EditorText.t("exit.discard"), CFHGuiButton.Variant.DANGER, discard).button(EditorText.t("exit.save"), CFHGuiButton.Variant.PRIMARY, save).show();
   }
}
