package com.onehumanawa.cfhcore.content.gui.emi.api.stack;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public interface EmiRegistryAdapter<T> {
	Class<T> getBaseClass();

	Registry<T> getRegistry();

	EmiStack of(T var1, DataComponentPatch var2, long var3);

	static <T> EmiRegistryAdapter<T> simple(final Class<T> clazz, final Registry<T> registry, final EmiRegistryAdapter.StackConstructor<T> constructor) {
		return new EmiRegistryAdapter<T>() {
			@Override
			public Class<T> getBaseClass() {
				return clazz;
			}

			@Override
			public Registry<T> getRegistry() {
				return registry;
			}

			@Override
			public EmiStack of(T t, DataComponentPatch componentChanges, long amount) {
				return constructor.of(t, componentChanges, amount);
			}
		};
	}

	public interface StackConstructor<T> {
		EmiStack of(T var1, DataComponentPatch var2, long var3);
	}
}
