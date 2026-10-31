package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.EditorOverlay;
import com.onehumanawa.cfhcore.content.gui.core.layout.editor.LayoutEditor;
import com.onehumanawa.cfhcore.content.gui.core.mixin.MouseHandlerAccessor;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class DevHarness {
   private static List<Shot> queue;
   private static List<Shot> worldQueue;
   private static int frames;
   private static int waitFrames = 45;
   private static long openedMs;
   private static boolean started;
   private static boolean overridesApplied;
   private static boolean pauseOnLostFocus = true;
   private static Phase phase;
   private static File dir;
   private static boolean wantWorld;
   private static boolean wholeFrame;
   private static final String RELOAD = "reload";
   private static boolean afterOverlay;
   private static @Nullable Shot hudShot;
   private static long shotAt;
   private static int shots;
   private static int guiScaleBefore;

   public static boolean replayIntros() {
      return Boolean.getBoolean("cfhgui.autoIntro");
   }

   public static boolean sampleData() {
      return Boolean.getBoolean("cfhgui.sampleData");
   }

   public static @Nullable String autoLayout() {
      String layout = System.getProperty("cfhgui.autoLayout");
      return layout != null && !layout.isBlank() ? layout.trim() : null;
   }

   public static @Nullable String autoSkin() {
      String skin = System.getProperty("cfhgui.autoSkin");
      return skin != null && !skin.isBlank() ? skin.trim() : null;
   }

   public static boolean active() {
      return queue != null && phase != DevHarness.Phase.DONE;
   }

   static void init() {
      String screens = System.getProperty("cfhgui.autoScreens");
      wantWorld = Boolean.getBoolean("cfhgui.autoWorld");
      if (screens != null && !screens.isBlank() || wantWorld) {
         queue = new ArrayList();
         if (screens != null && !screens.isBlank()) {
            for(String s : screens.split(",")) {
               queue.add(DevHarness.Shot.parse(s));
            }
         }

         worldQueue = new ArrayList();

         for(String s : System.getProperty("cfhgui.autoWorldScreens", "hud,minecraft:pause,minecraft:chat,cfhgui:hub").split(",")) {
            worldQueue.add(DevHarness.Shot.parse(s));
         }

         waitFrames = Integer.getInteger("cfhgui.autoFrames", 45);
         wholeFrame = Boolean.getBoolean("cfhgui.autoWholeFrame");
         dir = new File(System.getProperty("cfhgui.autoDir", CFHGuiPlatform.get().gameDir().resolve("cfhgui-shots").toString()));
         CFHGuiEvents.HUD_RENDER.register((CFHGuiEvents.HudRender)(g, pt) -> {
            if (Minecraft.getInstance().screen == null) {
               onFrame();
            }

         });
         CFHGuiEvents.CLIENT_TICK_END.register(DevHarness::hudFrameEnd);
         CFHGui.LOGGER.info("[CFHGui] dev harness: {} menu screen(s){} -> {}", new Object[]{queue.size(), wantWorld ? " + world" : "", dir});
      }
   }

   public static void screenFrame() {
      if (queue != null) {
         onFrame();
      }

   }

   private static void onFrame() {
      Minecraft mc = Minecraft.getInstance();
      if (!overridesApplied) {
         overridesApplied = true;
         pauseOnLostFocus = mc.options.pauseOnLostFocus;
         mc.options.pauseOnLostFocus = false;
         String skin = System.getProperty("cfhgui.autoSkin");
         String layout = System.getProperty("cfhgui.autoLayout");
         String motion = System.getProperty("cfhgui.autoMotion");
         if (skin != null) {
            CFHGui.config().skin = skin;
         }

         if (layout != null) {
            CFHGui.config().setLayout(Layout.parse(layout, CFHGui.config().layout()));
         }

         if (motion != null) {
            try {
               CFHGui.config().motion = Double.parseDouble(motion);
            } catch (NumberFormatException var5) {
            }
         }

         if (skin != null || layout != null || motion != null) {
            Theme.reload();
            Reskin.invalidate();
         }
      }

      switch (phase.ordinal()) {
         case 0:
            if (mc.getOverlay() != null || mc.screen == null) {
               return;
            }

            if (afterOverlay) {
               afterOverlay = false;
               frames = 0;
               if (queue.isEmpty()) {
                  advanceFromMenu();
               } else {
                  open((Shot)queue.get(0));
               }

               return;
            }

            if (!started) {
               started = true;
               frames = 0;
               if (queue.isEmpty()) {
                  advanceFromMenu();
                  return;
               }

               open((Shot)queue.get(0));
               return;
            }

            if ("reload".equals(((Shot)queue.get(0)).id())) {
               if (!ready((Shot)queue.get(0))) {
                  return;
               }

               frames = 0;
               capture((Shot)queue.remove(0));
               if (queue.isEmpty()) {
                  advanceFromMenu();
               } else {
                  open((Shot)queue.get(0));
               }

               return;
            }

            if (hudShot != null) {
               pin(hudShot);
               return;
            }

            if (!ready((Shot)queue.get(0))) {
               return;
            }

            frames = 0;
            if (wholeFrame) {
               hudShot = (Shot)queue.get(0);
               return;
            }

            if (!queue.isEmpty()) {
               capture((Shot)queue.remove(0));
            }

            if (queue.isEmpty()) {
               advanceFromMenu();
            } else {
               open((Shot)queue.get(0));
            }
            break;
         case 1:
            if (mc.level == null || mc.player == null || mc.screen != null || mc.getOverlay() != null) {
               frames = 0;
               return;
            }

            if (++frames < waitFrames * 2) {
               return;
            }

            frames = 0;
            seedChat();
            phase = DevHarness.Phase.WORLD;
            openWorldScreen((Shot)worldQueue.get(0));
            break;
         case 2:
            if (hudShot != null) {
               pin(hudShot);
               return;
            }

            if (!ready((Shot)worldQueue.get(0))) {
               return;
            }

            frames = 0;
            if (wholeFrame || mc.screen == null || mc.screen instanceof AbstractContainerScreen) {
               hudShot = (Shot)worldQueue.get(0);
               return;
            }

            capture((Shot)worldQueue.remove(0));
            if (worldQueue.isEmpty()) {
               finish();
            } else {
               openWorldScreen((Shot)worldQueue.get(0));
            }
         case 3:
      }

   }

   private static void hudFrameEnd() {
      Shot shot = hudShot;
      if (shot != null) {
         hudShot = null;
         if (phase == DevHarness.Phase.MENU) {
            if (!queue.isEmpty() && queue.get(0) == shot) {
               capture((Shot)queue.remove(0));
               afterOverlay = true;
            }
         } else if (phase == DevHarness.Phase.WORLD && !worldQueue.isEmpty() && worldQueue.get(0) == shot) {
            capture((Shot)worldQueue.remove(0));
            if (worldQueue.isEmpty()) {
               finish();
            } else {
               openWorldScreen((Shot)worldQueue.get(0));
            }

         }
      }
   }

   public static void overlayFrame() {
      if (queue != null && phase == DevHarness.Phase.MENU && started && !afterOverlay && !queue.isEmpty() && "reload".equals(((Shot)queue.get(0)).id())) {
         if (ready((Shot)queue.get(0))) {
            frames = 0;
            capture((Shot)queue.remove(0));
            afterOverlay = true;
         }
      }
   }

   private static boolean ready(Shot shot) {
      pin(shot);
      return ++frames >= waitFrames && System.currentTimeMillis() - openedMs >= shot.waitMs();
   }

   private static void pin(Shot shot) {
      if (shot.hasMouse()) {
         Minecraft mc = Minecraft.getInstance();
         MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
         mouse.cfhgui$setXpos(shot.mouseX() * (double)mc.getWindow().getScreenWidth());
         mouse.cfhgui$setYpos(shot.mouseY() * (double)mc.getWindow().getScreenHeight());
      }
   }

   private static void advanceFromMenu() {
      if (!wantWorld) {
         finish();
      } else {
         phase = DevHarness.Phase.WORLD_LOADING;
         frames = 0;
         Minecraft mc = Minecraft.getInstance();

         try {
            LevelSettings settings = new LevelSettings("cfhgui-harness", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            WorldOptions options = new WorldOptions(20260923L, false, false);
            String id = System.getProperty("cfhgui.autoWorldId");
            if (id != null && !id.isBlank() && mc.getLevelSource().levelExists(id)) {
               mc.createWorldOpenFlows().openWorld(id, DevHarness::finish);
               return;
            }

            mc.createWorldOpenFlows().createFreshLevel(id != null && !id.isBlank() ? id : "cfhgui-harness-" + System.currentTimeMillis() % 100000L, settings, options, WorldPresets::createNormalWorldDimensions, new TitleScreen());
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] dev harness could not create a world", e);
            finish();
         }

      }
   }

   private static void seedChat() {
      Minecraft mc = Minecraft.getInstance();

      try {
         mc.gui.getChat().addMessage(Component.literal("<Steve> hey, anyone up for the nether trip tonight?"));
         mc.gui.getChat().addMessage(Component.literal("<Steve> I found a fortress at -240 80"));
         mc.gui.getChat().addMessage(Component.literal("<Alex> sure! bring blaze rods :)"));
         mc.gui.getChat().addMessage(Component.literal("<" + mc.getUser().getName() + "> on my way"));
         mc.gui.getChat().addMessage(Component.literal("Steve joined the game"));
         String file = System.getProperty("cfhgui.autoChatFile");
         if (file != null && !file.isBlank()) {
            for(String line : Files.readAllLines(Path.of(file.trim()), StandardCharsets.UTF_8)) {
               if (!line.isBlank()) {
                  mc.gui.getChat().addMessage(Component.literal(line.replace("%me%", mc.getUser().getName())));
               }
            }
         }
      } catch (Exception e) {
         CFHGui.LOGGER.warn("[CFHGui] dev harness could not seed chat: {}", e.toString());
      }

   }

   private static void openWorldScreen(Shot shot) {
      Minecraft mc = Minecraft.getInstance();
      openedMs = System.currentTimeMillis();

      try {
         switch (shot.id()) {
            case "hud":
            case "none":
               mc.setScreen((Screen)null);
               break;
            case "front":
               mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
               mc.setScreen((Screen)null);
               break;
            case "behind":
               mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
               mc.setScreen((Screen)null);
               break;
            case "first":
               mc.options.setCameraType(CameraType.FIRST_PERSON);
               mc.setScreen((Screen)null);
               break;
            case "minecraft:pause":
               mc.setScreen(new PauseScreen(true));
               break;
            case "minecraft:chat":
               mc.setScreen(new ChatScreen(""));
               break;
            case "leave":
               if (mc.level != null) {
                  mc.level.disconnect();
               }

               mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
               mc.setScreen(new TitleScreen());
               break;
            default:
               if (shot.id().startsWith("command:") && mc.player != null) {
                  mc.player.connection.sendCommand(shot.id().substring(8));
               } else if (!input(shot)) {
                  CoreActions.openScreen(shot.id());
               }
         }
      } catch (Exception e) {
         CFHGui.LOGGER.error("[CFHGui] dev harness could not open {}", shot.id(), e);
      }

   }

   private static void capture(Shot shot) {
      Minecraft mc = Minecraft.getInstance();
      String var10000 = shot.raw().replaceAll("[^A-Za-z0-9_.-]", "_");
      String file = var10000 + "-" + Theme.current().skin().name().toLowerCase() + ".png";
      mc.renderBuffers().bufferSource().endBatch();
      Screenshot.grab(dir, file, mc.getMainRenderTarget(), (c) -> CFHGui.LOGGER.info("[CFHGui] dev harness: {}", c.getString()));
   }

   public static void loadingFrame(GuiGraphics g, String what) {
      if (queue != null && dir != null) {
         long every = Long.getLong("cfhgui.autoLoadingShots", 0L);
         long now = System.currentTimeMillis();
         if (every > 0L && now - shotAt >= every && shots < 120) {
            shotAt = now;
            g.flush();
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(dir, "loading-%03d-%s-%s.png".formatted(shots++, what.replaceAll("[^A-Za-z0-9_.-]", "_"), Theme.current().skin().name().toLowerCase()), mc.getMainRenderTarget(), (c) -> {
            });
         }
      }
   }

   public static void loadingFrame(String what) {
      if (queue != null && dir != null) {
         long every = Long.getLong("cfhgui.autoLoadingShots", 0L);
         long now = System.currentTimeMillis();
         if (every > 0L && now - shotAt >= every && shots < 120) {
            shotAt = now;
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(dir, "loading-%03d-%s-%s.png".formatted(shots++, what.replaceAll("[^A-Za-z0-9_.-]", "_"), Theme.current().skin().name().toLowerCase()), mc.getMainRenderTarget(), (c) -> {
            });
         }
      }
   }

   private static void finish() {
      phase = DevHarness.Phase.DONE;
      Minecraft mc = Minecraft.getInstance();
      boolean scaled = guiScaleBefore >= 0 && (Integer)mc.options.guiScale().get() != guiScaleBefore;
      if (scaled) {
         mc.options.guiScale().set(guiScaleBefore);
      }

      if (overridesApplied && mc.options.pauseOnLostFocus != pauseOnLostFocus || scaled) {
         mc.options.pauseOnLostFocus = pauseOnLostFocus;
         mc.options.save();
      }

      if (Boolean.getBoolean("cfhgui.autoQuit")) {
         (new Thread(() -> {
            try {
               Thread.sleep(2000L);
            } catch (InterruptedException var2) {
            }

            Objects.requireNonNull(mc);
            mc.execute(mc::stop);
         }, "cfhgui-harness-quit")).start();
      }

   }

   private static void open(Shot shot) {
      Minecraft mc = Minecraft.getInstance();
      openedMs = System.currentTimeMillis();

      try {
         if ("reload".equals(shot.id())) {
            mc.reloadResourcePacks();
         } else if ("none".equals(shot.id())) {
            mc.setScreen((Screen)null);
         } else if ("minecraft:title".equals(shot.id())) {
            mc.setScreen(new TitleScreen());
         } else if (!input(shot)) {
            CoreActions.openScreen(shot.id());
         }
      } catch (Exception e) {
         CFHGui.LOGGER.error("[CFHGui] dev harness could not open {}", shot.id(), e);
      }

   }

   private static boolean input(Shot shot) {
      Minecraft mc = Minecraft.getInstance();
      if (!"click".equals(shot.id()) && !"rclick".equals(shot.id())) {
         if (shot.id().startsWith("hold:")) {
            String name = shot.id().substring(5);

            for(KeyMapping key : mc.options.keyMappings) {
               if (key.getName().equals(name)) {
                  key.setDown(true);
               }
            }

            return true;
         } else if ("press".equals(shot.id())) {
            if (mc.screen != null && shot.hasMouse()) {
               MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
               mouse.cfhgui$setXpos(shot.mouseX() * (double)mc.getWindow().getScreenWidth());
               mouse.cfhgui$setYpos(shot.mouseY() * (double)mc.getWindow().getScreenHeight());
               long window = mc.getWindow().getWindow();
               mouse.cfhgui$onPress(window, 0, 1, 0);
               mouse.cfhgui$onPress(window, 0, 0, 0);
            }

            return true;
         } else if (shot.id().startsWith("key:")) {
            try {
               String[] parts = shot.id().substring(4).split(":");
               int key = Integer.parseInt(parts[0]);
               int mods = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
               if (mc.screen != null) {
                  mc.screen.keyPressed(key, 0, mods);
               }
            } catch (NumberFormatException var8) {
            }

            return true;
         } else if (shot.id().startsWith("keypress:")) {
            try {
               String[] parts = shot.id().substring(9).split(":");
               int key = Integer.parseInt(parts[0]);
               int mods = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
               long window = mc.getWindow().getWindow();
               int scan = GLFW.glfwGetKeyScancode(key);
               mc.keyboardHandler.keyPress(window, key, scan, 1, mods);
               mc.keyboardHandler.keyPress(window, key, scan, 0, mods);
            } catch (NumberFormatException var9) {
            }

            return true;
         } else if (editorEntry(shot)) {
            return true;
         } else if (shot.id().startsWith("wheel:")) {
            try {
               double notches = Double.parseDouble(shot.id().substring(6));
               if (mc.screen != null && shot.hasMouse()) {
                  mc.screen.mouseScrolled(shot.mouseX() * (double)mc.getWindow().getGuiScaledWidth(), shot.mouseY() * (double)mc.getWindow().getGuiScaledHeight(), (double)0.0F, notches);
               }
            } catch (NumberFormatException var10) {
            }

            return true;
         } else if (shot.id().startsWith("type:")) {
            String text = shot.id().substring(5);

            for(int i = 0; i < text.length() && mc.screen != null; ++i) {
               mc.screen.charTyped(text.charAt(i), 0);
            }

            return true;
         } else if (shot.id().startsWith("drop:")) {
            if (mc.screen != null) {
               mc.screen.onFilesDrop(List.of(Path.of(shot.id().substring(5))));
            }

            return true;
         } else if (shot.id().startsWith("window:")) {
            String[] wh = shot.id().substring(7).split("x");

            try {
               if (wh.length == 2) {
                  mc.getWindow().setWindowed(Integer.parseInt(wh[0].trim()), Integer.parseInt(wh[1].trim()));
               }
            } catch (NumberFormatException var11) {
            }

            return true;
         } else if (shot.id().startsWith("scale:")) {
            try {
               int scale = Integer.parseInt(shot.id().substring(6).trim());
               if (guiScaleBefore < 0) {
                  guiScaleBefore = (Integer)mc.options.guiScale().get();
               }

               mc.options.guiScale().set(scale);
               mc.resizeDisplay();
            } catch (NumberFormatException var12) {
            }

            return true;
         } else if (shot.id().startsWith("container:")) {
            openContainer(shot.id().substring(10));
            return true;
         } else if (!shot.id().startsWith("skin:") && !shot.id().startsWith("layout:")) {
            if (shot.id().startsWith("core:")) {
               if (coreField(shot.id().substring(5))) {
                  Theme.reload();
                  Reskin.invalidate();
                  if (mc.screen != null) {
                     mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                  }
               }

               return true;
            } else if (!shot.id().startsWith("recipebook:")) {
               return false;
            } else {
               if (mc.player != null) {
                  boolean open = "on".equals(shot.id().substring(11).trim());

                  for(RecipeBookType type : RecipeBookType.values()) {
                     mc.player.getRecipeBook().setOpen(type, open);
                  }
               }

               return true;
            }
         } else {
            String value = shot.id().substring(shot.id().indexOf(58) + 1).trim().toUpperCase(Locale.ROOT);
            if (shot.id().startsWith("skin:")) {
               CFHGui.config().skin = value;
            } else {
               CFHGui.config().setLayout(Layout.parse(value, CFHGui.config().layout()));
            }

            Theme.reload();
            Reskin.invalidate();
            if (mc.screen != null) {
               mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }

            return true;
         }
      } else {
         if (mc.screen != null && shot.hasMouse()) {
            int button = "rclick".equals(shot.id()) ? 1 : 0;
            double x = shot.mouseX() * (double)mc.getWindow().getGuiScaledWidth();
            double y = shot.mouseY() * (double)mc.getWindow().getGuiScaledHeight();
            mc.screen.mouseClicked(x, y, button);
            if (mc.screen != null) {
               mc.screen.mouseReleased(x, y, button);
            }
         }

         return true;
      }
   }

   private static boolean coreField(String entry) {
      int eq = entry.indexOf(61);
      if (eq <= 0) {
         return false;
      } else {
         String name = entry.substring(0, eq).trim();
         String value = entry.substring(eq + 1).trim();

         try {
            Field f = CoreConfig.class.getField(name);
            Object cfg = CFHGui.config();
            if (f.getType() == Boolean.TYPE) {
               f.setBoolean(cfg, Boolean.parseBoolean(value));
            } else if (f.getType() == Integer.TYPE) {
               f.setInt(cfg, Integer.parseInt(value));
            } else if (f.getType() == Double.TYPE) {
               f.setDouble(cfg, Double.parseDouble(value));
            } else {
               if (f.getType() != String.class) {
                  return false;
               }

               f.set(cfg, value);
            }

            return true;
         } catch (NumberFormatException | ReflectiveOperationException e) {
            CFHGui.LOGGER.warn("[CFHGui] dev harness: no core setting {}: {}", entry, ((Exception)e).toString());
            return false;
         }
      }
   }

   private static boolean editorEntry(Shot shot) {
      Minecraft mc = Minecraft.getInstance();
      switch (shot.id()) {
         case "edit":
            CFHGui.config().devMode = true;
            if (mc.screen != null && !LayoutEditor.isEditing()) {
               LayoutEditor.start(mc.screen);
            }

            return true;
         case "editsave":
            EditorOverlay ed = LayoutEditor.overlay();
            if (ed != null) {
               ed.saveNow();
            }

            return true;
         case "editexit":
            LayoutEditor.stop();
            return true;
         default:
            if (!shot.id().startsWith("editselect:") && !shot.id().startsWith("editshow:")) {
               if (shot.id().startsWith("drag:") && mc.screen != null && shot.hasMouse()) {
                  String[] to = shot.id().substring(5).split("x");

                  try {
                     double x1 = Double.parseDouble(to[0]);
                     double y1 = Double.parseDouble(to[1]);
                     MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
                     double sw = (double)mc.getWindow().getScreenWidth();
                     double sh = (double)mc.getWindow().getScreenHeight();
                     long window = mc.getWindow().getWindow();
                     mouse.cfhgui$setXpos(shot.mouseX() * sw);
                     mouse.cfhgui$setYpos(shot.mouseY() * sh);
                     mouse.cfhgui$onPress(window, 0, 1, 0);
                     int steps = 12;
                     double gw = (double)mc.getWindow().getGuiScaledWidth();
                     double gh = (double)mc.getWindow().getGuiScaledHeight();
                     double lastX = shot.mouseX() * gw;
                     double lastY = shot.mouseY() * gh;

                     for(int i = 1; i <= 12; ++i) {
                        double t = (double)i / (double)12.0F;
                        double fx = shot.mouseX() + (x1 - shot.mouseX()) * t;
                        double fy = shot.mouseY() + (y1 - shot.mouseY()) * t;
                        mouse.cfhgui$setXpos(fx * sw);
                        mouse.cfhgui$setYpos(fy * sh);
                        ScreenInput.mouseMoved();
                        if (mc.screen != null && LayoutEditor.overlay() == null) {
                           mc.screen.mouseDragged(fx * gw, fy * gh, 0, fx * gw - lastX, fy * gh - lastY);
                        }

                        lastX = fx * gw;
                        lastY = fy * gh;
                     }

                     mouse.cfhgui$onPress(window, 0, 0, 0);
                  } catch (RuntimeException e) {
                     CFHGui.LOGGER.warn("[CFHGui] dev harness: bad drag entry {}: {}", shot.raw(), e.toString());
                  }

                  return true;
               } else {
                  return false;
               }
            } else {
               boolean show = shot.id().startsWith("editshow:");
               String key = shot.id().substring(show ? 9 : 11);
               if (!key.contains("#") && !"logo".equals(key) && !"footer".equals(key)) {
                  key = key + "#0";
               }

               EditorOverlay ed2 = LayoutEditor.overlay();
               if (ed2 != null) {
                  ed2.harnessPick(key, show);
               }

               return true;
            }
      }
   }

   private static void openContainer(String kind) {
      Minecraft mc = Minecraft.getInstance();
      IntegratedServer server = mc.getSingleplayerServer();
      if (server != null && mc.player != null) {
         UUID id = mc.player.getUUID();
         server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
               SimpleMenuProvider var10000;
               switch (kind) {
                  case "chest" -> var10000 = new SimpleMenuProvider((c, inv, p) -> ChestMenu.threeRows(c, inv, new SimpleContainer(27)), Component.translatable("container.chest"));
                  case "large_chest" -> var10000 = new SimpleMenuProvider((c, inv, p) -> ChestMenu.sixRows(c, inv, new SimpleContainer(54)), Component.translatable("container.chestDouble"));
                  case "crafting" -> var10000 = new SimpleMenuProvider((c, inv, p) -> new CraftingMenu(c, inv), Component.translatable("container.crafting"));
                  case "furnace" -> var10000 = new SimpleMenuProvider((c, inv, p) -> new FurnaceMenu(c, inv), Component.translatable("container.furnace"));
                  default -> var10000 = null;
               }

               MenuProvider provider = var10000;
               if (provider != null) {
                  player.openMenu(provider);
               } else {
                  CFHGui.LOGGER.warn("[CFHGui] dev harness: no container {}", kind);
               }

            }
         });
      }
   }

   private DevHarness() {
   }

   static {
      phase = DevHarness.Phase.MENU;
      guiScaleBefore = -1;
   }

   private static enum Phase {
      MENU,
      WORLD_LOADING,
      WORLD,
      DONE;
   }

   private static record Shot(String raw, String id, long waitMs, double mouseX, double mouseY) {
      static Shot parse(String entry) {
         String id = entry.trim();
         double mx = (double)-1.0F;
         double my = (double)-1.0F;
         long wait = 0L;
         int hash = id.indexOf(35);
         if (hash >= 0) {
            String[] xy = id.substring(hash + 1).split("x");
            id = id.substring(0, hash);

            try {
               if (xy.length == 2) {
                  mx = Double.parseDouble(xy[0]);
                  my = Double.parseDouble(xy[1]);
               }
            } catch (NumberFormatException var12) {
            }
         }

         int at = id.lastIndexOf(64);
         if (at > 0) {
            try {
               wait = Long.parseLong(id.substring(at + 1));
            } catch (NumberFormatException var11) {
            }

            id = id.substring(0, at);
         }

         return new Shot(entry.trim(), id, wait, mx, my);
      }

      boolean hasMouse() {
         return this.mouseX >= (double)0.0F && this.mouseY >= (double)0.0F;
      }
   }
}
