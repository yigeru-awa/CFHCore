package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap;

import com.onehumanawa.cfhcore.content.world.concurrent.cache.ExpiringEntry;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.Cell;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap.Heightmap;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.gen.GenWarp;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.Network;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.domain.Domain;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public class Rivermap implements ExpiringEntry {
   private int x;
   private int z;
   private Domain lakeWarp;
   private Domain riverWarp;
   private Network[] networks;
   private long timestamp = System.currentTimeMillis();

   public Rivermap(int x, int z, Network[] networks, GenWarp warp) {
      this.x = x;
      this.z = z;
      this.networks = networks;
      this.lakeWarp = warp.lake();
      this.riverWarp = warp.river();
   }

   public void apply(Cell cell, float x, float z) {
      long rpos = this.riverWarp.getPos(x, z, 0);
      float rx = PosUtil.unpackLeftf(rpos);
      float rz = PosUtil.unpackRightf(rpos);
      long loff = this.lakeWarp.getOffset(rx, rz, 0);
      float lx = PosUtil.unpackLeftf(loff);
      float lz = PosUtil.unpackRightf(loff);

      for (Network network : this.networks) {
         if (network.contains(rx, rz)) {
            network.carve(cell, rx, rz, lx, lz);
         }
      }
   }

   @Override
   public long getTimestamp() {
      return this.timestamp;
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   public static Rivermap get(Cell cell, Rivermap instance, Heightmap heightmap) {
      return get(cell.continentX, cell.continentZ, instance, heightmap);
   }

   public static Rivermap get(int x, int z, Rivermap instance, Heightmap heightmap) {
      return instance != null && x == instance.getX() && z == instance.getZ() ? instance : heightmap.continent().getRivermap(x, z);
   }
}
