package com.onehumanawa.cfhcore.content.burner;

import com.simibubi.create.content.kinetics.base.ShaftRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Burner renderer. Fuelled heat is shown by the block model itself (orange or blue
 * fire overlay, see {@code assets/cfhcore/blockstates/burner.json}), so this only
 * renders the kinetic shaft.
 */
public class BurnerRenderer extends ShaftRenderer<BurnerBlockEntity> {

	public BurnerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}
}
