package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsServerConfig;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.net.QuestsNet;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.FtbAuto;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.FtbImport;
import com.onehumanawa.cfhcore.content.gui.quests.server.ftb.Snbt;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class SelfTest {
	private static final UUID ADA = new UUID(5351392L, 1L);
	private static final UUID BO = new UUID(5351392L, 2L);
	private static final UUID CREW = new UUID(5351392L, 100L);
	private final MinecraftServer server;
	private final List<String> failures = new ArrayList<>();
	private int passed;
	private SelfTest.Scratch world;
	private QuestEngine engine;

	private SelfTest(MinecraftServer server) {
		this.server = server;
	}

	static SelfTest.Report run(MinecraftServer server, @Nullable ServerPlayer runner) {
		SelfTest t = new SelfTest(server);
		t.group("rules", t::rules);
		t.group("tasks", t::tasks);
		t.group("what a done quest sets off", t::chain);
		t.group("rewards", t::rewards);
		t.group("claiming everything, choices too", t::claimAllPicking);
		t.group("repeatable quests", t::repeatable);
		t.group("resets", t::resets);
		t.group("teams", t::teams);
		t.group("renames", t::renames);
		t.group("the editor's mending", t::mending);
		t.group("reading the book's words", t::specs);
		t.group("the FTB importer", t::importer);
		t.group("what an FTB Quests book can ask and give", t::kinds);
		t.group("an FTB book converted again, and its progress", () -> {
			for (String failure : FtbAuto.selfCheck(result -> {
				if (result) {
					t.passed++;
				}
			})) {
				t.fail(failure);
			}
		});
		t.group("hooks into the game", t::hooks);
		if (runner != null) {
			t.group("the real readers", () -> t.live(runner));
		}

		if (runner != null && Boolean.getBoolean("cfh.quests.debug")) {
			t.group("the hooks, for real", () -> t.real(runner));
		}

		t.group("a large book", t::scale);
		CFHGuiQuests.LOGGER.info("[CFHGui Quests] selftest: {} passed, {} failed", t.passed, t.failures.size());
		return new SelfTest.Report(t.passed, List.copyOf(t.failures));
	}

	private void group(String name, Runnable checks) {
		try {
			checks.run();
		} catch (Throwable var4) {
			this.fail(name + ": stopped by " + var4);
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] selftest: {} threw", name, var4);
		}
	}

	private void is(String what, boolean ok) {
		if (ok) {
			this.passed++;
		} else {
			this.fail(what);
		}
	}

	private void eq(String what, Object want, Object got) {
		if (Objects.equals(want, got)) {
			this.passed++;
		} else {
			this.fail(what + " (expected " + want + ", was " + got + ")");
		}
	}

	private void fail(String what) {
		this.failures.add(what);
		CFHGuiQuests.LOGGER.error("[CFHGui Quests] selftest FAILED: {}", what);
	}

	private void fresh() {
		this.world = new SelfTest.Scratch();
		this.engine = new QuestEngine(this.world, new HolderStore(null), this.server.registryAccess(), 42L);
		this.engine.book(book());
	}

	private Seat sit(UUID id, String name) {
		return this.engine.join(id, name, null);
	}

	private boolean done(Seat s, String key) {
		QuestProgress p = this.engine.peek(s, key);
		return p != null && p.done;
	}

	private long count(Seat s, String key, int task) {
		QuestProgress p = this.engine.peek(s, key);
		return p == null ? 0L : p.task(task);
	}

	private boolean heard(Seat s, String kind, String subject) {
		this.engine.flush();
		return this.world.happened(s, kind, subject);
	}

	private String submit(Seat s, String key) {
		return this.engine.submit(s, key, 0).code();
	}

	private static QuestBook book() {
		QuestBook book = new QuestBook();
		book.title = "Scratch";
		chapter(
			book,
			"main",
			"{\"title\": \"Main\", \"quests\": [\n  {\"id\": \"start\"},\n  {\"id\": \"logs\", \"deps\": [\"start\"], \"tasks\": [{\"item\": \"#minecraft:logs\", \"count\": 4}], \"rewards\": [{\"item\": \"minecraft:apple\", \"count\": 3}, {\"xp\": 10}]},\n  {\"id\": \"planks\", \"deps\": [\"logs\"], \"tasks\": [{\"type\": \"craft\", \"item\": \"minecraft:oak_planks\", \"count\": 8}],\n   \"rewards\": [{\"type\": \"choice\", \"choices\": [{\"item\": \"minecraft:bread\", \"count\": 6}, {\"item\": \"minecraft:cooked_beef\", \"count\": 4}]}]},\n  {\"id\": \"zombies\", \"deps\": [\"logs\"], \"teamXp\": 25, \"tasks\": [{\"entity\": \"minecraft:zombie\", \"count\": 3}, {\"entity\": \"#minecraft:skeletons\", \"count\": 2}],\n   \"rewards\": [{\"item\": \"minecraft:diamond\", \"team\": true}, {\"xp\": 5}]},\n  {\"id\": \"stone\", \"tasks\": [{\"type\": \"break\", \"block\": \"#minecraft:base_stone_overworld\", \"count\": 5}], \"rewards\": [{\"type\": \"team_xp\", \"xp\": 7}]},\n  {\"id\": \"mending\", \"tasks\": [{\"item\": \"*[stored_enchantments~[{enchantments:\\\"minecraft:mending\\\"}]]\"}]},\n  {\"id\": \"torch\", \"tasks\": [{\"type\": \"place\", \"block\": \"minecraft:torch\", \"count\": 2}]},\n  {\"id\": \"wheat\", \"repeatable\": true, \"cooldownSeconds\": 60, \"tasks\": [{\"item\": \"minecraft:wheat\", \"count\": 12, \"consume\": true}],\n   \"rewards\": [{\"item\": \"minecraft:bone_meal\", \"count\": 12}]},\n  {\"id\": \"bread\", \"deps\": [\"wheat\"], \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"pay\", \"tasks\": [{\"type\": \"xp\", \"count\": 5, \"consume\": true}]},\n  {\"id\": \"levels\", \"tasks\": [{\"type\": \"xp\", \"count\": 10}]},\n  {\"id\": \"jump\", \"tasks\": [{\"stat\": \"minecraft:custom/minecraft:jump\", \"count\": 50}]},\n  {\"id\": \"adv\", \"tasks\": [{\"advancement\": \"minecraft:story/root\"}]},\n  {\"id\": \"nether\", \"tasks\": [{\"type\": \"location\", \"dimension\": \"minecraft:the_nether\"}]},\n  {\"id\": \"look\", \"tasks\": [{\"type\": \"observe\", \"block\": \"minecraft:diamond_block\", \"seconds\": 1}]},\n  {\"id\": \"tick\", \"autoClaim\": true, \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [{\"item\": \"minecraft:stick\", \"count\": 2}]},\n  {\"id\": \"after\", \"tasks\": [{\"type\": \"quest\", \"quest\": \"tick\"}]},\n  {\"id\": \"chap\", \"tasks\": [{\"type\": \"chapter\", \"chapter\": \"side\"}]},\n  {\"id\": \"crew\", \"tasks\": [{\"type\": \"team\", \"value\": \"members\", \"count\": 2}]},\n  {\"id\": \"boss\", \"tasks\": [{\"type\": \"custom\", \"value\": \"boss\", \"count\": 2}]},\n  {\"id\": \"either\", \"anyTask\": true, \"tasks\": [{\"entity\": \"minecraft:pig\"}, {\"item\": \"minecraft:diamond\"}]},\n  {\"id\": \"gate\", \"subtree\": \"tree\"},\n  {\"id\": \"potion\", \"tasks\": [{\"item\": \"minecraft:potion[potion_contents={potion:\\\"minecraft:swiftness\\\"}]\"}]},\n  {\"id\": \"wither\", \"kind\": \"advancement\", \"frame\": \"challenge\", \"title\": \"Withering\", \"tasks\": [{\"entity\": \"minecraft:wither\"}], \"rewards\": [{\"xp\": 500}]},\n  {\"id\": \"own\", \"sync\": \"player\", \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [{\"xp\": 1}]},\n  {\"id\": \"one_of\", \"deps\": [\"tick\", \"boss\"], \"depMode\": \"any\"},\n  {\"id\": \"lucky\", \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [{\"type\": \"random\", \"choices\": [{\"item\": \"minecraft:coal\", \"weight\": 1}, {\"item\": \"minecraft:emerald\", \"weight\": 3}]}]}\n]}"
		);
		chapter(
			book,
			"tree",
			"{\"title\": \"Tree\", \"parent\": \"main\", \"quests\": [\n  {\"id\": \"t1\", \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"t2\", \"deps\": [\"t1\"], \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"t3\", \"optional\": true, \"tasks\": [{\"type\": \"checkmark\"}]}\n]}"
		);
		chapter(
			book,
			"side",
			"{\"title\": \"Side\", \"requires\": [\"main/tick\"], \"quests\": [\n  {\"id\": \"s1\", \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"s2\", \"deps\": [\"s1\"]}\n]}"
		);
		chapter(book, "loose", "{\"title\": \"Loose\", \"parent\": \"side\", \"quests\": [{\"id\": \"l1\", \"tasks\": [{\"type\": \"checkmark\"}]}]}");
		return book.sanitize();
	}

	private static void chapter(QuestBook book, String id, String json) {
		Chapter c = (Chapter)BookJson.GSON.fromJson(json, Chapter.class);
		c.id = id;
		book.chapters.put(id, c.sanitize());
	}

	private void rules() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.is("a quest without tasks is done the moment it is open", this.done(a, "main/start"));
		this.is("a quest whose dependency is done is open", a.open.contains("main/logs"));
		this.eq("a quest whose dependency is not done is locked", Rules.Status.LOCKED, this.engine.status(a, "main/planks"));
		this.eq("an open quest with nothing done is available", Rules.Status.AVAILABLE, this.engine.status(a, "main/logs"));
		this.is("a chapter that requires a quest is closed until that quest is done", !a.open.contains("side/s1"));
		this.is("a tree's quests are open once the quest that leads into it is", a.open.contains("tree/t1") && !a.open.contains("tree/t2"));
		this.is("a tree no quest leads into is closed while its parent chapter is", !a.open.contains("loose/l1"));
		this.is("a quest that waits for one of two is not done while neither is", !this.done(a, "main/one_of"));
		this.eq("the status of a quest that is not in the book is hidden", Rules.Status.HIDDEN, this.engine.status(a, "main/nope"));
		this.eq("the book is sent on sitting down: its head", 1, this.world.count(a, QuestsNet.BookStart.class));
		this.eq("the book is sent on sitting down: a chapter a payload", 4, this.world.count(a, QuestsNet.ChapterData.class));
		this.eq("the book is sent on sitting down: its end", 1, this.world.count(a, QuestsNet.BookEnd.class));
		QuestState start = this.world.lastState(a, "main/start");
		this.is("all progress is sent on sitting down, done quests as done", this.world.resets(a) == 1 && start != null && start.done && "Ada".equals(start.doneBy));
		this.engine.crafted(a, new ItemStack(Items.OAK_PLANKS, 8), 8);
		this.eq("what is done for a quest that is not open does not count", 0L, this.count(a, "main/planks", 0));
		this.engine.leave(ADA);
		this.is("a player who got up has no seat", this.engine.seat(ADA) == null);
	}

	private void tasks() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.world.items(ADA).add(new ItemStack(Items.OAK_LOG, 3));
		this.engine.lookFor();
		this.eq("items held are counted", 3L, this.count(a, "main/logs", 0));
		this.eq("a quest with some of it done is in progress", Rules.Status.IN_PROGRESS, this.engine.status(a, "main/logs"));
		this.world.items(ADA).clear();
		this.engine.lookFor();
		this.eq("items that are gone again are not counted any more", 0L, this.count(a, "main/logs", 0));
		this.world.items(ADA).add(new ItemStack(Items.OAK_LOG, 3));
		this.world.items(ADA).add(new ItemStack(Items.BIRCH_LOG, 2));
		this.engine.lookFor();
		this.is("an item task is done once enough are held (a tag takes every item in it)", this.done(a, "main/logs"));
		this.world.items(ADA).clear();
		this.engine.lookFor();
		this.is("a done quest stays done when the items are gone", this.done(a, "main/logs") && this.count(a, "main/logs", 0) == 4L);
		this.eq("a done quest with rewards waiting is claimable", Rules.Status.CLAIMABLE, this.engine.status(a, "main/logs"));
		this.is("a completion is told to the player", this.heard(a, "quest", "main/logs"));
		this.is("a completion is told to whoever listens to the API", this.world.questsDone.contains("Ada:main/logs"));
		this.is("what waited for it is open", a.open.contains("main/planks") && a.open.contains("main/zombies"));
		this.engine.crafted(a, new ItemStack(Items.OAK_PLANKS, 4), 4);
		this.engine.crafted(a, new ItemStack(Items.STICK, 4), 4);
		this.eq("making things counts by how many were made", 4L, this.count(a, "main/planks", 0));
		this.engine.crafted(a, new ItemStack(Items.OAK_PLANKS, 4), 4);
		this.is("a craft task is done at its count", this.done(a, "main/planks"));

		for (int i = 0; i < 3; i++) {
			this.engine.killed(a, EntityType.ZOMBIE);
		}

		this.engine.killed(a, EntityType.STRAY);
		this.engine.killed(a, EntityType.COW);
		this.eq("a kill counts for the task about that mob", 3L, this.count(a, "main/zombies", 0));
		this.eq("a kill counts for a task about a tag the mob is in", 1L, this.count(a, "main/zombies", 1));
		this.is("a quest is not done while one of its tasks is not", !this.done(a, "main/zombies"));
		this.engine.killed(a, EntityType.SKELETON);
		this.is("a quest is done when all its tasks are", this.done(a, "main/zombies"));
		this.engine.broke(a, Blocks.DIRT);

		for (int i = 0; i < 5; i++) {
			this.engine.broke(a, i % 2 == 0 ? Blocks.STONE : Blocks.DEEPSLATE);
		}

		this.is("breaking blocks of a tag counts, other blocks do not", this.done(a, "main/stone"));
		this.engine.placed(a, Blocks.TORCH);
		this.engine.placed(a, Blocks.REDSTONE_TORCH);
		this.eq("placing counts the block placed", 1L, this.count(a, "main/torch", 0));
		this.engine.placed(a, Blocks.WALL_TORCH);
		this.is("a torch on a wall is a torch", this.done(a, "main/torch"));
		this.world.items(ADA).add(new ItemStack(Items.WHEAT, 5));
		this.engine.lookFor();
		this.eq("items that are to be handed in are not counted by being held", 0L, this.count(a, "main/wheat", 0));
		this.eq("handing in part of what a task asks is accepted", "ok", this.submit(a, "main/wheat"));
		this.is("what was handed in is taken and counted", this.count(a, "main/wheat", 0) == 5L && this.world.items(ADA).stream().allMatch(ItemStack::isEmpty));
		QuestEngine.Outcome missing = this.engine.submit(a, "main/wheat", 0);
		this.is("handing in with empty hands says how many are missing", missing.code().equals("missing_items") && missing.args().equals(List.of("7")));
		this.world.items(ADA).add(new ItemStack(Items.WHEAT, 20));
		this.submit(a, "main/wheat");
		this.is(
			"handing in the rest finishes the quest and takes no more than asked",
			this.done(a, "main/wheat") && this.world.items(ADA).stream().mapToInt(ItemStack::getCount).sum() == 13
		);
		this.eq("a task that notices by itself cannot be handed in", "not_manual", this.submit(a, "main/jump"));
		this.eq("nothing can be handed in to a quest that is not open", "not_open", this.submit(a, "side/s1"));
		this.eq("a task that is not there cannot be handed in", "invalid", this.engine.submit(a, "main/jump", 7).code());
		this.eq("a quest that is not there cannot be handed in to", "no_such_quest", this.submit(a, "main/nope"));
		this.world.xp(ADA)[0] = 3;
		this.eq("paying part of the levels a task asks is accepted", "ok", this.submit(a, "main/pay"));
		this.is("the levels paid are taken and counted", this.count(a, "main/pay", 0) == 3L && this.world.xp(ADA)[0] == 0);
		this.eq("paying without experience is refused", "missing_xp", this.submit(a, "main/pay"));
		this.world.xp(ADA)[0] = 10;
		this.submit(a, "main/pay");
		this.is("paying the rest finishes the quest and takes no more than asked", this.done(a, "main/pay") && this.world.xp(ADA)[0] == 8);
		this.engine.lookFor();
		this.eq("levels a player has are counted as they stand", 8L, this.count(a, "main/levels", 0));
		this.world.xp(ADA)[0] = 2;
		this.engine.lookFor();
		this.eq("levels that are spent are not counted any more", 2L, this.count(a, "main/levels", 0));
		this.world.xp(ADA)[0] = 12;
		this.engine.lookFor();
		this.is("a level task is done at its count", this.done(a, "main/levels"));
		Stat<?> jump = Stats.CUSTOM.get(Stats.JUMP);
		this.world.stats(ADA).put(jump, 20L);
		this.engine.lookFor();
		this.eq("a statistic is counted as it stands", 20L, this.count(a, "main/jump", 0));
		this.world.stats(ADA).put(jump, 60L);
		this.engine.lookFor();
		this.is("a statistic task is done once the statistic reaches its count", this.done(a, "main/jump"));
		this.is("an advancement the player does not have is not counted", !this.done(a, "main/adv"));
		this.world.advancements(ADA).add(ResourceLocation.parse("minecraft:story/root"));
		this.engine.lookFor();
		this.is("an advancement task is done once the player has the advancement", this.done(a, "main/adv"));
		this.world.places.put(ADA, new SelfTest.Scratch.Place("minecraft:overworld", "minecraft:plains", "", 0, 64, 0));
		this.engine.lookFor();
		this.is("being elsewhere does not finish a location task", !this.done(a, "main/nether"));
		this.world.places.put(ADA, new SelfTest.Scratch.Place("minecraft:the_nether", "minecraft:nether_wastes", "", 0, 64, 0));
		this.engine.lookFor();
		this.is("being in the dimension finishes a location task", this.done(a, "main/nether"));
		this.world.sights.put(ADA, new World.Sight(Blocks.DIAMOND_BLOCK, null));
		this.engine.observe(5);
		this.engine.observe(5);
		this.eq("looking at what a task asks counts the ticks", 10L, this.count(a, "main/look", 0));
		this.world.sights.put(ADA, new World.Sight(Blocks.DIRT, null));
		this.engine.observe(5);
		this.world.sights.put(ADA, new World.Sight(Blocks.DIAMOND_BLOCK, null));
		this.engine.observe(5);
		this.eq("a look that broke off starts again; the longest so far is kept", 10L, this.count(a, "main/look", 0));

		for (int i = 0; i < 3; i++) {
			this.engine.observe(5);
		}

		this.is("an observe task is done after an unbroken look of its length", this.done(a, "main/look"));
		this.engine.lookFor();
		this.eq("a player alone is a team of one", 1L, this.count(a, "main/crew", 0));
		this.eq("a custom task is moved by its name from outside", 1, this.engine.custom(a, "boss", 1L));
		this.eq("a custom task counts what it is given", 1L, this.count(a, "main/boss", 0));
		this.eq("a name no task goes by moves nothing", 0, this.engine.custom(a, "nobody", 1L));
		this.eq("a task is moved by its number too", "ok", this.engine.task(a, "main/boss", "1", 1L).code());
		this.is("a custom task is done at its count", this.done(a, "main/boss"));
		this.eq("a task that a quest does not have is refused", "no_such_task", this.engine.task(a, "main/jump", "nope", 1L).code());
		this.engine.killed(a, EntityType.PIG);
		this.is("with anyTask one finished task finishes the quest", this.done(a, "main/either"));
		this.world.items(ADA).clear();
		this.world.items(ADA).add(new ItemStack(Items.POTION));
		this.engine.lookFor();
		this.is("an item without the components a task names does not count", !this.done(a, "main/potion"));
		this.world.items(ADA).add(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
		this.engine.lookFor();
		this.is("an item with the components a task names counts", this.done(a, "main/potion"));
		this.world.items(ADA).add(this.book(Enchantments.SHARPNESS));
		this.engine.lookFor();
		this.is("an item that does not pass the game's predicate in a task does not count", !this.done(a, "main/mending"));
		this.world.items(ADA).add(this.book(Enchantments.MENDING));
		this.engine.lookFor();
		this.is("any item that passes the game's predicate in a task counts", this.done(a, "main/mending"));
		this.world.broadcasts.clear();
		this.engine.killed(a, EntityType.WITHER);
		this.is("an advancement-kind quest is the player's own", this.done(a, "main/wither") && a.own.done("main/wither"));
		this.is("an advancement-kind quest is announced as the game announces a challenge", this.world.broadcast("chat.type.advancement.challenge"));
		this.is("an advancement-kind quest hands its rewards out at once", this.world.gave(ADA, r -> r.is("xp") && r.xp == 500));
		this.eq("an advancement-kind quest has nothing left to claim", Rules.Status.COMPLETE, this.engine.status(a, "main/wither"));
	}

	private void chain() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.world.clear();
		this.eq("a checkmark is ticked off by the player", "ok", this.submit(a, "main/tick"));
		this.is("the quest is done", this.done(a, "main/tick"));
		this.is("a quest with autoClaim hands its rewards out at once", this.world.gave(ADA, r -> r.is("item") && r.item.equals("minecraft:stick")));
		this.eq("a quest with autoClaim has nothing left to claim", Rules.Status.COMPLETE, this.engine.status(a, "main/tick"));
		this.is("a task that waits for a quest is done when that quest is", this.done(a, "main/after"));
		this.is("a quest without tasks that waits for one of two is done when one is", this.done(a, "main/one_of"));
		this.is("a chapter opens when the quest it requires is done", a.open.contains("side/s1"));
		this.is("a chapter that opened is told", this.heard(a, "unlocked", "side"));
		this.is("a tree no quest leads into opens with its parent chapter", a.open.contains("loose/l1"));
		this.is("a quest of a chapter that just opened is still locked behind its own dependency", !a.open.contains("side/s2") && !this.done(a, "side/s2"));
		this.submit(a, "side/s1");
		this.is("a quest without tasks is done as soon as its dependency is", this.done(a, "side/s2"));
		this.is("a chapter is done when its quests are, and that is told", this.heard(a, "chapter", "side") && this.world.chaptersDone.contains("Ada:side"));
		this.is("a task that waits for a chapter is done when that chapter is", this.done(a, "main/chap"));
		this.is("a way in without tasks is not done while its tree is not", !this.done(a, "main/gate"));
		this.submit(a, "tree/t1");
		this.is("a quest of the tree opens as the one before it is done", a.open.contains("tree/t2"));
		this.submit(a, "tree/t2");
		this.is("a way in without tasks is done when its tree is (optional quests aside)", this.done(a, "main/gate") && !this.done(a, "tree/t3"));
		this.engine.flush();
		QuestState gate = this.world.lastState(a, "main/gate");
		this.is("progress reaches the client as it changes, without the whole of it being sent again", gate != null && gate.done && this.world.resets(a) == 0);
		List<CustomPacketPayload> sent = this.world.sent.getOrDefault(ADA, List.of());
		int told = -1;
		int progress = -1;

		for (int i = 0; i < sent.size(); i++) {
			if (sent.get(i) instanceof QuestsNet.Happened h && h.subject().equals("main/tick") && told < 0) {
				told = i;
			}

			if (sent.get(i) instanceof QuestsNet.Progress && progress < 0) {
				progress = i;
			}
		}

		this.is("a client hears of a finished quest after it was sent the progress", progress >= 0 && told > progress);
	}

	private void rewards() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.eq("the rewards of a quest that is not done cannot be claimed", "not_done", this.engine.claim(a, "main/logs", -1, -1).code());
		this.eq("the rewards of a quest that is not there cannot be claimed", "no_such_quest", this.engine.claim(a, "main/nope", -1, -1).code());
		this.engine.complete(a, "main/logs");
		this.is("a quest made done by an operator is done, its tasks full", this.done(a, "main/logs") && this.count(a, "main/logs", 0) == 4L);
		this.eq("one reward is claimed", "ok", this.engine.claim(a, "main/logs", 0, -1).code());
		this.is("the reward claimed is handed over", this.world.gave(ADA, r -> r.is("item") && r.item.equals("minecraft:apple") && r.count == 3));
		this.eq("a reward is not claimed twice", "already_claimed", this.engine.claim(a, "main/logs", 0, -1).code());
		this.eq("a reward that is not there cannot be claimed", "invalid", this.engine.claim(a, "main/logs", 5, -1).code());
		this.eq("the rest of a quest's rewards is claimed at once", "ok", this.engine.claim(a, "main/logs", -1, -1).code());
		this.eq("there is nothing left then", "nothing_to_claim", this.engine.claim(a, "main/logs", -1, -1).code());
		this.eq("a done quest with everything claimed is complete", Rules.Status.COMPLETE, this.engine.status(a, "main/logs"));
		this.world.clear();
		this.engine.flush();
		QuestState logs = this.world.lastState(a, "main/logs");
		this.is("the player is told which rewards they have claimed", logs != null && logs.claimed == 3L);
		this.engine.complete(a, "main/planks");
		this.eq("a choice reward claimed with the rest waits for its answer", "choose", this.engine.claim(a, "main/planks", -1, -1).code());
		this.eq("a choice reward claimed without a choice asks for one", "choose", this.engine.claim(a, "main/planks", 0, -1).code());
		this.eq("a choice reward claimed with a choice is handed over", "ok", this.engine.claim(a, "main/planks", 0, 1).code());
		this.is(
			"the one chosen is the one given",
			this.world.gave(ADA, r -> r.item.equals("minecraft:cooked_beef")) && !this.world.gave(ADA, r -> r.item.equals("minecraft:bread"))
		);
		this.engine.submit(a, "main/lucky", 0);
		this.engine.claim(a, "main/lucky", -1, -1);
		this.eq(
			"a random reward gives one of its choices",
			1L,
			this.world.given(ADA).stream().filter(r -> r.item.equals("minecraft:coal") || r.item.equals("minecraft:emerald")).count()
		);
		this.engine.complete(a, "main/zombies");
		this.engine.submit(a, "main/own", 0);
		int before = this.world.given(ADA).size();
		this.eq("everything that waits is claimed at once", "ok", this.engine.claimAll(a, false).code());
		this.eq("claiming everything hands every waiting reward over", before + 3, this.world.given(ADA).size());
		this.eq("claiming everything twice finds nothing", "nothing_to_claim", this.engine.claimAll(a, false).code());
		boolean slowed = false;

		for (int i = 0; i < 61; i++) {
			slowed = this.engine.tooFast(a);
		}

		this.is("a client that asks too much within a second is told to slow down", slowed);
	}

	private void claimAllPicking() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		Predicate<Reward> ofTheChoice = r -> r.item.equals("minecraft:bread") || r.item.equals("minecraft:cooked_beef");
		this.engine.complete(a, "main/logs");
		this.engine.complete(a, "main/planks");
		this.eq("claiming everything claims what needs no picking", "ok", this.engine.claimAll(a, false).code());
		this.is("and leaves a choice the player has not made", this.world.given(ADA).stream().noneMatch(ofTheChoice));
		this.eq("with only choices left, claiming everything asks for them", "choose", this.engine.claimAll(a, false).code());
		this.eq("claiming everything with the picking left to it claims the choice", "ok", this.engine.claimAll(a, true).code());
		this.eq("one of what there was to choose from is handed over, and only one", 1L, this.world.given(ADA).stream().filter(ofTheChoice).count());
		this.eq("nothing is left then", "nothing_to_claim", this.engine.claimAll(a, true).code());
		this.eq("the quest is complete", Rules.Status.COMPLETE, this.engine.status(a, "main/planks"));
	}

	private void repeatable() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.world.items(ADA).add(new ItemStack(Items.WHEAT, 40));
		this.submit(a, "main/wheat");
		QuestProgress p = this.engine.peek(a, "main/wheat");
		this.is("a repeatable quest that is done rests for its cooldown", p != null && p.done && p.times == 1 && p.readyAt == this.world.clock + 60000L);
		this.is("what waits for it is open", a.open.contains("main/bread"));
		this.engine.rested();
		this.is("it does not start over while it rests", p.done);
		this.world.clock += 61000L;
		this.engine.rested();
		this.is("it does not start over while its reward waits for a player who is here", p.done);
		this.eq("its reward is claimed", "ok", this.engine.claim(a, "main/wheat", -1, -1).code());
		this.is(
			"with the rest over and the reward claimed it starts over at once",
			!p.done && p.task(0) == 0L && p.times == 1 && p.readyAt == 0L && a.open.contains("main/wheat")
		);
		this.is("what it had opened stays open", a.open.contains("main/bread") && this.engine.status(a, "main/bread") == Rules.Status.AVAILABLE);
		this.submit(a, "main/wheat");
		this.is("it can be done again, and counts how often", p.done && p.times == 2);
		this.engine.claim(a, "main/wheat", -1, -1);
		this.is("done again and claimed, it rests again", p.done);
		this.world.clock += 61000L;
		this.engine.rested();
		this.is("and starts over when that rest is over", !p.done && p.times == 2);
		QuestState state = this.stateOf(a, "main/wheat");
		this.is("the player is told how often it was done", state != null && state.times == 2 && !state.done);
	}

	private ItemStack book(ResourceKey<Enchantment> enchantment) {
		net.minecraft.core.Holder<Enchantment> holder = this.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
		return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, 1));
	}

	@Nullable
	private QuestState stateOf(Seat s, String key) {
		this.world.clear();
		this.engine.sendProgress(s);
		return this.world.lastState(s, key);
	}

	private void resets() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.engine.complete(a, "main/stone");
		this.eq("one quest is forgotten", "ok", this.engine.reset(a, "main/stone").code());
		this.is(
			"a forgotten quest is open again with nothing done",
			!this.done(a, "main/stone") && a.open.contains("main/stone") && this.engine.peek(a, "main/stone") == null
		);
		this.submit(a, "main/tick");
		this.is("before the reset the chapter is open", a.open.contains("side/s1"));
		this.eq("a chapter is forgotten", "ok", this.engine.reset(a, "chapter:main").code());
		this.is("what the forgotten quests had opened is closed again", !this.done(a, "main/tick") && !a.open.contains("side/s1"));
		this.is("a quest without tasks is done again at once", this.done(a, "main/start"));
		this.submit(a, "tree/t1");
		this.eq("everything is forgotten", "ok", this.engine.reset(a, "all").code());
		this.is(
			"after forgetting everything only what is done by being open is done", !this.done(a, "tree/t1") && this.done(a, "main/start") && a.own.quests.size() == 1
		);
		this.eq("a quest that is not there cannot be forgotten", "no_such_quest", this.engine.reset(a, "main/nope").code());
		this.eq("a chapter that is not there cannot be forgotten", "no_such_chapter", this.engine.reset(a, "chapter:nope").code());
		this.eq("a quest that is not there cannot be made done", "no_such_quest", this.engine.complete(a, "main/nope").code());
		this.is("all progress is sent again after a reset", this.world.resets(a) >= 4);
	}

	private void teams() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.engine.complete(a, "main/logs");
		this.engine.claim(a, "main/logs", 0, -1);
		this.submit(a, "main/own");
		this.engine.counter(a, "kills", 3L, false);
		this.world.teams.names.put(CREW, "Crew");
		this.world.teams.teamOf.put(BO, CREW);
		Seat b = this.sit(BO, "Bo");
		this.is("a member's team quests are kept under the team, their own under themself", b.team.id.equals(CREW) && b.own.id.equals(BO));
		this.is(
			"a team begins with what its first member brings (here: nothing but what is done by being open)", this.done(b, "main/start") && !this.done(b, "main/logs")
		);
		this.world.teams.teamOf.put(ADA, CREW);
		this.engine.memberJoined(CREW, ADA);
		this.is("a player who joins a team is seated under it", a.team == b.team && a.own.id.equals(ADA));
		this.is("joining a team brings the player's progress along", this.done(b, "main/logs") && b.open.contains("main/planks"));
		this.is("a player's own quests stay their own", this.done(a, "main/own") && !this.done(b, "main/own"));
		this.eq("what was claimed alone is claimed in the team", "already_claimed", this.engine.claim(a, "main/logs", 0, -1).code());
		this.eq("a teammate claims for themself", "ok", this.engine.claim(b, "main/logs", 0, -1).code());
		this.eq("quests a joiner brings are counted for the team", 2L, this.world.teams.counts.getOrDefault("quests", 0L));
		this.eq("what a player had counted alone is the team's count from then on", 3L, b.team.counter("kills"));
		this.engine.counter(b, "kills", 2L, false);
		this.world.clear();
		this.engine.killed(b, EntityType.ZOMBIE);
		this.engine.killed(b, EntityType.ZOMBIE);
		this.engine.killed(a, EntityType.ZOMBIE);
		this.engine.killed(a, EntityType.SKELETON);
		this.is("what any member does counts for the team", this.count(a, "main/zombies", 0) == 3L && this.count(b, "main/zombies", 1) == 1L);
		this.engine.killed(b, EntityType.BOGGED);
		this.is("a team's quest is done for all of its members", this.done(a, "main/zombies") && this.done(b, "main/zombies"));
		QuestProgress zombies = this.engine.peek(a, "main/zombies");
		this.is("it says whose doing finished it", zombies != null && zombies.doneBy.equals("Bo"));
		this.is("every member is told", this.heard(a, "quest", "main/zombies") && this.heard(b, "quest", "main/zombies"));
		this.is(
			"the team gets the quest's experience, a count and a line in its log",
			this.world.teams.xp == 35L && this.world.teams.counts.getOrDefault("quests", 0L) == 3L && this.world.teams.logs.contains("quest:Bo:zombies")
		);
		this.is(
			"a team's quest is announced to the team in chat", this.world.told(a, "cfhgui_quests.announce.team") && this.world.told(b, "cfhgui_quests.announce.team")
		);
		this.engine.flush();
		this.is("every member is sent the progress", this.world.lastState(a, "main/zombies") != null && this.world.lastState(b, "main/zombies") != null);
		this.eq("a reward for the whole team is claimed by one", "ok", this.engine.claim(b, "main/zombies", 0, -1).code());
		this.eq("and is then claimed for all", "already_claimed", this.engine.claim(a, "main/zombies", 0, -1).code());
		this.eq("a reward for each member is still the other member's to claim", "ok", this.engine.claim(a, "main/zombies", 1, -1).code());
		this.world.xp(ADA)[0] = 4;
		this.world.xp(BO)[0] = 7;
		this.engine.lookFor();
		this.eq("of what members have, the best counts, not the sum", 7L, this.count(a, "main/levels", 0));
		this.is("the team's size is counted", this.done(a, "main/crew"));
		this.world.teams.teamOf.remove(ADA);
		this.engine.memberLeft(CREW, ADA);
		this.is("a player who leaves is seated under themself again", a.team == a.own);
		this.is("leaving a team keeps a copy of its progress", this.done(a, "main/zombies") && this.done(a, "main/crew") && this.done(b, "main/zombies"));
		this.eq("the copy carries the player's own claims", "already_claimed", this.engine.claim(a, "main/zombies", 1, -1).code());
		this.eq("and what the team had counted", 5L, a.own.counter("kills"));
		long before = this.world.teams.xp;
		this.engine.complete(b, "main/stone");
		this.is("what the team does afterwards is not the leaver's", this.done(b, "main/stone") && !this.done(a, "main/stone"));
		this.eq("a done team quest gives its team the server's default experience", before + 10L, this.world.teams.xp);
		this.engine.claim(b, "main/stone", -1, -1);
		this.eq("a team_xp reward gives its experience when it is claimed", before + 17L, this.world.teams.xp);
		this.is("a team_xp reward is nothing to hand over", !this.world.gave(BO, r -> r.is("team_xp")));
		this.world.config.keepOnLeave = false;
		this.world.teams.teamOf.remove(BO);
		this.engine.memberLeft(CREW, BO);
		this.is(
			"without keepOnLeave a leaver starts the team's quests over", !this.done(b, "main/zombies") && !this.done(b, "main/stone") && this.done(b, "main/start")
		);
		this.engine.disbanded(CREW);
		this.is("a disbanded team's progress is gone", this.engine.store().peek(CREW) == null);
		this.fresh();
		this.world.config.mergeOnJoin = false;
		Seat c = this.sit(ADA, "Ada");
		this.world.teams.names.put(CREW, "Crew");
		this.world.teams.teamOf.put(BO, CREW);
		Seat d = this.sit(BO, "Bo");
		this.engine.complete(d, "main/stone");
		this.engine.complete(c, "main/torch");
		this.world.teams.teamOf.put(ADA, CREW);
		this.engine.memberJoined(CREW, ADA);
		this.is(
			"without mergeOnJoin a joiner takes the team's progress as it is", this.done(c, "main/stone") && !this.done(c, "main/torch") && !this.done(d, "main/torch")
		);
		this.fresh();
		this.world.config.teamProgress = false;
		this.world.teams.names.put(CREW, "Crew");
		this.world.teams.teamOf.put(ADA, CREW);
		this.world.teams.teamOf.put(BO, CREW);
		Seat e = this.sit(ADA, "Ada");
		Seat f = this.sit(BO, "Bo");
		this.engine.complete(e, "main/stone");
		this.is("without teamProgress every member quests alone", this.done(e, "main/stone") && !this.done(f, "main/stone") && e.team == e.own);
	}

	private void renames() {
		this.fresh();
		Seat a = this.sit(ADA, "Ada");
		this.submit(a, "main/tick");
		QuestBook book = this.engine.book();
		Set<String> changed = BookEditor.renameQuest(book, "main/tick", "ticked");
		this.engine.renamed(k -> k.equals("main/tick") ? "main/ticked" : k);
		this.is("a renamed quest keeps its progress", this.done(a, "main/ticked") && this.engine.peek(a, "main/tick") == null);
		Quest oneOf = book.quest("main/one_of");
		Quest after = book.quest("main/after");
		this.is("a dependency follows the rename", oneOf != null && oneOf.deps.contains("ticked") && !oneOf.deps.contains("tick"));
		this.is("a task that waits for the quest follows the rename", after != null && after.tasks.get(0).quest.equals("ticked"));
		this.is("a chapter that requires the quest follows the rename", book.chapter("side").requires.equals(List.of("main/ticked")));
		this.is("the chapters that changed are named", changed.contains("main") && changed.contains("side") && !changed.contains("tree"));
		this.is("what the quest had opened is still open", a.open.contains("side/s1") && this.done(a, "main/after"));
		this.submit(a, "side/s1");
		changed = BookEditor.renameChapter(book, "side", "aside");
		this.engine.renamed(k -> k.startsWith("side/") ? "aside/" + k.substring("side/".length()) : k);
		this.is(
			"a renamed chapter is in the book under its new id only, in its old place",
			book.chapter("aside") != null && book.chapter("side") == null && new ArrayList<>(book.chapters.keySet()).indexOf("aside") == 2
		);
		this.is("its quests keep their progress", this.done(a, "aside/s1") && this.done(a, "aside/s2") && this.engine.peek(a, "side/s1") == null);
		this.is("a tree under it follows the rename", book.chapter("loose").parent.equals("aside") && a.open.contains("loose/l1"));
		Quest chap = book.quest("main/chap");
		this.is("a task that waits for the chapter follows the rename", chap != null && chap.tasks.get(0).chapter.equals("aside") && this.done(a, "main/chap"));
		this.is("the chapters that changed are named", changed.contains("aside") && changed.contains("loose") && changed.contains("main"));
		this.is("all progress is sent again after a rename", this.world.resets(a) >= 3);
	}

	private void mending() {
		Chapter c = (Chapter)BookJson.GSON
			.fromJson(
				"{\"parent\": \"Not An Id\", \"accent\": \"red\", \"quests\": [{\"id\": \"Bad Id!\"}, {\"id\": \"x\"}, {\"id\": \"x\"}, {\"id\": \"bad_id\"}, {\"subtree\": \"c\"}]}",
				Chapter.class
			);
		c.id = "c";
		Quest first = c.quests.get(0);
		first.x = Float.NaN;

		for (int i = 0; i < 70; i++) {
			first.rewards.add(new Reward());
		}

		for (int i = 0; i < 70; i++) {
			first.tasks.add(new Task());
		}

		BookEditor.clean(c);
		Set<String> ids = new HashSet<>();
		boolean valid = true;

		for (Quest q : c.quests) {
			valid &= Ids.valid(q.id) && ids.add(q.id);
		}

		this.is("every quest of a chapter that arrives has an id that can be a key, and its own", valid && ids.size() == 5);
		this.is("an id is mended to the nearest one that is free", c.quests.get(0).id.equals("bad_id_2") && c.quests.get(3).id.equals("bad_id"));
		this.is("a quest has no more rewards than can be claimed, and no more tasks than a book needs", first.rewards.size() == 64 && first.tasks.size() == 64);
		this.is("a number that is none is mended", first.x == 0.0F);
		this.is(
			"a parent that is no id, a colour that is none and a tree that is the chapter itself are dropped",
			c.parent.isEmpty() && c.accent.isEmpty() && c.quests.get(4).subtree.isEmpty()
		);
	}

	private void specs() {
		Frozen registries = this.server.registryAccess();
		Specs.ItemReader items = new Specs.ItemReader(registries);
		Specs.ItemSpec stone = items.read("minecraft:stone");
		this.is("an item id reads as that item", stone != null && stone.test(new ItemStack(Items.STONE)) && !stone.test(new ItemStack(Items.DIRT)));
		Specs.ItemSpec logs = items.read("#minecraft:logs");
		this.is("an item tag reads as every item in it", logs != null && logs.test(new ItemStack(Items.OAK_LOG)) && !logs.test(new ItemStack(Items.STONE)));
		Specs.ItemSpec swift = items.read("minecraft:potion[potion_contents={potion:\"minecraft:swiftness\"}]");
		this.is(
			"an item with components reads as that item carrying them",
			swift != null
				&& swift.test(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS))
				&& !swift.test(new ItemStack(Items.POTION))
				&& !swift.test(PotionContents.createItemStack(Items.POTION, Potions.HEALING))
		);
		Specs.ItemSpec mending = items.read("*[stored_enchantments~[{enchantments:\"minecraft:mending\"}]]");
		this.is(
			"any item with the game's predicate reads as every stack that passes it",
			mending != null
				&& mending.any()
				&& mending.test(this.book(Enchantments.MENDING))
				&& !mending.test(this.book(Enchantments.SHARPNESS))
				&& !mending.test(new ItemStack(Items.BOOK))
		);
		Specs.ItemSpec taggedWorn = items.read("#minecraft:logs[count~{min:3}]");
		this.is(
			"a tag with the game's predicate reads as the tag's items that pass it",
			taggedWorn != null
				&& taggedWorn.tag != null
				&& taggedWorn.test(new ItemStack(Items.OAK_LOG, 5))
				&& !taggedWorn.test(new ItemStack(Items.OAK_LOG, 2))
				&& !taggedWorn.test(new ItemStack(Items.STONE, 5))
		);
		Specs.ItemSpec unnamed = items.read("minecraft:diamond_sword[!custom_name]");
		ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Edge"));
		this.is("a component that must be absent reads as such", unnamed != null && unnamed.test(new ItemStack(Items.DIAMOND_SWORD)) && !unnamed.test(named));
		this.is(
			"what names nothing reads as nothing",
			items.read("nomod:nothing") == null
				&& items.read("") == null
				&& items.read("minecraft:air") == null
				&& items.read("minecraft:stone[nonsense") == null
				&& items.read("minecraft:stone[no_such_component=1]") == null
		);
		ItemStack made = Specs.stack("minecraft:potion[potion_contents={potion:\"minecraft:swiftness\"}]", registries, 2);
		this.is("a reward's item is made with its components and count", swift != null && swift.test(made) && made.getCount() == 2);
		this.is(
			"a reward cannot be made of a tag or of any item", Specs.stack("#minecraft:logs", registries, 1).isEmpty() && Specs.stack("*", registries, 1).isEmpty()
		);
		Specs.Named skeletons = Specs.Named.parse("#minecraft:skeletons");
		this.is(
			"an entity tag takes the mobs in it",
			skeletons != null
				&& skeletons.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityType.STRAY), Registries.ENTITY_TYPE)
				&& !skeletons.matches(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityType.ZOMBIE), Registries.ENTITY_TYPE)
		);
		this.eq("a statistic reads as type and id", Stats.CUSTOM.get(Stats.JUMP), Specs.stat("minecraft:custom/minecraft:jump"));
		this.eq("a statistic without a type is a custom one", Stats.CUSTOM.get(Stats.JUMP), Specs.stat("minecraft:jump"));
		this.eq("a statistic of another type reads too", Stats.BLOCK_MINED.get(Blocks.STONE), Specs.stat("minecraft:mined/minecraft:stone"));
		this.is("a statistic that is none reads as none", Specs.stat("minecraft:custom/minecraft:nope") == null && Specs.stat("nope/nope") == null);
		QuestBook book = new QuestBook();
		chapter(
			book, "c", "{\"quests\": [{\"id\": \"q\", \"tasks\": [{\"item\": \"nomod:thing\"}, {\"entity\": \"minecraft:zombie\"}, {\"stat\": \"no such stat\"}]}]}"
		);
		TriggerIndex index = new TriggerIndex(new BookIndex(book), registries);
		this.eq("tasks whose words cannot be read are named for the log", 2, index.unreadable.size());
		QuestBook scratch = book();
		this.eq("the server's words for a quest without a title are its first task's", "Any Logs", ServerText.title(scratch, scratch.quest("main/logs")).getString());
		this.eq("formatting codes are read and can be taken out", "Arms & Armor", ServerText.plain("&cArms && &lArmor"));
		QuestState[] few = new QuestState[]{new QuestState("a/b"), new QuestState("a/c")};
		few[0].done = true;
		few[0].tasks = new long[]{3L, 4L};
		QuestState[] back = Wire.unpack(Wire.pack(few), QuestState[].class);
		this.is(
			"small progress is packed without compressing and unpacks as the client unpacks",
			back != null && back.length == 2 && back[0].done && back[0].tasks[1] == 4L && back[1].key.equals("a/c")
		);
		QuestState[] many = new QuestState[400];

		for (int i = 0; i < many.length; i++) {
			many[i] = new QuestState("chapter/quest_" + i);
		}

		byte[] packed = Wire.pack(many);
		QuestState[] manyBack = Wire.unpack(packed, QuestState[].class);
		this.is(
			"large progress is compressed and unpacks the same",
			manyBack != null && manyBack.length == 400 && manyBack[399].key.equals("chapter/quest_399") && packed.length < 4000
		);
	}

	private void importer() {
		Object parsed = Snbt.parse(
			"{\n\ta: 1\n\tb: \"two\"\n\t\"c:d\": [1L, 2.5d, true]\n\tquest.AB.title: \"T \\\\& U\"\n\tnested: { x: 1b, y: [ ] }, e: 'it''s'\n}\n".replace("''", "\\'")
		);
		Map<String, Object> map = Snbt.map(parsed);
		this.eq("SNBT with line breaks for commas reads: a number", 1L, Snbt.number(map, "a", 0L));
		this.eq("SNBT reads: a string", "two", Snbt.string(map, "b", ""));
		List<Object> list = Snbt.list(map.get("c:d"));
		this.is(
			"SNBT reads: a quoted key and a list of mixed things",
			list.size() == 3
				&& list.get(0) instanceof Snbt.Num n
				&& n.raw().equals("1L")
				&& n.longValue() == 1L
				&& list.get(1) instanceof Snbt.Num d
				&& d.doubleValue() == 2.5
				&& Boolean.TRUE.equals(list.get(2))
		);
		this.eq("SNBT reads: a key with dots, an escaped backslash", "T \\& U", Snbt.string(map, "quest.AB.title", ""));
		this.is(
			"SNBT reads: a compound on one line with commas, a byte as a switch, an empty list",
			Snbt.flag(Snbt.map(map.get("nested")), "x", false) && Snbt.list(Snbt.map(map.get("nested")).get("y")).isEmpty()
		);
		this.eq("SNBT reads: single quotes", "it's", Snbt.string(map, "e", ""));
		this.eq("SNBT is written back as the game's item syntax wants it", "{x:1b,y:[]}", Snbt.write(map.get("nested")));
		boolean refused = false;

		try {
			Snbt.parse("{a: [1, 2");
		} catch (Snbt.SyntaxException var17) {
			refused = true;
		}

		this.is("SNBT that cannot be anything is refused", refused);
		Path tmp = null;

		try {
			tmp = Files.createTempDirectory("cfhgui-quests-selftest");
			put(tmp.resolve("data.snbt"), "{\n\tdefault_consume_items: false\n\tdefault_quest_shape: \"circle\"\n\tversion: 13\n}\n");
			put(tmp.resolve("chapter_groups.snbt"), "{\n\tchapter_groups: [\n\t\t{ id: \"00000000000000A1\" }\n\t]\n}\n");
			put(
				tmp.resolve("reward_tables/000000000000abcd.snbt"),
				"{\n\tid: \"000000000000ABCD\"\n\tloot_size: 1\n\trewards: [\n\t\t{ id: \"01\", item: { count: 1, id: \"minecraft:coal\" } }\n\t\t{\n\t\t\tcount: 2\n\t\t\tid: \"02\"\n\t\t\titem: {\n\t\t\t\tcount: 1\n\t\t\t\tid: \"minecraft:emerald\"\n\t\t\t}\n\t\t\tweight: 0.2f\n\t\t}\n\t]\n}\n"
			);
			put(
				tmp.resolve("lang/en_us.snbt"),
				"{\n\tchapter.00000000000000C1.title: \"&6First \\\\& Last\"\n\tchapter.00000000000000C1.chapter_subtitle: [\"Where it starts.\"]\n\tchapter_group.00000000000000A1.title: \"The Start\"\n\tfile.0000000000000001.title: \"Sample Pack\"\n\tquest.0000000000000001.title: \"Grey Rock\"\n\tquest.0000000000000001.quest_subtitle: \"A stone\"\n\tquest.0000000000000001.quest_desc: [\n\t\t\"This line is long enough to be one that an editor wrapped by hand, so it runs\"\n\t\t\"on into this one.\"\n\t\t\"\"\n\t\t\"A second paragraph.\"\n\t]\n\tquest.0000000000000002.title: \"Grey Rock\"\n\tquest.0000000000000003.title: \"Wet Work\"\n}\n"
			);
			put(
				tmp.resolve("chapters/first.snbt"),
				"{\n\tdefault_hide_dependency_lines: true\n\tfilename: \"first\"\n\tgroup: \"00000000000000A1\"\n\ticon: {\n\t\tid: \"minecraft:stone\"\n\t}\n\tid: \"00000000000000C1\"\n\timages: [ ]\n\torder_index: 3\n\tquest_links: [ ]\n\tquests: [\n\t\t{\n\t\t\tid: \"0000000000000001\"\n\t\t\trewards: [\n\t\t\t\t{\n\t\t\t\t\tid: \"11\"\n\t\t\t\t\ttype: \"xp\"\n\t\t\t\t\txp: 50\n\t\t\t\t}\n\t\t\t\t{\n\t\t\t\t\tid: \"12\"\n\t\t\t\t\ttable_id: 43981L\n\t\t\t\t\ttype: \"random\"\n\t\t\t\t}\n\t\t\t\t{ id: \"13\", type: \"toast\", description: \"Hi\" }\n\t\t\t\t{ id: \"14\", ignore: true, key: \"pack:flag\", type: \"extraquests:key_value\", value: 1L }\n\t\t\t\t{ id: \"15\", ignore: true, key: \"pack:count\", type: \"extraquests:key_value\", value: 1L }\n\t\t\t]\n\t\t\ttasks: [\n\t\t\t\t{\n\t\t\t\t\tcount: 32L\n\t\t\t\t\tid: \"21\"\n\t\t\t\t\titem: { count: 1, id: \"minecraft:andesite\" }\n\t\t\t\t\ttype: \"item\"\n\t\t\t\t}\n\t\t\t\t{\n\t\t\t\t\tentity: \"minecraft:zombie\"\n\t\t\t\t\tid: \"22\"\n\t\t\t\t\ttype: \"kill\"\n\t\t\t\t\tvalue: 10L\n\t\t\t\t}\n\t\t\t]\n\t\t\tx: 0.0d\n\t\t\ty: 1.5d\n\t\t}\n\t\t{\n\t\t\tdependencies: [\"0000000000000001\"]\n\t\t\tdependency_requirement: \"one_completed\"\n\t\t\thide_dependency_lines: false\n\t\t\tid: \"0000000000000002\"\n\t\t\toptional: true\n\t\t\tshape: \"hexagon\"\n\t\t\tsize: 1.5d\n\t\t\ttasks: [\n\t\t\t\t{\n\t\t\t\t\tid: \"23\"\n\t\t\t\t\tstat: \"minecraft:walk_one_cm\"\n\t\t\t\t\ttype: \"stat\"\n\t\t\t\t\tvalue: 2000\n\t\t\t\t}\n\t\t\t\t{ id: \"26\", key: \"pack:flag\", type: \"extraquests:key_value\", value: 1L }\n\t\t\t\t{ id: \"27\", key: \"pack:count\", type: \"extraquests:key_value\", value: 2L }\n\t\t\t]\n\t\t\tx: 2.5d\n\t\t\ty: 0.0d\n\t\t}\n\t\t{\n\t\t\tid: \"0000000000000003\"\n\t\t\trewards: [{ id: \"16\", ignore: true, key: \"pack:count\", type: \"extraquests:key_value\", value: 1L }]\n\t\t\ttasks: [\n\t\t\t\t{\n\t\t\t\t\tfluid: {\n\t\t\t\t\t\tamount: 1000\n\t\t\t\t\t\tid: \"minecraft:water\"\n\t\t\t\t\t}\n\t\t\t\t\tid: \"24\"\n\t\t\t\t\ttype: \"fluid\"\n\t\t\t\t}\n\t\t\t\t{ id: \"25\", key: \"pack:boss\", type: \"extraquests:key_value\", value: 1L }\n\t\t\t]\n\t\t\tx: 5.0d\n\t\t\ty: 0.0d\n\t\t}\n\t]\n}\n"
			);
			FtbImport.Result r = FtbImport.read(tmp);
			this.is("an FTB book is found", r != null);
			if (r != null) {
				QuestBook book = r.book();
				Chapter first = book.chapter("first");
				this.eq("the book's title comes from the lang file", "Sample Pack", book.title);
				this.is(
					"a group is made with its title", book.groups.size() == 1 && book.groups.get(0).id.equals("the_start") && book.groups.get(0).title.equals("The Start")
				);
				this.is(
					"a chapter keeps its file's name, its group, order and icon",
					first != null && first.group.equals("the_start") && first.order == 3 && first.icon.equals("minecraft:stone")
				);
				if (first != null) {
					this.is(
						"a title's leading colour becomes the chapter's own, FTB's escaped & becomes ours",
						first.title.equals("First && Last") && first.accent.equals("#FFAA00") && first.subtitle.equals("Where it starts.")
					);
					this.eq("every quest is converted", 3, first.quests.size());
					Quest rock = first.quest("grey_rock");
					Quest second = first.quest("grey_rock_2");
					Quest wet = first.quest("wet_work");
					this.is("quests get ids from their titles, each its own", rock != null && second != null && wet != null);
					if (rock != null && second != null && wet != null) {
						this.is("a quest keeps its place and its words", rock.y == 1.5F && rock.subtitle.equals("A stone") && rock.title.equals("Grey Rock"));
						this.is(
							"lines wrapped by hand are joined into paragraphs", rock.description.size() == 2 && rock.description.get(0).endsWith("so it runs on into this one.")
						);
						this.is(
							"an item task keeps its item and count",
							rock.tasks.size() == 2 && rock.tasks.get(0).is("item") && rock.tasks.get(0).item.equals("minecraft:andesite") && rock.tasks.get(0).count == 32L
						);
						this.is(
							"a kill task keeps its mob and count",
							rock.tasks.get(1).is("kill") && rock.tasks.get(1).entity.equals("minecraft:zombie") && rock.tasks.get(1).count == 10L
						);
						this.eq("every reward is converted", 5, rock.rewards.size());
						this.is("an experience reward is kept", rock.rewards.get(0).is("xp") && rock.rewards.get(0).xp == 50);
						Reward random = rock.rewards.get(1);
						this.is(
							"a reward table becomes a random reward, its fractions whole weights",
							random.is("random")
								&& random.choices.size() == 2
								&& random.choices.get(0).weight == 10
								&& random.choices.get(1).weight == 2
								&& random.choices.get(1).count == 2
						);
						this.is("a toast is a toast", rock.rewards.get(2).is("toast") && rock.rewards.get(2).text.equals("Hi"));
						this.is(
							"a reward of the key and value add-on adds to a counter",
							rock.rewards.get(3).is("counter") && rock.rewards.get(3).value.equals("pack:flag") && rock.rewards.get(3).count == 1
						);
						this.is("a dependency is pointed at the converted quest", second.deps.equals(List.of("grey_rock")) && second.depMode == Quest.DepMode.ANY);
						this.is(
							"optional, shape, size and shown lines carry over",
							second.optional && second.shape == Quest.Shape.HEXAGON && second.size == 1.5F && !second.hideLines && rock.hideLines
						);
						this.is(
							"a statistic task is a custom statistic here",
							second.tasks.get(0).is("stat") && second.tasks.get(0).stat.equals("minecraft:custom/minecraft:walk_one_cm") && second.tasks.get(0).count == 2000L
						);
						this.is(
							"a fluid task is a fluid handed in",
							wet.tasks.get(0).is("fluid") && wet.tasks.get(0).fluid.equals("minecraft:water") && wet.tasks.get(0).count == 1000L && wet.tasks.get(0).consume
						);
						this.is(
							"a task of the key and value add-on waits for a counter",
							wet.tasks.get(1).is("counter")
								&& wet.tasks.get(1).value.equals("pack:boss")
								&& second.tasks.size() == 3
								&& second.tasks.get(2).is("counter")
								&& second.tasks.get(2).value.equals("pack:count")
								&& second.tasks.get(2).count == 2L
						);
						this.is("the rewards that add to those counters are kept", wet.rewards.size() == 1 && wet.rewards.get(0).is("counter"));
					}
				}

				this.is(
					"nothing is dropped: what was read is what was written",
					r.counts().quests == 3
						&& r.counts().writtenQuests == 3
						&& r.counts().tasks == r.counts().writtenTasks
						&& r.counts().rewards == r.counts().writtenRewards
						&& r.counts().tasks == 7
						&& r.counts().rewards == 6
				);
				this.eq("everything in this book has an equal: nothing is approximated", 0, r.unconverted());
				this.is("the report says what was read and written", r.report().stream().anyMatch(l -> l.startsWith("DROPPED   0 quests, 0 tasks, 0 rewards")));
				this.is(
					"which FTB id became what is remembered",
					"first/grey_rock".equals(r.ids().quests.get("0000000000000001"))
						&& "first/grey_rock#1".equals(r.ids().tasks.get("22"))
						&& "first/grey_rock#0".equals(r.ids().rewards.get("11"))
				);
				Path out = tmp.resolve("out");
				FtbImport.write(out, r);
				QuestBook back = BookJson.load(out);
				String written = Files.readString(out.resolve("chapters").resolve("first.json"), StandardCharsets.UTF_8);
				this.is(
					"the converted book is written as a CFHGui book and reads back the same",
					back.chapters.size() == 1
						&& back.questCount() == 3
						&& back.title.equals("Sample Pack")
						&& back.source.equals("ftbquests")
						&& BookJson.GSON.toJson(back.chapter("first")).equals(BookJson.GSON.toJson(first))
				);
				this.is(
					"its files leave out what has its default",
					!written.contains("\"cooldownSeconds\"") && !written.contains("\"sequential\"") && written.contains("\"grey_rock\"")
				);
				this.is(
					"the report and the ids are written beside it", Files.isRegularFile(out.resolve("ftb-report.txt")) && Files.isRegularFile(out.resolve("ftb-ids.json"))
				);
				put(tmp.resolve("lang/en_us.snbt"), Files.readString(tmp.resolve("lang/en_us.snbt"), StandardCharsets.UTF_8).replace("\"Wet Work\"", "\"Dry Work\""));
				FtbImport.Result again = FtbImport.read(tmp, FtbImport.ids(out));
				this.is(
					"converted again, a quest keeps its id whatever its title has become",
					again != null
						&& again.book().quest("first/wet_work") != null
						&& again.book().quest("first/wet_work").title.equals("Dry Work")
						&& !again.hash().equals(r.hash())
				);
			}

			this.is("a folder without a book is no book", FtbImport.read(tmp.resolve("lang")) == null);
		} catch (Exception var18) {
			this.fail("the FTB importer: stopped by " + var18);
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] selftest: the importer threw", var18);
		} finally {
			if (tmp != null) {
				delete(tmp);
			}
		}
	}

	private static QuestBook more() {
		QuestBook book = new QuestBook();
		book.title = "More";
		chapter(
			book,
			"more",
			"{\"title\": \"More\", \"quests\": [\n  {\"id\": \"either_item\", \"tasks\": [{\"item\": \"minecraft:coal | minecraft:charcoal\", \"count\": 4}]},\n  {\"id\": \"modded\", \"tasks\": [{\"item\": \"@minecraft\", \"count\": 2}]},\n  {\"id\": \"filtered\", \"tasks\": [{\"item\": \"ftbfilter:or(item(minecraft:stone))\"}]},\n  {\"id\": \"named\", \"tasks\": [{\"entity\": \"minecraft:zombie\", \"name\": \"Bob\", \"count\": 2}]},\n  {\"id\": \"crit\", \"tasks\": [{\"advancement\": \"minecraft:story/root\", \"criterion\": \"crafting_table\"}]},\n  {\"id\": \"box\", \"tasks\": [{\"type\": \"location\", \"x\": 10, \"y\": 60, \"z\": 10, \"sizeX\": 5, \"sizeY\": 5, \"sizeZ\": 5}]},\n  {\"id\": \"water\", \"tasks\": [{\"type\": \"fluid\", \"fluid\": \"minecraft:water\", \"count\": 3000, \"consume\": true}]},\n  {\"id\": \"lava\", \"tasks\": [{\"type\": \"fluid\", \"fluid\": \"minecraft:lava\", \"count\": 2000}]},\n  {\"id\": \"power\", \"tasks\": [{\"type\": \"energy\", \"count\": 1000}]},\n  {\"id\": \"staged\", \"tasks\": [{\"type\": \"stage\", \"value\": \"iron_age\"}]},\n  {\"id\": \"counted\", \"tasks\": [{\"type\": \"counter\", \"value\": \"pack:kills\", \"count\": 3}]},\n  {\"id\": \"adds\", \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [{\"type\": \"counter\", \"value\": \"pack:kills\", \"count\": 2}]},\n  {\"id\": \"order\", \"sequential\": true, \"tasks\": [{\"type\": \"checkmark\"}, {\"entity\": \"minecraft:pig\", \"count\": 2}, {\"type\": \"checkmark\"}]},\n  {\"id\": \"extra\", \"tasks\": [{\"type\": \"checkmark\"}, {\"type\": \"checkmark\", \"optional\": true}]},\n  {\"id\": \"glance\", \"tasks\": [{\"type\": \"observe\", \"block\": \"minecraft:gold_block\", \"seconds\": 0}]},\n  {\"id\": \"fork\", \"maxDependents\": 1, \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"left\", \"deps\": [\"fork\"], \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"right\", \"deps\": [\"fork\"], \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"long\", \"tasks\": [{\"entity\": \"minecraft:cow\", \"count\": 5}]},\n  {\"id\": \"early\", \"deps\": [\"long\"], \"depsStarted\": true, \"tasks\": [{\"type\": \"checkmark\"}]},\n  {\"id\": \"gifts\", \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [\n    {\"type\": \"all\", \"choices\": [{\"item\": \"minecraft:apple\"}, {\"xp\": 3}]},\n    {\"type\": \"random\", \"rolls\": 3, \"choices\": [{\"item\": \"minecraft:coal\"}]},\n    {\"type\": \"toast\", \"title\": \"Well done\", \"text\": \"A notice\"},\n    {\"type\": \"stage\", \"value\": \"iron_age\"},\n    {\"type\": \"currency\", \"count\": 5},\n    {\"type\": \"custom\", \"value\": \"pack:blessing\"},\n    {\"item\": \"minecraft:stick\", \"count\": 2, \"bonus\": 3},\n    {\"type\": \"command\", \"command\": \"say hi\", \"level\": 4, \"text\": \"Said\"},\n    {\"type\": \"advancement\", \"advancement\": \"minecraft:story/root\", \"criterion\": \"crafting_table\"}]},\n  {\"id\": \"handed\", \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [{\"xp\": 7, \"auto\": true}, {\"item\": \"minecraft:bread\", \"hidden\": true},\n    {\"item\": \"minecraft:cake\"}, {\"item\": \"minecraft:cookie\", \"excluded\": true}]},\n  {\"id\": \"empty_draw\", \"tasks\": [{\"type\": \"checkmark\"}], \"rewards\": [{\"type\": \"random\", \"empty\": 1000000, \"choices\": [{\"item\": \"minecraft:flint\"}]}]},\n  {\"id\": \"ghost\", \"visibility\": \"invisible\", \"revealTasks\": 1, \"tasks\": [{\"type\": \"checkmark\"}, {\"type\": \"checkmark\"}]},\n  {\"id\": \"shy\", \"deps\": [\"ghost\"], \"visibility\": \"follow\", \"tasks\": [{\"type\": \"checkmark\"}]}\n]}"
		);
		Reward bread = new Reward();
		bread.type = "item";
		bread.item = "minecraft:bread";
		bread.count = 3;
		book.emergency.add(bread);
		book.emergencyCooldownSeconds = 60;
		return book.sanitize();
	}

	private void kinds() {
		this.world = new SelfTest.Scratch();
		this.engine = new QuestEngine(this.world, new HolderStore(null), this.server.registryAccess(), 42L);
		this.engine.book(more());
		Seat a = this.sit(ADA, "Ada");
		this.world.items(ADA).add(new ItemStack(Items.COAL, 1));
		this.world.items(ADA).add(new ItemStack(Items.CHARCOAL, 2));
		this.engine.lookFor();
		this.eq("an item task that takes any of several items counts them together", 3L, this.count(a, "more/either_item", 0));
		this.is("an item task that takes any item of a mod is done by any of them", this.done(a, "more/modded"));
		this.is(
			"a filter of FTB Filter System is said to need that mod where it is missing",
			Mods.has("ftbfiltersystem")
				|| this.engine.triggers().unreadable.stream().anyMatch(u -> u.contains("more/filtered") && u.contains("needs FTB Filter System"))
		);
		this.eq("several items are read apart at their bars, a bar inside brackets stays", List.of("a", "#b[c=1|d=2]", "@e"), Specs.split("a | #b[c=1|d=2] | @e"));
		this.engine.killed(a, EntityType.ZOMBIE, "Zed");
		this.eq("a kill task that asks for a name leaves the others", 0L, this.count(a, "more/named", 0));
		this.engine.killed(a, EntityType.ZOMBIE, "bob");
		this.eq("and counts those called so, whatever the case", 1L, this.count(a, "more/named", 0));
		this.world.advancements(ADA).add(ResourceLocation.parse("minecraft:story/root"));
		this.engine.lookFor();
		this.is("an advancement task that names a criterion is not done by the advancement alone", !this.done(a, "more/crit"));
		this.world.advancements(ADA).add(ResourceLocation.parse("minecraft:story/root/crafting_table"));
		this.engine.lookFor();
		this.is("and is done by the criterion", this.done(a, "more/crit"));
		this.world.places.put(ADA, new SelfTest.Scratch.Place("minecraft:overworld", "", "", 15, 62, 12));
		this.engine.lookFor();
		this.is("a place that is a box: one block outside is outside", !this.done(a, "more/box"));
		this.world.places.put(ADA, new SelfTest.Scratch.Place("minecraft:overworld", "", "", 14, 62, 12));
		this.engine.lookFor();
		this.is("a place that is a box: inside counts", this.done(a, "more/box"));
		this.eq("a fluid cannot be handed in by someone who carries none", "missing_fluid", this.submit(a, "more/water"));
		this.world.fluids.computeIfAbsent(ADA, k -> new HashMap<>()).put("minecraft:water", 2000L);
		this.eq("a fluid is handed in in parts", "ok", this.submit(a, "more/water"));
		this.eq("what was handed in counts", 2000L, this.count(a, "more/water", 0));
		this.world.fluids.get(ADA).put("minecraft:water", 5000L);
		this.submit(a, "more/water");
		this.is("only what the task still asks is taken", this.done(a, "more/water") && this.world.fluids.get(ADA).get("minecraft:water") == 4000L);
		this.world.fluids.get(ADA).put("minecraft:lava", 1000L);
		this.engine.lookFor();
		this.eq("a fluid that is only looked for counts what is carried", 1000L, this.count(a, "more/lava", 0));
		this.world.fluids.get(ADA).put("minecraft:lava", 0L);
		this.engine.lookFor();
		this.eq("and counts back down when it is gone", 0L, this.count(a, "more/lava", 0));
		this.world.fluids.get(ADA).put("minecraft:lava", 2000L);
		this.engine.lookFor();
		this.is("until there is enough of it", this.done(a, "more/lava"));
		this.eq("energy cannot be handed in where nothing can hold any", "no_energy_here", this.submit(a, "more/power"));
		this.world.energy.put(ADA, 0L);
		this.eq("nor by someone who carries none", "missing_energy", this.submit(a, "more/power"));
		this.world.energy.put(ADA, 600L);
		this.eq("energy is handed in in parts", "ok", this.submit(a, "more/power"));
		this.world.energy.put(ADA, 600L);
		this.submit(a, "more/power");
		this.is("and only as much as the task asks is taken", this.done(a, "more/power") && this.world.energy.get(ADA) == 200L);
		this.engine.lookFor();
		this.is("a stage task waits for the stage", !this.done(a, "more/staged"));
		this.world.stages.computeIfAbsent(ADA, k -> new HashSet<>()).add("iron_age");
		this.engine.lookFor();
		this.is("and is done by it", this.done(a, "more/staged"));
		this.submit(a, "more/adds");
		this.engine.claim(a, "more/adds", -1, -1);
		this.engine.lookFor();
		this.eq("a counter reward adds to the counter a counter task waits for", 2L, this.count(a, "more/counted", 0));
		this.eq("a counter is added to from outside", 3L, this.engine.counter(a, "pack:kills", 1L, false));
		this.engine.lookFor();
		this.is("a counter task is done when its counter is there", this.done(a, "more/counted"));
		this.engine.killed(a, EntityType.PIG, null);
		this.eq("with tasks in order, a later one does not count before the first is done", 0L, this.count(a, "more/order", 1));
		this.eq("nor can it be handed in", "task_locked", this.engine.submit(a, "more/order", 2).code());
		this.submit(a, "more/order");
		this.engine.killed(a, EntityType.PIG, null);
		this.engine.killed(a, EntityType.PIG, null);
		this.eq("then each in its turn", "ok", this.engine.submit(a, "more/order", 2).code());
		this.is("and the quest is done when all of them are", this.done(a, "more/order"));
		this.submit(a, "more/extra");
		this.is("a task that may be left out is not waited for", this.done(a, "more/extra") && this.count(a, "more/extra", 1) == 0L);
		this.world.sights.put(ADA, new World.Sight(Blocks.GOLD_BLOCK, null));
		this.engine.observe(5);
		this.is("a look that asks for no time is done by a glance", this.done(a, "more/glance"));
		this.submit(a, "more/fork");
		this.is("a fork opens both ways", a.open.contains("more/left") && a.open.contains("more/right"));
		this.submit(a, "more/left");
		this.is(
			"and shuts the other once one is taken",
			this.done(a, "more/left") && !a.open.contains("more/right") && this.engine.status(a, "more/right") == Rules.Status.LOCKED
		);
		this.eq("what is shut cannot be done", "not_open", this.submit(a, "more/right"));
		this.is("a quest that waits for another to be begun is closed while nothing of that is done", !a.open.contains("more/early"));
		this.engine.killed(a, EntityType.COW, null);
		this.is("and opens with the first of it", a.open.contains("more/early") && !this.done(a, "more/long"));
		this.submit(a, "more/gifts");
		this.eq("rewards of every new kind are claimed", "ok", this.engine.claim(a, "more/gifts", -1, -1).code());
		this.is(
			"a reward that gives all it holds gives each",
			this.world.gave(ADA, r -> r.item.equals("minecraft:apple")) && this.world.gave(ADA, r -> r.is("xp") && r.xp == 3)
		);
		this.eq("a random reward draws as often as it says", 3L, this.world.given(ADA).stream().filter(r -> r.item.equals("minecraft:coal")).count());
		this.is(
			"a toast, a stage, money and a reward for another mod reach the world",
			this.world.gave(ADA, r -> r.is("toast") && r.text.equals("A notice"))
				&& this.world.gave(ADA, r -> r.is("stage") && r.value.equals("iron_age"))
				&& this.world.gave(ADA, r -> r.is("currency") && r.count == 5)
				&& this.world.gave(ADA, r -> r.is("custom") && r.value.equals("pack:blessing"))
		);
		this.is(
			"an item reward with a bonus gives its count and up to that many more",
			this.world.gave(ADA, r -> r.item.equals("minecraft:stick") && r.count >= 2 && r.count <= 5)
		);
		this.is(
			"a command keeps its level and its line, an advancement its criterion",
			this.world.gave(ADA, r -> r.is("command") && r.level == 4 && r.text.equals("Said"))
				&& this.world.gave(ADA, r -> r.is("advancement") && r.criterion.equals("crafting_table"))
		);
		this.submit(a, "more/handed");
		this.is(
			"rewards that hand themselves over do so the moment the quest is done",
			this.world.gave(ADA, r -> r.is("xp") && r.xp == 7)
				&& this.world.gave(ADA, r -> r.item.equals("minecraft:bread"))
				&& !this.world.gave(ADA, r -> r.item.equals("minecraft:cake"))
		);
		this.eq("the others wait to be claimed", Rules.Status.CLAIMABLE, this.engine.status(a, "more/handed"));
		this.engine.claimAll(a, false);
		this.is(
			"claiming everything leaves what the book keeps out of it",
			this.world.gave(ADA, r -> r.item.equals("minecraft:cake")) && !this.world.gave(ADA, r -> r.item.equals("minecraft:cookie"))
		);
		this.eq("which is claimed on its own quest", "ok", this.engine.claim(a, "more/handed", 3, -1).code());
		this.is("and then given", this.world.gave(ADA, r -> r.item.equals("minecraft:cookie")));
		this.submit(a, "more/empty_draw");
		this.engine.claim(a, "more/empty_draw", -1, -1);
		this.is("a random reward may draw nothing where its table has a weight for that", !this.world.gave(ADA, r -> r.item.equals("minecraft:flint")));
		this.eq("a quest that keeps out of sight is not shown though it is open", Rules.Status.HIDDEN, this.engine.status(a, "more/ghost"));
		this.eq("nor is one that follows it", Rules.Status.HIDDEN, this.engine.status(a, "more/shy"));
		this.submit(a, "more/ghost");
		this.eq("it shows once as many of its tasks are done as it says", Rules.Status.IN_PROGRESS, this.engine.status(a, "more/ghost"));
		this.eq("and what follows it shows locked", Rules.Status.LOCKED, this.engine.status(a, "more/shy"));
		this.eq("the emergency items are handed out", "ok", this.engine.emergency(a).code());
		this.is("each of them", this.world.given(ADA).stream().filter(r -> r.item.equals("minecraft:bread") && r.count == 3).count() == 1L);
		this.eq("and rest before they can be had again", "emergency_wait", this.engine.emergency(a).code());
		this.world.clock += 61000L;
		this.eq("until the rest is over", "ok", this.engine.emergency(a).code());
		Seat b = this.sit(BO, "Bo");
		int took = this.engine
			.absorb(
				BO,
				List.of(
					new Foreign.Quest("more/fork", new long[]{1L}, true, 5L, 6L, 1, Map.of(BO.toString(), 0L), 0L),
					new Foreign.Quest("more/long", new long[]{3L}, false, 7L, 0L, 0, Map.of(), 0L),
					new Foreign.Quest("more/nope", new long[0], true, 1L, 2L, 1, Map.of(), 0L)
				)
			);
		this.eq("progress kept by another mod is taken in for the quests the book has", 2, took);
		this.is(
			"what was done there is done here, and what it opens is open", this.done(b, "more/fork") && b.open.contains("more/left") && b.open.contains("more/right")
		);
		this.is("what was begun there is begun here", this.count(b, "more/long", 0) == 3L && b.open.contains("more/early"));
		QuestState fork = this.world.lastState(b, "more/fork");
		this.is("and the player is told how they stand", fork != null && fork.done);
		this.engine.absorb(BO, List.of(new Foreign.Quest("more/handed", new long[]{1L}, true, 5L, 6L, 1, Map.of(BO.toString(), 15L), 0L)));
		this.engine.rebind(b);
		QuestProgress handed = this.engine.peek(b, "more/handed");
		this.is(
			"rewards claimed in the other mod are not claimed or handed out again",
			handed != null && handed.done && this.engine.unclaimed(b, this.engine.index().get("more/handed"), handed) == 0L && this.world.given(BO).isEmpty()
		);
	}

	private static void put(Path file, String text) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, text, StandardCharsets.UTF_8);
	}

	private static void delete(Path dir) {
		try (Stream<Path> s = Files.walk(dir)) {
			s.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException var2) {
				}
			});
		} catch (IOException var6) {
		}
	}

	private void hooks() {
		this.is("kills are hooked (ServerPlayer.awardKillScore)", hooked(ServerPlayer.class, "slateQuests$kill"));
		this.is("making things is hooked (ItemStack.onCraftedBy)", hooked(ItemStack.class, "slateQuests$crafted"));
		this.is("breaking is hooked (ServerPlayerGameMode.destroyBlock)", hooked(ServerPlayerGameMode.class, "slateQuests$afterBreak"));
		this.is("placing is hooked (BlockItem.place)", hooked(BlockItem.class, "slateQuests$placed"));
	}

	private static boolean hooked(Class<?> type, String handler) {
		for (Method m : type.getDeclaredMethods()) {
			if (m.getName().contains(handler)) {
				return true;
			}
		}

		return false;
	}

	private void live(ServerPlayer player) {
		LiveWorld live = new LiveWorld(this.server, TeamLink.NONE);
		this.fresh();
		Seat seat = new Seat(player.getUUID(), player.getGameProfile().getName(), player, this.engine);
		int[] carried = new int[]{0};
		live.inventory(seat, stack -> carried[0] += stack.getCount());
		int expected = 0;

		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			expected += player.getInventory().getItem(i).getCount();
		}

		this.eq("the real inventory is read whole", expected, carried[0]);
		this.eq("the real level is read", player.experienceLevel, live.experience(seat, false));
		this.is(
			"the real experience in points is at least what the levels cost",
			live.experience(seat, true) >= 0 && (player.experienceLevel == 0 || live.experience(seat, true) > 0)
		);
		Stat<?> jump = Stats.CUSTOM.get(Stats.JUMP);
		this.eq("a real statistic is read", (long)player.getStats().getValue(jump), live.statistic(seat, jump));
		this.is("an advancement that does not exist is not had", !live.advancement(seat, ResourceLocation.parse("cfhgui_quests:no/such"), ""));
		String here = player.serverLevel().dimension().location().toString();
		String biome = player.serverLevel().getBiome(player.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("");
		QuestBook book = new QuestBook();
		chapter(
			book,
			"c",
			"{\"quests\": [{\"id\": \"q\", \"tasks\": [{\"type\": \"location\", \"dimension\": \""
				+ here
				+ "\"},{\"type\": \"location\", \"dimension\": \"cfhgui_quests:nowhere\"},{\"type\": \"location\", \"biome\": \""
				+ biome
				+ "\"},{\"type\": \"location\", \"biome\": \"cfhgui_quests:nowhere\"},{\"type\": \"location\", \"x\": "
				+ player.getBlockX()
				+ ", \"y\": "
				+ player.getBlockY()
				+ ", \"z\": "
				+ player.getBlockZ()
				+ ", \"radius\": 4},{\"type\": \"location\", \"x\": "
				+ (player.getBlockX() + 500)
				+ ", \"y\": "
				+ player.getBlockY()
				+ ", \"z\": "
				+ player.getBlockZ()
				+ ", \"radius\": 4},{\"type\": \"location\", \"structure\": \"cfhgui_quests:nowhere\"},{\"type\": \"location\", \"biome\": \"#minecraft:is_overworld\"},{\"type\": \"location\", \"biome\": \"#minecraft:is_nether\"},{\"type\": \"location\", \"structure\": \"#minecraft:village\"}]}]}"
		);
		TriggerIndex index = new TriggerIndex(new BookIndex(book), this.server.registryAccess());
		this.is("the real place is read: the dimension the player is in", live.at(seat, index.ref("c/q", 0)) && !live.at(seat, index.ref("c/q", 1)));
		if (!biome.isEmpty()) {
			this.is("the real place is read: the biome the player is in", live.at(seat, index.ref("c/q", 2)) && !live.at(seat, index.ref("c/q", 3)));
		}

		this.is("the real place is read: near a point", live.at(seat, index.ref("c/q", 4)) && !live.at(seat, index.ref("c/q", 5)));
		this.is("the real place is read: not in a structure that does not exist", !live.at(seat, index.ref("c/q", 6)));
		if (here.equals("minecraft:overworld")) {
			this.is("the real place is read: a biome tag", live.at(seat, index.ref("c/q", 7)) && !live.at(seat, index.ref("c/q", 8)));
		}

		live.at(seat, index.ref("c/q", 9));
		this.is("a structure tag is asked without harm", true);
		live.sight(seat);
		this.is("the real line of sight is read without harm", true);
		QuestBook boxes = new QuestBook();
		chapter(
			boxes,
			"c",
			"{\"quests\": [{\"id\": \"q\", \"tasks\": [{\"type\": \"location\", \"x\": "
				+ (player.getBlockX() - 2)
				+ ", \"y\": "
				+ (player.getBlockY() - 2)
				+ ", \"z\": "
				+ (player.getBlockZ() - 2)
				+ ", \"sizeX\": 5, \"sizeY\": 5, \"sizeZ\": 5},{\"type\": \"location\", \"x\": "
				+ (player.getBlockX() + 3)
				+ ", \"y\": "
				+ (player.getBlockY() - 2)
				+ ", \"z\": "
				+ (player.getBlockZ() - 2)
				+ ", \"sizeX\": 5, \"sizeY\": 5, \"sizeZ\": 5}]}]}"
		);
		TriggerIndex boxIndex = new TriggerIndex(new BookIndex(boxes), this.server.registryAccess());
		this.is("the real place is read: inside a box, and beside it", live.at(seat, boxIndex.ref("c/q", 0)) && !live.at(seat, boxIndex.ref("c/q", 1)));
		long buckets = 0L;

		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(Items.WATER_BUCKET)) {
				buckets += (long)player.getInventory().getItem(i).getCount();
			}
		}

		long water = live.fluid(seat, new Specs.Named(ResourceLocation.parse("minecraft:water"), false));
		this.is(
			"the fluids a real player carries are read: every water bucket is a thousand millibuckets (" + water + " mB in " + buckets + " buckets)",
			water >= buckets * 1000L
		);
		this.eq("a fluid tag is read as well", water, live.fluid(seat, new Specs.Named(ResourceLocation.parse("minecraft:water"), true)));
		long energy = live.takeEnergy(seat, 0L);
		this.is("energy is asked for without harm (" + (energy < 0L ? "nothing here can hold any" : "a loader that knows batteries") + ")", energy <= 0L);
		this.is("a stage nobody has is not had", !live.stage(seat, "cfhgui_quests:selftest_no_such_stage"));
		this.is(
			"a filter of FTB Filter System is only read where that mod is",
			Mods.has("ftbfiltersystem") == (Mods.filter("item(minecraft:stone)", this.server.registryAccess()) != null)
		);
		Specs.ItemSpec either = new Specs.ItemReader(this.server.registryAccess()).read("minecraft:coal | #minecraft:planks | cfhgui_quests:no_such_item");
		this.is(
			"several items are read against real stacks, one that does not exist among them",
			either != null
				&& either.test(new ItemStack(Items.COAL))
				&& either.test(new ItemStack(Items.OAK_PLANKS))
				&& !either.test(new ItemStack(Items.STONE))
				&& either.names() != null
				&& either.names().size() == 2
		);
		this.is("the runner has a seat at the server's own book", QuestsServer.seat(player) != null);
	}

	private void real(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos base = player.blockPosition().above(4);
		BlockPos top = base.above();
		if (level.isLoaded(base) && level.getBlockState(base).isAir() && level.getBlockState(top).isAir()) {
			List<String> reported = new ArrayList<>();
			QuestEvents.tap = reported::add;

			try {
				level.setBlock(base, Blocks.STONE.defaultBlockState(), 3);
				ItemStack torch = new ItemStack(Items.TORCH);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(base).add(0.0, 0.5, 0.0), Direction.UP, base, false);
				InteractionResult used = torch.useOn(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, torch, hit));
				this.is("a block placed the way an item places it is there (" + used + ")", level.getBlockState(top).is(Blocks.TORCH));
				this.is("and is reported to the book", reported.contains("placed minecraft:torch"));
				level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
				boolean broken = player.gameMode.destroyBlock(base);
				this.is("a block broken the way a player breaks it is gone", broken && level.getBlockState(base).isAir());
				this.is("and is reported to the book", reported.contains("broke minecraft:stone"));
				new ItemStack(Items.STICK, 4).onCraftedBy(level, player, 4);
				this.is("something made is reported to the book with how many", reported.contains("crafted 4 minecraft:stick"));
				CFHGuiQuests.LOGGER.info("[CFHGui Quests] selftest: the hooks reported {}", reported);
			} finally {
				QuestEvents.tap = null;
				level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
				level.setBlock(base, Blocks.AIR.defaultBlockState(), 3);
			}
		} else {
			CFHGuiQuests.LOGGER.info("[CFHGui Quests] selftest: no room above the player to try the hooks for real; skipped");
		}
	}

	private void scale() {
		int chapters = 30;
		int perChapter = 100;
		int players = 100;
		List<Item> some = new ArrayList<>();

		for (Item item : BuiltInRegistries.ITEM) {
			if (item != Items.AIR && some.size() < 60) {
				some.add(item);
			}
		}

		EntityType<?>[] mobs = new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.COW};
		QuestBook book = new QuestBook();

		for (int c = 0; c < 30; c++) {
			Chapter chapter = new Chapter();
			chapter.id = "c" + c;

			for (int q = 0; q < 100; q++) {
				Quest quest = new Quest();
				quest.id = "q" + q;
				if (q % 10 != 0) {
					quest.deps.add("q" + (q - 1));
				}

				Task itemx = new Task();
				itemx.item = BuiltInRegistries.ITEM.getKey(some.get((c * 7 + q) % some.size())).toString();
				itemx.count = 64L;
				Task kill = new Task();
				kill.entity = BuiltInRegistries.ENTITY_TYPE.getKey(mobs[(c + q) % mobs.length]).toString();
				kill.count = 1000000L;
				quest.tasks.add(itemx);
				quest.tasks.add(kill);
				if (q % 10 == 0) {
					Task stat = new Task();
					stat.stat = "minecraft:custom/minecraft:jump";
					stat.count = 1000000L;
					quest.tasks.add(stat);
				}

				chapter.quests.add(quest);
			}

			book.chapters.put(chapter.id, chapter.sanitize());
		}

		this.world = new SelfTest.Scratch();
		this.world.quiet = true;
		this.engine = new QuestEngine(this.world, new HolderStore(null), this.server.registryAccess(), 7L);
		long t0 = System.nanoTime();
		this.engine.book(book);
		long indexing = System.nanoTime() - t0;
		List<Seat> seats = new ArrayList<>();
		t0 = System.nanoTime();

		for (int p = 0; p < 100; p++) {
			UUID id = new UUID(5351393L, (long)p);

			for (int i = 0; i < 36; i++) {
				this.world.items(id).add(new ItemStack((ItemLike)some.get((p + i) % some.size()), 1 + i % 5));
			}

			seats.add(this.engine.join(id, "P" + p, null));
		}

		long seating = (System.nanoTime() - t0) / 100L;
		this.eq("a tenth of a chained book is open to a new player", 300, seats.get(0).open.size());
		this.engine.lookFor();
		t0 = System.nanoTime();

		for (int i = 0; i < 20; i++) {
			this.engine.lookFor();
		}

		long look = (System.nanoTime() - t0) / 20L;
		t0 = System.nanoTime();

		for (int i = 0; i < 20000; i++) {
			this.engine.killed(seats.get(i % 100), mobs[i % mobs.length]);
		}

		long kill = (System.nanoTime() - t0) / 20000L;
		t0 = System.nanoTime();
		this.engine.flush();
		long flush = System.nanoTime() - t0;
		this.is("kills in a large book are counted for the open quests about that mob", this.count(seats.get(0), "c0/q0", 1) > 0L);
		this.is("items in a large book are counted", seats.get(0).own.quests.values().stream().anyMatch(p -> p.task(0) > 0L));

		for (int i = 0; i < 20; i++) {
			this.engine.tick();
		}

		t0 = System.nanoTime();

		for (int i = 0; i < 100; i++) {
			for (int k = 0; k < 10; k++) {
				this.engine.killed(seats.get((i * 10 + k) % 100), mobs[k % mobs.length]);
			}

			this.engine.tick();
		}

		long tick = (System.nanoTime() - t0) / 100L;
		CFHGuiQuests.LOGGER
			.info(
				"[CFHGui Quests] selftest: {} quests, {} players: indexing {} ms, sitting down {} us each, a look at everyone at once {} us, a kill {} ns, sending everything that changed to everyone {} us, a tick with ten kills in it {} us",
				new Object[]{3000, 100, indexing / 1000000L, seating / 1000L, look / 1000L, kill, flush / 1000L, tick / 1000L}
			);
		this.is("a look at a hundred players in a book of three thousand quests is cheap (" + look / 1000L + " us)", look < 50000000L);
		this.is("a kill in a book of three thousand quests is cheap (" + kill + " ns)", kill < 200000L);
		this.is("a tick with a hundred players in a book of three thousand quests is cheap (" + tick / 1000L + " us)", tick < 10000000L);
	}

	static record Report(int passed, List<String> failures) {
	}

	private static final class Scratch implements World {
		long clock = 1700000000000L;
		final QuestsServerConfig config = new QuestsServerConfig();
		final SelfTest.ScratchTeams teams = new SelfTest.ScratchTeams();
		boolean quiet;
		final Map<UUID, List<CustomPacketPayload>> sent = new HashMap<>();
		final Map<UUID, List<Component>> told = new HashMap<>();
		final List<Component> broadcasts = new ArrayList<>();
		final Map<UUID, List<ItemStack>> items = new HashMap<>();
		final Map<UUID, Map<Stat<?>, Long>> stats = new HashMap<>();
		final Map<UUID, Set<ResourceLocation>> advancements = new HashMap<>();
		final Map<UUID, int[]> xp = new HashMap<>();
		final Map<UUID, SelfTest.Scratch.Place> places = new HashMap<>();
		final Map<UUID, World.Sight> sights = new HashMap<>();
		final Map<UUID, List<Reward>> given = new HashMap<>();
		final Map<UUID, Set<String>> stages = new HashMap<>();
		final Map<UUID, Map<String, Long>> fluids = new HashMap<>();
		final Map<UUID, Long> energy = new HashMap<>();
		final List<String> questsDone = new ArrayList<>();
		final List<String> chaptersDone = new ArrayList<>();

		List<ItemStack> items(UUID id) {
			return this.items.computeIfAbsent(id, k -> new ArrayList<>());
		}

		Map<Stat<?>, Long> stats(UUID id) {
			return this.stats.computeIfAbsent(id, k -> new HashMap<>());
		}

		Set<ResourceLocation> advancements(UUID id) {
			return this.advancements.computeIfAbsent(id, k -> new HashSet<>());
		}

		int[] xp(UUID id) {
			return this.xp.computeIfAbsent(id, k -> new int[2]);
		}

		List<Reward> given(UUID id) {
			return this.given.computeIfAbsent(id, k -> new ArrayList<>());
		}

		boolean gave(UUID id, Predicate<Reward> which) {
			return this.given(id).stream().anyMatch(which);
		}

		void clear() {
			this.sent.clear();
			this.told.clear();
			this.broadcasts.clear();
		}

		int count(Seat s, Class<?> type) {
			int n = 0;

			for (CustomPacketPayload p : this.sent.getOrDefault(s.id, List.of())) {
				if (type.isInstance(p)) {
					n++;
				}
			}

			return n;
		}

		int resets(Seat s) {
			int n = 0;

			for (CustomPacketPayload p : this.sent.getOrDefault(s.id, List.of())) {
				if (p instanceof QuestsNet.Progress) {
					QuestsNet.Progress progress = (QuestsNet.Progress)p;
					if (progress.reset()) {
						n++;
					}
				}
			}

			return n;
		}

		boolean happened(Seat s, String kind, String subject) {
			for (CustomPacketPayload p : this.sent.getOrDefault(s.id, List.of())) {
				if (p instanceof QuestsNet.Happened h && h.kind().equals(kind) && h.subject().equals(subject)) {
					return true;
				}
			}

			return false;
		}

		@Nullable
		QuestState lastState(Seat s, String key) {
			QuestState last = null;

			for (CustomPacketPayload p : this.sent.getOrDefault(s.id, List.of())) {
				if (p instanceof QuestsNet.Progress) {
					QuestsNet.Progress progress = (QuestsNet.Progress)p;
					QuestState[] states = (QuestState[])JsonWire.decode(BookJson.GSON, progress.states(), QuestState[].class);
					if (progress.reset()) {
						last = null;
					}

					if (states != null) {
						for (QuestState st : states) {
							if (st != null && key.equals(st.key)) {
								last = st;
							}
						}
					}
				}
			}

			return last;
		}

		boolean told(Seat s, String key) {
			for (Component c : this.told.getOrDefault(s.id, List.of())) {
				if (keyed(c, key)) {
					return true;
				}
			}

			return false;
		}

		boolean broadcast(String key) {
			for (Component c : this.broadcasts) {
				if (keyed(c, key)) {
					return true;
				}
			}

			return false;
		}

		private static boolean keyed(Component c, String key) {
			if (c.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
				return true;
			}

			return false;
		}

		@Override
		public long now() {
			return this.clock;
		}

		@Override
		public QuestsServerConfig config() {
			return this.config;
		}

		@Override
		public TeamLink teams() {
			return this.teams;
		}

		@Override
		public Object datapacks() {
			return this;
		}

		@Override
		public boolean hasClient(Seat seat) {
			return true;
		}

		@Override
		public void send(Seat seat, CustomPacketPayload payload) {
			if (!this.quiet) {
				this.sent.computeIfAbsent(seat.id, k -> new ArrayList<>()).add(payload);
			}
		}

		@Override
		public void tell(Seat seat, Component message) {
			if (!this.quiet) {
				this.told.computeIfAbsent(seat.id, k -> new ArrayList<>()).add(message);
			}
		}

		@Override
		public void broadcast(Component message) {
			if (!this.quiet) {
				this.broadcasts.add(message);
			}
		}

		@Override
		public Component displayName(Seat seat) {
			return Component.literal(seat.name);
		}

		@Override
		public boolean announcesAdvancements() {
			return true;
		}

		@Override
		public boolean mayEdit(Seat seat) {
			return false;
		}

		@Override
		public void inventory(Seat seat, Consumer<ItemStack> each) {
			List<ItemStack> stacks = this.items.get(seat.id);
			if (stacks != null) {
				for (ItemStack stack : stacks) {
					if (!stack.isEmpty()) {
						each.accept(stack);
					}
				}
			}
		}

		@Override
		public long statistic(Seat seat, Stat<?> stat) {
			Map<Stat<?>, Long> of = this.stats.get(seat.id);
			return of == null ? 0L : of.getOrDefault(stat, 0L);
		}

		@Override
		public boolean advancement(Seat seat, ResourceLocation id, String criterion) {
			Set<ResourceLocation> of = this.advancements.get(seat.id);
			return of != null && of.contains(criterion.isEmpty() ? id : ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/" + criterion));
		}

		@Override
		public boolean stage(Seat seat, String stage) {
			return this.stages.getOrDefault(seat.id, Set.of()).contains(stage);
		}

		@Override
		public long fluid(Seat seat, Specs.Named fluid) {
			Map<String, Long> of = this.fluids.get(seat.id);
			return of == null ? 0L : of.getOrDefault(fluid.id().toString(), 0L);
		}

		@Override
		public long drain(Seat seat, Specs.Named fluid, long amount) {
			Map<String, Long> of = this.fluids.get(seat.id);
			long had = of == null ? 0L : of.getOrDefault(fluid.id().toString(), 0L);
			long taken = Math.min(had, amount);
			if (taken > 0L) {
				of.put(fluid.id().toString(), had - taken);
			}

			return taken;
		}

		@Override
		public long takeEnergy(Seat seat, long amount) {
			Long had = this.energy.get(seat.id);
			if (had == null) {
				return -1L;
			} else {
				long taken = Math.min(had, amount);
				this.energy.put(seat.id, had - taken);
				return taken;
			}
		}

		@Override
		public int experience(Seat seat, boolean points) {
			return this.xp(seat.id)[points ? 1 : 0];
		}

		@Override
		public boolean at(Seat seat, TriggerIndex.Ref place) {
			SelfTest.Scratch.Place p = this.places.get(seat.id);
			if (p == null) {
				return false;
			} else {
				Task t = place.task;
				if (place.id != null && !place.id.toString().equals(p.dimension())) {
					return false;
				} else {
					if (t.box()) {
						if (p.x() < t.x || p.x() >= t.x + t.sizeX || p.y() < t.y || p.y() >= t.y + t.sizeY || p.z() < t.z || p.z() >= t.z + t.sizeZ) {
							return false;
						}
					} else if (t.radius > 0) {
						long dx = (long)(t.x - p.x());
						long dy = (long)(t.y - p.y());
						long dz = (long)(t.z - p.z());
						if (dx * dx + dy * dy + dz * dz > (long)t.radius * (long)t.radius) {
							return false;
						}
					}

					return place.biome != null && !place.biome.id().toString().equals(p.biome())
						? false
						: place.structure == null || place.structure.id().toString().equals(p.structure());
				}
			}
		}

		@Nullable
		@Override
		public World.Sight sight(Seat seat) {
			return this.sights.get(seat.id);
		}

		@Override
		public long take(Seat seat, Specs.ItemSpec spec, long count) {
			long left = count;

			for (ItemStack stack : this.items(seat.id)) {
				if (left > 0L && spec.test(stack)) {
					int taken = (int)Math.min(left, (long)stack.getCount());
					stack.shrink(taken);
					left -= (long)taken;
				}
			}

			return count - left;
		}

		@Override
		public void pay(Seat seat, int amount, boolean points) {
			int[] var10000 = this.xp(seat.id);
			var10000[points ? 1 : 0] = var10000[points ? 1 : 0] - amount;
		}

		@Override
		public void give(Seat seat, Reward reward, String quest) {
			this.given(seat.id).add(reward);
		}

		@Override
		public void questDone(Seat doer, BookIndex.Entry entry) {
			if (!this.quiet) {
				this.questsDone.add(doer.name + ":" + entry.key());
			}
		}

		@Override
		public void chapterDone(Seat seat, Chapter chapter) {
			if (!this.quiet) {
				this.chaptersDone.add(seat.name + ":" + chapter.id);
			}
		}

		static record Place(String dimension, String biome, String structure, int x, int y, int z) {
		}
	}

	private static final class ScratchTeams implements TeamLink {
		final Map<UUID, UUID> teamOf = new HashMap<>();
		final Map<UUID, String> names = new HashMap<>();
		final Map<String, Long> counts = new HashMap<>();
		final List<String> logs = new ArrayList<>();
		long xp;

		@Override
		public UUID holderOf(UUID player) {
			return this.teamOf.getOrDefault(player, player);
		}

		@Override
		public boolean isTeam(UUID holder) {
			return this.names.containsKey(holder);
		}

		@Override
		public String name(UUID holder) {
			return this.names.getOrDefault(holder, "");
		}

		@Override
		public long figure(UUID holder, String what) {
			if (!this.isTeam(holder)) {
				return TeamLink.NONE.figure(holder, what);
			} else {
				return "members".equals(what) ? this.teamOf.values().stream().filter(holder::equals).count() : ("level".equals(what) ? 1L : 0L);
			}
		}

		@Override
		public void addXp(UUID team, long amount) {
			this.xp += amount;
		}

		@Override
		public void count(UUID team, String counter, long delta) {
			this.counts.merge(counter, delta, Long::sum);
		}

		@Override
		public void log(UUID team, String type, UUID actor, String actorName, String subject) {
			this.logs.add(type + ":" + actorName + ":" + subject);
		}
	}
}
