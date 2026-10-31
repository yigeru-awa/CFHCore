package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

final class Camera {
   final float[] view = new float[16];
   final float[] viewProj = new float[16];
   final float[] eye = new float[3];
   final float[] right = new float[3];
   final float[] up = new float[3];
   private final float[] clip = new float[4];

   void fit(float[] target, float yaw, float pitch, float fov, float aspect, float[] corners, float[] box, float settle) {
      double y = Math.toRadians((double)yaw);
      double p = Math.toRadians((double)pitch);
      float dx = (float)(-Math.sin(y) * Math.cos(p));
      float dy = (float)Math.sin(p);
      float dz = (float)(Math.cos(y) * Math.cos(p));
      float near = 1.0F;
      float far = 400.0F;
      float[] extent = new float[4];

      for(int i = 0; i < 40; ++i) {
         float d = (near + far) / 2.0F;
         this.measure(target, dx, dy, dz, d, fov, aspect, corners, extent);
         boolean fits = extent[1] - extent[0] <= box[1] - box[0] && extent[3] - extent[2] <= box[3] - box[2];
         if (fits) {
            far = d;
         } else {
            near = d;
         }
      }

      this.measure(target, dx, dy, dz, far, fov, aspect, corners, extent);
      float shiftX = (box[0] + box[1]) / 2.0F - (extent[0] + extent[1]) / 2.0F;
      float room = box[3] - box[2] - (extent[3] - extent[2]);
      float shiftY = box[2] + room * settle - extent[2];
      this.eye[0] = target[0] + dx * far;
      this.eye[1] = target[1] + dy * far;
      this.eye[2] = target[2] + dz * far;
      Mat4.set(this.view, Mat4.lookAt(this.eye[0], this.eye[1], this.eye[2], target[0], target[1], target[2], 0.0F, 1.0F, 0.0F));
      float[] proj = Mat4.perspective(fov, aspect, Math.max(0.5F, far - 40.0F), far + 80.0F, shiftX, shiftY);
      Mat4.mul(this.viewProj, proj, this.view);
      this.right[0] = this.view[0];
      this.right[1] = this.view[4];
      this.right[2] = this.view[8];
      this.up[0] = this.view[1];
      this.up[1] = this.view[5];
      this.up[2] = this.view[9];
   }

   private void measure(float[] target, float dx, float dy, float dz, float d, float fov, float aspect, float[] corners, float[] extent) {
      float[] v = Mat4.lookAt(target[0] + dx * d, target[1] + dy * d, target[2] + dz * d, target[0], target[1], target[2], 0.0F, 1.0F, 0.0F);
      float[] vp = new float[16];
      Mat4.mul(vp, Mat4.perspective(fov, aspect, 0.5F, d + 80.0F, 0.0F, 0.0F), v);
      extent[0] = extent[2] = Float.MAX_VALUE;
      extent[1] = extent[3] = -Float.MAX_VALUE;

      for(int i = 0; i + 2 < corners.length; i += 3) {
         Mat4.transform(vp, corners[i], corners[i + 1], corners[i + 2], this.clip);
         if (this.clip[3] <= 0.01F) {
            extent[0] = extent[2] = -1000000.0F;
            extent[1] = extent[3] = 1000000.0F;
            return;
         }

         float x = this.clip[0] / this.clip[3];
         float yy = this.clip[1] / this.clip[3];
         extent[0] = Math.min(extent[0], x);
         extent[1] = Math.max(extent[1], x);
         extent[2] = Math.min(extent[2], yy);
         extent[3] = Math.max(extent[3], yy);
      }

   }

   void project(float x, float y, float z, float w, float h, float[] out) {
      Mat4.transform(this.viewProj, x, y, z, this.clip);
      out[0] = (this.clip[0] / this.clip[3] * 0.5F + 0.5F) * w;
      out[1] = (1.0F - (this.clip[1] / this.clip[3] * 0.5F + 0.5F)) * h;
   }

   static float[] box(float x0, float y0, float z0, float x1, float y1, float z1) {
      return new float[]{x0, y0, z0, x1, y0, z0, x0, y1, z0, x1, y1, z0, x0, y0, z1, x1, y0, z1, x0, y1, z1, x1, y1, z1};
   }
}
