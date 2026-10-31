package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class SplinePath implements CameraPath {
   private final List<CameraPose> keys = new ArrayList();
   private final float durationMs;
   private final Ease ease;

   public SplinePath(List<CameraPose> points, float durationMs, Ease ease) {
      for(CameraPose p : points) {
         this.keys.add(p.copy());
      }

      if (this.keys.isEmpty()) {
         this.keys.add(new CameraPose());
      }

      this.durationMs = Math.max(0.0F, durationMs);
      this.ease = ease == null ? Ease.OUT_CUBIC : ease;
   }

   public void sample(float t, CameraPose out) {
      int n = this.keys.size();
      if (n == 1) {
         out.set((CameraPose)this.keys.get(0));
      } else {
         float f = Mth.clamp(t, 0.0F, 1.0F) * (float)(n - 1);
         int i = Math.min(n - 2, (int)f);
         float u = f - (float)i;
         CameraPose p0 = (CameraPose)this.keys.get(Math.max(0, i - 1));
         CameraPose p1 = (CameraPose)this.keys.get(i);
         CameraPose p2 = (CameraPose)this.keys.get(i + 1);
         CameraPose p3 = (CameraPose)this.keys.get(Math.min(n - 1, i + 2));
         catmull(p0.position, p1.position, p2.position, p3.position, u, out.position);
         catmull(p0.target, p1.target, p2.target, p3.target, u, out.target);
         out.fov = Mth.lerp(u, p1.fov, p2.fov);
         out.roll = Mth.lerp(u, p1.roll, p2.roll);
      }
   }

   private static void catmull(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t, Vector3f out) {
      float t2 = t * t;
      float t3 = t2 * t;
      out.x = 0.5F * (2.0F * p1.x + (-p0.x + p2.x) * t + (2.0F * p0.x - 5.0F * p1.x + 4.0F * p2.x - p3.x) * t2 + (-p0.x + 3.0F * p1.x - 3.0F * p2.x + p3.x) * t3);
      out.y = 0.5F * (2.0F * p1.y + (-p0.y + p2.y) * t + (2.0F * p0.y - 5.0F * p1.y + 4.0F * p2.y - p3.y) * t2 + (-p0.y + 3.0F * p1.y - 3.0F * p2.y + p3.y) * t3);
      out.z = 0.5F * (2.0F * p1.z + (-p0.z + p2.z) * t + (2.0F * p0.z - 5.0F * p1.z + 4.0F * p2.z - p3.z) * t2 + (-p0.z + 3.0F * p1.z - 3.0F * p2.z + p3.z) * t3);
   }

   public float durationMs() {
      return this.durationMs;
   }

   public Ease ease() {
      return this.ease;
   }
}
