package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPath;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPose;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Scene;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class BuildCamera {
   private static final float ASPECT = 2.4F;
   private static final float FOV_MARGIN = 6.0F;
   private static final float EYE_STEP = 10.0F;
   private final Vector3f min;
   private final Vector3f max;
   private final Vector3f rest;
   private final List<Matrix4f> views;
   private final List<Vector3f> eyes;
   private final float far;

   private BuildCamera(Vector3f min, Vector3f max, Vector3f rest, List<Matrix4f> views, List<Vector3f> eyes, float far) {
      this.min = min;
      this.max = max;
      this.rest = rest;
      this.views = views;
      this.eyes = eyes;
      this.far = far;
   }

   public static BuildCamera of(Scene.CameraConfig camera) {
      List<CameraPose> poses = new ArrayList();
      CameraPose rest = camera.rest().copy();
      poses.add(rest);
      IdleMotion idle = camera.idle();
      if (idle != null && idle.kind() != IdleMotion.Kind.NONE) {
         for(int i = 0; i < 48; ++i) {
            CameraPose p = new CameraPose();
            idle.apply(rest, (float)i * 1000.0F, p);
            poses.add(p);
         }
      }

      List<Vector3f> eyes = new ArrayList();
      eyes.add(new Vector3f(rest.position));
      CameraPose at = rest.copy();

      for(CameraPath path : camera.intro()) {
         path.begin(at.copy());

         for(int i = 0; i <= 32; ++i) {
            CameraPose p = new CameraPose();
            path.sample((float)i / 32.0F, p);
            poses.add(p);
            if (i < 32 && ((Vector3f)eyes.get(eyes.size() - 1)).distance(p.position) > 10.0F) {
               eyes.add(new Vector3f(p.position));
            }
         }

         CameraPose end = new CameraPose();
         path.sample(1.0F, end);
         at = end;
      }

      Vector3f min = new Vector3f(Float.MAX_VALUE);
      Vector3f max = new Vector3f(-Float.MAX_VALUE);
      List<Matrix4f> views = new ArrayList();

      for(CameraPose p : poses) {
         min.min(p.position);
         max.max(p.position);
         views.add(viewProjection(p, camera.near(), camera.far()));
      }

      min.sub(0.75F, 0.75F, 0.75F);
      max.add(0.75F, 0.75F, 0.75F);
      return new BuildCamera(min, max, new Vector3f(rest.position), views, eyes, camera.far());
   }

   private static Matrix4f viewProjection(CameraPose p, float near, float far) {
      Vector3f dir = (new Vector3f(p.target)).sub(p.position);
      if (dir.lengthSquared() < 1.0E-8F) {
         dir.set(0.0F, 0.0F, -1.0F);
      }

      dir.normalize();
      Vector3f up = Math.abs(dir.y) > 0.999F ? new Vector3f(0.0F, 0.0F, -1.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
      float fov = Math.min(150.0F, p.fov + 6.0F);
      return (new Matrix4f()).perspective((float)Math.toRadians((double)fov), 2.4F, Math.max(0.01F, near), Math.max(near + 1.0F, far)).lookAt(p.position.x, p.position.y, p.position.z, p.position.x + dir.x, p.position.y + dir.y, p.position.z + dir.z, up.x, up.y, up.z);
   }

   public BuildCamera moved(Vector3f origin) {
      return this.moved((new Matrix4f()).translation(origin));
   }

   public BuildCamera moved(Matrix4f model) {
      Matrix4f inverse = (new Matrix4f(model)).invert();
      List<Matrix4f> v = new ArrayList(this.views.size());

      for(Matrix4f m : this.views) {
         v.add((new Matrix4f(m)).mul(model));
      }

      Vector3f lo = new Vector3f(Float.MAX_VALUE);
      Vector3f hi = new Vector3f(-Float.MAX_VALUE);
      Vector3f c = new Vector3f();

      for(int i = 0; i < 8; ++i) {
         c.set((i & 1) == 0 ? this.min.x : this.max.x, (i & 2) == 0 ? this.min.y : this.max.y, (i & 4) == 0 ? this.min.z : this.max.z);
         inverse.transformPosition(c);
         lo.min(c);
         hi.max(c);
      }

      List<Vector3f> e = new ArrayList(this.eyes.size());

      for(Vector3f p : this.eyes) {
         e.add(inverse.transformPosition(new Vector3f(p)));
      }

      float scale = Math.max(1.0E-4F, model.getScale(new Vector3f()).x);
      return new BuildCamera(lo, hi, inverse.transformPosition(new Vector3f(this.rest)), v, e, this.far / scale);
   }

   BuildMesher.Cull cull() {
      float[] at = new float[this.eyes.size() * 3];

      for(int i = 0; i < this.eyes.size(); ++i) {
         at[i * 3] = ((Vector3f)this.eyes.get(i)).x;
         at[i * 3 + 1] = ((Vector3f)this.eyes.get(i)).y;
         at[i * 3 + 2] = ((Vector3f)this.eyes.get(i)).z;
      }

      return new BuildMesher.Cull(this.min.x, this.min.y, this.min.z, this.max.x, this.max.y, this.max.z, at);
   }

   public Vector3f rest() {
      return this.rest;
   }

   public float far() {
      return this.far;
   }

   boolean sees(float x0, float y0, float z0, float x1, float y1, float z1) {
      FrustumIntersection f = new FrustumIntersection();

      for(Matrix4f m : this.views) {
         f.set(m, false);
         if (f.testAab(x0, y0, z0, x1, y1, z1)) {
            return true;
         }
      }

      return false;
   }

   boolean seenAtRest(float x0, float y0, float z0, float x1, float y1, float z1) {
      return this.views.isEmpty() ? true : (new FrustumIntersection((Matrix4fc)this.views.get(0), false)).testAab(x0, y0, z0, x1, y1, z1);
   }

   String key() {
      int hash = 1;
      float[] values = new float[16];

      for(Matrix4f m : this.views) {
         m.get(values);
         hash = 31 * hash + Arrays.hashCode(values);
      }

      return String.format(Locale.ROOT, "%.1f,%.1f,%.1f;%.1f,%.1f,%.1f;%d;%.0f;%08x", this.min.x, this.min.y, this.min.z, this.max.x, this.max.y, this.max.z, this.views.size(), this.far, hash);
   }
}
