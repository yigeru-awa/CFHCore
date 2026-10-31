package com.onehumanawa.cfhcore.content.gui.core.event;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CFHGuiEvents {
   public static final Event<Runnable> CLIENT_TICK_END = new Event<Runnable>("CLIENT_TICK_END");
   public static final Event<Consumer<Screen>> SCREEN_INIT_POST = new Event<Consumer<Screen>>("SCREEN_INIT_POST");
   public static final Event<Consumer<Screen>> SCREEN_OPENED = new Event<Consumer<Screen>>("SCREEN_OPENED");
   public static final Event<ScreenRender> SCREEN_RENDER_POST = new Event<ScreenRender>("SCREEN_RENDER_POST");
   public static final Event<HudRender> HUD_RENDER = new Event<HudRender>("HUD_RENDER");
   public static final Event<Runnable> CLIENT_JOINED_SERVER = new Event<Runnable>("CLIENT_JOINED_SERVER");
   public static final Event<Runnable> CLIENT_LEFT_SERVER = new Event<Runnable>("CLIENT_LEFT_SERVER");
   public static final Event<KeyPress> KEY_PRESSED = new Event<KeyPress>("KEY_PRESSED");
   public static final Event<Consumer<MinecraftServer>> SERVER_STARTED = new Event<Consumer<MinecraftServer>>("SERVER_STARTED");
   public static final Event<Consumer<MinecraftServer>> SERVER_STOPPING = new Event<Consumer<MinecraftServer>>("SERVER_STOPPING");
   public static final Event<Consumer<MinecraftServer>> SERVER_TICK_END = new Event<Consumer<MinecraftServer>>("SERVER_TICK_END");
   public static final Event<Consumer<ServerPlayer>> PLAYER_JOINED = new Event<Consumer<ServerPlayer>>("PLAYER_JOINED");
   public static final Event<Consumer<ServerPlayer>> PLAYER_LEFT = new Event<Consumer<ServerPlayer>>("PLAYER_LEFT");

   private CFHGuiEvents() {
   }

   @FunctionalInterface
   public interface HudRender {
      void render(GuiGraphics var1, float var2);
   }

   @FunctionalInterface
   public interface KeyPress {
      boolean onKey(int var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface ScreenRender {
      void render(Screen var1, GuiGraphics var2, int var3, int var4, float var5);
   }
}
