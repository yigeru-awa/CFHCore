package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import com.onehumanawa.cfhcore.content.gui.quests.server.BookDoor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

public final class AdvConverter {
	private static final int SUBTITLE_MAX = 72;
	private static final int MAX_TASKS = 64;
	private static final int MAX_REWARDS = 64;
	private static final int RECIPES_APART = 12;
	private static final float FOCUS_W = 16.0F;
	private static final float FOCUS_H = 9.0F;
	private final MinecraftServer server;
	private final Provider registries;
	private final AdvCriteria criteria;

	public AdvConverter(MinecraftServer server) {
		this.server = server;
		this.registries = server.registryAccess();
		this.criteria = new AdvCriteria(server);
	}

	public static String chapterId(ResourceLocation root) {
		String path = root.getPath();
		if (path.endsWith("/root")) {
			path = path.substring(0, path.length() - "/root".length());
		} else if (path.equals("root")) {
			path = "";
		}

		return valid(root.getNamespace() + (path.isEmpty() ? "" : "_" + path), "advancements");
	}

	static String valid(String text, String fallback) {
		StringBuilder sb = new StringBuilder(text.length());

		for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
			sb.append((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_' && c != '-' && c != '.' ? '_' : c);
		}

		String id = sb.toString();
		if (id.isEmpty()) {
			id = fallback;
		}

		if (id.length() > 64) {
			id = id.substring(0, 55) + "-" + String.format("%08x", text.hashCode());
		}

		return id;
	}

	private static String questId(ResourceLocation root, ResourceLocation id, Set<String> used) {
		String path = id.getPath();
		int slash = root.getPath().lastIndexOf(47);
		String folder = slash < 0 ? "" : root.getPath().substring(0, slash + 1);
		if (!folder.isEmpty() && path.startsWith(folder) && path.length() > folder.length()) {
			path = path.substring(folder.length());
		}

		String base = path.replace('/', '.');
		if (!id.getNamespace().equals(root.getNamespace())) {
			base = id.getNamespace() + "." + base;
		}

		base = valid(base, "quest");
		String out = base;

		for (int n = 2; !used.add(out); n++) {
			String tail = "_" + n;
			out = (base.length() + tail.length() > 64 ? base.substring(0, 64 - tail.length()) : base) + tail;
		}

		return out;
	}

	public AdvConverter.Made convert(AdvancementNode root, AdvOptions options, String chapterId, String title) {
		ResourceLocation rootId = root.holder().id();
		DisplayInfo rootDisplay = (DisplayInfo)root.advancement().display().orElseThrow();
		AdvAccount account = new AdvAccount();
		account.root = rootId.toString();
		account.chapter = chapterId;
		Chapter c = new Chapter();
		c.id = chapterId;
		c.title = title.isBlank() ? line(rootDisplay.getTitle(), options.keys, rootId.getPath()) : title;
		c.subtitle = line(rootDisplay.getDescription(), options.keys, "");
		c.icon = this.icon(rootDisplay.getIcon(), "minecraft:book");
		c.backdrop = rootDisplay.getBackground().<String>map(ResourceLocation::toString).orElse("");
		c.shape = Quest.Shape.ROUNDED;
		c.sync = options.advancementKind() ? Quest.Sync.PLAYER : Quest.Sync.TEAM;
		account.title = c.title;
		Set<String> used = new HashSet<>();
		Deque<AdvancementNode> nodes = new ArrayDeque<>();
		Deque<String> parents = new ArrayDeque<>();
		nodes.push(root);
		parents.push("");
		float ox = rootDisplay.getX();
		float oy = rootDisplay.getY();
		float minX = 0.0F;
		float minY = 0.0F;
		float maxX = 0.0F;
		float maxY = 0.0F;
		String first = "";

		while (!nodes.isEmpty()) {
			AdvancementNode node = nodes.pop();
			String parent = parents.pop();
			DisplayInfo d = (DisplayInfo)node.advancement().display().orElse(null);
			if (d == null) {
				account.skipped++;
			} else {
				Quest q = this.quest(rootId, node, d, parent, options, used, account);
				Quest above = parent.isEmpty() ? null : c.quest(parent);
				if (above != null && !options.free() && q.visibility == Quest.Visibility.VISIBLE && above.visibility != Quest.Visibility.VISIBLE) {
					q.visibility = Quest.Visibility.FOLLOW;
				}

				q.x = tidy((d.getX() - ox) * options.spacing);
				q.y = tidy((d.getY() - oy) * options.spacing);
				minX = Math.min(minX, q.x);
				minY = Math.min(minY, q.y);
				maxX = Math.max(maxX, q.x);
				maxY = Math.max(maxY, q.y);
				if (node == root) {
					first = q.id;
					if (!options.advancementKind()) {
						q.shape = Quest.Shape.HEXAGON;
						q.size = 1.4F;
					}
				}

				c.quests.add(q);
				parent = q.id;
			}

			List<AdvancementNode> children = new ArrayList<>();

			for (AdvancementNode child : node.children()) {
				children.add(child);
			}

			children.sort(
				Comparator.<AdvancementNode>comparingDouble(n -> (double)n.advancement().display().<Float>map(DisplayInfo::getY).orElse(0.0F).floatValue())
					.thenComparingDouble(n -> (double)n.advancement().display().<Float>map(DisplayInfo::getX).orElse(0.0F).floatValue())
					.thenComparing(n -> n.holder().id().toString())
			);

			for (int i = children.size() - 1; i >= 0; i--) {
				nodes.push(children.get(i));
				parents.push(parent);
			}
		}

		if (maxX - minX > 16.0F || maxY - minY > 9.0F) {
			c.focus = first;
		}

		c.sanitize();

		for (String unreadable : BookDoor.unreadable(c)) {
			Quest qx = c.quest(unreadable);
			if (qx != null) {
				String id = tagged(qx);
				if (!id.isEmpty()) {
					wasCounted(account, qx);
					qx.tasks.clear();
					qx.anyTask = false;
					qx.tasks.add(advancement(id, ""));
					account.kept++;
					account.mark(qx.id, "kept", "unreadable");
				}
			}
		}

		account.quests = c.quests.size();
		account.tasks = 0;
		account.rewards = 0;

		for (Quest qx : c.quests) {
			account.tasks = account.tasks + qx.tasks.size();
			account.rewards = account.rewards + qx.rewards.size();
		}

		return new AdvConverter.Made(c, account);
	}

	private static void wasCounted(AdvAccount account, Quest q) {
		String mark = account.marks.remove(q.id);
		account.why.remove(q.id);
		if (mark == null) {
			account.translated--;
		} else if ("partly".equals(mark)) {
			account.partly--;
		} else {
			account.kept--;
		}
	}

	private static String tagged(Quest q) {
		return q.tags.isEmpty() ? "" : q.tags.get(0);
	}

	private static float tidy(float v) {
		float r = (float)Math.round(v * 100.0F) / 100.0F;
		return r == 0.0F ? 0.0F : r;
	}

	private Quest quest(ResourceLocation rootId, AdvancementNode node, DisplayInfo d, String parent, AdvOptions options, Set<String> used, AdvAccount account) {
		ResourceLocation id = node.holder().id();
		Advancement advancement = node.advancement();
		Quest q = new Quest();
		q.id = questId(rootId, id, used);
		q.title = line(d.getTitle(), options.keys, id.getPath());
		String said = d.getDescription().getString().strip();
		if (said.length() <= 72 && said.indexOf(10) < 0) {
			q.subtitle = line(d.getDescription(), options.keys, "");
		} else if (options.keys && key(d.getDescription()) != null) {
			q.description.add("{" + key(d.getDescription()) + "}");
		} else {
			for (String paragraph : said.split("\n")) {
				q.description.add(rich(paragraph));
			}
		}

		q.icon = this.icon(d.getIcon(), "");
		q.frame = d.getType() == AdvancementType.GOAL ? Quest.Frame.GOAL : (d.getType() == AdvancementType.CHALLENGE ? Quest.Frame.CHALLENGE : Quest.Frame.TASK);
		if (options.advancementKind()) {
			q.kind = Quest.Kind.ADVANCEMENT;
		} else if (q.frame == Quest.Frame.GOAL) {
			q.shape = Quest.Shape.DIAMOND;
			q.size = 1.2F;
		} else if (q.frame == Quest.Frame.CHALLENGE) {
			q.shape = Quest.Shape.STAR;
			q.size = 1.4F;
		}

		if (!parent.isEmpty()) {
			q.deps.add(parent);
		}

		q.looseDeps = options.free();
		if (d.isHidden()) {
			q.visibility = options.free() ? Quest.Visibility.INVISIBLE : Quest.Visibility.HIDDEN;
			account.hidden++;
		}

		q.silent = !d.shouldShowToast() && !d.shouldAnnounceChat();
		q.tags.add(id.toString());
		this.tasks(q, id, advancement, options, account);
		if (options.rewards) {
			q.rewards.addAll(this.rewards(advancement.rewards()));
		}

		return q;
	}

	private static Task advancement(String id, String criterion) {
		Task t = new Task();
		t.type = "advancement";
		t.advancement = id;
		t.criterion = criterion;
		return t;
	}

	private void tasks(Quest q, ResourceLocation id, Advancement advancement, AdvOptions options, AdvAccount account) {
		String name = id.toString();
		List<List<String>> groups = advancement.requirements().requirements();
		if (!options.translate()) {
			this.keep(q, name, account, "off");
		} else if (groups.isEmpty()) {
			this.keep(q, name, account, "no_criteria");
		} else {
			boolean single = true;

			for (List<String> group : groups) {
				if (group.size() != 1) {
					single = false;
				}
			}

			List<Task> tasks = new ArrayList<>();
			List<String> missed = new ArrayList<>();
			int own = 0;
			int left = 0;
			boolean any = false;
			if (single) {
				for (List<String> groupx : groups) {
					String criterion = groupx.get(0);
					AdvCriteria.Piece piece = this.piece(advancement, criterion);
					if (piece.said()) {
						tasks.addAll(piece.tasks());
						own++;
					} else {
						tasks.add(advancement(name, criterion));
						missed.add(piece.why());
						left++;
					}
				}
			} else if (groups.size() == 1) {
				any = true;

				for (String criterion : groups.get(0)) {
					AdvCriteria.Piece piece = this.piece(advancement, criterion);
					if (piece.said() && piece.tasks().isEmpty()) {
						tasks.clear();
						any = false;
						own = 1;
						left = 0;
						missed.clear();
						break;
					}

					if (piece.said() && piece.tasks().size() == 1) {
						tasks.add(piece.tasks().get(0));
						own++;
					} else {
						tasks.add(advancement(name, criterion));
						missed.add(piece.said() ? trigger(advancement, criterion) : piece.why());
						left++;
					}
				}
			} else {
				for (List<String> groupxx : groups) {
					if (groupxx.size() == 1) {
						AdvCriteria.Piece piecex = this.piece(advancement, groupxx.get(0));
						if (piecex.said()) {
							tasks.addAll(piecex.tasks());
							own++;
						} else {
							tasks.add(advancement(name, groupxx.get(0)));
							missed.add(piecex.why());
							left++;
						}
					} else {
						Task either = this.either(advancement, groupxx);
						if (either == null) {
							this.keep(q, name, account, "requirements");
							return;
						}

						tasks.add(either);
						own++;
					}
				}
			}

			if (tasks.size() > 64) {
				this.keep(q, name, account, "too_many");
			} else {
				for (String trigger : missed) {
					account.trigger(trigger);
				}

				if (own != 0 || left <= 0) {
					q.tasks.addAll(tasks);
					q.anyTask = any && tasks.size() > 1;
					if (left > 0) {
						account.partly++;
						account.mark(q.id, "partly", "trigger");
					} else {
						account.translated++;
					}
				} else if (left != 1 && !any) {
					q.tasks.addAll(tasks);
					account.kept++;
					account.mark(q.id, "kept", "trigger");
				} else {
					this.keep(q, name, account, "trigger");
				}
			}
		}
	}

	private void keep(Quest q, String advancement, AdvAccount account, String why) {
		q.tasks.clear();
		q.anyTask = false;
		q.tasks.add(advancement(advancement, ""));
		account.kept++;
		account.mark(q.id, "kept", why);
	}

	private AdvCriteria.Piece piece(Advancement advancement, String name) {
		Criterion<?> criterion = (Criterion<?>)advancement.criteria().get(name);
		return criterion == null ? AdvCriteria.Piece.cannot("unknown") : this.criteria.read(criterion);
	}

	private static String trigger(Advancement advancement, String name) {
		Criterion<?> criterion = (Criterion<?>)advancement.criteria().get(name);
		return criterion == null ? "unknown" : AdvCriteria.triggerId(criterion);
	}

	@Nullable
	private Task either(Advancement advancement, List<String> group) {
		StringBuilder spec = new StringBuilder();
		String type = null;
		long count = -1L;

		for (String name : group) {
			AdvCriteria.Piece piece = this.piece(advancement, name);
			if (piece.said() && piece.tasks().size() == 1) {
				Task t = piece.tasks().get(0);
				if (!t.is("item") && !t.is("craft")) {
					return null;
				}

				if ((type == null || type.equals(t.type)) && (count < 0L || count == t.count)) {
					type = t.type;
					count = t.count;
					if (spec.length() > 0) {
						spec.append(" | ");
					}

					spec.append(t.item);
					continue;
				}

				return null;
			}

			return null;
		}

		if (type == null) {
			return null;
		} else {
			Task tx = new Task();
			tx.type = type;
			tx.item = spec.toString();
			tx.count = count;
			return tx;
		}
	}

	private List<Reward> rewards(AdvancementRewards given) {
		List<Reward> out = new ArrayList<>();
		if (given.experience() > 0) {
			Reward r = new Reward();
			r.type = "xp";
			r.xp = given.experience();
			out.add(r);
		}

		for (ResourceKey<LootTable> table : given.loot()) {
			Reward r = new Reward();
			r.type = "loot";
			r.loot = table.location().toString();
			out.add(r);
		}

		List<Reward> recipes = new ArrayList<>();

		for (ResourceLocation recipe : given.recipes()) {
			Reward r = new Reward();
			r.type = "command";
			r.command = "recipe give {player} " + recipe;
			r.title = "{cfhgui_quests.adv.reward.recipe}";
			RecipeHolder<?> holder = (RecipeHolder<?>)this.server.getRecipeManager().byKey(recipe).orElse(null);
			ItemStack result = holder == null ? ItemStack.EMPTY : holder.value().getResultItem(this.registries);
			r.icon = result.isEmpty() ? "minecraft:knowledge_book" : BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
			recipes.add(r);
		}

		if (recipes.size() > 12) {
			Reward all = new Reward();
			all.type = "all";
			all.title = "{cfhgui_quests.adv.reward.recipes}";
			all.icon = "minecraft:knowledge_book";
			all.choices.addAll(recipes);
			out.add(all);
		} else {
			out.addAll(recipes);
		}

		given.function().ifPresent(function -> {
			Reward rx = new Reward();
			rx.type = "command";
			rx.command = "function " + function.getId();
			rx.title = "{cfhgui_quests.adv.reward.function}";
			rx.icon = "minecraft:command_block";
			out.add(rx);
		});
		if (out.size() > 64) {
			out.subList(64, out.size()).clear();
		}

		return out;
	}

	@Nullable
	private static String key(Component c) {
		if (!(c.getContents() instanceof TranslatableContents t) || t.getArgs().length > 0 || !c.getSiblings().isEmpty()) {
			return null;
		}

		return t.getFallback() == null && !t.getKey().isEmpty() && t.getKey().indexOf(32) < 0 ? t.getKey() : null;
	}

	private static String line(Component c, boolean keys, String fallback) {
		String key = keys ? key(c) : null;
		if (key != null) {
			return "{" + key + "}";
		} else {
			String s = c.getString().replace('\n', ' ').strip();
			if (s.isEmpty()) {
				return fallback;
			} else {
				s = s.replace("&", "&&");
				if (s.length() > 2 && s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}' && s.indexOf(32) < 0) {
					s = s + " ";
				}

				return s;
			}
		}
	}

	private static String rich(String line) {
		StringBuilder sb = new StringBuilder(line.length() + 8);
		String s = line.strip();
		if (!s.isEmpty() && "#-•!".indexOf(s.charAt(0)) >= 0) {
			sb.append('\\');
		}

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("\\*_~`[{&".indexOf(c) >= 0) {
				sb.append('\\');
			}

			sb.append(c);
		}

		return sb.toString();
	}

	private String icon(ItemStack stack, String fallback) {
		if (stack.isEmpty()) {
			return fallback;
		} else {
			String plain = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			if (stack.getComponentsPatch().isEmpty()) {
				return plain;
			} else {
				try {
					String spec = new ItemInput(stack.getItemHolder(), stack.getComponentsPatch()).serialize(this.registries);
					if (spec.length() <= 1024) {
						return spec;
					}
				} catch (RuntimeException var5) {
				}

				return plain;
			}
		}
	}

	public static record Made(Chapter chapter, AdvAccount account) {
	}
}
