package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class RiftNode extends StageNode {
   private static final int SPECKS = 150;
   private static final int ARMS = 3;
   private static final float TWIST = 1.15F;
   private static final int TAIL = 3;
   private static final float TAIL_STEP = 0.022F;
   private static final int HEART = 6;
   private final float radius;
   private final float[] arm = new float[150];
   private final float[] phase = new float[150];
   private final float[] speed = new float[150];
   private final float[] size = new float[150];
   private final float[] tone = new float[150];
   private final float[] stray = new float[150];
   private final int warm;
   private final int accent;
   private float strength;
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();

   RiftNode(float radius, int warm, int accent, long seed) {
      this.radius = radius;
      this.warm = warm;
      this.accent = accent;
      Random rnd = new Random(seed);

      for(int i = 0; i < 150; ++i) {
         this.arm[i] = (float)(i % 3) * ((float)Math.PI * 2F) / 3.0F + (rnd.nextFloat() - 0.5F) * 0.34F;
         this.phase[i] = rnd.nextFloat();
         this.speed[i] = 0.1F + rnd.nextFloat() * 0.09F;
         this.size[i] = radius * (0.022F + rnd.nextFloat() * rnd.nextFloat() * 0.06F);
         this.tone[i] = rnd.nextFloat();
         this.stray[i] = (rnd.nextFloat() - 0.5F) * 0.12F;
      }

      this.bounds(-radius, -radius, -radius, radius, radius, radius);
      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("rift");
   }

   RiftNode strength(float s) {
      this.strength = Mth.clamp(s, 0.0F, 1.0F);
      return this;
   }

   protected void draw(StageRenderContext ctx) {
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      float a = this.strength * this.alpha();
      if (!(a <= 0.004F)) {
         Matrix4f m = ctx.pose.last().pose();
         Matrix4f view = ctx.camera.view();
         this.right.set(view.m00(), view.m10(), view.m20());
         this.up.set(view.m01(), view.m11(), view.m21());
         this.modelInverse.transformDirection(this.right).normalize();
         this.modelInverse.transformDirection(this.up).normalize();
         float t = Theme.current().motion() > 0.0F ? ctx.seconds() : 0.0F;
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         StageBlend.add();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for(int k = 0; k < 6; ++k) {
            float f = (float)k / 5.0F;
            this.square(bb, m, 0.0F, 0.0F, this.radius * (0.74F - 0.56F * f), t * (0.18F + 0.1F * (float)k) * ((k & 1) == 0 ? 1.0F : -1.0F) + (float)k * 0.52F, Colors.lerp(this.accent, this.warm, f), (0.035F + 0.05F * f) * a);
         }

         this.square(bb, m, 0.0F, 0.0F, this.radius * 0.1F, t * 0.9F, -1, 0.85F * a);

         for(int i = 0; i < 150; ++i) {
            float head = (this.phase[i] + t * this.speed[i]) % 1.0F;
            float twinkle = 0.7F + 0.3F * Mth.sin(t * 3.1F + (float)i * 1.7F);
            int colour = this.tone[i] > 0.62F ? this.accent : Colors.lerp(this.warm, this.accent, this.tone[i] * 0.5F);

            for(int k = 0; k < 3; ++k) {
               float life = head - (float)k * 0.022F;
               if (life <= 0.0F) {
                  break;
               }

               float in = life * life * (3.0F - 2.0F * life);
               float r = this.radius * (1.0F - in) * (1.0F + this.stray[i]);
               float angle = this.arm[i] + in * 1.15F * ((float)Math.PI * 2F) + t * 0.22F;
               float light = Math.min(1.0F, life / 0.18F) * Math.min(1.0F, (1.0F - life) / 0.22F);
               float pa = a * light * twinkle * (0.35F + 0.5F * this.tone[i]) * (k == 0 ? 1.0F : (k == 1 ? 0.45F : 0.2F));
               if (!(pa <= 0.004F)) {
                  this.square(bb, m, Mth.cos(angle) * r, Mth.sin(angle) * r * 0.9F, this.size[i] * (0.55F + 0.45F * (1.0F - in)) * (1.0F - 0.18F * (float)k), 0.0F, colour, pa);
               }
            }
         }

         MeshData mesh = bb.build();
         if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
         }

         StageBlend.over();
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
      }
   }

   private void square(BufferBuilder bb, Matrix4f m, float x, float y, float half, float turn, int rgb, float alpha) {
      int pa = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
      if (pa > 0) {
         int r = rgb >> 16 & 255;
         int g = rgb >> 8 & 255;
         int b = rgb & 255;
         float c = Mth.cos(turn) * half;
         float s = Mth.sin(turn) * half;
         this.corner(bb, m, x + -c + s, y + (-s - c), r, g, b, pa);
         this.corner(bb, m, x + c + s, y + (s - c), r, g, b, pa);
         this.corner(bb, m, x + (c - s), y + s + c, r, g, b, pa);
         this.corner(bb, m, x + (-c - s), y + -s + c, r, g, b, pa);
      }
   }

   private void corner(BufferBuilder bb, Matrix4f m, float x, float y, int r, int g, int b, int a) {
      bb.addVertex(m, this.right.x * x + this.up.x * y, this.right.y * x + this.up.y * y, this.right.z * x + this.up.z * y).setColor(r, g, b, a);
   }
}
