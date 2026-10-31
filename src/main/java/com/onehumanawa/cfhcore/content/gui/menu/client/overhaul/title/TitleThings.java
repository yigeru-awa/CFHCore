package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlocksNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.CubePlanetNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ModelNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.CubePlanetNode.Kind;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Part;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.BlockGear;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class TitleThings {
   private static final ResourceLocation CREATE_COG = ResourceLocation.fromNamespaceAndPath("create", "block/large_cogwheel");
   private static final ResourceLocation SLATE_COG = CFHGuiMenu.id("stage/large_cogwheel");
   private static final long PLAY_SEED = 20260929L;
   private static final float HEAD_TURN = 18.0F;
   private static final float HEAD_NOD = 7.0F;

   private TitleThings() {
   }

   static ChestButton.HoverListener play(Stage s, PivotNode holder, boolean onLid) {
      return TitlePlayIcon.of(CFHGuiMenu.config().titlePlayIcon) == TitlePlayIcon.PLANET ? planet(s, holder) : block(s, holder, onLid);
   }

   static ChestButton.HoverListener block(Stage s, PivotNode holder, boolean onLid) {
      PivotNode tilt = new PivotNode();
      PlayBlock block = (PlayBlock)s.add(onLid ? new PlayBlock(tilt, 0.16F, 0.01F) : new PlayBlock(tilt, 0.09F, 0.0F));
      ((PivotNode)s.add(tilt)).rotate(0.0F, 24.0F, 0.0F).attachTo(holder);
      block.attachTo(tilt);
      return (amount, entered) -> block.pointedAt(amount);
   }

   static ChestButton.HoverListener planet(Stage s, PivotNode holder) {
      PivotNode tilt = (PivotNode)s.add(new PivotNode());
      tilt.rotate(0.0F, 24.0F, 0.0F).attachTo(holder);
      CubePlanetNode planet = (CubePlanetNode)s.add(CubePlanetNode.create(20260929L, Kind.TEMPERATE));
      planet.size(0.29012346F).pickable(false).attachTo(tilt);
      planet.spin(16.0F);
      planet.yaw(35.0F);
      float[] spin = new float[]{16.0F};
      return (amount, entered) -> {
         float dt = Clock.frameDelta() / 1000.0F;
         spin[0] = Mth.lerp(Math.min(1.0F, dt * 6.0F), spin[0], 16.0F + 84.0F * amount);
         planet.spin(spin[0]);
         planet.cloudSpeed(-7.0F - 38.0F * amount);
      };
   }

   static ChestButton.HoverListener head(Stage s, PivotNode holder, boolean locked) {
      PivotNode turn = (PivotNode)s.add(new PivotNode());
      turn.attachTo(holder);
      PlayerNode me = (PlayerNode)s.add(PlayerNode.local());
      me.show(EnumSet.of(Part.HEAD)).lookAtCursor(false).breathe(false);
      float head = 0.66197187F;
      me.scale(0.66197187F).at(0.0F, -1.1584507F, 0.0F).pickable(false).hoverFeel(0.0F, 1.0F).attachTo(turn);
      if (locked) {
         me.muted(1.0F);
      }

      long began = Clock.nowMs();
      return (amount, entered) -> {
         float sway = Theme.current().motion() > 0.0F ? Mth.sin((float)(Clock.nowMs() - began) / 1000.0F * 0.9F) : -0.8F;
         turn.rotate(18.0F * sway * (1.0F - amount), 7.0F * amount, 0.0F);
      };
   }

   static ChestButton.HoverListener cog(Stage s, PivotNode holder, boolean locked) {
      boolean create = CFHGuiPlatform.get().isModLoaded("create");
      PivotNode lean = (PivotNode)s.add(new PivotNode());
      lean.rotate(-24.0F, 58.0F, 0.0F).scale(0.2611111F);
      lean.attachTo(holder);
      if (!create) {
         BlocksNode gear = BlockGear.build(s.level(), true, false);
         s.add(gear);
         gear.scale(BlockGear.scale(true));
         gear.pickable(false).hoverFeel(0.0F, 1.0F).attachTo(lean);
         if (locked) {
            gear.muted(1.0F);
         }

         float[] boost = new float[]{1.0F};
         lean.onFrame((ctx) -> {
            if (boost[0] != 1.0F) {
               boost[0] += (1.0F - boost[0]) * (1.0F - (float)Math.exp((double)(-ctx.deltaMs / 400.0F)));
            }

            gear.spin(22.0F * boost[0]);
         });
         gear.spin(22.0F);
         return (amount, entered) -> {
            if (entered) {
               boost[0] = 7.0F;
            }

         };
      } else {
         ModelNode cog = (ModelNode)s.add(create ? (new ModelNode(CREATE_COG)).fallback(SLATE_COG) : new ModelNode(SLATE_COG));
         cog.lamp(PlayBlock.LAMP, 0.34F, 1.0F);
         cog.pickable(false).hoverFeel(0.0F, 1.0F).attachTo(lean);
         if (locked) {
            cog.muted(1.0F);
         }

         cog.spin(22.0F);
         cog.at(0.0F, -0.5F, 0.0F);
         return (amount, entered) -> {
            if (entered) {
               cog.boostSpin(7.0F);
            }

         };
      }
   }
}
