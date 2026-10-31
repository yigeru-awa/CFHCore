package com.onehumanawa.cfhcore.content.worldgen.feature.template.decorator;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.worldgen.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHBuiltInRegistries;

public class TemplateDecorators {

	public static void bootstrap() {
		register("tree", TreeDecorator.CODEC);
	}
	
	public static TreeDecorator tree(net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator decorator) {
		return tree(decorator, decorator);
	}
	
	public static TreeDecorator tree(net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator decorator, net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator modifiedDecorator) {
		return new TreeDecorator(decorator, modifiedDecorator);
	}

	private static void register(String name, MapCodec<? extends TemplateDecorator<?>> placement) {
		RegistryUtil.register(CFHBuiltInRegistries.TEMPLATE_DECORATOR_TYPE, name, placement);
	}
}
