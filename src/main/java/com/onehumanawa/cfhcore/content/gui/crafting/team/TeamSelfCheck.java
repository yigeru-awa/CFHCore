package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.GivenOnce;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamPinsClient;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeCodec;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeMarks;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public final class TeamSelfCheck {
	private static int passed;
	private static int failed;

	public static int run() {
		failed = 0;
		passed = 0;

		try {
			givenOnce();
			rules();
			wire();
			store();
			if (EmiReloadManager.isLoaded()) {
				realTree();
			} else {
				CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] team pins self-check: the viewer has no recipes yet, the real tree is not checked");
			}

			if (TeamPinsClient.active()) {
				page();
			} else {
				CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] team pins self-check: the player is in no team, the Team page is not checked");
			}
		} catch (Exception var1) {
			failed++;
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] team pins self-check threw", var1);
		}

		CFHGuiCrafting.LOGGER.info("[CFHGui Crafting] team pins self-check: {} passed, {} failed", passed, failed);
		return failed;
	}

	private static void check(String what, boolean ok) {
		if (ok) {
			passed++;
		} else {
			failed++;
			CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] team pins self-check FAILED: {}", what);
		}
	}

	private static void givenOnce() throws Exception {
		GivenOnce given = new GivenOnce(null);
		String a = GivenOnce.key("world-1", "team-a");
		String b = GivenOnce.key("world-1", "team-b");
		String other = GivenOnce.key("world-2", "team-a");
		check(
			"first sight gives every pinned recipe",
			given.due(a, List.of("minecraft:compass", "minecraft:clock")).equals(List.of("minecraft:compass", "minecraft:clock"))
		);
		check("a pin seen again is not given again", given.due(a, List.of("minecraft:compass", "minecraft:clock")).isEmpty());
		check("taken out of the favourites, it stays out", given.due(a, List.of("minecraft:compass")).isEmpty());
		check(
			"a new pin of the same team is given",
			given.due(a, List.of("minecraft:compass", "minecraft:clock", "minecraft:lantern")).equals(List.of("minecraft:lantern"))
		);
		check("unpinned and pinned again, it is not given again", given.due(a, List.of("minecraft:clock")).isEmpty());
		check("another team of the same world is given its own", given.due(b, List.of("minecraft:compass")).equals(List.of("minecraft:compass")));
		check("the same team in another world is given its own", given.due(other, List.of("minecraft:compass")).equals(List.of("minecraft:compass")));
		check("a recipe twice among the pins is given once", given.due(GivenOnce.key("w", "t"), List.of("x:y", "x:y")).equals(List.of("x:y")));
		Path tmp = Files.createTempFile("cfhgui-team-given", ".json");

		try {
			Files.writeString(tmp, given.toJson().toString());
			GivenOnce read = new GivenOnce(tmp);
			read.load();
			check("what was given is read back from the file", read.has(a, "minecraft:lantern") && read.has(b, "minecraft:compass") && !read.has(b, "minecraft:clock"));
			check("read back, nothing is given twice", read.due(a, List.of("minecraft:compass", "minecraft:clock", "minecraft:lantern")).isEmpty());
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	private static Pin recipePin(String recipe, UUID by) {
		Pin p = new Pin();
		p.id = UUID.randomUUID().toString();
		p.kind = "recipe";
		p.recipe = recipe;
		p.by = by;
		p.byName = "Alex";
		p.at = 1759400000000L;
		return p;
	}

	private static Pin treePin(String recipe, int steps, int resolutions, UUID by) {
		Pin p = recipePin(recipe, by);
		p.kind = "tree";
		JsonObject tree = new JsonObject();
		tree.addProperty("goal", "item:minecraft:blast_furnace");
		tree.addProperty("recipe", recipe);
		JsonArray res = new JsonArray();

		for (int i = 0; i < resolutions; i++) {
			JsonObject o = new JsonObject();
			o.addProperty("stack", "item:minecraft:stone");
			res.add(o);
		}

		if (resolutions > 0) {
			tree.add("resolutions", res);
		}

		p.tree = tree;
		p.steps = steps;
		return p;
	}

	private static void rules() {
		UUID owner = UUID.randomUUID();
		UUID officer = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		UUID outsider = UUID.randomUUID();
		TeamsLink.TeamInfo team = new TeamsLink.TeamInfo(UUID.randomUUID(), "Testers", List.of(owner, officer, member), Set.of(owner, officer));
		String asked = team.id().toString();
		Predicate<ResourceLocation> server = id -> id.equals(ResourceLocation.withDefaultNamespace("compass"));
		List<Pin> none = List.of();
		check("a member pins a recipe the server has", PinRules.refusePin(team, asked, none, recipePin("minecraft:compass", member), server) == null);
		check("one in no team may not pin", Objects.equals(PinRules.refusePin(null, asked, none, recipePin("minecraft:compass", outsider), server), "no_team"));
		check(
			"a client behind (another team meant) may not pin",
			Objects.equals(PinRules.refusePin(team, UUID.randomUUID().toString(), none, recipePin("minecraft:compass", member), server), "other_team")
		);
		check(
			"a recipe the server does not have is refused",
			Objects.equals(PinRules.refusePin(team, asked, none, recipePin("minecraft:clock_from_nowhere", member), server), "unknown_recipe")
		);
		check(
			"a recipe only the viewers make (a synthetic id) is taken",
			PinRules.refusePin(team, asked, none, recipePin("emi:/world/fluid_interaction/cobblestone", member), server) == null
		);
		check("an id that is no id is refused", Objects.equals(PinRules.refusePin(team, asked, none, recipePin("Not An Id!", member), server), "unknown_recipe"));
		check("an id of a bare slash is refused", Objects.equals(PinRules.refusePin(team, asked, none, recipePin("mod:/", member), server), "unknown_recipe"));
		check(
			"the same recipe twice is refused",
			Objects.equals(PinRules.refusePin(team, asked, List.of(recipePin("minecraft:compass", owner)), recipePin("minecraft:compass", member), server), "already")
		);
		List<Pin> full = new ArrayList<>();

		for (int i = 0; i < 100; i++) {
			full.add(recipePin("emi:/made/" + i, owner));
		}

		check("the 101st pin is refused", Objects.equals(PinRules.refusePin(team, asked, full, recipePin("minecraft:compass", member), server), "full"));
		check("the 100th pin is taken", PinRules.refusePin(team, asked, full.subList(0, 99), recipePin("minecraft:compass", member), server) == null);
		check("a tree of 2000 steps is taken", PinRules.refusePin(team, asked, none, treePin("minecraft:compass", 2000, 3, member), server) == null);
		check(
			"a tree of 2001 steps is refused", Objects.equals(PinRules.refusePin(team, asked, none, treePin("minecraft:compass", 2001, 3, member), server), "too_big")
		);
		check(
			"a tree that says it has few steps but holds more is refused",
			Objects.equals(PinRules.refusePin(team, asked, none, treePin("minecraft:compass", 5, 2001, member), server), "too_big")
		);
		Pin wrongGoal = treePin("minecraft:compass", 4, 1, member);
		wrongGoal.tree.addProperty("recipe", "minecraft:clock");
		check("a tree listed under another recipe than its own is refused", Objects.equals(PinRules.refusePin(team, asked, none, wrongGoal, server), "invalid"));
		Pin tree = treePin("minecraft:compass", 4, 1, member);
		Pin sameTree = treePin("minecraft:compass", 4, 1, owner);
		Pin otherTree = treePin("minecraft:compass", 4, 2, owner);
		check("the very same tree twice is refused", Objects.equals(PinRules.refusePin(team, asked, List.of(tree), sameTree, server), "already"));
		check("another tree of the same goal is taken", PinRules.refusePin(team, asked, List.of(tree), otherTree, server) == null);
		check(
			"a recipe pinned does not stand in the way of its tree",
			PinRules.refusePin(team, asked, List.of(recipePin("minecraft:compass", owner)), tree, server) == null
		);
		Pin byMember = recipePin("minecraft:compass", member);
		check("the one who pinned it may unpin it", PinRules.refuseUnpin(team, asked, member, byMember) == null);
		check("an officer may unpin anyone's", PinRules.refuseUnpin(team, asked, officer, byMember) == null);
		check("the owner may unpin anyone's", PinRules.refuseUnpin(team, asked, owner, byMember) == null);
		check("another member may not unpin it", Objects.equals(PinRules.refuseUnpin(team, asked, UUID.randomUUID(), byMember), "not_yours"));
		check("one in no team may not unpin", Objects.equals(PinRules.refuseUnpin(null, asked, member, byMember), "no_team"));
		check("a pin that is not there cannot be unpinned", Objects.equals(PinRules.refuseUnpin(team, asked, member, null), "no_pin"));
		check("unpinning for another team is refused", Objects.equals(PinRules.refuseUnpin(team, UUID.randomUUID().toString(), member, byMember), "other_team"));
	}

	private static boolean same(Pin a, Pin b) {
		return a.id.equals(b.id)
			&& a.kind.equals(b.kind)
			&& a.recipe.equals(b.recipe)
			&& Objects.equals(a.tree, b.tree)
			&& a.steps == b.steps
			&& a.by.equals(b.by)
			&& a.byName.equals(b.byName)
			&& a.at == b.at;
	}

	private static void wire() {
		UUID by = UUID.randomUUID();
		Pin[] pins = new Pin[]{recipePin("minecraft:compass", by), treePin("minecraft:compass", 12, 5, by)};
		byte[] packed = JsonWire.encode(pins);
		Pin[] back = (Pin[])JsonWire.decode(packed, Pin[].class);
		check("pins come through the wire as they went", back != null && back.length == 2 && same(pins[0], back[0]) && same(pins[1], back[1]) && back[1].sanitize());
		check("a pin without an id is let go", !new Pin().sanitize());
	}

	private static void store() throws Exception {
		Path dir = Files.createTempDirectory("cfhgui-team-pins");
		Path file = dir.resolve("team-pins.json");

		try {
			UUID team = UUID.randomUUID();
			UUID by = UUID.randomUUID();
			PinStore s = new PinStore(file);
			s.load();
			String world = s.world();
			Pin recipe = recipePin("minecraft:compass", by);
			Pin tree = treePin("minecraft:compass", 7, 4, by);
			s.add(team, recipe);
			s.add(team, tree);
			s.save();
			PinStore read = new PinStore(file);
			read.load();
			List<Pin> back = read.pins(team);
			check("the world keeps its id", world.equals(read.world()) && !world.isBlank());
			check("the world's file keeps a team's pins in their order", back.size() == 2 && same(recipe, back.get(0)) && same(tree, back.get(1)));
			read.remove(team, back.get(0));
			read.drop(UUID.randomUUID());
			read.save();
			PinStore again = new PinStore(file);
			again.load();
			check("a pin taken off is gone from the file", again.pins(team).size() == 1 && again.pins(team).get(0).isTree());
			again.drop(team);
			check("a team disbanded takes its pins with it", again.pins(team).isEmpty());
		} finally {
			Files.deleteIfExists(file);
			Files.deleteIfExists(dir);
		}
	}

	private static void page() {
		boolean shown = EmiConfig.leftSidebarPages.pages.stream().anyMatch(p -> p.type == SidebarType.TEAM);
		check("in a team, the Team page stands among the left sidebar's pages", shown);
		String line = null;

		for (String l : EmiConfig.getSavedConfig().split("\n")) {
			if (l.trim().startsWith("left-sidebar-pages:")) {
				line = l;
			}
		}

		check("the Team page put there is not written into emi.css", line != null && !line.contains(SidebarType.TEAM.getName()));
	}

	private static void realTree() {
		EmiRecipe furnace = EmiApi.getRecipeManager().getRecipe(ResourceLocation.withDefaultNamespace("blast_furnace"));
		EmiRecipe smooth = EmiApi.getRecipeManager().getRecipe(ResourceLocation.withDefaultNamespace("smooth_stone"));
		if (furnace != null && smooth != null) {
			MaterialTree tree = new MaterialTree(furnace);
			tree.addResolution(EmiStack.of(Items.SMOOTH_STONE), smooth);
			tree.batches = 3L;
			TreeMarks marks = new TreeMarks();
			marks.name = "Furnaces for the base";
			TreeSession original = new TreeSession(tree, marks).grown();
			JsonObject written = TreeCodec.write(original.tree, original.marks);
			check("a real tree can be written", written != null);
			if (written != null) {
				Pin pin = new Pin();
				pin.id = UUID.randomUUID().toString();
				pin.kind = "tree";
				pin.recipe = Objects.requireNonNull(PinRules.goalRecipe(written));
				pin.tree = written;
				pin.steps = 3;
				Pin[] back = (Pin[])JsonWire.decode(JsonWire.encode(new Pin[]{pin}), Pin[].class);
				check("the tree comes through the wire whole", back != null && back.length == 1 && written.equals(back[0].tree));
				if (back != null && back.length == 1 && back[0].tree != null) {
					TreeSession read = TreeCodec.read(back[0].tree);
					check("the tree is read back", read != null);
					if (read != null) {
						check("its goal's recipe is the same", read.tree.goal.recipe == furnace);
						check("its batches are the same", read.tree.batches == 3L);
						check("its chosen recipe is kept", read.tree.resolutions.get(EmiStack.of(Items.SMOOTH_STONE)) == smooth);
						check("its name is kept", "Furnaces for the base".equals(read.marks.name));
						JsonObject again = TreeCodec.write(read.tree, read.marks);
						check("written again, it is the tree that was pinned", written.equals(again));
					}
				}
			}
		} else {
			check("the viewer knows the blast furnace's recipes", false);
		}
	}

	private TeamSelfCheck() {
	}
}
