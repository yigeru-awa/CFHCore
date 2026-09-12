package com.onehumanawa.cfhcore.content.world.concurrent.task;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

public abstract class LazyCallable<T> implements Callable<T>, Future<T>, Supplier<T> {
   private StampedLock lock = new StampedLock();
   protected volatile T value = (T)null;

   @Override
   public T call() {
      long optRead = this.lock.tryOptimisticRead();
      T result = this.value;
      if (this.lock.validate(optRead) && result != null) {
         return result;
      } else {
         long read = this.lock.readLock();

         try {
            result = this.value;
            if (result != null) {
               return result;
            }
         } finally {
            this.lock.unlockRead(read);
         }

         long write = this.lock.writeLock();

         Object var8;
         try {
            result = this.value;
            if (result == null) {
               result = this.create();
               Objects.requireNonNull(result);
               this.value = result;
            }

            var8 = result;
         } finally {
            this.lock.unlockWrite(write);
         }

         return (T)var8;
      }
   }

   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
   }

   @Override
   public boolean isCancelled() {
      return false;
   }

   @Override
   public boolean isDone() {
      long optRead = this.lock.tryOptimisticRead();
      boolean done = this.value != null;
      if (this.lock.validate(optRead)) {
         return done;
      } else {
         long read = this.lock.readLock();

         boolean var6;
         try {
            var6 = this.value != null;
         } finally {
            this.lock.unlockRead(read);
         }

         return var6;
      }
   }

   @Override
   public T get() {
      return this.call();
   }

   @Override
   public T get(long timeout, TimeUnit unit) {
      return this.call();
   }

   protected abstract T create();

   public static LazyCallable<Void> adapt(Runnable runnable) {
      return new LazyCallable.RunnableAdapter(runnable);
   }

   public static <T> LazyCallable<T> adapt(Callable<T> callable) {
      return (LazyCallable<T>)(callable instanceof LazyCallable ? (LazyCallable)callable : new LazyCallable.CallableAdapter<>(callable));
   }

   public static <T> LazyCallable<T> adaptComplete(Callable<T> callable) {
      return new LazyCallable.CompleteAdapter<>(callable);
   }

   public static class CallableAdapter<T> extends LazyCallable<T> {
      private Callable<T> callable;

      public CallableAdapter(Callable<T> callable) {
         this.callable = callable;
      }

      @Override
      protected T create() {
         try {
            return this.callable.call();
         } catch (Throwable var2) {
            var2.printStackTrace();
            return null;
         }
      }
   }

   public static class CompleteAdapter<T> extends LazyCallable<T> {
      private Callable<T> callable;

      public CompleteAdapter(Callable<T> callable) {
         this.callable = callable;
      }

      @Override
      protected T create() {
         try {
            return this.callable.call();
         } catch (Exception var2) {
            return null;
         }
      }

      @Override
      public boolean isDone() {
         return true;
      }
   }

   public static class FutureAdapter<T> extends LazyCallable<T> {
      private Future<T> future;

      FutureAdapter(Future<T> future) {
         this.future = future;
      }

      @Override
      public boolean isDone() {
         return this.future.isDone();
      }

      @Override
      protected T create() {
         try {
            return this.future.get();
         } catch (Throwable var2) {
            var2.printStackTrace();
            return null;
         }
      }
   }

   public static class RunnableAdapter extends LazyCallable<Void> {
      private Runnable runnable;

      RunnableAdapter(Runnable runnable) {
         this.runnable = runnable;
      }

      protected Void create() {
         this.runnable.run();
         return null;
      }
   }
}
