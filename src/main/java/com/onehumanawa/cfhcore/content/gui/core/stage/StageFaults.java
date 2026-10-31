package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class StageFaults {
   private static final boolean ASKED = Boolean.getBoolean("cfhgui.stage.faults");
   private static Boolean on;
   private static boolean installed;
   private static final Set<String> FIRED = ConcurrentHashMap.newKeySet();

   private StageFaults() {
   }

   public static boolean on() {
      Boolean o = on;
      if (o == null) {
         o = ASKED && CFHGuiPlatform.get().isDevelopmentEnvironment();
         if (ASKED && !o) {
            CFHGui.LOGGER.warn("[CFHGui] -Dcfhgui.stage.faults is only honoured in a development environment; ignored");
         }

         on = o;
      }

      return o;
   }

   public static void install() {
      if (!installed && ASKED && on()) {
         installed = true;
         BlockColors colors = Minecraft.getInstance().getBlockColors();
         colors.register((state, level, pos, tint) -> {
            noPlayer("the colour of grass");
            return level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor();
         }, new Block[]{Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN});
         colors.register((state, level, pos, tint) -> {
            noPlayer("the colour of leaves");
            return level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor();
         }, new Block[]{Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.VINE});
         colors.register((state, level, pos, tint) -> {
            noPlayer("the colour of birch leaves");
            return FoliageColor.getBirchColor();
         }, new Block[]{Blocks.BIRCH_LEAVES});
         colors.register((state, level, pos, tint) -> {
            noPlayer("the colour of spruce leaves");
            return FoliageColor.getEvergreenColor();
         }, new Block[]{Blocks.SPRUCE_LEAVES});
         CFHGui.LOGGER.warn("[CFHGui] stage faults are on (-Dcfhgui.stage.faults): a fluid's layer and the colours of grass and leaves throw without a player, and the light double chests and beds ask of a scene's world throws, as other mods' hooks make them throw in a pack");
      }
   }

   public static void noPlayer(String what) {
      if (Minecraft.getInstance().player == null) {
         fired(what);
         throw new NullPointerException("CFHGui's fault for " + what + " (-Dcfhgui.stage.faults): Cannot invoke \"LocalPlayer.level()\" because \"Minecraft.getInstance().player\" is null");
      }
   }

   public static void noWorldOfItsOwn(String what) {
      fired(what);
      throw new NullPointerException("CFHGui's fault for " + what + " (-Dcfhgui.stage.faults): Cannot invoke \"ColoredLightEngine.sampleLightColorInt(BlockPos)\" because the return value of \"LevelAttachments.getColoredLightEngine()\" is null");
   }

   private static void fired(String what) {
      StackTraceElement[] stack = (new Throwable()).getStackTrace();
      String caller = "?";

      for(int i = 1; i < stack.length; ++i) {
         String c = stack[i].getClassName();
         if (!c.startsWith(StageFaults.class.getName()) && !c.startsWith("net.minecraft.") && !c.startsWith("com.mojang.") && !c.startsWith("it.unimi.") && !c.startsWith("java.")) {
            caller = c + "." + stack[i].getMethodName();
            break;
         }
      }

      if (FIRED.add(what + "|" + caller)) {
         CFHGui.LOGGER.info("[CFHGui] stage fault: {} thrown, asked by {}", what, caller);
      }

   }
}
