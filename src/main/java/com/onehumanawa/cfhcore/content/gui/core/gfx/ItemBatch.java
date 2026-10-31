package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ItemBatch {
   private final List<ItemStack> stacks = new ArrayList();
   private int[] xs = new int[64];
   private int[] ys = new int[64];
   private BakedModel[] models = new BakedModel[64];

   public void add(ItemStack stack, int x, int y) {
      if (!stack.isEmpty()) {
         int i = this.stacks.size();
         if (i == this.xs.length) {
            this.xs = Arrays.copyOf(this.xs, i * 2);
            this.ys = Arrays.copyOf(this.ys, i * 2);
            this.models = (BakedModel[])Arrays.copyOf(this.models, i * 2);
         }

         this.stacks.add(stack);
         this.xs[i] = x;
         this.ys[i] = y;
      }
   }

   public void draw(GuiGraphics g, Font font) {
      int n = this.stacks.size();
      if (n != 0) {
         Minecraft mc = Minecraft.getInstance();
         ItemRenderer renderer = mc.getItemRenderer();

         try {
            g.flush();

            for(int i = 0; i < n; ++i) {
               this.models[i] = renderer.getModel((ItemStack)this.stacks.get(i), mc.level, mc.player, 0);
            }

            this.pass(g, renderer, n, true);
            this.pass(g, renderer, n, false);
            g.drawManaged(() -> {
               for(int i = 0; i < n; ++i) {
                  g.renderItemDecorations(font, (ItemStack)this.stacks.get(i), this.xs[i], this.ys[i]);
               }

            });
         } finally {
            this.stacks.clear();
            Arrays.fill(this.models, 0, n, null);
         }

      }
   }

   private void pass(GuiGraphics g, ItemRenderer renderer, int n, boolean flat) {
      PoseStack pose = g.pose();
      boolean any = false;

      for(int i = 0; i < n; ++i) {
         if (this.models[i].usesBlockLight() != flat) {
            pose.pushPose();

            try {
               pose.translate((float)(this.xs[i] + 8), (float)(this.ys[i] + 8), 150.0F);
               pose.scale(16.0F, -16.0F, 16.0F);
               renderer.render((ItemStack)this.stacks.get(i), ItemDisplayContext.GUI, false, pose, g.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, this.models[i]);
            } finally {
               pose.popPose();
            }

            any = true;
         }
      }

      if (any) {
         if (flat) {
            Lighting.setupForFlatItems();
         }

         g.flush();
         if (flat) {
            Lighting.setupFor3DItems();
         }

      }
   }
}
