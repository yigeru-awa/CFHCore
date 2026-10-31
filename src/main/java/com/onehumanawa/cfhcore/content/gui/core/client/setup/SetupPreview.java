package com.onehumanawa.cfhcore.content.gui.core.client.setup;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class SetupPreview {
   private static final ResourceLocation INVENTORY = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
   private static final int MENU_W = 300;
   private static final int MENU_H = 200;
   static final int INV_W = 176;
   static final int INV_H = 166;
   private static final LogoRenderer LOGO = new LogoRenderer(false);
   private static final ItemStack[] HOTBAR;
   private static final ItemStack[] ROW;

   private SetupPreview() {
   }

   private static Palette dark() {
      return Palette.dark(Colors.fromHex(CFHGui.config().accent, -7726052));
   }

   private static Font font() {
      return CFHGuiDraw.font();
   }

   static void menus(GuiGraphics g, Rect r, boolean customLayout, boolean customStyle, float alpha) {
      if (r.w() >= 20 && r.h() >= 20) {
         float s = Math.min((float)r.w() / 300.0F, (float)r.h() / 200.0F);
         int vw = Math.max(300, Math.round((float)r.w() / s));
         int vh = Math.max(200, Math.round((float)r.h() / s));
         g.enableScissor(r.x(), r.y(), r.right(), r.bottom());
         g.pose().pushPose();
         g.pose().translate((float)r.x(), (float)r.y(), 0.0F);
         g.pose().scale(s, s, 1.0F);
         sky(g, vw, vh, customStyle, alpha);
         if (customLayout) {
            cfhguiTitle(g, vw, vh, customStyle, alpha);
         } else {
            vanillaTitle(g, vw, vh, customStyle, alpha);
         }

         g.pose().popPose();
         g.disableScissor();
      }
   }

   private static void sky(GuiGraphics g, int vw, int vh, boolean customStyle, float alpha) {
      CFHGuiDraw.vgradient(g, 0, 0, vw, vh, Colors.scaleAlpha(-10842161, alpha), Colors.scaleAlpha(-6306584, alpha));
      int horizon = vh * 3 / 5;
      CFHGuiDraw.vgradient(g, 0, horizon, vw, vh - horizon, Colors.scaleAlpha(-11632827, alpha), Colors.scaleAlpha(-13742806, alpha));
      CFHGuiDraw.pixelCircle(g, vw * 3 / 4, vh / 4, 9, Colors.scaleAlpha(-3390, alpha));

      for(int i = 0; i < 6; ++i) {
         int hx = i * vw / 5 - 20;
         int hh = 18 + i * 7 % 16;
         CFHGuiDraw.pixelRound(g, hx, horizon - hh / 2, vw / 4, hh, Colors.scaleAlpha(-12620742, alpha), 6);
      }

      if (customStyle) {
         CFHGuiDraw.rect(g, 0, 0, vw, vh, Colors.scaleAlpha(Colors.withAlpha(921101, 112), alpha));
         CFHGuiDraw.vignette(g, 0, 0, vw, vh, 0.5F * alpha);
      }

   }

   private static void cfhguiTitle(GuiGraphics g, int vw, int vh, boolean customStyle, float alpha) {
      Palette p = dark();
      Font f = font();
      LOGO.renderLogo(g, vw, alpha, 10);
      int navX = 16;
      int navW = 104;
      int top = 72;
      int rowH = 22;
      int gap = 4;
      String[] labels = new String[]{"Singleplayer", "Multiplayer", "Screenshots", "Options", "Quit"};
      Icon[] icons = new Icon[]{Icon.SINGLEPLAYER, Icon.MULTIPLAYER, Icon.CAMERA, Icon.SETTINGS, Icon.QUIT};
      if (customStyle) {
         CFHGuiDraw.pixelRound(g, 10, 66, 116, labels.length * 26 - 4 + 12, Colors.scaleAlpha(Colors.withAlpha(p.bg(), 140), alpha), 3);
      }

      for(int i = 0; i < labels.length; ++i) {
         int y = 72 + i * 26;
         boolean danger = i == labels.length - 1;
         boolean hot = i == 0;
         if (customStyle) {
            int fill = hot ? Colors.withAlpha(p.surfaceHover(), 240) : Colors.withAlpha(p.surface(), 180);
            CFHGuiDraw.pixelRound(g, 16, y, 104, 22, Colors.scaleAlpha(fill, alpha), 3);
            CFHGuiDraw.outline(g, 16, y, 104, 22, Colors.scaleAlpha(hot ? p.borderStrong() : Colors.withAlpha(p.border(), 176), alpha), 3);
            if (hot) {
               CFHGuiDraw.rect(g, 18, y + 5, 2, 12, Colors.scaleAlpha(p.accent(), alpha));
            }

            int ic = danger ? p.danger() : (hot ? p.accent() : p.textMuted());
            Icons.draw(g, icons[i], 24, y + 5, 12, Colors.scaleAlpha(ic, alpha));
            g.drawString(f, labels[i], 42, y + 7, Colors.scaleAlpha(danger ? p.danger() : p.text(), alpha), false);
         } else {
            CFHGuiDraw.vanillaButton(g, 16, y, 104, 22, hot ? 1.0F : 0.0F, true, alpha);
            int fg = danger ? -38294 : -1;
            Icons.draw(g, icons[i], 24, y + 5, 12, Colors.scaleAlpha(fg, alpha));
            g.drawString(f, labels[i], 42, y + 7, Colors.scaleAlpha(fg, alpha), true);
         }
      }

      int cx = 134;
      int cw = vw - 134 - 16;
      card(g, 134, 72, cw, 54, customStyle, alpha, p);
      g.drawString(f, "CONTINUE", 142, 78, Colors.scaleAlpha(customStyle ? p.textDim() : -6250336, alpha), !customStyle);
      CFHGuiDraw.pixelRound(g, 142, 90, 28, 28, Colors.scaleAlpha(customStyle ? p.bg2() : Integer.MIN_VALUE, alpha), 3);
      Icons.draw(g, Icon.WORLD, 148, 96, 16, Colors.scaleAlpha(customStyle ? p.textMuted() : -4144960, alpha));
      g.drawString(f, "My World", 176, 91, Colors.scaleAlpha(customStyle ? p.text() : -1, alpha), !customStyle);
      g.drawString(f, "Singleplayer · last played 2h ago", 176, 103, Colors.scaleAlpha(customStyle ? p.textMuted() : -4144960, alpha), !customStyle);
      CFHGuiDraw.chip(g, Component.literal("Survival"), 176, 112, p.accent(), alpha);
      int playX = 134 + cw - 26;
      int playY = 100;
      CFHGuiDraw.pixelRound(g, playX, 100, 18, 18, Colors.scaleAlpha(p.accent(), alpha), 3);
      Icons.draw(g, Icon.PLAY, playX + 3, 103, 12, Colors.scaleAlpha(p.accentText(), alpha));
      int ay = 134;
      card(g, 134, 134, cw, 30, customStyle, alpha, p);
      CFHGuiDraw.pixelRound(g, 142, 141, 16, 16, Colors.scaleAlpha(-9728422, alpha), 2);
      CFHGuiDraw.pixelRound(g, 145, 144, 10, 10, Colors.scaleAlpha(-3892628, alpha), 1);
      g.drawString(f, "Steve", 166, 140, Colors.scaleAlpha(customStyle ? p.text() : -1, alpha), !customStyle);
      g.drawString(f, "Online · Microsoft account", 166, 151, Colors.scaleAlpha(customStyle ? p.textMuted() : -4144960, alpha), !customStyle);
      g.drawString(f, "Minecraft 1.21.1 · 142 mods · CFHGui", 16, vh - 12, Colors.scaleAlpha(customStyle ? p.textMuted() : -1, alpha), !customStyle);
   }

   private static void card(GuiGraphics g, int x, int y, int w, int h, boolean customStyle, float alpha, Palette p) {
      if (customStyle) {
         CFHGuiDraw.pixelRound(g, x + 2, y + h - 3, w, 5, Colors.scaleAlpha(p.shadow(), alpha), 3);
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(p.surface(), alpha), 3);
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), alpha), 3);
      } else {
         CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(1610612736, alpha));
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-12566464, alpha), 0);
      }

   }

   private static void vanillaTitle(GuiGraphics g, int vw, int vh, boolean customStyle, float alpha) {
      Palette p = dark();
      Font f = font();
      LOGO.renderLogo(g, vw, alpha, 12);
      int j = vh / 4 + 48;
      int bw = 200;
      int bx = (vw - 200) / 2;
      String[] labels = new String[]{"Singleplayer", "Multiplayer", "Minecraft Realms"};

      for(int i = 0; i < labels.length; ++i) {
         button(g, bx, j + i * 24, 200, 20, labels[i], customStyle, i == 0, alpha, p);
      }

      button(g, bx, j + 72, 98, 20, "Options...", customStyle, false, alpha, p);
      button(g, bx + 102, j + 72, 98, 20, "Quit Game", customStyle, false, alpha, p);
      square(g, bx - 24, j + 72, Icon.LANGUAGE, customStyle, alpha, p);
      square(g, bx + 200 + 4, j + 72, Icon.ACCESSIBILITY, customStyle, alpha, p);
      square(g, bx + 200 + 28, j + 72, Icon.CFHGUI, customStyle, alpha, p);
      g.drawString(f, "Minecraft 1.21.1", 2, vh - 10, Colors.scaleAlpha(-1, alpha), true);
   }

   private static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean customStyle, boolean hot, float alpha, Palette p) {
      if (customStyle) {
         int fill = hot ? p.surfaceHover() : p.surface();
         CFHGuiDraw.pixelRound(g, x + 2, y + h - 3, w, 5, Colors.scaleAlpha(p.shadow(), 0.35F * alpha), 3);
         CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, alpha), 3);
         CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(hot ? p.borderStrong() : p.border(), alpha), 3);
         CFHGuiDraw.textCentered(g, (Component)Component.literal(label), x + w / 2, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(p.text(), alpha));
      } else {
         CFHGuiDraw.vanillaButton(g, x, y, w, h, hot ? 1.0F : 0.0F, true, alpha);
         CFHGuiDraw.textCentered(g, Component.literal(label), x + w / 2, CFHGuiDraw.textY(y, h), Colors.scaleAlpha(-1, alpha), true);
      }

   }

   private static void square(GuiGraphics g, int x, int y, Icon icon, boolean customStyle, float alpha, Palette p) {
      if (customStyle) {
         CFHGuiDraw.pixelRound(g, x, y, 20, 20, Colors.scaleAlpha(p.surface(), alpha), 3);
         CFHGuiDraw.outline(g, x, y, 20, 20, Colors.scaleAlpha(p.border(), alpha), 3);
         Icons.draw(g, icon, x + 4, y + 4, 12, Colors.scaleAlpha(icon == Icon.CFHGUI ? p.accentInk() : p.textMuted(), alpha));
      } else {
         CFHGuiDraw.vanillaButton(g, x, y, 20, 20, 0.0F, true, alpha);
         Icons.draw(g, icon, x + 4, y + 4, 12, Colors.scaleAlpha(-1, alpha));
      }

   }

   static void container(GuiGraphics g, Rect r, boolean custom, float alpha) {
      if (r.w() >= 20 && r.h() >= 20) {
         float s = Math.min((float)r.w() / 176.0F, (float)r.h() / 166.0F);
         int dw = Math.round(176.0F * s);
         int dh = Math.round(166.0F * s);
         int ox = r.x() + (r.w() - dw) / 2;
         int oy = r.y() + (r.h() - dh) / 2;
         g.enableScissor(r.x(), r.y(), r.right(), r.bottom());
         g.pose().pushPose();
         g.pose().translate((float)ox, (float)oy, 0.0F);
         g.pose().scale(s, s, 1.0F);
         Font f = font();
         if (custom) {
            ReskinDraw.containerPanel(g, 0, 0, 176, 166);

            for(int i = 0; i < 4; ++i) {
               ReskinDraw.containerSlot(g, 7, 7 + i * 18);
            }

            ReskinDraw.containerSlot(g, 76, 61);

            for(int row = 0; row < 2; ++row) {
               for(int col = 0; col < 2; ++col) {
                  ReskinDraw.containerSlot(g, 97 + col * 18, 17 + row * 18);
               }
            }

            ReskinDraw.containerSlot(g, 153, 27);

            for(int row = 0; row < 3; ++row) {
               for(int col = 0; col < 9; ++col) {
                  ReskinDraw.containerSlot(g, 7 + col * 18, 83 + row * 18);
               }
            }

            for(int col = 0; col < 9; ++col) {
               ReskinDraw.containerSlot(g, 7 + col * 18, 141);
            }

            ReskinDraw.containerInset(g, 25, 7, 51, 72);
            ReskinDraw.containerArrow(g, 133, 30);
            g.drawString(f, "Crafting", 97, 8, dark().text(), false);
         } else {
            RenderSystem.enableBlend();
            g.setColor(1.0F, 1.0F, 1.0F, alpha);
            g.blit(INVENTORY, 0, 0, 0.0F, 0.0F, 176, 166, 256, 256);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            g.drawString(f, "Crafting", 97, 8, -12566464, false);
         }

         int body = custom ? Colors.withAlpha(dark().textDim(), 80) : 805306368;
         CFHGuiDraw.pixelRound(g, 44, 14, 12, 12, body, 2);
         CFHGuiDraw.pixelRound(g, 42, 28, 16, 24, body, 2);
         CFHGuiDraw.rect(g, 44, 54, 5, 20, body);
         CFHGuiDraw.rect(g, 51, 54, 5, 20, body);

         for(int i = 0; i < HOTBAR.length; ++i) {
            g.renderItem(HOTBAR[i], 8 + i * 18, 142);
         }

         for(int i = 0; i < ROW.length; ++i) {
            g.renderItem(ROW[i], 8 + i * 18, 84);
         }

         g.renderItem(new ItemStack(Items.IRON_CHESTPLATE), 8, 26);
         g.renderItem(new ItemStack(Items.SHIELD), 77, 62);
         if (custom) {
            ReskinDraw.containerSlotHighlight(g, 44, 142, 0);
         } else {
            CFHGuiDraw.rect(g, 44, 142, 16, 16, Colors.scaleAlpha(-2130706433, alpha));
         }

         g.pose().popPose();
         g.disableScissor();
      }
   }

   static {
      HOTBAR = new ItemStack[]{new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.TORCH), new ItemStack(Items.BREAD), new ItemStack(Items.OAK_STAIRS)};
      ROW = new ItemStack[]{new ItemStack(Items.COBBLESTONE), new ItemStack(Items.OAK_LOG), new ItemStack(Items.GLASS)};
   }
}
