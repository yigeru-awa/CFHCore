package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.realmsclient.RealmsMainScreen;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.WidgetKey;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPose;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildFinder;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Scene;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneLoader;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.Fmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.LastPlayed;
import com.onehumanawa.cfhcore.content.gui.menu.client.ModsScreenOpener;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.TitleScene;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.TitleSet;
import com.onehumanawa.cfhcore.content.gui.menu.client.title.NavButton;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.SplashRendererAccessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class OverhaulTitleScreen extends CFHGuiScreen implements LayoutAware, LayoutBackground.Host {
   public static final ResourceLocation SCENE = CFHGuiMenu.id("title");
   private static boolean introPlayed;
   private static final int VEIL_OUT_MS = 200;
   private static final int VEIL_IN_MS = 520;
   private final LogoRenderer logo;
   private final List<ChestButton> chests;
   private final List<CFHGuiIconButton> corner;
   private final Anim logoIn;
   private final Anim chromeIn;
   private final Anim veil;
   private @Nullable Stage stage;
   private @Nullable ChestButton play;
   private @Nullable ChestButton options;
   private @Nullable String splash;
   private LastPlayed.@Nullable Target continueTarget;
   private @Nullable SceneFetchCard fetchCard;
   private int seenChanges;
   private long cardCheckedMs;
   private TitleScene scene;
   private @Nullable TitleScene pending;
   private boolean ownBackdrop;
   private boolean swapping;
   private static final List<float[]> DEV_CAMERA = devCamera(System.getProperty("cfhgui.titleCamera"));
   private boolean cinematic;
   private boolean skipped;
   private long sceneStartMs;
   private boolean chestless;
   private @Nullable TitleTiles tiles;
   private @Nullable NavButton continueButton;
   private boolean tilesEnter;
   private @Nullable CameraPose restPose;
   private @Nullable Vector3f[] anchors;
   private static final String[] LID_KEYS = new String[]{"cfhgui.overhaul.play#0", "cfhgui.overhaul.options#0"};
   private static final String[] SIGN_KEYS = new String[]{"cfhgui.overhaul.continue#0", "cfhgui.overhaul.quit#0"};
   private static final String LOGO = "logo";
   private static final String FOOTER = "footer";
   private ScreenLayout layout;
   private boolean plain;
   private int plainLeft;
   private int plainTop;
   private int plainBottom;
   private int plainW;
   private int plainCount;
   private int cornerLeft;
   private final AbstractWidget[] tileWidgets;
   private String stageHides;
   private static final int PLAIN_W = 180;
   private float restFov;
   private float restReach;
   private static boolean choicesWritten;
   private static final int TURN_LEFT = 0;
   private static final int TURN_RIGHT = 1;
   private static final int TURN_MIDDLE = 2;
   private static final Map<String, List<float[]>> DEV_CAMERA_BY_SCENE = new HashMap();

   private String chestHides() {
      StringBuilder b = new StringBuilder(4);

      for(int i = 0; i < LID_KEYS.length; ++i) {
         b.append((char)(this.layout.hides(LID_KEYS[i]) ? 'L' : '-')).append((char)(this.layout.hides(SIGN_KEYS[i]) ? 'S' : '-'));
      }

      return b.toString();
   }

   public OverhaulTitleScreen() {
      this((TitleScene)null);
   }

   public OverhaulTitleScreen(@Nullable TitleScene scene) {
      super(Component.translatable("narrator.screen.title"), (Screen)null);
      this.logo = new LogoRenderer(false);
      this.chests = new ArrayList();
      this.corner = new ArrayList();
      this.logoIn = new Anim(0.0F, 700, Ease.OUT_CUBIC);
      this.chromeIn = new Anim(0.0F, 500, Ease.OUT_CUBIC);
      this.veil = new Anim(0.0F, 200, Ease.IN_OUT_CUBIC);
      this.layout = new ScreenLayout();
      this.tileWidgets = new AbstractWidget[4];
      this.stageHides = "";
      this.restFov = 24.1F;
      this.restReach = 0.355F;
      this.showHeader = false;
      this.showBack = false;
      this.scene = scene != null ? scene : TitleScene.of(CFHGuiMenu.config().titleScene);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void build() {
      Minecraft mc = Minecraft.getInstance();
      MenuConfig cfg = CFHGuiMenu.config();
      this.layout = LayoutApplier.current(this);
      boolean wasPlain = this.plain;
      this.plain = this.layout.hasBackground();
      if (this.plain) {
         if (this.stage != null) {
            this.stage.close();
            this.stage = null;
         }

         this.chests.clear();
         this.play = this.options = null;
         this.pending = null;
      } else if (this.stage != null && !this.stage.isClosed()) {
         if (!this.chestHides().equals(this.stageHides)) {
            this.stage.close();
            this.stage = null;
            this.createScene(true);
         }
      } else {
         this.createScene(this.swapping || wasPlain);
      }

      if (this.splash == null && cfg.splash) {
         SplashRenderer s = mc.getSplashManager().getSplash();
         this.splash = s == null ? null : ((SplashRendererAccessor)s).cfhgui$splash();
      }

      this.corner.clear();
      if (this.tiles != null) {
         this.tiles.close();
         this.tiles = null;
      }

      Arrays.fill(this.tileWidgets, null);
      this.continueButton = null;
      this.plainCount = 0;
      if (this.plain) {
         this.buildPlain(mc);
      } else {
         StageWidget view = (StageWidget)this.add(new StageWidget(0, 0, this.width, this.height, this.stage, this.getTitle()));
         if (this.chestless) {
            view.active = false;
            this.buildTiles(mc);
            this.applyContinue();
         }
      }

      this.tilesEnter = false;
      this.fetchCard = null;
      if (!this.plain && this.scene.fetched() && (this.scene.missing() || this.scene.problem() != null)) {
         this.fetchCard = (SceneFetchCard)this.add(this.placeFetchCard());
      }

      this.seenChanges = BuildFinder.changes();
      List<CFHGuiIconButton> small = new ArrayList();
      if (cfg.showModsButton) {
         Optional<Function<Screen, Screen>> mods = ModsScreenOpener.factory();
         mods.ifPresent((f) -> small.add(new CFHGuiIconButton(0, 0, 20, Icon.MODS, Component.translatable("cfhgui.title.mods"), () -> mc.setScreen((Screen)f.apply(this)))));
      }

      small.add(new CFHGuiIconButton(0, 0, 20, Icon.CAMERA, Component.translatable("cfhgui.screenshots.title"), () -> MenuSlots.open("cfhgui:screenshots", this)));
      if (mc.allowsRealms()) {
         small.add(new CFHGuiIconButton(0, 0, 20, Icon.REALMS, Component.translatable("menu.online"), () -> mc.setScreen(new RealmsMainScreen(this))));
      }

      small.add(new CFHGuiIconButton(0, 0, 20, Icon.ACCESSIBILITY, Component.translatable("options.accessibility.title"), () -> mc.setScreen(new AccessibilityOptionsScreen(this, mc.options))));
      small.add(new CFHGuiIconButton(0, 0, 20, Icon.LANGUAGE, Component.translatable("options.language"), () -> mc.setScreen(new LanguageSelectScreen(this, mc.options, mc.getLanguageManager()))));
      int x = this.width - 12 - 20;
      this.cornerLeft = this.width - 12 + 2;

      for(CFHGuiIconButton b : small) {
         String key = WidgetKey.first(b.getMessage());
         if (!this.layout.hides(key)) {
            b.setPosition(x, this.height - 24);
            this.corner(b);
            if (!this.layout.moves(key)) {
               this.cornerLeft = x;
               x -= 22;
            }
         }
      }

   }

   private SceneFetchCard placeFetchCard() {
      int logoH = Math.round(51.0F * this.logoScale());
      int logoRight = (this.width + Math.round(256.0F * this.logoScale())) / 2;
      int logoBottom = this.logoTop(logoH) + logoH;
      int right = this.width - 12;
      int under = 12;
      if (right - (logoRight + 6) >= 132) {
         int w = Math.min(196, right - logoRight - 6);
         return new SceneFetchCard(right - w, under, w, this.height - 28 - under, this.scene);
      } else {
         Rect chest = this.anchors != null && this.restPose != null ? this.projected(this.anchors[1], -0.5F, 0.5F, 0.0F, 1.45F, 0.45F) : null;
         int chestRight = chest != null ? chest.right() : Math.round((float)this.width * 0.68F);
         int beside = right - chestRight - 6;
         if (beside >= 132) {
            int w = Math.min(196, beside);
            int y = Math.max(under, logoBottom + 4);
            return new SceneFetchCard(right - w, y, w, this.height - 28 - y, this.scene);
         } else {
            int y = Math.max(under, logoBottom + 4);
            int lids = Integer.MAX_VALUE;
            if (this.anchors != null && this.restPose != null) {
               for(Vector3f a : this.anchors) {
                  Rect lid = this.projected(a, -0.5F, 0.5F, 0.0F, 1.45F, 0.45F);
                  if (lid != null) {
                     lids = Math.min(lids, lid.y());
                  }
               }
            }

            if (lids == Integer.MAX_VALUE) {
               lids = Math.round((float)this.height * 0.58F);
            }

            int w = Math.min(392, this.width - 24);
            return new SceneFetchCard((this.width - w) / 2, y, w, lids - 4 - y, this.scene);
         }
      }
   }

   private void buildPlain(Minecraft mc) {
      int logoH = Math.round(51.0F * this.logoScale());
      int top = this.logoTop(logoH) + logoH + 14;
      int room = this.height - 30 - top;
      boolean compact = room < 168;
      int rowH = compact ? 20 : 24;
      int gap = compact ? 2 : 4;
      int w = Math.min(180, this.width - 24);
      int x = (this.width - w) / 2;
      NavButton playButton = (new NavButton(x, top, w, Icon.SINGLEPLAYER, Component.translatable("cfhgui.overhaul.play"), () -> mc.setScreen(new SelectWorldScreen(this)))).primary();
      NavButton resume = new NavButton(x, top, w, Icon.PLAY, Component.translatable("cfhgui.overhaul.continue"), this::continuePlaying);
      NavButton settings = new NavButton(x, top, w, Icon.SETTINGS, Component.translatable("cfhgui.overhaul.options"), () -> mc.setScreen(new OptionsScreen(this, mc.options)));
      NavButton leave = (new NavButton(x, top, w, Icon.QUIT, Component.translatable("cfhgui.overhaul.quit"), () -> quit(mc))).danger();
      int y = top;

      for(NavButton b : new NavButton[]{playButton, resume, settings, leave}) {
         String key = WidgetKey.first(b.getMessage());
         if (!this.layout.hides(key)) {
            b.setY(y);
            b.setHeight(rowH);
            this.add(b);
            if (b == resume) {
               this.continueButton = resume;
            }

            if (!this.layout.moves(key)) {
               y += rowH + gap;
               ++this.plainCount;
            }
         }
      }

      this.plainLeft = x;
      this.plainTop = top;
      this.plainW = w;
      this.plainBottom = this.plainCount > 0 ? y - gap : top;
      this.resolveContinue();
   }

   public List<LayoutAware.Part> layoutParts() {
      List<LayoutAware.Part> parts = new ArrayList();
      float s = this.logoScale();
      int logoW = Math.round(256.0F * s);
      int logoH = Math.round(51.0F * s);
      parts.add(new LayoutAware.Part("logo", Component.translatable("cfhgui.layout.part.logo"), Rect.of((this.width - logoW) / 2, this.logoTop(logoH), logoW, logoH)));
      parts.add(new LayoutAware.Part("footer", Component.translatable("cfhgui.layout.part.footer"), Rect.of(12, this.height - 12, Math.max(1, this.cornerLeft - 8 - 12), 10)));
      if (this.plain) {
         return parts;
      } else {
         Component[] lids = new Component[]{Component.translatable("cfhgui.overhaul.play"), Component.translatable("cfhgui.overhaul.options")};
         Component[] signs = new Component[]{Component.translatable("cfhgui.overhaul.continue"), Component.translatable("cfhgui.overhaul.quit")};

         for(int i = 0; i < 2; ++i) {
            Rect lid = null;
            Rect sign = null;
            if (this.chestless) {
               if (this.tileWidgets[i] != null) {
                  lid = rectOf(this.tileWidgets[i]);
               }

               if (this.tileWidgets[2 + i] != null) {
                  sign = rectOf(this.tileWidgets[2 + i]);
               }
            } else if (this.anchors != null && this.restPose != null) {
               lid = this.projected(this.anchors[i], -0.44F, 0.44F, 0.6F, 1.45F, -0.3F);
               sign = this.projected(this.anchors[i], -0.38F, 0.38F, 0.05F, 0.5F, 0.45F);
            }

            parts.add(new LayoutAware.Part(LID_KEYS[i], lids[i], lid));
            parts.add(new LayoutAware.Part(SIGN_KEYS[i], signs[i], sign));
         }

         return parts;
      }
   }

   private static Rect rectOf(AbstractWidget w) {
      return Rect.of(w.getX(), w.getY(), w.getWidth(), w.getHeight());
   }

   private @Nullable Rect projected(Vector3f a, float x0, float x1, float y0, float y1, float z) {
      CameraPose r = this.restPose;
      if (r == null) {
         return null;
      } else {
         float aspect = (float)this.width / (float)Math.max(1, this.height);
         Matrix4f view = (new Matrix4f()).perspective((float)Math.toRadians((double)this.fovFor(aspect)), aspect, 0.05F, 400.0F).lookAt(r.position.x, r.position.y, r.position.z, r.target.x, r.target.y, r.target.z, 0.0F, 1.0F, 0.0F);
         Vector4f p = new Vector4f();
         float minX = Float.MAX_VALUE;
         float minY = Float.MAX_VALUE;
         float maxX = -Float.MAX_VALUE;
         float maxY = -Float.MAX_VALUE;

         for(float dx : new float[]{x0, x1}) {
            for(float dy : new float[]{y0, y1}) {
               if (!this.onScreen(view, a.x + dx, a.y + dy, a.z + z, p)) {
                  return null;
               }

               minX = Math.min(minX, p.x);
               maxX = Math.max(maxX, p.x);
               minY = Math.min(minY, p.y);
               maxY = Math.max(maxY, p.y);
            }
         }

         return Rect.of(Math.round(minX), Math.round(minY), Math.max(1, Math.round(maxX - minX)), Math.max(1, Math.round(maxY - minY)));
      }
   }

   static Vector3f middle(Scene scene) {
      Anchor profile = scene.anchorOr("chest_profile", -1.0F, 0.0F, 0.0F, 0.0F);
      Anchor play = scene.anchorOr("chest_play", 0.0F, 0.0F, 0.0F, 0.0F);
      Anchor options = scene.anchorOr("chest_options", 1.0F, 0.0F, 0.0F, 0.0F);
      return (new Vector3f(profile.position())).add(play.position()).add(options.position()).div(3.0F);
   }

   private void corner(CFHGuiIconButton button) {
      this.corner.add((CFHGuiIconButton)this.add(button));
   }

   private void buildTiles(Minecraft mc) {
      float[] middles = new float[2];
      float top = Float.MAX_VALUE;
      float bottom = -Float.MAX_VALUE;
      boolean seen = this.restPose != null && this.anchors != null;
      if (seen) {
         float aspect = (float)this.width / (float)Math.max(1, this.height);
         CameraPose r = this.restPose;
         Matrix4f view = (new Matrix4f()).perspective((float)Math.toRadians((double)this.fovFor(aspect)), aspect, 0.05F, 400.0F).lookAt(r.position.x, r.position.y, r.position.z, r.target.x, r.target.y, r.target.z, 0.0F, 1.0F, 0.0F);
         Vector4f p = new Vector4f();

         for(int i = 0; i < 2 && seen; ++i) {
            Vector3f a = this.anchors[i];
            seen = this.onScreen(view, a.x, a.y + 0.6F, a.z, p);
            middles[i] = p.x;
            seen = seen && this.onScreen(view, a.x, a.y + 1.45F, a.z - 0.3F, p);
            top = Math.min(top, p.y);
            seen = seen && this.onScreen(view, a.x, a.y, a.z + 0.45F, p);
            bottom = Math.max(bottom, p.y);
         }

         seen = seen && middles[0] < middles[1];
      }

      if (!seen) {
         middles[0] = (float)this.width * 0.36F;
         middles[1] = (float)this.width * 0.64F;
         top = (float)this.height * 0.3F;
         bottom = (float)this.height * 0.84F;
      }

      int logoH = Math.round(51.0F * this.logoScale());
      int areaTop = Math.max(this.logoTop(logoH) + logoH + 8, Math.round(top));
      int areaBottom = Math.min(this.height - 28, Math.round(bottom));
      int spacing = Math.round(middles[1] - middles[0]);
      int tileW = Math.max(72, Math.min(Mth.clamp(spacing - 8, 72, 152), (this.width - 24 - 12 - 16) / 2));
      int buttonH = areaBottom - areaTop >= 170 ? 24 : 20;
      int room = areaBottom - areaTop - 12 - 5 - buttonH;
      int tileH = Math.max(48, Math.min(Math.round((float)tileW * 1.02F), room));
      int plateH = 12 + tileH + 5 + buttonH;
      int left = Math.round(middles[0] - (float)tileW / 2.0F) - 6;
      int right = Math.round(middles[1] + (float)tileW / 2.0F) + 6;
      int plateY = areaTop + Math.max(0, (areaBottom - areaTop - plateH) / 2);
      int[] relX = new int[2];

      for(int i = 0; i < 2; ++i) {
         relX[i] = Math.round(middles[i] - (float)tileW / 2.0F) - left;
      }

      TitleTiles t = new TitleTiles(this, left, plateY, right - left, plateH);
      TitleTile playTile = (new TitleTile(0, 0, tileW, tileH, Component.translatable("cfhgui.overhaul.play"), () -> mc.setScreen(new SelectWorldScreen(this)))).primary();
      TitleTile optionsTile = new TitleTile(0, 0, tileW, tileH, Component.translatable("cfhgui.overhaul.options"), () -> mc.setScreen(new OptionsScreen(this, mc.options)));
      if (!this.layout.hides(LID_KEYS[0])) {
         this.tileWidgets[0] = t.tile(playTile, relX[0], 6, (holder) -> TitleThings.play(t.stage(), holder, false));
      }

      if (!this.layout.hides(LID_KEYS[1])) {
         this.tileWidgets[1] = t.tile(optionsTile, relX[1], 6, (holder) -> TitleThings.cog(t.stage(), holder, false));
      }

      NavButton resume = (new NavButton(0, 0, tileW, Icon.PLAY, Component.translatable("cfhgui.overhaul.continue"), this::continuePlaying)).centred();
      NavButton leave = (new NavButton(0, 0, tileW, Icon.QUIT, Component.translatable("cfhgui.overhaul.quit"), () -> quit(mc))).danger().centred();
      NavButton[] under = new NavButton[]{resume, leave};

      for(int i = 0; i < 2; ++i) {
         if (!this.layout.hides(SIGN_KEYS[i])) {
            under[i].setHeight(buttonH);
            if (under[i] != resume && under[i].iconOnly(tileW - 14 - 24) && !under[i].isLocked()) {
               under[i].tip(under[i].getMessage());
            }

            t.add(under[i], relX[i], 6 + tileH + 5);
            this.tileWidgets[2 + i] = under[i];
         }
      }

      this.continueButton = this.layout.hides(SIGN_KEYS[0]) ? null : resume;
      this.tiles = (TitleTiles)this.add(t);
      if (this.tilesEnter) {
         t.comeIn(Math.max(0, Math.round(this.introFirstMs()) - 120));
         TitleTile[] tile = new TitleTile[]{playTile, optionsTile};
         int[] turn = new int[]{2, 1};

         for(int i = 0; i < 2; ++i) {
            int at = Math.round(this.openAtMs(turn[i]));
            tile[i].playEntrance(at);
            under[i].playEntrance(at + 160);
         }
      }

   }

   private boolean onScreen(Matrix4f view, float x, float y, float z, Vector4f out) {
      out.set(x, y, z, 1.0F);
      view.transform(out);
      if (out.w <= 1.0E-4F) {
         return false;
      } else {
         out.set((out.x / out.w + 1.0F) * 0.5F * (float)this.width, (1.0F - out.y / out.w) * 0.5F * (float)this.height, 0.0F, 1.0F);
         return true;
      }
   }

   private float fovFor(float aspect) {
      return Math.max(this.restFov, (float)Math.toDegrees((double)2.0F * Math.atan((double)(this.restReach / Math.max(0.5F, aspect)))));
   }

   private static boolean customScene() {
      return Files.isRegularFile(SceneLoader.configDir().resolve(SceneLoader.configName(SCENE, "json")), new LinkOption[0]);
   }

   static Dressed dress(Stage s, TitleScene which) {
      boolean custom = customScene();
      Scene scene = SceneLoader.load(custom ? SCENE : which.sceneId());
      scene.apply(s);
      s.finish(StageFinish.cinematic());
      s.soft(true);
      s.stylize(TitleStyle.of(CFHGuiMenu.config().titleTextures) == TitleStyle.STYLIZED ? 1.0F : 0.0F);
      Vector3f middle = middle(scene);
      s.lighting().key(middle.x + 2.4F, middle.y + 4.6F, middle.z + 5.8F);
      TitleSet set = !custom && scene.structure() == null ? which.create() : null;
      if (set != null) {
         try {
            set.build(s, scene, 20260930L + (long)which.ordinal());
         } catch (RuntimeException e) {
            CFHGui.LOGGER.error("[CFHGui] title scene {} could not be built", which.id(), e);
         }
      }

      return new Dressed(scene, set);
   }

   private static void writeChoices() {
      if (!choicesWritten) {
         choicesWritten = true;

         try {
            Path file = CFHGuiMenu.configFile().file();
            if (Files.isRegularFile(file, new LinkOption[0]) && !Files.readString(file).contains("\"titlePlayIcon\"")) {
               CFHGuiMenu.configFile().save();
            }
         } catch (RuntimeException | IOException var1) {
         }

      }
   }

   private void createScene(boolean swap) {
      Minecraft mc = Minecraft.getInstance();
      Theme theme = Theme.current();
      writeChoices();
      this.chests.clear();
      this.skipped = false;
      this.cinematic = !swap && (!introPlayed || DevHarness.replayIntros()) && theme.motion() > 0.0F;
      introPlayed = true;
      if (!swap) {
         this.sceneStartMs = Clock.nowMs();
      }

      Stage s = (new Stage()).bind(this);
      this.stage = s;
      Dressed dressed = dress(s, this.scene);
      Scene scene = dressed.scene();
      TitleSet set = dressed.set();
      s.focusOutline(true);
      s.resolutionScale(mc.getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      if (!this.cinematic) {
         s.camera().timeline().skip(s.camera().pose());
      }

      MenuConfig cfg = CFHGuiMenu.config();
      this.chestless = !cfg.titleChests;
      ChestButton.Look look = TitleStyle.of(cfg.titleTextures) == TitleStyle.DEFAULT ? ChestButton.Look.VANILLA : ChestButton.Look.PAINTED;
      Anchor homePlay = scene.anchorOr("chest_play", 0.0F, 0.0F, 0.0F, 0.0F);
      Anchor homeOptions = scene.anchorOr("chest_options", 1.0F, 0.0F, 0.0F, 0.0F);
      Vector3f middle = middle(scene);
      float span = homeOptions.x() - homePlay.x();
      Anchor playAt = homePlay.offset(middle.x - span / 2.0F - homePlay.x(), 0.0F, 0.0F);
      Anchor optionsAt = homeOptions.offset(middle.x + span / 2.0F - homeOptions.x(), 0.0F, 0.0F);
      this.ownBackdrop = set != null || scene.structure() != null;
      if (!this.ownBackdrop && theme.isVanilla() && CFHGuiMenu.config().panorama) {
         s.background(0);
      }

      boolean[] lidShown = new boolean[2];
      boolean[] signShown = new boolean[2];

      for(int i = 0; i < 2; ++i) {
         lidShown[i] = !this.layout.hides(LID_KEYS[i]);
         signShown[i] = !this.layout.hides(SIGN_KEYS[i]);
      }

      Anchor[] at = new Anchor[]{playAt, optionsAt};
      if (!this.ownBackdrop) {
         ((FxNode)s.add(FxNode.glow(16.0F, -13948377).fade(2.6F))).at(middle.x, middle.y, middle.z + 0.5F);
      }

      int poolColor = set != null ? set.pool() : 788523464;
      if (poolColor != 0) {
         int pool = Colors.withAlpha(Colors.lerp(poolColor | -16777216, theme.accent(), 0.25F), Colors.alpha(poolColor));

         for(int i = 0; i < at.length; ++i) {
            if (lidShown[i] || signShown[i]) {
               ChestButton.pool(s, at[i], pool, !this.chestless);
            }
         }
      }

      int dust = set != null ? set.dust() : 1728047042;
      if (dust != 0) {
         ((MotesNode)s.add(new MotesNode(54, 5.2F, 2.6F, 2.6F, 0.02F, dust, 4L))).at(middle.x, middle.y + 0.1F, middle.z + 0.4F);
      }

      this.restFov = scene.camera().rest().fov;
      this.restPose = scene.camera().rest().copy();
      this.restReach = 2.34F / Math.max(1.0F, this.restPose.position.distance(middle));
      this.anchors = new Vector3f[]{new Vector3f(playAt.position()), new Vector3f(optionsAt.position())};
      this.tilesEnter = this.cinematic;
      this.stageHides = this.chestHides();
      if (!this.chestless) {
         this.play = !lidShown[0] && !signShown[0] ? null : new ChestButton(s, playAt, 0, 2, this.openAtMs(2), this.cinematic, look, Component.translatable("cfhgui.overhaul.play"), () -> mc.setScreen(new SelectWorldScreen(this)), (String)null, Component.translatable("cfhgui.overhaul.continue"), this::continuePlaying, (String)null, lidShown[0], signShown[0]);
         this.options = !lidShown[1] && !signShown[1] ? null : new ChestButton(s, optionsAt, 1, 1, this.openAtMs(1), this.cinematic, look, Component.translatable("cfhgui.overhaul.options"), () -> mc.setScreen(new OptionsScreen(this, mc.options)), (String)null, Component.translatable("cfhgui.overhaul.quit"), () -> quit(mc), (String)null, lidShown[1], signShown[1]);
         if (this.play != null) {
            this.chests.add(this.play);
         }

         if (this.options != null) {
            this.chests.add(this.options);
         }

         for(ChestButton chest : this.chests) {
            chest.seenFrom(scene.camera().rest().position);
         }

         if (this.play != null && lidShown[0]) {
            this.play.onLidHover(TitleThings.play(s, (PivotNode)this.play.show(s, new PivotNode()), true));
         }

         if (this.options != null && lidShown[1]) {
            this.options.onLidHover(TitleThings.cog(s, (PivotNode)this.options.show(s, new PivotNode()), this.options.lidLocked()));
         }
      } else {
         this.play = null;
         this.options = null;
      }

      if (!swap && this.cinematic) {
         this.logoIn.snap(0.0F);
         this.chromeIn.snap(0.0F);
      } else {
         for(ChestButton c : this.chests) {
            c.finish();
         }

         if (!swap) {
            this.logoIn.snap(1.0F);
            this.chromeIn.snap(1.0F);
         }
      }

      this.resolveContinue();
   }

   private void swapNow() {
      TitleScene next = this.pending;
      this.pending = null;
      if (next != null) {
         this.scene = next;
         if (this.stage != null) {
            this.stage.close();
            this.stage = null;
         }

         this.swapping = true;

         try {
            this.rebuildWidgets();
         } finally {
            this.swapping = false;
         }

         if (Theme.current().motion() > 0.0F) {
            this.veil.set(0.0F, 520);
         } else {
            this.veil.snap(0.0F);
         }

      }
   }

   public TitleScene scene() {
      return this.scene;
   }

   void buildArrived(TitleScene arrived) {
      CFHGuiMenu.configFile().update((c) -> c.titleScene = arrived.id());
      if (!this.plain && !customScene() && arrived.available()) {
         this.pending = arrived;
         if (Theme.current().motion() <= 0.0F) {
            this.swapNow();
         } else {
            this.veil.set(1.0F, 200);
         }

      }
   }

   public void onFilesDrop(List<Path> files) {
      SceneFetch.dropped(files);
   }

   private void resolveContinue() {
      Stage asked = this.stage;
      this.continueTarget = (LastPlayed.Target)LastPlayed.quick().orElse(null);
      this.applyContinue();
      LastPlayed.resolve((found) -> {
         if (asked == this.stage) {
            this.continueTarget = (LastPlayed.Target)found.orElse(null);
            this.applyContinue();
         }
      });
   }

   private void applyContinue() {
      LastPlayed.Target t = this.continueTarget;
      Component tip;
      if (t == null) {
         tip = Component.translatable("cfhgui.title.nothing_yet_hint");
      } else {
         String name = t.name() != null && !t.name().isBlank() ? t.name() : t.id();
         tip = Component.translatable(t.kind() == LastPlayed.Kind.WORLD ? "cfhgui.overhaul.continue.world" : "cfhgui.overhaul.continue.server", new Object[]{name, Fmt.ago(t.at())});
      }

      if (this.play != null) {
         if (t == null) {
            this.play.disableSign(tip);
         } else {
            this.play.enableSign(this::continuePlaying);
            this.play.signTip(tip);
         }
      }

      if (this.continueButton != null) {
         this.continueButton.enabled(t != null);
         this.continueButton.tip(tip);
      }

   }

   private void continuePlaying() {
      if (this.continueTarget != null) {
         LastPlayed.play(this.continueTarget, this);
      }

   }

   private static void quit(Minecraft mc) {
      if (!CFHGuiMenu.config().confirmQuitGame) {
         mc.stop();
      } else {
         MutableComponent var10000 = Component.translatable("menu.quit");
         MutableComponent var10001 = Component.translatable("cfhgui.title.confirm_quit");
         MutableComponent var10002 = Component.translatable("menu.quit");
         Objects.requireNonNull(mc);
         CFHGuiModal.confirmDanger(var10000, var10001, var10002, mc::stop);
      }
   }

   private float introMs() {
      float motion = Theme.current().motion();
      return motion <= 0.0F ? Float.MAX_VALUE : (float)(Clock.nowMs() - this.sceneStartMs) / motion;
   }

   private float introFirstMs() {
      return this.cinematic ? 2350.0F : 80.0F;
   }

   private float introStepMs() {
      return this.cinematic ? 390.0F : 110.0F;
   }

   private float openAtMs(int turn) {
      return this.introFirstMs() + this.introStepMs() * (turn == 0 ? 0.0F : (turn == 1 ? 0.6F : 1.8F));
   }

   private boolean introRunning() {
      if (!this.skipped && this.stage != null) {
         if (this.stage.camera().timeline().playing()) {
            return true;
         } else {
            for(ChestButton c : this.chests) {
               if (!c.settled()) {
                  return true;
               }
            }

            return this.tiles != null && this.tiles.entering();
         }
      } else {
         return false;
      }
   }

   private boolean skipIntro() {
      if (this.introRunning() && this.stage != null) {
         this.skipped = true;
         this.stage.camera().timeline().skip(this.stage.camera().pose());

         for(ChestButton c : this.chests) {
            c.finish();
         }

         if (this.tiles != null) {
            this.tiles.comeInNow();
         }

         this.logoIn.snap(1.0F);
         this.chromeIn.snap(1.0F);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.cinematic && this.stage != null && this.stage.camera().timeline().playing() && this.skipIntro() ? true : super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return keyCode == 258 || !this.skipIntro() || keyCode != 256 && keyCode != 32 && keyCode != 257 && keyCode != 335 ? super.keyPressed(keyCode, scanCode, modifiers) : true;
   }

   public void removed() {
      super.removed();
      if (this.stage != null) {
         this.stage.close();
         this.stage = null;
      }

      if (this.tiles != null) {
         this.tiles.close();
         this.tiles = null;
      }

   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      if (this.plain) {
         if (!LayoutBackground.drawOwn(this, g, partialTick, PANORAMA)) {
            g.fill(0, 0, this.width, this.height, t.isVanilla() ? -15724528 : t.bg());
         }

         if (!t.isVanilla() && this.plainCount > 0) {
            CFHGuiDraw.floatingPanel(g, this.plainLeft - 6, this.plainTop - 6, this.plainW + 12, this.plainBottom - this.plainTop + 12, 1.0F);
         }

      } else {
         if (!this.ownBackdrop && t.isVanilla() && CFHGuiMenu.config().panorama) {
            PANORAMA.render(g, this.width, this.height, 1.0F, partialTick);
            g.fill(0, 0, this.width, this.height, 1711276032);
            CFHGuiDraw.vignette(g, 0, 0, this.width, this.height, 0.4F);
         } else {
            g.fill(0, 0, this.width, this.height, t.isVanilla() ? -15724528 : t.bg());
         }

      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.pending != null && this.veil.get() >= 0.999F) {
         this.swapNow();
      }

      if (this.stage != null) {
         this.stage.alpha(1.0F - this.veil.get());
      }

      float ms = this.introMs();
      if (ms >= (this.cinematic ? 1500.0F : 0.0F)) {
         this.logoIn.set(1.0F);
      }

      if (ms >= (this.cinematic ? 3300.0F : 250.0F)) {
         this.chromeIn.set(1.0F);
      }

      float ci = this.chromeIn.get();

      for(CFHGuiIconButton b : this.corner) {
         b.setAlpha(ci);
         b.active = ci > 0.5F;
      }

      if (this.fetchCard != null) {
         this.fetchCard.setAlpha(ci);
         this.fetchCard.active = ci > 0.5F;
      }

      int changes = BuildFinder.changes();
      if (changes != this.seenChanges) {
         this.seenChanges = changes;
         SceneFetch.recheck();
      }

      long nowMs = Util.getMillis();
      if (nowMs - this.cardCheckedMs > 500L && this.pending == null && !this.swapping) {
         this.cardCheckedMs = nowMs;
         boolean broken = this.scene.fetched() && this.scene.problem() != null;
         boolean wants = this.scene.fetched() && (broken || this.scene.missing());
         if (wants != (this.fetchCard != null)) {
            if (broken) {
               this.pending = this.scene;
               if (Theme.current().motion() <= 0.0F) {
                  this.swapNow();
               } else {
                  this.veil.set(1.0F, 200);
               }
            } else {
               this.rebuildWidgets();
            }
         }
      }

      if (this.stage != null && !this.stage.camera().timeline().playing() && this.height > 0) {
         this.stage.camera().fov(this.fovFor((float)this.width / (float)this.height));
      }

      if (!this.devCamera().isEmpty() && this.stage != null) {
         this.holdDevCamera(this.stage);
      }

      super.render(g, mouseX, mouseY, partialTick);
   }

   private List<float[]> devCamera() {
      List<float[]> own = (List)DEV_CAMERA_BY_SCENE.computeIfAbsent(this.scene.id(), (id) -> devCamera(System.getProperty("cfhgui.titleCamera." + id)));
      return own.isEmpty() ? DEV_CAMERA : own;
   }

   private void holdDevCamera(Stage s) {
      List<float[]> poses = this.devCamera();
      long every = Math.max(200L, Long.getLong("cfhgui.titleCameraMs", 2000L));
      float[] p = (float[])poses.get((int)((Clock.nowMs() - this.sceneStartMs) / every % (long)poses.size()));
      if (s.camera().timeline().playing()) {
         s.camera().timeline().skip(s.camera().pose());
      }

      s.camera().idle((IdleMotion)null);
      s.camera().at(p[0], p[1], p[2]).lookAt(p[3], p[4], p[5]).fov(p[6]);
      this.logoIn.snap(0.0F);
      this.chromeIn.snap(0.0F);

      for(CFHGuiIconButton b : this.corner) {
         b.setAlpha(0.0F);
      }

      if (this.fetchCard != null) {
         this.fetchCard.setAlpha(0.0F);
      }

      if (this.tiles != null) {
         this.tiles.visible = false;
      }

   }

   private static List<float[]> devCamera(@Nullable String spec) {
      List<float[]> poses = new ArrayList();
      if (spec != null && !spec.isBlank()) {
         for(String pose : spec.split(";")) {
            String[] v = pose.split("[,:]");
            if (v.length >= 7) {
               try {
                  float[] p = new float[7];

                  for(int i = 0; i < 7; ++i) {
                     p[i] = Float.parseFloat(v[i].trim());
                  }

                  poses.add(p);
               } catch (NumberFormatException var9) {
               }
            }
         }

         return List.copyOf(poses);
      } else {
         return poses;
      }
   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      float li = this.logoIn.get();
      float scale = this.logoScale();
      int logoH = Math.round(51.0F * scale);
      int logoY = this.logoTop(logoH);
      if (li > 0.004F && !this.layout.hides("logo")) {
         g.pose().pushPose();
         float s = scale * (1.0F + 0.06F * (1.0F - li));
         g.pose().translate((float)this.width / 2.0F, (float)logoY + (float)logoH / 2.0F + (1.0F - li) * -6.0F, 0.0F);
         g.pose().scale(s, s, 1.0F);
         g.pose().translate((float)(-this.width) / 2.0F, -25.5F, 0.0F);
         if (CFHGuiMenu.config().showLogo) {
            this.logo.renderLogo(g, this.width, li, 0);
         } else {
            Palette p = t.palette();
            g.pose().pushPose();
            g.pose().translate((float)this.width / 2.0F, 8.0F, 0.0F);
            g.pose().scale(3.0F, 3.0F, 1.0F);
            Component word = Fonts.heading(Component.literal("CFHGui"));
            g.drawString(this.font, word, -this.font.width(word) / 2, 0, Colors.scaleAlpha(p.text(), li), t.isVanilla());
            g.pose().popPose();
         }

         if (this.splash != null && !this.splash.isEmpty() && t.isVanilla()) {
            this.renderSplash(g, li);
         }

         g.pose().popPose();
         RenderSystem.enableBlend();
      }

      float ci = this.chromeIn.get();
      if (ci > 0.004F && CFHGuiMenu.config().showFooter && !this.layout.hides("footer")) {
         int fg = Colors.scaleAlpha(t.isVanilla() ? -1 : t.palette().textDim(), ci * (t.isVanilla() ? 0.85F : 1.0F));
         String var10000 = SharedConstants.getCurrentVersion().getName();
         Component version = Component.literal("Minecraft " + var10000 + " · " + CFHGuiPlatform.get().loader().displayName + " · " + CFHGuiPlatform.get().allMods().size() + " mods · CFHGui " + CFHGui.version());
         int y = this.height - 11;
         var10000 = SharedConstants.getCurrentVersion().getName();
         Component brief = Component.literal(var10000 + " · " + CFHGuiPlatform.get().loader().displayName);
         int right = this.cornerLeft - 8;
         int room = right - 12;
         g.drawString(this.font, this.font.width(version) <= room ? version : brief, 12, y, fg, t.isVanilla());
      }

   }

   private float logoScale() {
      float byWidth = (float)this.width * 0.51F / 256.0F;
      float byHeight = (float)this.height * 0.22F / 51.0F;
      return Mth.clamp(Math.min(byWidth, byHeight), 0.7F, 4.0F);
   }

   private int logoTop(int logoH) {
      return Math.max(4, Math.round((float)this.height * 0.16F - (float)logoH / 2.0F));
   }

   private void renderSplash(GuiGraphics g, float alpha) {
      Theme t = Theme.current();
      g.pose().pushPose();
      g.pose().translate((float)this.width / 2.0F + 123.0F, 39.0F, 0.0F);
      g.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      float s = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * ((float)Math.PI * 2F)) * 0.1F);
      s = s * 100.0F / (float)(this.font.width(this.splash) + 32);
      g.pose().scale(s, s, s);
      g.drawCenteredString(this.font, this.splash, 0, -8, Colors.scaleAlpha(t.isVanilla() ? -256 : t.accentInk(), alpha));
      g.pose().popPose();
   }

   static record Dressed(Scene scene, @Nullable TitleSet set) {
   }
}
