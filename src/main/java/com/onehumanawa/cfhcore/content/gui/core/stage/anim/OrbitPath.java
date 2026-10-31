package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class OrbitPath implements CameraPath {
   private final Vector3f center = new Vector3f();
   private final float radius;
   private final float height;
   private final float fromDeg;
   private final float toDeg;
   private final float fov;
   private final float targetHeight;
   private final float durationMs;
   private final Ease ease;

   public OrbitPath(Vector3f center, float radius, float height, float fromDeg, float toDeg, float fov, float targetHeight, float durationMs, Ease ease) {
      this.center.set(center);
      this.radius = radius;
      this.height = height;
      this.fromDeg = fromDeg;
      this.toDeg = toDeg;
      this.fov = fov;
      this.targetHeight = targetHeight;
      this.durationMs = Math.max(0.0F, durationMs);
      this.ease = ease == null ? Ease.LINEAR : ease;
   }

   public void sample(float t, CameraPose out) {
      float a = (float)Math.toRadians((double)Mth.lerp(t, this.fromDeg, this.toDeg));
      out.position.set(this.center.x + Mth.sin(a) * this.radius, this.center.y + this.height, this.center.z + Mth.cos(a) * this.radius);
      out.target.set(this.center.x, this.center.y + this.targetHeight, this.center.z);
      out.fov = this.fov;
      out.roll = 0.0F;
   }

   public float durationMs() {
      return this.durationMs;
   }

   public Ease ease() {
      return this.ease;
   }
}
