package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

final class HolderStore {
	private static final long SAVE_AFTER_MS = 5000L;
	private static final int PER_TICK = 4;
	@Nullable
	private final Path dir;
	private final Map<UUID, Holder> loaded = new HashMap<>();
	private final ArrayDeque<Holder> queue = new ArrayDeque<>();
	@Nullable
	private final ExecutorService io;
	private final Map<UUID, Future<?>> writing = new HashMap<>();
	private int ticks;

	HolderStore(@Nullable Path dir) {
		this.dir = dir;
		this.io = dir == null ? null : Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "CFHGui Quests IO");
			t.setDaemon(true);
			return t;
		});
	}

	Holder get(UUID id) {
		Holder h = this.loaded.get(id);
		if (h == null) {
			h = this.read(id);
			this.loaded.put(id, h);
		}

		return h;
	}

	@Nullable
	Holder peek(UUID id) {
		return this.loaded.get(id);
	}

	Collection<Holder> loaded() {
		return this.loaded.values();
	}

	void touch(Holder h, long now) {
		h.dirty = true;
		if (!h.queued) {
			h.queued = true;
			h.dirtyAt = now;
			this.queue.add(h);
		}
	}

	void tick(long now) {
		for (int n = 0; n < 4 && !this.queue.isEmpty() && now - this.queue.peek().dirtyAt >= 5000L; n++) {
			Holder h = this.queue.poll();
			h.queued = false;
			if (h.dirty && this.loaded.get(h.id) == h) {
				this.save(h);
			}
		}

		if (++this.ticks % 1200 == 0) {
			this.writing.values().removeIf(Future::isDone);
		}
	}

	void release(Holder h) {
		if (h.seats.isEmpty() && this.loaded.get(h.id) == h) {
			if (h.dirty) {
				this.save(h);
			}

			if (h.queued) {
				this.queue.remove(h);
				h.queued = false;
			}

			this.loaded.remove(h.id);
		}
	}

	void delete(UUID id) {
		Holder h = this.loaded.remove(id);
		if (h != null) {
			h.dirty = false;
			if (h.queued) {
				this.queue.remove(h);
				h.queued = false;
			}
		}

		if (this.dir != null && this.io != null) {
			Path file = this.file(id);
			this.writing.put(id, this.io.submit(() -> {
				try {
					Files.deleteIfExists(file);
				} catch (IOException var2x) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] {} could not be deleted: {}", file, var2x.toString());
				}
			}));
		}
	}

	void rename(UnaryOperator<String> to) {
		for (Holder h : this.loaded.values()) {
			if (rename(h.quests, to)) {
				this.dirtyNow(h);
			}
		}

		if (this.dir != null && Files.isDirectory(this.dir)) {
			this.drain();
			List<Path> files = new ArrayList<>();

			try (Stream<Path> s = Files.list(this.dir)) {
				s.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(files::add);
			} catch (IOException var12) {
				CFHGuiQuests.LOGGER.error("[CFHGui Quests] {} cannot be listed: {}", this.dir, var12.toString());
			}

			for (Path file : files) {
				String name = file.getFileName().toString();
				UUID id = uuid(name.substring(0, name.length() - ".json".length()));
				if (id != null && !this.loaded.containsKey(id)) {
					HolderStore.Saved saved = readFile(file);
					if (saved != null && saved.quests != null && rename(saved.quests, to)) {
						try {
							write(file, BookJson.GSON.toJson(saved));
						} catch (IOException var9) {
							CFHGuiQuests.LOGGER.error("[CFHGui Quests] {} could not be rewritten after a rename: {}", file, var9.toString());
						}
					}
				}
			}
		}
	}

	private static boolean rename(Map<String, QuestProgress> quests, UnaryOperator<String> to) {
		Map<String, QuestProgress> moved = null;
		Iterator<Entry<String, QuestProgress>> it = quests.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, QuestProgress> e = it.next();
			String key = to.apply(e.getKey());
			if (key != null && !key.equals(e.getKey())) {
				if (moved == null) {
					moved = new HashMap<>();
				}

				moved.put(key, e.getValue());
				it.remove();
			}
		}

		if (moved == null) {
			return false;
		} else {
			for (Entry<String, QuestProgress> e : moved.entrySet()) {
				quests.putIfAbsent(e.getKey(), e.getValue());
			}

			return true;
		}
	}

	private void dirtyNow(Holder h) {
		h.dirty = true;
		if (!h.queued) {
			h.queued = true;
			h.dirtyAt = 0L;
			this.queue.addFirst(h);
		}
	}

	void flush() {
		for (Holder h : this.loaded.values()) {
			if (h.dirty) {
				this.save(h);
			}
		}

		for (Holder hx : this.queue) {
			hx.queued = false;
		}

		this.queue.clear();
		this.drain();
	}

	void close() {
		this.flush();
		if (this.io != null) {
			this.io.shutdown();

			try {
				if (!this.io.awaitTermination(30L, TimeUnit.SECONDS)) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] progress was still being written when the server stopped");
				}
			} catch (InterruptedException var2) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void drain() {
		for (Future<?> f : this.writing.values()) {
			await(f);
		}

		this.writing.clear();
	}

	private static void await(Future<?> f) {
		try {
			f.get(30L, TimeUnit.SECONDS);
		} catch (InterruptedException var2) {
			Thread.currentThread().interrupt();
		} catch (Exception var3) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] a write of progress did not finish: {}", var3.toString());
		}
	}

	private Path file(UUID id) {
		return this.dir.resolve(id + ".json");
	}

	private void save(Holder h) {
		h.dirty = false;
		if (this.dir != null && this.io != null) {
			HolderStore.Saved saved = new HolderStore.Saved();
			saved.id = h.id.toString();
			saved.members = new ArrayList<>(h.members);

			for (Entry<String, QuestProgress> e : h.quests.entrySet()) {
				if (e.getValue().any()) {
					saved.quests.put(e.getKey(), e.getValue());
				}
			}

			saved.counters.putAll(h.counters);
			saved.emergencyAt = h.emergencyAt;
			String json = BookJson.GSON.toJson(saved);
			Path file = this.file(h.id);
			this.writing.put(h.id, this.io.submit(() -> {
				try {
					write(file, json);
				} catch (IOException var3) {
					CFHGuiQuests.LOGGER.error("[CFHGui Quests] progress could not be written to {}: {}", file, var3.toString());
				}
			}));
		}
	}

	private Holder read(UUID id) {
		Holder h = new Holder(id);
		if (this.dir == null) {
			return h;
		} else {
			Future<?> pending = this.writing.remove(id);
			if (pending != null) {
				await(pending);
			}

			Path file = this.file(id);
			if (!Files.isRegularFile(file)) {
				return h;
			} else {
				HolderStore.Saved saved = readFile(file);
				if (saved == null) {
					return h;
				} else {
					if (saved.members != null) {
						for (String m : saved.members) {
							if (m != null) {
								h.members.add(m);
							}
						}
					}

					if (saved.quests != null) {
						for (Entry<String, QuestProgress> e : saved.quests.entrySet()) {
							if (e.getKey() != null && e.getValue() != null) {
								h.quests.put(e.getKey(), e.getValue().sanitize());
							}
						}
					}

					if (saved.counters != null) {
						for (Entry<String, Long> ex : saved.counters.entrySet()) {
							if (ex.getKey() != null && ex.getValue() != null) {
								h.counters.put(ex.getKey(), ex.getValue());
							}
						}
					}

					h.emergencyAt = Math.max(0L, saved.emergencyAt);
					return h;
				}
			}
		}
	}

	@Nullable
	private static HolderStore.Saved readFile(Path file) {
		try {
			HolderStore.Saved var2;
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				var2 = (HolderStore.Saved)BookJson.GSON.fromJson(r, HolderStore.Saved.class);
			}

			return var2;
		} catch (Exception var7) {
			CFHGuiQuests.LOGGER.error("[CFHGui Quests] {} cannot be read ({}); it is kept as .broken and the holder starts over", file, var7.toString());

			try {
				Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException var4) {
			}

			return null;
		}
	}

	private static void write(Path file, String json) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, json, StandardCharsets.UTF_8);

		try {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException var4) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Nullable
	private static UUID uuid(String text) {
		try {
			return UUID.fromString(text);
		} catch (IllegalArgumentException var2) {
			return null;
		}
	}

	private static final class Saved {
		String id = "";
		List<String> members = new ArrayList<>();
		Map<String, QuestProgress> quests = new LinkedHashMap<>();
		Map<String, Long> counters = new LinkedHashMap<>();
		long emergencyAt;
	}
}
