package com.onehumanawa.cfhcore.content.gui.emi.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import java.io.InputStreamReader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public class RecipeDefaultLoader extends SimplePreparableReloadListener<RecipeDefaults> implements EmiResourceReloadListener {
	private static final Gson GSON = new Gson();
	public static final ResourceLocation ID = EmiPort.id("emi:recipe_defaults");

	protected RecipeDefaults prepare(ResourceManager manager, ProfilerFiller profiler) {
		RecipeDefaults defaults = new RecipeDefaults();

		for (ResourceLocation id : EmiPort.findResources(manager, "recipe/defaults", i -> i.endsWith(".json"))) {
			if (id.getNamespace().equals("emi")) {
				try {
					for (Resource resource : manager.getResourceStack(id)) {
						InputStreamReader reader = new InputStreamReader(EmiPort.getInputStream(resource));
						JsonObject json = (JsonObject)GsonHelper.fromJson(GSON, reader, JsonObject.class);
						loadDefaults(defaults, json);
					}
				} catch (Exception var10) {
					EmiLog.error("Error loading recipe default file " + id, var10);
				}
			}
		}

		return defaults;
	}

	protected void apply(RecipeDefaults prepared, ResourceManager manager, ProfilerFiller profiler) {
		BoM.setDefaults(prepared);
	}

	@Override
	public ResourceLocation getEmiId() {
		return ID;
	}

	public static void loadDefaults(RecipeDefaults defaults, JsonObject json) {
		if (GsonHelper.getAsBoolean(json, "replace", false)) {
			defaults.clear();
		}

		for (JsonElement el : GsonHelper.getAsJsonArray(json, "disabled", new JsonArray())) {
			ResourceLocation id = EmiPort.id(el.getAsString());
			defaults.remove(id);
		}

		JsonArray added = GsonHelper.getAsJsonArray(json, "added", new JsonArray());
		if (GsonHelper.isArrayNode(json, "recipes")) {
			added.addAll(GsonHelper.getAsJsonArray(json, "recipes"));
		}

		for (JsonElement el : added) {
			ResourceLocation id = EmiPort.id(el.getAsString());
			defaults.add(id);
		}

		JsonObject resolutions = GsonHelper.getAsJsonObject(json, "resolutions", new JsonObject());

		for (String key : resolutions.keySet()) {
			ResourceLocation id = EmiPort.id(key);
			if (GsonHelper.isArrayNode(resolutions, key)) {
				defaults.add(id, GsonHelper.getAsJsonArray(resolutions, key));
			}
		}

		JsonObject addedTags = GsonHelper.getAsJsonObject(json, "tags", new JsonObject());

		for (String keyx : addedTags.keySet()) {
			defaults.addTag(new JsonPrimitive(keyx), addedTags.get(keyx));
		}
	}
}
