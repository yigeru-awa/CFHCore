package com.onehumanawa.cfhcore.content.gui.shops.client;

import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ShopsActions {
	private static int next = 1;
	private static final Map<Integer, Consumer<ShopsNet.Result>> WAITING = new HashMap<>();

	public static void send(String op, CompoundTag args, @Nullable Consumer<ShopsNet.Result> then) {
		if (!ShopsState.available()) {
			if (then != null) {
				then.accept(new ShopsNet.Result(0, false, "unavailable", List.of()));
			}
		} else {
			int id = next++;
			if (then != null) {
				WAITING.put(id, then);
			}

			CFHGuiNetwork.get().sendToServer(new ShopsNet.Action(id, op, args));
		}
	}

	public static void onResult(ShopsNet.Result r) {
		Consumer<ShopsNet.Result> then = WAITING.remove(r.request());
		if (then != null) {
			then.accept(r);
		}
	}

	public static void clear() {
		WAITING.clear();
	}

	public static Component describe(ShopsNet.Result r) {
		return Component.translatable("cfhgui_shops.result." + r.code(), r.args().toArray());
	}

	public static void buy(String shop, int offer, int lots, Consumer<ShopsNet.Result> then) {
		send("buy", trade(shop, offer, lots), then);
	}

	public static void sell(String shop, int offer, int lots, Consumer<ShopsNet.Result> then) {
		send("sell", trade(shop, offer, lots), then);
	}

	private static CompoundTag trade(String shop, int offer, int lots) {
		CompoundTag a = shopArgs(shop, offer);
		a.putInt("lots", lots);
		return a;
	}

	public static void create(String name, boolean server, Consumer<ShopsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("name", name);
		send(server ? "admin_create" : "create", a, then);
	}

	public static void edit(String shop, @Nullable String name, @Nullable String description, @Nullable Boolean open, Consumer<ShopsNet.Result> then) {
		CompoundTag a = shopArgs(shop, 0);
		if (name != null) {
			a.putString("name", name);
		}

		if (description != null) {
			a.putString("description", description);
		}

		if (open != null) {
			a.putBoolean("open", open);
		}

		send("edit", a, then);
	}

	public static void delete(String shop, Consumer<ShopsNet.Result> then) {
		send("delete", shopArgs(shop, 0), then);
	}

	public static void offerAdd(String shop, int slot, long price, long sellPrice, Consumer<ShopsNet.Result> then) {
		CompoundTag a = shopArgs(shop, 0);
		a.putInt("slot", slot);
		a.putLong("price", price);
		a.putLong("sell_price", sellPrice);
		send("offer_add", a, then);
	}

	public static void offerEdit(String shop, int offer, long price, long sellPrice, Consumer<ShopsNet.Result> then) {
		CompoundTag a = shopArgs(shop, offer);
		a.putLong("price", price);
		a.putLong("sell_price", sellPrice);
		send("offer_edit", a, then);
	}

	public static void offerRemove(String shop, int offer, Consumer<ShopsNet.Result> then) {
		send("offer_remove", shopArgs(shop, offer), then);
	}

	public static void stockAdd(String shop, int offer, Consumer<ShopsNet.Result> then) {
		send("stock_add", shopArgs(shop, offer), then);
	}

	public static void stockTake(String shop, int offer, long count, Consumer<ShopsNet.Result> then) {
		CompoundTag a = shopArgs(shop, offer);
		a.putLong("count", count);
		send("stock_take", a, then);
	}

	public static void pay(String player, long amount, Consumer<ShopsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("player", player);
		a.putLong("amount", amount);
		send("pay", a, then);
	}

	public static void money(String player, String what, long amount, Consumer<ShopsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("player", player);
		a.putString("what", what);
		a.putLong("amount", amount);
		send("money", a, then);
	}

	public static void exchange(String what, long count, Consumer<ShopsNet.Result> then) {
		CompoundTag a = new CompoundTag();
		a.putString("what", what);
		a.putLong("count", count);
		send("exchange", a, then);
	}

	public static void sync() {
		send("sync", new CompoundTag(), null);
	}

	private static CompoundTag shopArgs(String shop, int offer) {
		CompoundTag a = new CompoundTag();
		a.putString("shop", shop);
		if (offer > 0) {
			a.putInt("offer", offer);
		}

		return a;
	}

	private ShopsActions() {
	}
}
