package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Quaternionf;

public class TextNode extends StageNode {
   private Component text;
   private FormattedCharSequence sequence;
   private float size = 0.025F;
   private boolean billboard = true;
   private boolean seeThrough;
   private boolean shadow;
   private int color = -1;
   private int background = 1073741824;
   private final Quaternionf facing = new Quaternionf();

   public TextNode(Component text) {
      this.text = text;
      this.sequence = text.getVisualOrderText();
      this.pickable(false);
      this.named(text);
      this.updateBounds();
   }

   public TextNode text(Component t) {
      this.text = t;
      this.sequence = t.getVisualOrderText();
      this.named(t);
      this.updateBounds();
      return this;
   }

   public Component text() {
      return this.text;
   }

   public TextNode size(float blocksPerPixel) {
      this.size = blocksPerPixel;
      this.updateBounds();
      return this;
   }

   public TextNode billboard(boolean on) {
      this.billboard = on;
      return this;
   }

   public TextNode seeThrough(boolean on) {
      this.seeThrough = on;
      return this;
   }

   public TextNode shadow(boolean on) {
      this.shadow = on;
      return this;
   }

   public TextNode color(int argb) {
      this.color = argb;
      return this;
   }

   public TextNode background(int argb) {
      this.background = argb;
      return this;
   }

   private void updateBounds() {
      Font font = Minecraft.getInstance().font;
      float w = (float)font.width(this.sequence) * this.size;
      Objects.requireNonNull(font);
      float h = 9.0F * this.size;
      this.bounds(-w / 2.0F, -h / 2.0F, -0.05F, w / 2.0F, h / 2.0F, 0.05F);
   }

   protected void draw(StageRenderContext ctx) {
      Font font = Minecraft.getInstance().font;
      ctx.pose.pushPose();
      if (this.billboard) {
         ctx.pose.last().pose().getNormalizedRotation(this.facing);
         this.facing.conjugate();
         ctx.pose.mulPose(this.facing);
      }

      ctx.pose.scale(this.size, -this.size, this.size);
      float x = (float)(-font.width(this.sequence)) / 2.0F;
      Objects.requireNonNull(font);
      float y = (float)(-9) / 2.0F;
      float alpha = this.alpha();
      int c = alpha < 1.0F ? Math.round((float)(this.color >>> 24 & 255) * alpha) << 24 | this.color & 16777215 : this.color;
      int bg = alpha < 1.0F ? Math.round((float)(this.background >>> 24 & 255) * alpha) << 24 | this.background & 16777215 : this.background;
      Font.DisplayMode mode = this.seeThrough ? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL;
      if (this.seeThrough) {
         font.drawInBatch(this.sequence, x, y, c, this.shadow, ctx.pose.last().pose(), ctx.buffers, mode, bg, ctx.light);
         font.drawInBatch(this.sequence, x, y, c, this.shadow, ctx.pose.last().pose(), ctx.buffers, DisplayMode.NORMAL, 0, ctx.light);
      } else {
         font.drawInBatch(this.sequence, x, y, c, this.shadow, ctx.pose.last().pose(), ctx.buffers, mode, bg, ctx.light);
      }

      ctx.pose.popPose();
   }
}
