package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class CameraPose {
   public final Vector3f position = new Vector3f(0.0F, 2.0F, 6.0F);
   public final Vector3f target = new Vector3f(0.0F, 1.0F, 0.0F);
   public float fov = 45.0F;
   public float roll = 0.0F;

   public CameraPose() {
   }

   public CameraPose(float px, float py, float pz, float tx, float ty, float tz, float fov, float roll) {
      this.set(px, py, pz, tx, ty, tz, fov, roll);
   }

   public CameraPose set(float px, float py, float pz, float tx, float ty, float tz, float fov, float roll) {
      this.position.set(px, py, pz);
      this.target.set(tx, ty, tz);
      this.fov = fov;
      this.roll = roll;
      return this;
   }

   public CameraPose set(CameraPose other) {
      this.position.set(other.position);
      this.target.set(other.target);
      this.fov = other.fov;
      this.roll = other.roll;
      return this;
   }

   public CameraPose copy() {
      return (new CameraPose()).set(this);
   }

   public CameraPose lerp(CameraPose a, CameraPose b, float t) {
      this.position.set(a.position).lerp(b.position, t);
      this.target.set(a.target).lerp(b.target, t);
      this.fov = Mth.lerp(t, a.fov, b.fov);
      this.roll = Mth.lerp(t, a.roll, b.roll);
      return this;
   }

   public String toString() {
      return "CameraPose[" + this.position.x + "," + this.position.y + "," + this.position.z + " -> " + this.target.x + "," + this.target.y + "," + this.target.z + " fov=" + this.fov + " roll=" + this.roll + "]";
   }
}
