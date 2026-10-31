package com.onehumanawa.cfhcore.content.gui.core.client.quick;

import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.mixin.ContainerScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class QuickBar extends CFHGuiWidget {
   static final int SIZE = 18;
   static final int GAP = 2;
   static final int GLYPH = 16;
   private static final int LIFT = 6;
   private static final int TAB_H = 28;
   private static final int EDGE = 2;
   private static final String SLATE_CREATIVE = "com.onehumanawa.cfhcore.content.gui.menu.client.creative.";
   private final AbstractContainerScreen<?> screen;
   private final ContainerScreenAccessor place;
   private final boolean tabsAbove;
   private final List<QuickAction> shown = new ArrayList();
   private final Anim[] hover = new Anim[QuickAction.values().length];
   private boolean column;
   private int focusIndex;
   private int pressedIndex = -1;

   QuickBar(AbstractContainerScreen<?> screen, ContainerScreenAccessor place) {
      super(0, 0, 0, 0, Component.empty());
      this.screen = screen;
      this.place = place;
      this.tabsAbove = screen instanceof CreativeModeInventoryScreen && !screen.getClass().getName().startsWith("com.onehumanawa.cfhcore.content.gui.menu.client.creative.");

      for(int i = 0; i < this.hover.length; ++i) {
         this.hover[i] = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      }

      this.follow();
   }

   private void refresh() {
      this.shown.clear();
      if (QuickActions.enabled()) {
         for(QuickAction action : QuickAction.values()) {
            if (QuickActions.allowed(action)) {
               this.shown.add(action);
            }
         }
      }

      if (this.focusIndex >= this.shown.size()) {
         this.focusIndex = 0;
      }

   }

   private void follow() {
      this.refresh();
      int n = this.shown.size();
      if (n == 0) {
         this.place(0, 0, false, 0);
      } else {
         int left = this.place.cfhgui$leftPos();
         int top = this.place.cfhgui$topPos();
         List<int[]> blocked = this.obstacles(left, top, this.place.cfhgui$imageWidth(), this.place.cfhgui$imageHeight());
         int length = n * 18 + (n - 1) * 2;
         int y = (this.tabsAbove ? top - 28 : top) - 6 - 18;

         for(int pass = 0; pass < 6; ++pass) {
            int[] hit = hit(left, y, length, 18, blocked);
            if (hit == null) {
               break;
            }

            y = hit[1] - 18 - 6;
         }

         if (y < 2 && hit(left, 2, length, 18, blocked) == null) {
            y = 2;
         }

         if (y >= 2 && left >= 2 && left + length <= this.screen.width - 2 && hit(left, y, length, 18, blocked) == null) {
            this.place(left, y, false, n);
         } else {
            int x = (this.bookOpen() ? Math.min(left, this.bookLeft()) : left) - 6 - 18;
            y = Math.max(2, top);

            for(int pass = 0; pass < 6; ++pass) {
               int[] hit = hit(x, y, 18, length, blocked);
               if (hit == null) {
                  break;
               }

               y = hit[1] + hit[3] + 6;
            }

            if (x >= 2 && y + length <= this.screen.height - 2 && hit(x, y, 18, length, blocked) == null) {
               this.place(x, y, true, n);
            } else {
               this.place(0, 0, false, 0);
            }
         }
      }
   }

   private void place(int x, int y, boolean column, int n) {
      this.column = column;
      int length = n == 0 ? 0 : n * 18 + (n - 1) * 2;
      int across = n == 0 ? 0 : 18;
      this.setX(x);
      this.setY(y);
      this.setWidth(column ? across : length);
      this.setHeight(column ? length : across);
      this.active = n > 0;
   }

   private List<int[]> obstacles(int left, int top, int pw, int ph) {
      List<int[]> out = new ArrayList();
      out.add(new int[]{left, this.tabsAbove ? top - 28 : top, pw, ph + (this.tabsAbove ? 56 : 0)});
      if (this.bookOpen()) {
         out.add(new int[]{this.bookLeft(), (this.screen.height - 166) / 2, 147, 166});
      }

      int[] effects = ViewerEffects.overPanel(this.screen, left, top, pw);
      if (effects != null) {
         out.add(effects);
      }

      for(GuiEventListener child : this.screen.children()) {
         if (child != this && !(child instanceof InventoryRow) && child instanceof AbstractWidget other) {
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
      return this.getX() + (this.column ? 0 : i * 20);
   }

   private int buttonY(int i) {
      return this.getY() + (this.column ? i * 20 : 0);
   }

   private int indexAt(double mouseX, double mouseY) {
      if (this.getWidth() <= 0) {
         return -1;
      } else {
         for(int i = 0; i < this.shown.size(); ++i) {
            int bx = this.buttonX(i);
            int by = this.buttonY(i);
            if (mouseX >= (double)bx && mouseX < (double)(bx + 18) && mouseY >= (double)by && mouseY < (double)(by + 18)) {
               return i;
            }
         }

         return -1;
      }
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
         this.place.cfhgui$skipNextRelease(true);
         CFHGuiSounds.click();
         QuickActions.run((QuickAction)this.shown.get(i));
      }
   }

   public void playDownSound(SoundManager handler) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.active) {
         return false;
      } else if (keyCode == (this.column ? 264 : 262) && this.focusIndex < this.shown.size() - 1) {
         ++this.focusIndex;
         return true;
      } else if (keyCode == (this.column ? 265 : 263) && this.focusIndex > 0) {
         --this.focusIndex;
         return true;
      } else if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
         return false;
      } else {
         CFHGuiSounds.click();
         QuickActions.run((QuickAction)this.shown.get(this.focusIndex));
         return true;
      }
   }

   private void draw(GuiGraphics g, int mouseX, int mouseY, boolean vanilla) {
      this.follow();
      if (this.getWidth() > 0) {
         Palette p = Theme.current().palette();
         float a = this.effectiveAlpha();
         int hot = g.containsPointInScissor(mouseX, mouseY) ? this.indexAt((double)mouseX, (double)mouseY) : -1;
         int inset = 1;
         if (this.pressedIndex >= 0 && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), 0) != 1) {
            this.pressedIndex = -1;
         }

         for(int i = 0; i < this.shown.size(); ++i) {
            QuickAction action = (QuickAction)this.shown.get(i);
            QuickAction.State state = QuickActions.state(action);
            this.hover[i].set(i == hot);
            float h = this.hover[i].get();
            boolean down = i == this.pressedIndex && i == hot;
            float focused = i == this.focusIndex ? this.focus() : 0.0F;
            int x = this.buttonX(i);
            int y = this.buttonY(i) + (down ? 1 : 0);
            if (vanilla) {
               CFHGuiDraw.vanillaButton(g, x, y, 18, 18, Math.max(h, focused), true, a);
               Icons.draw(g, state.icon, x + 1, y + 1, 16, Colors.scaleAlpha(-1, a));
            } else {
               CFHGuiDraw.darkFace(g, x, y, 18, 18, h, down ? 1.0F : 0.0F, true, a);
               Icons.draw(g, state.icon, x + 1, y + 1, 16, Colors.scaleAlpha(Colors.lerp(p.textMuted(), p.text(), h), a));
               CFHGuiDraw.focusRing(g, x, y, 18, 18, focused * a);
            }

            if (i == hot && h > 0.9F) {
               CFHGuiTooltips.request(List.of(action.title(state), action.hint(action.after(state)).copy().withStyle(ChatFormatting.GRAY)), this);
            }
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
      if (this.active) {
         QuickAction action = (QuickAction)this.shown.get(Math.min(this.focusIndex, this.shown.size() - 1));
         QuickAction.State state = QuickActions.state(action);
         out.add(NarratedElementType.TITLE, action.title(state));
         out.add(NarratedElementType.USAGE, action.hint(action.after(state)));
      }
   }
}
