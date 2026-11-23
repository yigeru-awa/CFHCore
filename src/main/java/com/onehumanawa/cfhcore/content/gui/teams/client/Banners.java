package com.onehumanawa.cfhcore.content.gui.teams.client;

import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Builder;
import org.jetbrains.annotations.Nullable;

public final class Banners {
	public static final List<DyeColor> COLORS = List.of(DyeColor.values());
	private static final Map<String, ItemStack> CACHE = new HashMap<>();
	@Nullable
	private static RegistryAccess cachedFor;

	public static DyeColor dye(@Nullable String name) {
		return DyeColor.byName(name == null ? "white" : name, DyeColor.WHITE);
	}

	public static ItemStack stack(@Nullable BannerData banner) {
		BannerData b = banner == null ? new BannerData() : banner;
		RegistryAccess registries = registries();
		if (registries != cachedFor) {
			CACHE.clear();
			cachedFor = registries;
		}

		return CACHE.computeIfAbsent(b.signature(), k -> build(b, registries));
	}

	private static ItemStack build(BannerData b, @Nullable RegistryAccess registries) {
		ItemStack stack = new ItemStack(BannerBlock.byColor(dye(b.base)));
		if (registries != null && !b.layers.isEmpty()) {
			Optional<RegistryLookup<BannerPattern>> patterns = registries.lookup(Registries.BANNER_PATTERN);
			if (patterns.isEmpty()) {
				return stack;
			} else {
				Builder layers = new Builder();

				for (BannerData.Layer l : b.layers) {
					ResourceLocation id = ResourceLocation.tryParse(l.pattern);
					if (id != null) {
						Optional<Reference<BannerPattern>> pattern = patterns.get().get(ResourceKey.create(Registries.BANNER_PATTERN, id));
						pattern.ifPresent(holder -> layers.add(holder, dye(l.color)));
					}
				}

				stack.set(DataComponents.BANNER_PATTERNS, layers.build());
				return stack;
			}
		} else {
			return stack;
		}
	}

	@Nullable
	public static RegistryAccess registries() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			return mc.level.registryAccess();
		} else {
			return mc.getConnection() != null ? mc.getConnection().registryAccess() : null;
		}
	}

	public static List<ResourceLocation> patterns() {
		RegistryAccess registries = registries();
		return registries == null
			? List.of()
			: registries.lookup(Registries.BANNER_PATTERN).map(l -> l.listElementIds().<ResourceLocation>map(ResourceKey::location).sorted().toList()).orElse(List.of());
	}

	public static void draw(GuiGraphics g, @Nullable BannerData banner, int x, int y, float scale) {
		ItemStack stack = stack(banner);
		if (scale == 1.0F) {
			g.renderItem(stack, x, y);
		} else {
			g.pose().pushPose();
			g.pose().translate((float)x, (float)y, 0.0F);
			g.pose().scale(scale, scale, 1.0F);
			g.renderItem(stack, 0, 0);
			g.pose().popPose();
		}
	}

	private Banners() {
	}
}
