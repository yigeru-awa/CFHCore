package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.client.PlayerSkins;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class CFHGuiAvatar extends CFHGuiWidget {
   private final Supplier<PlayerSkin> skin;
   private Status status;
   private Runnable onClick;

   public CFHGuiAvatar(int x, int y, int size, GameProfile profile) {
      super(x, y, size, size, Component.literal(profile.getName() == null ? "" : profile.getName()));
      this.status = CFHGuiAvatar.Status.NONE;
      this.skin = Minecraft.getInstance().getSkinManager().lookupInsecure(profile);
      this.active = false;
   }

   public CFHGuiAvatar(int x, int y, int size, UUID uuid, String name) {
      this(x, y, size, new GameProfile(uuid, name != null && !name.isEmpty() ? name : "?"));
   }

   public CFHGuiAvatar status(Status s) {
      this.status = s;
      return this;
   }

   public Status status() {
      return this.status;
   }

   public CFHGuiAvatar onClick(Runnable r) {
      this.onClick = r;
      this.active = r != null;
      return this;
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      if (this.onClick != null) {
         this.onClick.run();
      }

   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.active || this.onClick == null || keyCode != 257 && keyCode != 32 && keyCode != 335) {
         return false;
      } else {
         this.flashPress();
         this.onClick.run();
         return true;
      }
   }

   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return this.onClick == null ? null : super.nextFocusPath(event);
   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float a = this.effectiveAlpha();
      if (!(a <= 0.004F)) {
         int x = this.getX();
         int y = this.getY() + this.enterOffset();
         int s = this.getWidth();
         draw(g, (PlayerSkin)this.skin.get(), x, y, s, this.status, a);
         if (this.onClick != null) {
            float lift = Math.max(this.hover(), this.focus());
            if (lift > 0.01F) {
               CFHGuiDraw.outline(g, x - 1, y - 1, s + 2, s + 2, Colors.scaleAlpha(Theme.current().accent(), lift * a), 1);
            }
         }

      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.renderDark(g, mouseX, mouseY, partialTick);
   }

   public static ResourceLocation skinFor(UUID uuid, @Nullable String name) {
      try {
         return PlayerSkins.of(uuid).texture();
      } catch (Exception var3) {
         return DefaultPlayerSkin.get(uuid).texture();
      }
   }

   public static void draw(GuiGraphics g, PlayerSkin skin, int x, int y, int size, Status status, float alpha) {
      draw(g, skin == null ? DefaultPlayerSkin.getDefaultTexture() : skin.texture(), x, y, size, status, alpha);
   }

   public static void draw(GuiGraphics g, ResourceLocation skinTexture, int x, int y, int size, Status status, float alpha) {
      CFHGuiDraw.playerHead(g, skinTexture, x, y, size, alpha);
      if (status != CFHGuiAvatar.Status.NONE) {
         int d = Math.max(3, size / 4);
         int c = statusColor(status);
         int dx = x + size - d;
         int dy = y + size - d;
         g.fill(dx - 1, dy - 1, dx + d + 1, dy + d + 1, Colors.scaleAlpha(Theme.current().bg(), alpha));
         g.fill(dx, dy, dx + d, dy + d, Colors.scaleAlpha(c, alpha));
      }

   }

   public static int statusColor(Status status) {
      int var10000;
      switch (status.ordinal()) {
         case 1:
            var10000 = Theme.current().palette().success();
            break;
         case 2:
            var10000 = Theme.current().palette().warning();
            break;
         case 3:
         default:
            var10000 = Theme.current().palette().textDim();
            break;
         case 4:
            var10000 = Theme.current().palette().danger();
      }

      return var10000;
   }

   public static enum Status {
      NONE,
      ONLINE,
      AWAY,
      OFFLINE,
      BUSY;
   }
}
