package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import org.jetbrains.annotations.Nullable;

public final class HoldPath implements CameraPath {
   private final @Nullable CameraPose pose;
   private final CameraPose current = new CameraPose();
   private final float durationMs;

   public HoldPath(@Nullable CameraPose pose, float durationMs) {
      this.pose = pose == null ? null : pose.copy();
      this.durationMs = Math.max(0.0F, durationMs);
   }

   public void begin(CameraPose currentPose) {
      this.current.set(currentPose);
   }

   public void sample(float t, CameraPose out) {
      out.set(this.pose != null ? this.pose : this.current);
   }

   public float durationMs() {
      return this.durationMs;
   }

   public Ease ease() {
      return Ease.LINEAR;
   }
}
