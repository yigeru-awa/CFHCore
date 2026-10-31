package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public class CFHGuiModal implements Popup {
   public static final int WIDTH = 240;
   private static final int PAD = 12;
   private static final int TITLE_H = 16;
   private static final int LINE_H = 10;
   private static final int BUTTON_H = 20;
   private final Component title;
   private final @Nullable Component body;
   private final @Nullable Icon icon;
   private final List<AbstractWidget> widgets = new ArrayList();
   private final List<CFHGuiButton> buttons = new ArrayList();
   private final List<AbstractWidget> extras = new ArrayList();
   private final Anim open;
   private int x;
   private int y;
   private int w;
   private int h;
   private int extrasHeight;
   private @Nullable AbstractWidget focused;
   private @Nullable Runnable onDismiss;
   private boolean closing;

   public CFHGuiModal(Component title, @Nullable Component body, @Nullable Icon icon) {
      this.open = new Anim(0.0F, 200, Ease.OUT_BACK);
      this.title = title;
      this.body = body;
      this.icon = icon;
      this.w = 240;
      this.open.snap(0.0F);
      this.open.set(1.0F);
   }

   public CFHGuiModal button(Component label, CFHGuiButton.Variant variant, Runnable action) {
      CFHGuiButton b = (new CFHGuiButton(0, 0, 80, label, () -> {
         Popups.close(this);
         if (action != null) {
            action.run();
         }

      })).variant(variant);
      this.buttons.add(b);
      this.widgets.add(b);
      return this;
   }

   public CFHGuiModal extra(AbstractWidget widget) {
      this.extras.add(widget);
      this.widgets.add(widget);
      this.extrasHeight += widget.getHeight() + 6;
      return this;
   }

   public CFHGuiModal width(int width) {
      this.w = width;
      return this;
   }

   public CFHGuiModal onDismiss(Runnable r) {
      this.onDismiss = r;
      return this;
   }

   public void show() {
      Popups.open(this);
      if (!this.extras.isEmpty()) {
         this.focused = (AbstractWidget)this.extras.get(0);
         this.focused.setFocused(true);
      }

   }

   public void close() {
      Popups.close(this);
   }

   private List<FormattedCharSequence> bodyLines() {
      return this.body == null ? List.of() : CFHGuiDraw.font().split(this.body, this.w - 24);
   }

   private void layout() {
      Minecraft mc = Minecraft.getInstance();
      int sw = mc.getWindow().getGuiScaledWidth();
      int sh = mc.getWindow().getGuiScaledHeight();
      this.w = Math.min(this.w, sw - 16);
      int bodyH = this.bodyLines().size() * 10;
      this.h = 28 + (bodyH > 0 ? bodyH + 6 : 0) + this.extrasHeight + (this.buttons.isEmpty() ? 0 : 26) + 12;
      this.x = (sw - this.w) / 2;
      this.y = (sh - this.h) / 2;
      int yy = this.y + 12 + 16 + (bodyH > 0 ? bodyH + 6 : 0);

      for(AbstractWidget e : this.extras) {
         e.setX(this.x + 12);
         e.setY(yy);
         e.setWidth(this.w - 24);
         yy += e.getHeight() + 6;
      }

      int bx = this.x + this.w - 12;

      for(int i = this.buttons.size() - 1; i >= 0; --i) {
         CFHGuiButton b = (CFHGuiButton)this.buttons.get(i);
         b.setWidth(Math.max(70, b.preferredWidth() + 12));
         bx -= b.getWidth();
         b.setX(bx);
         b.setY(this.y + this.h - 12 - 20);
         bx -= 6;
      }

   }

   public boolean isModal() {
      return true;
   }

   public boolean contains(double mx, double my) {
      return true;
   }

   private int animDy() {
      return Math.round((1.0F - Math.min(1.0F, this.open.get())) * 10.0F);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.layout();
      Theme t = Theme.current();
      Palette p = t.palette();
      float o = Math.max(0.0F, this.open.get());
      float a = Math.min(1.0F, o * 1.5F);
      if (!(a <= 0.01F)) {
         int dy = this.animDy();
         g.pose().pushPose();
         g.pose().translate(0.0F, (float)dy, 0.0F);
         if (t.isVanilla()) {
            CFHGuiDraw.vanillaDialog(g, this.x, this.y, this.w, this.h, a);
         } else {
            CFHGuiDraw.popupPlate(g, this.x, this.y, this.w, this.h, p.surface(), p.borderStrong(), 0.7F, a);
         }

         int tx = this.x + 12;
         if (this.icon != null) {
            int ic = this.icon == Icon.WARNING ? p.warning() : (this.icon == Icon.ERROR ? p.danger() : p.accent());
            Icons.draw(g, this.icon, tx, this.y + 12 - 1, 12, Colors.scaleAlpha(ic, a));
            tx += 16;
         }

         g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Fonts.heading(this.title), this.x + this.w - 12 - tx), tx, this.y + 12, Colors.scaleAlpha(p.text(), a), t.isVanilla());
         int by = this.y + 12 + 16;

         for(FormattedCharSequence line : this.bodyLines()) {
            g.drawString(CFHGuiDraw.font(), line, this.x + 12, by, Colors.scaleAlpha(p.textMuted(), a), t.isVanilla());
            by += 10;
         }

         Theme.enterPanel();

         try {
            for(AbstractWidget wd : this.widgets) {
               wd.setAlpha(a);
               wd.render(g, this.closing ? -1 : mouseX, this.closing ? -1 : mouseY - dy, partialTick);
            }
         } finally {
            Theme.exitPanel();
         }

         g.pose().popPose();
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      this.layout();
      double ay = my - (double)this.animDy();

      for(AbstractWidget wd : this.widgets) {
         if (wd.mouseClicked(mx, ay, button)) {
            if (this.focused != null && this.focused != wd) {
               this.focused.setFocused(false);
            }

            this.focused = wd;
            this.focused.setFocused(true);
            return true;
         }
      }

      return true;
   }

   public boolean mouseReleased(double mx, double my, int button) {
      for(AbstractWidget wd : this.widgets) {
         wd.mouseReleased(mx, my - (double)this.animDy(), button);
      }

      return true;
   }

   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      return this.focused != null && this.focused.mouseDragged(mx, my - (double)this.animDy(), button, dx, dy);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.focused != null && this.focused.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         if (keyCode == 257 || keyCode == 335) {
            for(CFHGuiButton b : this.buttons) {
               if (b.variant() == CFHGuiButton.Variant.PRIMARY) {
                  b.activate();
                  return true;
               }
            }
         }

         if (keyCode == 258 && !this.widgets.isEmpty()) {
            boolean back = (modifiers & 1) != 0;
            int i = this.focused == null ? (back ? 0 : -1) : this.widgets.indexOf(this.focused);
            if (this.focused != null) {
               this.focused.setFocused(false);
            }

            i = ((i + (back ? -1 : 1)) % this.widgets.size() + this.widgets.size()) % this.widgets.size();
            this.focused = (AbstractWidget)this.widgets.get(i);
            this.focused.setFocused(true);
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean charTyped(char c, int modifiers) {
      return this.focused != null && this.focused.charTyped(c, modifiers);
   }

   public void onClose() {
      if (this.onDismiss != null) {
         this.onDismiss.run();
      }

   }

   public boolean beginClose() {
      if (Theme.current().motion() <= 0.0F) {
         return false;
      } else {
         this.closing = true;
         this.open.set(0.0F, 120);
         this.open.ease(Ease.OUT_CUBIC);
         return true;
      }
   }

   public boolean closeFinished() {
      return this.open.get() <= 0.02F;
   }

   public static void confirm(Component title, Component body, Component okLabel, Runnable onOk) {
      (new CFHGuiModal(title, body, Icon.QUESTION)).button(Component.translatable("gui.cancel"), CFHGuiButton.Variant.SECONDARY, (Runnable)null).button(okLabel, CFHGuiButton.Variant.PRIMARY, onOk).show();
   }

   public static void confirmDanger(Component title, Component body, Component okLabel, Runnable onOk) {
      (new CFHGuiModal(title, body, Icon.WARNING)).button(Component.translatable("gui.cancel"), CFHGuiButton.Variant.SECONDARY, (Runnable)null).button(okLabel, CFHGuiButton.Variant.DANGER, onOk).show();
   }

   public static void info(Component title, Component body) {
      (new CFHGuiModal(title, body, Icon.INFO)).button(Component.translatable("gui.ok"), CFHGuiButton.Variant.PRIMARY, (Runnable)null).show();
   }

   public static void prompt(Component title, @Nullable Component body, String initial, Consumer<String> onOk) {
      CFHGuiTextField field = new CFHGuiTextField(0, 0, 216, Component.empty());
      field.setValue(initial == null ? "" : initial);
      CFHGuiModal m = (new CFHGuiModal(title, body, Icon.EDIT)).extra(field);
      field.onEnter(() -> {
         Popups.close(m);
         onOk.accept(field.getValue());
      });
      m.button(Component.translatable("gui.cancel"), CFHGuiButton.Variant.SECONDARY, (Runnable)null).button(Component.translatable("gui.ok"), CFHGuiButton.Variant.PRIMARY, () -> onOk.accept(field.getValue())).show();
   }
}
