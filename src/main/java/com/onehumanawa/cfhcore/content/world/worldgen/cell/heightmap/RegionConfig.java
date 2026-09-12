package com.onehumanawa.cfhcore.content.world.worldgen.cell.heightmap;

import com.onehumanawa.cfhcore.content.world.worldgen.noise.module.Noise;

public record RegionConfig(int seed, int scale, Noise warpX, Noise warpZ, float warpStrength) {
}
