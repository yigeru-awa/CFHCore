package com.onehumanawa.cfhcore.content.world.concurrent.cache;

import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import com.onehumanawa.cfhcore.content.world.concurrent.task.LazyCallable;

public class CacheEntry<T> extends LazyCallable<T> implements ExpiringEntry {
   private volatile long timestamp;
   private Future<T> task;

   public CacheEntry(Future<T> task) {
      this.task = task;
      this.timestamp = System.currentTimeMillis();
   }

   @Override
   public T get() {
      this.timestamp = System.currentTimeMillis();
      return super.get();
   }

   @Override
   public boolean isDone() {
      return this.task.isDone();
   }

   @Override
   public long getTimestamp() {
      return this.timestamp;
   }

   @Override
   public void close() {
      if (this.value instanceof SafeCloseable value) {
         value.close();
      } else {
         if (this.value instanceof AutoCloseable value) {
            try {
               value.close();
            } catch (Exception var3) {
               var3.printStackTrace();
            }
         }
      }
   }

   @Override
   protected T create() {
      if (this.task instanceof ForkJoinTask<T> task) {
         return task.join();
      } else {
         try {
            return this.task.get();
         } catch (Throwable var3) {
            throw new RuntimeException(var3);
         }
      }
   }

   public static <T> CacheEntry<T> supply(Future<T> task) {
      return new CacheEntry<>(task);
   }
}
