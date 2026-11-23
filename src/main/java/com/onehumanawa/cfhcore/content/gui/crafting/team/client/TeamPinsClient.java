package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorites;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import com.onehumanawa.cfhcore.content.gui.crafting.team.TeamPinsNet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TeamPinsClient {
	private static String world = "";
	@Nullable
	private static UUID team;
	private static final Map<String, Pin> PINS = new LinkedHashMap<>();
	private static int version;
	private static GivenOnce given = new GivenOnce(null);
	private static boolean givenPending;
	private static int loadedTicks;
	private static int generation = -1;
	private static int nextRequest = 1;
	private static final Map<Integer, Consumer<Boolean>> WAITING = new HashMap<>();
	private static final List<Pin> FRESH = new ArrayList<>();
	private static final Set<String> SYNCED = new HashSet<>();

	public static void init() {
		boolean harness = System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld");
		given = new GivenOnce(harness ? null : JsonConfig.dir().resolve("crafting-team.json"));
		given.load();
		CFHGuiEvents.CLIENT_TICK_END.register(TeamPinsClient::tick);
		CFHGuiEvents.CLIENT_LEFT_SERVER.register(TeamPinsClient::left);
		TeamShots.init();
	}

	public static boolean active() {
		return team != null;
	}

	@Nullable
	public static UUID team() {
		return team;
	}

	public static String world() {
		return world;
	}

	public static Collection<Pin> pins() {
		return PINS.values();
	}

	@Nullable
	public static Pin pin(String id) {
		return PINS.get(id);
	}

	public static int version() {
		return version;
	}

	static UUID self() {
		return Minecraft.getInstance().getUser().getProfileId();
	}

	static TeamsView.Look look() {
		try {
			return TeamsView.look(team);
		} catch (LinkageError var1) {
			return new TeamsView.Look("", -2523042);
		}
	}

	public static boolean mayUnpin(Pin pin) {
		if (pin.by.equals(self())) {
			return true;
		} else {
			try {
				return TeamsView.officer(team);
			} catch (LinkageError var2) {
				return false;
			}
		}
	}

	@Nullable
	public static Pin recipePin(@Nullable EmiRecipe recipe) {
		if (recipe != null && recipe.getId() != null) {
			String id = recipe.getId().toString();

			for (Pin pin : PINS.values()) {
				if (!pin.isTree() && pin.recipe.equals(id)) {
					return pin;
				}
			}

			return null;
		} else {
			return null;
		}
	}

	@Nullable
	static EmiRecipe recipe(String id) {
		ResourceLocation key = ResourceLocation.tryParse(id);
		return key != null && EmiReloadManager.isLoaded() ? EmiApi.getRecipeManager().getRecipe(key) : null;
	}

	public static void onReset(TeamPinsNet.Reset p) {
		UUID now = parse(p.team());
		boolean other = !Objects.equals(now, team) || !p.world().equals(world);
		world = p.world();
		team = now;
		PINS.clear();
		FRESH.clear();
		SYNCED.clear();
		changed();
		TeamPage.teamChanged(other);
	}

	public static void onPut(TeamPinsNet.Put p) {
		if (team != null) {
			Pin[] pins = (Pin[])JsonWire.decode(p.pins(), Pin[].class);
			if (pins != null) {
				UUID me = self();

				for (Pin pin : pins) {
					if (pin != null && pin.sanitize()) {
						PINS.put(pin.id, pin);
						if (p.sync()) {
							SYNCED.add(pin.recipe);
						} else if (!pin.by.equals(me)) {
							FRESH.add(pin);
						}
					}
				}

				givenPending = true;
				changed();
			}
		}
	}

	public static void onGone(TeamPinsNet.Gone p) {
		if (PINS.remove(p.id()) != null) {
			changed();
		}
	}

	public static void onAnswer(TeamPinsNet.Answer p) {
		Consumer<Boolean> then = WAITING.remove(p.request());
		if (then != null) {
			then.accept(p.ok());
		}

		if (!p.ok()) {
			CFHGuiToasts.show(Component.translatable("cfhgui_crafting.team.refused"), Component.translatable("cfhgui_crafting.team.refused." + p.code()), Icon.WARNING);
		}
	}

	private static void changed() {
		version++;
		TeamPage.changed();
	}

	public static void pinRecipe(EmiRecipe recipe) {
		if (recipe.getId() != null) {
			ask("pin_recipe", recipe.getId().toString(), new byte[0], 0, null);
		}
	}

	public static void unpin(Pin pin) {
		if (!mayUnpin(pin)) {
			CFHGuiToasts.show(Component.translatable("cfhgui_crafting.team.refused"), Component.translatable("cfhgui_crafting.team.refused.not_yours"), Icon.WARNING);
		} else {
			ask("unpin", pin.id, new byte[0], 0, null);
		}
	}

	static void ask(String op, String subject, byte[] tree, int steps, @Nullable Consumer<Boolean> then) {
		UUID t = team;
		if (t != null) {
			if (subject.length() > 256) {
				if (then != null) {
					then.accept(false);
				}

				CFHGuiToasts.show(Component.translatable("cfhgui_crafting.team.refused"), Component.translatable("cfhgui_crafting.team.refused.unknown_recipe"), Icon.WARNING);
			} else {
				int request = nextRequest++;
				if (then != null) {
					WAITING.put(request, then);
				}

				CFHGuiNetwork.get().sendToServer(new TeamPinsNet.Ask(request, op, t.toString(), subject, tree, steps));
			}
		}
	}

	private static void tick() {
		TeamPage.tick();
		if (!EmiReloadManager.isLoaded()) {
			loadedTicks = 0;
		} else if (loadedTicks < 3) {
			loadedTicks++;
		} else {
			if (generation != BoM.generation) {
				generation = BoM.generation;
				givenPending = true;
				changed();
			}

			if (givenPending && team != null) {
				givenPending = false;
				give();
			}
		}
	}

	private static void give() {
		String key = GivenOnce.key(world, String.valueOf(team));
		Map<String, EmiRecipe> known = new LinkedHashMap<>();

		for (Pin pin : PINS.values()) {
			if (!pin.isTree()) {
				EmiRecipe recipe = recipe(pin.recipe);
				if (recipe != null && !recipe.getOutputs().isEmpty()) {
					known.putIfAbsent(pin.recipe, recipe);
				}
			}
		}

		List<String> added = new ArrayList<>();

		for (String id : given.due(key, known.keySet())) {
			EmiRecipe recipe = known.get(id);
			if (!favorite(recipe)) {
				EmiFavorites.addFavorite(recipe.getOutputs().get(0), recipe);
				added.add(id);
			}
		}

		given.save();
		if (!added.isEmpty()) {
			EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
			CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] team pins: {} given to the favourites: {}", added.size(), added);
		}

		tell(added);
	}

	private static boolean favorite(EmiRecipe recipe) {
		for (EmiFavorite fav : EmiFavorites.favorites) {
			if (fav.getRecipe() == recipe) {
				return true;
			}
		}

		return false;
	}

	private static void tell(List<String> added) {
		int told = 0;

		for (Pin pin : FRESH) {
			if (PINS.containsKey(pin.id)) {
				if (++told <= 3) {
					boolean favourite = !pin.isTree() && added.contains(pin.recipe);
					Component what = pin.isTree() ? TeamTrees.name(pin) : name(pin);
					CFHGuiToasts.show(
						Component.translatable("cfhgui_crafting.team.toast.pinned", new Object[]{pin.byName}),
						(Component)(favourite ? Component.translatable("cfhgui_crafting.team.toast.favourite", new Object[]{what}) : what),
						Icon.PIN
					);
				}
			}
		}

		FRESH.clear();
		int synced = 0;

		for (String recipe : added) {
			if (SYNCED.contains(recipe)) {
				synced++;
			}
		}

		SYNCED.clear();
		if (synced > 0) {
			CFHGuiToasts.show(
				Component.translatable("cfhgui_crafting.team.toast.given_title"),
				Component.translatable(synced == 1 ? "cfhgui_crafting.team.toast.given.one" : "cfhgui_crafting.team.toast.given", new Object[]{synced}),
				Icon.PIN
			);
		}
	}

	static Component name(Pin pin) {
		EmiRecipe recipe = recipe(pin.recipe);
		return (Component)(recipe != null && !recipe.getOutputs().isEmpty() ? recipe.getOutputs().get(0).getName() : Component.literal(pin.recipe));
	}

	private static void left() {
		world = "";
		team = null;
		PINS.clear();
		FRESH.clear();
		SYNCED.clear();
		WAITING.clear();
		givenPending = false;
		changed();
		TeamPage.teamChanged(true);
		TeamTrees.forget();
		given.save();
	}

	@Nullable
	private static UUID parse(String id) {
		if (id != null && !id.isBlank()) {
			try {
				return UUID.fromString(id);
			} catch (IllegalArgumentException var2) {
				return null;
			}
		} else {
			return null;
		}
	}

	static GivenOnce given() {
		return given;
	}

	private TeamPinsClient() {
	}
}
