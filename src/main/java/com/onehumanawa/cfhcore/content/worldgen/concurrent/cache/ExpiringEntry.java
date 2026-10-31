package com.onehumanawa.cfhcore.content.worldgen.concurrent.cache;

public interface ExpiringEntry {
    long getTimestamp();
    
    default void close() {
    }
}
