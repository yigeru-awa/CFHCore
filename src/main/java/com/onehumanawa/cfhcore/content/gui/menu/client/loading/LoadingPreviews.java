package com.onehumanawa.cfhcore.content.gui.menu.client.loading;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.ConnectScreenAccessor;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen.Reason;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class LoadingPreviews {
   public static void register() {
      CoreActions.SCREEN_FACTORIES.put("cfhgui:loading_world", (p) -> new LevelLoadingScreen(halfGenerated()));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:loading_terrain", (p) -> new ReceivingLevelScreen(() -> false, Reason.OTHER));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:loading_message", (p) -> new GenericMessageScreen(Component.translatable("menu.savingLevel")));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:loading_progress", (p) -> {
         ProgressScreen s = new ProgressScreen(false);
         s.progressStartNoAbort(Component.translatable("menu.generatingLevel"));
         s.progressStage(Component.translatable("menu.generatingTerrain"));
         s.progressStagePercentage(42);
         return s;
      });
      CoreActions.SCREEN_FACTORIES.put("cfhgui:loading_join", (p) -> {
         Minecraft mc = Minecraft.getInstance();
         String address = System.getProperty("cfhgui.joinPreview", "10.255.255.1:25565");
         ConnectScreen.startConnecting(p, mc, ServerAddress.parseString(address), new ServerData("Hollow Peak", address, Type.OTHER), false, (TransferState)null);
         return mc.screen;
      });
      CoreActions.SCREEN_FACTORIES.put("cfhgui:loading_connect", (p) -> ConnectScreenAccessor.cfhgui$create(p, Component.translatable("connect.failed")));
   }

   private static StoringChunkProgressListener halfGenerated() {
      StoringChunkProgressListener l = StoringChunkProgressListener.createFromGameruleRadius(3);
      l.updateSpawnPos(ChunkPos.ZERO);
      l.start();

      for(int x = -12; x <= 12; ++x) {
         for(int z = -12; z <= 12; ++z) {
            int d = Math.max(Math.abs(x), Math.abs(z));
            ChunkStatus s = d <= 2 ? ChunkStatus.FULL : (d <= 4 ? ((x + z) % 2 == 0 ? ChunkStatus.FULL : ChunkStatus.FEATURES) : (d <= 6 ? ChunkStatus.NOISE : (d <= 9 ? ChunkStatus.BIOMES : ChunkStatus.EMPTY)));
            l.onStatusChange(new ChunkPos(x, z), s);
         }
      }

      return l;
   }

   private LoadingPreviews() {
   }
}
