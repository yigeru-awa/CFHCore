package com.onehumanawa.cfhcore.content.gui.shops.service;

import com.onehumanawa.cfhcore.content.gui.shops.ShopNodes;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.model.ShopRules;
import com.onehumanawa.cfhcore.content.gui.shops.model.Wallet;
import com.onehumanawa.cfhcore.content.gui.shops.storage.ShopStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ShopService {
	public static final int MAX_LOTS = 64;
	public static final int NAME_MAX = 32;
	public static final int DESCRIPTION_MAX = 120;
	private final ShopStore store;
	private final Provider registries;
	private final LongSupplier clock;
	private final ShopService.Listener listener;
	private ShopRules rules;
	private final Map<String, ItemStack> templates = new HashMap<>();

	public ShopService(ShopStore store, ShopRules rules, Provider registries, LongSupplier clock, ShopService.Listener listener) {
		this.store = store;
		this.rules = rules;
		this.registries = registries;
		this.clock = clock;
		this.listener = listener;
	}

	public ShopStore store() {
		return this.store;
	}

	public ShopRules rules() {
		return this.rules;
	}

	public void rules(ShopRules rules) {
		this.rules = rules;
	}

	public Provider registries() {
		return this.registries;
	}

	public ItemStack template(Offer offer) {
		if (this.templates.size() > 4096) {
			this.templates.clear();
		}

		return this.templates.computeIfAbsent(offer.item, s -> Stacks.read(s, this.registries));
	}

	public Wallet joined(UUID id, String name) {
		Wallet w = this.store.wallet(id);
		if (w == null) {
			w = new Wallet();
			w.name = name;
			w.balance = this.rules.startingBalance;
			this.store.wallets().put(id, w);
			this.store.touch();
		} else if (!w.name.equals(name)) {
			w.name = name;
			this.store.touch();
		}

		Shop own = this.store.shopOf(id);
		if (own != null && !own.ownerName.equals(name)) {
			own.ownerName = name;
			this.store.touch();
			this.listener.catalogueChanged();
		}

		return w;
	}

	public long balance(@Nullable UUID id) {
		Wallet w = this.store.wallet(id);
		return w == null ? 0L : w.balance;
	}

	@Nullable
	private Wallet walletOf(Actor a) {
		if (a.id() == null) {
			return null;
		} else {
			Wallet w = this.store.wallet(a.id());
			if (w == null) {
				w = this.joined(a.id(), a.name());
			}

			return w;
		}
	}

	public OpResult create(Actor a, String name) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (!a.may(ShopNodes.CREATE)) {
			return OpResult.fail("no_permission");
		} else if (!this.rules.playerShops) {
			return OpResult.fail("disabled");
		} else if (a.id() == null) {
			return OpResult.fail("player_only");
		} else {
			Shop had = this.store.shopOf(a.id());
			if (had != null) {
				return OpResult.fail("has_shop", had.name);
			} else {
				String clean = cleanName(name);
				if (clean == null) {
					return OpResult.fail("bad_name", 32);
				} else {
					Shop shop = this.newShop(clean);
					shop.owner = a.id();
					shop.ownerName = a.name();
					this.store.put(shop);
					this.listener.catalogueChanged();
					this.listener.selfChanged(a.id());
					return OpResult.ok(shop.name, shop.id);
				}
			}
		}
	}

	public OpResult adminCreate(Actor a, String name) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (!a.may(ShopNodes.ADMIN)) {
			return OpResult.fail("no_permission");
		} else {
			String clean = cleanName(name);
			if (clean == null) {
				return OpResult.fail("bad_name", 32);
			} else {
				Shop shop = this.newShop(clean);
				shop.server = true;
				this.store.put(shop);
				this.listener.catalogueChanged();
				return OpResult.ok(shop.name, shop.id);
			}
		}
	}

	private Shop newShop(String name) {
		Shop shop = new Shop();
		shop.id = this.freeId(name);
		shop.name = name;
		shop.created = this.clock.getAsLong();
		return shop;
	}

	public OpResult edit(Actor a, String shopId, @Nullable String name, @Nullable String description, @Nullable Boolean open) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayEdit(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			String clean = null;
			if (name != null && (clean = cleanName(name)) == null) {
				return OpResult.fail("bad_name", 32);
			} else {
				if (clean != null) {
					shop.name = clean;
				}

				if (description != null) {
					shop.description = cleanText(description, 120);
				}

				if (open != null) {
					shop.open = open;
				}

				this.store.touch();
				this.listener.catalogueChanged();
				return OpResult.ok(shop.name);
			}
		}
	}

	public OpResult delete(Actor a, String shopId) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayEdit(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			long held = shop.stockTotal();
			if (held > 0L) {
				return OpResult.fail("has_stock", held);
			} else {
				this.store.remove(shop);
				this.listener.catalogueChanged();
				if (shop.owner != null) {
					this.listener.selfChanged(shop.owner);
				}

				return OpResult.ok(shop.name);
			}
		}
	}

	public OpResult offerAdd(Actor a, String shopId, int slot, long price, long sellPrice) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayEdit(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else if (a.pocket() == null) {
			return OpResult.fail("player_only");
		} else {
			ItemStack stack = a.pocket().slot(slot);
			if (stack.isEmpty()) {
				return OpResult.fail("empty_slot");
			} else if (!prices(price, sellPrice)) {
				return OpResult.fail("bad_price");
			} else if (!shop.server && shop.offers.size() >= this.rules.maxOffers) {
				return OpResult.fail("too_many_offers", this.rules.maxOffers);
			} else {
				Offer offer = new Offer();
				offer.id = shop.nextOffer++;
				offer.item = Stacks.write(stack.copy(), this.registries);
				offer.price = price;
				offer.sellPrice = sellPrice;
				shop.offers.add(offer);
				this.store.touch();
				this.listener.catalogueChanged();
				return OpResult.ok(Stacks.named(stack, (long)stack.getCount()).getString(), shop.name, offer.id);
			}
		}
	}

	public OpResult offerEdit(Actor a, String shopId, int offerId, long price, long sellPrice) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayEdit(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			Offer offer = shop.offer(offerId);
			if (offer == null) {
				return OpResult.fail("no_such_offer", offerId);
			} else if (!prices(price, sellPrice)) {
				return OpResult.fail("bad_price");
			} else {
				offer.price = price;
				offer.sellPrice = sellPrice;
				this.store.touch();
				this.listener.catalogueChanged();
				return OpResult.ok(this.name(offer), shop.name);
			}
		}
	}

	public OpResult offerRemove(Actor a, String shopId, int offerId) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayEdit(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			Offer offer = shop.offer(offerId);
			if (offer == null) {
				return OpResult.fail("no_such_offer", offerId);
			} else if (offer.stock > 0L) {
				return OpResult.fail("has_stock", offer.stock);
			} else {
				shop.offers.remove(offer);
				this.store.touch();
				this.listener.catalogueChanged();
				return OpResult.ok(this.name(offer), shop.name);
			}
		}
	}

	public OpResult stockAdd(Actor a, String shopId, int offerId) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayStock(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			List<Offer> which = new ArrayList<>();
			if (offerId > 0) {
				Offer o = shop.offer(offerId);
				if (o == null) {
					return OpResult.fail("no_such_offer", offerId);
				}

				which.add(o);
			} else {
				which.addAll(shop.offers);
			}

			long moved = 0L;

			for (Offer o : which) {
				ItemStack like = this.template(o);
				if (!like.isEmpty()) {
					long have = a.pocket().count(like);
					if (have > 0L) {
						long taken = a.pocket().take(like, have);
						o.stock += taken;
						moved += taken;
					}
				}
			}

			if (moved == 0L) {
				return OpResult.fail("not_enough", 0);
			} else {
				this.store.touch();
				this.listener.catalogueChanged();
				return OpResult.ok(moved, shop.name);
			}
		}
	}

	public OpResult stockTake(Actor a, String shopId, int offerId, long count) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayStock(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			Offer offer = shop.offer(offerId);
			if (offer == null) {
				return OpResult.fail("no_such_offer", offerId);
			} else if (offer.stock <= 0L) {
				return OpResult.fail("out_of_stock", 0);
			} else {
				ItemStack like = this.template(offer);
				if (like.isEmpty()) {
					return OpResult.fail("invalid");
				} else {
					long want = count <= 0L ? offer.stock : Math.min(count, offer.stock);
					long n = Math.min(want, a.pocket().room(like));
					if (n <= 0L) {
						return OpResult.fail("no_room", 0);
					} else {
						long given = a.pocket().give(like, n);
						offer.stock -= given;
						this.store.touch();
						this.listener.catalogueChanged();
						return OpResult.ok(Stacks.named(like, given).getString(), offer.stock);
					}
				}
			}
		}
	}

	public OpResult buy(Actor a, String shopId, int offerId, int lots) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayTrade(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			Offer offer = shop.offer(offerId);
			if (offer == null) {
				return OpResult.fail("no_such_offer", offerId);
			} else if (!offer.sells()) {
				return OpResult.fail("not_sold");
			} else if (lots >= 1 && lots <= 64) {
				ItemStack like = this.template(offer);
				if (like.isEmpty()) {
					return OpResult.fail("invalid");
				} else {
					long items = (long)like.getCount() * (long)lots;
					if (!shop.server && offer.stock < items) {
						return OpResult.fail("out_of_stock", offer.stock);
					} else {
						long cost = offer.price * (long)lots;
						Wallet buyer = this.walletOf(a);
						if (buyer.balance < cost) {
							return OpResult.fail("poor", this.rules.format(cost), this.rules.format(buyer.balance));
						} else {
							Wallet owner = shop.server ? null : this.store.wallet(shop.owner);
							if (!shop.server && owner == null) {
								return OpResult.fail("closed");
							} else if (owner != null && owner.balance + cost > 1000000000000L) {
								return OpResult.fail("shop_poor", shop.name);
							} else {
								long room = a.pocket().room(like);
								if (room < items) {
									return OpResult.fail("no_room", room);
								} else {
									long given = a.pocket().give(like, items);
									long now = this.clock.getAsLong();
									String what = Stacks.named(like, given).getString();
									buyer.balance -= cost;
									buyer.note("bought", -cost, what, shop.name, now);
									offer.sold += (long)lots;
									if (owner != null) {
										offer.stock -= given;
										owner.balance += cost;
										owner.note("shop_sold", cost, what, a.name(), now);
										this.listener.selfChanged(shop.owner);
										this.listener.notice(shop.owner, "shop_sold", a.name(), what, this.rules.format(cost), shop.name);
									}

									this.store.touch();
									this.listener.selfChanged(a.id());
									this.listener.catalogueChanged();
									return OpResult.ok(what, this.rules.format(cost), shop.name);
								}
							}
						}
					}
				}
			} else {
				return OpResult.fail("invalid");
			}
		}
	}

	public OpResult sell(Actor a, String shopId, int offerId, int lots) {
		Shop shop = this.store.shop(shopId);
		OpResult refused = this.mayTrade(a, shop, shopId);
		if (refused != null) {
			return refused;
		} else {
			Offer offer = shop.offer(offerId);
			if (offer == null) {
				return OpResult.fail("no_such_offer", offerId);
			} else if (!offer.buys()) {
				return OpResult.fail("not_bought");
			} else if (lots >= 1 && lots <= 64) {
				ItemStack like = this.template(offer);
				if (like.isEmpty()) {
					return OpResult.fail("invalid");
				} else {
					long items = (long)like.getCount() * (long)lots;
					long have = a.pocket().count(like);
					if (have < items) {
						return OpResult.fail("not_enough", have);
					} else {
						long pay = offer.sellPrice * (long)lots;
						Wallet seller = this.walletOf(a);
						if (seller.balance + pay > 1000000000000L) {
							return OpResult.fail("too_rich", this.rules.format(seller.balance + pay));
						} else {
							Wallet owner = shop.server ? null : this.store.wallet(shop.owner);
							if (!shop.server && owner == null) {
								return OpResult.fail("closed");
							} else if (owner != null && owner.balance < pay) {
								return OpResult.fail("shop_poor", shop.name);
							} else {
								long taken = a.pocket().take(like, items);
								long now = this.clock.getAsLong();
								String what = Stacks.named(like, taken).getString();
								seller.balance += pay;
								seller.note("sold", pay, what, shop.name, now);
								offer.bought += (long)lots;
								if (owner != null) {
									offer.stock += taken;
									owner.balance -= pay;
									owner.note("shop_bought", -pay, what, a.name(), now);
									this.listener.selfChanged(shop.owner);
									this.listener.notice(shop.owner, "shop_bought", a.name(), what, this.rules.format(pay), shop.name);
								}

								this.store.touch();
								this.listener.selfChanged(a.id());
								this.listener.catalogueChanged();
								return OpResult.ok(what, this.rules.format(pay), shop.name);
							}
						}
					}
				}
			} else {
				return OpResult.fail("invalid");
			}
		}
	}

	public OpResult pay(Actor a, String to, long amount) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (!a.may(ShopNodes.PAY)) {
			return OpResult.fail("no_permission");
		} else if (a.id() == null) {
			return OpResult.fail("player_only");
		} else if (amount >= 1L && amount <= 1000000000000L) {
			UUID target = this.store.byName(to);
			if (target == null) {
				return OpResult.fail("no_such_player", to);
			} else if (target.equals(a.id())) {
				return OpResult.fail("self");
			} else {
				Wallet from = this.walletOf(a);
				Wallet into = this.store.wallet(target);
				if (from.balance < amount) {
					return OpResult.fail("poor", this.rules.format(amount), this.rules.format(from.balance));
				} else if (into.balance + amount > 1000000000000L) {
					return OpResult.fail("too_rich", this.rules.format(into.balance + amount));
				} else {
					long now = this.clock.getAsLong();
					from.balance -= amount;
					into.balance += amount;
					from.note("paid", -amount, "", into.name, now);
					into.note("received", amount, "", a.name(), now);
					this.store.touch();
					this.listener.selfChanged(a.id());
					this.listener.selfChanged(target);
					this.listener.notice(target, "paid", a.name(), this.rules.format(amount));
					return OpResult.ok(this.rules.format(amount), into.name);
				}
			}
		} else {
			return OpResult.fail("bad_price");
		}
	}

	public OpResult money(Actor a, String who, String what, long amount) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (!a.may(ShopNodes.MONEY)) {
			return OpResult.fail("no_permission");
		} else if (amount >= 0L && amount <= 1000000000000L) {
			UUID target = this.store.byName(who);
			if (target == null) {
				return OpResult.fail("no_such_player", who);
			} else {
				Wallet w = this.store.wallet(target);
				long before = w.balance;

				long after = switch (what) {
					case "give" -> before + amount;
					case "take" -> Math.max(0L, before - amount);
					case "set" -> amount;
					default -> -1L;
				};
				if (after < 0L) {
					return OpResult.fail("invalid");
				} else if (after > 1000000000000L) {
					return OpResult.fail("too_rich", this.rules.format(after));
				} else {
					w.balance = after;
					w.note("admin", after - before, "", a.name(), this.clock.getAsLong());
					this.store.touch();
					this.listener.selfChanged(target);
					this.listener.notice(target, "balance_set", this.rules.format(after));
					return OpResult.ok(w.name, this.rules.format(after));
				}
			}
		} else {
			return OpResult.fail("bad_price");
		}
	}

	public OpResult exchange(Actor a, String what, long count) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (!a.may(ShopNodes.USE)) {
			return OpResult.fail("no_permission");
		} else if (a.id() != null && a.pocket() != null) {
			ItemStack like = Stacks.plain(this.rules.exchangeItem);
			if (like.isEmpty()) {
				return OpResult.fail("exchange_off");
			} else {
				Wallet w = this.walletOf(a);
				long value = this.rules.exchangeValue;
				long now = this.clock.getAsLong();
				if ("deposit".equals(what)) {
					long have = a.pocket().count(like);
					long n = count <= 0L ? have : count;
					if (n > 0L && have >= n) {
						if (w.balance + n * value > 1000000000000L) {
							return OpResult.fail("too_rich", this.rules.format(w.balance + n * value));
						} else {
							long taken = a.pocket().take(like, n);
							w.balance += taken * value;
							w.note("exchange", taken * value, Stacks.named(like, taken).getString(), "", now);
							this.store.touch();
							this.listener.selfChanged(a.id());
							return OpResult.ok(Stacks.named(like, taken).getString(), this.rules.format(taken * value));
						}
					} else {
						return OpResult.fail("not_enough", have);
					}
				} else if (!"withdraw".equals(what)) {
					return OpResult.fail("invalid");
				} else if (count >= 1L && count <= 1000000000000L / value) {
					long cost = count * value;
					if (w.balance < cost) {
						return OpResult.fail("poor", this.rules.format(cost), this.rules.format(w.balance));
					} else {
						long room = a.pocket().room(like);
						if (room < count) {
							return OpResult.fail("no_room", room);
						} else {
							long given = a.pocket().give(like, count);
							w.balance -= given * value;
							w.note("exchange", -given * value, Stacks.named(like, given).getString(), "", now);
							this.store.touch();
							this.listener.selfChanged(a.id());
							return OpResult.ok(Stacks.named(like, given).getString(), this.rules.format(given * value));
						}
					}
				} else {
					return OpResult.fail("invalid");
				}
			}
		} else {
			return OpResult.fail("player_only");
		}
	}

	@Nullable
	private OpResult mayEdit(Actor a, @Nullable Shop shop, String shopId) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (shop == null) {
			return OpResult.fail("no_such_shop", shopId);
		} else if (a.may(ShopNodes.ADMIN)) {
			return null;
		} else if (!shop.ownedBy(a.id())) {
			return OpResult.fail("not_yours");
		} else {
			return !a.may(ShopNodes.CREATE) ? OpResult.fail("no_permission") : null;
		}
	}

	@Nullable
	private OpResult mayStock(Actor a, @Nullable Shop shop, String shopId) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (shop == null) {
			return OpResult.fail("no_such_shop", shopId);
		} else if (a.pocket() == null) {
			return OpResult.fail("player_only");
		} else {
			return !shop.server && shop.ownedBy(a.id()) ? null : OpResult.fail("not_yours");
		}
	}

	@Nullable
	private OpResult mayTrade(Actor a, @Nullable Shop shop, String shopId) {
		if (this.store.unavailable()) {
			return OpResult.fail("unavailable");
		} else if (!a.may(ShopNodes.USE)) {
			return OpResult.fail("no_permission");
		} else if (a.id() == null || a.pocket() == null) {
			return OpResult.fail("player_only");
		} else if (shop == null) {
			return OpResult.fail("no_such_shop", shopId);
		} else if (shop.ownedBy(a.id())) {
			return OpResult.fail("own_shop");
		} else if (!shop.open) {
			return OpResult.fail("closed");
		} else {
			return !shop.server && !this.rules.playerShops ? OpResult.fail("disabled") : null;
		}
	}

	private static boolean prices(long price, long sellPrice) {
		return price >= 0L && sellPrice >= 0L && price <= 1000000000L && sellPrice <= 1000000000L && price + sellPrice > 0L;
	}

	private String name(Offer offer) {
		ItemStack like = this.template(offer);
		return like.isEmpty() ? "#" + offer.id : Stacks.named(like, (long)like.getCount()).getString();
	}

	@Nullable
	static String cleanName(@Nullable String name) {
		String s = cleanText(name, Integer.MAX_VALUE);
		return !s.isEmpty() && s.length() <= 32 ? s : null;
	}

	static String cleanText(@Nullable String text, int max) {
		if (text == null) {
			return "";
		} else {
			StringBuilder b = new StringBuilder(text.length());

			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c >= ' ' && c != 127 && c != 167) {
					b.append(c);
				}
			}

			String s = b.toString().strip();
			return s.length() <= max ? s : s.substring(0, max).strip();
		}
	}

	private String freeId(String name) {
		StringBuilder b = new StringBuilder();

		for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
			if ((c < 'a' || c > 'z') && (c < '0' || c > '9')) {
				if (b.length() > 0 && b.charAt(b.length() - 1) != '-') {
					b.append('-');
				}
			} else {
				b.append(c);
			}
		}

		String base = b.toString();

		while (base.endsWith("-")) {
			base = base.substring(0, base.length() - 1);
		}

		if (base.length() > 24) {
			base = base.substring(0, 24);
		}

		if (base.isEmpty()) {
			base = "shop";
		}

		String id = base;

		for (int i = 2; this.store.shop(id) != null; i++) {
			id = base + "-" + i;
		}

		return id;
	}

	public interface Listener {
		void catalogueChanged();

		void selfChanged(UUID var1);

		void notice(UUID var1, String var2, String... var3);
	}
}
