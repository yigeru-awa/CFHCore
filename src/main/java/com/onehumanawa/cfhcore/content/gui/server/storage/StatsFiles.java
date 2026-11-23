package com.onehumanawa.cfhcore.content.gui.server.storage;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.onehumanawa.cfhcore.content.gui.server.CFHGuiServer;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;

public final class StatsFiles {
	private final Map<UUID, Object2LongMap<String>> stats = new ConcurrentHashMap<>();
	private final Map<UUID, Long> stamps = new HashMap<>();
	private final Map<String, String> keys = new ConcurrentHashMap<>();
	private final AtomicBoolean reading = new AtomicBoolean();
	private volatile long readAt;
	private final AtomicInteger generation = new AtomicInteger();

	public long readAt() {
		return this.readAt;
	}

	public int generation() {
		return this.generation.get();
	}

	@Nullable
	public Object2LongMap<String> of(UUID id) {
		return this.stats.get(id);
	}

	public Set<UUID> players() {
		return this.stats.keySet();
	}

	public void put(UUID id, Object2LongMap<String> live) {
		this.stats.put(id, live);
		this.generation.incrementAndGet();
	}

	public String key(String key) {
		String old = this.keys.putIfAbsent(key, key);
		return old == null ? key : old;
	}

	public void refresh(@Nullable Path dir, Saver saver, long now) {
		if (dir != null && this.reading.compareAndSet(false, true)) {
			saver.run(() -> {
				try {
					int read = 0;
					if (Files.isDirectory(dir)) {
						try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
							for (Path file : files) {
								String name = file.getFileName().toString();

								UUID id;
								try {
									id = UUID.fromString(name.substring(0, name.length() - 5));
								} catch (IllegalArgumentException var23) {
									continue;
								}

								long stamp;
								try {
									stamp = Files.getLastModifiedTime(file).toMillis();
								} catch (IOException var22) {
									continue;
								}

								Long old = this.stamps.get(id);
								if (old == null || old != stamp) {
									Object2LongMap<String> map = this.read(file);
									if (map != null) {
										this.stamps.put(id, stamp);
										this.stats.put(id, map);
										read++;
									}
								}
							}
						} catch (IOException var25) {
							CFHGuiServer.LOGGER.warn("[CFHGui Server] the statistics folder could not be listed: {}", var25.toString());
						}
					}

					if (read > 0) {
						this.generation.incrementAndGet();
					}

					this.readAt = now;
				} finally {
					this.reading.set(false);
				}
			});
		}
	}

	@Nullable
	private Object2LongMap<String> read(Path file) {
		Object2LongOpenHashMap<String> out = new Object2LongOpenHashMap();

		try {
			Object2LongOpenHashMap var14;
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				JsonReader json = new JsonReader(r);

				try {
					json.beginObject();

					while (json.hasNext()) {
						if (json.nextName().equals("stats") && json.peek() == JsonToken.BEGIN_OBJECT) {
							json.beginObject();

							while (json.hasNext()) {
								String type = json.nextName();
								if (json.peek() != JsonToken.BEGIN_OBJECT) {
									json.skipValue();
								} else {
									json.beginObject();

									while (json.hasNext()) {
										String value = json.nextName();
										if (json.peek() != JsonToken.NUMBER) {
											json.skipValue();
										} else {
											long n = json.nextLong();
											if (n != 0L) {
												out.put(this.key(type + "|" + value), n);
											}
										}
									}

									json.endObject();
								}
							}

							json.endObject();
						} else {
							json.skipValue();
						}
					}

					json.endObject();
					out.trim();
					var14 = out;
				} catch (Throwable var11) {
					try {
						json.close();
					} catch (Throwable var10) {
						var11.addSuppressed(var10);
					}

					throw var11;
				}

				json.close();
			}

			return var14;
		} catch (RuntimeException | IOException var13) {
			return null;
		}
	}
}
