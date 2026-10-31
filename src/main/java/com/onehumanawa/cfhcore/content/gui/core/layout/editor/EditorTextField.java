package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class EditorTextField extends EditBox {
   static final int PAD_X = 6;
   static final int PAD_Y = 5;
   static final int H = 20;
   private final Anim focusAnim;
   private final Anim hoverAnim;
   private @Nullable Component placeholder;
   private boolean invalid;
   private @Nullable Runnable onEnter;

   EditorTextField(int x, int y, int width, Component narration) {
      super(CFHGuiDraw.font(), x + 6, y + 5, Math.max(1, width - 12), 10, narration);
      this.focusAnim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.hoverAnim = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      this.setBordered(false);
      this.setMaxLength(2048);
      this.setTextColor(Theme.current().text());
   }

   EditorTextField text(String v) {
      this.setValue(v == null ? "" : v);
      return this;
   }

   EditorTextField onChange(Consumer<String> c) {
      this.setResponder(c);
      return this;
   }

   EditorTextField onEnter(Runnable r) {
      this.onEnter = r;
      return this;
   }

   EditorTextField placeholder(Component c) {
      this.placeholder = c;
      return this;
   }

   EditorTextField integer() {
      this.setFilter((s) -> s.matches("-?\\d{0,6}"));
      return this;
   }

   EditorTextField decimal() {
      this.setFilter((s) -> s.matches("-?\\d{0,7}(\\.\\d{0,4})?"));
      return this;
   }

   void setInvalid(boolean invalid) {
      this.invalid = invalid;
   }

   int frameX() {
      return this.getX() - 6;
   }

   int frameY() {
      return this.getY() - 5;
   }

   int frameW() {
      return this.getWidth() + 12;
   }

   int frameH() {
      return 20;
   }

   private boolean inFrame(double mx, double my) {
      return mx >= (double)this.frameX() && mx < (double)(this.frameX() + this.frameW()) && my >= (double)this.frameY() && my < (double)(this.frameY() + this.frameH());
   }

   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && this.inFrame(mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.visible && this.active && button == 0 && this.inFrame(mouseX, mouseY)) {
         if (!super.isMouseOver(mouseX, mouseY)) {
            this.moveCursorToEnd(false);
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.isFocused() && EditorKeys.isEnter(keyCode) && this.onEnter != null) {
         this.onEnter.run();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.visible) {
         Theme t = Theme.current();
         Palette p = t.palette();
         this.hoverAnim.set(this.inFrame((double)mouseX, (double)mouseY));
         this.focusAnim.set(this.isFocused());
         float foc = this.focusAnim.get();
         float hov = this.hoverAnim.get();
         int fx = this.frameX();
         int fy = this.frameY();
         int fw = this.frameW();
         int fh = this.frameH();
         if (t.isVanilla()) {
            CFHGuiDraw.vanillaTextField(g, fx, fy, fw, fh, this.isFocused());
            if (this.invalid) {
               CFHGuiDraw.outline(g, fx, fy, fw, fh, p.danger(), 0);
            }

            this.setTextColor(-2039584);
         } else {
            int fill = Colors.lerp(Colors.lerp(p.bg2(), p.surface(), hov), p.surface(), foc);
            int border = this.invalid ? p.danger() : Colors.lerp(Colors.lerp(p.border(), p.borderStrong(), hov), p.accent(), foc);
            CFHGuiDraw.pixelRound(g, fx, fy, fw, fh, fill, t.radius());
            CFHGuiDraw.outline(g, fx, fy, fw, fh, border, t.radius());
            this.setTextColor(p.text());
         }

         if (this.placeholder != null && this.getValue().isEmpty() && !this.isFocused()) {
            g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.placeholder, this.getWidth()), this.getX(), this.getY(), p.textDim(), false);
         }

         super.renderWidget(g, mouseX, mouseY, partialTick);
      }
   }
}
