package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class OptionRow extends AbstractContainerWidget {
   public static final int HEIGHT = 24;
   public static final int STACKED_HEIGHT = 42;
   public static final int INLINE_MIN_WIDTH = 236;
   private static final Set<String> RESTART_TOASTED = new HashSet();
   private final OptionBinding binding;
   private final List<AbstractWidget> children;
   private final CFHGuiIconButton star;
   private final @Nullable AbstractWidget control;
   private final @Nullable CFHGuiIconButton reset;
   private @Nullable Consumer<OptionRow> onChanged;
   private long flashStart;
   private final int depth;
   private final Look look;
   private final Anim enter;
   private long enterStartMs;
   private final Anim hoverGlow;

   public OptionRow(int x, int y, int width, OptionBinding binding, int depth) {
      this(x, y, width, binding, depth, OptionRow.Look.ROW);
   }

   public OptionRow(int x, int y, int width, OptionBinding binding, int depth, Look look) {
      super(x, y, width, heightOf(look), binding.label());
      this.children = new ArrayList();
      this.enter = new Anim(1.0F, 220, Ease.OUT_CUBIC);
      this.hoverGlow = new Anim(0.0F, 150, Ease.OUT_CUBIC);
      this.binding = binding;
      this.depth = depth;
      this.look = look;
      int labelX = 22 + depth * 8;
      this.star = new CFHGuiIconButton(0, 0, 16, ConfigSettings.isFavourite(binding.id()) ? Icon.STAR_FILLED : Icon.STAR, Component.translatable("cfhgui_config.row.favourite"), (Runnable)null);
      this.star.onPress(() -> {
         ConfigSettings.toggleFavourite(binding.id());
         boolean fav = ConfigSettings.isFavourite(binding.id());
         this.star.setIcon(fav ? Icon.STAR_FILLED : Icon.STAR).toggled(fav);
         if (this.onChanged != null) {
            this.onChanged.accept(this);
         }

      });
      this.star.toggled(ConfigSettings.isFavourite(binding.id()));
      this.children.add(this.star);
      boolean hasReset = binding.hasDefault() && binding.type().snapshotable();
      int resetW = 20;
      int controlW = look == OptionRow.Look.STACKED ? stackedControlWidth(binding, width - labelX - 8) : controlWidth(binding, width - labelX - 20, look);
      this.control = Controls.create(binding, 0, 0, controlW, this::apply);
      if (this.control != null) {
         this.control.active = binding.enabled();
         this.children.add(this.control);
      }

      if (hasReset) {
         this.reset = new CFHGuiIconButton(0, 0, 16, Icon.UNDO, Component.translatable("cfhgui_config.row.reset"), this::resetToDefault);
         this.reset.active = !binding.isDefault() && binding.enabled();
         this.children.add(this.reset);
      } else {
         this.reset = null;
      }

      this.layout();
   }

   public static int heightOf(Look look) {
      return look == OptionRow.Look.STACKED ? 42 : 24;
   }

   public Look look() {
      return this.look;
   }

   private static int stackedControlWidth(OptionBinding b, int available) {
      int var10000;
      switch (b.type()) {
         case BOOLEAN -> var10000 = 22;
         case INFO -> var10000 = 0;
         default -> var10000 = Math.max(60, Math.min(220, available));
      }

      return var10000;
   }

   private static int controlWidth(OptionBinding b, int available, Look look) {
      int var10000;
      switch (b.type()) {
         case BOOLEAN -> var10000 = 22;
         case INFO -> var10000 = 0;
         default -> var10000 = look == OptionRow.Look.TILE ? Math.max(72, Math.min(150, Math.round((float)available * 0.45F))) : Math.max(60, Math.min(150, available - 40));
      }

      return var10000;
   }

   public OptionBinding binding() {
      return this.binding;
   }

   public OptionRow onChanged(Consumer<OptionRow> c) {
      this.onChanged = c;
      return this;
   }

   public @Nullable AbstractWidget control() {
      return this.control;
   }

   public void flash() {
      this.flashStart = Clock.nowMs();
   }

   public void playEntrance(int delayMs) {
      for(AbstractWidget c : this.children) {
         if (c instanceof CFHGuiWidget w) {
            w.playEntrance(delayMs);
         }
      }

      float motion = Theme.current().motion();
      if (motion <= 0.0F) {
         this.enter.snap(1.0F);
         this.enterStartMs = 0L;
      } else {
         this.enter.snap(0.0F);
         this.enterStartMs = Clock.nowMs() + (long)Math.max(0, Math.round((float)delayMs * motion));
      }
   }

   private float enterProgress() {
      if (this.enterStartMs != 0L) {
         if (Theme.current().motion() <= 0.0F) {
            this.enterStartMs = 0L;
            this.enter.snap(1.0F);
            return 1.0F;
         }

         if (Clock.nowMs() < this.enterStartMs) {
            return 0.0F;
         }

         this.enterStartMs = 0L;
         this.enter.set(1.0F, 220);
      }

      return Math.min(1.0F, Math.max(0.0F, this.enter.get()));
   }

   private void apply(Object value) {
      try {
         this.binding.set(value);
      } catch (Exception e) {
         CFHGuiConfig.LOGGER.error("[CFHGui Config] cannot set {}", this.binding.id(), e);
         CFHGuiToasts.show(Component.translatable("cfhgui_config.toast.set_failed"), Component.literal(String.valueOf(e.getMessage())), Icon.ERROR);
      }

      if (this.binding.requiresRestart() && RESTART_TOASTED.add(this.binding.id())) {
         CFHGuiToasts.show(Component.translatable("cfhgui_config.toast.restart_title"), Component.translatable("cfhgui_config.toast.restart_body", new Object[]{this.binding.label()}), Icon.WARNING);
      }

      if (this.reset != null) {
         this.reset.active = !this.binding.isDefault() && this.binding.enabled();
      }

      if (this.onChanged != null) {
         this.onChanged.accept(this);
      }

   }

   private void resetToDefault() {
      this.binding.reset();
      this.refresh();
      if (this.onChanged != null) {
         this.onChanged.accept(this);
      }

   }

   public void refresh() {
      Controls.refresh(this.binding, this.control);
      if (this.reset != null) {
         this.reset.active = !this.binding.isDefault() && this.binding.enabled();
      }

      boolean fav = ConfigSettings.isFavourite(this.binding.id());
      this.star.setIcon(fav ? Icon.STAR_FILLED : Icon.STAR).toggled(fav);
   }

   private int labelLineHeight() {
      return this.look == OptionRow.Look.STACKED ? 21 : 24;
   }

   private void layout() {
      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int line = this.labelLineHeight();
      this.star.setX(x + 2 + this.depth * 8);
      this.star.setY(y + (line - 16) / 2);
      int right = x + w - 2;
      if (this.reset != null) {
         this.reset.setX(right - 16);
         this.reset.setY(y + (line - 16) / 2);
      }

      right -= 20;
      if (this.control != null) {
         if (this.look == OptionRow.Look.STACKED) {
            this.control.setX(x + 22 + this.depth * 8 - (this.binding.type() == OptionType.BOOLEAN ? 10 : 0));
            this.control.setY(y + line - 2 + (42 - line - 2 - this.control.getHeight()) / 2);
         } else {
            this.control.setX(right - this.control.getWidth());
            this.control.setY(y + (24 - this.control.getHeight()) / 2);
         }

      }
   }

   private int labelRight() {
      if (this.look == OptionRow.Look.STACKED) {
         return this.reset != null ? this.reset.getX() - 4 : this.getX() + this.getWidth() - 6;
      } else if (this.control != null) {
         return this.control.getX() - 6;
      } else {
         return this.reset != null ? this.reset.getX() - 6 : this.getX() + this.getWidth() - 4;
      }
   }

   private boolean overLabel(double mx, double my) {
      int lx = this.getX() + 22 + this.depth * 8;
      return mx >= (double)lx && mx < (double)this.labelRight() && my >= (double)this.getY() && my < (double)(this.getY() + this.labelLineHeight());
   }

   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   private boolean inside(double mx, double my) {
      return mx >= (double)this.getX() && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.inside(mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.inside(mouseX, mouseY)) {
         this.layout();

         for(AbstractWidget c : this.children) {
            if (c.mouseClicked(mouseX, mouseY, button)) {
               this.setFocused(c);
               if (button == 0) {
                  this.setDragging(true);
               }

               return true;
            }
         }

         if (button == 0 && this.binding.type() == OptionType.BOOLEAN && this.control != null && this.control.active && this.overLabel(mouseX, mouseY)) {
            this.control.mouseClicked((double)(this.control.getX() + 1), (double)(this.control.getY() + 1), 0);
            this.setFocused(this.control);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (!this.inside(mouseX, mouseY)) {
         return false;
      } else {
         this.layout();

         for(AbstractWidget c : this.children) {
            if (c.isMouseOver(mouseX, mouseY) && c.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
               return true;
            }
         }

         return false;
      }
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      this.layout();
      float ea = this.enterProgress();
      int x = this.getX();
      int w = this.getWidth();
      int y = this.getY() + Math.round((1.0F - ea) * 6.0F);
      boolean hov = this.inside((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
      boolean enabled = this.binding.enabled();
      int h = this.getHeight();
      int line = this.labelLineHeight();
      this.hoverGlow.set(hov);
      float hv = this.hoverGlow.get();
      if (this.look != OptionRow.Look.ROW && ea > 0.01F) {
         if (t.isVanilla()) {
            g.fill(x, y, x + w, y + h, Colors.scaleAlpha(Colors.lerp(1342177280, 1881153568, hv), ea));
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-16777216, -1, hv), ea), 0);
         } else {
            CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(Colors.lerp(p.surface(), p.surfaceHover(), hv)), ea), t.radius());
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.border(), Colors.withAlpha(p.accent(), 160), hv), ea), t.radius());
         }
      } else if (hov && ea > 0.01F) {
         if (t.isVanilla()) {
            g.fill(x, y, x + w, y + 24, Colors.scaleAlpha(419430399, ea));
         } else {
            CFHGuiDraw.pixelRound(g, x, y, w, 24, Colors.scaleAlpha(Colors.withAlpha(p.surfaceHover(), 112), ea), t.radius());
         }
      }

      if (this.flashStart > 0L) {
         float f = 1.0F - (float)(Clock.nowMs() - this.flashStart) / 1500.0F;
         if (f <= 0.0F) {
            this.flashStart = 0L;
         } else {
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.accent(), f), t.radius());
         }
      }

      int lx = x + 22 + this.depth * 8;
      int fg = enabled ? p.text() : p.textDim();
      if (t.isVanilla()) {
         fg = enabled ? -1 : -6250336;
      }

      int labelW = Math.max(10, this.labelRight() - lx - (this.binding.requiresRestart() ? 46 : 0));
      if (ea > 0.03F) {
         int ty = y + (line - 9) / 2 + 1;
         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.binding.label(), labelW), lx, ty, Colors.scaleAlpha(fg, ea), t.isVanilla());
         int after = lx + Math.min(labelW, CFHGuiDraw.width(this.binding.label())) + 6;
         if (this.binding.requiresRestart() && ea > 0.5F) {
            after += CFHGuiBadge.draw(g, Component.translatable("cfhgui_config.row.restart"), after, y + (line - 10) / 2, p.warning()) + 4;
         }

         if (this.binding.type() == OptionType.INFO) {
            Component vt = this.binding.valueText(this.binding.get());
            if (this.look == OptionRow.Look.STACKED) {
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(vt, Math.max(10, w - 28)), lx, y + line + 3, Colors.scaleAlpha(p.textMuted(), ea), t.isVanilla());
            } else {
               g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(vt, Math.max(10, x + w - 4 - after)), after, ty, Colors.scaleAlpha(p.textMuted(), ea), t.isVanilla());
            }
         } else if (this.look == OptionRow.Look.STACKED && this.binding.type() == OptionType.BOOLEAN && this.control != null) {
            boolean on = OptionValues.asBoolean(this.binding.get(), false);
            g.drawString(CFHGuiDraw.font(), Component.translatable(on ? "options.on" : "options.off"), this.control.getX() + this.control.getWidth() + 4, this.control.getY() + (this.control.getHeight() - 9) / 2 + 1, Colors.scaleAlpha(on ? (t.isVanilla() ? -1 : p.text()) : (t.isVanilla() ? -6250336 : p.textMuted()), ea), t.isVanilla());
         }
      }

      if (this.look != OptionRow.Look.ROW) {
         Theme.enterPanel();
      }

      try {
         for(AbstractWidget c : this.children) {
            if (c.visible) {
               boolean set = this.look != OptionRow.Look.ROW && c == this.control;
               if (set) {
                  Theme.enterRow();
               }

               try {
                  c.render(g, mouseX, mouseY, partialTick);
               } finally {
                  if (set) {
                     Theme.exitRow();
                  }

               }
            }
         }
      } finally {
         if (this.look != OptionRow.Look.ROW) {
            Theme.exitPanel();
         }

      }

      if (hov && this.overLabel((double)mouseX, (double)mouseY)) {
         List<Component> tip = this.tooltipLines();
         if (!tip.isEmpty()) {
            CFHGuiTooltips.request(tip, this);
         }
      }

   }

   private List<Component> tooltipLines() {
      List<Component> lines = new ArrayList();
      Component tip = this.binding.tooltip();
      if (tip != null) {
         for(String l : tip.getString().split("\n")) {
            lines.add(Component.literal(l));
         }
      }

      if (this.binding.hasDefault() && this.binding.type() != OptionType.ACTION) {
         lines.add(Component.translatable("cfhgui_config.row.default", new Object[]{this.binding.valueText(this.binding.defaultValue())}).withStyle(ChatFormatting.DARK_GRAY));
      }

      if (this.binding.requiresRestart()) {
         lines.add(Component.translatable("cfhgui_config.row.restart_tip").withStyle(ChatFormatting.GOLD));
      }

      return lines;
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, this.binding.label());
   }

   public static enum Look {
      ROW,
      TILE,
      STACKED;
   }
}
