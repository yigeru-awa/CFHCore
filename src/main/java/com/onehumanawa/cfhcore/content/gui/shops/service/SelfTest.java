package com.onehumanawa.cfhcore.content.gui.shops.service;

import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.shops.ShopNodes;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.model.ShopRules;
import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import com.onehumanawa.cfhcore.content.gui.shops.storage.Saver;
import com.onehumanawa.cfhcore.content.gui.shops.storage.ShopStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SelfTest {
	private int passed;
	private int failed;
	private final List<String> failures = new ArrayList<>();

	public static SelfTest.Outcome run(MinecraftServer server) {
		SelfTest t = new SelfTest();

		try {
			t.all(server);
		} catch (RuntimeException var4) {
			t.failed++;
			t.failures.add("threw " + var4);
			CFHGuiShops.LOGGER.error("[CFHGui Shops] selftest threw", var4);
		}

		for (String f : t.failures) {
			CFHGuiShops.LOGGER.warn("[CFHGui Shops] selftest failed: {}", f);
		}

		CFHGuiShops.LOGGER.info("[CFHGui Shops] selftest: {} passed, {} failed", t.passed, t.failed);
		return new SelfTest.Outcome(t.passed, t.failed);
	}

	private void check(String what, boolean ok) {
		if (ok) {
			this.passed++;
		} else {
			this.failed++;
			this.failures.add(what);
		}
	}

	private void code(String what, OpResult r, String expected) {
		this.check(what + " (" + r.code() + " " + r.args() + ", expected " + expected + ")", r.code().equals(expected));
	}

	private static Actor player(String name, Set<Node> may) {
		return player(name, may, NonNullList.withSize(36, ItemStack.EMPTY));
	}

	private static Actor player(String name, Set<Node> may, NonNullList<ItemStack> slots) {
		return new Actor(UUID.nameUUIDFromBytes(("cfhgui-shops-selftest:" + name).getBytes(StandardCharsets.UTF_8)), name, null, new Pocket(slots, () -> {
		}), may::contains);
	}

	private void all(MinecraftServer server) {
		ShopStore store = new ShopStore(null);
		ShopRules rules = new ShopRules();
		rules.startingBalance = 100L;
		long[] now = new long[]{1000000L};
		final List<String> notices = new ArrayList<>();
		ShopService s = new ShopService(store, rules, server.registryAccess(), () -> (long)(now[0]++), new ShopService.Listener() {
			@Override
			public void catalogueChanged() {
			}

			@Override
			public void selfChanged(UUID player) {
			}

			@Override
			public void notice(UUID player, String key, String... args) {
				notices.add(key);
			}
		});
		Set<Node> everyone = Set.of(ShopNodes.USE, ShopNodes.CREATE, ShopNodes.PAY);
		Set<Node> operator = Set.of(ShopNodes.USE, ShopNodes.CREATE, ShopNodes.PAY, ShopNodes.ADMIN, ShopNodes.MONEY);
		NonNullList<ItemStack> aliceSlots = NonNullList.withSize(36, ItemStack.EMPTY);
		aliceSlots.set(0, new ItemStack(Items.COBBLESTONE, 16));
		aliceSlots.set(1, new ItemStack(Items.COBBLESTONE, 48));
		Actor op = player("Opal", operator);
		Actor alice = player("Alice", everyone, aliceSlots);
		Actor bob = player("Bob", everyone);
		Actor carol = player("Carol", everyone);
		Actor dave = player("Dave", everyone);

		for (Actor a : List.of(op, alice, bob, carol, dave)) {
			s.joined(a.id(), a.name());
		}

		this.check("starting balance", s.balance(bob.id()) == 100L);
		this.code("a player makes no server shop", s.adminCreate(bob, "Bank"), "no_permission");
		OpResult made = s.adminCreate(op, "General Store");
		this.code("server shop", made, "ok");
		String store1 = made.arg(1);
		this.check("shop id from the name", "general-store".equals(store1));
		op.pocket().give(new ItemStack(Items.DIAMOND), 1L);
		this.code("server offer", s.offerAdd(op, store1, 0, 10L, 6L), "ok");
		Offer diamonds = store.shop(store1).offers.get(0);
		this.code("buy three diamonds", s.buy(bob, store1, diamonds.id, 3), "ok");
		this.check("paid 30", s.balance(bob.id()) == 70L);
		this.check("got three diamonds", bob.pocket().count(new ItemStack(Items.DIAMOND)) == 3L);
		this.code("sell two back", s.sell(bob, store1, diamonds.id, 2), "ok");
		this.check("was paid 12", s.balance(bob.id()) == 82L);
		this.check("one diamond left", bob.pocket().count(new ItemStack(Items.DIAMOND)) == 1L);
		this.code("sell more than carried", s.sell(bob, store1, diamonds.id, 2), "not_enough");
		this.check("nothing moved on a refused sale", s.balance(bob.id()) == 82L && bob.pocket().count(new ItemStack(Items.DIAMOND)) == 1L);
		this.code("too poor", s.buy(carol, store1, diamonds.id, 11), "poor");
		this.check("nothing moved on a refused buy", s.balance(carol.id()) == 100L && carol.pocket().count(new ItemStack(Items.DIAMOND)) == 0L);
		ItemStack named = new ItemStack(Items.IRON_SWORD);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Edge"));
		named.setDamageValue(5);
		ItemStack back = Stacks.read(Stacks.write(named, server.registryAccess()), server.registryAccess());
		this.check("an item with data survives as text", Stacks.same(named, back) && back.getDamageValue() == 5);
		this.check("a plain item is not the named one", !Stacks.same(named, new ItemStack(Items.IRON_SWORD)));
		this.code("open a shop", s.create(alice, "Alice's Cobble"), "ok");
		String shopA = store.shopOf(alice.id()).id;
		this.code("one shop each", s.create(alice, "Another"), "has_shop");
		ItemStack cobble = new ItemStack(Items.COBBLESTONE, 16);
		this.code("an empty slot is no offer", s.offerAdd(alice, shopA, 5, 5L, 3L), "empty_slot");
		this.code("an offer for nothing", s.offerAdd(alice, shopA, 0, 0L, 0L), "bad_price");
		this.code("offer of 16", s.offerAdd(alice, shopA, 0, 5L, 3L), "ok");
		Offer cob = store.shop(shopA).offers.get(0);
		this.check("the lot is the stack's count", s.template(cob).getCount() == 16);
		long carried = alice.pocket().count(cobble);
		this.code("stock in", s.stockAdd(alice, shopA, 0), "ok");
		this.check("the stock is what was carried", carried == 64L && cob.stock == 64L && alice.pocket().count(cobble) == 0L);
		this.code("nobody else stocks it", s.stockAdd(bob, shopA, 0), "not_yours");
		this.code("buy from own shop", s.buy(alice, shopA, cob.id, 1), "own_shop");
		long total0 = cob.stock + bob.pocket().count(cobble) + alice.pocket().count(cobble);
		long money0 = s.balance(alice.id()) + s.balance(bob.id());
		this.code("buy two lots", s.buy(bob, shopA, cob.id, 2), "ok");
		this.check("the owner earned it", s.balance(alice.id()) == 110L && s.balance(bob.id()) == 72L);
		this.check("the stock went down by 32", cob.stock == carried - 32L && bob.pocket().count(cobble) == 32L);
		this.check("the owner was told", notices.contains("shop_sold"));
		this.code("more than the stock", s.buy(bob, shopA, cob.id, 3), "out_of_stock");
		this.code("sell a lot to the shop", s.sell(bob, shopA, cob.id, 1), "ok");
		this.check("the shop paid from its owner", s.balance(alice.id()) == 107L && s.balance(bob.id()) == 75L && cob.stock == carried - 16L);
		this.check("no item made or lost", cob.stock + bob.pocket().count(cobble) + alice.pocket().count(cobble) == total0);
		this.check("no money made or lost", s.balance(alice.id()) + s.balance(bob.id()) == money0);
		NonNullList<ItemStack> full = NonNullList.withSize(36, ItemStack.EMPTY);

		for (int i = 0; i < full.size(); i++) {
			full.set(i, new ItemStack(Items.DIRT, 64));
		}

		Actor stuffed = new Actor(dave.id(), dave.name(), null, new Pocket(full, () -> {
		}), dave.may());
		long daveBefore = s.balance(dave.id());
		long stockBefore = cob.stock;
		this.code("no room", s.buy(stuffed, shopA, cob.id, 1), "no_room");
		this.check("nothing moved without room", s.balance(dave.id()) == daveBefore && cob.stock == stockBefore);
		this.code("operator sets a balance", s.money(op, "Alice", "set", 2L), "ok");
		this.code("shop cannot pay", s.sell(bob, shopA, cob.id, 1), "shop_poor");
		this.check("nothing moved when the shop could not pay", s.balance(bob.id()) == 75L && cob.stock == stockBefore && s.balance(alice.id()) == 2L);
		this.code("a player sets no balance", s.money(bob, "Bob", "set", 999L), "no_permission");
		long lotsLeft = cob.stock / 16L;

		for (int i = 0; (long)i < lotsLeft - 1L; i++) {
			this.code("buy down to the last lot", s.buy(carol, shopA, cob.id, 1), "ok");
		}

		OpResult first = s.buy(carol, shopA, cob.id, 1);
		OpResult second = s.buy(dave, shopA, cob.id, 1);
		this.check("the first of two gets the last lot", first.ok() && second.code().equals("out_of_stock") && cob.stock == 0L);
		this.check("the second paid nothing", s.balance(dave.id()) == daveBefore);
		this.code("no lots", s.sell(bob, shopA, cob.id, 0), "invalid");
		s.money(op, "Alice", "set", 100L);
		this.code("the shop buys again", s.sell(bob, shopA, cob.id, 1), "ok");
		this.code("an offer with stock stays", s.offerRemove(alice, shopA, cob.id), "has_stock");
		this.code("a shop with stock stays", s.delete(alice, shopA), "has_stock");
		long had = alice.pocket().count(cobble);
		this.code("stock out", s.stockTake(alice, shopA, cob.id, 0L), "ok");
		this.check("the stock came back", cob.stock == 0L && alice.pocket().count(cobble) == had + 16L);
		this.code("the offer goes", s.offerRemove(alice, shopA, cob.id), "ok");
		this.code("the shop goes", s.delete(alice, shopA), "ok");
		long bob0 = s.balance(bob.id());
		long carol0 = s.balance(carol.id());
		this.code("pay", s.pay(bob, "carol", 5L), "ok");
		this.check("paid", s.balance(bob.id()) == bob0 - 5L && s.balance(carol.id()) == carol0 + 5L);
		this.code("pay oneself", s.pay(bob, "Bob", 1L), "self");
		this.code("pay more than one has", s.pay(bob, "Carol", bob0 * 2L + 10L), "poor");
		this.code("pay nobody", s.pay(bob, "Nobody", 1L), "no_such_player");
		this.code("no exchange item", s.exchange(bob, "deposit", 1L), "exchange_off");
		rules.exchangeItem = "minecraft:emerald";
		rules.exchangeValue = 10L;
		bob.pocket().give(new ItemStack(Items.EMERALD), 5L);
		long bob1 = s.balance(bob.id());
		this.code("deposit", s.exchange(bob, "deposit", 0L), "ok");
		this.check("five emeralds are 50", s.balance(bob.id()) == bob1 + 50L && bob.pocket().count(new ItemStack(Items.EMERALD)) == 0L);
		this.code("withdraw", s.exchange(bob, "withdraw", 3L), "ok");
		this.check("three emeralds back", s.balance(bob.id()) == bob1 + 20L && bob.pocket().count(new ItemStack(Items.EMERALD)) == 3L);
		this.roundTrip(store);
	}

	private void roundTrip(ShopStore memory) {
		Path dir = null;

		try {
			dir = Files.createTempDirectory("cfhgui-shops-selftest");
			ShopStore disk = new ShopStore(dir);
			disk.load();

			for (Shop shop : memory.shops()) {
				disk.put(shop);
			}

			disk.wallets().putAll(memory.wallets());
			Saver saver = new Saver();
			disk.save(saver);
			this.check("files written", saver.await());
			saver.close();
			ShopStore again = new ShopStore(dir);
			again.load();
			this.check("shops read back", again.shops().size() == memory.shops().size() && !again.unavailable());
			boolean same = again.wallets().size() == memory.wallets().size();

			for (Entry<UUID, Wallet> e : memory.wallets().entrySet()) {
				Wallet w = again.wallet(e.getKey());
				same &= w != null && w.balance == e.getValue().balance && w.history.size() == e.getValue().history.size();
			}

			this.check("balances read back", same);
			Shop general = again.shop("general-store");
			this.check("offers read back", general != null && general.offers.size() == 1 && general.offers.get(0).sold == 3L);
		} catch (IOException var28) {
			this.check("a scratch folder for the round trip (" + var28 + ")", false);
		} finally {
			if (dir != null) {
				try (Stream<Path> walk = Files.walk(dir)) {
					for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
						Files.deleteIfExists(p);
					}
				} catch (IOException var27) {
				}
			}
		}
	}

	public static record Outcome(int passed, int failed) {
	}
}
