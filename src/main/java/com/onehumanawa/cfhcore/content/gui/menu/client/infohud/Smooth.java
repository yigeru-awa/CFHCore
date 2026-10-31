package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

final class Smooth {
   float value;
   float target;
   private boolean begun;

   void to(float where) {
      this.target = where;
      if (!this.begun) {
         this.begun = true;
         this.value = where;
      }

   }

   void snap(float where) {
      this.begun = true;
      this.value = this.target = where;
   }

   float step(float share) {
      float left = this.target - this.value;
      if (!(share >= 1.0F) && !(Math.abs(left) < 0.004F)) {
         this.value += left * share;
      } else {
         this.value = this.target;
      }

      return this.value;
   }

   boolean begun() {
      return this.begun;
   }
}
