package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;

public final class Anim {
   private float from;
   private float to;
   private float value;
   private long startMs;
   private int durationMs;
   private Ease ease;

   public Anim(float initial) {
      this(initial, 150, Ease.OUT_CUBIC);
   }

   public Anim(float initial, int durationMs, Ease ease) {
      this.from = this.to = this.value = initial;
      this.durationMs = durationMs;
      this.ease = ease;
      this.startMs = 0L;
   }

   public Anim duration(int ms) {
      this.durationMs = ms;
      return this;
   }

   public Anim ease(Ease e) {
      this.ease = e;
      return this;
   }

   public void set(float target) {
      if (target != this.to) {
         this.from = this.get();
         this.to = target;
         this.startMs = Clock.nowMs();
      }
   }

   public void set(float target, int ms) {
      if (target != this.to) {
         this.from = this.get();
         this.to = target;
         this.durationMs = ms;
         this.startMs = Clock.nowMs();
      }
   }

   public void snap(float v) {
      this.from = this.to = this.value = v;
      this.startMs = 0L;
   }

   public float target() {
      return this.to;
   }

   public boolean isAnimating() {
      return this.get() != this.to;
   }

   public float get() {
      if (this.startMs != 0L && this.from != this.to) {
         float motion = Theme.current().motion();
         if (motion <= 0.0F) {
            return this.value = this.to;
         } else {
            float dur = Math.max(1.0F, (float)this.durationMs * motion);
            float t = (float)(Clock.nowMs() - this.startMs) / dur;
            if (t >= 1.0F) {
               this.startMs = 0L;
               return this.value = this.to;
            } else {
               return this.value = this.from + (this.to - this.from) * this.ease.apply(t);
            }
         }
      } else {
         return this.value = this.to;
      }
   }

   public void set(boolean on) {
      this.set(on ? 1.0F : 0.0F);
   }
}
