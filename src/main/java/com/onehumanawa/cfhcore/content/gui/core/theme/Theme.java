package com.onehumanawa.cfhcore.content.gui.core.theme;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class Theme {
   /** @deprecated */
   @Deprecated
   public static final ResourceLocation HEADING_FONT;
   public static final int PANEL_ALPHA = 156;
   public static final int CHROME_ALPHA = 230;
   private static volatile Theme dark;
   private static volatile Theme vanilla;
   private static volatile Theme current;
   private static volatile Style globalStyle;
   private static volatile Style activeStyle;
   private static volatile @Nullable Supplier<Style> activeResolver;
   private static final List<Runnable> LISTENERS;
   private static int panelDepth;
   private final Skin skin;
   private final Palette palette;
   private final Palette containerPalette;
   private final int radius;
   private final int containerRadius;
   private final boolean smoothCorners;
   private final float motion;
   private final boolean headingFont;
   private final PixelFont pixelFont;
   private final boolean transitions;
   private final boolean uiSounds;
   private final boolean toasts;
   private final boolean blurInGame;
   private final boolean translucentPanels;
   private static final String CONTROL_LOOK;
   private static int rowDepth;
   private static final float REST_LIFT = 0.06F;

   private Theme(CoreConfig cfg, Skin skin) {
      this.skin = skin;
      int accent = Colors.fromHex(cfg.accent, -7726052);
      this.palette = skin == Skin.VANILLA ? Palette.vanilla(accent) : Palette.dark(accent);
      this.containerPalette = Palette.dark(accent);
      this.containerRadius = Math.max(0, Math.min(4, cfg.radius));
      this.radius = skin == Skin.VANILLA ? 0 : this.containerRadius;
      this.smoothCorners = cfg.smoothCorners;
      this.motion = (float)Math.max((double)0.0F, Math.min((double)2.0F, cfg.motion));
      this.headingFont = cfg.headingFont;
      this.pixelFont = PixelFont.parse(cfg.pixelFont);
      this.transitions = cfg.transitions;
      this.uiSounds = cfg.uiSounds;
      this.toasts = cfg.toasts;
      this.blurInGame = cfg.blurInGame;
      this.translucentPanels = skin == Skin.DARK && cfg.translucentPanels;
   }

   public static Theme current() {
      return current;
   }

   public static Theme variant(Style style) {
      return style == Style.VANILLA ? vanilla : dark;
   }

   public static void reload() {
      CoreConfig cfg = CFHGui.config();
      dark = new Theme(cfg, Skin.DARK);
      vanilla = new Theme(cfg, Skin.VANILLA);
      globalStyle = cfg.style();
      Supplier<Style> resolver = activeResolver;
      Style style = null;
      if (resolver != null) {
         try {
            style = (Style)resolver.get();
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] style resolver threw: {}", e.toString());
         }
      }

      activate(style);

      for(Runnable r : LISTENERS) {
         try {
            r.run();
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] theme listener threw", e);
         }
      }

   }

   public static void onChange(Runnable listener) {
      LISTENERS.add(listener);
   }

   public static void setActiveStyleResolver(@Nullable Supplier<Style> resolver) {
      activeResolver = resolver;
   }

   public static void activate(@Nullable Style style) {
      activeStyle = style == null ? globalStyle : style;
      current = variant(activeStyle);
   }

   public static Style globalStyle() {
      return globalStyle;
   }

   public static Style activeStyle() {
      return activeStyle;
   }

   public Skin skin() {
      return this.skin;
   }

   public boolean isVanilla() {
      return this.skin == Skin.VANILLA;
   }

   public Palette palette() {
      return this.palette;
   }

   public Palette containerPalette() {
      return this.containerPalette;
   }

   public int radius() {
      return this.radius;
   }

   public int containerRadius() {
      return this.containerRadius;
   }

   public boolean smoothCorners() {
      return this.smoothCorners;
   }

   /** @deprecated */
   @Deprecated
   public static boolean customLayout() {
      return CFHGui.config().layout() != Layout.VANILLA;
   }

   public float motion() {
      return this.motion;
   }

   public boolean transitions() {
      return this.transitions;
   }

   public boolean uiSounds() {
      return this.uiSounds;
   }

   public boolean toasts() {
      return this.toasts;
   }

   public boolean blurInGame() {
      return this.blurInGame;
   }

   public boolean translucentPanels() {
      return this.translucentPanels;
   }

   public int panelAlpha() {
      return this.translucentPanels && panelDepth == 0 ? 156 : 255;
   }

   public int chromeAlpha() {
      return this.translucentPanels && panelDepth == 0 ? 230 : 255;
   }

   public int panel(int color) {
      return Colors.withAlpha(color, this.panelAlpha());
   }

   public int chrome(int color) {
      return Colors.withAlpha(color, this.chromeAlpha());
   }

   public int control(int tone) {
      if (this.translucentPanels && !CONTROL_LOOK.isEmpty()) {
         float own = (float)Colors.alpha(tone) / 255.0F;
         if (saturated(tone)) {
            return Colors.withAlpha(tone, Math.round(184.0F * own));
         } else {
            int var10000;
            switch (CONTROL_LOOK) {
               case "glass" -> var10000 = this.veil(tone, 1.7F);
               case "smoke" -> var10000 = Colors.withAlpha(tone, Math.round(102.0F * own));
               case "panel" -> var10000 = Colors.withAlpha(tone, Math.round(156.0F * own));
               case "outline" -> var10000 = Colors.withAlpha(tone, Math.round(36.0F * own));
               default -> var10000 = tone;
            }

            return var10000;
         }
      } else {
         return tone;
      }
   }

   public int controlEdge(int tone) {
      if (this.translucentPanels && !CONTROL_LOOK.isEmpty() && !saturated(tone)) {
         float own = (float)Colors.alpha(tone) / 255.0F;
         int var10000;
         switch (CONTROL_LOOK) {
            case "glass" -> var10000 = this.veil(tone, 1.5F);
            case "smoke" -> var10000 = this.veil(tone, 1.4F);
            case "panel" -> var10000 = Colors.withAlpha(tone, Math.round(208.0F * own));
            case "outline" -> var10000 = this.veil(tone, 1.9F);
            default -> var10000 = tone;
         }

         return var10000;
      } else {
         return tone;
      }
   }

   public static void enterRow() {
      ++rowDepth;
   }

   public static void exitRow() {
      if (rowDepth > 0) {
         --rowDepth;
      }

   }

   public static boolean setIn() {
      return rowDepth > 0;
   }

   public int field(int tone) {
      if (!setIn()) {
         return this.control(tone);
      } else {
         return saturated(tone) ? tone : this.veil(tone, 2.6F, 0.06F);
      }
   }

   public int fieldEdge(int tone) {
      return !setIn() ? this.controlEdge(tone) : Colors.withAlpha(tone, Math.round((float)Colors.alpha(tone) * chroma(tone)));
   }

   public int fieldLine(int tone) {
      return Colors.lerp(this.veil(tone, 1.4F), tone, chroma(tone));
   }

   private static float chroma(int c) {
      int max = Math.max(Colors.red(c), Math.max(Colors.green(c), Colors.blue(c)));
      int min = Math.min(Colors.red(c), Math.min(Colors.green(c), Colors.blue(c)));
      return max == 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, ((float)(max - min) / (float)max - 0.1F) / 0.4F));
   }

   private int veil(int tone, float gain, float from) {
      float lo = lightness(this.palette.bg());
      float span = lightness(this.palette.text()) - lo;
      float lift = span == 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, (lightness(tone) - lo) / span));
      return Colors.withAlpha(this.palette.text(), Math.round(Math.min(1.0F, Math.max(0.0F, lift - from) * gain) * (float)Colors.alpha(tone)));
   }

   private int veil(int tone, float gain) {
      return this.veil(tone, gain, 0.0F);
   }

   private static float lightness(int c) {
      return (float)(Colors.red(c) * 299 + Colors.green(c) * 587 + Colors.blue(c) * 114) / 255000.0F;
   }

   private static boolean saturated(int c) {
      int max = Math.max(Colors.red(c), Math.max(Colors.green(c), Colors.blue(c)));
      int min = Math.min(Colors.red(c), Math.min(Colors.green(c), Colors.blue(c)));
      return max > 0 && (float)(max - min) > (float)max * 0.25F;
   }

   public static void enterPanel() {
      ++panelDepth;
   }

   public static void exitPanel() {
      if (panelDepth > 0) {
         --panelDepth;
      }

   }

   public static boolean onPanel() {
      return panelDepth > 0;
   }

   public PixelFont pixelFont() {
      return this.pixelFont;
   }

   public net.minecraft.network.chat.Style headingStyle() {
      return this.headingFont ? net.minecraft.network.chat.Style.EMPTY.withFont(this.pixelFont.id()) : net.minecraft.network.chat.Style.EMPTY;
   }

   public int ms(int base) {
      return Math.round((float)base * this.motion);
   }

   public int bg() {
      return this.palette.bg();
   }

   public int surface() {
      return this.palette.surface();
   }

   public int border() {
      return this.palette.border();
   }

   public int text() {
      return this.palette.text();
   }

   public int muted() {
      return this.palette.textMuted();
   }

   public int accent() {
      return this.palette.accent();
   }

   public int accentInk() {
      return this.palette.accentInk();
   }

   static {
      HEADING_FONT = PixelFont.DEFAULT.id();
      dark = new Theme(new CoreConfig(), Skin.DARK);
      vanilla = new Theme(new CoreConfig(), Skin.VANILLA);
      current = dark;
      globalStyle = Style.CFHGUI;
      activeStyle = Style.CFHGUI;
      LISTENERS = new CopyOnWriteArrayList();
      CONTROL_LOOK = System.getProperty("cfhgui.controlLook", "").trim();
   }
}
