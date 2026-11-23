package com.onehumanawa.cfhcore.content.gui.shops.service;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class Pocket {
	private final List<ItemStack> slots;
	private final Runnable changed;

	public Pocket(List<ItemStack> slots, Runnable changed) {
		this.slots = slots;
		this.changed = changed;
	}

	public int size() {
		return this.slots.size();
	}

	public ItemStack slot(int index) {
		return index >= 0 && index < this.slots.size() ? this.slots.get(index) : ItemStack.EMPTY;
	}

	public long count(ItemStack like) {
		long n = 0L;

		for (ItemStack s : this.slots) {
			if (!s.isEmpty() && Stacks.same(s, like)) {
				n += (long)s.getCount();
			}
		}

		return n;
	}

	public long room(ItemStack like) {
		int max = like.getMaxStackSize();
		long n = 0L;

		for (ItemStack s : this.slots) {
			if (s.isEmpty()) {
				n += (long)max;
			} else if (Stacks.same(s, like)) {
				n += (long)Math.max(0, Math.min(max, s.getMaxStackSize()) - s.getCount());
			}
		}

		return n;
	}

	public long take(ItemStack like, long n) {
		long left = n;

		for (int i = this.slots.size() - 1; i >= 0 && left > 0L; i--) {
			ItemStack s = this.slots.get(i);
			if (!s.isEmpty() && Stacks.same(s, like)) {
				int t = (int)Math.min(left, (long)s.getCount());
				s.shrink(t);
				if (s.isEmpty()) {
					this.slots.set(i, ItemStack.EMPTY);
				}

				left -= (long)t;
			}
		}

		if (left != n) {
			this.changed.run();
		}

		return n - left;
	}

	public long give(ItemStack like, long n) {
		int max = like.getMaxStackSize();
		long left = n;

		for (int i = 0; i < this.slots.size() && left > 0L; i++) {
			ItemStack s = this.slots.get(i);
			if (!s.isEmpty() && Stacks.same(s, like)) {
				int add = (int)Math.min(left, (long)Math.max(0, Math.min(max, s.getMaxStackSize()) - s.getCount()));
				if (add > 0) {
					s.grow(add);
					left -= (long)add;
				}
			}
		}

		for (int ix = 0; ix < this.slots.size() && left > 0L; ix++) {
			if (this.slots.get(ix).isEmpty()) {
				int add = (int)Math.min(left, (long)max);
				this.slots.set(ix, like.copyWithCount(add));
				left -= (long)add;
			}
		}

		if (left != n) {
			this.changed.run();
		}

		return n - left;
	}
}
