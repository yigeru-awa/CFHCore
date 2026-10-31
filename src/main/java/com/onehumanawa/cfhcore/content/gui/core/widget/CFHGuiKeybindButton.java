package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.client.KeyBinds;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class CFHGuiKeybindButton extends CFHGuiButton {
   private final KeyMapping mapping;
   private boolean capturing;
   private int waitingOn;
   private boolean conflict;
   private final @Nullable Consumer<KeyMapping> onChange;

   public CFHGuiKeybindButton(int x, int y, int width, KeyMapping mapping, @Nullable Consumer<KeyMapping> onChange) {
      super(x, y, width, Component.empty(), (Runnable)null);
      this.mapping = mapping;
      this.onChange = onChange;
      this.refresh();
   }

   public KeyMapping mapping() {
      return this.mapping;
   }

   public boolean isCapturing() {
      return this.capturing;
   }

   public CFHGuiKeybindButton conflict(boolean c) {
      this.conflict = c;
      return this;
   }

   public void refresh() {
      this.setMessage((Component)(this.capturing ? Component.literal("> ").append(this.mapping.getTranslatedKeyMessage()).append(" <") : this.mapping.getTranslatedKeyMessage()));
   }

   public void onClick(double mouseX, double mouseY) {
      this.pressAnim.snap(1.0F);
      if (!this.capturing) {
         this.capturing = true;
         this.setFocused(true);
         this.refresh();
      }
   }

   public boolean captureMouse(int button) {
      if (!this.capturing) {
         return false;
      } else {
         KeyBinds.bind(this.mapping, Type.MOUSE.getOrCreate(button), KeyBinds.held());
         this.finish();
         return true;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.capturing) {
         this.captureMouse(button);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.capturing) {
         InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
         if (keyCode == 256) {
            KeyBinds.bind(this.mapping, InputConstants.UNKNOWN, "");
         } else {
            if (KeyBinds.supported() && !KeyBinds.modifierOf(keyCode).isEmpty()) {
               KeyBinds.bind(this.mapping, key, "");
               this.waitingOn = keyCode;
               KeyMapping.resetMapping();
               this.refresh();
               return true;
            }

            KeyBinds.bind(this.mapping, key, KeyBinds.held());
         }

         this.finish();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (this.capturing && this.waitingOn != 0 && keyCode == this.waitingOn) {
         this.finish();
         return true;
      } else {
         return super.keyReleased(keyCode, scanCode, modifiers);
      }
   }

   private void finish() {
      this.capturing = false;
      this.waitingOn = 0;
      KeyMapping.resetMapping();
      this.refresh();
      CFHGuiSounds.tick();
      if (this.onChange != null) {
         this.onChange.accept(this.mapping);
      }

   }

   protected void drawContent(GuiGraphics g, int x, int y, int w, int h, int fg, boolean shadow) {
      if (this.capturing && this.waitingOn != 0 && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), this.waitingOn)) {
         this.finish();
      }

      int color = fg;
      if (this.capturing) {
         color = Colors.scaleAlpha(Theme.current().accent(), this.effectiveAlpha());
      } else if (this.conflict) {
         color = Colors.scaleAlpha(Theme.current().palette().warning(), this.effectiveAlpha());
      }

      super.drawContent(g, x, y, w, h, color, shadow);
   }
}
