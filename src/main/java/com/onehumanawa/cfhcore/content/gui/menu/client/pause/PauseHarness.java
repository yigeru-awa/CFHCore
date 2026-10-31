package com.onehumanawa.cfhcore.content.gui.menu.client.pause;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.mixin.MouseHandlerAccessor;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.LastPlayed;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.OverhaulTitleScreen;
import com.onehumanawa.cfhcore.content.gui.menu.client.title.CFHGuiTitleScreen;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class PauseHarness {
   private static Step step;
   private static After after;
   private static long since;
   private static @Nullable Screen menu;
   private static @Nullable Screen title;
   private static boolean down;
   private static double restX;
   private static double restY;
   private static int leaves;
   private static final List<Entity> summoned;

   public static void register() {
      ride("horse", EntityType.HORSE, (EntityType)null);
      ride("camel", EntityType.CAMEL, (EntityType)null);
      ride("pig", EntityType.PIG, (EntityType)null);
      ride("strider", EntityType.STRIDER, (EntityType)null);
      ride("boat", EntityType.BOAT, (EntityType)null);
      ride("minecart", EntityType.MINECART, (EntityType)null);
      ride("boat_pair", EntityType.BOAT, EntityType.PIG);
      ride("camel_pair", EntityType.CAMEL, EntityType.PIG);
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause/on_foot", (parent) -> {
         mount((EntityType)null, (EntityType)null);
         return parent;
      });
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause/leave", (parent) -> leave(parent, PauseHarness.After.NOTHING));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause/leave_options", (parent) -> leave(parent, PauseHarness.After.CLICK));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause/leave_options_still", (parent) -> leave(parent, PauseHarness.After.CLICK_STILL));
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause/continue", (parent) -> {
         Minecraft mc = Minecraft.getInstance();
         LastPlayed.quick().ifPresent((target) -> LastPlayed.play(target, mc.screen));
         return mc.screen;
      });
      CFHGuiEvents.SCREEN_RENDER_POST.register((CFHGuiEvents.ScreenRender)(screen, g, mouseX, mouseY, partialTick) -> frame(screen, g));
      CFHGuiMenu.LOGGER.info("[CFHGui Menu] the escape menus' states are registered for the harness");
   }

   private static void ride(String id, EntityType<?> carrier, @Nullable EntityType<?> companion) {
      CoreActions.SCREEN_FACTORIES.put("cfhgui:pause/ride_" + id, (parent) -> {
         mount(carrier, companion);
         return parent;
      });
   }

   private static void mount(@Nullable EntityType<?> carrier, @Nullable EntityType<?> companion) {
      Minecraft mc = Minecraft.getInstance();
      IntegratedServer server = mc.getSingleplayerServer();
      if (server != null && mc.player != null) {
         UUID id = mc.player.getUUID();
         server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
               player.stopRiding();

               for(Entity old : summoned) {
                  old.discard();
               }

               summoned.clear();
               if (carrier != null) {
                  Entity under = summon(carrier, player);
                  if (under != null) {
                     if (under instanceof AbstractHorse) {
                        AbstractHorse horse = (AbstractHorse)under;
                        horse.setTamed(true);
                        horse.getSlot(400).set(new ItemStack(Items.SADDLE));
                     }

                     player.startRiding(under, true);
                     Entity beside = companion == null ? null : summon(companion, player);
                     if (beside != null) {
                        beside.startRiding(under, true);
                     }

                  }
               }
            }
         });
      }
   }

   private static @Nullable Entity summon(EntityType<?> type, ServerPlayer at) {
      Entity entity = type.create(at.serverLevel());
      if (entity == null) {
         return null;
      } else {
         entity.moveTo(at.getX(), at.getY(), at.getZ(), at.getYRot(), 0.0F);
         if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            mob.setNoAi(true);
         }

         at.serverLevel().addFreshEntity(entity);
         summoned.add(entity);
         return entity;
      }
   }

   private static Screen leave(@Nullable Screen parent, After then) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] harness: there is no world to leave");
         return parent;
      } else {
         after = then;
         menu = null;
         title = null;
         go(PauseHarness.Step.MENU);
         return new PauseScreen(true);
      }
   }

   private static void go(Step next) {
      step = next;
      since = Util.getMillis();
   }

   private static void fail(String why) {
      CFHGuiMenu.LOGGER.warn("[CFHGui Menu] harness: {}; the run goes on without it", why);
      step = PauseHarness.Step.IDLE;
   }

   private static void frame(Screen screen, GuiGraphics g) {
      if (step != PauseHarness.Step.IDLE) {
         Minecraft mc = Minecraft.getInstance();
         long now = Util.getMillis();
         if (down && (screen != menu || now - since > 2000L)) {
            release();
            if (after == PauseHarness.After.CLICK_STILL && restX >= (double)0.0F) {
               point(restX, restY);
            }
         }

         switch (step.ordinal()) {
            case 1:
               if (now - since < 900L) {
                  return;
               }

               AbstractWidget row = find(screen.children(), Component.translatable(PauseActions.singleplayer() ? "menu.returnToMenu" : "menu.disconnect").getString());
               if (row == null) {
                  fail("the escape menu has no row that leaves");
                  return;
               }

               menu = screen;
               go(PauseHarness.Step.ASKED);
               double rowX = (double)row.getX() + (double)row.getWidth() / (double)2.0F;
               double rowY = (double)row.getY() + (double)row.getHeight() / (double)2.0F;
               down = true;
               between(() -> {
                  point(rowX, rowY);
                  button(true);
                  if (down) {
                     release();
                  }

               });
               break;
            case 2:
               if (now - since < 700L) {
                  return;
               }

               if (Popups.top() instanceof CFHGuiModal) {
                  Component question = PauseActions.leaveQuestion(PauseActions.singleplayer());
                  go(PauseHarness.Step.HELD);
                  down = true;
                  between(() -> {
                     answer(mc, question);
                     button(true);
                  });
               } else if (screen != menu) {
                  go(PauseHarness.Step.LEFT);
               } else {
                  fail("the escape menu neither asked nor left");
               }
               break;
            case 3:
               if (!down) {
                  go(PauseHarness.Step.LEFT);
               }
               break;
            case 4:
               if (!mainMenu(screen)) {
                  return;
               }

               if (title != screen) {
                  title = screen;
                  since = now;
                  ++leaves;
                  CFHGuiMenu.LOGGER.info("[CFHGui Menu] harness: the world is left, the main menu is up ({})", ScreenIds.of(screen));
               }

               if (after == PauseHarness.After.NOTHING) {
                  step = PauseHarness.Step.IDLE;
                  return;
               }

               if (now - since < 1500L) {
                  return;
               }

               Rect options = options(screen);
               if (options == null) {
                  fail("nothing on this main menu opens the options");
                  return;
               }

               double px = mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth();
               double py = mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight();
               boolean rests = after == PauseHarness.After.CLICK_STILL && options.contains(px, py);
               if (!rests) {
                  restX = (double)options.centerX();
                  restY = (double)options.centerY();
                  point(restX, restY);
               }

               CFHGuiMenu.LOGGER.info("[CFHGui Menu] harness: Options stands at {}, {} of the main menu; the pointer {}", new Object[]{options.centerX(), options.centerY(), rests ? "has rested there since the world was being saved" : "is brought to it"});
               go(PauseHarness.Step.AIMED);
               break;
            case 5:
               if (now - since < 700L) {
                  return;
               }

               picture(mc, g);
               go(PauseHarness.Step.CLICKED);
               between(PauseHarness::click);
               break;
            case 6:
               if (now - since < 900L) {
                  return;
               }

               boolean took = mc.screen != title;
               CFHGuiMenu.LOGGER.info("[CFHGui Menu] harness: one click on Options {}: the screen is {}", took ? "opened them" : "DID NOTHING", ScreenIds.of(mc.screen));
               if (took) {
                  step = PauseHarness.Step.IDLE;
                  return;
               }

               go(PauseHarness.Step.AGAIN);
               between(PauseHarness::click);
               break;
            case 7:
               if (now - since < 900L) {
                  return;
               }

               CFHGuiMenu.LOGGER.info("[CFHGui Menu] harness: a second click on Options {}: the screen is {}", mc.screen != title ? "opened them" : "did nothing either", ScreenIds.of(mc.screen));
               step = PauseHarness.Step.IDLE;
         }

      }
   }

   private static boolean mainMenu(Screen screen) {
      return screen instanceof TitleScreen || screen instanceof CFHGuiTitleScreen || screen instanceof OverhaulTitleScreen;
   }

   private static @Nullable Rect options(Screen screen) {
      if (screen instanceof LayoutAware aware) {
         for(LayoutAware.Part part : aware.layoutParts()) {
            if ("cfhgui.overhaul.options#0".equals(part.key()) && part.rect() != null) {
               return part.rect();
            }
         }
      }

      AbstractWidget button = find(screen.children(), Component.translatable("menu.options").getString());
      return button == null ? null : Rect.of(button.getX(), button.getY(), button.getWidth(), button.getHeight());
   }

   private static @Nullable AbstractWidget find(List<? extends GuiEventListener> in, String words) {
      for(GuiEventListener child : in) {
         if (child instanceof AbstractWidget widget) {
            if (widget.visible && widget.getMessage().getString().equals(words)) {
               return widget;
            }
         }

         if (child instanceof ContainerEventHandler container) {
            AbstractWidget inside = find(container.children(), words);
            if (inside != null) {
               return inside;
            }
         }
      }

      return null;
   }

   private static void answer(Minecraft mc, Component question) {
      int sw = mc.getWindow().getGuiScaledWidth();
      int sh = mc.getWindow().getGuiScaledHeight();
      int w = Math.min(240, sw - 16);
      int lines = CFHGuiDraw.font().split(question, w - 24).size();
      int h = 66 + (lines > 0 ? lines * 10 + 6 : 0);
      point((double)(sw + w) / (double)2.0F - (double)42.0F, (double)(sh + h) / (double)2.0F - (double)22.0F);
   }

   private static void point(double guiX, double guiY) {
      Minecraft mc = Minecraft.getInstance();
      MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
      mouse.cfhgui$setXpos(guiX * (double)mc.getWindow().getScreenWidth() / (double)mc.getWindow().getGuiScaledWidth());
      mouse.cfhgui$setYpos(guiY * (double)mc.getWindow().getScreenHeight() / (double)mc.getWindow().getGuiScaledHeight());
   }

   private static void between(Runnable input) {
      Minecraft.getInstance().tell(input);
   }

   private static void button(boolean press) {
      Minecraft mc = Minecraft.getInstance();
      ((MouseHandlerAccessor)mc.mouseHandler).cfhgui$onPress(mc.getWindow().getWindow(), 0, press ? 1 : 0, 0);
   }

   private static void release() {
      down = false;
      button(false);
   }

   private static void click() {
      button(true);
      button(false);
   }

   private static void picture(Minecraft mc, GuiGraphics g) {
      String dir = System.getProperty("cfhgui.autoDir");
      if (dir != null) {
         g.flush();
         Screenshot.grab(new File(dir), "pause_leave-" + leaves + "-before_click-" + Theme.current().skin().name().toLowerCase(Locale.ROOT) + ".png", mc.getMainRenderTarget(), (saved) -> {
         });
      }
   }

   private PauseHarness() {
   }

   static {
      step = PauseHarness.Step.IDLE;
      after = PauseHarness.After.NOTHING;
      restX = (double)-1.0F;
      restY = (double)-1.0F;
      summoned = new ArrayList();
   }

   private static enum After {
      NOTHING,
      CLICK,
      CLICK_STILL;
   }

   private static enum Step {
      IDLE,
      MENU,
      ASKED,
      HELD,
      LEFT,
      AIMED,
      CLICKED,
      AGAIN;
   }
}
