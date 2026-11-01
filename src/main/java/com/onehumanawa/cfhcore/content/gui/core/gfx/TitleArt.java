package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class TitleArt {
   public static final ResourceLocation TITLE = ResourceLocation.fromNamespaceAndPath("cfhcore", "textures/gui/title/create.png");
   public static final ResourceLocation SUBTITLE = ResourceLocation.fromNamespaceAndPath("cfhcore", "textures/gui/title/forge_of_horizons.png");
   private static final int TITLE_TEX_W = 814;
   private static final int TITLE_TEX_H = 207;
   private static final int SUB_TEX_W = 914;
   private static final int SUB_TEX_H = 90;
   private static final float SCALE = 0.8F;
   public static final int TITLE_W = Math.round(256.0F * SCALE);
   public static final int TITLE_H = Math.round((float)TITLE_W * (float)TITLE_TEX_H / (float)TITLE_TEX_W);
   public static final int SUB_W = Math.round(128.0F * SCALE);
   public static final int SUB_H = Math.round((float)SUB_W * (float)SUB_TEX_H / (float)SUB_TEX_W);
   public static final int SUB_OVERLAP = Math.max(1, Math.round(7.0F * SCALE));
   public static final int BLOCK_H = TITLE_H + SUB_H - SUB_OVERLAP;

   private TitleArt() {
   }

   public static void render(GuiGraphics g, int screenWidth, float alpha, int y) {
      render(g, screenWidth, alpha, y, 1.0F);
   }

   public static void render(GuiGraphics g, int screenWidth, float alpha, int y, float scale) {
      RenderSystem.enableBlend();
      g.setColor(1.0F, 1.0F, 1.0F, alpha);
      int cx = screenWidth / 2;
      int tw = Math.round((float)TITLE_W * scale);
      int th = Math.round((float)TITLE_H * scale);
      int sw = Math.round((float)SUB_W * scale);
      int sh = Math.round((float)SUB_H * scale);
      int overlap = Math.max(1, Math.round((float)SUB_OVERLAP * scale));
      g.blit(TITLE, cx - tw / 2, y, tw, th, 0.0F, 0.0F, TITLE_TEX_W, TITLE_TEX_H, TITLE_TEX_W, TITLE_TEX_H);
      g.blit(SUBTITLE, cx - sw / 2, y + th - overlap, sw, sh, 0.0F, 0.0F, SUB_TEX_W, SUB_TEX_H, SUB_TEX_W, SUB_TEX_H);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
   }
}
