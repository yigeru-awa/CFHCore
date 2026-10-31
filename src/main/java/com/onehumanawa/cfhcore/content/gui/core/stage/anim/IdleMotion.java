package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class IdleMotion {
   public static final IdleMotion NONE;
   private final Kind kind;
   private final float periodMs;
   private final float yawDeg;
   private final float pitchDeg;
   private final float bob;
   private final Vector3f d = new Vector3f();

   private IdleMotion(Kind kind, float periodMs, float yawDeg, float pitchDeg, float bob) {
      this.kind = kind;
      this.periodMs = periodMs;
      this.yawDeg = yawDeg;
      this.pitchDeg = pitchDeg;
      this.bob = bob;
   }

   public static IdleMotion sway(float periodMs, float yawDeg, float pitchDeg, float bob) {
      return new IdleMotion(IdleMotion.Kind.SWAY, periodMs, yawDeg, pitchDeg, bob);
   }

   public static IdleMotion orbit(float periodMs, float bob) {
      return new IdleMotion(IdleMotion.Kind.ORBIT, periodMs, 0.0F, 0.0F, bob);
   }

   public Kind kind() {
      return this.kind;
   }

   public void apply(CameraPose base, float timeMs, CameraPose out) {
      out.set(base);
      if (this.kind != IdleMotion.Kind.NONE && !(this.periodMs <= 0.0F) && !(Theme.current().motion() <= 0.0F)) {
         float phase = timeMs / this.periodMs * ((float)Math.PI * 2F);
         float yaw = this.kind == IdleMotion.Kind.ORBIT ? phase : (float)Math.toRadians((double)this.yawDeg) * Mth.sin(phase);
         float pitch = this.kind == IdleMotion.Kind.SWAY ? (float)Math.toRadians((double)this.pitchDeg) * Mth.sin(phase * 0.71F + 1.3F) : 0.0F;
         this.d.set(base.position).sub(base.target);
         float r = this.d.length();
         if (r > 1.0E-4F) {
            float azimuth = (float)Math.atan2((double)this.d.x, (double)this.d.z) + yaw;
            float elevation = (float)Math.asin((double)Mth.clamp(this.d.y / r, -1.0F, 1.0F)) + pitch;
            elevation = Mth.clamp(elevation, -1.5F, 1.5F);
            float horiz = r * Mth.cos(elevation);
            out.position.set(base.target.x + Mth.sin(azimuth) * horiz, base.target.y + r * Mth.sin(elevation), base.target.z + Mth.cos(azimuth) * horiz);
         }

         if (this.bob != 0.0F) {
            float b = this.bob * Mth.sin(phase * 1.37F);
            Vector3f var10000 = out.position;
            var10000.y += b;
            var10000 = out.target;
            var10000.y += b * 0.5F;
         }

      }
   }

   static {
      NONE = new IdleMotion(IdleMotion.Kind.NONE, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public static enum Kind {
      NONE,
      SWAY,
      ORBIT;
   }
}
