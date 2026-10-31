package com.onehumanawa.cfhcore.content.gui.core.widget.popup;

import net.minecraft.client.gui.GuiGraphics;

public interface Popup {
   void render(GuiGraphics var1, int var2, int var3, float var4);

   boolean mouseClicked(double var1, double var3, int var5);

   default boolean mouseReleased(double mouseX, double mouseY, int button) {
      return false;
   }

   default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return false;
   }

   default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return false;
   }

   default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return false;
   }

   default boolean charTyped(char c, int modifiers) {
      return false;
   }

   boolean contains(double var1, double var3);

   default boolean isModal() {
      return false;
   }

   default void onClose() {
   }

   default boolean beginClose() {
      return false;
   }

   default boolean closeFinished() {
      return true;
   }
}
