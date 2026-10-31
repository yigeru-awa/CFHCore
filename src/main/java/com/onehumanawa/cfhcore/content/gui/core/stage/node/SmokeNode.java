package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SmokeNode extends StageNode {
   private final Kind kind;
   private final int count;
   private final float[] phase;
   private final float[] jx;
   private final float[] jz;
   private final float[] wobble;
   private final float[] wobbleSpeed;
   private final float[] lifeScale;
   private final float[] spin;
   private final int[] frame;
   private final float[] age;
   private final int[] order;
   private float life = 6.0F;
   private float rise = 0.55F;
   private float spread = 0.5F;
   private float windX = 0.12F;
   private float windZ = 0.0F;
   private float sizeFrom = 0.35F;
   private float sizeTo = 1.4F;
   private int colorFrom = -1334810010;
   private int colorTo = 3157550;
   private float strength = 1.0F;
   private @Nullable TextureAtlasSprite[] sprites;
   private int spritesFor = -1;
   private final Vector3f right = new Vector3f();
   private final Vector3f up = new Vector3f();

   public SmokeNode(Kind kind, int count, long seed) {
      this.kind = kind;
      this.count = Math.max(1, count);
      Random rnd = new Random(seed);
      this.phase = new float[this.count];
      this.jx = new float[this.count];
      this.jz = new float[this.count];
      this.wobble = new float[this.count];
      this.wobbleSpeed = new float[this.count];
      this.lifeScale = new float[this.count];
      this.spin = new float[this.count];
      this.frame = new int[this.count];
      this.age = new float[this.count];
      this.order = new int[this.count];

      for(int i = 0; i < this.count; ++i) {
         this.phase[i] = ((float)i + rnd.nextFloat() * 0.6F) / (float)this.count;
         float a = rnd.nextFloat() * ((float)Math.PI * 2F);
         float r = (float)Math.sqrt((double)rnd.nextFloat());
         this.jx[i] = Mth.cos(a) * r;
         this.jz[i] = Mth.sin(a) * r;
         this.wobble[i] = rnd.nextFloat() * ((float)Math.PI * 2F);
         this.wobbleSpeed[i] = 0.6F + rnd.nextFloat() * 0.9F;
         this.lifeScale[i] = 0.8F + rnd.nextFloat() * 0.4F;
         this.spin[i] = rnd.nextBoolean() ? 1.0F : -1.0F;
         this.frame[i] = rnd.nextInt(Math.max(1, kind.frames));
      }

      this.pickable(false);
      this.hoverFeel(0.0F, 1.0F);
      this.named("smoke");
      this.bounds(-1.0F, 0.0F, -1.0F, 1.0F, 4.0F, 1.0F);
   }

   public SmokeNode rise(float seconds, float blocksPerSecond) {
      this.life = Math.max(0.2F, seconds);
      this.rise = blocksPerSecond;
      return this;
   }

   public SmokeNode spread(float blocks) {
      this.spread = Math.max(0.0F, blocks);
      return this;
   }

   public SmokeNode wind(float x, float z) {
      this.windX = x;
      this.windZ = z;
      return this;
   }

   public SmokeNode size(float from, float to) {
      this.sizeFrom = from;
      this.sizeTo = to;
      return this;
   }

   public SmokeNode colors(int from, int to) {
      this.colorFrom = from;
      this.colorTo = to;
      return this;
   }

   public SmokeNode strength(float s) {
      this.strength = Mth.clamp(s, 0.0F, 1.0F);
      return this;
   }

   protected void draw(StageRenderContext ctx) {
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      float opacity = this.strength * this.alpha();
      if (!(opacity <= 0.004F)) {
         TextureAtlasSprite[] sheet = this.sprites();
         boolean motion = Theme.current().motion() > 0.0F;
         float t = motion ? ctx.seconds() : 7.3F;

         for(int i = 0; i < this.count; this.order[i] = i++) {
            float l = this.life * this.lifeScale[i];
            float u = t / l + this.phase[i];
            this.age[i] = u - (float)Math.floor((double)u);
         }

         for(int i = 1; i < this.count; ++i) {
            int k = this.order[i];

            int j;
            for(j = i - 1; j >= 0 && this.age[this.order[j]] < this.age[k]; --j) {
               this.order[j + 1] = this.order[j];
            }

            this.order[j + 1] = k;
         }

         Matrix4f m = ctx.pose.last().pose();
         Matrix4f view = ctx.camera.view();
         this.right.set(view.m00(), view.m10(), view.m20());
         this.up.set(view.m01(), view.m11(), view.m21());
         this.modelInverse.transformDirection(this.right).normalize();
         this.modelInverse.transformDirection(this.up).normalize();
         RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
         RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
         RenderSystem.enableBlend();
         StageBlend.over();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         int drawn = 0;

         for(int n = 0; n < this.count; ++n) {
            int i = this.order[n];
            float u = this.age[i];
            float l = this.life * this.lifeScale[i];
            float left = 1.0F - u;
            float py = this.rise * l * (1.0F - 0.35F * left * left - 0.65F * left);
            float out = this.spread * (float)Math.sqrt((double)u);
            float drift = l * u * u;
            float sway = motion ? Mth.sin(t * this.wobbleSpeed[i] + this.wobble[i]) * 0.08F * u : 0.0F;
            float px = this.jx[i] * out + this.windX * drift + sway;
            float pz = this.jz[i] * out + this.windZ * drift;
            float fade = Math.min(1.0F, u / 0.12F) * left * left;
            int c = Colors.lerp(this.colorFrom, this.colorTo, u);
            int a = Math.round((float)(c >>> 24 & 255) * opacity * fade);
            if (a > 1) {
               float s = Mth.lerp((float)Math.pow((double)u, 0.8), this.sizeFrom, this.sizeTo) / 2.0F;
               int r = c >> 16 & 255;
               int g = c >> 8 & 255;
               int b = c & 255;
               float turn = this.spin[i] * u * 0.9F;
               float cos = Mth.cos(turn);
               float sin = Mth.sin(turn);
               float ax = (this.right.x * cos + this.up.x * sin) * s;
               float ay = (this.right.y * cos + this.up.y * sin) * s;
               float az = (this.right.z * cos + this.up.z * sin) * s;
               float bx = (this.up.x * cos - this.right.x * sin) * s;
               float by = (this.up.y * cos - this.right.y * sin) * s;
               float bz = (this.up.z * cos - this.right.z * sin) * s;
               float u0 = 0.0F;
               float u1 = 1.0F;
               float v0 = 0.0F;
               float v1 = 1.0F;
               if (sheet != null) {
                  int f = this.kind.byAge ? Math.min(this.kind.frames - 1, (int)(u * (float)this.kind.frames)) : this.frame[i];
                  TextureAtlasSprite sp = sheet[Math.min(sheet.length - 1, f)];
                  u0 = sp.getU0();
                  u1 = sp.getU1();
                  v0 = sp.getV0();
                  v1 = sp.getV1();
               }

               bb.addVertex(m, px - ax - bx, py - ay - by, pz - az - bz).setUv(u0, v1).setColor(r, g, b, a);
               bb.addVertex(m, px + ax - bx, py + ay - by, pz + az - bz).setUv(u1, v1).setColor(r, g, b, a);
               bb.addVertex(m, px + ax + bx, py + ay + by, pz + az + bz).setUv(u1, v0).setColor(r, g, b, a);
               bb.addVertex(m, px - ax + bx, py - ay + by, pz - az + bz).setUv(u0, v0).setColor(r, g, b, a);
               ++drawn;
            }
         }

         MeshData mesh = bb.build();
         if (mesh != null && drawn > 0) {
            BufferUploader.drawWithShader(mesh);
         } else if (mesh != null) {
            mesh.close();
         }

         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
      }
   }

   private @Nullable TextureAtlasSprite[] sprites() {
      int gen = StageResources.generation();
      if (this.spritesFor == gen) {
         return this.sprites;
      } else {
         this.spritesFor = gen;
         this.sprites = null;
         AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
         if (tex instanceof TextureAtlas) {
            TextureAtlas atlas = (TextureAtlas)tex;
            TextureAtlasSprite[] out = new TextureAtlasSprite[this.kind.frames];

            for(int i = 0; i < this.kind.frames; ++i) {
               out[i] = atlas.getSprite(ResourceLocation.withDefaultNamespace(this.kind.sprite + "_" + i));
            }

            this.sprites = out;
         }

         return this.sprites;
      }
   }

   public static enum Kind {
      COSY("big_smoke", 12, false),
      BIG("big_smoke", 12, true),
      WISP("generic", 8, true);

      final String sprite;
      final int frames;
      final boolean byAge;

      private Kind(final String sprite, final int frames, final boolean byAge) {
         this.sprite = sprite;
         this.frames = frames;
         this.byAge = byAge;
      }
   }
}
