package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class StageLighting {
   public static final int LAMPS = 8;
   private final Vector3f sun = (new Vector3f(0.35F, 1.0F, 0.55F)).normalize();
   private float ambient = 0.45F;
   private float sunInfluence = 0.55F;
   private int blockLight = 15;
   private int skyLight = 15;
   private float brightness = 1.0F;
   private int generation;
   private final Vector3f keyPos = new Vector3f(2.6F, 5.2F, 6.5F);
   private final Vector3f keyColor = new Vector3f(0.6F, 0.555F, 0.48F);
   private final Vector3f fillDir = (new Vector3f(-1.0F, 0.25F, 0.2F)).normalize();
   private final Vector3f fillColor = new Vector3f(0.11F, 0.14F, 0.2F);
   private final Vector3f ambientColor = new Vector3f(0.37F, 0.35F, 0.36F);
   private final Vector3f rimColor = new Vector3f(1.0F, 0.9F, 0.76F);
   private float rimStrength = 0.2F;
   private float wrap = 0.45F;
   private final float[] lamps = new float[64];
   private final float[] lampStrength = new float[8];
   private final Vector3f light0 = new Vector3f();
   private final Vector3f light1 = new Vector3f();
   private final Vector3f fill = new Vector3f();
   private final Vector3f lampScratch = new Vector3f();
   private static final Vector3f[] DIR = new Vector3f[6];

   public StageLighting sun(float x, float y, float z) {
      this.sun.set(x, y, z);
      if (this.sun.lengthSquared() < 1.0E-6F) {
         this.sun.set(0.0F, 1.0F, 0.0F);
      }

      this.sun.normalize();
      ++this.generation;
      return this;
   }

   public Vector3f sun() {
      return this.sun;
   }

   public StageLighting ambient(float a) {
      this.ambient = Mth.clamp(a, 0.0F, 1.0F);
      ++this.generation;
      return this;
   }

   public float ambient() {
      return this.ambient;
   }

   public StageLighting sunInfluence(float f) {
      this.sunInfluence = Mth.clamp(f, 0.0F, 1.0F);
      ++this.generation;
      return this;
   }

   public StageLighting light(int block, int sky) {
      this.blockLight = Mth.clamp(block, 0, 15);
      this.skyLight = Mth.clamp(sky, 0, 15);
      ++this.generation;
      return this;
   }

   public int blockLight() {
      return this.blockLight;
   }

   public int skyLight() {
      return this.skyLight;
   }

   public int packedLight() {
      return this.skyLight << 20 | this.blockLight << 4;
   }

   public StageLighting brightness(float b) {
      this.brightness = Mth.clamp(b, 0.0F, 2.0F);
      return this;
   }

   public float brightness() {
      return this.brightness;
   }

   public StageLighting key(float x, float y, float z) {
      this.keyPos.set(x, y, z);
      return this;
   }

   public StageLighting keyColor(float r, float g, float b) {
      this.keyColor.set(r, g, b);
      return this;
   }

   public StageLighting fill(float x, float y, float z, float r, float g, float b) {
      this.fillDir.set(x, y, z);
      if (this.fillDir.lengthSquared() < 1.0E-6F) {
         this.fillDir.set(-1.0F, 0.0F, 0.0F);
      }

      this.fillDir.normalize();
      this.fillColor.set(r, g, b);
      return this;
   }

   public StageLighting ambientColor(float r, float g, float b) {
      this.ambientColor.set(r, g, b);
      return this;
   }

   public StageLighting rim(float r, float g, float b, float strength) {
      this.rimColor.set(r, g, b);
      this.rimStrength = Math.max(0.0F, strength);
      return this;
   }

   public StageLighting wrap(float w) {
      this.wrap = Mth.clamp(w, 0.0F, 1.0F);
      return this;
   }

   public int lamp(float x, float y, float z, float r, float g, float b, float reach) {
      for(int i = 0; i < 8; ++i) {
         if (!(this.lamps[i * 8 + 3] > 0.0F)) {
            this.lamp(i, x, y, z, r, g, b, reach);
            return i;
         }
      }

      return -1;
   }

   public StageLighting lamp(int index, float x, float y, float z, float r, float g, float b, float reach) {
      if (index >= 0 && index < 8) {
         int o = index * 8;
         this.lamps[o] = x;
         this.lamps[o + 1] = y;
         this.lamps[o + 2] = z;
         this.lamps[o + 3] = Math.max(0.0F, reach);
         this.lamps[o + 4] = r;
         this.lamps[o + 5] = g;
         this.lamps[o + 6] = b;
         this.lampStrength[index] = 1.0F;
         return this;
      } else {
         return this;
      }
   }

   public StageLighting lampFlicker(int index, float amount) {
      if (index >= 0 && index < 8) {
         this.lamps[index * 8 + 7] = Mth.clamp(amount, 0.0F, 1.0F);
      }

      return this;
   }

   public StageLighting lampStrength(int index, float strength) {
      if (index >= 0 && index < 8) {
         this.lampStrength[index] = Mth.clamp(strength, 0.0F, 2.0F);
      }

      return this;
   }

   public StageLighting lampAt(int index, float x, float y, float z) {
      if (index >= 0 && index < 8) {
         this.lamps[index * 8] = x;
         this.lamps[index * 8 + 1] = y;
         this.lamps[index * 8 + 2] = z;
         return this;
      } else {
         return this;
      }
   }

   public StageLighting clearLamps() {
      Arrays.fill(this.lamps, 0.0F);
      Arrays.fill(this.lampStrength, 0.0F);
      return this;
   }

   public boolean lampOn(int index) {
      return index >= 0 && index < 8 && this.lamps[index * 8 + 3] > 0.0F;
   }

   void lampsFor(Matrix4f view, float seconds, boolean animate, float[] positions, float[] colors) {
      for(int i = 0; i < 8; ++i) {
         int o = i * 8;
         int p = i * 4;
         float reach = this.lamps[o + 3];
         if (!(reach <= 0.0F) && !(this.lampStrength[i] <= 0.0F)) {
            this.lampScratch.set(this.lamps[o], this.lamps[o + 1], this.lamps[o + 2]);
            view.transformPosition(this.lampScratch);
            positions[p] = this.lampScratch.x;
            positions[p + 1] = this.lampScratch.y;
            positions[p + 2] = this.lampScratch.z;
            positions[p + 3] = reach;
            float f = this.lamps[o + 7];
            float s = this.lampStrength[i];
            if (f > 0.0F && animate) {
               float n = 0.5F + 0.24F * Mth.sin(seconds * 7.3F + (float)i * 1.9F) + 0.16F * Mth.sin(seconds * 12.7F + (float)i * 4.1F) + 0.1F * Mth.sin(seconds * 23.1F + (float)i * 0.7F);
               s *= 1.0F - f * (1.0F - n);
            }

            colors[p] = this.lamps[o + 4] * s;
            colors[p + 1] = this.lamps[o + 5] * s;
            colors[p + 2] = this.lamps[o + 6] * s;
            colors[p + 3] = 0.0F;
         } else {
            positions[p] = positions[p + 1] = positions[p + 2] = positions[p + 3] = 0.0F;
            colors[p] = colors[p + 1] = colors[p + 2] = colors[p + 3] = 0.0F;
         }
      }

   }

   public Vector3f keyPos() {
      return this.keyPos;
   }

   public Vector3f keyColor() {
      return this.keyColor;
   }

   public Vector3f fillDir() {
      return this.fillDir;
   }

   public Vector3f fillColor() {
      return this.fillColor;
   }

   public Vector3f ambientColor() {
      return this.ambientColor;
   }

   public Vector3f rimColor() {
      return this.rimColor;
   }

   public float rimStrength() {
      return this.rimStrength;
   }

   public float wrap() {
      return this.wrap;
   }

   public int generation() {
      return this.generation;
   }

   public float shade(Direction direction, boolean shade) {
      if (!shade) {
         return 1.0F;
      } else {
         float var10000;
         switch (direction) {
            case DOWN:
               var10000 = 0.5F;
               break;
            case UP:
               var10000 = 1.0F;
               break;
            case NORTH:
            case SOUTH:
               var10000 = 0.8F;
               break;
            default:
               var10000 = 0.6F;
         }

         float vanilla = var10000;
         float lambert = Mth.clamp(this.ambient + (1.0F - this.ambient) * Math.max(0.0F, DIR[direction.ordinal()].dot(this.sun)), 0.3F, 1.0F);
         return Mth.lerp(this.sunInfluence, vanilla, lambert);
      }
   }

   public void applyShaderLights(Matrix4f view) {
      this.light0.set(this.sun);
      view.transformDirection(this.light0).normalize();
      this.fill.set(-this.sun.x, 0.35F, -this.sun.z);
      if (this.fill.lengthSquared() < 1.0E-6F) {
         this.fill.set(0.0F, 1.0F, 0.0F);
      }

      this.fill.normalize().mul(0.45F);
      this.light1.set(this.fill);
      view.transformDirection(this.light1);
      RenderSystem.setShaderLights(this.light0, this.light1);
   }

   static {
      for(Direction d : Direction.values()) {
         DIR[d.ordinal()] = new Vector3f((float)d.getStepX(), (float)d.getStepY(), (float)d.getStepZ());
      }

   }
}
