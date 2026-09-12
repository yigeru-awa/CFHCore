package com.onehumanawa.cfhcore.content.world.concurrent;

public interface Disposable {
   void dispose();

   public interface Listener<T> {
      void onDispose(T var1);
   }
}
