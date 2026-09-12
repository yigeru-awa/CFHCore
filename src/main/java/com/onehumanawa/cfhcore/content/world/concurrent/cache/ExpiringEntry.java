package com.onehumanawa.cfhcore.content.world.concurrent.cache;

public interface ExpiringEntry {
   long getTimestamp();

   default void close() {
   }
}
