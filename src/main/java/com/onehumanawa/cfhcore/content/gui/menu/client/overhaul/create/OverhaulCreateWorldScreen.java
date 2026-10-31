package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTabStrip;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.CreateWorldScreenAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

public final class OverhaulCreateWorldScreen extends CFHGuiScreen {
   private static final Map<CreateWorldScreen, Session> SESSIONS = new WeakHashMap();
   private static final Component[] TABS = new Component[]{Component.translatable("createWorld.tab.game.title"), Component.translatable("createWorld.tab.world.title"), Component.translatable("createWorld.tab.more.title")};
   private static final float SCALE = 0.083333336F;
   private static final int FOOTER_H = 30;
   private static final int BAR_H = 20;
   private static final int CHIP_H = 15;
   private static final int HAZE = -2119036;
   private static final Aerial.Margins MARGINS = new Aerial.Margins(0.8F, 0.7F, 1.1F);
   private static final int SHELF = 10;
   private static final float EDGE_FROM = 0.94F;
   private static final float EDGE_ROUND = 16.0F;
   private final CreateWorldScreen vanilla;
   private final WorldCreationUiState state;
   private final Session session;
   private final boolean withView;
   private @Nullable Stage stage;
   private @Nullable TerrainNode terrain;
   private boolean closed;
   private long changedAt;
   private Rect band = new Rect(0, 0, 0, 0);
   private Rect full = new Rect(0, 0, 0, 0);
   private final Anim viewIn;
   private final Anim grow;
   private boolean formShown;
   private final List<Runnable> refreshers;
   private WorldCreationUiState.@Nullable WorldTypeEntry builtFor;
   private boolean builtEditor;
   private @Nullable CFHGuiTextField seedField;
   private @Nullable StageWidget view;
   private @Nullable CFHGuiIconButton reroll;
   private @Nullable CFHGuiIconButton resize;
   private @Nullable CFHGuiIconButton fold;
   private @Nullable CFHGuiIconButton narrower;
   private @Nullable CFHGuiIconButton wider;
   private float worstMs;

   public OverhaulCreateWorldScreen(CreateWorldScreen vanilla, boolean withView) {
      super(Component.translatable("selectWorld.create"), ((CreateWorldScreenAccess)vanilla).cfhgui$lastScreen());
      this.viewIn = new Anim(0.0F, 500, Ease.OUT_CUBIC);
      this.grow = new Anim(0.0F, 340, Ease.OUT_CUBIC);
      this.refreshers = new ArrayList();
      this.vanilla = vanilla;
      this.state = vanilla.getUiState();
      this.withView = withView;
      synchronized(SESSIONS) {
         this.session = (Session)SESSIONS.computeIfAbsent(vanilla, (v) -> new Session());
      }

      this.grow.snap(this.session.maximised ? 1.0F : 0.0F);
      this.state.addListener((s) -> {
         if (!this.closed) {
            this.changedAt = Clock.nowMs();
         }

      });
   }

   public Component getTitle() {
      String var10000 = super.getTitle().getString();
      return Component.literal(var10000 + " – " + TABS[Mth.clamp(this.session.tab, 0, TABS.length - 1)].getString());
   }

   public void back() {
      Popups.closeAll();
      this.vanilla.popScreen();
   }

   private boolean minimised() {
      return CFHGuiMenu.config().createPreviewMinimised;
   }

   private static WorldPreview.Span span() {
      return WorldPreview.Span.of(CFHGuiMenu.config().createPreviewSpan);
   }

   private boolean viewShown() {
      return this.withView && !this.minimised();
   }

   protected void build() {
      Minecraft mc = Minecraft.getInstance();
      this.vanilla.init(mc, this.width, this.height);
      this.closed = false;
      this.refreshers.clear();
      this.seedField = null;
      this.view = null;
      this.reroll = this.resize = this.fold = this.narrower = this.wider = null;
      int x = 12;
      int w = this.width - 24;
      int footerTop = this.height - 30;
      int y = 38;
      this.formShown = true;
      if (this.withView) {
         if (this.minimised()) {
            this.session.maximised = false;
            this.grow.snap(0.0F);
            this.add((new CFHGuiButton(12, y, w, 20, Component.translatable("cfhgui.create.preview"), this::toggleView)).variant(Variant.SECONDARY).icon(Icon.MAP).leftAligned().tip(Component.translatable("cfhgui.create.preview.expand")));
            if (this.stage != null) {
               this.stage.close();
               this.stage = null;
               this.terrain = null;
            }

            this.session.preview.show((TerrainNode)null);
            y += 26;
         } else {
            int h = Mth.clamp(Math.round((float)this.height * (this.height < 300 ? 0.29F : 0.36F)), 56, Math.max(56, Math.round((float)w * 0.42F)));
            this.band = new Rect(12, y, w, h);
            this.full = new Rect(12, y, w, Math.max(h, footerTop - 4 - y));
            if (this.stage == null || this.stage.isClosed()) {
               this.createScene();
            }

            this.addRenderableOnly(this::renderPlate);
            this.view = (StageWidget)this.addRenderableOnly(new StageWidget(13, y + 1, w - 2, h - 2, this.stage, Component.translatable("cfhgui.create.preview")));
            this.addRenderableOnly(this::renderChips);
            this.reroll = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 18, Icon.REFRESH, Component.translatable("cfhgui.create.reroll"), this::reroll));
            this.resize = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 18, this.session.maximised ? Icon.WINDOWED : Icon.FULLSCREEN, Component.translatable(this.session.maximised ? "cfhgui.create.preview.restore" : "cfhgui.create.preview.maximise"), this::toggleMaximised));
            if (!this.session.maximised) {
               this.fold = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 18, Icon.CHEVRON_UP, Component.translatable("cfhgui.create.preview.minimise"), this::toggleView));
            }

            this.narrower = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 13, Icon.MINUS, Component.translatable("cfhgui.create.preview.narrower"), () -> this.changeSpan(-1)));
            this.wider = (CFHGuiIconButton)this.add(new CFHGuiIconButton(0, 0, 13, Icon.PLUS, Component.translatable("cfhgui.create.preview.wider"), () -> this.changeSpan(1)));
            this.layoutView();
            this.formShown = !this.session.maximised && this.grow.get() <= 0.001F;
            y += h + 7;
         }

         if (this.formShown) {
            this.add(new HangingTabs(12, y, w, Arrays.asList(TABS), this.session.tab, this::selectTab));
            y += 27;
         }
      } else {
         List<CFHGuiTabStrip.Tab> tabs = new ArrayList();

         for(Component c : TABS) {
            tabs.add(new CFHGuiTabStrip.Tab(c));
         }

         CFHGuiTabStrip strip = (CFHGuiTabStrip)this.add(new CFHGuiTabStrip(12, y - 2, w, tabs, this.session.tab, this::selectTab));
         y += strip.getHeight() + 4;
      }

      if (this.formShown) {
         CFHGuiScrollPanel panel = new CFHGuiScrollPanel(12, y, w + 8, Math.max(24, footerTop - y - 2));
         this.fill(panel, this.session.tab);
         this.add(panel);
      }

      int by = this.height - 12 + 2 - 20;
      int createW = Math.min(150, Math.max(96, w / 3));
      int cancelW = Math.min(90, Math.max(56, w / 5));
      this.add((new CFHGuiButton(12 + w - createW, by, createW, Component.translatable("selectWorld.create"), this::create)).variant(Variant.PRIMARY).icon(Icon.NEW_WORLD));
      this.add(new CFHGuiButton(12 + w - createW - 6 - cancelW, by, cancelW, CommonComponents.GUI_CANCEL, this::back));
      this.builtFor = this.state.getWorldType();
      this.builtEditor = this.state.getPresetEditor() != null;
      if (this.viewShown()) {
         this.session.preview.update(this.state.getSettings(), span());
      }

   }

   private void selectTab(int tab) {
      if (tab != this.session.tab) {
         this.session.tab = tab;
         this.rebuildWidgets();
      }
   }

   private void toggleView() {
      CFHGuiMenu.configFile().update((c) -> c.createPreviewMinimised = !c.createPreviewMinimised);
      this.viewIn.snap(0.0F);
      this.rebuildWidgets();
   }

   private void toggleMaximised() {
      Popups.closeAll();
      this.session.maximised = !this.session.maximised;
      this.grow.set(this.session.maximised ? 1.0F : 0.0F);
      this.rebuildWidgets();
   }

   private void changeSpan(int delta) {
      WorldPreview.Span now = span();
      WorldPreview.Span next = delta > 0 ? now.wider() : now.narrower();
      if (next != null) {
         CFHGuiMenu.configFile().update((c) -> c.createPreviewSpan = next.blocksWide());
         this.changedAt = Clock.nowMs();
      }
   }

   private void reroll() {
      String seed = Long.toString((new Random()).nextLong());
      if (this.seedField != null) {
         this.seedField.setValue(seed);
      } else {
         this.state.setSeed(seed);
      }

   }

   private void create() {
      ((CreateWorldScreenAccess)this.vanilla).cfhgui$create();
   }

   private void fill(CFHGuiScrollPanel panel, int tab) {
      int inner = panel.innerWidth();
      boolean two = inner >= 260;
      int gap = 4;
      int colW = two ? (inner - 4) / 2 : inner;
      List var10000;
      switch (tab) {
         case 1 -> var10000 = this.worldTab(colW);
         case 2 -> var10000 = this.moreTab(colW);
         default -> var10000 = this.gameTab(colW);
      }

      List<Tile> tiles = var10000;
      int i = 0;
      int y = 0;

      for(Tile t : tiles) {
         int col = two ? i % 2 : 0;
         panel.add(t, col * (colW + 4), y);
         t.playEntrance(Math.min(200, i * 18 * 2));
         if (!two || col == 1) {
            y += 46;
         }

         ++i;
      }

      if (two && i % 2 == 1) {
         y += 46;
      }

      panel.setContentHeight(y);
   }

   private static Component onOff(boolean on) {
      return on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
   }

   private CFHGuiToggle toggle(BooleanSupplier get, Consumer<Boolean> set) {
      Component var10005 = onOff(get.getAsBoolean());
      boolean var10006 = get.getAsBoolean();
      Objects.requireNonNull(set);
      CFHGuiToggle t = (new CFHGuiToggle(0, 0, 70, var10005, var10006, set::accept)).switchFirst();
      this.refreshers.add(() -> {
         boolean on = get.getAsBoolean();
         if (t.value() != on) {
            t.setValue(on);
         }

         t.setMessage(onOff(on));
      });
      return t;
   }

   private List<Tile> gameTab(int w) {
      List<Tile> out = new ArrayList();
      CFHGuiTextField name = new CFHGuiTextField(0, 0, w, Component.translatable("selectWorld.enterName"));
      name.text(this.state.getName()).maxLength(64);
      WorldCreationUiState var10001 = this.state;
      Objects.requireNonNull(var10001);
      name.onChange(var10001::setName);
      out.add((new Tile(w, Component.translatable("selectWorld.enterName"), name)).tip(() -> Component.translatable("selectWorld.targetFolder", new Object[]{Component.literal(this.state.getTargetFolder()).withStyle(ChatFormatting.ITALIC)})));
      List<WorldCreationUiState.SelectedGameMode> modes = new ArrayList(List.of(SelectedGameMode.SURVIVAL, SelectedGameMode.HARDCORE, SelectedGameMode.CREATIVE));
      if (this.state.isDebug()) {
         modes.add(SelectedGameMode.DEBUG);
      }

      WorldCreationUiState.SelectedGameMode var10006 = this.state.getGameMode();
      Function<WorldCreationUiState.SelectedGameMode, Component> var10007 = (m) -> m.displayName;
      WorldCreationUiState var10008 = this.state;
      Objects.requireNonNull(var10008);
      CFHGuiSegmented<WorldCreationUiState.SelectedGameMode> mode = new CFHGuiSegmented<WorldCreationUiState.SelectedGameMode>(0, 0, w, modes, var10006, var10007, var10008::setGameMode);
      mode.optionTip(WorldCreationUiState.SelectedGameMode::getInfo);
      this.refreshers.add(() -> {
         if (mode.value() != this.state.getGameMode()) {
            mode.setValue(this.state.getGameMode());
         }

      });
      out.add((new Tile(w, Component.translatable("selectWorld.gameMode"), mode)).tip(() -> this.state.getGameMode().getInfo()).enabled(() -> !this.state.isDebug()));
      List<Difficulty> var10005 = List.of(Difficulty.values());
      Difficulty var7 = this.state.getDifficulty();
      Function<Difficulty, Component> var10009 = Difficulty::getDisplayName;
      var10008 = this.state;
      Objects.requireNonNull(var10008);
      CFHGuiSegmented<Difficulty> difficulty = new CFHGuiSegmented<Difficulty>(0, 0, w, var10005, var7, var10009, var10008::setDifficulty);
      this.refreshers.add(() -> {
         if (difficulty.value() != this.state.getDifficulty()) {
            difficulty.setValue(this.state.getDifficulty());
         }

      });
      out.add((new Tile(w, Component.translatable("options.difficulty"), difficulty)).tip(() -> this.state.getDifficulty().getInfo()).enabled(() -> !this.state.isHardcore()));
      MutableComponent var10004 = Component.translatable("selectWorld.allowCommands.new");
      WorldCreationUiState var8 = this.state;
      Objects.requireNonNull(var8);
      BooleanSupplier var9 = var8::isAllowCommands;
      WorldCreationUiState var11 = this.state;
      Objects.requireNonNull(var11);
      out.add((new Tile(w, var10004, this.toggle(var9, var11::setAllowCommands))).tip(Component.translatable("selectWorld.allowCommands.info")).enabled(() -> !this.state.isDebug() && !this.state.isHardcore()));
      return out;
   }

   private List<Tile> worldTab(int w) {
      List<Tile> out = new ArrayList();
      List<WorldCreationUiState.WorldTypeEntry> types = new ArrayList(this.state.getNormalPresetList());

      for(WorldCreationUiState.WorldTypeEntry e : this.state.getAltPresetList()) {
         if (!types.contains(e)) {
            types.add(e);
         }
      }

      if (!types.contains(this.state.getWorldType())) {
         types.add(0, this.state.getWorldType());
      }

      WorldCreationUiState.WorldTypeEntry var10006 = this.state.getWorldType();
      Function<WorldCreationUiState.WorldTypeEntry, Component> var10007 = WorldCreationUiState.WorldTypeEntry::describePreset;
      WorldCreationUiState var10008 = this.state;
      Objects.requireNonNull(var10008);
      CFHGuiDropdown<WorldCreationUiState.WorldTypeEntry> type = new CFHGuiDropdown<WorldCreationUiState.WorldTypeEntry>(0, 0, w, types, var10006, var10007, var10008::setWorldType);
      Tile typeTile = (new Tile(w, Component.translatable("selectWorld.mapType"), type)).tip(() -> this.state.getWorldType().isAmplified() ? Component.translatable("generator.minecraft.amplified.info") : null);
      PresetEditor editor = this.state.getPresetEditor();
      if (editor != null) {
         typeTile.extra(new CFHGuiIconButton(0, 0, 20, Icon.SLIDERS, Component.translatable("selectWorld.customizeType"), () -> Minecraft.getInstance().setScreen(editor.createEditScreen(this.vanilla, this.state.getSettings()))));
      }

      out.add(typeTile);
      CFHGuiTextField seed = new CFHGuiTextField(0, 0, w, Component.translatable("selectWorld.enterSeed"));
      seed.placeholder(Component.translatable("selectWorld.seedInfo")).text(this.state.getSeed()).maxLength(32);
      seed.onChange((s) -> {
         if (!s.equals(this.state.getSeed())) {
            this.state.setSeed(s);
         }

      });
      this.seedField = seed;
      out.add((new Tile(w, Component.translatable("selectWorld.enterSeed"), seed)).extra(new CFHGuiIconButton(0, 0, 20, Icon.REFRESH, Component.translatable("cfhgui.create.reroll"), this::reroll)).tip(Component.translatable("selectWorld.seedInfo")));
      MutableComponent var10004 = Component.translatable("selectWorld.mapFeatures");
      WorldCreationUiState var11 = this.state;
      Objects.requireNonNull(var11);
      BooleanSupplier var12 = var11::isGenerateStructures;
      WorldCreationUiState var15 = this.state;
      Objects.requireNonNull(var15);
      out.add((new Tile(w, var10004, this.toggle(var12, var15::setGenerateStructures))).tip(Component.translatable("selectWorld.mapFeatures.info")).enabled(() -> !this.state.isDebug()));
      var10004 = Component.translatable("selectWorld.bonusItems");
      WorldCreationUiState var13 = this.state;
      Objects.requireNonNull(var13);
      BooleanSupplier var14 = var13::isBonusChest;
      var15 = this.state;
      Objects.requireNonNull(var15);
      out.add((new Tile(w, var10004, this.toggle(var14, var15::setBonusChest))).enabled(() -> !this.state.isHardcore() && !this.state.isDebug()));
      return out;
   }

   private List<Tile> moreTab(int w) {
      Minecraft mc = Minecraft.getInstance();
      List<Tile> out = new ArrayList();
      out.add((new Tile(w, Component.translatable("selectWorld.gameRules"), (new CFHGuiButton(0, 0, w, Component.translatable("cfhgui.create.edit"), () -> mc.setScreen(new EditGameRulesScreen(this.state.getGameRules().copy(), (changed) -> {
            mc.setScreen(this.vanilla);
            WorldCreationUiState var10001 = this.state;
            Objects.requireNonNull(var10001);
            changed.ifPresent(var10001::setGameRules);
         })))).icon(Icon.SLIDERS))).tip(Component.translatable("cfhgui.create.game_rules.info")));
      out.add((new Tile(w, Component.translatable("selectWorld.dataPacks"), (new CFHGuiButton(0, 0, w, Component.translatable("cfhgui.create.choose"), () -> ((CreateWorldScreenAccess)this.vanilla).cfhgui$openDataPacks(this.state.getSettings().dataConfiguration()))).icon(Icon.PACK))).tip(Component.translatable("cfhgui.create.data_packs.info")));
      out.add((new Tile(w, Component.translatable("selectWorld.experiments"), (new CFHGuiButton(0, 0, w, Component.translatable("cfhgui.create.choose"), () -> ((CreateWorldScreenAccess)this.vanilla).cfhgui$openExperiments(this.state.getSettings().dataConfiguration()))).icon(Icon.SPARKLE))).tip(Component.translatable("cfhgui.create.experiments.info")));
      return out;
   }

   private void createScene() {
      Stage s = (new Stage()).bind(this);
      this.stage = s;
      s.background(-2119036);
      s.fog(true, 13.0F, 25.0F, -2119036);
      s.finish(StageFinish.cinematic().vignette(0.26F));
      s.soft(true);
      s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
      s.lighting().key(9.0F, 9.0F, 5.0F).keyColor(0.78F, 0.66F, 0.5F).ambientColor(0.4F, 0.4F, 0.47F).fill(-1.0F, 0.3F, 0.2F, 0.12F, 0.16F, 0.26F).rim(1.0F, 0.85F, 0.65F, 0.16F).sun(0.75F, 0.6F, 0.35F);
      s.camera().clip(0.1F, 90.0F).idle(IdleMotion.sway(26000.0F, 2.5F, 0.3F, 0.03F));
      TerrainNode land = (TerrainNode)s.add((new TerrainNode(192, 96)).edgeFade(0.94F, 16.0F));
      land.scale(0.083333336F);
      this.terrain = land;
      ((MotesNode)s.add(new MotesNode(30, 12.0F, 3.0F, 6.0F, 0.035F, 1358946992, 3L))).at(0.0F, 4.0F, 3.0F);
      this.viewIn.snap(0.0F);
      this.viewIn.set(1.0F);
      this.session.preview.show(land);
   }

   private Rect shown() {
      return this.band.withH(Math.round(Mth.lerp(this.grow.get(), (float)this.band.h(), (float)this.full.h())));
   }

   private Rect spanChip(Rect r) {
      int tw = CFHGuiDraw.font().width(this.spanText(r));
      int cw = tw + 26 + 12;
      return new Rect(r.right() - 5 - cw, r.bottom() - 5 - 15, cw, 15);
   }

   private Component spanText(Rect r) {
      WorldPreview.Span s = span();
      Component said = Component.translatable("cfhgui.create.preview.span", new Object[]{s.blocksWide(), s.blocksDeep()});
      return (Component)(CFHGuiDraw.font().width(said) + 40 < r.w() / 2 ? said : Component.translatable("cfhgui.create.preview.span.short", new Object[]{s.blocksWide(), s.blocksDeep()}));
   }

   private void layoutView() {
      Rect r = this.shown();
      if (this.view != null) {
         this.view.setRectangle(r.w() - 2, r.h() - 2, r.x() + 1, r.y() + 1);
      }

      int bx = r.right() - 24;

      for(CFHGuiIconButton b : new CFHGuiIconButton[]{this.fold, this.resize, this.reroll}) {
         if (b != null) {
            b.setX(bx);
            b.setY(r.y() + 5);
            bx -= 22;
         }
      }

      boolean failed = this.session.preview.state() == WorldPreview.State.FAILED;
      Rect chip = this.spanChip(r);
      WorldPreview.Span s = span();
      if (this.narrower != null) {
         this.narrower.setX(chip.x() + 1);
         this.narrower.setY(chip.y() + 1);
         this.narrower.visible = !failed;
         this.narrower.active = s.narrower() != null;
      }

      if (this.wider != null) {
         this.wider.setX(chip.right() - 15 + 1);
         this.wider.setY(chip.y() + 1);
         this.wider.visible = !failed;
         this.wider.active = s.wider() != null;
      }

      this.aim(r);
   }

   private void aim(Rect view) {
      if (this.stage != null) {
         float aspect = Math.max(0.3F, (float)(view.w() - 2) / (float)Math.max(1, view.h() - 2));
         float pitch = Mth.lerp(smooth((aspect - 1.4F) / 3.6F), 50.0F, 26.0F);
         int sea = Math.round(39.0F / (float)span().rise);
         float floor = (float)Math.max(1, sea - 4) * 0.083333336F;
         Aerial.View v = Aerial.over(8.0F, 4.0F, floor, MARGINS, (double)25.0F, aspect, (double)pitch);
         this.stage.camera().at(0.0F, v.eyeY(), v.eyeZ()).lookAt(0.0F, v.targetY(), v.targetZ()).fov(v.fov());
         if (this.terrain != null) {
            this.terrain.shelf(10, sea);
         }

      }
   }

   private static float smooth(float t) {
      float u = Mth.clamp(t, 0.0F, 1.0F);
      return u * u * (3.0F - 2.0F * u);
   }

   public void tick() {
      super.tick();

      for(Runnable r : this.refreshers) {
         r.run();
      }

      if (!this.formShown || this.session.tab != 1 || this.builtFor == this.state.getWorldType() && this.builtEditor == (this.state.getPresetEditor() != null)) {
         if (this.changedAt != 0L && Clock.nowMs() - this.changedAt > 320L) {
            this.changedAt = 0L;
            if (this.viewShown()) {
               this.session.preview.update(this.state.getSettings(), span());
            }
         }

      } else {
         this.rebuildWidgets();
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256 && this.viewShown() && this.session.maximised) {
         this.toggleMaximised();
         return true;
      } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (keyCode != 257 && keyCode != 335) {
         return false;
      } else {
         this.create();
         return true;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.viewShown() && scrollY != (double)0.0F && this.session.preview.state() != WorldPreview.State.FAILED && this.shown().contains(mouseX, mouseY)) {
         this.changeSpan(scrollY < (double)0.0F ? 1 : -1);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   public void removed() {
      super.removed();
      this.closed = true;
      this.session.preview.show((TerrainNode)null);
      if (this.stage != null) {
         if (DevHarness.active()) {
            CFHGuiMenu.LOGGER.info("[CFHGui Menu] world preview: {} frames, {} ms a frame, {} ms at worst, {}x{}", new Object[]{this.stage.frames(), "%.2f".formatted(this.stage.frameMs()), "%.2f".formatted(this.worstMs), this.stage.targetWidth(), this.stage.targetHeight()});
         }

         this.stage.close();
         this.stage = null;
      }

      this.terrain = null;
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      Theme t = Theme.current();
      if (!t.isVanilla() && !MenuBackdrop.worldBehind()) {
         CFHGuiDraw.vgradient(g, 0, 32, this.width, this.height - 32, -16053233, -15067114);
         CFHGuiDraw.vignette(g, 0, 32, this.width, this.height - 32, 0.4F);
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.viewShown()) {
         if (!this.formShown && !this.session.maximised && this.grow.get() <= 0.001F) {
            this.rebuildWidgets();
         }

         this.layoutView();
      }

      super.render(g, mouseX, mouseY, partialTick);
      if (this.stage != null && this.stage.frames() > 30L && DevHarness.active()) {
         this.worstMs = Math.max(this.worstMs, this.stage.lastFrameMs());
      }

   }

   private void renderPlate(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Rect r = this.shown();
      CFHGuiDraw.shadow(g, r.x(), r.y(), r.w(), r.h(), 0.6F);
      CFHGuiDraw.pixelRound(g, r.x(), r.y(), r.w(), r.h(), t.isVanilla() ? -16777216 : -16053233, t.isVanilla() ? 0 : t.radius());
   }

   private void renderChips(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      Rect r = this.shown();
      float a = this.viewIn.get();
      int radius = van ? 0 : t.radius();
      WorldPreview preview = this.session.preview;
      Component seed = Component.translatable("cfhgui.create.seed", new Object[]{Long.toString(preview.seed())});
      int buttons = (this.fold != null ? 3 : 2) * 22 + 2;
      int cw = Math.min(this.font.width(seed) + 22, r.w() - buttons - 12);
      int cx = r.x() + 5;
      int cy = r.y() + 5;
      CFHGuiDraw.pixelRound(g, cx, cy, cw, 15, Colors.scaleAlpha(van ? -1073741824 : Colors.withAlpha(p.bg(), 216), a), radius);
      Icons.draw(g, Icon.MAP, cx + 4, cy + 3, 9, Colors.scaleAlpha(van ? -2039584 : p.accentInk(), a));
      g.drawString(this.font, CFHGuiDraw.truncate(seed, cw - 20), cx + 17, cy + 4, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
      CFHGuiDraw.pixelRound(g, r.right() - buttons - 3, r.y() + 3, buttons, 22, van ? -1073741824 : Colors.withAlpha(t.bg(), 216), radius);
      if (preview.state() != WorldPreview.State.FAILED) {
         Rect chip = this.spanChip(r);
         Component span = this.spanText(r);
         CFHGuiDraw.pixelRound(g, chip.x(), chip.y(), chip.w(), chip.h(), Colors.scaleAlpha(van ? -1073741824 : Colors.withAlpha(p.bg(), 200), a), radius);
         g.drawString(this.font, span, chip.x() + 15 + 4, chip.y() + 4, Colors.scaleAlpha(van ? -2039584 : p.textMuted(), a), van);
      }

   }

   protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      boolean van = t.isVanilla();
      int by = this.height - 12 + 2 - 20;
      Component folder = Component.translatable("selectWorld.targetFolder", new Object[]{Component.literal(this.state.getTargetFolder()).withStyle(ChatFormatting.ITALIC)});
      int room = this.width - 24 - Math.min(150, Math.max(96, (this.width - 24) / 3)) - Math.min(90, Math.max(56, (this.width - 24) / 5)) - 18;
      if (room > 60) {
         g.drawString(this.font, CFHGuiDraw.truncate(folder, room), 12, by + 6, van ? -6250336 : p.textDim(), van);
      }

      if (this.viewShown()) {
         Rect r = this.shown();
         CFHGuiDraw.outline(g, r.x(), r.y(), r.w(), r.h(), van ? -16777216 : p.borderStrong(), van ? 0 : t.radius());
         WorldPreview preview = this.session.preview;
         WorldPreview.State st = preview.state();
         if (st == WorldPreview.State.FAILED) {
            Component none = Component.translatable(this.state.isDebug() ? "cfhgui.create.preview.debug" : "cfhgui.create.preview.none");
            CFHGuiDraw.textCentered(g, none, r.centerX(), r.centerY() - 4, -1, true);
         }

         if (st == WorldPreview.State.WORKING) {
            int lw = Math.round((float)(r.w() - 4) * Mth.clamp(preview.progress(), 0.0F, 1.0F));
            CFHGuiDraw.rect(g, r.x() + 2, r.bottom() - 3, r.w() - 4, 1, 1342177280);
            CFHGuiDraw.rect(g, r.x() + 2, r.bottom() - 3, lw, 1, van ? -1 : p.accent());
         }

      }
   }

   public boolean previewComplete() {
      return this.session.preview.state() == WorldPreview.State.DONE;
   }

   private static final class Session {
      int tab;
      boolean maximised;
      final WorldPreview preview = new WorldPreview();
   }
}
