package com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.SimpleContinent;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.gen.GenWarp;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.BaseRiverGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.Network;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.CFHWorldGenRiverCarver;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.River;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.RiverCarverSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.RiverWarp;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river.UpliftRiverCarver;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.NoiseUtil;

public class SimpleRiverGenerator extends BaseRiverGenerator<SimpleContinent> {
   public SimpleRiverGenerator(SimpleContinent continent, GeneratorContext context) {
      super(continent, context);
   }

   @Override
   public List<Network.Builder> generateRoots(int x, int z, Random random, GenWarp warp) {
      List<Network.Builder> roots = new ArrayList<>(this.count);

      for (int i = 0; i < this.count; i++) {
         float angle = random.nextFloat() * (float) (Math.PI * 2);
         float dx = NoiseUtil.sin(angle);
         float dz = NoiseUtil.cos(angle);
         float startMod = 0.05F + random.nextFloat() * 0.45F;
         float length = this.continent.getDistanceToOcean(x, z, dx, dz);
         float startDist = Math.max(400.0F, startMod * length);
         float x2 = (float)x + dx * startDist;
         float z2 = (float)z + dz * startDist;
         float x3 = (float)x + dx * length;
         float z3 = (float)z + dz * length;
         float valleyWidth = 275.0F * River.MAIN_VALLEY.next(random);
         River river = new River((float)((int)x2), (float)((int)z2), (float)((int)x3), (float)((int)z3));
         RiverCarverSettings settings = new RiverCarverSettings(random);
         settings.fadeIn = this.main.fade;
         settings.valleySize = valleyWidth;
         RiverWarp riverWarp = RiverWarp.create(0.1F, 0.85F, random);
         CFHWorldGenRiverCarver carver = new UpliftRiverCarver(
            river, riverWarp, this.main, settings, this.levels, this.lake, this.continent instanceof UpliftContinentGenerator
         );
         Network.Builder branch = Network.builder(carver);
         roots.add(branch);
      }

      return roots;
   }
}
