package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.CubePlanetNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.HitNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.ItemNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

final class Ring {
   static final float RADIUS = 3.3F;
   static final float MINI_RADIUS = 0.95F;
   static final float PLANET = 0.86F;
   static final float MINI_PLANET = 0.25F;
   static final float MINI_LIFT = 0.14F;
   private static final float BACK_SIZE = 0.44F;
   private static final float BACK_STEP = 0.42F;
   private static final float FRONT_GROWS = 0.102F;
   private static final float HOVER_GROWS = 0.15F;
   private static final float HOVER_LIFTS = 0.06F;
   private static final float RIFT = 0.62F;
   private static final float TILT = 4.0F;
   private static final float STIFFNESS = 64.0F;
   private static final float DAMPING = 16.0F;
   private static final int GROW_PER_FRAME = 2;
   final boolean servers;
   final PivotNode pivot = new PivotNode();
   final HitNode miniHit = new HitNode();
   private final OrbitNode orbit;
   private final RiftNode rift;
   private float riftShown;
   private final Stage stage;
   private final Anim active;
   private final Anim miniHover;
   private boolean hoverRaw;
   private boolean hoverArmed;
   private final PlusNode plus;
   private final int pointedLight;
   private boolean offerNew;
   private float plusShown;
   private List<RingEntry> shown;
   private int selected;
   private float position;
   private float target;
   private float velocity;
   private float idle;
   private int capacity;
   private @Nullable Consumer<RingEntry> onPick;

   Ring(Stage stage, boolean servers, boolean startsActive, Runnable onMini, Component miniTip, Runnable onNew, Component newTip) {
      this.miniHover = new Anim(0.0F, 180, Ease.OUT_CUBIC);
      this.hoverArmed = true;
      this.shown = List.of();
      this.capacity = 11;
      this.stage = stage;
      this.servers = servers;
      this.active = new Anim(startsActive ? 1.0F : 0.0F, 680, Ease.IN_OUT_CUBIC);
      stage.add(this.pivot);
      Theme theme = Theme.current();
      this.pointedLight = Colors.withAlpha(theme.accentInk(), 240);
      this.orbit = (OrbitNode)stage.add(new OrbitNode(1.0F, 0.012F, Colors.withAlpha(Colors.lerp(-5688, theme.accent(), theme.isVanilla() ? 0.0F : 0.4F), 112)));
      this.orbit.attachTo(this.pivot);
      int warm = Colors.lerp(-5688, theme.accent(), theme.isVanilla() ? 0.0F : 0.25F);
      this.rift = (RiftNode)stage.add(new RiftNode(0.62F, warm, theme.isVanilla() ? -1 : theme.accent(), servers ? 41L : 17L));
      this.rift.attachTo(this.pivot);
      stage.add(this.miniHit);
      this.miniHit.bounds(-1.25F, -0.3F, -1.25F, 1.25F, 0.35F, 1.25F);
      this.miniHit.named(miniTip).onClick(onMini).onHover((h) -> this.hoverRaw = h);
      this.miniHit.attachTo(this.pivot);
      this.plus = (PlusNode)stage.add(new PlusNode(theme.isVanilla() ? -1513240 : Colors.lerp(theme.accent(), -5688, 0.2F)));
      this.plus.named(newTip).tooltip(newTip).onClick(onNew);
      this.plus.attachTo(this.pivot);
      this.plus.visible(false);
      this.pivot.onFrame(this::frame);
   }

   void offerNew(boolean on) {
      this.offerNew = on;
   }

   float miniHover() {
      return this.miniHover.get();
   }

   void onPick(Consumer<RingEntry> listener) {
      this.onPick = listener;
   }

   void capacity(int n) {
      this.capacity = Mth.clamp(n, 4, 24);
   }

   List<RingEntry> shown() {
      return this.shown;
   }

   int selectedIndex() {
      return this.selected;
   }

   @Nullable RingEntry selected() {
      return this.shown.isEmpty() ? null : (RingEntry)this.shown.get(Mth.clamp(this.selected, 0, this.shown.size() - 1));
   }

   boolean isActive() {
      return this.active.target() > 0.5F;
   }

   float activeAmount() {
      return this.active.get();
   }

   void active(boolean on) {
      this.active.set(on);
      if (!on) {
         this.hoverArmed = false;
      }

   }

   void armHover() {
      this.hoverArmed = true;
   }

   void activeNow(boolean on) {
      this.active.snap(on ? 1.0F : 0.0F);
   }

   void show(List<RingEntry> entries, @Nullable String keep) {
      for(RingEntry e : this.shown) {
         if (!entries.contains(e)) {
            this.release(e);
         }
      }

      this.shown = new ArrayList(entries);
      this.selected = 0;
      if (keep != null) {
         for(int i = 0; i < this.shown.size(); ++i) {
            if (((RingEntry)this.shown.get(i)).id().equals(keep)) {
               this.selected = i;
               break;
            }
         }
      }

      this.position = this.target = (float)this.selected;
      this.velocity = 0.0F;
   }

   void select(int index) {
      int m = this.shown.size();
      if (m != 0) {
         this.selected = Math.floorMod(index, m);
         this.target = this.position + wrap((float)this.selected - this.position, m);
      }
   }

   void select(RingEntry entry) {
      int i = this.shown.indexOf(entry);
      if (i >= 0) {
         this.select(i);
      }

   }

   void step(int by) {
      if (!this.shown.isEmpty()) {
         this.select(this.selected + by);
      }

   }

   private static float round(float u, float half) {
      float x = Math.min(u, half);
      return (float)Math.PI * (x - 0.58000004F * x * x / (2.0F * half)) / (half * 1.42F / 2.0F);
   }

   private static float wrap(float a, int m) {
      if (m <= 0) {
         return 0.0F;
      } else {
         float r = a % (float)m;
         if (r < 0.0F) {
            r += (float)m;
         }

         if (r > (float)m / 2.0F) {
            r -= (float)m;
         }

         return r;
      }
   }

   private void frame(StageRenderContext ctx) {
      int m = this.shown.size();
      float motion = Theme.current().motion();
      if (motion <= 0.0F) {
         this.position = this.target;
         this.velocity = 0.0F;
      } else {
         float dt;
         for(float left = Math.min(0.1F, ctx.deltaMs / 1000.0F) / motion; left > 0.0F; left -= dt) {
            dt = Math.min(left, 0.008333334F);
            this.velocity += (64.0F * (this.target - this.position) - 16.0F * this.velocity) * dt;
            this.position += this.velocity * dt;
         }

         this.idle += ctx.deltaMs / 1000.0F * 0.22F;
      }

      float act = this.active.get();
      this.pivot.at(0.0F, Mth.lerp(act, 0.14F, 0.0F), 0.0F);
      this.miniHit.pickable(act < 0.04F);
      this.miniHover.set(this.hoverRaw && this.hoverArmed);
      float hover = this.miniHover.get();
      float miniGrow = 1.0F + 0.14F * hover * (1.0F - act);
      float radius = Mth.lerp(act, 0.95F * miniGrow, 3.3F);
      this.orbit.scale(radius);
      float breath = motion > 0.0F ? 0.5F + 0.5F * Mth.sin(ctx.seconds() * 2.1F) : 1.0F;
      this.orbit.alpha(Mth.lerp(act, 0.4F + 0.16F * breath + 0.44F * hover, m == 0 ? 0.45F : 1.0F));
      float wantPlus = this.offerNew && m == 0 ? act : 0.0F;
      this.plusShown = motion <= 0.0F ? wantPlus : this.plusShown + (wantPlus - this.plusShown) * Math.min(1.0F, ctx.deltaMs / 220.0F);
      this.plus.visible(this.plusShown > 0.01F);
      this.plus.pickable(this.plusShown > 0.9F && act > 0.96F);
      if (this.plusShown > 0.01F) {
         float bob = motion > 0.0F ? Mth.sin(ctx.seconds() * 0.9F) * 0.045F : 0.0F;
         this.plus.at(0.0F, 0.16F + bob, radius);
         this.plus.scale(0.94772F * this.plusShown);
      }

      int window = Math.min(m, this.capacity);
      boolean more = m > this.capacity;
      float half = (float)Math.max(window, 6) / 2.0F + 0.5F;
      float even = m <= 1 ? 0.0F : ((float)Math.PI * 2F) / (float)(more ? Math.max(window, 6) : m);
      float want = more ? act : 0.0F;
      this.riftShown = motion <= 0.0F ? want : this.riftShown + (want - this.riftShown) * Math.min(1.0F, ctx.deltaMs / 260.0F);
      this.rift.at(0.0F, 0.2F, -radius);
      this.rift.strength(this.riftShown);
      this.rift.visible(this.riftShown > 0.01F);
      int grown = 0;

      for(int i = 0; i < m; ++i) {
         RingEntry e = (RingEntry)this.shown.get(i);
         float d = wrap((float)i - this.position, m);
         float u = Math.abs(d);
         float edge = more ? Mth.clamp(half - u, 0.0F, 1.0F) : 1.0F;
         if (edge <= 0.001F) {
            if (e.holder != null) {
               if (u > half + 1.5F) {
                  this.release(e);
               } else {
                  e.holder.visible(false);
               }
            }
         } else {
            if (e.holder == null) {
               if (grown >= 2 && u > 0.5F) {
                  continue;
               }

               this.grow(e);
               ++grown;
            }

            PivotNode holder = e.holder;
            CubePlanetNode planet = e.planet;
            if (holder != null && planet != null) {
               e.shown = motion <= 0.0F ? 1.0F : Math.min(1.0F, e.shown + ctx.deltaMs / (260.0F * motion));
               float born = Ease.OUT_CUBIC.apply(e.shown);
               float turn = Math.signum(d) * (more ? Mth.lerp(act, u * even, round(u, half)) : u * even);
               float angle = turn + this.idle * (1.0F - act);
               float depth = (1.0F + Mth.cos(turn)) / 2.0F;
               float front = Math.max(0.0F, 1.0F - u);
               float pointed = planet.hover() * act;
               float full = 0.86F * Mth.lerp(depth, 0.44F, 1.0F) * (1.0F + 0.102F * front) * (1.0F + 0.15F * (1.0F - 0.34F * front) * pointed);
               float bob = motion > 0.0F ? Mth.sin(ctx.seconds() * 0.9F + (float)i * 1.9F) * 0.045F * act : 0.0F;
               holder.visible(true);
               holder.at(Mth.sin(angle) * radius, bob + 0.16F * front * act + 0.2F * (1.0F - edge) + 0.06F * full * pointed, Mth.cos(angle) * radius);
               if (e.halo != null) {
                  e.halo.visible(pointed > 0.01F);
                  e.halo.alpha(pointed);
                  e.halo.scale(0.82F + 0.18F * pointed);
               }

               holder.scale(Math.max(0.001F, Mth.lerp(act, 0.25F * miniGrow, full) * born * (0.2F + 0.8F * edge)));
               holder.alpha(edge);
               planet.spin(5.0F + 11.0F * front);
               planet.clouds(act > 0.45F);
               planet.pickable(act > 0.96F && edge > 0.5F);
               planet.tooltip(front > 0.5F ? null : Component.literal(e.name()));
               if (e instanceof RingEntry.Server) {
                  RingEntry.Server s = (RingEntry.Server)e;
                  planet.ring(s.statusColor());
               }

               if (e.star != null) {
                  e.star.visible(e.favorite() && act > 0.45F);
               }
            }
         }
      }

   }

   private void grow(RingEntry e) {
      PivotNode holder = (PivotNode)this.stage.add(new PivotNode());
      holder.rotate(0.0F, 4.0F, 0.0F).attachTo(this.pivot);
      CubePlanetNode planet = (CubePlanetNode)this.stage.add(e.grow());
      planet.size(1.0F);
      planet.yaw((float)Math.floorMod(CubePlanetNode.seedOf(e.id()), 360L));
      planet.named(e.name()).attachTo(holder);
      planet.onClick(() -> {
         if (this.onPick != null) {
            this.onPick.accept(e);
         }

      });
      ItemNode star = (ItemNode)this.stage.add(new ItemNode(new ItemStack(Items.NETHER_STAR)));
      star.context(ItemDisplayContext.GROUND).size(0.62F).spin(50.0F).bob(0.03F);
      star.pickable(false).at(0.0F, 0.86F, 0.0F).attachTo(holder);
      star.visible(e.favorite());
      HaloNode halo = (HaloNode)this.stage.add(new HaloNode(this.pointedLight));
      halo.at(0.0F, -0.68F, 0.0F).attachTo(holder);
      halo.spin(14.0F);
      halo.visible(false);
      e.holder = holder;
      e.planet = planet;
      e.star = star;
      e.halo = halo;
      e.shown = 0.0F;
   }

   private void release(RingEntry e) {
      if (e.planet != null) {
         this.stage.remove(e.planet);
      }

      if (e.star != null) {
         this.stage.remove(e.star);
      }

      if (e.halo != null) {
         this.stage.remove(e.halo);
      }

      if (e.holder != null) {
         this.stage.remove(e.holder);
      }

      e.planet = null;
      e.star = null;
      e.halo = null;
      e.holder = null;
      e.play.snap(0.0F);
   }

   void clear() {
      for(RingEntry e : this.shown) {
         this.release(e);
      }

      this.shown = List.of();
      this.selected = 0;
      this.position = this.target = this.velocity = 0.0F;
   }
}
