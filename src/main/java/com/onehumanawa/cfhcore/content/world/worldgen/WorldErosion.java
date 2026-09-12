package com.onehumanawa.cfhcore.content.world.worldgen;

import java.util.concurrent.locks.StampedLock;
import java.util.function.IntFunction;

public class WorldErosion<T> {
   private volatile T value = (T)null;
   private IntFunction<T> factory;
   private WorldErosion.Validator<T> validator;
   private StampedLock lock = new StampedLock();

   public WorldErosion(IntFunction<T> factory, WorldErosion.Validator<T> validator) {
      this.factory = factory;
      this.validator = validator;
   }

   public T get(int ctx) {
      T value = this.readValue();
      return this.validate(value, ctx) ? value : this.writeValue(ctx);
   }

   private T readValue() {
      long optRead = this.lock.tryOptimisticRead();
      T value = this.value;
      if (!this.lock.validate(optRead)) {
         long stamp = this.lock.readLock();

         Object var6;
         try {
            var6 = this.value;
         } finally {
            this.lock.unlockRead(stamp);
         }

         return (T)var6;
      } else {
         return value;
      }
   }

   private T writeValue(int ctx) {
      long stamp = this.lock.writeLock();

      Object var4;
      try {
         if (!this.validate(this.value, ctx)) {
            return this.value = this.factory.apply(ctx);
         }

         var4 = this.value;
      } finally {
         this.lock.unlockWrite(stamp);
      }

      return (T)var4;
   }

   private boolean validate(T value, int ctx) {
      return value != null && this.validator.validate(value, ctx);
   }

   public interface Validator<T> {
      boolean validate(T var1, int var2);
   }
}
