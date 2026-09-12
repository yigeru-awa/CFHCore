package com.onehumanawa.cfhcore.content.world.concurrent.cache.map;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Predicate;

public interface LongMap<T> {
   int size();

   void clear();

   void remove(long var1);

   void remove(long var1, Consumer<T> var3);

   int removeIf(Predicate<T> var1);

   void put(long var1, T var3);

   T get(long var1);

   T computeIfAbsent(long var1, LongFunction<T> var3);

   default <V> V map(long key, LongFunction<T> factory, Function<T, V> mapper) {
      return mapper.apply(this.computeIfAbsent(key, factory));
   }
}
