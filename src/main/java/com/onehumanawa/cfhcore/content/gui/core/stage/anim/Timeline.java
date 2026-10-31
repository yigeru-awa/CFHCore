package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class Timeline {
   private final List<CameraPath> segments = new ArrayList();
   private int index;
   private float elapsedMs;
   private boolean finished = true;
   private boolean started;
   private @Nullable Runnable onFinished;

   public Timeline add(CameraPath segment) {
      this.segments.add(segment);
      return this;
   }

   public Timeline clear() {
      this.segments.clear();
      this.finished = true;
      this.started = false;
      return this;
   }

   public Timeline onFinished(@Nullable Runnable r) {
      this.onFinished = r;
      return this;
   }

   public boolean isEmpty() {
      return this.segments.isEmpty();
   }

   public boolean finished() {
      return this.finished;
   }

   public boolean playing() {
      return !this.finished;
   }

   public float progress() {
      if (!this.finished && !this.segments.isEmpty()) {
         float dur = ((CameraPath)this.segments.get(this.index)).durationMs();
         float inSegment = dur <= 0.0F ? 1.0F : Math.min(1.0F, this.elapsedMs / dur);
         return ((float)this.index + inSegment) / (float)this.segments.size();
      } else {
         return 1.0F;
      }
   }

   public void play() {
      this.index = 0;
      this.elapsedMs = 0.0F;
      this.started = false;
      this.finished = this.segments.isEmpty();
   }

   public void skip(CameraPose out) {
      if (this.segments.isEmpty()) {
         this.finished = true;
      } else {
         CameraPath last = (CameraPath)this.segments.get(this.segments.size() - 1);
         if (!this.started) {
            last.begin(out);
            this.started = true;
         }

         last.sample(1.0F, out);
         this.finish();
      }
   }

   public boolean advance(float deltaMs, CameraPose out) {
      if (this.finished) {
         return false;
      } else {
         float motion = Theme.current().motion();
         if (motion <= 0.0F) {
            this.skip(out);
            return true;
         } else {
            if (!this.started) {
               ((CameraPath)this.segments.get(0)).begin(out);
               this.started = true;
            }

            this.elapsedMs += deltaMs / motion;

            while(true) {
               CameraPath seg = (CameraPath)this.segments.get(this.index);
               float dur = seg.durationMs();
               if (!(this.elapsedMs >= dur) || this.index >= this.segments.size() - 1) {
                  float t = dur <= 0.0F ? 1.0F : Math.min(1.0F, this.elapsedMs / dur);
                  seg.sample(seg.ease().apply(t), out);
                  if (t >= 1.0F && this.index == this.segments.size() - 1) {
                     this.finish();
                  }

                  return true;
               }

               seg.sample(1.0F, out);
               this.elapsedMs -= dur;
               ++this.index;
               ((CameraPath)this.segments.get(this.index)).begin(out);
            }
         }
      }
   }

   private void finish() {
      this.finished = true;
      if (this.onFinished != null) {
         this.onFinished.run();
      }

   }
}
