package com.onehumanawa.cfhcore.content.gui.veinminer.server;

import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class VeinJob {
	private static final double MAX_DISTANCE_SQR = 9216.0;
	private static final int MAX_REFUSALS = 3;
	final ServerPlayer player;
	final ServerLevel level;
	final BlockPos origin;
	private final long[] positions;
	private final Block[] expected;
	private final int slot;
	private final Item item;
	private int next;
	private int refusals;
	int broken;
	VeinJob.Stop stop = VeinJob.Stop.NONE;

	VeinJob(ServerPlayer player, ServerLevel level, BlockPos origin, long[] positions, Block[] expected, int slot, Item item) {
		this.player = player;
		this.level = level;
		this.origin = origin;
		this.positions = positions;
		this.expected = expected;
		this.slot = slot;
		this.item = item;
	}

	boolean done() {
		return this.next >= this.positions.length || this.stop != VeinJob.Stop.NONE;
	}

	int total() {
		return this.positions.length;
	}

	int run(int budget, VeinRules rules) {
		int went;
		for (went = 0; went < budget && !this.done(); went++) {
			this.step(rules);
		}

		return went;
	}

	private void step(VeinRules rules) {
		if (!this.player.isRemoved() && !this.player.hasDisconnected() && this.player.isAlive() && !this.player.isSpectator() && this.player.level() == this.level) {
			ItemStack held = this.player.getMainHandItem();
			if (this.player.getInventory().selected == this.slot && held.getItem() == this.item) {
				if (this.player.distanceToSqr((double)this.origin.getX() + 0.5, (double)this.origin.getY() + 0.5, (double)this.origin.getZ() + 0.5) > 9216.0) {
					this.stop = VeinJob.Stop.CHANGED;
				} else {
					int i = this.next++;
					BlockPos pos = BlockPos.of(this.positions[i]);
					BlockState state = this.level.getBlockState(pos);
					if (state.getBlock() == this.expected[i]) {
						if (this.level.mayInteract(this.player, pos) && this.level.getWorldBorder().isWithinBounds(pos)) {
							if (VeinFinder.check(this.level, pos, state, rules, VeinPlanner.harvest(this.player, rules)) == VeinFinder.Reason.OK) {
								boolean free = VeinPlanner.free(this.player);
								if (!free) {
									if (rules.stopBeforeToolBreaks && held.isDamageableItem() && held.getMaxDamage() - held.getDamageValue() <= 1) {
										this.stop = VeinJob.Stop.TOOL;
										return;
									}

									if (VeinPlanner.hungry(this.player, rules)) {
										this.stop = VeinJob.Stop.HUNGRY;
										return;
									}
								}

								boolean ok = VeinServer.breakExtra(this, pos, rules);
								if (!ok) {
									if (++this.refusals >= 3) {
										this.stop = VeinJob.Stop.PROTECTED;
									}
								} else {
									this.refusals = 0;
									this.broken++;
									if (!free && rules.exhaustionPerBlock > 0.0F) {
										this.player.causeFoodExhaustion(rules.exhaustionPerBlock);
									}

									this.player.connection.send(new ClientboundLevelEventPacket(2001, pos, Block.getId(state), false));
								}
							}
						}
					}
				}
			} else {
				this.stop = VeinJob.Stop.CHANGED;
			}
		} else {
			this.stop = VeinJob.Stop.CHANGED;
		}
	}

	Component stopMessage() {
		return switch (this.stop) {
			case TOOL -> Component.translatable("cfhgui_veinminer.stopped.tool", new Object[]{this.broken + 1});
			case HUNGRY -> Component.translatable("cfhgui_veinminer.stopped.hungry", new Object[]{this.broken + 1});
			default -> null;
			case PROTECTED -> Component.translatable("cfhgui_veinminer.stopped.protected", new Object[]{this.broken + 1});
		};
	}

	static enum Stop {
		NONE,
		TOOL,
		HUNGRY,
		CHANGED,
		PROTECTED;
	}
}
