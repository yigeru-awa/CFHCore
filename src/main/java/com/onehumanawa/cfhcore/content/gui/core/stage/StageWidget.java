package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class StageWidget extends AbstractWidget {
   private final Stage stage;

   public StageWidget(int x, int y, int width, int height, Stage stage, Component label) {
      super(x, y, width, height, label);
      this.stage = stage;
   }

   public Stage stage() {
      return this.stage;
   }

   protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.stage.focusVisible(this.isFocused());
      boolean mouse = this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
      this.stage.render(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), mouse ? (double)mouseX : (double)-1.0E9F, mouse ? (double)mouseY : (double)-1.0E9F, partialTick);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.active && this.visible && this.isMouseOver(mouseX, mouseY) ? this.stage.mouseClicked(mouseX, mouseY, button) : false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return this.stage.mouseReleased(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.active && this.visible && this.stage.keyPressed(keyCode, scanCode, modifiers);
   }

   public void setFocused(boolean focused) {
      super.setFocused(focused);
      this.stage.focusVisible(focused);
      if (focused && this.stage.focused() == null) {
         this.stage.focusNext(1);
      }

   }

   public void playDownSound(SoundManager handler) {
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      out.add(NarratedElementType.TITLE, this.getMessage());
      StageNode n = this.stage.focused() != null ? this.stage.focused() : this.stage.hovered();
      if (n != null) {
         out.add(NarratedElementType.USAGE, n.name());
      }

   }
}
