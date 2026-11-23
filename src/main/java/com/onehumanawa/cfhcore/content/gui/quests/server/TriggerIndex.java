package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

final class TriggerIndex {
	private static final TriggerIndex.Ref[] NONE = new TriggerIndex.Ref[0];
	final TriggerIndex.ByThing<Item> held = new TriggerIndex.ByThing<>(BuiltInRegistries.ITEM::wrapAsHolder);
	final TriggerIndex.ByThing<Item> crafted = new TriggerIndex.ByThing<>(BuiltInRegistries.ITEM::wrapAsHolder);
	final TriggerIndex.ByThing<EntityType<?>> kills = new TriggerIndex.ByThing<>(BuiltInRegistries.ENTITY_TYPE::wrapAsHolder);
	final TriggerIndex.ByThing<Block> breaks = new TriggerIndex.ByThing<>(BuiltInRegistries.BLOCK::wrapAsHolder);
	final TriggerIndex.ByThing<Block> places = new TriggerIndex.ByThing<>(BuiltInRegistries.BLOCK::wrapAsHolder);
	private final Map<String, TriggerIndex.Tasks> tasks = new HashMap<>();
	private final Map<String, List<TriggerIndex.Ref>> questTasks = new HashMap<>();
	private final Map<String, List<TriggerIndex.Ref>> chapterTasks = new HashMap<>();
	private final Map<String, List<TriggerIndex.Ref>> customs = new HashMap<>();
	private final Map<String, List<String>> requiredBy = new HashMap<>();
	private final Map<String, List<String>> looseTrees = new HashMap<>();
	final List<String> unreadable = new ArrayList<>();

	TriggerIndex(BookIndex index, Provider registries) {
		Specs.ItemReader items = new Specs.ItemReader(registries);

		for (BookIndex.Entry e : index.all()) {
			Quest q = e.quest();
			if (!q.tasks.isEmpty()) {
				TriggerIndex.Ref[] all = new TriggerIndex.Ref[q.tasks.size()];
				List<TriggerIndex.Ref> looked = new ArrayList<>();
				List<TriggerIndex.Ref> observed = new ArrayList<>();

				for (int i = 0; i < all.length; i++) {
					TriggerIndex.Ref r = all[i] = new TriggerIndex.Ref(e, i);
					Task t = r.task;
					String var13 = t.type;
					switch (var13) {
						case "item":
							r.item = items.read(t.item);
							if (r.item == null) {
								this.unreadable(r, items.why.isEmpty() ? t.item : t.item + ": " + items.why);
							} else if (!t.consume) {
								file(this.held, r.item, r);
							}
							break;
						case "craft":
							r.item = items.read(t.item);
							if (r.item == null && !t.item.isBlank()) {
								this.unreadable(r, items.why.isEmpty() ? t.item : t.item + ": " + items.why);
							} else if (r.item == null) {
								this.crafted.add(null, r);
							} else {
								file(this.crafted, r.item, r);
							}
							break;
						case "fluid":
							r.thing = Specs.Named.parse(t.fluid);
							if (r.thing == null) {
								this.unreadable(r, t.fluid);
							} else if (!t.consume) {
								looked.add(r);
							}
							break;
						case "stage":
						case "counter":
							if (t.value.isBlank()) {
								this.unreadable(r, t.value);
							} else {
								looked.add(r);
							}
							break;
						case "kill":
							r.thing = Specs.Named.parse(t.entity);
							if (r.thing == null && !t.entity.isBlank()) {
								this.unreadable(r, t.entity);
							} else {
								this.kills.add(r.thing, r);
							}
							break;
						case "break":
						case "place":
							r.thing = Specs.Named.parse(t.block);
							if (r.thing == null && !t.block.isBlank()) {
								this.unreadable(r, t.block);
							} else {
								(t.is("break") ? this.breaks : this.places).add(r.thing, r);
							}
							break;
						case "advancement":
							r.id = ResourceLocation.tryParse(t.advancement.trim());
							if (r.id == null) {
								this.unreadable(r, t.advancement);
							} else {
								looked.add(r);
							}
							break;
						case "stat":
							r.stat = Specs.stat(t.stat);
							if (r.stat == null) {
								this.unreadable(r, t.stat);
							} else {
								looked.add(r);
							}
							break;
						case "xp":
							if (!t.consume) {
								looked.add(r);
							}
							break;
						case "location":
							r.id = t.dimension.isBlank() ? null : ResourceLocation.tryParse(t.dimension.trim());
							r.biome = Specs.Named.parse(t.biome);
							r.structure = Specs.Named.parse(t.structure);
							if (!t.dimension.isBlank() && r.id == null) {
								this.unreadable(r, t.dimension);
							} else if (!t.biome.isBlank() && r.biome == null) {
								this.unreadable(r, t.biome);
							} else if (!t.structure.isBlank() && r.structure == null) {
								this.unreadable(r, t.structure);
							} else {
								looked.add(r);
							}
							break;
						case "observe":
							r.block = !t.block.isBlank();
							r.thing = Specs.Named.parse(r.block ? t.block : t.entity);
							if (r.thing == null) {
								this.unreadable(r, r.block ? t.block : t.entity);
							} else {
								observed.add(r);
							}
							break;
						case "quest":
							this.questTasks.computeIfAbsent(Ids.resolve(e.chapter().id, t.quest), k -> new ArrayList<>()).add(r);
							break;
						case "chapter":
							this.chapterTasks.computeIfAbsent(t.chapter, k -> new ArrayList<>()).add(r);
							break;
						case "team":
							looked.add(r);
							break;
						case "custom":
							this.customs.computeIfAbsent(t.value, k -> new ArrayList<>()).add(r);
					}
				}

				this.tasks.put(e.key(), new TriggerIndex.Tasks(all, looked.isEmpty() ? NONE : looked.toArray(NONE), observed.isEmpty() ? NONE : observed.toArray(NONE)));
			}
		}

		for (Chapter c : index.book().chapters.values()) {
			for (String required : c.requires) {
				this.requiredBy.computeIfAbsent(Ids.resolve(c.id, required), k -> new ArrayList<>()).add(c.id);
			}

			if (c.isSubtree() && index.gateways(c.id).isEmpty()) {
				this.looseTrees.computeIfAbsent(c.parent, k -> new ArrayList<>()).add(c.id);
			}
		}
	}

	private static void file(TriggerIndex.ByThing<Item> into, Specs.ItemSpec spec, TriggerIndex.Ref ref) {
		List<Specs.Named> names = spec.names();
		if (names == null) {
			into.add(null, ref);
		} else {
			for (Specs.Named name : names) {
				into.add(name, ref);
			}
		}
	}

	private void unreadable(TriggerIndex.Ref r, String words) {
		this.unreadable.add(r.key + " task " + (r.index + 1) + " (" + r.task.type + " \"" + words + "\")");
	}

	@Nullable
	TriggerIndex.Tasks tasks(String key) {
		return this.tasks.get(key);
	}

	@Nullable
	TriggerIndex.Ref ref(String key, int task) {
		TriggerIndex.Tasks t = this.tasks.get(key);
		return t != null && task >= 0 && task < t.all.length ? t.all[task] : null;
	}

	List<TriggerIndex.Ref> questTasks(String questKey) {
		return this.questTasks.getOrDefault(questKey, List.of());
	}

	List<TriggerIndex.Ref> chapterTasks(String chapterId) {
		return this.chapterTasks.getOrDefault(chapterId, List.of());
	}

	List<TriggerIndex.Ref> customs(String name) {
		return this.customs.getOrDefault(name, List.of());
	}

	List<String> requiredBy(String questKey) {
		return this.requiredBy.getOrDefault(questKey, List.of());
	}

	List<String> looseTrees(String chapterId) {
		return this.looseTrees.getOrDefault(chapterId, List.of());
	}

	void forget() {
		this.held.forget();
		this.crafted.forget();
		this.kills.forget();
		this.breaks.forget();
		this.places.forget();
	}

	static final class ByThing<T> {
		private final Function<T, net.minecraft.core.Holder<T>> holder;
		private final Map<ResourceLocation, List<TriggerIndex.Ref>> ids = new HashMap<>();
		private final Map<ResourceLocation, List<TriggerIndex.Ref>> tags = new HashMap<>();
		private final List<TriggerIndex.Ref> any = new ArrayList<>();
		private final IdentityHashMap<T, TriggerIndex.Ref[]> resolved = new IdentityHashMap<>();

		ByThing(Function<T, net.minecraft.core.Holder<T>> holder) {
			this.holder = holder;
		}

		void add(@Nullable Specs.Named named, TriggerIndex.Ref ref) {
			if (named == null) {
				this.any.add(ref);
			} else {
				(named.tag() ? this.tags : this.ids).computeIfAbsent(named.id(), k -> new ArrayList<>()).add(ref);
			}
		}

		boolean isEmpty() {
			return this.ids.isEmpty() && this.tags.isEmpty() && this.any.isEmpty();
		}

		TriggerIndex.Ref[] of(T thing) {
			TriggerIndex.Ref[] refs = this.resolved.get(thing);
			if (refs == null) {
				List<TriggerIndex.Ref> out = new ArrayList<>(this.any);
				Set<TriggerIndex.Ref> seen = new HashSet<>(out);
				net.minecraft.core.Holder<T> h = this.holder.apply(thing);
				List<TriggerIndex.Ref> byId = h.unwrapKey().map(k -> this.ids.get(k.location())).orElse(null);
				if (byId != null) {
					for (TriggerIndex.Ref r : byId) {
						if (seen.add(r)) {
							out.add(r);
						}
					}
				}

				if (!this.tags.isEmpty()) {
					h.tags().forEach(tag -> {
						List<TriggerIndex.Ref> byTag = this.tags.get(tag.location());
						if (byTag != null) {
							for (TriggerIndex.Ref rx : byTag) {
								if (seen.add(rx)) {
									out.add(rx);
								}
							}
						}
					});
				}

				refs = out.isEmpty() ? TriggerIndex.NONE : out.toArray(TriggerIndex.NONE);
				this.resolved.put(thing, refs);
			}

			return refs;
		}

		void forget() {
			this.resolved.clear();
		}
	}

	static final class Ref {
		final BookIndex.Entry entry;
		final String key;
		final int index;
		final Task task;
		@Nullable
		Specs.ItemSpec item;
		@Nullable
		Specs.Named thing;
		boolean block;
		@Nullable
		Stat<?> stat;
		@Nullable
		ResourceLocation id;
		@Nullable
		Specs.Named biome;
		@Nullable
		Specs.Named structure;

		Ref(BookIndex.Entry entry, int index) {
			this.entry = entry;
			this.key = entry.key();
			this.index = index;
			this.task = entry.quest().tasks.get(index);
		}
	}

	static final class Tasks {
		final TriggerIndex.Ref[] all;
		final TriggerIndex.Ref[] looked;
		final TriggerIndex.Ref[] observed;

		Tasks(TriggerIndex.Ref[] all, TriggerIndex.Ref[] looked, TriggerIndex.Ref[] observed) {
			this.all = all;
			this.looked = looked;
			this.observed = observed;
		}
	}
}
