package com.onehumanawa.cfhcore.content.gui.shops.service;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Stacks {
	public static String write(ItemStack stack, Provider registries) {
		return stack.save(registries).toString();
	}

	public static ItemStack read(String snbt, Provider registries) {
		if (snbt != null && !snbt.isEmpty()) {
			try {
				return ItemStack.parse(registries, TagParser.parseTag(snbt)).orElse(ItemStack.EMPTY);
			} catch (Exception var3) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}
	}

	public static boolean same(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b);
	}

	public static ItemStack plain(String id) {
		ResourceLocation rl = id != null && !id.isEmpty() ? ResourceLocation.tryParse(id) : null;
		if (rl == null) {
			return ItemStack.EMPTY;
		} else {
			Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.AIR);
			return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
		}
	}

	public static MutableComponent named(ItemStack stack, long count) {
		return Component.literal(count + " × ").append(name(stack));
	}

	private static Component name(ItemStack stack) {
		Component name = stack.getHoverName();
		if (name.getContents() instanceof TranslatableContents t && !Language.getInstance().has(t.getKey())) {
			return Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		}

		return name;
	}

	private Stacks() {
	}
}
