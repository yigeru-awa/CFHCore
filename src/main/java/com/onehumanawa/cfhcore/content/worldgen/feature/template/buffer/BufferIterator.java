package com.onehumanawa.cfhcore.content.worldgen.feature.template.buffer;

public interface BufferIterator {
    boolean isEmpty();

    boolean next();

    int nextIndex();
}