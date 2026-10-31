package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class StageNode {
   protected final Vector3f position = new Vector3f();
   protected float yaw;
   protected float pitch;
   protected float roll;
   protected final Vector3f scale = new Vector3f(1.0F, 1.0F, 1.0F);
   protected float alpha = 1.0F;
   protected float muted;
   protected int tint = -1;
   protected boolean visible = true;
   private boolean pickable;
   protected final Vector3f boundsMin = new Vector3f(-0.5F, 0.0F, -0.5F);
   protected final Vector3f boundsMax = new Vector3f(0.5F, 1.0F, 0.5F);
   protected float hoverLift = 0.05F;
   protected float hoverScale = 1.04F;
   protected final Anim hoverAnim;
   protected final Anim pressAnim;
   protected final Anim focusAnim;
   private @Nullable Runnable onClick;
   private @Nullable Consumer<Boolean> onHoverChanged;
   private @Nullable Component tooltip;
   private Component name;
   private boolean hovered;
   private boolean pressed;
   private boolean focused;
   private @Nullable StageNode parent;
   private long preparedFrame;
   public final Matrix4f model;
   public final Matrix4f modelInverse;
   private final Quaternionf rotation;
   private final Vector3f pickOrigin;
   private final Vector3f pickDir;
   private float spinDegPerSec;

   public StageNode() {
      this.hoverAnim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.pressAnim = new Anim(0.0F, 90, Ease.OUT_CUBIC);
      this.focusAnim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.name = Component.empty();
      this.preparedFrame = -1L;
      this.model = new Matrix4f();
      this.modelInverse = new Matrix4f();
      this.rotation = new Quaternionf();
      this.pickOrigin = new Vector3f();
      this.pickDir = new Vector3f();
   }

   public StageNode at(float x, float y, float z) {
      this.position.set(x, y, z);
      return this;
   }

   public StageNode at(Vector3f p) {
      this.position.set(p);
      return this;
   }

   public StageNode rotate(float yaw, float pitch, float roll) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
      return this;
   }

   public StageNode yaw(float degrees) {
      this.yaw = degrees;
      return this;
   }

   public StageNode scale(float s) {
      this.scale.set(s, s, s);
      return this;
   }

   public StageNode scale(float x, float y, float z) {
      this.scale.set(x, y, z);
      return this;
   }

   public StageNode alpha(float a) {
      this.alpha = Math.max(0.0F, Math.min(1.0F, a));
      return this;
   }

   public StageNode visible(boolean v) {
      this.visible = v;
      return this;
   }

   public StageNode muted(float amount) {
      this.muted = Math.max(0.0F, Math.min(1.0F, amount));
      return this;
   }

   public StageNode tint(int argb) {
      this.tint = argb | -16777216;
      return this;
   }

   public StageNode pickable(boolean p) {
      this.pickable = p;
      return this;
   }

   public StageNode onClick(@Nullable Runnable r) {
      this.onClick = r;
      if (r != null) {
         this.pickable = true;
      }

      return this;
   }

   public StageNode onHover(@Nullable Consumer<Boolean> c) {
      this.onHoverChanged = c;
      return this;
   }

   public StageNode tooltip(@Nullable Component c) {
      this.tooltip = c;
      return this;
   }

   public StageNode named(Component c) {
      this.name = (Component)(c == null ? Component.empty() : c);
      return this;
   }

   public StageNode named(String s) {
      return this.named((Component)Component.literal(s));
   }

   public StageNode bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      this.boundsMin.set(minX, minY, minZ);
      this.boundsMax.set(maxX, maxY, maxZ);
      return this;
   }

   public StageNode attachTo(@Nullable StageNode parent) {
      this.parent = parent == this ? null : parent;
      return this;
   }

   public @Nullable StageNode parent() {
      return this.parent;
   }

   public StageNode hoverFeel(float lift, float scale) {
      this.hoverLift = lift;
      this.hoverScale = scale;
      return this;
   }

   public Vector3f position() {
      return this.position;
   }

   public float yaw() {
      return this.yaw;
   }

   public float pitch() {
      return this.pitch;
   }

   public float roll() {
      return this.roll;
   }

   public Vector3f scale() {
      return this.scale;
   }

   public float alpha() {
      return this.parent == null ? this.alpha : this.alpha * this.parent.alpha();
   }

   public boolean visible() {
      return this.visible && (this.parent == null || this.parent.visible());
   }

   public float muted() {
      return this.parent == null ? this.muted : Math.max(this.muted, this.parent.muted());
   }

   public int tint() {
      if (this.parent == null) {
         return this.tint;
      } else {
         int p = this.parent.tint();
         if (p == -1) {
            return this.tint;
         } else {
            return this.tint == -1 ? p : -16777216 | (this.tint >> 16 & 255) * (p >> 16 & 255) / 255 << 16 | (this.tint >> 8 & 255) * (p >> 8 & 255) / 255 << 8 | (this.tint & 255) * (p & 255) / 255;
         }
      }
   }

   public boolean pickable() {
      return this.pickable;
   }

   public boolean hovered() {
      return this.hovered;
   }

   public boolean pressed() {
      return this.pressed;
   }

   public boolean focused() {
      return this.focused;
   }

   public @Nullable Component tooltip() {
      return this.tooltip;
   }

   public Component name() {
      return this.name;
   }

   public Vector3f boundsMin() {
      return this.boundsMin;
   }

   public Vector3f boundsMax() {
      return this.boundsMax;
   }

   public float hover() {
      return this.hoverAnim.get();
   }

   public float press() {
      return this.pressAnim.get();
   }

   public float focus() {
      return this.focusAnim.get();
   }

   public void setHovered(boolean h) {
      if (this.hovered != h) {
         this.hovered = h;
         this.hoverAnim.set(h);
         if (this.onHoverChanged != null) {
            this.onHoverChanged.accept(h);
         }

      }
   }

   public void setPressed(boolean p) {
      this.pressed = p;
      this.pressAnim.set(p);
   }

   public void setFocused(boolean f) {
      this.focused = f;
      this.focusAnim.set(f);
   }

   public void activate() {
      this.pressAnim.snap(1.0F);
      this.pressAnim.set(0.0F);
      if (this.onClick != null) {
         this.onClick.run();
      }

   }

   public StageNode spin(float degPerSec) {
      this.spinDegPerSec = degPerSec;
      return this;
   }

   public void prepare(StageRenderContext ctx) {
      if (this.preparedFrame != ctx.frame) {
         this.preparedFrame = ctx.frame;
         if (this.spinDegPerSec != 0.0F && Theme.current().motion() > 0.0F) {
            this.yaw = (this.yaw + this.spinDegPerSec * ctx.deltaMs / 1000.0F) % 360.0F;
         }

         float h = this.hoverAnim.get();
         float p = this.pressAnim.get();
         float s = 1.0F + (this.hoverScale - 1.0F) * h - 0.04F * p;
         this.rotation.rotationYXZ((float)Math.toRadians((double)this.yaw), (float)Math.toRadians((double)this.pitch), (float)Math.toRadians((double)this.roll));
         this.model.translationRotateScale(this.position.x, this.position.y + this.hoverLift * h, this.position.z, this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w, this.scale.x * s, this.scale.y * s, this.scale.z * s);
         if (this.parent != null) {
            this.parent.prepare(ctx);
            this.model.mulLocal(this.parent.model);
         }

         this.model.invert(this.modelInverse);
      }
   }

   public void update(StageRenderContext ctx) {
   }

   public void tick(StageRenderContext ctx) {
   }

   public void render(StageRenderContext ctx) {
      if (this.visible() && !(this.alpha() <= 0.004F)) {
         ctx.pose.pushPose();
         ctx.pose.mulPose(this.model);
         StageSoft.muted(this.muted());

         try {
            this.draw(ctx);
         } finally {
            StageSoft.muted(0.0F);
            ctx.pose.popPose();
         }

      }
   }

   protected abstract void draw(StageRenderContext var1);

   public boolean hasTranslucentPass() {
      return false;
   }

   public void renderTranslucent(StageRenderContext ctx) {
      if (this.visible() && !(this.alpha() <= 0.004F) && this.hasTranslucentPass()) {
         ctx.pose.pushPose();
         ctx.pose.mulPose(this.model);
         StageSoft.muted(this.muted());

         try {
            this.drawTranslucent(ctx);
         } finally {
            StageSoft.muted(0.0F);
            ctx.pose.popPose();
         }

      }
   }

   protected void drawTranslucent(StageRenderContext ctx) {
   }

   public void dispose() {
   }

   public double pick(Vector3f origin, Vector3f dir) {
      this.pickOrigin.set(origin);
      this.modelInverse.transformPosition(this.pickOrigin);
      this.pickDir.set(dir);
      this.modelInverse.transformDirection(this.pickDir);
      double tmin = Double.NEGATIVE_INFINITY;
      double tmax = Double.POSITIVE_INFINITY;

      for(int axis = 0; axis < 3; ++axis) {
         float o = axis == 0 ? this.pickOrigin.x : (axis == 1 ? this.pickOrigin.y : this.pickOrigin.z);
         float d = axis == 0 ? this.pickDir.x : (axis == 1 ? this.pickDir.y : this.pickDir.z);
         float lo = axis == 0 ? this.boundsMin.x : (axis == 1 ? this.boundsMin.y : this.boundsMin.z);
         float hi = axis == 0 ? this.boundsMax.x : (axis == 1 ? this.boundsMax.y : this.boundsMax.z);
         if (Math.abs(d) < 1.0E-7F) {
            if (o < lo || o > hi) {
               return Double.POSITIVE_INFINITY;
            }
         } else {
            double t1 = (double)((lo - o) / d);
            double t2 = (double)((hi - o) / d);
            if (t1 > t2) {
               double t = t1;
               t1 = t2;
               t2 = t;
            }

            if (t1 > tmin) {
               tmin = t1;
            }

            if (t2 < tmax) {
               tmax = t2;
            }

            if (tmin > tmax) {
               return Double.POSITIVE_INFINITY;
            }
         }
      }

      if (tmax < (double)0.0F) {
         return Double.POSITIVE_INFINITY;
      } else {
         return Math.max((double)0.0F, tmin);
      }
   }
}
