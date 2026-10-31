package com.onehumanawa.cfhcore.content.gui.core.map;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record MapPoint(String id, String group, Component label, String dimension, double x, double y, double z, int rgb, @Nullable Icon icon, @Nullable UUID owner) {
   public static final String HOMES = "homes";
   public static final String WARPS = "warps";
   public static final String SPAWN = "spawn";
   public static final String DEATHS = "deaths";

   public int blockX() {
      return (int)Math.floor(this.x);
   }

   public int blockY() {
      return (int)Math.floor(this.y);
   }

   public int blockZ() {
      return (int)Math.floor(this.z);
   }
}
