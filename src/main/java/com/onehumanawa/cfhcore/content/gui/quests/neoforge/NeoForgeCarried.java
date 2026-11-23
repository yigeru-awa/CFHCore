package com.onehumanawa.cfhcore.content.gui.quests.neoforge;

import com.onehumanawa.cfhcore.content.gui.quests.server.Carried;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

final class NeoForgeCarried implements Carried.Reader {
	@Nullable
	@Override
	public Carried.Tank fluid(ItemStack stack) {
		IFluidHandlerItem handler = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
		if (handler == null) {
			return null;
		} else {
			int i = 0;

			for (int n = handler.getTanks(); i < n; i++) {
				FluidStack in = handler.getFluidInTank(i);
				if (!in.isEmpty()) {
					return new Carried.Tank(in.getFluid(), (long)in.getAmount());
				}
			}

			return null;
		}
	}

	@Nullable
	@Override
	public Carried.Drained drain(ItemStack stack, Fluid fluid, long amount) {
		ItemStack copy = stack.copy();
		IFluidHandlerItem handler = (IFluidHandlerItem)copy.getCapability(FluidHandler.ITEM);
		if (handler == null) {
			return null;
		} else {
			int want = (int)Math.min(2147483647L, amount);
			FluidStack out = handler.drain(new FluidStack(fluid, want), FluidAction.EXECUTE);
			if (out.isEmpty()) {
				int i = 0;

				for (int n = handler.getTanks(); i < n && out.isEmpty(); i++) {
					FluidStack in = handler.getFluidInTank(i);
					if (!in.isEmpty() && in.getFluid() == fluid) {
						out = handler.drain(in.copy(), FluidAction.EXECUTE);
					}
				}
			}

			return out.isEmpty() ? null : new Carried.Drained((long)out.getAmount(), handler.getContainer());
		}
	}

	@Override
	public long energy(ItemStack stack) {
		IEnergyStorage storage = (IEnergyStorage)stack.getCapability(EnergyStorage.ITEM);
		return storage != null && storage.canExtract() ? (long)storage.extractEnergy(Integer.MAX_VALUE, true) : 0L;
	}

	@Override
	public long takeEnergy(ItemStack stack, long amount) {
		IEnergyStorage storage = (IEnergyStorage)stack.getCapability(EnergyStorage.ITEM);
		return storage != null && storage.canExtract() ? (long)storage.extractEnergy((int)Math.min(2147483647L, amount), false) : 0L;
	}
}
