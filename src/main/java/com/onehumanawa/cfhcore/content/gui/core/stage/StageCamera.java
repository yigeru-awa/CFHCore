package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPose;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.Timeline;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class StageCamera {
   private final CameraPose pose = new CameraPose();
   private final CameraPose rendered = new CameraPose();
   private final Timeline timeline = new Timeline();
   private IdleMotion idle;
   private float near;
   private float far;
   private float aspect;
   private float timeMs;
   private final Matrix4f view;
   private final Matrix4f projection;
   private final Matrix4f viewProjection;
   private final Matrix4f invViewProjection;
   private final Vector3f up;
   private final Vector3f dir;
   private final Vector4f a;
   private final Vector4f b;

   public StageCamera() {
      this.idle = IdleMotion.NONE;
      this.near = 0.05F;
      this.far = 256.0F;
      this.aspect = 1.0F;
      this.view = new Matrix4f();
      this.projection = new Matrix4f();
      this.viewProjection = new Matrix4f();
      this.invViewProjection = new Matrix4f();
      this.up = new Vector3f(0.0F, 1.0F, 0.0F);
      this.dir = new Vector3f();
      this.a = new Vector4f();
      this.b = new Vector4f();
   }

   public CameraPose pose() {
      return this.pose;
   }

   public StageCamera at(float x, float y, float z) {
      this.pose.position.set(x, y, z);
      return this;
   }

   public StageCamera lookAt(float x, float y, float z) {
      this.pose.target.set(x, y, z);
      return this;
   }

   public StageCamera fov(float degrees) {
      this.pose.fov = Mth.clamp(degrees, 5.0F, 150.0F);
      return this;
   }

   public StageCamera roll(float degrees) {
      this.pose.roll = degrees;
      return this;
   }

   public StageCamera clip(float near, float far) {
      this.near = Math.max(0.001F, near);
      this.far = Math.max(this.near + 0.01F, far);
      return this;
   }

   public StageCamera idle(IdleMotion motion) {
      this.idle = motion == null ? IdleMotion.NONE : motion;
      return this;
   }

   public IdleMotion idle() {
      return this.idle;
   }

   public Timeline timeline() {
      return this.timeline;
   }

   public void update(float deltaMs, float aspect) {
      this.timeMs += deltaMs;
      this.aspect = aspect <= 0.0F ? 1.0F : aspect;
      this.timeline.advance(deltaMs, this.pose);
      if (this.timeline.finished()) {
         this.idle.apply(this.pose, this.timeMs, this.rendered);
      } else {
         this.rendered.set(this.pose);
      }

      this.computeMatrices();
   }

   private void computeMatrices() {
      this.projection.setPerspective((float)Math.toRadians((double)this.rendered.fov), this.aspect, this.near, this.far);
      this.dir.set(this.rendered.target).sub(this.rendered.position);
      if (this.dir.lengthSquared() < 1.0E-8F) {
         this.dir.set(0.0F, 0.0F, -1.0F);
      }

      this.dir.normalize();
      if (Math.abs(this.dir.y) > 0.999F) {
         this.up.set(0.0F, 0.0F, -1.0F);
      } else {
         this.up.set(0.0F, 1.0F, 0.0F);
      }

      this.view.identity();
      if (this.rendered.roll != 0.0F) {
         this.view.rotateZ((float)Math.toRadians((double)this.rendered.roll));
      }

      this.view.lookAt(this.rendered.position.x, this.rendered.position.y, this.rendered.position.z, this.rendered.position.x + this.dir.x, this.rendered.position.y + this.dir.y, this.rendered.position.z + this.dir.z, this.up.x, this.up.y, this.up.z);
      this.viewProjection.set(this.projection).mul(this.view);
      this.viewProjection.invert(this.invViewProjection);
   }

   public CameraPose rendered() {
      return this.rendered;
   }

   public Matrix4f view() {
      return this.view;
   }

   public Matrix4f projection() {
      return this.projection;
   }

   public float aspect() {
      return this.aspect;
   }

   public float near() {
      return this.near;
   }

   public float far() {
      return this.far;
   }

   public void ray(float ndcX, float ndcY, Vector3f originOut, Vector3f dirOut) {
      this.a.set(ndcX, ndcY, -1.0F, 1.0F);
      this.invViewProjection.transform(this.a);
      this.a.div(this.a.w);
      this.b.set(ndcX, ndcY, 1.0F, 1.0F);
      this.invViewProjection.transform(this.b);
      this.b.div(this.b.w);
      originOut.set(this.a.x, this.a.y, this.a.z);
      dirOut.set(this.b.x - this.a.x, this.b.y - this.a.y, this.b.z - this.a.z);
      if (dirOut.lengthSquared() < 1.0E-12F) {
         dirOut.set(0.0F, 0.0F, -1.0F);
      } else {
         dirOut.normalize();
      }

   }

   public boolean project(float x, float y, float z, Vector3f ndcOut) {
      this.a.set(x, y, z, 1.0F);
      this.viewProjection.transform(this.a);
      if (this.a.w <= 1.0E-6F) {
         return false;
      } else {
         ndcOut.set(this.a.x / this.a.w, this.a.y / this.a.w, this.a.z / this.a.w);
         return true;
      }
   }
}
