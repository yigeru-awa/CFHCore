package com.onehumanawa.cfhcore.content.world.worldgen.densityfunction.tile;

public interface TileFactory {
   Tile provide(int var1, int var2);

   void queue(int var1, int var2);

   void drop(int var1, int var2);

   int chunkToTile(int var1);

   default Tile provideAtChunk(int chunkX, int chunkZ) {
      return this.provide(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
   }

   default void queueAtChunk(int chunkX, int chunkZ) {
      this.queue(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
   }

   default void dropAtChunk(int chunkX, int chunkZ) {
      this.drop(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
   }
}
