package com.onehumanawa.cfhcore.content.gui.emi.api.stack.serializer;

import com.google.gson.JsonElement;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiIngredientSerializers;
import org.jetbrains.annotations.Nullable;

public interface EmiIngredientSerializer<T extends EmiIngredient> {
	String getType();

	EmiIngredient deserialize(JsonElement var1);

	JsonElement serialize(T var1);

	@Nullable
	static JsonElement getSerialized(EmiIngredient ingredient) {
		return EmiIngredientSerializers.serialize(ingredient);
	}

	static EmiIngredient getDeserialized(JsonElement element) {
		return EmiIngredientSerializers.deserialize(element);
	}
}
