package com.onehumanawa.cfhcore.content.gui.veinminer.server;

import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinPlanner;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class VeinExtras {
	public static final VeinExtras.Use HANDLED = new VeinExtras.Use(null, null, InteractionHand.MAIN_HAND, null, Direction.UP, Vec3.ZERO, List.of());
	private static boolean busy;

	static boolean busy() {
		return busy;
	}

	@Nullable
	public static VeinExtras.Use before(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit) {
		if (!busy && level instanceof ServerLevel server && !player.isSpectator() && VeinServer.active(player)) {
			VeinRules rules = VeinRules.local();
			if (!VeinServer.allowed(player, rules)) {
				return null;
			}

			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (rules.harvestCrops && hand == InteractionHand.MAIN_HAND && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
				harvest(player, server, pos, crop, rules);
				return HANDLED;
			}

			if (rules.toolActions && isTool(stack.getItem())) {
				if (VeinFinder.check(level, pos, state, rules, null) != VeinFinder.Reason.OK) {
					return null;
				}

				List<BlockPos> group = stack.getItem() instanceof AxeItem
					? vein(server, pos, state, player, rules, hit.getDirection())
					: ground(server, pos, state, rules.maxBlocks, VeinServer.diagonals(player, rules, false), rules);
				if (group.size() <= 1) {
					return null;
				}

				return new VeinExtras.Use(player, server, hand, stack.getItem(), hit.getDirection(), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)), group);
			}

			return null;
		}

		return null;
	}

	public static void after(VeinExtras.Use use, InteractionResult result) {
		if (use != HANDLED && result.consumesAction()) {
			VeinRules rules = VeinRules.local();
			ServerPlayer player = use.player;
			boolean free = VeinPlanner.free(player);
			int done = 0;
			busy = true;

			try {
				for (int i = 1; i < use.group.size(); i++) {
					ItemStack held = player.getItemInHand(use.hand);
					if (held.getItem() != use.item
						|| player.isRemoved()
						|| !free
							&& (rules.stopBeforeToolBreaks && held.isDamageableItem() && held.getMaxDamage() - held.getDamageValue() <= 1 || VeinPlanner.hungry(player, rules))) {
						break;
					}

					BlockPos pos = use.group.get(i);
					if (use.level.mayInteract(player, pos)) {
						BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(use.offset), use.face, pos, false);
						if (player.gameMode.useItemOn(player, use.level, held, use.hand, hit).consumesAction()) {
							done++;
							if (!free && rules.exhaustionPerBlock > 0.0F) {
								player.causeFoodExhaustion(rules.exhaustionPerBlock);
							}
						}
					}
				}
			} finally {
				busy = false;
			}

			log(player, "used " + use.item + " on", done + 1);
		}
	}

	static boolean isTool(Item item) {
		return item instanceof AxeItem || item instanceof HoeItem || item instanceof ShovelItem;
	}

	static int harvest(ServerPlayer player, ServerLevel level, BlockPos start, CropBlock crop, VeinRules rules) {
		List<BlockPos> field = ripe(level, start, crop, rules.maxBlocks, VeinServer.diagonals(player, rules, false));
		boolean free = VeinPlanner.free(player);
		int done = 0;
		int refused = 0;
		busy = true;

		try {
			for (BlockPos pos : field) {
				if (done > 0 && !free && VeinPlanner.hungry(player, rules)) {
					break;
				}

				BlockState state = level.getBlockState(pos);
				if (state.getBlock() == crop && crop.isMaxAge(state) && level.mayInteract(player, pos)) {
					Item seed = crop.getCloneItemStack(level, pos, state).getItem();
					List<ItemStack> drops = new ArrayList<>();
					DropCollector.collect(level, drops);

					boolean broken;
					try {
						broken = player.gameMode.destroyBlock(pos);
					} finally {
						DropCollector.end();
					}

					if (!broken) {
						if (++refused >= 3) {
							break;
						}
					} else {
						refused = 0;
						done++;
						replant(player, level, pos, crop, seed, drops, free);
						DropCollector.begin(level, player, start, rules.drops);

						try {
							for (ItemStack d : drops) {
								if (!d.isEmpty()) {
									Block.popResource(level, pos, d);
								}
							}
						} finally {
							DropCollector.end();
						}

						if (done > 1 && !free && rules.exhaustionPerBlock > 0.0F) {
							player.causeFoodExhaustion(rules.exhaustionPerBlock);
						}
					}
				}
			}
		} finally {
			busy = false;
		}

		log(player, "harvested", done);
		return done;
	}

	private static void replant(ServerPlayer player, ServerLevel level, BlockPos pos, CropBlock crop, Item seed, List<ItemStack> drops, boolean free) {
		BlockState young = crop.getStateForAge(0);
		if (level.getBlockState(pos).isAir() && young.canSurvive(level, pos)) {
			boolean paid = free;

			for (ItemStack d : drops) {
				if (!paid && d.getItem() == seed && !d.isEmpty()) {
					d.shrink(1);
					paid = true;
				}
			}

			if (!paid) {
				Inventory inv = player.getInventory();

				for (int i = 0; i < inv.getContainerSize() && !paid; i++) {
					ItemStack s = inv.getItem(i);
					if (s.getItem() == seed && !s.isEmpty()) {
						s.shrink(1);
						paid = true;
					}
				}
			}

			if (paid) {
				level.setBlock(pos, young, 3);
			}
		}
	}

	static List<BlockPos> ripe(Level level, BlockPos start, CropBlock crop, int limit, boolean diagonals) {
		return flat(level, start, limit, diagonals, s -> s.getBlock() == crop && crop.isMaxAge(s));
	}

	private static List<BlockPos> vein(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, VeinRules rules, Direction face) {
		VeinFinder.Context ctx = new VeinFinder.Context(
			face, player.getDirection(), VeinPlanner.lookingUp(player), 0, VeinServer.diagonals(player, rules, VeinRules.isLog(state))
		);
		VeinFinder.Result r = VeinFinder.find(level, pos, state, VeinShape.SHAPELESS, ctx, rules, null, rules.maxBlocks, new VeinFinder.Result());
		List<BlockPos> out = new ArrayList<>(r.size());

		for (int i = 0; i < r.size(); i++) {
			out.add(BlockPos.of(r.get(i)));
		}

		return out;
	}

	static List<BlockPos> ground(Level level, BlockPos start, BlockState state, int limit, boolean diagonals, VeinRules rules) {
		Block block = state.getBlock();
		return flat(level, start, limit, diagonals, s -> s.getBlock() == block && rules.blockAllowed(s));
	}

	private static List<BlockPos> flat(Level level, BlockPos start, int limit, boolean diagonals, Predicate<BlockState> match) {
		List<BlockPos> out = new ArrayList<>();
		LongOpenHashSet seen = new LongOpenHashSet();
		LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
		out.add(start.immutable());
		seen.add(start.asLong());
		queue.enqueue(start.asLong());
		MutableBlockPos at = new MutableBlockPos();

		while (!queue.isEmpty() && out.size() < limit) {
			long from = queue.dequeueLong();
			int fx = BlockPos.getX(from);
			int fy = BlockPos.getY(from);
			int fz = BlockPos.getZ(from);

			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if ((dx != 0 || dz != 0) && (diagonals || dx == 0 || dz == 0)) {
						at.set(fx + dx, fy, fz + dz);
						if (seen.add(at.asLong()) && match.test(level.getBlockState(at))) {
							if (out.size() >= limit) {
								return out;
							}

							out.add(at.immutable());
							queue.enqueue(at.asLong());
						}
					}
				}
			}
		}

		return out;
	}

	private static void log(ServerPlayer player, String what, int n) {
		if ("true".equalsIgnoreCase(CFHGuiVeinminer.devProperty("log"))) {
			CFHGuiVeinminer.LOGGER.info("[CFHGui Veinminer] {} {} {} blocks", new Object[]{player.getGameProfile().getName(), what, n});
		}
	}

	private VeinExtras() {
	}

	public static final class Use {
		final ServerPlayer player;
		final ServerLevel level;
		final InteractionHand hand;
		final Item item;
		final Direction face;
		final Vec3 offset;
		final List<BlockPos> group;

		Use(ServerPlayer player, ServerLevel level, InteractionHand hand, Item item, Direction face, Vec3 offset, List<BlockPos> group) {
			this.player = player;
			this.level = level;
			this.hand = hand;
			this.item = item;
			this.face = face;
			this.offset = offset;
			this.group = group;
		}
	}
}
