package com.onehumanawa.cfhcore.content.gui.shops.api;

import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import com.onehumanawa.cfhcore.content.gui.shops.service.ShopService;
import com.onehumanawa.cfhcore.content.gui.shops.service.ShopsRuntime;
import java.util.UUID;

public final class ShopsApi {
	public static boolean available() {
		ShopService s = ShopsRuntime.service();
		return s != null && !s.store().unavailable();
	}

	public static long balance(UUID player) {
		ShopService s = ShopsRuntime.service();
		return s == null ? 0L : s.balance(player);
	}

	public static boolean give(UUID player, long amount, String from) {
		return amount >= 1L && change(player, amount, from);
	}

	public static boolean take(UUID player, long amount, String to) {
		return amount >= 1L && change(player, -amount, to);
	}

	public static String format(long amount) {
		ShopService s = ShopsRuntime.service();
		return s == null ? Long.toString(amount) : s.rules().format(amount);
	}

	private static boolean change(UUID player, long amount, String who) {
		ShopService s = ShopsRuntime.service();
		if (s != null && !s.store().unavailable()) {
			Wallet w = s.store().wallet(player);
			if (w == null) {
				return false;
			} else {
				long after = w.balance + amount;
				if (after >= 0L && after <= 1000000000000L) {
					w.balance = after;
					w.note("other", amount, "", who == null ? "" : who, System.currentTimeMillis());
					s.store().touch();
					ShopsRuntime.walletChanged(player);
					return true;
				} else {
					return false;
				}
			}
		} else {
			return false;
		}
	}

	private ShopsApi() {
	}
}
