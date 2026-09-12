package com.onehumanawa.cfhcore.content.world.concurrent.cache;

public interface SafeCloseable extends AutoCloseable {
   @Override
   void close();
}
