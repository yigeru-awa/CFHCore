package com.onehumanawa.cfhcore.content.gui.quests.server;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public final class Carried {
	@Nullable
	private static volatile Carried.Reader reader;
	@Nullable
	private static Map<Item, Fluid> buckets;

	public static void install(Carried.Reader r) {
		reader = r;
	}

	static boolean energyKnown() {
		return reader != null;
	}

	@Nullable
	private static Fluid bucket(Item item) {
		Map<Item, Fluid> map = buckets;
		if (map == null) {
			map = new IdentityHashMap<>();

			for (Fluid fluid : BuiltInRegistries.FLUID) {
				if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState())) {
					Item b = fluid.getBucket();
					if (b != Items.AIR && b != Items.BUCKET) {
						map.putIfAbsent(b, fluid);
					}
				}
			}

			buckets = map;
		}

		return map.get(item);
	}

	@Nullable
	private static Carried.Tank tank(ItemStack stack) {
		Carried.Reader r = reader;
		if (r != null && stack.getCount() == 1) {
			try {
				Carried.Tank t = r.fluid(stack);
				if (t != null && t.amount() > 0L) {
					return t;
				}
			} catch (Throwable var3) {
			}
		}

		Fluid f = bucket(stack.getItem());
		return f == null ? null : new Carried.Tank(f, 1000L * (long)stack.getCount());
	}

	static long fluid(ServerPlayer player, Predicate<Fluid> which) {
		Inventory inventory = player.getInventory();
		long total = 0L;
		int i = 0;

		for (int n = inventory.getContainerSize(); i < n; i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) {
				Carried.Tank t = tank(stack);
				if (t != null && which.test(t.fluid())) {
					total += t.amount();
				}
			}
		}

		return total;
	}

	static long drain(ServerPlayer player, Predicate<Fluid> which, long amount) {
		Inventory inventory = player.getInventory();
		Carried.Reader r = reader;
		long left = amount;
		int i = 0;

		for (int n = inventory.getContainerSize(); i < n && left > 0L; i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) {
				Carried.Tank t = tank(stack);
				if (t != null && which.test(t.fluid())) {
					Carried.Drained drained = null;
					if (r != null && stack.getCount() == 1) {
						try {
							drained = r.drain(stack, t.fluid(), left);
						} catch (Throwable var14) {
							drained = null;
						}
					}

					if (drained != null && drained.amount() > 0L) {
						inventory.setItem(i, drained.left());
						left -= drained.amount();
					} else if (bucket(stack.getItem()) != null) {
						while (left > 0L && !stack.isEmpty()) {
							ItemStack empty = stack.getItem().hasCraftingRemainingItem() ? new ItemStack(stack.getItem().getCraftingRemainingItem()) : ItemStack.EMPTY;
							stack.shrink(1);
							left -= 1000L;
							if (!empty.isEmpty()) {
								if (stack.isEmpty()) {
									inventory.setItem(i, empty);
								} else {
									Rewards.stack(player, empty);
								}
							}
						}
					}
				}
			}
		}

		long taken = amount - Math.max(0L, left);
		if (taken > 0L) {
			inventory.setChanged();
			player.containerMenu.broadcastChanges();
		}

		return taken;
	}

	static long energy(ServerPlayer player) {
		Carried.Reader r = reader;
		if (r == null) {
			return -1L;
		} else {
			Inventory inventory = player.getInventory();
			long total = 0L;
			int i = 0;

			for (int n = inventory.getContainerSize(); i < n; i++) {
				ItemStack stack = inventory.getItem(i);
				if (!stack.isEmpty() && stack.getCount() == 1) {
					try {
						total += Math.max(0L, r.energy(stack));
					} catch (Throwable var9) {
					}
				}
			}

			return total;
		}
	}

	static long takeEnergy(ServerPlayer player, long amount) {
		Carried.Reader r = reader;
		if (r == null) {
			return -1L;
		} else {
			Inventory inventory = player.getInventory();
			long left = amount;
			int i = 0;

			for (int n = inventory.getContainerSize(); i < n && left > 0L; i++) {
				ItemStack stack = inventory.getItem(i);
				if (!stack.isEmpty() && stack.getCount() == 1) {
					try {
						left -= Math.max(0L, Math.min(left, r.takeEnergy(stack, left)));
					} catch (Throwable var11) {
					}
				}
			}

			if (left < amount) {
				inventory.setChanged();
				player.containerMenu.broadcastChanges();
			}

			return amount - left;
		}
	}

	private Carried() {
	}

	public static record Drained(long amount, ItemStack left) {
	}

	public interface Reader {
		@Nullable
		Carried.Tank fluid(ItemStack var1);

		@Nullable
		Carried.Drained drain(ItemStack var1, Fluid var2, long var3);

		long energy(ItemStack var1);

		long takeEnergy(ItemStack var1, long var2);
	}

	public static record Tank(Fluid fluid, long amount) {
	}
}
