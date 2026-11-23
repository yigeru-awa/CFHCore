package com.onehumanawa.cfhcore.content.gui.shops.client;

import com.onehumanawa.cfhcore.content.gui.core.event.Event;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.shops.CFHGuiShops;
import com.onehumanawa.cfhcore.content.gui.shops.model.Catalogue;
import com.onehumanawa.cfhcore.content.gui.shops.model.Hello;
import com.onehumanawa.cfhcore.content.gui.shops.model.Offer;
import com.onehumanawa.cfhcore.content.gui.shops.model.SelfState;
import com.onehumanawa.cfhcore.content.gui.shops.model.Shop;
import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import com.onehumanawa.cfhcore.content.gui.shops.service.Stacks;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ShopsState {
	public static final Event<Runnable> CHANGED = new Event("cfhgui_shops:changed");
	private static final Hello NONE = new Hello();
	@Nullable
	private static Hello hello;
	private static Catalogue catalogue = new Catalogue();
	private static SelfState self = new SelfState();
	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	public static void onState(ShopsNet.State p) {
		String var1 = p.kind();
		switch (var1) {
			case "hello":
				Hello h = (Hello)JsonWire.decode(p.data(), Hello.class);
				if (h != null) {
					hello = h;
				}
				break;
			case "catalogue":
				Catalogue c = (Catalogue)JsonWire.decode(p.data(), Catalogue.class);
				if (c != null) {
					for (Shop sx : c.shops) {
						sx.sanitize();
					}

					catalogue = c;
				}
				break;
			case "self":
				SelfState s = (SelfState)JsonWire.decode(p.data(), SelfState.class);
				if (s != null) {
					self = s;
				}
				break;
			default:
				CFHGuiShops.LOGGER.debug("[CFHGui Shops] state of an unknown kind: {}", p.kind());
		}

		CHANGED.invoke(Runnable::run);
	}

	public static void onNotice(ShopsNet.Notice n) {
		Object[] args = n.args().toArray();
		CFHGuiToasts.show(Component.translatable("cfhgui_shops.notice." + n.key()), Component.translatable("cfhgui_shops.notice." + n.key() + ".body", args), Icon.TAG);
	}

	public static void clear() {
		hello = null;
		catalogue = new Catalogue();
		self = new SelfState();
		STACKS.clear();
		CHANGED.invoke(Runnable::run);
	}

	public static boolean available() {
		return hello != null && Minecraft.getInstance().getConnection() != null && CFHGuiNetwork.get().serverHasChannel(ShopsNet.Action.TYPE);
	}

	public static boolean newerServer() {
		return hello != null && hello.protocol > 1;
	}

	public static Hello hello() {
		return hello == null ? NONE : hello;
	}

	public static boolean may(String what) {
		return hello != null && hello.may.contains(what);
	}

	public static List<Shop> shops() {
		return catalogue.shops;
	}

	@Nullable
	public static Shop shop(@Nullable String id) {
		if (id != null && !id.isEmpty()) {
			for (Shop s : catalogue.shops) {
				if (s.id.equals(id)) {
					return s;
				}
			}

			return null;
		} else {
			return null;
		}
	}

	@Nullable
	public static Shop mine() {
		return shop(self.shop);
	}

	public static long balance() {
		return self.balance;
	}

	public static SelfState self() {
		return self;
	}

	@Nullable
	public static UUID me() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player == null ? null : mc.player.getUUID();
	}

	public static String money(long amount) {
		return hello().format(amount);
	}

	public static ItemStack stack(Offer offer) {
		String text = offer.item;
		ItemStack known = STACKS.get(text);
		if (known != null) {
			return known;
		} else {
			Minecraft mc = Minecraft.getInstance();
			Provider registries = mc.level != null ? mc.level.registryAccess() : null;
			if (registries == null) {
				return ItemStack.EMPTY;
			} else {
				ItemStack stack = Stacks.read(text, registries);
				if (STACKS.size() > 2048) {
					STACKS.clear();
				}

				STACKS.put(text, stack);
				return stack;
			}
		}
	}

	private ShopsState() {
	}
}
