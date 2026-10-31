package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import org.jetbrains.annotations.Nullable;

public final class LinePath implements CameraPath {
   private final @Nullable CameraPose from;
   private final CameraPose to;
   private final CameraPose start = new CameraPose();
   private final float durationMs;
   private final Ease ease;

   public LinePath(@Nullable CameraPose from, CameraPose to, float durationMs, Ease ease) {
      this.from = from == null ? null : from.copy();
      this.to = to.copy();
      this.durationMs = Math.max(0.0F, durationMs);
      this.ease = ease == null ? Ease.OUT_CUBIC : ease;
      if (this.from != null) {
         this.start.set(this.from);
      }

   }

   public void begin(CameraPose current) {
      this.start.set(this.from != null ? this.from : current);
   }

   public void sample(float t, CameraPose out) {
      out.lerp(this.start, this.to, t);
   }

   public float durationMs() {
      return this.durationMs;
   }

   public Ease ease() {
      return this.ease;
   }
}
