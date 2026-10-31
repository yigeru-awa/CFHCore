package com.onehumanawa.cfhcore.content.gui.core.screen.reskin;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.mixin.SpriteContentsAccessor;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.Nullable;

public final class BetterInventoryPalette implements ResourceManagerReloadListener {
   public static final String MOD = "betterinventory";
   private static final String CLASSES = "dev.fallingcloud.betterinventory.";
   private static final String ART = "textures/gui";
   private static final String[] HOTBAR = new String[]{"hud/hotbar", "hud/hotbar_selection", "hud/hotbar_offhand_left", "hud/hotbar_offhand_right"};
   private static final String INVENTORY_SHEET = "textures/gui/inventory.png";
   private static final String HUD_SHEET = "textures/gui/hud.png";
   private static final int ORNAMENT_MAX = 8;
   private static final String VANILLA_PACK = "vanilla_style";
   private static boolean looked;
   private static boolean installed;
   private static boolean registered;
   private static boolean failed;
   private static boolean noHotbar;
   private static int applied;
   private static @Nullable Tones tones;
   private static final Map<ResourceLocation, DynamicTexture> SHEETS = new HashMap();
   private static final Map<ResourceLocation, Stitched> SPRITES = new HashMap();
   private static @Nullable Screen seen;
   private static Open open;
   private static @Nullable Sprite tabSprite;
   private static boolean tabLooked;
   private static boolean onOpenTab;

   private BetterInventoryPalette() {
   }

   public static synchronized boolean installed() {
      if (!looked) {
         looked = true;

         try {
            installed = CFHGuiPlatform.get().isModLoaded("betterinventory");
         } catch (RuntimeException e) {
            CFHGui.LOGGER.warn("[CFHGui] could not ask whether Better Inventory is installed: {}", e.toString());
         }
      }

      return installed;
   }

   public static void init() {
      if (installed()) {
         MenuSlots.resolveWith((screen) -> screen.getClass().getName().startsWith("dev.fallingcloud.betterinventory.") ? "betterinventory:inventory" : null);
         MenuSlots.onChange(Reskin::invalidate);
      }
   }

   public static boolean ownLook(Screen screen) {
      return screen.getClass().getName().startsWith("dev.fallingcloud.betterinventory.") && installed() && style() == Style.VANILLA;
   }

   private static Style style() {
      return MenuSlots.style("betterinventory:inventory");
   }

   private static boolean wanted() {
      if (!installed()) {
         return false;
      } else if (style() != Style.CFHGUI) {
         return false;
      } else {
         CoreConfig cfg = CFHGui.config();
         if (!cfg.reskinContainers) {
            return false;
         } else {
            for(String deny : cfg.reskinDenylist) {
               if (!deny.isEmpty() && "dev.fallingcloud.betterinventory.".startsWith(deny)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getOverlay() == null) {
         if (installed()) {
            followStyle(mc);
         }

         int want = wanted() ? Theme.current().containerPalette().accent() | -16777216 : 0;
         if ((want != 0 || applied != 0) && !failed) {
            if (!registered) {
               registered = true;
               ResourceManager var3 = mc.getResourceManager();
               if (var3 instanceof ReloadableResourceManager) {
                  ReloadableResourceManager reloadable = (ReloadableResourceManager)var3;
                  reloadable.registerReloadListener(new BetterInventoryPalette());
               }
            }

            if (want != applied) {
               apply(mc.getResourceManager(), want);
            }

         }
      }
   }

   public void onResourceManagerReload(ResourceManager manager) {
      SPRITES.clear();
      failed = false;
      noHotbar = false;
      int want = wanted() ? Theme.current().containerPalette().accent() | -16777216 : 0;
      if (want != 0 || applied != 0) {
         apply(manager, want);
      }

   }

   private static void followStyle(Minecraft mc) {
      Style style = style();
      String matched = CFHGui.config().betterInventoryStyle;
      if (!style.name().equals(matched)) {
         CFHGui.configFile().update((c) -> c.betterInventoryStyle = style.name());
         if (matched != null && !matched.isEmpty()) {
            try {
               PackRepository packs = mc.getResourcePackRepository();
               String id = null;

               for(String available : packs.getAvailableIds()) {
                  if (available.contains("betterinventory") && available.contains("vanilla_style")) {
                     id = available;
                     break;
                  }
               }

               if (id == null) {
                  return;
               }

               boolean on = packs.getSelectedIds().contains(id);
               boolean want = style == Style.VANILLA;
               if (on == want) {
                  return;
               }

               if (want) {
                  packs.addPack(id);
               } else {
                  packs.removePack(id);
               }

               mc.options.updateResourcePacks(packs);
            } catch (RuntimeException e) {
               CFHGui.LOGGER.warn("[CFHGui] Better Inventory's Vanilla Style pack could not be switched: {}", e.toString());
            }

         }
      }
   }

   public static boolean paints(@Nullable Screen screen) {
      if (screen != null && screen.getClass().getName().startsWith("dev.fallingcloud.betterinventory.")) {
         return applied != 0 || style() == Style.VANILLA;
      } else {
         return false;
      }
   }

   private static void apply(ResourceManager manager, int want) {
      Minecraft mc = Minecraft.getInstance();
      TextureManager textures = mc.getTextureManager();
      tones = want == 0 ? null : BetterInventoryPalette.Tones.of(Theme.current().containerPalette());
      seen = null;

      try {
         Map<ResourceLocation, DynamicTexture> gone = new HashMap(SHEETS);
         if (tones != null) {
            Map<ResourceLocation, Resource> found = manager.listResources("textures/gui", (idx) -> idx.getNamespace().equals("betterinventory") && idx.getPath().endsWith(".png"));

            for(Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
               NativeImage image = repainted((Resource)e.getValue(), ((ResourceLocation)e.getKey()).getPath(), tones);
               if (image != null) {
                  gone.remove(e.getKey());
                  DynamicTexture texture = new DynamicTexture(image);
                  textures.register((ResourceLocation)e.getKey(), texture);
                  SHEETS.put((ResourceLocation)e.getKey(), texture);
               }
            }
         }

         for(ResourceLocation id : gone.keySet()) {
            textures.release(id);
            SHEETS.remove(id);
         }

         applied = want;
      } catch (Exception e) {
         CFHGui.LOGGER.warn("[CFHGui] Better Inventory could not be given CFHGui's palette: {}", e.toString());

         for(ResourceLocation id : SHEETS.keySet()) {
            textures.release(id);
         }

         SHEETS.clear();
         tones = null;
         applied = 0;
         failed = true;
         return;
      }

      if (!noHotbar) {
         try {
            for(String name : HOTBAR) {
               sprite(mc, ResourceLocation.withDefaultNamespace(name), tones);
            }
         } catch (Exception e) {
            noHotbar = true;
            CFHGui.LOGGER.warn("[CFHGui] Better Inventory's hotbar keeps its own colours: {}", e.toString());
         }

      }
   }

   private static @Nullable NativeImage repainted(Resource resource, String path, Tones t) {
      NativeImage image;
      try {
         InputStream in = resource.open();

         try {
            image = NativeImage.read(in);
         } catch (Throwable var11) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (in != null) {
            in.close();
         }
      } catch (Exception var12) {
         return null;
      }

      int w = image.getWidth();
      int h = image.getHeight();
      int[] from = new int[w * h];

      for(int y = 0; y < h; ++y) {
         for(int x = 0; x < w; ++x) {
            from[y * w + x] = argb(image.getPixelRGBA(x, y));
         }
      }

      int[] to = repaint(from, w, h, t, path.equals("textures/gui/hud.png"), path.equals("textures/gui/inventory.png") ? tabSprite() : null);
      if (to == null) {
         image.close();
         return null;
      } else {
         for(int y = 0; y < h; ++y) {
            for(int x = 0; x < w; ++x) {
               image.setPixelRGBA(x, y, argb(to[y * w + x]));
            }
         }

         return image;
      }
   }

   private static int @Nullable [] repaint(int[] from, int w, int h, Tones t, boolean whiteIsActive, @Nullable Sprite openTab) {
      boolean light = light(from);
      int[] white = light ? null : whiteRuns(from, w, h);
      int[] to = new int[from.length];
      boolean changed = false;

      for(int i = 0; i < from.length; ++i) {
         int c = from[i];
         int alpha = c >>> 24;
         int rgb = c & 16777215;
         if (alpha == 0) {
            to[i] = c;
         } else {
            int tone = light ? ofLight(rgb, t) : ofDark(rgb, t);
            if (white != null && white[i] > 0) {
               tone = white[i] <= 8 ? t.ornament : (whiteIsActive ? t.accent : t.lit);
            }

            if (!light && rgb == 0 && alpha == 255 && besideSlot(from, w, h, i)) {
               tone = t.line;
            }

            if (tone == 0) {
               to[i] = c;
            } else {
               to[i] = alpha << 24 | tone & 16777215;
               changed |= to[i] != c;
            }
         }
      }

      if (openTab != null && openTab.x >= 0 && openTab.y >= 0 && openTab.x + openTab.w <= w && openTab.y + openTab.h <= h) {
         changed |= paintOpenTab(from, to, w, openTab, light, t);
      }

      return changed ? to : null;
   }

   private static int[] whiteRuns(int[] pixels, int w, int h) {
      int[] size = new int[pixels.length];
      int[] run = new int[pixels.length];

      for(int start = 0; start < pixels.length; ++start) {
         if (pixels[start] == -1 && size[start] == 0) {
            int count = 0;
            run[count++] = start;
            size[start] = -1;

            for(int next = 0; next < count; ++next) {
               int x = run[next] % w;
               int y = run[next] / w;

               for(int dy = -1; dy <= 1; ++dy) {
                  for(int dx = -1; dx <= 1; ++dx) {
                     int nx = x + dx;
                     int ny = y + dy;
                     if (nx >= 0 && ny >= 0 && nx < w && ny < h) {
                        int n = ny * w + nx;
                        if (pixels[n] == -1 && size[n] == 0) {
                           size[n] = -1;
                           run[count++] = n;
                        }
                     }
                  }
               }
            }

            for(int i = 0; i < count; ++i) {
               size[run[i]] = count;
            }
         }
      }

      return size;
   }

   private static boolean paintOpenTab(int[] from, int[] to, int w, Sprite tab, boolean light, Tones t) {
      boolean changed = false;
      if (light) {
         for(int y = 0; y < tab.h; ++y) {
            for(int x = 0; x < tab.w; ++x) {
               int i = (tab.y + y) * w + tab.x + x;
               if (from[i] == -1) {
                  to[i] = -16777216 | t.accent & 16777215;
                  changed = true;
               }
            }
         }

         return changed;
      } else {
         int x0 = tab.w;
         int y0 = tab.h;
         int x1 = -1;
         int y1 = -1;

         for(int y = 0; y < tab.h; ++y) {
            for(int x = 0; x < tab.w; ++x) {
               if (frame(from[(tab.y + y) * w + tab.x + x])) {
                  x0 = Math.min(x0, x);
                  y0 = Math.min(y0, y);
                  x1 = Math.max(x1, x);
                  y1 = Math.max(y1, y);
               }
            }
         }

         for(int y = 0; y < tab.h; ++y) {
            for(int x = 0; x < tab.w; ++x) {
               int i = (tab.y + y) * w + tab.x + x;
               boolean within = x > x0 && x < x1 && y > y0 && y < y1;
               int tone = frame(from[i]) ? t.accent : (within && from[i] == -16777216 ? t.tabFill : 0);
               if (tone != 0) {
                  to[i] = -16777216 | tone & 16777215;
                  changed = true;
               }
            }
         }

         return changed;
      }
   }

   private static boolean frame(int argb) {
      return argb == -8355712 || argb == -8421505;
   }

   private static @Nullable Sprite tabSprite() {
      if (!tabLooked) {
         tabLooked = true;

         try {
            Class<?> layout = Class.forName("dev.fallingcloud.betterinventory.BetterInventoryLayout");
            int x = layout.getField("SPR_TAB_X").getInt(null);
            int y = layout.getField("SPR_TAB_Y").getInt(null);
            int w = layout.getField("TAB_W").getInt(null);
            int h = layout.getField("TAB_H").getInt(null);
            if (w > 0 && h > 0 && w <= 64 && h <= 64) {
               tabSprite = new Sprite(x, y + h + 1, w, h);
            }
         } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
            CFHGui.LOGGER.info("[CFHGui] Better Inventory's tab layout is not the one known here; its open tab keeps the frame colour ({})", ((Throwable)e).toString());
         }
      }

      return tabSprite;
   }

   private static boolean light(int[] pixels) {
      int dark = 0;
      int bright = 0;

      for(int c : pixels) {
         if (c >>> 24 >= 128) {
            if (Colors.luminance(c) > 0.4F) {
               ++bright;
            } else {
               ++dark;
            }
         }
      }

      return bright > dark;
   }

   private static boolean besideSlot(int[] pixels, int w, int h, int at) {
      int x = at % w;
      int y = at / w;

      for(int dy = -1; dy <= 1; ++dy) {
         for(int dx = -1; dx <= 1; ++dx) {
            int nx = x + dx;
            int ny = y + dy;
            if (nx >= 0 && ny >= 0 && nx < w && ny < h && pixels[ny * w + nx] == -15066598) {
               return true;
            }
         }
      }

      return false;
   }

   private static int ofDark(int rgb, Tones t) {
      int var10000;
      switch (rgb) {
         case 0:
            var10000 = t.bg;
            break;
         case 1315860:
            var10000 = t.deep;
            break;
         case 1710618:
            var10000 = t.well;
            break;
         case 2368548:
            var10000 = t.faint;
            break;
         case 2500140:
            var10000 = t.bg;
            break;
         case 3355443:
            var10000 = t.glyph;
            break;
         case 4210752:
         case 5723991:
            var10000 = t.strong;
            break;
         case 6052956:
            var10000 = t.mid;
            break;
         case 6052966:
            var10000 = t.dim;
            break;
         case 8355711:
         case 8421504:
            var10000 = t.dim;
            break;
         case 8618895:
            var10000 = t.muted;
            break;
         case 14145495:
            var10000 = t.text;
            break;
         case 16777215:
            var10000 = t.lit;
            break;
         default:
            var10000 = 0;
      }

      return var10000;
   }

   private static int ofLight(int rgb, Tones t) {
      int var10000;
      switch (rgb) {
         case 0:
         case 1315860:
            var10000 = t.rim;
            break;
         case 3618615:
         case 16777215:
            var10000 = t.line;
            break;
         case 3618912:
            var10000 = t.hoverShade;
            break;
         case 5592405:
            var10000 = t.border;
            break;
         case 7237230:
            var10000 = t.deep;
            break;
         case 7829664:
            var10000 = t.hover;
            break;
         case 9145227:
            var10000 = t.well;
            break;
         case 11447982:
            var10000 = t.sunk;
            break;
         case 13027014:
            var10000 = t.bg;
            break;
         case 13619447:
            var10000 = t.hoverEdge;
            break;
         case 13816530:
         case 14079702:
            var10000 = t.surface;
            break;
         default:
            var10000 = 0;
      }

      return var10000;
   }

   private static void sprite(Minecraft mc, ResourceLocation id, @Nullable Tones t) {
      TextureAtlasSprite sprite = mc.getGuiSprites().getSprite(id);
      if (!sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
         NativeImage image = ((SpriteContentsAccessor)sprite.contents()).cfhgui$originalImage();
         int w = image.getWidth();
         int h = image.getHeight();
         Stitched stitched = (Stitched)SPRITES.get(id);
         if (stitched == null || stitched.image != image) {
            int[] pixels = new int[w * h];

            for(int y = 0; y < h; ++y) {
               for(int x = 0; x < w; ++x) {
                  pixels[y * w + x] = argb(image.getPixelRGBA(x, y));
               }
            }

            stitched = new Stitched(image, pixels);
            SPRITES.put(id, stitched);
         }

         int[] to = stitched.pixels;
         if (t != null) {
            if (!darkHotbar(stitched.pixels)) {
               return;
            }

            int[] painted = repaint(stitched.pixels, w, h, t, true, (Sprite)null);
            if (painted != null) {
               to = painted;
            }
         }

         boolean same = true;

         for(int y = 0; y < h; ++y) {
            for(int x = 0; x < w; ++x) {
               int c = argb(to[y * w + x]);
               if (image.getPixelRGBA(x, y) != c) {
                  image.setPixelRGBA(x, y, c);
                  same = false;
               }
            }
         }

         if (!same) {
            mc.getTextureManager().getTexture(sprite.atlasLocation()).bind();
            sprite.uploadFirstFrame();
         }
      }
   }

   private static boolean darkHotbar(int[] pixels) {
      for(int c : pixels) {
         if (c >>> 24 != 0) {
            int rgb = c & 16777215;
            if (rgb != 0 && rgb != 5723991 && rgb != 8421504 && rgb != 16777215) {
               return false;
            }
         }
      }

      return true;
   }

   private static int argb(int abgr) {
      return abgr & -16711936 | abgr >> 16 & 255 | (abgr & 255) << 16;
   }

   private static Open open() {
      if (tones == null) {
         return BetterInventoryPalette.Open.NONE;
      } else {
         Screen screen = Minecraft.getInstance().screen;
         if (screen != seen) {
            seen = screen;
            String name = screen == null ? "" : screen.getClass().getName();
            open = name.startsWith("dev.fallingcloud.betterinventory.") ? (name.endsWith("BlockChooserScreen") ? BetterInventoryPalette.Open.CHOOSER : BetterInventoryPalette.Open.CONTAINER) : (screen instanceof InventoryScreen ? BetterInventoryPalette.Open.KEY : BetterInventoryPalette.Open.NONE);
         }

         return open;
      }
   }

   public static int fill(int color) {
      Tones t = tones;
      if (t == null) {
         return color;
      } else {
         Open on = open();
         if (on == BetterInventoryPalette.Open.NONE) {
            return color;
         } else {
            int rgb = color & 16777215;
            if (on == BetterInventoryPalette.Open.KEY) {
               int var6;
               switch (rgb) {
                  case 1711395 -> var6 = t.border;
                  case 7239302 -> var6 = t.key;
                  case 9344936 -> var6 = t.keyHover;
                  case 10134452 -> var6 = t.strong;
                  case 11976654 -> var6 = t.muted;
                  case 15922681 -> var6 = t.text;
                  default -> var6 = 0;
               }

               int key = var6;
               return key == 0 ? color : color & -16777216 | key & 16777215;
            } else {
               int var10000;
               if (on == BetterInventoryPalette.Open.CHOOSER) {
                  switch (rgb) {
                     case 0 -> var10000 = t.border;
                     case 4034109 -> var10000 = t.accent;
                     case 5592405 -> var10000 = t.strong;
                     case 9474192 -> var10000 = t.surface;
                     case 10526880 -> var10000 = t.key;
                     case 11579568 -> var10000 = t.well;
                     case 13027014 -> var10000 = t.bg;
                     default -> var10000 = 0;
                  }
               } else {
                  switch (rgb) {
                     case 1513243:
                        var10000 = t.well;
                        break;
                     case 1710618:
                     case 9145227:
                        var10000 = color >>> 24 == 158 ? t.well : 0;
                        break;
                     case 1710622:
                     case 2500140:
                        var10000 = t.rim;
                        break;
                     case 1711395:
                        var10000 = t.border;
                        break;
                     case 2763313:
                        var10000 = t.line;
                        break;
                     case 4868682:
                        var10000 = t.strong;
                        break;
                     case 5685850:
                        var10000 = t.success;
                        break;
                     case 6052966:
                        var10000 = t.dim;
                        break;
                     case 7239302:
                        var10000 = t.key;
                        break;
                     case 8618895:
                        var10000 = t.muted;
                        break;
                     case 9344936:
                        var10000 = t.keyHover;
                        break;
                     case 10134452:
                        var10000 = t.strong;
                        break;
                     case 11813951:
                     case 12597547:
                        var10000 = t.danger;
                        break;
                     case 11976654:
                        var10000 = t.muted;
                        break;
                     case 12105912:
                     case 15132394:
                        var10000 = t.muted;
                        break;
                     case 15922681:
                        var10000 = t.text;
                        break;
                     default:
                        var10000 = 0;
                  }
               }

               int tone = var10000;
               return tone == 0 ? color : color & -16777216 | tone & 16777215;
            }
         }
      }
   }

   public static int text(int color) {
      Tones t = tones;
      if (t == null) {
         return color;
      } else {
         Open on = open();
         if (on != BetterInventoryPalette.Open.NONE && on != BetterInventoryPalette.Open.KEY) {
            int var10000;
            switch (color & 16777215) {
               case 2236970:
               case 3158064:
                  var10000 = t.text;
                  break;
               case 2254370:
                  var10000 = t.ink;
                  break;
               case 3355443:
                  var10000 = onOpenTab ? t.text : t.dim;
                  break;
               case 4144966:
               case 4210752:
               case 12171714:
                  var10000 = t.muted;
                  break;
               case 8026754:
                  var10000 = t.dim;
                  break;
               default:
                  var10000 = 0;
            }

            int tone = var10000;
            return tone == 0 ? color : color & -16777216 | tone & 16777215;
         } else {
            return color;
         }
      }
   }

   public static void blit(ResourceLocation texture, float u, float v, int w, int h) {
      if (tones != null && tabSprite != null) {
         if (texture.getNamespace().equals("betterinventory")) {
            Sprite tab = tabSprite;
            onOpenTab = (int)u == tab.x && (int)v == tab.y && w == tab.w && h == tab.h && texture.getPath().equals("textures/gui/inventory.png");
         }
      }
   }

   static {
      open = BetterInventoryPalette.Open.NONE;
   }

   private static enum Open {
      NONE,
      CONTAINER,
      CHOOSER,
      KEY;
   }

   private static record Tones(int bg, int rim, int deep, int sunk, int well, int line, int faint, int glyph, int border, int strong, int mid, int dim, int muted, int text, int accent, int ink, int lit, int ornament, int tabFill, int surface, int key, int keyHover, int danger, int success, int hover, int hoverEdge, int hoverShade) {
      static Tones of(Palette p) {
         int well = Colors.brighten(p.bg(), -0.32F);
         return new Tones(p.bg(), Colors.brighten(p.bg(), -0.5F), Colors.brighten(p.bg(), -0.2F), Colors.brighten(p.bg(), -0.14F), well, Colors.mix(p.bg(), p.border(), 0.8F), Colors.mix(p.bg(), p.border(), 0.55F), Colors.mix(p.border(), p.textDim(), 0.35F), p.border(), p.borderStrong(), Colors.mix(p.borderStrong(), p.textDim(), 0.5F), p.textDim(), p.textMuted(), p.text(), p.accent(), p.accentInk(), p.text(), p.textMuted(), Colors.mix(p.bg(), p.accent(), 0.34F), p.surface(), p.surfaceActive(), p.borderStrong(), p.danger(), p.success(), Colors.mix(well, p.accent(), 0.45F), p.accent(), Colors.mix(p.bg(), p.accent(), 0.3F));
      }
   }

   private static record Sprite(int x, int y, int w, int h) {
   }

   private static record Stitched(NativeImage image, int[] pixels) {
   }
}
