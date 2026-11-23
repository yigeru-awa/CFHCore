package com.onehumanawa.cfhcore.content.gui.quests.client.dev;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvAccount;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvNet;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvOptions;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvService;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvTrees;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class AdvClient {
	private static int nextId = 1;
	private static final Map<Integer, Consumer<AdvNet.Answer>> WAITING = new HashMap<>();
	@Nullable
	private static AdvService.Trees trees;
	@Nullable
	private static Object treesOf;
	private static boolean asking;
	private static boolean failed;
	private static int version;
	private static final Set<String> CHOSEN = new LinkedHashSet<>();
	private static AdvOptions options = new AdvOptions();
	@Nullable
	private static AdvClient.Last last;
	@Nullable
	private static final String TICK = System.getProperty("cfh.quests.advTick");
	private static boolean ticked;

	public static AdvClient.State state() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null && Minecraft.getInstance().level != null) {
			if (!QuestsState.connected() || !CFHGuiNetwork.get().serverHasChannel(AdvNet.Ask.TYPE)) {
				return AdvClient.State.NO_SERVER;
			} else if (!QuestsState.canEdit()) {
				return AdvClient.State.NO_PERMISSION;
			} else {
				if (treesOf != connection) {
					forget();
				}

				if (trees != null) {
					return AdvClient.State.READY;
				} else {
					return failed ? AdvClient.State.FAILED : AdvClient.State.LOADING;
				}
			}
		} else {
			return AdvClient.State.NO_WORLD;
		}
	}

	@Nullable
	public static AdvService.Trees trees() {
		return state() == AdvClient.State.READY ? trees : null;
	}

	@Nullable
	public static AdvTrees.Tree tree(String root) {
		AdvService.Trees t = trees();
		if (t != null) {
			for (AdvTrees.Tree tree : t.trees) {
				if (tree.root.equals(root)) {
					return tree;
				}
			}
		}

		return null;
	}

	public static int version() {
		return version;
	}

	public static Set<String> chosen() {
		return CHOSEN;
	}

	public static void choose(String root, boolean on) {
		if (on ? CHOSEN.add(root) : CHOSEN.remove(root)) {
			version++;
		}
	}

	public static AdvOptions options() {
		return options;
	}

	public static List<String> undoable() {
		AdvService.Trees t = trees();
		return t == null ? List.of() : t.undo;
	}

	public static void want() {
		AdvClient.State s = state();
		if (s == AdvClient.State.LOADING && !asking) {
			refresh();
		}
	}

	public static void refresh() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null && !asking) {
			AdvClient.State s = state();
			if (s != AdvClient.State.NO_WORLD && s != AdvClient.State.NO_SERVER && s != AdvClient.State.NO_PERMISSION) {
				asking = true;
				failed = false;
				treesOf = connection;
				ask("trees", new CompoundTag(), answer -> {
					asking = false;
					AdvService.Trees got = answer.ok() ? (AdvService.Trees)JsonWire.decode(BookJson.GSON, answer.data(), AdvService.Trees.class) : null;
					if (got == null) {
						failed = true;
					} else {
						if (got.trees == null) {
							got.trees = new ArrayList<>();
						}

						if (got.groups == null) {
							got.groups = new LinkedHashMap<>();
						}

						if (got.undo == null) {
							got.undo = new ArrayList<>();
						}

						got.trees.removeIf(tx -> tx == null || tx.root == null || tx.root.isEmpty());
						trees = got;
						Set<String> there = new LinkedHashSet<>();

						for (AdvTrees.Tree t : got.trees) {
							there.add(t.root);
						}

						CHOSEN.retainAll(there);
						if (TICK != null && !ticked) {
							ticked = true;

							for (AdvTrees.Tree t : got.trees) {
								if (TICK.equals("*") || TICK.equals(t.namespace) || TICK.equals(t.root)) {
									CHOSEN.add(t.root);
								}
							}
						}
					}

					version++;
				});
			}
		}
	}

	public static void preview(String root, AdvOptions with, BiConsumer<AdvService.Preview, Component> then) {
		CompoundTag args = new CompoundTag();
		args.putString("root", root);
		args.putString("options", BookJson.GSON.toJson(with));
		ask("preview", args, answer -> {
			AdvService.Preview p = answer.ok() ? (AdvService.Preview)JsonWire.decode(BookJson.GSON, answer.data(), AdvService.Preview.class) : null;
			if (p != null && p.chapter != null) {
				p.chapter.sanitize();
				if (p.account == null) {
					p.account = new AdvAccount();
				}

				then.accept(p, null);
			} else {
				then.accept(null, describe(answer));
			}
		});
	}

	public static void convert(List<String> roots, AdvOptions with, BiConsumer<AdvService.Done, Component> then) {
		CompoundTag args = new CompoundTag();
		ListTag list = new ListTag();

		for (String root : roots) {
			list.add(StringTag.valueOf(root));
		}

		args.put("roots", list);
		args.putString("options", BookJson.GSON.toJson(with));
		ask("convert", args, answer -> {
			AdvService.Done done = answer.ok() ? (AdvService.Done)JsonWire.decode(BookJson.GSON, answer.data(), AdvService.Done.class) : null;
			refresh();
			if (done != null && done.accounts != null) {
				remember(done);
				then.accept(done, null);
			} else {
				then.accept(null, describe(answer));
			}
		});
	}

	public static void undo(Consumer<Component> then) {
		ask("undo", new CompoundTag(), answer -> {
			if (answer.ok()) {
				last = null;
			}

			refresh();
			then.accept(answer.ok() ? null : describe(answer));
		});
	}

	private static void ask(String op, CompoundTag args, Consumer<AdvNet.Answer> then) {
		int id = nextId++;
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (Minecraft.getInstance().getConnection() != null && net.serverHasChannel(AdvNet.Ask.TYPE)) {
			WAITING.put(id, then);
			net.sendToServer(new AdvNet.Ask(id, op, args));
		} else {
			then.accept(new AdvNet.Answer(id, false, "disabled", List.of(), new byte[0]));
		}
	}

	public static void onAnswer(AdvNet.Answer answer) {
		Consumer<AdvNet.Answer> then = WAITING.remove(answer.id());
		if (then != null) {
			then.accept(answer);
		}
	}

	public static Component describe(AdvNet.Answer answer) {
		String code = answer.code();
		boolean own = code.equals("no_tree")
			|| code.equals("nothing_to_undo")
			|| code.equals("edited_since")
			|| code.equals("bad_id")
			|| code.equals("id_twice")
			|| code.equals("too_big");
		return Component.translatable((own ? "cfhgui_quests.adv.result." : "cfhgui_quests.result.") + code, answer.args().toArray());
	}

	private static void remember(AdvService.Done done) {
		QuestBook book = QuestsState.book();
		Map<String, String> written = new LinkedHashMap<>();
		if (book != null) {
			for (AdvAccount a : done.accounts) {
				Chapter c = a == null ? null : book.chapter(a.chapter);
				if (c != null) {
					written.put(c.id, BookJson.GSON.toJson(c));
				}
			}
		}

		last = written.isEmpty() ? null : new AdvClient.Last(Minecraft.getInstance().getConnection(), written);
	}

	public static boolean undoes(@Nullable Chapter shown) {
		AdvClient.Last l = last;
		QuestBook book = QuestsState.book();
		if (l != null && shown != null && book != null && l.server() == Minecraft.getInstance().getConnection() && l.written().containsKey(shown.id)) {
			for (Entry<String, String> e : l.written().entrySet()) {
				Chapter c = book.chapter(e.getKey());
				if (c == null || !BookJson.GSON.toJson(c).equals(e.getValue())) {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public static int lastSize() {
		return last == null ? 0 : last.written().size();
	}

	private static void forget() {
		trees = null;
		treesOf = Minecraft.getInstance().getConnection();
		asking = false;
		failed = false;
		WAITING.clear();
		last = null;
		version++;
	}

	static void left() {
		forget();
		treesOf = null;
		CHOSEN.clear();
	}

	private AdvClient() {
	}

	private static record Last(Object server, Map<String, String> written) {
	}

	public static enum State {
		NO_WORLD,
		NO_SERVER,
		NO_PERMISSION,
		LOADING,
		FAILED,
		READY;
	}
}
