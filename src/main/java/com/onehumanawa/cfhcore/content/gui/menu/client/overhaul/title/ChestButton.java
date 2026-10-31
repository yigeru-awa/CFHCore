package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.module.KnownModules;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BurstNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ChestNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.HitNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SignBoardNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.TextNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class ChestButton {
   private static final float LID_TILT = 7.0F;
   private static final float THING_Y = 0.07F;
   private static final float THING_Z = 0.2F;
   private static final float THING_GROWS = 0.06F;
   private static final float LABEL_Y = -0.275F;
   private static final float LABEL_SIZE = 0.0128F;
   private static final float LABEL_ROOM = 0.62F;
   static final float THING_SIZE = 0.47F;
   private static final float SIGN_SCALE = 0.75F;
   private static final float SIGN_Y = 0.09375F;
   private static final float SIGN_Z = 0.4475F;
   private static final int SIGN_INK = -14017780;
   private static final int SIGN_INK_HOVER = -1;
   private static final int SIGN_INK_LOCKED = -6519720;
   private static final int OAK_INK = -16777216;
   private static final int OAK_INK_LOCKED = -1728053248;
   private static final int GLOW = -7496;
   final PivotNode root = new PivotNode();
   final ChestNode chest;
   final PivotNode signPivot = new PivotNode();
   final SignBoardNode sign;
   final TextNode signText;
   final PivotNode face = new PivotNode();
   final PivotNode floater = new PivotNode();
   final TextNode label;
   final FxNode lidGlow;
   final BurstNode sparks;
   final HitNode lidHit = new HitNode();
   final HitNode signHit = new HitNode();
   private final float openAtMs;
   private final boolean sounds;
   private final int turn;
   private final boolean lidLocked;
   private final boolean signLocked;
   private final boolean lidShown;
   private final boolean signShown;
   private final int ink;
   private final int inkLocked;
   private boolean opened;
   private boolean signDisabled;
   private final Anim pop;
   private final Anim labelIn;
   private final Anim lidHover;
   private final Anim signHover;
   private final float bobPhase;
   private @Nullable Vector3f eye;
   private final Vector3f seen;
   private @Nullable HoverListener lidListener;

   ChestButton(Stage stage, Anchor anchor, int index, int turn, float openAtMs, boolean sounds, Look look, Component lidLabel, Runnable lidAction, @Nullable String lidModule, Component signLabel, Runnable signAction, @Nullable String signModule, boolean lidShown, boolean signShown) {
      this.pop = new Anim(0.0F, 520, Ease.OUT_BACK);
      this.labelIn = new Anim(0.0F, 300, Ease.OUT_CUBIC);
      this.lidHover = new Anim(0.0F, 200, Ease.OUT_CUBIC);
      this.signHover = new Anim(0.0F, 170, Ease.OUT_CUBIC);
      this.seen = new Vector3f();
      this.openAtMs = openAtMs;
      this.sounds = sounds && lidShown;
      this.turn = turn;
      this.bobPhase = (float)index * 1.7F;
      this.lidLocked = lidModule != null && !Features.present(lidModule);
      this.signLocked = signModule != null && !Features.present(signModule);
      this.lidShown = lidShown;
      this.signShown = signShown;
      boolean vanilla = look == ChestButton.Look.VANILLA;
      this.ink = vanilla ? -16777216 : -14017780;
      this.inkLocked = vanilla ? -1728053248 : -6519720;
      anchor.place((PivotNode)stage.add(this.root));
      this.chest = (ChestNode)stage.add(new ChestNode());
      this.chest.vanilla(vanilla).lidAngle(97.0F).pickable(false).attachTo(this.root);
      ((PivotNode)stage.add(this.signPivot)).at(0.0F, 0.09375F, 0.4475F).scale(0.75F).attachTo(this.root);
      this.sign = (SignBoardNode)stage.add(new SignBoardNode());
      this.sign.vanilla(vanilla).pickable(false).attachTo(this.signPivot);
      this.signText = (TextNode)stage.add(new TextNode(signLabel));
      int signW = Math.max(1, Minecraft.getInstance().font.width(signLabel));
      this.signText.billboard(false).background(0).size(Math.min(0.0205F, 0.84F / (float)signW)).color(this.signLocked ? this.inkLocked : this.ink);
      this.signText.at(0.0F, 0.25F, 0.082125F).attachTo(this.signPivot);
      double a = Math.toRadians((double)-97.0F);
      double depth = vanilla ? (double)0.0F : (double)4.0F;
      float panelY = (float)((double)0.5625F + (depth * Math.cos(a) - (double)7.0F * Math.sin(a)) / (double)16.0F);
      float panelZ = (float)((double)-0.4375F + (depth * Math.sin(a) + (double)7.0F * Math.cos(a)) / (double)16.0F);
      ((PivotNode)stage.add(this.face)).at(0.0F, panelY, panelZ).rotate(0.0F, -7.0F, 0.0F).attachTo(this.root);
      this.lidGlow = (FxNode)stage.add(FxNode.glow(0.66F, Colors.withAlpha(-7496, 255)).clip(0.375F, 0.375F));
      this.lidGlow.rotate(0.0F, 90.0F, 0.0F).at(0.0F, 0.0F, 0.008F).attachTo(this.face);
      this.lidGlow.alpha(0.0F);
      ((PivotNode)stage.add(this.floater)).at(0.0F, 0.07F, 0.2F).attachTo(this.face);
      Component name = (Component)(vanilla ? lidLabel : Fonts.heading(lidLabel));
      this.label = (TextNode)stage.add(new TextNode(name));
      int nameW = Math.max(1, Minecraft.getInstance().font.width(name));
      this.label.billboard(false).background(0).shadow(true).size(Math.min(0.0128F, 0.62F / (float)nameW)).color(this.lidLocked ? -6645616 : -1);
      this.label.at(0.0F, -0.275F, 0.014F).attachTo(this.face);
      this.label.alpha(0.0F);
      boolean last = turn >= 2;
      this.sparks = (BurstNode)stage.add(new BurstNode(last ? 44 : 26, last ? 0.8F : 0.62F, 0.05F, Colors.withAlpha(Colors.lerp(-7496, Theme.current().accent(), 0.35F), 224), 11L + (long)index));
      this.sparks.at(0.0F, 0.6F, 0.0F).attachTo(this.root);
      ((HitNode)stage.add(this.lidHit)).bounds(-0.4375F, -0.44F, -0.06F, 0.4375F, 0.44F, 0.64F).named(lidLabel).attachTo(this.face);
      ((HitNode)stage.add(this.signHit)).bounds(-0.5F, 0.0F, -0.44F, 0.5F, 0.56F, 0.56F).named(signLabel).attachTo(this.root);
      this.lidHit.onHover((h) -> {
         this.lidHover.set(h);
         if (this.lidListener != null && h) {
            this.lidListener.hover(this.lidHover.get(), true);
         }

      });
      HitNode var10000 = this.signHit;
      Anim var10001 = this.signHover;
      Objects.requireNonNull(var10001);
      var10000.onHover(var10001::set);
      if (this.lidLocked) {
         this.lidHit.tooltip(Features.lockedTooltip(lidModule)).onClick(() -> locked(lidModule));
      } else {
         this.lidHit.onClick(lidAction);
      }

      if (this.signLocked) {
         this.signHit.tooltip(Features.lockedTooltip(signModule)).onClick(() -> locked(signModule));
      } else {
         this.signHit.onClick(signAction);
      }

      this.lidHit.pickable(false);
      if (!signShown) {
         this.signPivot.visible(false);
         this.signHit.visible(false);
         this.signHit.pickable(false);
      }

      if (!lidShown) {
         this.lidHit.visible(false);
      }

      this.root.onFrame(this::frame);
   }

   <T extends StageNode> T show(Stage stage, T node) {
      stage.add(node);
      node.pickable(false).hoverFeel(0.0F, 1.0F).attachTo(this.floater);
      if (this.lidLocked) {
         node.muted(1.0F);
      }

      return node;
   }

   ChestButton seenFrom(Vector3f camera) {
      this.eye = new Vector3f(camera);
      return this;
   }

   ChestButton onLidHover(HoverListener listener) {
      this.lidListener = listener;
      return this;
   }

   boolean lidLocked() {
      return this.lidLocked;
   }

   boolean signLocked() {
      return this.signLocked;
   }

   void signTip(@Nullable Component tip) {
      if (!this.signLocked && this.signShown) {
         this.signHit.tooltip(tip);
      }

   }

   void disableSign(Component why) {
      if (!this.signLocked && this.signShown) {
         this.signHit.onClick((Runnable)null);
         this.signHit.pickable(true);
         this.signHit.tooltip(why);
         this.signText.color(this.inkLocked);
         this.signDisabled = true;
      }
   }

   void enableSign(Runnable action) {
      if (!this.signLocked && this.signShown) {
         this.signDisabled = false;
         this.signHit.onClick(action);
      }
   }

   private static void locked(String module) {
      CFHGuiToasts.show(KnownModules.name(module), Features.lockedTooltip(module), Icon.LOCK);
   }

   void finish() {
      if (!this.opened) {
         this.opened = true;
      }

      if (this.lidShown) {
         this.chest.openNow(true);
         this.pop.snap(1.0F);
         this.labelIn.snap(1.0F);
      }
   }

   boolean settled() {
      if (!this.lidShown) {
         return true;
      } else {
         return this.opened && this.pop.get() >= 0.999F && this.chest.openness() >= 0.999F && this.chest.openness() <= 1.001F;
      }
   }

   private void frame(StageRenderContext ctx) {
      float motion = Theme.current().motion();
      if (motion <= 0.0F) {
         this.finish();
      } else if (this.lidShown) {
         float t = ctx.timeMs / motion;
         if (!this.opened && t >= this.openAtMs) {
            this.opened = true;
            this.chest.open(true);
            this.sparks.fire();
            if (this.sounds && Theme.current().uiSounds()) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHEST_OPEN, 0.95F + 0.1F * (float)this.turn, this.turn >= 2 ? 0.4F : 0.32F));
            }
         }

         if (this.opened && t >= this.openAtMs + 380.0F) {
            this.pop.set(1.0F);
         }

         if (this.opened && t >= this.openAtMs + 560.0F) {
            this.labelIn.set(1.0F);
         }
      }

      float shown = Mth.clamp(this.pop.get(), 0.0F, 1.2F);
      float hover = this.lidLocked ? 0.0F : this.lidHover.get();
      float bob = motion > 0.0F ? Mth.sin(ctx.seconds() * 1.5F + this.bobPhase) * 0.012F * Math.min(1.0F, shown) : 0.0F;
      this.floater.visible(shown > 0.01F);
      this.floater.scale(Math.max(0.001F, shown) * (1.0F + 0.06F * hover));
      float out = 0.2F + 0.03F * hover;
      float up = 0.07F + bob * 0.6F;
      float aside = 0.0F;
      float lift = 0.0F;
      if (this.eye != null) {
         this.face.modelInverse.transformPosition(this.eye, this.seen);
         if (this.seen.z > 1.0F) {
            aside = this.seen.x / this.seen.z * out;
            lift = (this.seen.y - up) / this.seen.z * out;
         }
      }

      this.floater.at(aside, up + lift, out);
      if (this.lidListener != null) {
         this.lidListener.hover(hover, false);
      }

      this.lidHit.pickable(shown > 0.6F);
      float in = this.labelIn.get();
      this.label.alpha(in);
      this.label.visible(in > 0.01F);
      if (!this.lidLocked) {
         this.label.color(Colors.lerp(-1, Colors.lerp(-1, Theme.current().accent(), 0.85F), hover));
      }

      float lit = Math.min(1.0F, shown) * (this.lidLocked ? 0.04F : 0.07F + 0.2F * hover);
      this.lidGlow.alpha(lit);
      this.lidGlow.visible(lit > 0.01F);
      float sh = !this.signLocked && !this.signDisabled ? this.signHover.get() : this.signHover.get() * 0.25F;
      this.signPivot.scale(0.75F * (1.0F + 0.06F * sh));
      this.signPivot.at(0.0F, 0.09375F - 0.0112499995F * sh, 0.4475F + 0.04F * sh);
      if (!this.signLocked && !this.signDisabled) {
         this.signText.color(Colors.lerp(this.ink, -1, sh));
      }

   }

   static FxNode pool(Stage stage, Anchor anchor, int argb, boolean standing) {
      FxNode glow = (FxNode)stage.add(FxNode.glow(2.3F, argb));
      glow.at(anchor.x(), anchor.y() + 0.004F, anchor.z() + 0.3F);
      if (standing) {
         ((FxNode)stage.add(FxNode.glow(0.95F, -1342177280))).at(anchor.x(), anchor.y() + 0.008F, anchor.z() + 0.05F);
      }

      return glow;
   }

   static enum Look {
      PAINTED,
      VANILLA;
   }

   interface HoverListener {
      void hover(float var1, boolean var2);
   }
}
