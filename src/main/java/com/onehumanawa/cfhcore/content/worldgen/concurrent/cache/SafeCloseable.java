package com.onehumanawa.cfhcore.content.worldgen.concurrent.cache;

public interface SafeCloseable extends AutoCloseable {
    void close();
}
