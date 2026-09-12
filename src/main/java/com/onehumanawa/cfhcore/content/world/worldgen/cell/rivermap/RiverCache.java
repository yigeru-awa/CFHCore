package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap;

import java.util.concurrent.TimeUnit;
import com.onehumanawa.cfhcore.content.world.concurrent.cache.Cache;
import com.onehumanawa.cfhcore.content.world.concurrent.cache.map.StampedLongMap;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public class RiverCache {
   protected RiverGenerator generator;
   protected Cache<Rivermap> cache = new Cache<>(32, 5L, 1L, TimeUnit.MINUTES, StampedLongMap::new);

   public RiverCache(RiverGenerator generator) {
      this.generator = generator;
   }

   public Rivermap getRivers(int x, int z) {
      return this.cache.computeIfAbsent(PosUtil.pack(x, z), id -> this.generator.generateRivers(PosUtil.unpackLeft(id), PosUtil.unpackRight(id), id));
   }
}
