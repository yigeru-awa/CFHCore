package com.onehumanawa.cfhcore.content.burner;

import com.onehumanawa.cfhcore.CFHAllBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Burner block, ported from Create Diesel Generators. It burns solid fuel (vanilla burn
 * times) following the blaze burner's rules: fuel heats to kindled, burns at a fixed
 * one-tick-per-tick rate, and cannot be superheated. Right-click feeds fuel (one item
 * per click, hoppers/pipes may feed it too); the creative blaze cake cycles the heat
 * level for free.
 */
public class BurnerBlock extends HorizontalAxisKineticBlock implements IBE<BurnerBlockEntity> {

	public static final EnumProperty<BlazeBurnerBlock.HeatLevel> HEAT_LEVEL = BlazeBurnerBlock.HEAT_LEVEL;

	public BurnerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, HeatLevel.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HEAT_LEVEL);
		super.createBlockStateDefinition(builder);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		// Feed fuel / apply the creative blaze cake
		return onBlockEntityUseItemOn(level, pos, be -> be.tryInsertFuel(player, stack));
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (state.getValue(HEAT_LEVEL).isAtLeast(HeatLevel.FADING) && entity instanceof LivingEntity)
			entity.hurt(level.damageSources().campfire(), 1.0F);
		super.entityInside(state, level, pos, entity);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Block.box(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return Math.max(0, state.getValue(HEAT_LEVEL)
				.ordinal() - 1);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		// Fuel already fed into the burner is consumed, it never drops on removal
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<BurnerBlockEntity> getBlockEntityClass() {
		return BurnerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BurnerBlockEntity> getBlockEntityType() {
		return CFHAllBlockEntityTypes.BURNER.get();
	}
}
