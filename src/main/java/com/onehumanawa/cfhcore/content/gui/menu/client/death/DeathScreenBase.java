package com.onehumanawa.cfhcore.content.gui.menu.client.death;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutBackground;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.menu.client.pause.PauseActions;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.ScreenRenderablesAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public abstract class DeathScreenBase extends DeathScreen {
   public static final int ARM_TICKS = 20;
   protected static final int LINE = 10;
   protected static final int FACT_ROW = 12;
   private static final int PREVIEW_BUSY_TICKS = 60;
   private static final int SHADE = 919047;
   private static final int EDGE = 4328205;
   private static final String DOT = " · ";
   protected final @Nullable Component cause;
   protected final boolean hardcore;
   protected final boolean preview;
   private final @Nullable BlockPos diedAt;
   private final @Nullable Component dimension;
   protected final Anim veil;
   protected final Anim rise;
   private final List<Renderable> own;
   private boolean entered;
   private int ticks;
   private boolean respawning;
   private int busyTicks;
   private @Nullable AbstractWidget respawnControl;
   private @Nullable AbstractWidget leaveControl;
   private List<FormattedCharSequence> causeLines;
   private int causeX;
   private int causeY;
   private int causeW;
   private boolean causeCentred;
   private @Nullable Component said;
   private @Nullable Component place;
   private @Nullable String coordinates;
   private int saidX;
   private int saidY;
   private int saidW;
   private int dotX;
   private boolean dot;

   protected DeathScreenBase(@Nullable Component cause, boolean hardcore, boolean preview) {
      super(cause, hardcore);
      this.veil = new Anim(0.0F, 520, Ease.OUT_CUBIC);
      this.rise = new Anim(0.0F, 300, Ease.OUT_CUBIC);
      this.own = new ArrayList();
      this.causeLines = List.of();
      this.cause = cause != null && !cause.getString().isEmpty() ? cause : null;
      this.hardcore = hardcore;
      this.preview = preview;
      LocalPlayer player = Minecraft.getInstance().player;
      this.diedAt = player == null ? null : player.blockPosition();
      this.dimension = PauseActions.dimension();
   }

   public static Component sampleCause() {
      Minecraft mc = Minecraft.getInstance();
      Component victim = (Component)(mc.player != null ? mc.player.getDisplayName() : Component.literal(mc.getUser().getName()));
      Component about = Component.translatable("entity.minecraft.zombie").append("\n").append(Component.literal("minecraft:zombie").withStyle(ChatFormatting.GRAY));
      Component killer = Component.translatable("entity.minecraft.zombie").withStyle((style) -> style.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, about)));
      return Component.translatable("death.attack.mob", new Object[]{victim, killer});
   }

   protected final void init() {
      this.own.clear();
      this.respawnControl = this.leaveControl = null;
      this.causeLines = List.of();
      if (this.veil.target() == 0.0F) {
         this.veil.set(1.0F);
         this.rise.set(1.0F);
      }

      this.gatherFacts();
      this.build();
      if (!this.entered) {
         this.entered = true;
         int i = 0;

         for(Renderable r : this.own) {
            i = CFHGuiScreen.entrance(r, i);
         }
      }

      this.sync();
   }

   protected abstract void build();

   protected abstract void renderContent(GuiGraphics var1, int var2, int var3, float var4);

   protected final <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
      this.own.add(widget);
      return (T)this.addRenderableWidget(widget);
   }

   protected final void exits(AbstractWidget respawn, AbstractWidget leave) {
      this.respawnControl = respawn;
      this.leaveControl = leave;
   }

   private void sync() {
      if (this.respawnControl != null) {
         this.respawnControl.active = this.armed() && !this.respawning;
      }

      if (this.leaveControl != null) {
         this.leaveControl.active = this.armed();
      }

   }

   protected void setInitialFocus() {
   }

   public final boolean armed() {
      return this.ticks >= 20;
   }

   public final float arming() {
      if (this.ticks >= 20) {
         return 1.0F;
      } else {
         float partial = this.minecraft != null && Theme.current().motion() > 0.0F ? this.minecraft.getTimer().getGameTimeDeltaPartialTick(true) : 0.0F;
         return Mth.clamp(((float)this.ticks + partial) / 20.0F, 0.0F, 1.0F);
      }
   }

   public final boolean respawning() {
      return this.respawning;
   }

   protected final Component respawnLabel() {
      return Component.translatable(this.hardcore ? "deathScreen.spectate" : "deathScreen.respawn");
   }

   protected final Icon respawnIcon() {
      return this.hardcore ? Icon.SPECTATOR : Icon.REFRESH;
   }

   public void tick() {
      super.tick();
      if (this.ticks < 20 && ++this.ticks == 20) {
         this.sync();
      }

      if (this.preview && this.respawning && ++this.busyTicks >= 60 && this.minecraft != null) {
         this.minecraft.setScreen((Screen)null);
      }

   }

   protected final void respawn() {
      if (!this.respawning && this.armed()) {
         this.respawning = true;
         if (!this.preview && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.respawn();
         }

         this.sync();
      }
   }

   protected final void leave() {
      if (this.armed() && this.minecraft != null) {
         this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, this::confirmLeave, true);
      }
   }

   private void confirmLeave() {
      if (this.minecraft != null) {
         if (this.hardcore) {
            this.exitToTitle();
         } else {
            if (this.minecraft.screen != this) {
               this.minecraft.setScreen(this);
            }

            if (this.leaveControl != null) {
               this.leaveControl.mouseReleased((double)0.0F, (double)0.0F, 0);
            }

            this.setDragging(false);
            LeaveConfirm.show(() -> PauseActions.afterClick(this::exitToTitle), this::respawn);
         }
      }
   }

   private void exitToTitle() {
      if (this.minecraft != null) {
         if (this.preview) {
            this.minecraft.setScreen((Screen)null);
         } else {
            if (this.minecraft.level != null) {
               this.minecraft.level.disconnect();
            }

            this.minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
            this.minecraft.setScreen(new TitleScreen());
         }
      }
   }

   public boolean shouldCloseOnEsc() {
      return this.preview;
   }

   public void onClose() {
      if (this.preview && this.minecraft != null) {
         this.minecraft.setScreen((Screen)null);
      }

   }

   public Component getNarrationMessage() {
      return (Component)(this.cause == null ? this.getTitle() : CommonComponents.joinForNarration(new Component[]{this.getTitle(), this.cause}));
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      Style style = this.causeStyleAt(mouseX, mouseY);
      if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == net.minecraft.network.chat.ClickEvent.Action.OPEN_URL) {
         this.handleComponentClicked(style);
         return false;
      } else {
         for(GuiEventListener child : this.children()) {
            if (child.mouseClicked(mouseX, mouseY, button)) {
               this.setFocused(child);
               if (button == 0) {
                  this.setDragging(true);
               }

               return true;
            }
         }

         return false;
      }
   }

   public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      boolean covered = Popups.any();
      int mx = covered ? -9999 : mouseX;
      int my = covered ? -9999 : mouseY;
      if (!LayoutBackground.render(this, g, partialTick, PANORAMA)) {
         if (this.minecraft != null && this.minecraft.level == null) {
            g.fill(0, 0, this.width, this.height, -16777216 | Theme.current().bg());
         }

         this.renderBackground(g, mx, my, partialTick);
      }

      for(Renderable r : ((ScreenRenderablesAccessor)this).cfhgui_menu$renderables()) {
         r.render(g, mx, my, partialTick);
      }

      this.renderContent(g, mx, my, partialTick);
      if (!covered) {
         Style style = this.causeStyleAt((double)mx, (double)my);
         if (style != null) {
            g.renderComponentHoverEffect(this.font, style, mx, my);
         }
      }

   }

   protected final void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
      if (color >>> 24 >= 4) {
         g.drawString(this.font, text, x, y, color, shadow);
      }
   }

   protected final int slide() {
      return Math.round((1.0F - this.rise.get()) * 6.0F);
   }

   protected final void shade(GuiGraphics g, float amount, int alpha) {
      if (!(amount <= 0.01F)) {
         g.fill(0, 0, this.width, this.height, Colors.withAlpha(919047, Math.round((float)alpha * amount)));
      }
   }

   protected final void edges(GuiGraphics g, float amount) {
      if (!(amount <= 0.01F)) {
         int strong = Colors.withAlpha(4328205, Math.round(196.0F * amount));
         int soft = Colors.withAlpha(4328205, Math.round(122.0F * amount));
         int clear = Colors.withAlpha(4328205, 0);
         int vh = Math.max(8, this.height / 3);
         int hw = Math.max(8, this.width / 6);
         CFHGuiDraw.vgradient(g, 0, 0, this.width, vh, strong, clear);
         CFHGuiDraw.vgradient(g, 0, this.height - vh, this.width, vh, clear, strong);
         CFHGuiDraw.hgradient(g, 0, 0, hw, this.height, soft, clear);
         CFHGuiDraw.hgradient(g, this.width - hw, 0, hw, this.height, clear, soft);
      }
   }

   protected final void vanillaVeil(GuiGraphics g, float amount) {
      if (!(amount <= 0.01F)) {
         g.fillGradient(0, 0, this.width, this.height, Colors.scaleAlpha(1615855616, amount), Colors.scaleAlpha(-1602211792, amount));
      }
   }

   protected final List<FormattedCharSequence> wrapCause(int width, int maxLines) {
      if (this.cause != null && maxLines > 0) {
         List<FormattedCharSequence> lines = this.font.split(this.cause, Math.max(40, width));
         return lines.size() <= maxLines ? lines : lines.subList(0, maxLines);
      } else {
         return List.of();
      }
   }

   protected final void placeCause(List<FormattedCharSequence> lines, int x, int y, int w, boolean centred) {
      this.causeLines = lines;
      this.causeX = x;
      this.causeY = y;
      this.causeW = w;
      this.causeCentred = centred;
   }

   private int causeLeft(FormattedCharSequence line) {
      return this.causeCentred ? this.causeX + (this.causeW - this.font.width(line)) / 2 : this.causeX;
   }

   protected final void drawCause(GuiGraphics g, int dy, int color, boolean shadow) {
      int y = this.causeY + dy;

      for(FormattedCharSequence line : this.causeLines) {
         this.text(g, line, this.causeLeft(line), y, color, shadow);
         y += 10;
      }

   }

   private @Nullable Style causeStyleAt(double mouseX, double mouseY) {
      if (!this.causeLines.isEmpty() && !(mouseY < (double)this.causeY)) {
         int row = (int)((mouseY - (double)this.causeY) / (double)10.0F);
         if (row < this.causeLines.size() && !(mouseY - (double)this.causeY - (double)(row * 10) >= (double)9.0F)) {
            FormattedCharSequence line = (FormattedCharSequence)this.causeLines.get(row);
            int left = this.causeLeft(line);
            return !(mouseX < (double)left) && !(mouseX > (double)(left + this.font.width(line))) ? this.font.getSplitter().componentStyleAtWidth(line, Mth.floor(mouseX - (double)left)) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void gatherFacts() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      Theme theme = Theme.current();
      MutableComponent line = Component.empty();
      boolean any = false;
      if (player != null) {
         Style number = theme.isVanilla() ? Style.EMPTY.withColor(ChatFormatting.YELLOW) : Style.EMPTY.withColor(theme.palette().text() & 16777215);
         line.append(Component.translatable("deathScreen.score.value", new Object[]{Component.literal(Integer.toString(player.getScore())).withStyle(number)}));
         if (player.experienceLevel > 0) {
            line.append(" · ").append(Component.translatable("cfhgui.pause.level", new Object[]{player.experienceLevel}));
         }

         any = true;
      }

      if (this.diedAt != null && !mc.showOnlyReducedInfo()) {
         this.place = Component.translatable("chat.coordinates", new Object[]{this.diedAt.getX(), this.diedAt.getY(), this.diedAt.getZ()});
         int var10001 = this.diedAt.getX();
         this.coordinates = var10001 + " " + this.diedAt.getY() + " " + this.diedAt.getZ();
      } else {
         this.place = null;
         this.coordinates = null;
         if (this.dimension != null) {
            if (any) {
               line.append(" · ");
            }

            line.append(this.dimension);
            any = true;
         }
      }

      this.said = any ? line : null;
   }

   private @Nullable Component placeWords(int w) {
      if (this.place == null) {
         return null;
      } else if (this.dimension == null) {
         return this.place;
      } else {
         Component full = Component.empty().append(this.place).append(" · ").append(this.dimension);
         return WhereLink.width(this.font, full) <= w ? full : this.place;
      }
   }

   protected final int factRows(int w) {
      Component words = this.placeWords(w);
      if (this.said == null && words == null) {
         return 0;
      } else if (this.said != null && words != null) {
         return this.font.width(this.said) + this.font.width(" · ") + WhereLink.width(this.font, words) <= w ? 1 : 2;
      } else {
         return 1;
      }
   }

   protected final void placeFacts(int x, int y, int w, boolean centred) {
      Component words = this.placeWords(w);
      int linkW = words == null ? 0 : Math.min(w, WhereLink.width(this.font, words));
      int dotW = this.font.width(" · ");
      this.saidW = this.said == null ? 0 : Math.min(w, this.font.width(this.said));
      this.saidY = y;
      this.dot = false;
      int linkX = x;
      int linkY = y;
      if (this.said != null && words != null && this.factRows(w) == 1) {
         this.saidX = centred ? x + (w - this.saidW - dotW - linkW) / 2 : x;
         this.dotX = this.saidX + this.saidW;
         this.dot = true;
         linkX = this.dotX + dotW;
      } else {
         this.saidX = centred ? x + (w - this.saidW) / 2 : x;
         if (words != null) {
            linkX = centred ? x + (w - linkW) / 2 : x;
            if (this.said != null) {
               linkY = y + 12;
            }
         }
      }

      if (words != null) {
         this.add(new WhereLink(linkX, linkY - 1, linkW, words, this::copyPlace));
      }

   }

   protected final void drawFacts(GuiGraphics g, int dy, float alpha, boolean shadow) {
      if (this.said != null) {
         Theme theme = Theme.current();
         int muted = Colors.scaleAlpha(theme.isVanilla() ? -3618616 : theme.palette().textMuted(), alpha);
         if (muted >>> 24 >= 4) {
            g.drawString(this.font, CFHGuiDraw.truncate(this.said, this.saidW), this.saidX, this.saidY + dy, muted, shadow);
            if (this.dot) {
               g.drawString(this.font, " · ", this.dotX, this.saidY + dy, muted, shadow);
            }

         }
      }
   }

   private void copyPlace() {
      if (this.coordinates != null && this.minecraft != null) {
         this.minecraft.keyboardHandler.setClipboard(this.coordinates);
         CFHGuiToasts.show(Component.translatable("cfhgui.copied"), Component.literal(this.coordinates), Icon.COPY);
      }
   }
}
