package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageLevel;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlockNode;
import com.onehumanawa.cfhcore.content.gui.teams.client.Banners;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class BannerNodes {
	static ItemStack stack(@Nullable BannerData banner) {
		if (Banners.registries() != null) {
			return Banners.stack(banner);
		} else {
			BannerData b = banner == null ? new BannerData() : banner;
			ItemStack stack = new ItemStack(BannerBlock.byColor(Banners.dye(b.base)));
			List<Layer> layers = new ArrayList<>();

			for (BannerData.Layer l : b.layers) {
				ResourceLocation id = ResourceLocation.tryParse(l.pattern);
				if (id != null) {
					Holder<BannerPattern> pattern = Holder.direct(new BannerPattern(id, "block." + id.getNamespace() + ".banner." + id.getPath()));
					if (!Sheets.getBannerMaterial(pattern).sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
						layers.add(new Layer(pattern, Banners.dye(l.color)));
					}
				}
			}

			if (!layers.isEmpty()) {
				stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layers));
			}

			return stack;
		}
	}

	static BlockNode hanging(StageLevel level, @Nullable BannerData banner) {
		DyeColor base = Banners.dye(banner == null ? null : banner.base);
		BlockState state = Build.of(base.getName() + "_wall_banner");
		if (state.hasProperty(WallBannerBlock.FACING)) {
			state = (BlockState)state.setValue(WallBannerBlock.FACING, Direction.SOUTH);
		}

		BlockNode node = new BlockNode(level, state);
		node.pickable(false);
		node.hoverFeel(0.0F, 1.0F);
		dress(node, banner);
		return node;
	}

	static BlockNode standing(StageLevel level, @Nullable BannerData banner) {
		DyeColor base = Banners.dye(banner == null ? null : banner.base);
		BlockNode node = new BlockNode(level, Build.of(base.getName() + "_banner[rotation=0]"));
		node.pickable(false);
		node.hoverFeel(0.0F, 1.0F);
		dress(node, banner);
		return node;
	}

	static void dress(BlockNode node, @Nullable BannerData banner) {
		if (node.blockEntity() instanceof BannerBlockEntity entity) {
			entity.fromItem(stack(banner), Banners.dye(banner == null ? null : banner.base));
		}
	}

	private BannerNodes() {
	}
}
