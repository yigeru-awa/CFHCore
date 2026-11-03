package com.onehumanawa.cfhcore.content.burner;

import java.util.List;

import com.onehumanawa.cfhcore.CFHCore;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Burner block entity, ported from Create Diesel Generators but driven by Create's own
 * blaze burner rules instead of a valve:
 * <ul>
 * <li>each fuel item contributes its vanilla burn time in ticks (1 stick = 100t = 5s,
 *     so 1.5 items = 150t = 7.5s), consumed at a fixed rate of one tick per tick — there
 *     is no burn rate to control</li>
 * <li>while fuel burns the heat level is {@link HeatLevel#KINDLED}, dropping to
 *     {@link HeatLevel#FADING} for the last 12.5% (Create's blaze burner behaviour)</li>
 * <li>fuel already burning/queued is stored in the internal slot and ignites once the
 *     current item burns out; fed fuel is never dropped when the block is removed</li>
 * <li>there is no superheating fuel (blaze cake is rejected); the creative blaze cake
 *     instead cycles the heat level through smouldering / kindled / seething without
 *     consuming fuel</li>
 * </ul>
 */
public class BurnerBlockEntity extends KineticBlockEntity {

	/** Same capacity the blaze burner uses for its "nearly out of fuel" check. */
	public static final int MAX_HEAT_CAPACITY = 10000;
	/** Display conversion: 1 item = 100 ticks = 5 seconds. */
	public static final int TICKS_PER_ITEM = 100;

	int tick;

	private FuelType activeFuel = FuelType.NONE;
	private int remainingBurnTime = 0;
	private boolean isCreative = false;
	private final ItemStackHandler fuelInventory = new FuelInventory();

	public BurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tick() {
		this.tick = this.tick + 1 & 0xffff;
		super.tick();
		if (this.level.isClientSide)
			return;
		if (isCreative)
			return;

		// Nothing burning: ignite the next queued fuel item (left-click fuel goes here)
		if (this.activeFuel == FuelType.NONE && this.remainingBurnTime <= 0 && this.consumeFuelItem())
			this.level.playSound(null, this.worldPosition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
					1.0F, this.level.getRandom().nextFloat() * 0.4F + 0.8F);

		if (this.remainingBurnTime > 0)
			this.remainingBurnTime--;

		if (this.activeFuel == FuelType.NORMAL)
			updateBlockState();

		if (this.remainingBurnTime > 0)
			return;

		this.activeFuel = FuelType.NONE;
		updateBlockState();
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		// Keep the client's remaining-burn-time reading from going stale
		if (!this.level.isClientSide && this.remainingBurnTime > 0)
			sendData();
	}

	/**
	 * Called when the player right-clicks the block with an item in hand. Exactly one
	 * item is accepted per click.
	 *
	 * @return {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} if the item is not usable fuel
	 */
	public ItemInteractionResult tryInsertFuel(Player player, ItemStack stack) {
		if (stack.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		// Creative blaze cake: cycle smouldering / kindled / seething, fuel-free
		if (isCreativeFuel(stack)) {
			if (!this.level.isClientSide) {
				this.applyCreativeFuel();
				if (player == null || !player.getAbilities().instabuild)
					stack.shrink(1);
			}
			return ItemInteractionResult.sidedSuccess(this.level.isClientSide);
		}

		if (isCreative)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		int burnTime = stack.getBurnTime(null);
		if (burnTime <= 0)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		// Queue exactly one item; it ignites once whatever is queued ahead of it burns out
		ItemStack single = stack.copyWithCount(1);
		if (!this.fuelInventory.insertItem(0, single, true).isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!this.level.isClientSide) {
			this.fuelInventory.insertItem(0, single, false);
			// Hand back crafting remainders such as the empty bucket of a lava bucket
			if (stack.hasCraftingRemainingItem()) {
				ItemStack container = stack.getCraftingRemainingItem();
				stack.shrink(1);
				if (player != null && !player.getInventory().add(container))
					player.drop(container, false);
			} else if (player == null || !player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
					this.level.getRandom().nextFloat() * 0.4F + 0.8F);
			this.setChanged();
			sendData();
		}
		return ItemInteractionResult.sidedSuccess(this.level.isClientSide);
	}

	public boolean isCreativeFuel(ItemStack stack) {
		return AllItems.CREATIVE_BLAZE_CAKE.isIn(stack);
	}

	/**
	 * Creative heat level: stop consuming fuel and step to the next of the three states,
	 * skipping the transient FADING level like the blaze burner does.
	 */
	private void applyCreativeFuel() {
		this.activeFuel = FuelType.NONE;
		this.remainingBurnTime = 0;
		this.isCreative = true;

		HeatLevel next = getBlockState().getValue(BurnerBlock.HEAT_LEVEL).nextActiveLevel();
		if (next == HeatLevel.FADING)
			next = next.nextActiveLevel();
		setBlockHeat(next);
	}

	public ItemStackHandler getFuelInventory() {
		return fuelInventory;
	}

	public int getRemainingBurnTicks() {
		return remainingBurnTime;
	}

	public boolean isCreative() {
		return isCreative;
	}

	/** Total remaining burn time: the active burn plus everything queued in the fuel slot. */
	public int getTotalRemainingBurnTicks() {
		int total = remainingBurnTime;
		ItemStack queued = fuelInventory.getStackInSlot(0);
		if (!queued.isEmpty())
			total += queued.getCount() * queued.getBurnTime(null);
		return total;
	}

	private void updateBlockState() {
		setBlockHeat(getHeatLevel());
	}

	private void setBlockHeat(HeatLevel heat) {
		BlockState state = getBlockState();
		if (state.getValue(BurnerBlock.HEAT_LEVEL) == heat)
			return;
		this.level.setBlockAndUpdate(this.worldPosition, state.setValue(BurnerBlock.HEAT_LEVEL, heat));
		this.notifyUpdate();
	}

	private HeatLevel getHeatLevel() {
		if (activeFuel == FuelType.NORMAL) {
			boolean lowPercent = (double) remainingBurnTime / MAX_HEAT_CAPACITY < 0.0125;
			return lowPercent ? HeatLevel.FADING : HeatLevel.KINDLED;
		}
		return HeatLevel.SMOULDERING;
	}

	/**
	 * Takes one item from the fuel slot and converts it into remaining burn time.
	 * Server-side only.
	 *
	 * @return whether an item was actually consumed
	 */
	private boolean consumeFuelItem() {
		ItemStack fuel = this.fuelInventory.getStackInSlot(0);
		if (fuel.isEmpty())
			return false;
		int burnTime = fuel.getBurnTime(null);
		if (burnTime <= 0)
			return false;
		fuel.shrink(1);
		this.fuelInventory.setStackInSlot(0, fuel);
		this.activeFuel = FuelType.NORMAL;
		this.remainingBurnTime = burnTime;
		this.setChanged();
		sendData();
		return true;
	}

	@Override
	public void tickAudio() {
		super.tickAudio();
		HeatLevel heatLevel = getBlockState().getValue(BurnerBlock.HEAT_LEVEL);
		if (heatLevel == HeatLevel.NONE)
			return;
		if (this.tick % 20 == 0 && heatLevel.isAtLeast(HeatLevel.FADING))
			this.level.playLocalSound(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F,
					this.worldPosition.getZ() + 0.5F, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
					heatLevel.isAtLeast(HeatLevel.SEETHING) ? 0.4F : 0.25F, 1.0F, true);

		// Spawn fire particles sparingly, like the blaze burner does
		if (this.level.getRandom().nextInt(4) != 0)
			return;
		double x = this.worldPosition.getX() + 0.5 + (this.level.getRandom().nextDouble() - 0.5) * 0.25;
		double z = this.worldPosition.getZ() + 0.5 + (this.level.getRandom().nextDouble() - 0.5) * 0.25;
		this.level.addParticle(ParticleTypes.LARGE_SMOKE, x, this.worldPosition.getY() + 0.75, z, 0, 0, 0);
		if (heatLevel.isAtLeast(HeatLevel.FADING))
			this.level.addParticle(heatLevel.isAtLeast(HeatLevel.SEETHING) ? ParticleTypes.SOUL_FIRE_FLAME
					: ParticleTypes.FLAME, x, this.worldPosition.getY() + 0.9, z, 0, 0.03, 0);
	}

	// Goggle information: current burn state + remaining burn time

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		HeatLevel heatLevel = getBlockState().getValue(BurnerBlock.HEAT_LEVEL);
		String stateKey = switch (heatLevel) {
			case SEETHING -> "gui.goggles.burner.state.seething";
			case KINDLED, FADING -> "gui.goggles.burner.state.kindled";
			default -> "gui.goggles.burner.state.smouldering";
		};
		ChatFormatting stateColor = switch (heatLevel) {
			case SEETHING -> ChatFormatting.AQUA;
			case KINDLED, FADING -> ChatFormatting.GOLD;
			default -> ChatFormatting.DARK_GRAY;
		};
		new LangBuilder(CFHCore.MOD_ID)
				.translate("gui.goggles.burner.state")
				.style(ChatFormatting.GRAY)
				.text(" ")
				.translate(stateKey)
				.style(stateColor)
				.forGoggles(tooltip);

		if (isCreative) {
			new LangBuilder(CFHCore.MOD_ID)
					.translate("gui.goggles.burner.creative")
					.style(ChatFormatting.LIGHT_PURPLE)
					.forGoggles(tooltip, 1);
		}

		// Remaining burn time (active burn plus queued fuel)
		int remaining = getTotalRemainingBurnTicks();
		new LangBuilder(CFHCore.MOD_ID)
				.translate("gui.goggles.burner.remaining")
				.style(ChatFormatting.GRAY)
				.text(" ")
				.text(String.format(java.util.Locale.ROOT, "%.1fs", remaining / 20.0F))
				.style(ChatFormatting.GOLD)
				.text(String.format(java.util.Locale.ROOT, " (%.1f ", remaining / (float) TICKS_PER_ITEM))
				.translate("gui.goggles.burner.items")
				.style(ChatFormatting.DARK_GRAY)
				.text(")")
				.style(ChatFormatting.DARK_GRAY)
				.forGoggles(tooltip);

		// Append kinetic stress stats when the burner has an impact value
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		if (!isCreative) {
			tag.putInt("fuelLevel", activeFuel.ordinal());
			tag.putInt("burnTimeRemaining", remainingBurnTime);
		} else
			tag.putBoolean("isCreative", true);
		tag.put("Fuel", this.fuelInventory.serializeNBT(registries));
		super.write(tag, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		this.activeFuel = FuelType.values()[tag.getInt("fuelLevel")];
		this.remainingBurnTime = tag.getInt("burnTimeRemaining");
		this.isCreative = tag.getBoolean("isCreative");
		this.fuelInventory.deserializeNBT(registries, tag.getCompound("Fuel"));
		super.read(tag, registries, clientPacket);
	}

	private enum FuelType {
		NONE, NORMAL
	}

	/** Single-slot inventory that only accepts fuel. */
	private class FuelInventory extends ItemStackHandler {

		FuelInventory() {
			super(1);
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sendData();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.isEmpty() || stack.getBurnTime(null) > 0;
		}
	}
}
