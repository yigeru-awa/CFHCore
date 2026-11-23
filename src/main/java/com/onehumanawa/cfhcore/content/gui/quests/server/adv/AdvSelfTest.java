package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.server.BookDoor;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestsServer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class AdvSelfTest {
	private static final String SCRATCH = "adv-selftest-";
	private final MinecraftServer server;
	private final List<String> failures = new ArrayList<>();
	private int passed;
	private int steps;

	private AdvSelfTest(MinecraftServer server) {
		this.server = server;
	}

	public static AdvSelfTest.Report run(MinecraftServer server, @Nullable ServerPlayer player) {
		AdvSelfTest t = new AdvSelfTest(server);

		try {
			t.choices();
			t.criteria();
			t.trees();
			t.book();
			t.live(player);
		} catch (Throwable var7) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] convert selftest threw", var7);
			t.failures.add("the self-test threw: " + var7);
		} finally {
			t.sweep();
		}

		for (String f : t.failures) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] convert selftest FAILED: {}", f);
		}

		CFHGuiQuests.LOGGER.info("[CFHGui Quests] convert selftest: {} passed, {} failed", t.passed, t.failures.size());
		return new AdvSelfTest.Report(t.passed, t.failures);
	}

	private void is(String what, boolean ok) {
		if (ok) {
			this.passed++;
		} else {
			this.failures.add(what);
		}
	}

	private void choices() {
		AdvOptions o = new AdvOptions();
		this.is("a conversion takes no rewards over by default: the game gives the advancement's when it is made", !o.rewards);
		this.is("rewards=true takes them over", o.word("rewards=true") && o.rewards);
		this.is("rewards=false leaves them to the game again", o.word("rewards=false") && !o.rewards);
		this.is("a word that is neither is no choice, and changes nothing", !o.word("rewards=maybe") && !o.rewards);
		this.is("choices kept without the word read it off", !((AdvOptions)BookJson.GSON.fromJson("{\"tasks\":\"translate\"}", AdvOptions.class)).rewards);
		this.is(
			"and choices kept with rewards on read it on (a tree converted so starts from it)",
			((AdvOptions)BookJson.GSON.fromJson("{\"rewards\":true}", AdvOptions.class)).rewards
		);
	}

	@Nullable
	private Criterion<?> criterion(String json) {
		return (Criterion<?>)Criterion.CODEC
			.parse(this.server.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
			.result()
			.orElse(null);
	}

	private AdvCriteria.Piece read(AdvCriteria reader, String json) {
		Criterion<?> c = this.criterion(json);
		if (c == null) {
			this.failures.add("the game cannot read the criterion " + json);
			return AdvCriteria.Piece.cannot("unreadable");
		} else {
			return reader.read(c);
		}
	}

	@Nullable
	private static Task only(AdvCriteria.Piece p) {
		return p.said() && p.tasks().size() == 1 ? p.tasks().get(0) : null;
	}

	private void criteria() {
		AdvCriteria r = new AdvCriteria(this.server);
		String inv = "{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}";
		Task t = only(
			this.read(r, String.format("{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}", "{\"items\":\"minecraft:diamond\"}"))
		);
		this.is("an item in the inventory is an item task", t != null && t.is("item") && t.item.equals("minecraft:diamond") && t.count == 1L);
		t = only(
			this.read(
				r, String.format("{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}", "{\"items\":\"#minecraft:logs\",\"count\":{\"min\":4}}")
			)
		);
		this.is("a tag and \"at least four\" are a tag and a count of four", t != null && t.item.equals("#minecraft:logs") && t.count == 4L);
		t = only(
			this.read(
				r,
				String.format("{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}", "{\"items\":[\"minecraft:coal\",\"minecraft:charcoal\"]}")
			)
		);
		this.is("several items in one predicate are any of them", t != null && t.item.equals("minecraft:coal | minecraft:charcoal"));
		AdvCriteria.Piece p = this.read(
			r,
			String.format(
				"{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}",
				"{\"items\":\"minecraft:iron_helmet\"},{\"items\":\"minecraft:iron_boots\"}"
			)
		);
		this.is("two predicates in one criterion are two tasks", p.said() && p.tasks().size() == 2);
		this.is(
			"an exact count is not \"at least\"",
			!this.read(
					r, String.format("{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}", "{\"items\":\"minecraft:diamond\",\"count\":3}")
				)
				.said()
		);
		this.is(
			"a word on the player is more than an item task says",
			!this.read(
					r,
					"{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"player\":{\"type\":\"minecraft:player\",\"flags\":{\"is_sneaking\":true}},\"items\":[{\"items\":\"minecraft:diamond\"}]}}"
				)
				.said()
		);
		t = only(
			this.read(
				r,
				String.format(
					"{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[%s]}}",
					"{\"items\":\"minecraft:diamond_sword\",\"components\":{\"minecraft:damage\":0}}"
				)
			)
		);
		this.is("components go into the item's brackets", t != null && t.item.startsWith("minecraft:diamond_sword[minecraft:damage="));
		if (t != null) {
			Chapter scratch = new Chapter();
			scratch.id = "adv-selftest-spec";
			Quest q = new Quest();
			q.id = "q";
			q.tasks.add(t);
			scratch.quests.add(q);
			this.is("and the server reads them back (" + t.item + ")", BookDoor.unreadable(scratch.sanitize()).isEmpty());
		}

		String kill = "{\"trigger\":\"minecraft:player_killed_entity\",\"conditions\":{%s}}";
		t = only(this.read(r, String.format("{\"trigger\":\"minecraft:player_killed_entity\",\"conditions\":{%s}}", "\"entity\":{\"type\":\"minecraft:zombie\"}")));
		this.is("a mob killed is a kill task", t != null && t.is("kill") && t.entity.equals("minecraft:zombie"));
		t = only(
			this.read(r, String.format("{\"trigger\":\"minecraft:player_killed_entity\",\"conditions\":{%s}}", "\"entity\":{\"type\":\"#minecraft:skeletons\"}"))
		);
		this.is("a tag of mobs is a tag", t != null && t.entity.equals("#minecraft:skeletons"));
		t = only(this.read(r, "{\"trigger\":\"minecraft:player_killed_entity\"}"));
		this.is("any mob killed is a kill task that names none", t != null && t.is("kill") && t.entity.isEmpty());
		this.is(
			"how a mob died is more than a kill task says",
			!this.read(
					r,
					String.format(
						"{\"trigger\":\"minecraft:player_killed_entity\",\"conditions\":{%s}}",
						"\"entity\":{\"type\":\"minecraft:ghast\"},\"killing_blow\":{\"tags\":[{\"id\":\"minecraft:is_projectile\",\"expected\":true}]}"
					)
				)
				.said()
		);
		this.is(
			"where a mob was is more than a kill task says",
			!this.read(
					r,
					String.format(
						"{\"trigger\":\"minecraft:player_killed_entity\",\"conditions\":{%s}}",
						"\"entity\":{\"type\":\"minecraft:ghast\",\"location\":{\"dimension\":\"minecraft:overworld\"}}"
					)
				)
				.said()
		);
		String at = "{\"trigger\":\"minecraft:location\",\"conditions\":{\"player\":{\"location\":{%s}}}}";
		t = only(
			this.read(r, String.format("{\"trigger\":\"minecraft:location\",\"conditions\":{\"player\":{\"location\":{%s}}}}", "\"biomes\":\"minecraft:plains\""))
		);
		this.is("a biome entered is a place", t != null && t.is("location") && t.biome.equals("minecraft:plains"));
		t = only(
			this.read(r, String.format("{\"trigger\":\"minecraft:location\",\"conditions\":{\"player\":{\"location\":{%s}}}}", "\"structures\":\"minecraft:fortress\""))
		);
		this.is("a structure entered is a place", t != null && t.structure.equals("minecraft:fortress"));
		t = only(
			this.read(
				r,
				String.format(
					"{\"trigger\":\"minecraft:location\",\"conditions\":{\"player\":{\"location\":{%s}}}}",
					"\"dimension\":\"minecraft:the_end\",\"position\":{\"x\":{\"min\":-8,\"max\":8},\"y\":{\"min\":60,\"max\":70},\"z\":{\"min\":-8,\"max\":8}}"
				)
			)
		);
		this.is(
			"a position between bounds is a box", t != null && t.dimension.equals("minecraft:the_end") && t.x == -8 && t.sizeX == 17 && t.y == 60 && t.sizeY == 11
		);
		this.is(
			"a height alone is no box",
			!this.read(r, String.format("{\"trigger\":\"minecraft:location\",\"conditions\":{\"player\":{\"location\":{%s}}}}", "\"position\":{\"y\":{\"min\":319}}"))
				.said()
		);
		this.is(
			"smoke is more than a place says",
			!this.read(r, String.format("{\"trigger\":\"minecraft:location\",\"conditions\":{\"player\":{\"location\":{%s}}}}", "\"smokey\":true")).said()
		);
		p = this.read(r, "{\"trigger\":\"minecraft:tick\"}");
		this.is("a tick without conditions is nothing to do", p.said() && p.tasks().isEmpty());
		t = only(this.read(r, "{\"trigger\":\"minecraft:changed_dimension\",\"conditions\":{\"to\":\"minecraft:the_nether\"}}"));
		this.is("a dimension changed to is a place", t != null && t.is("location") && t.dimension.equals("minecraft:the_nether"));
		this.is(
			"where from is more than a place says",
			!this.read(r, "{\"trigger\":\"minecraft:changed_dimension\",\"conditions\":{\"from\":\"minecraft:the_end\",\"to\":\"minecraft:overworld\"}}").said()
		);
		t = only(
			this.read(
				r,
				"{\"trigger\":\"minecraft:placed_block\",\"conditions\":{\"location\":[{\"condition\":\"minecraft:block_state_property\",\"block\":\"minecraft:wheat\"}]}}"
			)
		);
		this.is("a block placed is a place task", t != null && t.is("place") && t.block.equals("minecraft:wheat"));
		this.is(
			"a block's state is more than a place task says",
			!this.read(
					r,
					"{\"trigger\":\"minecraft:placed_block\",\"conditions\":{\"location\":[{\"condition\":\"minecraft:block_state_property\",\"block\":\"minecraft:wheat\",\"properties\":{\"age\":\"7\"}}]}}"
				)
				.said()
		);
		t = only(this.read(r, "{\"trigger\":\"minecraft:recipe_crafted\",\"conditions\":{\"recipe_id\":\"minecraft:crafting_table\"}}"));
		this.is("a recipe with one result is a craft task", t != null && t.is("craft") && t.item.equals("minecraft:crafting_table"));
		this.is(
			"an armour trim names no item to make",
			!this.read(r, "{\"trigger\":\"minecraft:recipe_crafted\",\"conditions\":{\"recipe_id\":\"minecraft:coast_armor_trim_smithing_template_smithing_trim\"}}")
				.said()
		);
		t = only(this.read(r, "{\"trigger\":\"minecraft:slept_in_bed\"}"));
		this.is("sleeping is the statistic the game counts it in", t != null && t.is("stat") && t.stat.equals("minecraft:custom/minecraft:sleep_in_bed"));
		t = only(this.read(r, "{\"trigger\":\"minecraft:villager_trade\"}"));
		this.is("trading is its statistic", t != null && t.stat.equals("minecraft:custom/minecraft:traded_with_villager"));
		t = only(this.read(r, "{\"trigger\":\"minecraft:bred_animals\"}"));
		this.is("breeding is its statistic", t != null && t.stat.equals("minecraft:custom/minecraft:animals_bred"));
		this.is(
			"breeding a certain animal is more than the statistic says",
			!this.read(r, "{\"trigger\":\"minecraft:bred_animals\",\"conditions\":{\"child\":{\"type\":\"minecraft:cow\"}}}").said()
		);
		this.is(
			"eating is no task of the book's",
			!this.read(r, "{\"trigger\":\"minecraft:consume_item\",\"conditions\":{\"item\":{\"items\":\"minecraft:apple\"}}}").said()
		);
		this.is("taming is no task of the book's", !this.read(r, "{\"trigger\":\"minecraft:tame_animal\"}").said());
		this.is("what only a command grants is no task of the book's", !this.read(r, "{\"trigger\":\"minecraft:impossible\"}").said());
	}

	private static AdvOptions options(String tasks, String order, String kind) {
		AdvOptions o = new AdvOptions();
		o.tasks = tasks;
		o.order = order;
		o.kind = kind;
		return o.sanitize();
	}

	private static List<AdvancementNode> shown(AdvancementNode root) {
		List<AdvancementNode> out = new ArrayList<>();
		Deque<AdvancementNode> nodes = new ArrayDeque<>();
		nodes.push(root);

		while (!nodes.isEmpty()) {
			AdvancementNode n = nodes.pop();
			if (n.advancement().display().isPresent()) {
				out.add(n);
			}

			for (AdvancementNode child : n.children()) {
				nodes.push(child);
			}
		}

		return out;
	}

	@Nullable
	private static Quest of(Chapter c, String advancement) {
		for (Quest q : c.quests) {
			if (!q.tags.isEmpty() && q.tags.get(0).equals(advancement)) {
				return q;
			}
		}

		return null;
	}

	private void trees() {
		AdvConverter converter = new AdvConverter(this.server);
		List<AdvancementNode> roots = AdvTrees.roots(this.server);
		this.is("the game has advancement trees to convert", !roots.isEmpty());
		int trees = 0;
		int quests = 0;
		int translated = 0;
		int partly = 0;
		int kept = 0;
		int paying = 0;

		for (AdvancementNode root : roots) {
			String name = root.holder().id().toString();
			List<AdvancementNode> shown = shown(root);
			AdvOptions[] ways = new AdvOptions[]{
				options("translate", "tree", "quest"), options("advancement", "tree", "quest"), options("translate", "free", "advancement")
			};
			ways[0].rewards = true;

			for (int way = 0; way < ways.length; way++) {
				AdvOptions o = ways[way];
				AdvConverter.Made made = converter.convert(root, o, AdvConverter.chapterId(root.holder().id()), "");
				Chapter c = made.chapter();
				AdvAccount a = made.account();
				String where = name + " (" + o.tasks + ", " + o.order + ", " + o.kind + "): ";
				boolean all = c.quests.size() == shown.size();

				for (AdvancementNode n : shown) {
					if (of(c, n.holder().id().toString()) == null) {
						all = false;
					}
				}

				this.is(where + "every advancement with a display is a quest (" + c.quests.size() + " of " + shown.size() + ")", all);
				this.is(where + "the account counts them", a.quests == shown.size() && a.translated + a.partly + a.kept == a.quests);
				Set<String> ids = new HashSet<>();
				Set<String> places = new HashSet<>();
				boolean valid = true;
				boolean hang = true;
				boolean apart = true;
				boolean within = true;
				boolean hidden = true;
				boolean frames = true;
				boolean own = true;

				for (Quest q : c.quests) {
					if (!Ids.valid(q.id) || !ids.add(q.id)) {
						valid = false;
					}

					if (!places.add(q.x + "/" + q.y)) {
						apart = false;
					}

					if (q.tasks.size() > 64 || q.rewards.size() > 64) {
						within = false;
					}
				}

				for (AdvancementNode nx : shown) {
					Quest q = of(c, nx.holder().id().toString());
					if (q != null) {
						DisplayInfo d = (DisplayInfo)nx.advancement().display().orElseThrow();
						if (nx == root ? !q.deps.isEmpty() : q.deps.size() != 1 || c.quest(q.deps.get(0)) == null) {
							hang = false;
						}

						if (q.looseDeps != o.free()) {
							hang = false;
						}

						boolean follows = nx != root
							&& c.quest(q.deps.isEmpty() ? "" : q.deps.get(0)) != null
							&& c.quest(q.deps.get(0)).visibility != Quest.Visibility.VISIBLE
							&& !o.free();
						Quest.Visibility wanted = d.isHidden()
							? (o.free() ? Quest.Visibility.INVISIBLE : Quest.Visibility.HIDDEN)
							: (follows ? Quest.Visibility.FOLLOW : Quest.Visibility.VISIBLE);
						if (q.visibility != wanted) {
							hidden = false;
						}

						Quest.Frame frame = d.getType() == AdvancementType.GOAL
							? Quest.Frame.GOAL
							: (d.getType() == AdvancementType.CHALLENGE ? Quest.Frame.CHALLENGE : Quest.Frame.TASK);
						if (q.frame != frame || q.isAdvancement() != o.advancementKind()) {
							frames = false;
						}

						if (!o.translate()
							&& (
								q.tasks.size() != 1
									|| !q.tasks.get(0).is("advancement")
									|| !q.tasks.get(0).advancement.equals(nx.holder().id().toString())
									|| !q.tasks.get(0).criterion.isEmpty()
							)) {
							own = false;
						}

						for (Task t : q.tasks) {
							if (t.is("advancement") && !t.criterion.isEmpty() && !nx.advancement().criteria().containsKey(t.criterion)) {
								own = false;
							}
						}

						if (q.anyTask && nx.advancement().requirements().requirements().size() != 1) {
							own = false;
						}
					}
				}

				this.is(where + "every id can be one and is there once", valid);
				this.is(where + "every quest but the first hangs on a quest of its chapter", hang);
				this.is(where + "no two quests stand in one place", apart);
				this.is(where + "no quest has more tasks or rewards than a quest may", within);
				boolean pays = false;

				for (Quest q : c.quests) {
					if (!q.rewards.isEmpty()) {
						pays = true;
					}
				}

				if (o.rewards) {
					if (pays) {
						paying++;
					}
				} else {
					this.is(where + "no quest pays a reward of its own: the game gives the advancement's", !pays && a.rewards == 0);
				}

				this.is(where + "what hides itself is out of sight", hidden);
				this.is(where + "frames and kinds are kept", frames);
				this.is(where + "the tasks are the advancement's", own);
				this.is(where + "the server reads every task", BookDoor.unreadable(c).isEmpty());
				this.is(
					where + "the chapter has the tab's name, picture and ground",
					!c.title.isBlank()
						&& !c.icon.isBlank()
						&& c.backdrop.equals(((DisplayInfo)root.advancement().display().orElseThrow()).getBackground().<String>map(ResourceLocation::toString).orElse(""))
				);
				if (way == 0) {
					trees++;
					quests += a.quests;
					translated += a.translated;
					partly += a.partly;
					kept += a.kept;
					CFHGuiQuests.LOGGER
						.info(
							"[CFHGui Quests] convert selftest: {} -> {} quests, {} with tasks of their own, {} partly, {} kept the advancement{}",
							new Object[]{name, a.quests, a.translated, a.partly, a.kept, a.triggers.isEmpty() ? "" : " (no task for: " + a.triggersInWords(12) + ")"}
						);
					if (root.holder().id().getNamespace().equals("minecraft")) {
						this.known(c, a);
					}
				}
			}
		}

		this.is("asked for, the game's trees hand their rewards to the quests (" + paying + " trees do)", paying > 0);
		CFHGuiQuests.LOGGER
			.info(
				"[CFHGui Quests] convert selftest: {} trees, {} quests in all: {} with tasks of their own, {} partly, {} kept the advancement",
				new Object[]{trees, quests, translated, partly, kept}
			);
	}

	private static boolean task(@Nullable Quest q, int tasks, String type) {
		if (q != null && q.tasks.size() == tasks) {
			for (Task t : q.tasks) {
				if (!t.is(type)) {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	private void known(Chapter c, AdvAccount a) {
		Quest q = of(c, "minecraft:story/root");
		if (q != null) {
			this.is("Minecraft: a crafting table in the inventory", task(q, 1, "item") && q.tasks.get(0).item.equals("minecraft:crafting_table"));
			this.is("the tree starts with a hexagon", q.shape == Quest.Shape.HEXAGON && q.x == 0.0F && q.y == 0.0F);
		}

		q = of(c, "minecraft:story/mine_stone");
		if (q != null) {
			this.is(
				"Stone Age: any stone for tools", task(q, 1, "item") && q.tasks.get(0).item.equals("#minecraft:stone_tool_materials") && q.deps.equals(List.of("root"))
			);
		}

		q = of(c, "minecraft:story/obtain_armor");
		if (q != null) {
			this.is("Suit Up: any one of four pieces", task(q, 4, "item") && q.anyTask);
		}

		q = of(c, "minecraft:story/enter_the_nether");
		if (q != null) {
			this.is("We Need to Go Deeper: the Nether as a place", task(q, 1, "location") && q.tasks.get(0).dimension.equals("minecraft:the_nether"));
		}

		q = of(c, "minecraft:story/enchant_item");
		if (q != null) {
			this.is("Enchanter: the statistic of enchanting", task(q, 1, "stat") && q.tasks.get(0).stat.equals("minecraft:custom/minecraft:enchant_item"));
		}

		q = of(c, "minecraft:nether/netherite_armor");
		if (q != null) {
			this.is(
				"Cover Me in Debris: all four pieces, a challenge, a star",
				task(q, 4, "item") && !q.anyTask && q.frame == Quest.Frame.CHALLENGE && q.shape == Quest.Shape.STAR
			);
			this.is("and, rewards being asked for, its hundred experience", q.rewards.size() == 1 && q.rewards.get(0).is("xp") && q.rewards.get(0).xp == 100);
		}

		q = of(c, "minecraft:nether/find_fortress");
		if (q != null) {
			this.is("A Terrible Fortress: the fortress as a place", task(q, 1, "location") && q.tasks.get(0).structure.equals("minecraft:fortress"));
		}

		q = of(c, "minecraft:adventure/kill_a_mob");
		if (q != null) {
			this.is("Monster Hunter: any one mob of many", q.anyTask && q.tasks.size() > 20 && task(q, q.tasks.size(), "kill"));
		}

		q = of(c, "minecraft:adventure/kill_all_mobs");
		if (q != null) {
			this.is("Monsters Hunted: every one of them", !q.anyTask && q.tasks.size() > 20 && task(q, q.tasks.size(), "kill"));
		}

		q = of(c, "minecraft:adventure/adventuring_time");
		if (q != null) {
			this.is("Adventuring Time: every biome a place", !q.anyTask && q.tasks.size() > 40 && task(q, q.tasks.size(), "location") && !q.tasks.get(0).biome.isEmpty());
		}

		q = of(c, "minecraft:adventure/sleep_in_bed");
		if (q != null) {
			this.is("Sweet Dreams: the statistic of sleeping", task(q, 1, "stat"));
		}

		q = of(c, "minecraft:adventure/trade");
		if (q != null) {
			this.is("What a Deal!: the statistic of trading", task(q, 1, "stat"));
		}

		q = of(c, "minecraft:husbandry/plant_seed");
		if (q != null) {
			this.is("A Seedy Place: any one of the crops placed", q.anyTask && q.tasks.size() > 3 && task(q, q.tasks.size(), "place"));
		}

		q = of(c, "minecraft:husbandry/balanced_diet");
		if (q != null) {
			this.is(
				"A Balanced Diet keeps the advancement, a task a food",
				task(q, q.tasks.size(), "advancement")
					&& q.tasks.size() > 30
					&& !q.tasks.get(0).criterion.isEmpty()
					&& "kept".equals(a.marks.get(q.id))
					&& "trigger".equals(a.why.get(q.id))
			);
		}

		q = of(c, "minecraft:husbandry/tame_an_animal");
		if (q != null) {
			this.is(
				"Best Friends Forever keeps the advancement whole", task(q, 1, "advancement") && q.tasks.get(0).criterion.isEmpty() && "kept".equals(a.marks.get(q.id))
			);
		}

		q = of(c, "minecraft:end/root");
		if (q != null) {
			this.is("The End?: the End as a place", task(q, 1, "location") && q.tasks.get(0).dimension.equals("minecraft:the_end"));
		}

		q = of(c, "minecraft:nether/all_potions");
		if (q != null) {
			this.is("A Furious Cocktail is a challenge that keeps the advancement", q.frame == Quest.Frame.CHALLENGE && task(q, 1, "advancement"));
		}
	}

	private AdvService.Result convert(String root, String chapter, AdvOptions o) {
		o.names.put(root, new AdvOptions.Name(chapter, ""));
		AdvService.Result r = AdvService.convert(this.server, List.of(root), o);
		if (r.ok() || r.data() != null) {
			this.steps++;
		}

		return r;
	}

	private static AdvAccount account(AdvService.Result r) {
		if (r.data() instanceof AdvService.Done d && !d.accounts.isEmpty()) {
			return d.accounts.get(0);
		}

		return new AdvAccount();
	}

	private void book() {
		QuestBook book = BookDoor.book();
		List<AdvancementNode> roots = AdvTrees.roots(this.server);
		if (book != null && !roots.isEmpty()) {
			String root = roots.get(0).holder().id().toString();
			String id = "adv-selftest-book";
			int before = book.chapters.size();
			List<QuestBook.Group> groups = new ArrayList<>(book.groups);
			AdvOptions o = options("translate", "tree", "quest");
			o.group = "adv-selftest-group";
			AdvService.Result r = this.convert(root, "adv-selftest-book", o);
			this.is("a tree converts into a chapter of the book", r.ok() && book.chapter("adv-selftest-book") != null && "new".equals(account(r).mode));
			this.is(
				"and into the group it names, which the book then has",
				book.group(o.group) != null && book.chapter("adv-selftest-book") != null && book.chapter("adv-selftest-book").group.equals(o.group)
			);
			AdvService.Trees listed = AdvService.trees(this.server);
			boolean known = false;

			for (AdvTrees.Tree t : listed.trees) {
				if (t.root.equals(root) && t.converted.equals("adv-selftest-book")) {
					known = true;
				}
			}

			this.is("the list knows which chapter the tree became", known && listed.undo.contains("adv-selftest-book"));
			o = options("translate", "tree", "quest");
			o.group = "adv-selftest-group";
			r = this.convert(root, "adv-selftest-book", o);
			AdvAccount a = account(r);
			this.is(
				"converting it again changes nothing (updated " + a.updated + ", kept " + a.edited + ", added " + a.added + ", gone " + a.removed + ")",
				r.ok() && "merged".equals(a.mode) && a.updated == 0 && a.edited == 0 && a.added == 0 && a.removed == 0
			);
			Chapter edited = BookJson.copy(book.chapter("adv-selftest-book"));
			Quest first = edited.quests.get(0);
			first.title = "Edited by hand";
			Quest mine = new Quest();
			mine.id = "written_here";
			mine.x = -4.0F;
			mine.y = -4.0F;
			edited.quests.add(mine);
			this.is("the chapter can be edited like any other", "ok".equals(BookDoor.put(edited.sanitize())));
			o = options("advancement", "tree", "quest");
			o.group = "adv-selftest-group";
			r = this.convert(root, "adv-selftest-book", o);
			a = account(r);
			Chapter merged = book.chapter("adv-selftest-book");
			this.is(
				"merging keeps what was edited",
				r.ok() && merged != null && merged.quest(first.id) != null && merged.quest(first.id).title.equals("Edited by hand") && a.edited >= 1
			);
			this.is("and what was written here", merged != null && merged.quest("written_here") != null);
			this.is(
				"and takes the new conversion's word for the rest",
				merged != null
					&& merged.quest(first.id) != null
					&& merged.quest(first.id).tasks.size() == 1
					&& merged.quest(first.id).tasks.get(0).is("advancement")
					&& a.updated >= 1
			);
			o = options("translate", "tree", "quest");
			o.exists = "copy";
			r = this.convert(root, "adv-selftest-book", o);
			this.is(
				"a copy stands beside it under an id of its own",
				r.ok() && "copy".equals(account(r).mode) && book.chapter("adv-selftest-book_2") != null && book.chapter("adv-selftest-book") == merged
			);
			o = options("translate", "tree", "quest");
			o.exists = "replace";
			r = this.convert(root, "adv-selftest-book", o);
			this.is(
				"replacing puts the conversion in its place",
				r.ok()
					&& "replaced".equals(account(r).mode)
					&& book.chapter("adv-selftest-book") != null
					&& book.chapter("adv-selftest-book").quest("written_here") == null
			);
			r = AdvService.undo(this.server);
			this.steps--;
			this.is(
				"one undo takes a conversion back: the edited chapter stands again",
				r.ok() && book.chapter("adv-selftest-book") != null && book.chapter("adv-selftest-book").quest("written_here") != null
			);
			Chapter touched = BookJson.copy(book.chapter("adv-selftest-book_2"));
			touched.title = "Changed since";
			BookDoor.put(touched.sanitize());
			r = AdvService.undo(this.server);
			this.is(
				"a conversion whose chapter was edited since is not taken back over the edit",
				!r.ok() && "edited_since".equals(r.code()) && book.chapter("adv-selftest-book_2") != null
			);
			BookDoor.remove("adv-selftest-book_2");
			AdvStore.restore("adv-selftest-book_2", null);

			while (this.steps > 0) {
				r = AdvService.undo(this.server);
				this.steps--;
				if (!r.ok()) {
					break;
				}
			}

			this.sweep();
			this.is("everything is taken back: the book has the chapters it had", book.chapters.size() == before);
			this.is("and the groups it had", BookJson.GSON.toJson(book.groups).equals(BookJson.GSON.toJson(groups)));
			this.is(
				"a bad id stops a conversion before anything is written",
				!this.convert(root, "Not An Id", options("translate", "tree", "quest")).ok() && book.chapters.size() == before
			);
			Chapter hand = new Chapter();
			hand.id = "adv-selftest-hand";
			hand.title = "Written by hand";
			Quest own = new Quest();
			own.id = "own";
			hand.quests.add(own);
			BookDoor.put(hand.sanitize());
			r = this.convert(root, hand.id, options("translate", "tree", "quest"));
			this.is(
				"a chapter of the same id that no conversion made is not merged into: the tree stands beside it",
				r.ok()
					&& "copy".equals(account(r).mode)
					&& book.chapter(hand.id) != null
					&& book.chapter(hand.id).quests.size() == 1
					&& book.chapter(hand.id + "_2") != null
			);
			this.sweep();
			this.is("and is swept away with the rest", book.chapters.size() == before);
		}
	}

	private void sweep() {
		QuestBook book = BookDoor.book();
		if (book != null) {
			for (String id : new ArrayList<>(book.chapters.keySet())) {
				if (id.startsWith("adv-selftest-")) {
					BookDoor.remove(id);
				}
			}

			for (String idx : AdvStore.all().keySet()) {
				if (idx.startsWith("adv-selftest-")) {
					AdvStore.restore(idx, null);
				}
			}

			List<QuestBook.Group> groups = new ArrayList<>(book.groups);
			if (groups.removeIf(g -> g.id.startsWith("adv-selftest-"))) {
				BookDoor.groups(groups);
			}

			AdvService.forget(this.server, "adv-selftest-");
			this.steps = 0;
		}
	}

	private boolean done(ServerPlayer player, String key) {
		Rules.Status s = QuestsServer.apiStatus(player, key);
		return s.done();
	}

	private void live(@Nullable ServerPlayer player) {
		if (player != null && player.isCreative()) {
			QuestBook book = BookDoor.book();
			if (book != null) {
				AdvOptions o = options("translate", "free", "quest");
				o.rewards = false;
				String story = "adv-selftest-story";
				String adventure = "adv-selftest-adventure";
				String husbandry = "adv-selftest-husbandry";
				o.names.put("minecraft:story/root", new AdvOptions.Name("adv-selftest-story", ""));
				o.names.put("minecraft:adventure/root", new AdvOptions.Name("adv-selftest-adventure", ""));
				o.names.put("minecraft:husbandry/root", new AdvOptions.Name("adv-selftest-husbandry", ""));
				List<String> roots = new ArrayList<>();

				for (String root : List.of("minecraft:story/root", "minecraft:adventure/root", "minecraft:husbandry/root")) {
					if (AdvTrees.find(this.server, root).size() == 1) {
						roots.add(root);
					}
				}

				if (!roots.isEmpty()) {
					AdvService.Result made = AdvService.convert(this.server, roots, o);
					this.is("the game's trees convert into scratch chapters to play through", made.ok());
					if (made.ok()) {
						if (book.chapter("adv-selftest-story") != null && book.chapter("adv-selftest-story").quest("root") != null) {
							boolean had = player.getInventory().contains(new ItemStack(Items.CRAFTING_TABLE));
							if (!had) {
								this.is("Minecraft is not done before there is a crafting table", !this.done(player, "adv-selftest-story/root"));
								player.getInventory().add(new ItemStack(Items.CRAFTING_TABLE));
							}

							BookDoor.look();
							this.is("a crafting table in the inventory does the quest made from Minecraft", this.done(player, "adv-selftest-story/root"));
							if (!had) {
								player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.CRAFTING_TABLE), 1, player.inventoryMenu.getCraftSlots());
							}
						}

						if (book.chapter("adv-selftest-adventure") != null && book.chapter("adv-selftest-adventure").quest("kill_a_mob") != null) {
							boolean before = this.done(player, "adv-selftest-adventure/kill_a_mob");
							ServerLevel level = player.serverLevel();
							Zombie zombie = (Zombie)EntityType.ZOMBIE.create(level);
							if (zombie != null) {
								zombie.moveTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
								level.addFreshEntity(zombie);
								zombie.hurt(level.damageSources().playerAttack(player), Float.MAX_VALUE);
								this.is("a zombie killed does the quest made from Monster Hunter", !before && this.done(player, "adv-selftest-adventure/kill_a_mob"));
								if (!zombie.isRemoved()) {
									zombie.discard();
								}
							}
						}

						AdvancementHolder tame = this.server.getAdvancements().get(ResourceLocation.withDefaultNamespace("husbandry/tame_an_animal"));
						if (tame != null && book.chapter("adv-selftest-husbandry") != null && book.chapter("adv-selftest-husbandry").quest("tame_an_animal") != null) {
							AdvancementProgress progress = player.getAdvancements().getOrStartProgress(tame);
							if (!progress.isDone()) {
								BookDoor.look();
								this.is("Best Friends Forever is not done before the advancement is", !this.done(player, "adv-selftest-husbandry/tame_an_animal"));
								List<String> granted = new ArrayList<>();

								for (String criterion : progress.getRemainingCriteria()) {
									granted.add(criterion);
								}

								for (String criterion : granted) {
									player.getAdvancements().award(tame, criterion);
								}

								BookDoor.look();
								this.is("the advancement made does the quest that kept it", this.done(player, "adv-selftest-husbandry/tame_an_animal"));

								for (String criterion : granted) {
									player.getAdvancements().revoke(tame, criterion);
								}
							}
						}

						ServerLevel nether = this.server.getLevel(Level.NETHER);
						if (nether != null
							&& player.level().dimension() != Level.NETHER
							&& book.chapter("adv-selftest-story") != null
							&& book.chapter("adv-selftest-story").quest("enter_the_nether") != null) {
							ServerLevel home = player.serverLevel();
							double x = player.getX();
							double y = player.getY();
							double z = player.getZ();
							float yaw = player.getYRot();
							float pitch = player.getXRot();
							BookDoor.look();
							boolean before = this.done(player, "adv-selftest-story/enter_the_nether");
							player.teleportTo(nether, x / 8.0, 200.0, z / 8.0, yaw, pitch);
							BookDoor.look();
							this.is("being in the Nether does the quest made from We Need to Go Deeper", !before && this.done(player, "adv-selftest-story/enter_the_nether"));
							player.teleportTo(home, x, y, z, yaw, pitch);
						}

						AdvService.Result undone = AdvService.undo(this.server);
						this.is(
							"the scratch chapters are taken back by one undo",
							undone.ok()
								&& book.chapter("adv-selftest-story") == null
								&& book.chapter("adv-selftest-adventure") == null
								&& book.chapter("adv-selftest-husbandry") == null
						);
					}
				}
			}
		} else {
			CFHGuiQuests.LOGGER
				.info("[CFHGui Quests] convert selftest: the quests were not done for real (that part needs a player in creative mode, in a world to test in)");
		}
	}

	public static record Report(int passed, List<String> failures) {
	}
}
