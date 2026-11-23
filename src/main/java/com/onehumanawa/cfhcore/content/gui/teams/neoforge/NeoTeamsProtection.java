package com.onehumanawa.cfhcore.content.gui.teams.neoforge;

import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent;
import net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent.Detonate;

final class NeoTeamsProtection {
	static void register() {
		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener(EventPriority.HIGH, BreakEvent.class, NeoTeamsProtection::onBreak);
		bus.addListener(EventPriority.HIGH, EntityPlaceEvent.class, NeoTeamsProtection::onPlace);
		bus.addListener(EventPriority.HIGH, RightClickBlock.class, NeoTeamsProtection::onUseBlock);
		bus.addListener(EventPriority.HIGH, LeftClickBlock.class, NeoTeamsProtection::onHitBlock);
		bus.addListener(EventPriority.HIGH, RightClickItem.class, NeoTeamsProtection::onUseItem);
		bus.addListener(EventPriority.HIGH, EntityInteract.class, NeoTeamsProtection::onUseEntity);
		bus.addListener(EventPriority.HIGH, EntityInteractSpecific.class, NeoTeamsProtection::onUseEntityAt);
		bus.addListener(EventPriority.HIGH, AttackEntityEvent.class, NeoTeamsProtection::onAttackEntity);
		bus.addListener(EventPriority.HIGH, Detonate.class, NeoTeamsProtection::onDetonate);
		bus.addListener(EventPriority.HIGH, EntityMobGriefingEvent.class, NeoTeamsProtection::onMobGriefing);
		bus.addListener(EventPriority.HIGH, LivingDestroyBlockEvent.class, NeoTeamsProtection::onMobDestroysBlock);
		bus.addListener(EventPriority.HIGH, FarmlandTrampleEvent.class, NeoTeamsProtection::onTrample);
		bus.addListener(EventPriority.HIGH, FluidPlaceBlockEvent.class, NeoTeamsProtection::onFluidPlacesBlock);
	}

	private static void onBreak(BreakEvent e) {
		if (e.getLevel() instanceof Level level && LandGuard.breakRefused(e.getPlayer(), level, e.getPos())) {
			e.setCanceled(true);
		}
	}

	private static void onPlace(EntityPlaceEvent e) {
		if (e.getLevel() instanceof Level level) {
			if (e instanceof EntityMultiPlaceEvent multi) {
				for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
					if (LandGuard.placeRefused(e.getEntity(), level, snapshot.getPos())) {
						e.setCanceled(true);
						return;
					}
				}
			} else {
				if (LandGuard.placeRefused(e.getEntity(), level, e.getPos())) {
					e.setCanceled(true);
				}
			}
		}
	}

	private static void onUseBlock(RightClickBlock e) {
		Player player = e.getEntity();
		int refused = LandGuard.useBlock(player, e.getLevel(), e.getHand(), e.getHitVec());
		if (refused != 0) {
			if ((refused & 1) != 0) {
				e.setUseBlock(TriState.FALSE);
				if (player.isSpectator()) {
					e.setCanceled(true);
				}
			}

			if ((refused & 2) != 0) {
				e.setUseItem(TriState.FALSE);
			}
		}
	}

	private static void onHitBlock(LeftClickBlock e) {
		if (e.getAction() != Action.ABORT) {
			if (LandGuard.attackBlockRefused(e.getEntity(), e.getLevel(), e.getPos())) {
				e.setCanceled(true);
			}
		}
	}

	private static void onUseItem(RightClickItem e) {
		if (LandGuard.useItemRefused(e.getEntity(), e.getLevel(), e.getHand())) {
			e.setCancellationResult(InteractionResult.FAIL);
			e.setCanceled(true);
		}
	}

	private static void onUseEntity(EntityInteract e) {
		if (LandGuard.useEntityRefused(e.getEntity(), e.getTarget())) {
			e.setCancellationResult(InteractionResult.FAIL);
			e.setCanceled(true);
		}
	}

	private static void onUseEntityAt(EntityInteractSpecific e) {
		if (LandGuard.useEntityRefused(e.getEntity(), e.getTarget())) {
			e.setCancellationResult(InteractionResult.FAIL);
			e.setCanceled(true);
		}
	}

	private static void onAttackEntity(AttackEntityEvent e) {
		if (LandGuard.attackEntityRefused(e.getEntity(), e.getTarget())) {
			e.setCanceled(true);
		}
	}

	private static void onDetonate(Detonate e) {
		LandGuard.filterExplosion(e.getLevel(), e.getAffectedBlocks(), e.getAffectedEntities());
	}

	private static void onMobGriefing(EntityMobGriefingEvent e) {
		Entity entity = e.getEntity();
		if (entity != null && e.canGrief() && !entity.level().isClientSide()) {
			if (!(entity instanceof Zombie)
				&& !(entity instanceof AbstractPiglin)
				&& !(entity instanceof Creeper)
				&& !(entity instanceof LargeFireball)
				&& !(entity instanceof WitherSkull)) {
				if (LandGuard.monsterGriefRefused(entity)) {
					e.setCanGrief(false);
				}
			}
		}
	}

	private static void onMobDestroysBlock(LivingDestroyBlockEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			if (LandGuard.mobGriefRefused(e.getEntity().level(), e.getPos())) {
				e.setCanceled(true);
			}
		}
	}

	private static void onTrample(FarmlandTrampleEvent e) {
		if (e.getLevel() instanceof Level level && LandGuard.trampleRefused(level, e.getPos(), e.getEntity())) {
			e.setCanceled(true);
		}
	}

	private static void onFluidPlacesBlock(FluidPlaceBlockEvent e) {
		if (e.getLevel() instanceof Level level) {
			if (e.getNewState().getBlock() instanceof BaseFireBlock
				? LandGuard.fireRefused(level, e.getPos())
				: LandGuard.reachRefused(level, e.getLiquidPos(), e.getPos())) {
				e.setCanceled(true);
			}
		}
	}

	private NeoTeamsProtection() {
	}
}
