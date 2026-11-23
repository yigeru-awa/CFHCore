package com.onehumanawa.cfhcore.content.gui.quests.client.text;

import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public final class QuestText {
	public static MutableComponent text(@Nullable String s) {
		if (s != null && !s.isEmpty()) {
			return s.length() > 2 && s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}' && s.indexOf(32) < 0
				? Component.translatable(s.substring(1, s.length() - 1))
				: Component.literal(codes(s));
		} else {
			return Component.empty();
		}
	}

	public static String codes(String s) {
		if (s.indexOf(38) < 0) {
			return s;
		} else {
			StringBuilder sb = new StringBuilder(s.length());

			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c == '&' && i + 1 < s.length()) {
					char n = Character.toLowerCase(s.charAt(i + 1));
					if (n == '&') {
						sb.append('&');
						i++;
						continue;
					}

					if ("0123456789abcdefklmnor".indexOf(n) >= 0) {
						sb.append('§').append(n);
						i++;
						continue;
					}
				}

				sb.append(c);
			}

			return sb.toString();
		}
	}

	public static Component title(QuestBook book) {
		return text(book.title);
	}

	public static Component title(Chapter chapter) {
		return text(chapter.title.isBlank() ? chapter.id : chapter.title);
	}

	public static Component title(Quest quest) {
		if (!quest.title.isBlank()) {
			return text(quest.title);
		} else if (!quest.tasks.isEmpty()) {
			return subject(quest.tasks.get(0));
		} else {
			if (quest.isGateway()) {
				QuestBook book = QuestsState.book();
				Chapter sub = book == null ? null : book.chapter(quest.subtree);
				if (sub != null) {
					return title(sub);
				}
			}

			return Component.translatable("cfhgui_quests.quest.untitled");
		}
	}

	public static Component title(BookIndex.Entry entry) {
		return title(entry.quest());
	}

	public static Component secret() {
		return Component.translatable("cfhgui_quests.quest.secret");
	}

	public static Component status(Rules.Status status) {
		return Component.translatable("cfhgui_quests.status." + status.name().toLowerCase(Locale.ROOT));
	}

	public static Component sync(BookIndex.Entry entry) {
		return Component.translatable(entry.sync() == Quest.Sync.TEAM ? "cfhgui_quests.sync.team" : "cfhgui_quests.sync.player");
	}

	public static String count(int done, int total) {
		return done + " / " + total;
	}

	@Nullable
	public static Component needs(BookIndex.Entry entry) {
		String names = names(entry.deps());
		String key = "cfhgui_quests.card.needs";
		if (names.isEmpty()) {
			names = names(BookStats.needs(entry.chapter()));
			key = entry.chapter().isSubtree() ? "cfhgui_quests.card.tree_needs" : "cfhgui_quests.card.chapter_needs";
		}

		return names.isEmpty() ? null : Component.translatable(key, new Object[]{names});
	}

	private static String names(List<String> keys) {
		StringBuilder sb = new StringBuilder();
		int more = 0;

		for (String key : keys) {
			if (!QuestsState.progress().everDone(key)) {
				BookIndex.Entry e = QuestsState.entry(key);
				if (e != null) {
					if (sb.length() > 60) {
						more++;
					} else {
						if (sb.length() > 0) {
							sb.append(", ");
						}

						Rules.Status s = QuestsState.status(key);
						boolean secret = s == Rules.Status.HIDDEN || s == Rules.Status.LOCKED && e.quest().visibility == Quest.Visibility.SECRET;
						sb.append((secret ? secret() : title(e)).getString());
					}
				}
			}
		}

		if (more > 0) {
			sb.append(" +").append(more);
		}

		return sb.toString();
	}

	public static Component span(long ms) {
		long s = Math.max(0L, (ms + 999L) / 1000L);
		if (s >= 86400L) {
			return Component.translatable("cfhgui_quests.time.days", new Object[]{s / 86400L, s % 86400L / 3600L});
		} else if (s >= 3600L) {
			return Component.translatable("cfhgui_quests.time.hours", new Object[]{s / 3600L, String.format(Locale.ROOT, "%02d", s % 3600L / 60L)});
		} else {
			return s >= 60L
				? Component.translatable("cfhgui_quests.time.minutes", new Object[]{s / 60L, String.format(Locale.ROOT, "%02d", s % 60L)})
				: Component.translatable("cfhgui_quests.time.seconds", new Object[]{s});
		}
	}

	public static Component ago(long epochMs) {
		long s = Math.max(0L, (System.currentTimeMillis() - epochMs) / 1000L);
		if (s < 60L) {
			return Component.translatable("cfhgui_quests.ago.now");
		} else if (s < 3600L) {
			return Component.translatable(s < 120L ? "cfhgui_quests.ago.minute" : "cfhgui_quests.ago.minutes", new Object[]{s / 60L});
		} else {
			return s < 86400L
				? Component.translatable(s < 7200L ? "cfhgui_quests.ago.hour" : "cfhgui_quests.ago.hours", new Object[]{s / 3600L})
				: Component.translatable(s < 172800L ? "cfhgui_quests.ago.day" : "cfhgui_quests.ago.days", new Object[]{s / 86400L});
		}
	}

	public static Component label(Task task) {
		if (!task.title.isBlank()) {
			return text(task.title);
		} else {
			Component what = subject(task);
			String var2 = task.type;

			return (Component)(switch (var2) {
				case "item" -> Component.translatable(task.consume ? "cfhgui_quests.task.item.consume" : "cfhgui_quests.task.item", new Object[]{task.count, what});
				case "craft" -> Component.translatable("cfhgui_quests.task.craft", new Object[]{task.count, what});
				case "kill" -> task.name.isBlank()
				? Component.translatable("cfhgui_quests.task.kill", new Object[]{task.count, what})
				: Component.translatable("cfhgui_quests.task.kill.named", new Object[]{task.count, what, task.name});
				case "fluid" -> Component.translatable(task.consume ? "cfhgui_quests.task.fluid.consume" : "cfhgui_quests.task.fluid", new Object[]{amount(task.count), what});
				case "energy" -> Component.translatable("cfhgui_quests.task.energy", new Object[]{String.format(Locale.ROOT, "%,d", task.count)});
				case "stage" -> Component.translatable("cfhgui_quests.task.stage", new Object[]{what});
				case "counter" -> Component.translatable("cfhgui_quests.task.counter", new Object[]{what, task.count});
				case "break" -> Component.translatable("cfhgui_quests.task.break", new Object[]{task.count, what});
				case "place" -> Component.translatable("cfhgui_quests.task.place", new Object[]{task.count, what});
				case "advancement" -> task.criterion.isBlank()
				? Component.translatable("cfhgui_quests.task.advancement", new Object[]{what})
				: Component.translatable("cfhgui_quests.task.advancement.criterion", new Object[]{what, ItemSpecs.readable(task.criterion)});
				case "stat" -> Component.translatable("cfhgui_quests.task.stat", new Object[]{what, statCount(task.stat, task.count)});
				case "xp" -> Component.translatable(
				task.points
					? (task.consume ? "cfhgui_quests.task.xp.points.consume" : "cfhgui_quests.task.xp.points")
					: (task.consume ? "cfhgui_quests.task.xp.levels.consume" : "cfhgui_quests.task.xp.levels"),
				new Object[]{task.count}
			);
				case "location" -> Component.translatable("cfhgui_quests.task.location", new Object[]{what});
				case "observe" -> Component.translatable("cfhgui_quests.task.observe", new Object[]{what});
				case "checkmark" -> Component.translatable("cfhgui_quests.task.checkmark");
				case "quest" -> Component.translatable("cfhgui_quests.task.quest", new Object[]{what});
				case "chapter" -> Component.translatable("cfhgui_quests.task.chapter", new Object[]{what});
				case "team" -> Component.translatable("cfhgui_quests.task.team." + (task.value.isBlank() ? "members" : task.value), new Object[]{task.count});
				default -> what;
			});
		}
	}

	public static Component subject(Task task) {
		if (!task.item.isBlank()) {
			return ItemSpecs.name(task.item);
		} else if (!task.fluid.isBlank()) {
			return fluid(task.fluid);
		} else if (!task.entity.isBlank()) {
			return entity(task.entity);
		} else if (!task.block.isBlank()) {
			return block(task.block);
		} else {
			QuestBook book = QuestsState.book();
			if (!task.quest.isBlank()) {
				Quest q = book == null ? null : book.quest(task.quest);
				return (Component)(q == null ? Component.literal(task.quest) : title(q));
			} else if (!task.chapter.isBlank()) {
				Chapter c = book == null ? null : book.chapter(task.chapter);
				return (Component)(c == null ? Component.literal(task.chapter) : title(c));
			} else if (!task.advancement.isBlank()) {
				return Component.literal(ItemSpecs.readable(task.advancement));
			} else if (!task.stat.isBlank()) {
				return statName(task.stat);
			} else if (!task.structure.isBlank()) {
				return Component.literal(ItemSpecs.readable(task.structure));
			} else if (!task.biome.isBlank()) {
				return Component.literal(ItemSpecs.readable(task.biome));
			} else if (!task.dimension.isBlank()) {
				return Component.literal(ItemSpecs.readable(task.dimension));
			} else if (task.box()) {
				return Component.literal(
					task.x + ", " + task.y + ", " + task.z + " – " + (task.x + task.sizeX - 1) + ", " + (task.y + task.sizeY - 1) + ", " + (task.z + task.sizeZ - 1)
				);
			} else if (task.radius > 0) {
				return Component.literal(task.x + ", " + task.y + ", " + task.z);
			} else {
				return !task.value.isBlank()
					? Component.literal(ItemSpecs.readable(task.value))
					: Component.translatable("cfhgui_quests.task." + (task.type.isBlank() ? "checkmark" : task.type) + ".name");
			}
		}
	}

	private static Component fluid(String spec) {
		if (spec.startsWith("#")) {
			return Component.translatable("cfhgui_quests.any", new Object[]{ItemSpecs.readable(spec.substring(1))});
		} else {
			ResourceLocation id = ResourceLocation.tryParse(spec);
			Fluid fluid = id == null ? null : (Fluid)BuiltInRegistries.FLUID.getOptional(id).orElse(null);
			if (fluid == null) {
				return Component.literal(ItemSpecs.readable(spec));
			} else {
				Block block = fluid.defaultFluidState().createLegacyBlock().getBlock();
				return block == Blocks.AIR ? Component.literal(ItemSpecs.readable(spec)) : block.getName();
			}
		}
	}

	public static Component amount(long millibuckets) {
		return millibuckets >= 1000L && millibuckets % 1000L == 0L
			? Component.translatable(millibuckets == 1000L ? "cfhgui_quests.amount.bucket" : "cfhgui_quests.amount.buckets", new Object[]{millibuckets / 1000L})
			: Component.translatable("cfhgui_quests.amount.mb", new Object[]{String.format(Locale.ROOT, "%,d", millibuckets)});
	}

	private static Component entity(String spec) {
		if (spec.startsWith("#")) {
			return Component.translatable("cfhgui_quests.any", new Object[]{ItemSpecs.readable(spec.substring(1))});
		} else {
			ResourceLocation id = ResourceLocation.tryParse(spec);
			EntityType<?> type = id == null ? null : (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
			return (Component)(type == null ? Component.literal(ItemSpecs.readable(spec)) : type.getDescription());
		}
	}

	private static Component block(String spec) {
		if (spec.startsWith("#")) {
			return Component.translatable("cfhgui_quests.any", new Object[]{ItemSpecs.readable(spec.substring(1))});
		} else {
			ResourceLocation id = ResourceLocation.tryParse(spec);
			Block block = id == null ? null : (Block)BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
			return block == null ? Component.literal(ItemSpecs.readable(spec)) : block.getName();
		}
	}

	public static Component count(Task task, long counted) {
		if (task.count <= 1L) {
			return Component.empty();
		} else {
			long at = Math.min(counted, task.count);
			if (task.is("stat")) {
				Stat<?> stat = stat(task.stat);
				if (stat != null) {
					return Component.literal(stat.format(toInt(at)) + " / " + stat.format(toInt(task.count)));
				}
			}

			if (task.is("fluid")) {
				return Component.translatable("cfhgui_quests.amount.mb", new Object[]{String.format(Locale.ROOT, "%,d / %,d", at, task.count)});
			} else {
				return task.is("energy") ? Component.literal(String.format(Locale.ROOT, "%,d / %,d", at, task.count)) : Component.literal(at + " / " + task.count);
			}
		}
	}

	@Nullable
	public static Component note(Quest quest, Task task, int index, QuestState state) {
		if (quest.sequential && !Rules.taskOpen(quest, state.tasks, index)) {
			return Component.translatable("cfhgui_quests.task.note.sequential");
		} else {
			if (task.is("item") || task.is("craft")) {
				for (String part : ItemSpecs.split(task.item)) {
					if (ItemSpecs.isFilter(part) && !ItemSpecs.filtersKnown()) {
						return Component.translatable("cfhgui_quests.task.note.filter");
					}
				}
			}

			return task.optional ? Component.translatable("cfhgui_quests.task.note.optional") : null;
		}
	}

	@Nullable
	public static Stat<?> stat(@Nullable String spec) {
		if (spec != null && !spec.isBlank()) {
			int slash = spec.indexOf(47, Math.max(0, spec.indexOf(58)));
			if (slash < 0) {
				return null;
			} else {
				ResourceLocation typeId = ResourceLocation.tryParse(spec.substring(0, slash).trim());
				ResourceLocation valueId = ResourceLocation.tryParse(spec.substring(slash + 1).trim());
				if (typeId != null && valueId != null) {
					StatType<?> type = (StatType<?>)BuiltInRegistries.STAT_TYPE.get(typeId);
					return type == null ? null : statOf(type, valueId);
				} else {
					return null;
				}
			}
		} else {
			return null;
		}
	}

	@Nullable
	private static <T> Stat<T> statOf(StatType<T> type, ResourceLocation valueId) {
		if (!type.getRegistry().containsKey(valueId)) {
			return null;
		} else {
			T value = (T)type.getRegistry().get(valueId);
			return value == null ? null : type.get(value);
		}
	}

	public static Component statName(String spec) {
		Stat<?> stat = stat(spec);
		if (stat == null) {
			return Component.literal(ItemSpecs.readable(spec));
		} else {
			Object value = stat.getValue();
			if (value instanceof ResourceLocation id) {
				return Component.translatable("stat." + id.toString().replace(':', '.'));
			} else {
				Component thing = (Component)(value instanceof Block b
					? b.getName()
					: (value instanceof Item i ? i.getDescription() : (value instanceof EntityType<?> e ? e.getDescription() : Component.literal(String.valueOf(value)))));
				return Component.translatable("cfhgui_quests.task.stat.of", new Object[]{thing, stat.getType().getDisplayName()});
			}
		}
	}

	public static String statCount(String spec, long count) {
		Stat<?> stat = stat(spec);
		return stat == null ? Long.toString(count) : stat.format(toInt(count));
	}

	private static int toInt(long n) {
		return (int)Math.max(-2147483648L, Math.min(2147483647L, n));
	}

	public static Component label(Reward reward) {
		if (!reward.title.isBlank()) {
			return text(reward.title);
		} else {
			String var1 = reward.type;

			return (Component)(switch (var1) {
				case "item" -> reward.bonus > 0
				? Component.translatable("cfhgui_quests.reward.item.bonus", new Object[]{reward.count, reward.count + reward.bonus, ItemSpecs.name(reward.item)})
				: (
					reward.count > 1
						? Component.translatable("cfhgui_quests.reward.item", new Object[]{reward.count, ItemSpecs.name(reward.item)})
						: ItemSpecs.name(reward.item)
				);
				case "all" -> Component.translatable("cfhgui_quests.reward.all", new Object[]{reward.choices.size()});
				case "toast" -> reward.text.isBlank() ? Component.translatable("cfhgui_quests.reward.toast") : text(reward.text);
				case "stage" -> Component.translatable(
				reward.remove ? "cfhgui_quests.reward.stage.remove" : "cfhgui_quests.reward.stage", new Object[]{ItemSpecs.readable(reward.value)}
			);
				case "counter" -> Component.translatable("cfhgui_quests.reward.counter", new Object[]{reward.count, ItemSpecs.readable(reward.value)});
				case "currency" -> Component.translatable("cfhgui_quests.reward.currency", new Object[]{reward.count});
				case "custom" -> Component.translatable("cfhgui_quests.reward.custom", new Object[]{ItemSpecs.readable(reward.value)});
				case "xp" -> Component.translatable(reward.levels ? "cfhgui_quests.reward.xp.levels" : "cfhgui_quests.reward.xp.points", new Object[]{reward.xp});
				case "command" -> Component.translatable("cfhgui_quests.reward.command");
				case "loot" -> Component.translatable("cfhgui_quests.reward.loot");
				case "choice" -> Component.translatable("cfhgui_quests.reward.choice", new Object[]{reward.choices.size()});
				case "random" -> reward.rolls > 1
				? Component.translatable("cfhgui_quests.reward.random.rolls", new Object[]{reward.rolls})
				: Component.translatable("cfhgui_quests.reward.random");
				case "team_xp" -> Component.translatable("cfhgui_quests.reward.team_xp", new Object[]{reward.xp});
				case "advancement" -> Component.translatable("cfhgui_quests.reward.advancement", new Object[]{ItemSpecs.readable(reward.advancement)});
				default -> Component.literal(reward.type);
			});
		}
	}

	private QuestText() {
	}
}
