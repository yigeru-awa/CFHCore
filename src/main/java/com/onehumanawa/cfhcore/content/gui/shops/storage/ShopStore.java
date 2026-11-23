package com.onehumanawa.cfhcore.content.gui.shops.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class ShopStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	@Nullable
	private final Path dir;
	private final Map<String, Shop> shops = new LinkedHashMap<>();
	private final Map<UUID, Wallet> wallets = new HashMap<>();
	private boolean dirty;
	private boolean unavailable;

	public ShopStore(@Nullable Path dir) {
		this.dir = dir;
	}

	public boolean unavailable() {
		return this.unavailable;
	}

	public Collection<Shop> shops() {
		return this.shops.values();
	}

	@Nullable
	public Shop shop(@Nullable String id) {
		return id == null ? null : this.shops.get(id.toLowerCase(Locale.ROOT));
	}

	public void put(Shop shop) {
		this.shops.put(shop.id, shop);
		this.touch();
	}

	public void remove(Shop shop) {
		this.shops.remove(shop.id);
		this.touch();
	}

	@Nullable
	public Shop shopOf(@Nullable UUID owner) {
		if (owner == null) {
			return null;
		} else {
			for (Shop s : this.shops.values()) {
				if (s.ownedBy(owner)) {
					return s;
				}
			}

			return null;
		}
	}

	public Map<UUID, Wallet> wallets() {
		return this.wallets;
	}

	@Nullable
	public Wallet wallet(@Nullable UUID id) {
		return id == null ? null : this.wallets.get(id);
	}

	@Nullable
	public UUID byName(String name) {
		for (Entry<UUID, Wallet> e : this.wallets.entrySet()) {
			if (e.getValue().name.equalsIgnoreCase(name)) {
				return e.getKey();
			}
		}

		return null;
	}

	public void touch() {
		this.dirty = true;
	}

	public boolean dirty() {
		return this.dirty;
	}

	public void load() {
		this.shops.clear();
		this.wallets.clear();
		if (this.dir != null) {
			ShopStore.ShopsFile sf = this.read(this.dir.resolve("shops.json"), ShopStore.ShopsFile.class);
			if (sf != null && sf.shops != null) {
				for (Shop s : sf.shops) {
					if (s != null) {
						s.sanitize();
						if (!s.id.isEmpty() && !this.shops.containsKey(s.id)) {
							this.shops.put(s.id, s);
						}
					}
				}
			}

			ShopStore.WalletsFile wf = this.read(this.dir.resolve("wallets.json"), ShopStore.WalletsFile.class);
			if (wf != null && wf.wallets != null) {
				for (Entry<String, Wallet> e : wf.wallets.entrySet()) {
					if (e.getValue() != null) {
						try {
							Wallet w = e.getValue();
							w.sanitize();
							this.wallets.put(UUID.fromString(e.getKey()), w);
						} catch (IllegalArgumentException var6) {
						}
					}
				}
			}

			this.dirty = false;
			CFHGuiShops.LOGGER.info("[CFHGui Shops] {} shops, {} wallets", this.shops.size(), this.wallets.size());
		}
	}

	@Nullable
	private <T> T read(Path file, Class<T> type) {
		String text;
		try {
			text = Files.readString(file, StandardCharsets.UTF_8);
		} catch (NoSuchFileException var6) {
			return null;
		} catch (IOException var7) {
			this.unavailable = true;
			CFHGuiShops.LOGGER
				.error("[CFHGui Shops] {} is there and cannot be read ({}): the shops stand still until the server starts again with it readable", file, var7.toString());
			return null;
		}

		try {
			T value = (T)GSON.fromJson(text, type);
			if (value != null) {
				return value;
			}
		} catch (JsonParseException var5) {
			CFHGuiShops.LOGGER.error("[CFHGui Shops] {} is spoilt: {}", file, var5.toString());
		}

		setAside(file);
		return null;
	}

	private static void setAside(Path file) {
		try {
			Path aside = file.resolveSibling(file.getFileName() + ".broken");

			for (int i = 2; Files.exists(aside); i++) {
				aside = file.resolveSibling(file.getFileName() + ".broken." + i);
			}

			Files.copy(file, aside);
			CFHGuiShops.LOGGER.error("[CFHGui Shops] kept as {}; going on without it", aside.getFileName());
		} catch (IOException var3) {
			CFHGuiShops.LOGGER.error("[CFHGui Shops] could not copy {} aside: {}", file, var3.toString());
		}
	}

	public void save(Saver saver) {
		if (this.dir != null && !this.unavailable && this.dirty) {
			ShopStore.ShopsFile sf = new ShopStore.ShopsFile();
			sf.shops.addAll(this.shops.values());
			ShopStore.WalletsFile wf = new ShopStore.WalletsFile();

			for (Entry<UUID, Wallet> e : this.wallets.entrySet()) {
				wf.wallets.put(e.getKey().toString(), e.getValue());
			}

			saver.text(this.dir.resolve("shops.json"), GSON.toJson(sf));
			saver.text(this.dir.resolve("wallets.json"), GSON.toJson(wf));
			this.dirty = false;
		}
	}

	private static final class ShopsFile {
		int version = 1;
		List<Shop> shops = new ArrayList<>();
	}

	private static final class WalletsFile {
		int version = 1;
		Map<String, Wallet> wallets = new LinkedHashMap<>();
	}
}
