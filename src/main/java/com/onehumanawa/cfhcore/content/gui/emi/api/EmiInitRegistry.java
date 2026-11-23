package com.onehumanawa.cfhcore.content.gui.emi.api;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiRegistryAdapter;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer.EmiIngredientSerializer;
import java.util.function.Predicate;

public interface EmiInitRegistry {
	<T extends EmiIngredient> void addIngredientSerializer(Class<T> var1, EmiIngredientSerializer<T> var2);

	void disableStack(EmiStack var1);

	void disableStacks(Predicate<EmiStack> var1);

	void addRegistryAdapter(EmiRegistryAdapter<?> var1);
}
