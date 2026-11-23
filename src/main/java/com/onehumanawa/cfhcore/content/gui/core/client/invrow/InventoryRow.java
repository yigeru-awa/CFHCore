package com.onehumanawa.cfhcore.content.gui.core.client.invrow;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.mixin.ContainerScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class InventoryRow extends CFHGuiWidget {
   public static final int SIZE = 22;
   public static final int GAP = 2;
   public static final int GLYPH = 16;
   private static final int LIFT = 6;
   private static final int TAB_H = 28;
   private static final int EDGE = 2;
   private static final String BETTER_INVENTORY = "dev.fallingcloud.betterinventory.";
   private static final String CFH_CREATIVE = "com.onehumanawa.cfhcore.content.gui.menu.client.creative.";
   private static final List<Entry> ENTRIES = new CopyOnWriteArrayList();
   private static boolean hooked;
   private final AbstractContainerScreen<?> screen;
   private final ContainerScreenAccessor place;
   private final boolean tabsAbove;
   private final List<Entry> shown = new ArrayList();
   private final Anim[] hover = new Anim[8];
   private int columns = 1;
   private int pitch = 24;
   private int focusIndex;
   private int pressedIndex = -1;
   private static final Tabs CREATIVE_TABS = new Tabs(0, 27, 26, 7, 0);

   public static synchronized void register(Entry entry) {
      ENTRIES.removeIf((e) -> e.id().equals(entry.id()));
      ENTRIES.add(entry);
      ENTRIES.sort(Comparator.comparingInt(Entry::order));
      if (!hooked) {
         hooked = true;
         CFHGuiEvents.SCREEN_INIT_POST.register(InventoryRow::onScreenInit);
      }

   }

   public static List<Entry> entries() {
      return List.copyOf(ENTRIES);
   }

   public static int @Nullable [] bounds(Screen screen) {
      for(GuiEventListener child : screen.children()) {
         if (child instanceof InventoryRow row) {
            if (row.visible && !row.shown.isEmpty()) {
               return new int[]{row.getX(), row.getY(), row.getWidth(), row.getHeight()};
            }
         }
      }

      return null;
   }

   private static void onScreenInit(Screen screen) {
      if (screen instanceof AbstractContainerScreen<?> container) {
         if (screen instanceof LayoutApplier.ScreenAccess access) {
            String name = screen.getClass().getName();
            boolean better = name.startsWith("dev.fallingcloud.betterinventory.") && name.endsWith("BetterInventoryScreen");
            if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen) && !better) {
               return;
            }

            if (!(screen instanceof ContainerScreenAccessor)) {
               return;
            }

            ContainerScreenAccessor place = (ContainerScreenAccessor)screen;

            for(GuiEventListener child : screen.children()) {
               if (child instanceof InventoryRow) {
                  return;
               }
            }

            access.cfhgui$add(new InventoryRow(container, place, screen instanceof CreativeModeInventoryScreen && !name.startsWith("com.onehumanawa.cfhcore.content.gui.menu.client.creative.")));
            return;
         }
      }

   }

   private InventoryRow(AbstractContainerScreen<?> screen, ContainerScreenAccessor place, boolean tabsAbove) {
      super(0, 0, 22, 22, Component.empty());
      this.screen = screen;
      this.place = place;
      this.tabsAbove = tabsAbove;

      for(int i = 0; i < this.hover.length; ++i) {
         this.hover[i] = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      }

      this.follow();
   }

   private void refresh() {
      this.shown.clear();

      for(Entry e : ENTRIES) {
         try {
            if (e.shown().getAsBoolean()) {
               this.shown.add(e);
            }
         } catch (LinkageError | RuntimeException var4) {
         }

         if (this.shown.size() == this.hover.length) {
            break;
         }
      }

      this.active = !this.shown.isEmpty();
      if (this.focusIndex >= this.shown.size()) {
         this.focusIndex = 0;
      }

   }

   private void follow() {
      this.refresh();
      int n = Math.max(1, this.shown.size());
      int left = this.place.cfhgui$leftPos();
      int top = this.place.cfhgui$topPos();
      int pw = this.place.cfhgui$imageWidth();
      int ph = this.place.cfhgui$imageHeight();
      List<int[]> blocked = this.obstacles(left, top, pw, ph);
      int topEdge = this.tabsAbove ? top - 28 : top;
      boolean better = this.screen.getClass().getName().startsWith("dev.fallingcloud.betterinventory.");
      Tabs tabs = better ? InventoryRow.BetterTabs.get() : (this.tabsAbove ? CREATIVE_TABS : null);
      this.pitch = tabs != null ? Math.max(23, tabs.pitch()) : 24;
      int rowW = n * 22 + (n - 1) * (this.pitch - 22);
      int middle = tabs != null && tabs.middle() > 0 ? left + tabs.middle() : left + pw / 2;
      int x = middle - rowW / 2;
      if (tabs != null) {
         for(int s = 0; s + n <= tabs.cells(); ++s) {
            int onCells = left + tabs.x0() + s * tabs.pitch() + (tabs.cellW() - 22) / 2;
            if (Math.abs(onCells - x) <= 3) {
               x = onCells;
               break;
            }
         }
      }

      int y = topEdge - 6 - 22;

      for(int pass = 0; pass < 6; ++pass) {
         int[] hit = hit(x, y, rowW, 22, blocked);
         if (hit == null) {
            break;
         }

         y = hit[1] - 22 - 6;
      }

      if (y < 2 && hit(x, 2, rowW, 22, blocked) == null) {
         y = 2;
      }

      if (y >= 2 && x >= 2 && x + rowW <= this.screen.width - 2 && hit(x, y, rowW, 22, blocked) == null) {
         this.place(x, y, n, n);
      } else {
         this.pitch = 24;
         int bookLeft = this.bookOpen() ? Math.min(left, this.bookLeft()) : left;

         for(int cols = 1; cols <= Math.min(2, n); ++cols) {
            int rows = (n + cols - 1) / cols;
            int w = cols * 22 + (cols - 1) * 2;
            int h = rows * 22 + (rows - 1) * 2;
            int[] xs = new int[]{left + pw + 6 + 1, bookLeft - 6 - 1 - w};

            for(int bx : xs) {
               if (bx >= 2 && bx + w <= this.screen.width - 2) {
                  int by = Mth.clamp(top + (ph - h) / 2, 2, this.screen.height - 2 - h);

                  for(int pass = 0; pass < 6; ++pass) {
                     int[] hit = hit(bx, by, w, h, blocked);
                     if (hit == null) {
                        break;
                     }

                     by = hit[1] + hit[3] + 6;
                  }

                  if (by + h <= this.screen.height - 2 && hit(bx, by, w, h, blocked) == null) {
                     this.place(bx, by, cols, n);
                     return;
                  }
               }
            }
         }

         this.place(Math.max(2, x), Math.max(1, y), n, n);
      }
   }

   private void place(int x, int y, int cols, int n) {
      this.columns = Math.max(1, cols);
      int rows = (n + this.columns - 1) / this.columns;
      this.setX(x);
      this.setY(y);
      this.setWidth(this.columns * 22 + (this.columns - 1) * (this.pitch - 22));
      this.setHeight(rows * 22 + (rows - 1) * (this.pitch - 22));
   }

   private List<int[]> obstacles(int left, int top, int pw, int ph) {
      List<int[]> out = new ArrayList();
      out.add(new int[]{left, this.tabsAbove ? top - 28 : top, pw, ph + (this.tabsAbove ? 56 : 0)});
      if (this.bookOpen()) {
         out.add(new int[]{this.bookLeft(), (this.screen.height - 166) / 2, 147, 166});
      }

      for(GuiEventListener child : this.screen.children()) {
         if (child != this && child instanceof AbstractWidget other) {
            if (other.visible && other.getWidth() > 0 && other.getHeight() > 0) {
               out.add(new int[]{other.getX(), other.getY(), other.getWidth(), other.getHeight()});
            }
         }
      }

      return out;
   }

   private static @Nullable int[] hit(int x, int y, int w, int h, List<int[]> blocked) {
      for(int[] b : blocked) {
         if (x < b[0] + b[2] + 1 && x + w > b[0] - 1 && y < b[1] + b[3] + 1 && y + h > b[1] - 1) {
            return b;
         }
      }

      return null;
   }

   private boolean bookOpen() {
      try {
         AbstractContainerScreen var2 = this.screen;
         boolean var10000;
         if (var2 instanceof RecipeUpdateListener l) {
            if (l.getRecipeBookComponent().isVisible()) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      } catch (RuntimeException var3) {
         return false;
      }
   }

   private int bookLeft() {
      return (this.screen.width - 147) / 2 - 86;
   }

   private int buttonX(int i) {
      return this.getX() + i % this.columns * this.pitch;
   }

   private int buttonY(int i) {
      return this.getY() + i / this.columns * this.pitch;
   }

   private int indexAt(double mouseX, double mouseY) {
      for(int i = 0; i < this.shown.size(); ++i) {
         int bx = this.buttonX(i);
         int by = this.buttonY(i);
         if (mouseX >= (double)bx && mouseX < (double)(bx + 22) && mouseY >= (double)by && mouseY < (double)(by + 22)) {
            return i;
         }
      }

      return -1;
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.indexAt(mouseX, mouseY) >= 0;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.follow();
      return button == 0 && this.indexAt(mouseX, mouseY) >= 0 ? super.mouseClicked(mouseX, mouseY, button) : false;
   }

   public void onClick(double mouseX, double mouseY) {
      int i = this.indexAt(mouseX, mouseY);
      if (i >= 0) {
         this.pressedIndex = i;
         this.focusIndex = i;
         CFHGuiSounds.click();
         run((Entry)this.shown.get(i));
      }
   }

   private static void run(Entry e) {
      try {
         e.action().run();
      } catch (LinkageError | RuntimeException var2) {
      }

   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.shown.isEmpty()) {
         return false;
      } else if (keyCode == 262 && this.focusIndex < this.shown.size() - 1) {
         ++this.focusIndex;
         return true;
      } else if (keyCode == 263 && this.focusIndex > 0) {
         --this.focusIndex;
         return true;
      } else if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
         return false;
      } else {
         CFHGuiSounds.click();
         run((Entry)this.shown.get(this.focusIndex));
         return true;
      }
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, boolean vanilla) {
      this.follow();
      if (!this.shown.isEmpty()) {
         Palette p = Theme.current().palette();
         float a = this.effectiveAlpha();
         int hot = g.containsPointInScissor(mouseX, mouseY) ? this.indexAt((double)mouseX, (double)mouseY) : -1;
         int inset = 3;

         for(int i = 0; i < this.shown.size(); ++i) {
            Entry e = (Entry)this.shown.get(i);
            this.hover[i].set(i == hot);
            float h = this.hover[i].get();
            boolean down = i == this.pressedIndex && this.isPressed();
            int x = this.buttonX(i);
            int y = this.buttonY(i) + (down ? 1 : 0);
            if (!vanilla) {
               CFHGuiDraw.darkFace(g, x, y, 22, 22, h, down ? 1.0F : 0.0F, true, a);
               Icons.draw(g, e.icon(), x + 3, y + 3, 16, Colors.scaleAlpha(Colors.lerp(p.textMuted(), p.text(), h), a));
               if (this.isFocused() && i == this.focusIndex) {
                  CFHGuiDraw.focusRing(g, x, y, 22, 22, this.focus() * a);
               }
            } else {
               CFHGuiDraw.vanillaButton(g, x, y, 22, 22, Math.max(h, this.isFocused() && i == this.focusIndex ? 1.0F : 0.0F), true, a);
               Icons.draw(g, e.icon(), x + 3, y + 3, 16, Colors.scaleAlpha(-1, a));
            }

            List<Component> waiting = waiting(e);
            if (!waiting.isEmpty()) {
               g.pose().pushPose();
               g.pose().translate(0.0F, 0.0F, 200.0F);
               CFHGuiDraw.pixelCircle(g, x + 22 - 2, y + 2, 3, -15724528);
               CFHGuiDraw.pixelCircle(g, x + 22 - 2, y + 2, 2, vanilla ? -22016 : p.warning());
               g.pose().popPose();
            }

            if (i == hot && h > 0.9F) {
               List<Component> tip = new ArrayList();
               tip.add(title(e));

               for(Component line : waiting) {
                  tip.add(line.copy().withStyle(ChatFormatting.GOLD));
               }

               CFHGuiTooltips.request(tip, this);
            }
         }

      }
   }

   private static Component title(Entry e) {
      Component label = (Component)e.label().get();
      KeyMapping key = e.key() == null ? null : (KeyMapping)e.key().get();
      return (Component)(key != null && !key.isUnbound() ? Component.empty().append(label).append(Component.literal("  ")).append(key.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY)) : label);
   }

   private static List<Component> waiting(Entry e) {
      if (e.waiting() == null) {
         return List.of();
      } else {
         try {
            List<Component> lines = (List)e.waiting().get();
            return lines == null ? List.of() : lines;
         } catch (LinkageError | RuntimeException var2) {
            return List.of();
         }
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, false);
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.draw(g, mouseX, mouseY, true);
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      if (!this.shown.isEmpty()) {
         out.add(NarratedElementType.TITLE, (Component)((Entry)this.shown.get(Math.min(this.focusIndex, this.shown.size() - 1))).label().get());
      }

   }

   public static record Entry(String id, Icon icon, Supplier<Component> label, @Nullable Supplier<KeyMapping> key, int order, BooleanSupplier shown, Runnable action, @Nullable Supplier<List<Component>> waiting) {
   }

   private static record Tabs(int x0, int pitch, int cellW, int cells, int middle) {
   }

   private static final class BetterTabs {
      private static @Nullable Tabs tabs;
      private static boolean read;

      static @Nullable Tabs get() {
         if (read) {
            return tabs;
         } else {
            read = true;

            try {
               Class<?> c = Class.forName("dev.fallingcloud.betterinventory.BetterInventoryLayout");
               int x0 = c.getField("TAB_X0").getInt(null);
               int spacing = c.getField("TAB_SPACING").getInt(null);
               int w = c.getField("TAB_W").getInt(null);
               int strip = c.getField("CENTER_X").getInt(null);
               int middle = c.getField("STRIP_CENTER_X").getInt(null);
               int stripEnd = strip + 2 * (middle - strip);
               if (spacing > 0 && w > 0 && x0 >= strip) {
                  tabs = new Tabs(x0, spacing, w, Math.max(1, (stripEnd - x0 + spacing - w) / spacing), middle);
               }
            } catch (LinkageError | RuntimeException | ReflectiveOperationException var7) {
               tabs = null;
            }

            return tabs;
         }
      }
   }
}
