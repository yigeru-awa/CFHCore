package com.onehumanawa.cfhcore.content.world.concurrent;

import com.onehumanawa.cfhcore.content.world.concurrent.cache.SafeCloseable;

public interface Resource<T> extends SafeCloseable {
   Resource<?> NONE = new Resource<Object>() {
      @Override
      public Object get() {
         return null;
      }

      @Override
      public boolean isOpen() {
         return false;
      }

      @Override
      public void close() {
      }
   };

   T get();

   boolean isOpen();

   static <T> Resource<T> empty() {
      return (Resource<T>)NONE;
   }
}
