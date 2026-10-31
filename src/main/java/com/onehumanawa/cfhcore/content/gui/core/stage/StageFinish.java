package com.onehumanawa.cfhcore.content.gui.core.stage;

public final class StageFinish {
   public float bloom = 0.42F;
   public float threshold = 0.8F;
   public float spread = 1.6F;
   public float vignette = 0.34F;
   public float contrast = 1.07F;
   public float saturation = 1.12F;
   public float warmth = 0.012F;
   public final float[] shadows = new float[3];
   public final float[] highlights = new float[3];
   public float grain;

   public static StageFinish cinematic() {
      return new StageFinish();
   }

   public StageFinish shadows(float r, float g, float b) {
      this.shadows[0] = r;
      this.shadows[1] = g;
      this.shadows[2] = b;
      return this;
   }

   public StageFinish highlights(float r, float g, float b) {
      this.highlights[0] = r;
      this.highlights[1] = g;
      this.highlights[2] = b;
      return this;
   }

   public StageFinish grain(float amount) {
      this.grain = Math.max(0.0F, amount);
      return this;
   }

   public StageFinish contrast(float amount) {
      this.contrast = amount;
      return this;
   }

   public StageFinish saturation(float amount) {
      this.saturation = amount;
      return this;
   }

   public StageFinish threshold(float value) {
      this.threshold = value;
      return this;
   }

   public StageFinish spread(float steps) {
      this.spread = steps;
      return this;
   }

   public static StageFinish glow() {
      StageFinish f = new StageFinish();
      f.vignette = 0.0F;
      f.bloom = 0.34F;
      return f;
   }

   public StageFinish bloom(float amount) {
      this.bloom = amount;
      return this;
   }

   public StageFinish vignette(float amount) {
      this.vignette = amount;
      return this;
   }

   public StageFinish warmth(float amount) {
      this.warmth = amount;
      return this;
   }
}
