package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class CFHGuiDropdown<T> extends CFHGuiWidget {
   private final List<T> options;
   private final Function<T, Component> labeler;
   private T value;
   private final Consumer<T> onChange;
   private Component label = Component.empty();
   private boolean open;

   public CFHGuiDropdown(int x, int y, int width, List<T> options, T value, Function<T, Component> labeler, Consumer<T> onChange) {
      super(x, y, width, 20, Component.empty());
      this.options = new ArrayList(options);
      this.labeler = labeler;
      this.value = value;
      this.onChange = onChange;
   }

   public CFHGuiDropdown<T> label(Component label) {
      this.label = label;
      return this;
   }

   public T value() {
      return this.value;
   }

   public CFHGuiDropdown<T> setValue(T v) {
      this.value = v;
      return this;
   }

   public void setOptions(List<T> opts) {
      this.options.clear();
      this.options.addAll(opts);
   }

   public List<T> options() {
      return this.options;
   }

   private Component display() {
      Component v = (Component)(this.value == null ? Component.literal("-") : (Component)this.labeler.apply(this.value));
      return (Component)(this.label.getString().isEmpty() ? v : Component.empty().append(this.label).append(": ").append(v));
   }

   private void choose(T opt, boolean sound) {
      if (opt == null) {
         if (this.value == null) {
            return;
         }
      } else if (opt.equals(this.value)) {
         return;
      }

      this.value = opt;
      if (sound) {
         CFHGuiSounds.tick();
      }

      if (this.onChange != null) {
         this.onChange.accept(opt);
      }

   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.openMenu();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.active && this.visible) {
         if (keyCode != 257 && keyCode != 32 && keyCode != 335 && keyCode != 264) {
            int i = this.options.indexOf(this.value);
            if (keyCode == 263 && !this.options.isEmpty()) {
               this.choose(this.options.get(Math.max(0, i - 1)), true);
               return true;
            } else if (keyCode == 262 && !this.options.isEmpty()) {
               this.choose(this.options.get(Math.min(this.options.size() - 1, i + 1)), true);
               return true;
            } else {
               return false;
            }
         } else {
            this.openMenu();
            return true;
         }
      } else {
         return false;
      }
   }

   private void openMenu() {
      if (!this.open) {
         List<MenuPopup.Item> items = new ArrayList();
         int current = -1;

         for(int i = 0; i < this.options.size(); ++i) {
            T opt = (T)this.options.get(i);
            boolean cur = opt != null && opt.equals(this.value);
            if (cur) {
               current = i;
            }

            items.add(MenuPopup.Item.checked((Component)this.labeler.apply(opt), cur, () -> this.choose(opt, false)));
         }

         MenuPopup menu = new MenuPopup(this.getX(), this.getY() + this.getHeight() + 1, items, this.getWidth()) {
            public void onClose() {
               CFHGuiDropdown.this.open = false;
            }
         };
         if (current >= 0) {
            menu.highlight(current);
         }

         this.open = true;
         Popups.open(menu);
      }
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         float lift = Math.max(this.hover(), this.open ? 1.0F : 0.0F);
         int fill = Colors.lerp(p.surface(), p.surfaceHover(), lift);
         int border = Colors.lerp(Colors.lerp(p.border(), p.borderStrong(), lift), p.accent(), Math.max(this.focus(), this.open ? 1.0F : 0.0F));
         if (!this.active) {
            fill = Colors.withAlpha(p.surface(), 128);
            border = Colors.withAlpha(p.border(), 128);
         }

         fill = t.field(fill);
         border = t.fieldEdge(border);
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, a), t.radius());
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(border, a), t.radius());
         int fg = Colors.scaleAlpha(this.active ? p.text() : p.textDim(), a);
         FormattedCharSequence shown = CFHGuiDraw.truncate(this.display(), w - 24);
         int tx = Theme.setIn() ? x + w - 18 - CFHGuiDraw.width(shown) : x + 6;
         g.drawString(CFHGuiDraw.font(), shown, tx, CFHGuiDraw.textY(y, h), fg, false);
         Icons.draw(g, this.open ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN, x + w - 14, y + (h - 8) / 2, 8, Colors.scaleAlpha(this.active ? p.textMuted() : p.textDim(), a));
      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int w = this.getWidth();
         int h = this.getHeight();
         CFHGuiDraw.vanillaButton(g, x, y, w, h, this.active ? Math.max(this.hover(), Math.max(this.focus(), this.open ? 1.0F : 0.0F)) : 0.0F, this.active, a);
         int fg = Colors.scaleAlpha(this.active ? -1 : -6250336, a);
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.display(), w - 24), x + 6, CFHGuiDraw.textY(y, h), fg, true);
         Icons.draw(g, this.open ? Icon.CHEVRON_UP : Icon.CHEVRON_DOWN, x + w - 14, y + (h - 8) / 2, 8, fg);
      }
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.button", new Object[]{this.display()}));
      if (this.active) {
         out.add(NarratedElementType.USAGE, Component.translatable(this.isFocused() ? "narration.button.usage.focused" : "narration.button.usage.hovered"));
      }

   }
}
