package com.onehumanawa.cfhcore.content.gui.server.service;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class Peek {
	private static final int ROWS = 5;
	private static final int SIZE = 45;
	private static final int USED = 41;

	static ChestMenu inventory(int id, Inventory viewer, ServerPlayer target, boolean edit) {
		return new Peek.Locked(MenuType.GENERIC_9x5, id, viewer, new Peek.View(target), 5, edit, 41);
	}

	static ChestMenu enderChest(int id, Inventory viewer, ServerPlayer target, boolean edit) {
		return new Peek.Locked(MenuType.GENERIC_9x3, id, viewer, new Peek.Guarded(target.getEnderChestInventory(), target), 3, edit, 27);
	}

	private Peek() {
	}

	private static final class Guarded implements Container {
		private final Container inner;
		private final ServerPlayer owner;

		Guarded(Container inner, ServerPlayer owner) {
			this.inner = inner;
			this.owner = owner;
		}

		public int getContainerSize() {
			return this.inner.getContainerSize();
		}

		public boolean isEmpty() {
			return this.inner.isEmpty();
		}

		public ItemStack getItem(int slot) {
			return this.inner.getItem(slot);
		}

		public ItemStack removeItem(int slot, int amount) {
			return this.inner.removeItem(slot, amount);
		}

		public ItemStack removeItemNoUpdate(int slot) {
			return this.inner.removeItemNoUpdate(slot);
		}

		public void setItem(int slot, ItemStack stack) {
			this.inner.setItem(slot, stack);
		}

		public void setChanged() {
			this.inner.setChanged();
		}

		public boolean stillValid(Player player) {
			return !this.owner.isRemoved() && !this.owner.hasDisconnected();
		}

		public void clearContent() {
		}
	}

	private static final class Locked extends ChestMenu {
		private final boolean edit;
		private final int used;
		private final int size;

		Locked(MenuType<?> type, int id, Inventory viewer, Container container, int rows, boolean edit, int used) {
			super(type, id, viewer, container, rows);
			this.edit = edit;
			this.used = used;
			this.size = rows * 9;
		}

		public void clicked(int slot, int button, ClickType type, Player player) {
			if (this.edit || slot >= this.size && type != ClickType.QUICK_MOVE && type != ClickType.PICKUP_ALL && type != ClickType.QUICK_CRAFT) {
				if (slot < this.used || slot >= this.size) {
					super.clicked(slot, button, type, player);
				}
			}
		}

		public ItemStack quickMoveStack(Player player, int index) {
			if (this.edit && index >= 0 && index < this.slots.size() && (index < this.used || index >= this.size)) {
				Slot slot = (Slot)this.slots.get(index);
				if (!slot.hasItem()) {
					return ItemStack.EMPTY;
				} else {
					ItemStack stack = slot.getItem();
					ItemStack before = stack.copy();
					if (index < this.size ? this.moveItemStackTo(stack, this.size, this.slots.size(), true) : this.moveItemStackTo(stack, 0, this.used, false)) {
						if (stack.isEmpty()) {
							slot.setByPlayer(ItemStack.EMPTY);
						} else {
							slot.setChanged();
						}

						return before;
					} else {
						return ItemStack.EMPTY;
					}
				}
			} else {
				return ItemStack.EMPTY;
			}
		}
	}

	private static final class View implements Container {
		private final ServerPlayer target;

		View(ServerPlayer target) {
			this.target = target;
		}

		private List<ItemStack> list(int slot) {
			Inventory inv = this.target.getInventory();
			if (slot < 36) {
				return inv.items;
			} else if (slot < 40) {
				return inv.armor;
			} else {
				return slot == 40 ? inv.offhand : null;
			}
		}

		private static int index(int slot) {
			if (slot < 27) {
				return slot + 9;
			} else if (slot < 36) {
				return slot - 27;
			} else {
				return slot < 40 ? 39 - slot : 0;
			}
		}

		public int getContainerSize() {
			return 45;
		}

		public boolean isEmpty() {
			return this.target.getInventory().isEmpty();
		}

		public ItemStack getItem(int slot) {
			List<ItemStack> list = this.list(slot);
			return list == null ? ItemStack.EMPTY : list.get(index(slot));
		}

		public ItemStack removeItem(int slot, int amount) {
			List<ItemStack> list = this.list(slot);
			if (list == null) {
				return ItemStack.EMPTY;
			} else {
				ItemStack out = ContainerHelper.removeItem(list, index(slot), amount);
				if (!out.isEmpty()) {
					this.setChanged();
				}

				return out;
			}
		}

		public ItemStack removeItemNoUpdate(int slot) {
			List<ItemStack> list = this.list(slot);
			return list == null ? ItemStack.EMPTY : ContainerHelper.takeItem(list, index(slot));
		}

		public void setItem(int slot, ItemStack stack) {
			List<ItemStack> list = this.list(slot);
			if (list != null) {
				list.set(index(slot), stack);
				this.setChanged();
			}
		}

		public void setChanged() {
			this.target.getInventory().setChanged();
		}

		public boolean stillValid(Player player) {
			return !this.target.isRemoved() && !this.target.hasDisconnected();
		}

		public void clearContent() {
		}
	}
}
