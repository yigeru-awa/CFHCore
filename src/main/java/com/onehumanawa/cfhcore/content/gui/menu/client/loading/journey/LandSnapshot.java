package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

final class LandSnapshot {
   static final String FILE = "cfhgui/land.bin";
   private static final int MAGIC = 1397509700;
   private static final int VERSION = 2;
   private static final int WHITE = -1;
   private static final int THIN = 32;
   private static final int TREE = 48;
   private static final int DEPTH = 48;

   private LandSnapshot() {
   }

   static @Nullable Land take(Minecraft mc) {
      ClientLevel level = mc.level;
      if (level != null && mc.player != null && !level.dimensionType().hasCeiling()) {
         try {
            Land land = new Land(mc.player.getBlockX(), mc.player.getBlockZ());
            land.dayTime = (int)Math.floorMod(level.getDayTime(), 24000L);
            int sea = level.getSeaLevel();
            int table = Math.max(level.getMinBuildHeight(), sea - 39);
            land.sea = cells(sea, table);
            Map<BlockState, Face> faces = new HashMap();
            Map<BlockState, Boolean> thin = new HashMap();
            RandomSource random = RandomSource.create(42L);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            LevelChunk chunk = null;

            for(int z = 0; z < 128; ++z) {
               for(int x = 0; x < 160; ++x) {
                  int wx = land.blockX(x);
                  int wz = land.blockZ(z);
                  if (chunk == null || chunk.getPos().x != wx >> 4 || chunk.getPos().z != wz >> 4) {
                     chunk = level.getChunkSource().getChunk(wx >> 4, wz >> 4, ChunkStatus.FULL, false);
                  }

                  if (chunk != null && !chunk.isEmpty()) {
                     int y = chunk.getHeight(Types.MOTION_BLOCKING, wx & 15, wz & 15);
                     if (y >= level.getMinBuildHeight()) {
                        for(int stop = Math.max(level.getMinBuildHeight(), y - 32); y > stop && (Boolean)thin.computeIfAbsent(chunk.getBlockState(pos.set(wx, y, wz)), (sx) -> thin(sx, level, pos)); --y) {
                        }

                        String trunk = null;
                        String canopy = null;
                        int canopyTint = -1;
                        BlockState top = chunk.getBlockState(pos.set(wx, y, wz));
                        if (top.is(BlockTags.LEAVES)) {
                           Face leaf = (Face)faces.computeIfAbsent(top, (sx) -> face(mc, sx, random));
                           canopy = leaf.top();
                           canopyTint = leaf.topTinted() ? tint(mc, level, top, pos, leaf.topIndex()) : -1;

                           for(int floor = Math.max(level.getMinBuildHeight(), y - 48); y > floor; --y) {
                              BlockState s = chunk.getBlockState(pos.set(wx, y, wz));
                              if (s.is(BlockTags.LOGS)) {
                                 if (trunk == null) {
                                    trunk = ((Face)faces.computeIfAbsent(s, (b) -> face(mc, b, random))).side();
                                 }
                              } else if (!s.is(BlockTags.LEAVES) && s.getFluidState().isEmpty() && s.blocksMotion() && !(Boolean)thin.computeIfAbsent(s, (b) -> thin(b, level, pos)) || !s.getFluidState().isEmpty()) {
                                 break;
                              }
                           }

                           if (trunk == null) {
                              trunk = "oak_log";
                           }
                        }

                        Land.Cell cell = cell(mc, level, chunk, pos.set(wx, y, wz), table, faces, random);
                        if (cell != null && canopy != null) {
                           cell = cell.withTree(trunk, canopy, canopyTint);
                        }

                        land.lay(x, z, cell);
                     }
                  }
               }
            }

            land.close();
            return land.any() ? land : null;
         } catch (Exception e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the land could not be taken down: {}", e.toString());
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean thin(BlockState state, ClientLevel level, BlockPos pos) {
      if (!state.getFluidState().isEmpty()) {
         return false;
      } else {
         VoxelShape shape = state.getCollisionShape(level, pos);
         if (shape.isEmpty()) {
            return true;
         } else {
            AABB box = shape.bounds();
            return box.getXsize() < 0.6 || box.getZsize() < 0.6;
         }
      }
   }

   private static int cells(int y, int table) {
      return Land.sunk(Math.round((float)(y - table) / 2.0F));
   }

   private static int lively(int rgb) {
      int r = rgb >> 16 & 255;
      int g = rgb >> 8 & 255;
      int b = rgb & 255;
      return -16777216 | r + (255 - r) / 3 << 16 | g + (255 - g) / 3 << 8 | b + (255 - b) / 3;
   }

   private static Land.@Nullable Cell cell(Minecraft mc, ClientLevel level, LevelChunk chunk, BlockPos.MutableBlockPos pos, int table, Map<BlockState, Face> faces, RandomSource random) {
      int surface = pos.getY();
      BlockState state = chunk.getBlockState(pos);
      int water = 0;
      int waterTint = -1;
      if (state.getFluidState().is(FluidTags.WATER) && !state.isFaceSturdy(level, pos, Direction.UP)) {
         waterTint = lively(BiomeColors.getAverageWaterColor(level, pos));
         int y = surface;

         while(y > level.getMinBuildHeight() && surface - y < 48) {
            --y;
            state = chunk.getBlockState(pos.setY(y));
            if (!state.getFluidState().is(FluidTags.WATER) && state.blocksMotion()) {
               break;
            }
         }

         water = Math.max(1, cells(surface + 1, table) - cells(y + 1, table));
      }

      if (state.isAir()) {
         return null;
      } else {
         Face face = (Face)faces.computeIfAbsent(state, (s) -> face(mc, s, random));
         int ground = cells(pos.getY() + 1, table);
         int topTint = face.topTinted() ? tint(mc, level, state, pos, face.topIndex()) : -1;
         if (!state.is(BlockTags.DIRT) && !state.is(Blocks.DIRT_PATH) && !state.is(Blocks.FARMLAND) && !state.is(BlockTags.SNOW)) {
            if (!state.is(BlockTags.SAND) && !state.is(Blocks.GRAVEL) && !state.is(Blocks.CLAY)) {
               if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                  return new Land.Cell(ground, face.top(), topTint, face.side(), -1, 3, water, waterTint, false, (String)null, (String)null, -1);
               } else {
                  int sideTint = face.sideTinted() ? tint(mc, level, state, pos, face.sideIndex()) : -1;
                  return new Land.Cell(ground, face.top(), topTint, face.side(), sideTint, 9999, water, waterTint, false, (String)null, (String)null, -1);
               }
            } else {
               String under = state.is(Blocks.RED_SAND) ? "red_sandstone" : (state.is(BlockTags.SAND) ? "sandstone" : "stone");
               return new Land.Cell(ground, face.top(), topTint, under, -1, 3, water, waterTint, false, (String)null, (String)null, -1);
            }
         } else {
            return new Land.Cell(ground, face.top(), topTint, "dirt", -1, 3, water, waterTint, false, (String)null, (String)null, -1);
         }
      }
   }

   private static int tint(Minecraft mc, ClientLevel level, BlockState state, BlockPos pos, int index) {
      int rgb = mc.getBlockColors().getColor(state, level, pos, index);
      return rgb == -1 ? -1 : rgb | -16777216;
   }

   private static Face face(Minecraft mc, BlockState state, RandomSource random) {
      if (!state.getFluidState().isEmpty() && state.getBlock() == Blocks.LAVA) {
         return new Face("lava_still", false, 0, "lava_still", false, 0);
      } else {
         BakedModel model = mc.getBlockRenderer().getBlockModel(state);
         BakedQuad top = quad(model, state, Direction.UP, random);
         BakedQuad side = quad(model, state, Direction.NORTH, random);
         String particle = model.getParticleIcon().contents().name().toString();
         String t = top == null ? particle : top.getSprite().contents().name().toString();
         String s = side == null ? t : side.getSprite().contents().name().toString();
         return new Face(t, top != null && top.isTinted(), top == null ? 0 : top.getTintIndex(), s, side != null && side.isTinted(), side == null ? 0 : side.getTintIndex());
      }
   }

   private static @Nullable BakedQuad quad(BakedModel model, BlockState state, Direction direction, RandomSource random) {
      random.setSeed(42L);
      List<BakedQuad> quads = model.getQuads(state, direction, random);
      if (quads.isEmpty()) {
         random.setSeed(42L);

         for(BakedQuad q : model.getQuads(state, (Direction)null, random)) {
            if (q.getDirection() == direction) {
               return q;
            }
         }

         return null;
      } else {
         return (BakedQuad)quads.get(0);
      }
   }

   static void write(Land land, Path file, String dimension) {
      List<String> names = new ArrayList();
      Map<String, Integer> index = new HashMap();
      Land.Cell[] cells = new Land.Cell[20480];

      for(int z = 0; z < 128; ++z) {
         for(int x = 0; x < 160; ++x) {
            Land.Cell c = land.at(x, z);
            cells[z * 160 + x] = c;
            if (c != null) {
               for(String name : new String[]{c.top(), c.under(), c.trunk(), c.canopy()}) {
                  if (name != null && !index.containsKey(name)) {
                     index.put(name, names.size());
                     names.add(name);
                  }
               }
            }
         }
      }

      Util.ioPool().execute(() -> {
         try {
            Files.createDirectories(file.getParent());
            Path part = file.resolveSibling(String.valueOf(file.getFileName()) + ".part");
            OutputStream raw = Files.newOutputStream(part);

            try {
               DataOutputStream out = new DataOutputStream(new GZIPOutputStream(raw));

               try {
                  out.writeInt(1397509700);
                  out.writeInt(2);
                  out.writeInt(160);
                  out.writeInt(128);
                  out.writeInt(4);
                  out.writeInt(land.centerX);
                  out.writeInt(land.centerZ);
                  out.writeInt(land.dayTime);
                  out.writeInt(land.sea);
                  out.writeUTF(dimension);
                  out.writeInt(names.size());

                  for(String name : names) {
                     out.writeUTF(name);
                  }

                  for(Land.Cell c : cells) {
                     out.writeBoolean(c != null);
                     if (c != null) {
                        out.writeShort(c.ground());
                        out.writeShort((Integer)index.get(c.top()));
                        out.writeInt(c.topTint());
                        out.writeShort((Integer)index.get(c.under()));
                        out.writeInt(c.underTint());
                        out.writeShort(Math.min(c.strata(), 32767));
                        out.writeShort(c.water());
                        out.writeInt(c.waterTint());
                        boolean tree = c.trunk() != null && c.canopy() != null;
                        out.writeBoolean(tree);
                        if (tree) {
                           out.writeShort((Integer)index.get(c.trunk()));
                           out.writeShort((Integer)index.get(c.canopy()));
                           out.writeInt(c.canopyTint());
                        }
                     }
                  }
               } catch (Throwable var16) {
                  try {
                     out.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }

                  throw var16;
               }

               out.close();
            } catch (Throwable var17) {
               if (raw != null) {
                  try {
                     raw.close();
                  } catch (Throwable var14) {
                     var17.addSuppressed(var14);
                  }
               }

               throw var17;
            }

            if (raw != null) {
               raw.close();
            }

            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the land could not be kept in {}: {}", file, e.toString());
         }

      });
   }

   static CompletableFuture<@Nullable Kept> read(Path file) {
      return CompletableFuture.supplyAsync(() -> {
         if (!Files.isRegularFile(file, new LinkOption[0])) {
            return null;
         } else {
            try {
               InputStream raw = Files.newInputStream(file);

               Kept var27;
               label147: {
                  Object var24;
                  label148: {
                     try {
                        DataInputStream in = new DataInputStream(new GZIPInputStream(raw));

                        label135: {
                           label134: {
                              try {
                                 if (in.readInt() != 1397509700 || in.readInt() != 2) {
                                    var24 = null;
                                    break label135;
                                 }

                                 if (in.readInt() != 160 || in.readInt() != 128 || in.readInt() != 4) {
                                    var24 = null;
                                    break label134;
                                 }

                                 Land land = new Land(in.readInt(), in.readInt());
                                 land.dayTime = in.readInt();
                                 land.sea = in.readInt();
                                 String dimension = in.readUTF();
                                 String[] names = new String[in.readInt()];

                                 for(int i = 0; i < names.length; ++i) {
                                    names[i] = in.readUTF();
                                 }

                                 for(int i = 0; i < 20480; ++i) {
                                    if (in.readBoolean()) {
                                       int ground = in.readShort();
                                       String top = names[in.readShort()];
                                       int topTint = in.readInt();
                                       String under = names[in.readShort()];
                                       int underTint = in.readInt();
                                       int strata = in.readShort();
                                       int water = in.readShort();
                                       int waterTint = in.readInt();
                                       boolean tree = in.readBoolean();
                                       String trunk = tree ? names[in.readShort()] : null;
                                       String canopy = tree ? names[in.readShort()] : null;
                                       int canopyTint = tree ? in.readInt() : -1;
                                       land.lay(i % 160, i / 160, new Land.Cell(Math.max(1, ground), top, topTint, under, underTint, strata == 32767 ? 9999 : strata, Math.max(0, water), waterTint, false, trunk, canopy, canopyTint));
                                    }
                                 }

                                 var27 = land.any() ? new Kept(land, dimension) : null;
                              } catch (Throwable var21) {
                                 try {
                                    in.close();
                                 } catch (Throwable var20) {
                                    var21.addSuppressed(var20);
                                 }

                                 throw var21;
                              }

                              in.close();
                              break label147;
                           }

                           in.close();
                           break label148;
                        }

                        in.close();
                     } catch (Throwable var22) {
                        if (raw != null) {
                           try {
                              raw.close();
                           } catch (Throwable var19) {
                              var22.addSuppressed(var19);
                           }
                        }

                        throw var22;
                     }

                     if (raw != null) {
                        raw.close();
                     }

                     return (Kept)var24;
                  }

                  if (raw != null) {
                     raw.close();
                  }

                  return (Kept)var24;
               }

               if (raw != null) {
                  raw.close();
               }

               return var27;
            } catch (Exception e) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the land kept in {} could not be read: {}", file, e.toString());
               return null;
            }
         }
      }, Util.ioPool()).exceptionally((t) -> null);
   }

   private static record Face(String top, boolean topTinted, int topIndex, String side, boolean sideTinted, int sideIndex) {
   }

   static record Kept(Land land, String dimension) {
   }
}
