package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.sun.management.ThreadMXBean;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.options.CFHGuiOptionsScreen;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

final class LookAtHarness {
   static final boolean PROFILE = Boolean.getBoolean("cfhgui.lookat.profile");
   private static final boolean STATES = Boolean.getBoolean("cfhgui.lookat.harness") || System.getProperty("cfhgui.autoScreens") != null || Boolean.getBoolean("cfhgui.autoWorld");
   private static final String TAG = "cfhgui_lookat";
   private static final String STILL = "NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:[\"cfhgui_lookat\"]";
   private static final String NEWEST = "@e[tag=cfhgui_lookat,sort=nearest,limit=1]";
   private static final double USUAL = 2.4;
   private static final double NEAR = 1.3;
   private static final int HEIGHT = 200;
   private static int stageX;
   private static int stageZ;
   private static double ahead = 2.4;
   private static final int EVERY_FRAMES = 600;
   private static @Nullable ThreadMXBean threads;
   static final int PLATE = 0;
   static final int FIGURE = 1;
   private static final long[] nanosOf = new long[2];
   private static final long[] bytesOf = new long[2];
   private static long markNanos;
   private static long markBytes;
   private static long frameNanos;
   private static long nanosWorst;
   private static int frames;
   private static @Nullable Runnable later;
   private static int laterIn;

   private LookAtHarness() {
   }

   static void init() {
      if (PROFILE) {
         try {
            java.lang.management.ThreadMXBean var1 = ManagementFactory.getThreadMXBean();
            if (var1 instanceof ThreadMXBean) {
               ThreadMXBean bean = (ThreadMXBean)var1;
               threads = bean;
            }
         } catch (Throwable var4) {
            threads = null;
         }

         CFHGuiMenu.LOGGER.info("[CFHGui Menu] look-at plate: its cost is measured and written here every {} frames it is drawn", 600);
      }

      if (STATES) {
         state("stage", LookAtHarness::stage);
         mob("zombie", "zombie", "");
         mob("hurt", "zombie", ",Health:7f");
         mob("named", "zombie", ",CustomName:'{\"text\":\"Gravedigger\"}'");
         mob("armored", "zombie", "", "item replace entity @e[tag=cfhgui_lookat,sort=nearest,limit=1] armor.head with minecraft:iron_helmet", "item replace entity @e[tag=cfhgui_lookat,sort=nearest,limit=1] armor.chest with minecraft:iron_chestplate");
         mob("villager", "villager", ",VillagerData:{profession:\"minecraft:farmer\",level:3,type:\"minecraft:plains\"}");
         mob("zombie_villager", "zombie_villager", ",VillagerData:{profession:\"minecraft:librarian\",level:2,type:\"minecraft:plains\"}");
         state("wolf", () -> pet("wolf", "Rex"));
         state("cat", () -> pet("cat", "Miso"));
         state("horse", () -> summon("horse", ",Tame:1b", "attribute @e[tag=cfhgui_lookat,sort=nearest,limit=1] minecraft:generic.movement_speed base set 0.3", "attribute @e[tag=cfhgui_lookat,sort=nearest,limit=1] " + jumpAttribute() + " base set 1.0"));
         mob("calf", "cow", ",Age:-24000");
         mob("golem", "iron_golem", "");
         state("none", LookAtHarness::clear);
         state("near", () -> ahead = 1.3);
         state("usual", () -> ahead = 2.4);
         aim("aim_head", 0.0F, 0.0F);
         aim("aim_body", 0.0F, 18.0F);
         aim("aim_feet", 0.0F, 30.0F);
         aim("aim_left", -7.0F, 2.0F);
         aim("aim_right", 7.0F, 2.0F);
         aim("aim_away", 80.0F, 0.0F);
         state("effects", LookAtHarness::effects);

         for(String mode : new String[]{"spectator", "survival", "creative"}) {
            state(mode, () -> command("gamemode " + mode));
         }

         config("on", (c) -> c.enabled = true);
         config("off", (c) -> c.enabled = false);
         config("lines_default", (c) -> lines(c, new LookAtConfig()));
         config("lines_all", (c) -> c.name = c.kind = c.health = c.armor = c.baby = c.owner = c.villager = c.mount = c.effects = true);
         config("lines_few", (c) -> {
            c.name = c.health = true;
            c.kind = c.armor = c.baby = c.owner = c.villager = c.mount = c.effects = false;
         });
         config("figure_on", (c) -> c.portrait = true);
         config("figure_off", (c) -> c.portrait = false);
         config("beside_on", (c) -> c.yieldToMods = false);
         config("beside_off", (c) -> c.yieldToMods = true);
         state("cost", LookAtHarness::report);
         CoreActions.SCREEN_FACTORIES.put("cfhgui:lookat/options", LookAtHarness::options);
         if (Modules.isLoaded("cfhgui_config")) {
            CoreActions.SCREEN_FACTORIES.put("cfhgui:lookat/settings", (parent) -> LookAtSettingsTab.screen(parent));
         }

         CFHGuiEvents.CLIENT_TICK_END.register(LookAtHarness::tick);
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] the look-at plate's states are registered for the harness");
      }
   }

   private static void stage() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         BlockPos at = mc.player.blockPosition();
         stageX = at.getX();
         stageZ = at.getZ();
         command("gamerule sendCommandFeedback false");
         command("gamerule doDaylightCycle false");
         command("gamerule doWeatherCycle false");
         command("gamerule doMobSpawning false");
         command("time set noon");
         command("weather clear");
         command("difficulty easy");
         int var10000 = stageX - 4;
         command("fill " + var10000 + " 199 " + (stageZ - 4) + " " + (stageX + 4) + " 199 " + (stageZ + 9) + " minecraft:smooth_stone");
         var10000 = stageX - 4;
         command("fill " + var10000 + " 205 " + (stageZ - 4) + " " + (stageX + 4) + " 205 " + (stageZ + 9) + " minecraft:smooth_stone");
         look(0.0F, 0.0F);
      }
   }

   private static void look(float yaw, float pitch) {
      command(String.format(Locale.ROOT, "tp @s %.1f %d %.1f %.1f %.1f", (double)stageX + (double)0.5F, 200, (double)stageZ + (double)0.5F, yaw, pitch));
   }

   private static void aim(String id, float yaw, float pitch) {
      state(id, () -> look(yaw, pitch));
   }

   private static void clear() {
      command("execute as @e[tag=cfhgui_lookat] run tp @s " + stageX + " 160 " + stageZ);
   }

   private static void mob(String id, String type, String more, String... after) {
      state(id, () -> summon(type, more, after));
   }

   private static void summon(String type, String more, String... after) {
      clear();
      command(String.format(Locale.ROOT, "summon minecraft:%s %.1f %d %.1f {%s%s}", type, (double)stageX + (double)0.5F, 200, (double)stageZ + (double)0.5F + ahead, "NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:[\"cfhgui_lookat\"]", more));

      for(String line : after) {
         command(line);
      }

   }

   private static String jumpAttribute() {
      String generic = "minecraft:generic.jump_strength";
      return BuiltInRegistries.ATTRIBUTE.containsKey(ResourceLocation.tryParse("minecraft:generic.jump_strength")) ? "minecraft:generic.jump_strength" : "minecraft:horse.jump_strength";
   }

   private static void pet(String type, String name) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         int[] id = UUIDUtil.uuidToIntArray(mc.player.getUUID());
         summon(type, ",Owner:[I;" + id[0] + "," + id[1] + "," + id[2] + "," + id[3] + "],CustomName:'{\"text\":\"" + name + "\"}'");
      }
   }

   private static void effects() {
      LivingEntity mob = LookTarget.of(Minecraft.getInstance());
      if (mob != null) {
         mob.forceAddEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1), (Entity)null);
         mob.forceAddEffect(new MobEffectInstance(MobEffects.REGENERATION, 2400, 0), (Entity)null);
         mob.forceAddEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 0), (Entity)null);
      }
   }

   private static void command(String line) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.connection.sendCommand(line);
      }

   }

   private static void config(String id, Consumer<LookAtConfig> change) {
      state(id, () -> {
         change.accept(LookAtConfig.get());
         LookAtConfig.touch();
      });
   }

   private static void lines(LookAtConfig c, LookAtConfig from) {
      c.name = from.name;
      c.kind = from.kind;
      c.health = from.health;
      c.armor = from.armor;
      c.baby = from.baby;
      c.owner = from.owner;
      c.villager = from.villager;
      c.mount = from.mount;
      c.effects = from.effects;
   }

   private static void state(String id, Runnable set) {
      CoreActions.SCREEN_FACTORIES.put("cfhgui:lookat/" + id, (parent) -> {
         try {
            set.run();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] harness state lookat/{} failed", id, e);
         }

         return parent;
      });
   }

   private static void tick() {
      if (later != null && --laterIn <= 0) {
         Runnable run = later;
         later = null;

         try {
            run.run();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] a harness state's second step failed", e);
         }

      }
   }

   private static Screen options(@Nullable Screen parent) {
      CFHGuiOptionsScreen screen = new CFHGuiOptionsScreen(parent);
      later = () -> {
         screen.showPage("menu");

         for(GuiEventListener child : screen.children()) {
            if (child instanceof CFHGuiScrollPanel panel) {
               panel.snapScroll((double)panel.maxScroll());
            }
         }

      };
      laterIn = 3;
      return screen;
   }

   private static long allocated() {
      ThreadMXBean bean = threads;
      return bean == null ? 0L : bean.getCurrentThreadAllocatedBytes();
   }

   static void began() {
      frameNanos = 0L;
      markBytes = allocated();
      markNanos = System.nanoTime();
   }

   static void part(int which) {
      long nanos = System.nanoTime() - markNanos;
      long bytes = allocated() - markBytes;
      long[] var10000 = nanosOf;
      var10000[which] += nanos;
      var10000 = bytesOf;
      var10000[which] += bytes;
      frameNanos += nanos;
      markBytes = allocated();
      markNanos = System.nanoTime();
   }

   static void ended(boolean drew) {
      if (drew) {
         if (frameNanos > nanosWorst) {
            nanosWorst = frameNanos;
         }

         if (++frames >= 600) {
            report();
         }

      }
   }

   private static void report() {
      if (frames != 0) {
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] look-at plate cost over {} frames it was drawn in: the plate {} us and {} bytes a frame, the mob in its well {} us and {} bytes, on the render thread (the bytes are the game's own: its font's, the mob's renderer's); worst frame {} us", new Object[]{frames, micros(nanosOf[0]), bytes(bytesOf[0]), micros(nanosOf[1]), bytes(bytesOf[1]), String.format(Locale.ROOT, "%.0f", (double)nanosWorst / (double)1000.0F)});
         frames = 0;
         nanosWorst = 0L;
         nanosOf[0] = nanosOf[1] = bytesOf[0] = bytesOf[1] = 0L;
      }
   }

   private static String micros(long nanos) {
      return String.format(Locale.ROOT, "%.1f", (double)nanos / (double)1000.0F / (double)frames);
   }

   private static String bytes(long sum) {
      return threads == null ? "?" : Long.toString(sum / (long)frames);
   }
}
