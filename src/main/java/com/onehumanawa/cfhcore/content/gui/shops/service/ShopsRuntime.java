package com.onehumanawa.cfhcore.content.gui.shops.service;

import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.shops.ShopNodes;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.model.Catalogue;
import com.onehumanawa.cfhcore.content.gui.shops.model.Hello;
import com.onehumanawa.cfhcore.content.gui.shops.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.model.ShopRules;
import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import com.onehumanawa.cfhcore.content.gui.shops.storage.Saver;
import com.onehumanawa.cfhcore.content.gui.shops.storage.ShopStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class ShopsRuntime {
	private static final int MAX_PER_SECOND = 40;
	private static final int CATALOGUE_EVERY = 10;
	private static final int HELLO_EVERY = 100;
	@Nullable
	private static JsonConfig<ShopRules> rulesFile;
	@Nullable
	private static MinecraftServer server;
	@Nullable
	private static ShopService service;
	@Nullable
	private static Saver saver;
	private static long catalogueVersion;
	@Nullable
	private static ShopsNet.State catalogueState;
	private static long catalogueStateVersion = -1L;
	private static boolean catalogueTooBig;
	private static final Map<UUID, Long> catalogueSent = new HashMap<>();
	private static final Set<UUID> selfDirty = new HashSet<>();
	private static final Map<UUID, List<String>> helloSent = new HashMap<>();
	private static final Map<UUID, long[]> rate = new HashMap<>();
	private static int ticks;
	private static final ShopService.Listener LISTENER = new ShopService.Listener() {
		@Override
		public void catalogueChanged() {
			ShopsRuntime.catalogueVersion++;
		}

		@Override
		public void selfChanged(UUID player) {
			if (player != null) {
				ShopsRuntime.selfDirty.add(player);
			}
		}

		@Override
		public void notice(UUID player, String key, String... args) {
			MinecraftServer s = ShopsRuntime.server;
			ServerPlayer p = s != null && player != null ? s.getPlayerList().getPlayer(player) : null;
			if (p != null) {
				if (ShopsNet.has(p)) {
					ShopsNet.send(p, new ShopsNet.Notice(key, List.of(args)));
				} else {
					p.sendSystemMessage(Msg.tr("cfhgui_shops.notice." + key + ".body", (Object[])args));
				}
			}
		}
	};

	public static void init() {
		CFHGuiEvents.SERVER_STARTED.register(ShopsRuntime::start);
		CFHGuiEvents.SERVER_STOPPING.register(ShopsRuntime::stop);
		CFHGuiEvents.SERVER_TICK_END.register(ShopsRuntime::tick);
		CFHGuiEvents.PLAYER_JOINED.register(ShopsRuntime::joined);
		CFHGuiEvents.PLAYER_LEFT.register(ShopsRuntime::left);
	}

	@Nullable
	public static ShopService service() {
		return service;
	}

	public static synchronized JsonConfig<ShopRules> rulesFile() {
		if (rulesFile == null) {
			rulesFile = JsonConfig.of("shops", ShopRules.class, ShopRules::new);
		}

		return rulesFile;
	}

	private static ShopRules readRules() {
		JsonConfig<ShopRules> file = rulesFile();
		file.load();
		ShopRules rules = (ShopRules)file.get();
		rules.sanitize();
		return rules;
	}

	private static void start(MinecraftServer s) {
		server = s;
		Path dir = s.getWorldPath(LevelResource.ROOT).resolve("slate").resolve("shops");
		ShopStore store = new ShopStore(dir);
		store.load();
		saver = new Saver();
		service = new ShopService(store, readRules(), s.registryAccess(), System::currentTimeMillis, LISTENER);
		catalogueVersion++;
		catalogueTooBig = false;
		catalogueSent.clear();
		selfDirty.clear();
		helloSent.clear();
		rate.clear();
	}

	private static void stop(MinecraftServer s) {
		ShopService svc = service;
		Saver sv = saver;
		if (svc != null && sv != null) {
			svc.store().save(sv);
			sv.close();
		}

		service = null;
		saver = null;
		server = null;
		catalogueState = null;
	}

	public static void worldSaved() {
		save();
	}

	private static void save() {
		ShopService svc = service;
		Saver sv = saver;
		if (svc != null && sv != null) {
			svc.store().save(sv);
		}
	}

	public static OpResult reload() {
		ShopService svc = service;
		if (svc == null) {
			return OpResult.fail("unavailable");
		} else {
			svc.rules(readRules());
			helloSent.clear();
			sendHellos();
			catalogueVersion++;
			return OpResult.DONE;
		}
	}

	private static void joined(ServerPlayer player) {
		ShopService svc = service;
		if (svc != null) {
			svc.joined(player.getUUID(), player.getGameProfile().getName());
			if (ShopsNet.has(player)) {
				sendHello(player, true);
				sendCatalogue(player);
				sendSelf(player);
			}
		}
	}

	private static void left(ServerPlayer player) {
		ShopService svc = service;
		if (svc != null && svc.store().dirty()) {
			save();
		}

		catalogueSent.remove(player.getUUID());
		helloSent.remove(player.getUUID());
		selfDirty.remove(player.getUUID());
		rate.remove(player.getUUID());
	}

	private static void tick(MinecraftServer s) {
		if (service != null) {
			ticks++;
			if (ticks % 10 == 0) {
				for (ServerPlayer p : s.getPlayerList().getPlayers()) {
					Long seen = catalogueSent.get(p.getUUID());
					if ((seen == null || seen < catalogueVersion) && ShopsNet.has(p)) {
						sendCatalogue(p);
					}
				}
			}

			if (!selfDirty.isEmpty()) {
				for (UUID id : List.copyOf(selfDirty)) {
					ServerPlayer px = s.getPlayerList().getPlayer(id);
					if (px != null && ShopsNet.has(px)) {
						sendSelf(px);
					}
				}

				selfDirty.clear();
			}

			if (ticks % 100 == 0) {
				sendHellos();
			}
		}
	}

	private static void sendHellos() {
		MinecraftServer s = server;
		if (s != null) {
			for (ServerPlayer p : s.getPlayerList().getPlayers()) {
				if (ShopsNet.has(p)) {
					sendHello(p, false);
				}
			}
		}
	}

	private static void sendHello(ServerPlayer player, boolean always) {
		ShopService svc = service;
		if (svc != null) {
			Hello h = hello(svc.rules(), Actor.of(player));
			List<String> was = helloSent.get(player.getUUID());
			if (always || !h.may.equals(was)) {
				helloSent.put(player.getUUID(), h.may);
				ShopsNet.send(player, ShopsNet.State.of("hello", h));
			}
		}
	}

	static Hello hello(ShopRules rules, Actor a) {
		Hello h = new Hello();
		h.currencyName = rules.currencyName;
		h.currencyPlural = rules.currencyPlural;
		h.currencySymbol = rules.currencySymbol;
		h.playerShops = rules.playerShops;
		h.maxOffers = rules.maxOffers;
		h.exchangeItem = Stacks.plain(rules.exchangeItem).isEmpty() ? "" : rules.exchangeItem;
		h.exchangeValue = rules.exchangeValue;
		if (a.may(ShopNodes.USE)) {
			h.may.add("use");
		}

		if (a.may(ShopNodes.CREATE)) {
			h.may.add("create");
		}

		if (a.may(ShopNodes.PAY)) {
			h.may.add("pay");
		}

		if (a.may(ShopNodes.ADMIN)) {
			h.may.add("admin");
		}

		if (a.may(ShopNodes.MONEY)) {
			h.may.add("money");
		}

		return h;
	}

	private static void sendCatalogue(ServerPlayer player) {
		ShopService svc = service;
		if (svc != null) {
			if (catalogueState == null || catalogueStateVersion != catalogueVersion) {
				Catalogue c = new Catalogue();
				c.shops = new ArrayList<>(svc.store().shops());
				catalogueState = ShopsNet.State.of("catalogue", c);
				catalogueStateVersion = catalogueVersion;
				if (catalogueState.data().length > 1048576 && !catalogueTooBig) {
					catalogueTooBig = true;
					CFHGuiShops.LOGGER
						.warn(
							"[CFHGui Shops] the shops come to {} bytes packed, more than one payload carries ({}): the screens show them as they last were; the commands see them all",
							catalogueState.data().length,
							1048576
						);
				}
			}

			catalogueSent.put(player.getUUID(), catalogueVersion);
			if (catalogueState.data().length <= 1048576) {
				ShopsNet.send(player, catalogueState);
			}
		}
	}

	private static void sendSelf(ServerPlayer player) {
		ShopService svc = service;
		if (svc != null) {
			SelfState self = new SelfState();
			Wallet w = svc.store().wallet(player.getUUID());
			if (w != null) {
				self.balance = w.balance;
				self.history = w.history;
			}

			Shop own = svc.store().shopOf(player.getUUID());
			self.shop = own == null ? "" : own.id;
			ShopsNet.send(player, ShopsNet.State.of("self", self));
		}
	}

	public static void walletChanged(UUID player) {
		if (player != null && service != null) {
			selfDirty.add(player);
		}
	}

	public static void open(ServerPlayer player, String what, String arg) {
		ShopsNet.send(player, new ShopsNet.Open(what, arg));
	}

	public static void onAction(ServerPlayer player, ShopsNet.Action action) {
		ShopService svc = service;
		OpResult result;
		if (svc == null) {
			result = OpResult.fail("unavailable");
		} else if (tooFast(player)) {
			result = OpResult.fail("too_fast");
		} else {
			result = run(svc, Actor.of(player), action.op(), action.args(), player);
		}

		if (ShopsNet.has(player)) {
			Long seen = catalogueSent.get(player.getUUID());
			if (seen == null || seen < catalogueVersion) {
				sendCatalogue(player);
			}

			if (selfDirty.remove(player.getUUID())) {
				sendSelf(player);
			}
		}

		ShopsNet.send(player, result.payload(action.request()));
	}

	private static boolean tooFast(ServerPlayer player) {
		long second = System.currentTimeMillis() / 1000L;
		long[] r = rate.computeIfAbsent(player.getUUID(), k -> new long[2]);
		if (r[0] != second) {
			r[0] = second;
			r[1] = 0L;
		}

		return ++r[1] > 40L;
	}

	private static OpResult run(ShopService s, Actor a, String op, CompoundTag args, ServerPlayer player) {
		String shop = args.getString("shop");
		int offer = args.getInt("offer");

		return switch (op) {
			case "create" -> s.create(a, args.getString("name"));
			case "admin_create" -> s.adminCreate(a, args.getString("name"));
			case "edit" -> s.edit(
			a,
			shop,
			args.contains("name", 8) ? args.getString("name") : null,
			args.contains("description", 8) ? args.getString("description") : null,
			args.contains("open", 1) ? args.getBoolean("open") : null
		);
			case "delete" -> s.delete(a, shop);
			case "offer_add" -> s.offerAdd(a, shop, args.getInt("slot"), args.getLong("price"), args.getLong("sell_price"));
			case "offer_edit" -> s.offerEdit(a, shop, offer, args.getLong("price"), args.getLong("sell_price"));
			case "offer_remove" -> s.offerRemove(a, shop, offer);
			case "stock_add" -> s.stockAdd(a, shop, offer);
			case "stock_take" -> s.stockTake(a, shop, offer, args.getLong("count"));
			case "buy" -> s.buy(a, shop, offer, args.getInt("lots"));
			case "sell" -> s.sell(a, shop, offer, args.getInt("lots"));
			case "pay" -> s.pay(a, args.getString("player"), args.getLong("amount"));
			case "money" -> s.money(a, args.getString("player"), args.getString("what"), args.getLong("amount"));
			case "exchange" -> s.exchange(a, args.getString("what"), args.getLong("count"));
			case "sync" -> {
				catalogueSent.remove(player.getUUID());
				selfDirty.add(player.getUUID());
				sendHello(player, true);
				yield OpResult.DONE;
			}
			default -> OpResult.fail("unknown");
		};
	}

	private ShopsRuntime() {
	}
}
