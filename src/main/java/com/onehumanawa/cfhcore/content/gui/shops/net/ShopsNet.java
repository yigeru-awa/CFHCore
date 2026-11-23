package com.onehumanawa.cfhcore.content.gui.shops.net;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsActions;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsClient;
import com.onehumanawa.cfhcore.content.gui.shops.client.ShopsState;
import com.onehumanawa.cfhcore.content.gui.shops.service.ShopsRuntime;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public final class ShopsNet {
	public static final int MAX_BYTES = 1048576;
	private static boolean registered;

	public static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(ShopsNet.Action.TYPE, ShopsNet.Action.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					ShopsRuntime.onAction(ctx.sender(), p);
				}
			});
			net.register(ShopsNet.Result.TYPE, ShopsNet.Result.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ShopsNet.ClientSide.result(p);
				}
			});
			net.register(ShopsNet.State.TYPE, ShopsNet.State.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ShopsNet.ClientSide.state(p);
				}
			});
			net.register(ShopsNet.Notice.TYPE, ShopsNet.Notice.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ShopsNet.ClientSide.notice(p);
				}
			});
			net.register(ShopsNet.Open.TYPE, ShopsNet.Open.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ShopsNet.ClientSide.open(p);
				}
			});
		}
	}

	private static boolean client() {
		return CFHGuiPlatform.get().isClient();
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.canSendToPlayer(player, payload.type())) {
			net.sendToPlayer(player, payload);
		}
	}

	public static boolean has(ServerPlayer player) {
		return CFHGuiNetwork.get().canSendToPlayer(player, ShopsNet.State.TYPE);
	}

	private ShopsNet() {
	}

	public static record Action(int request, String op, CompoundTag args) implements CustomPacketPayload {
		public static final Type<ShopsNet.Action> TYPE = new Type(CFHGui.id("shops_action"));
		public static final StreamCodec<ByteBuf, ShopsNet.Action> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ShopsNet.Action::request,
			ByteBufCodecs.stringUtf8(32),
			ShopsNet.Action::op,
			ByteBufCodecs.COMPOUND_TAG,
			ShopsNet.Action::args,
			ShopsNet.Action::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class ClientSide {
		static void result(ShopsNet.Result p) {
			ShopsActions.onResult(p);
		}

		static void state(ShopsNet.State p) {
			ShopsState.onState(p);
		}

		static void notice(ShopsNet.Notice p) {
			ShopsState.onNotice(p);
		}

		static void open(ShopsNet.Open p) {
			ShopsClient.onOpen(p);
		}
	}

	public static record Notice(String key, List<String> args) implements CustomPacketPayload {
		public static final String SHOP_SOLD = "shop_sold";
		public static final String SHOP_BOUGHT = "shop_bought";
		public static final String PAID = "paid";
		public static final String BALANCE_SET = "balance_set";
		public static final Type<ShopsNet.Notice> TYPE = new Type(CFHGui.id("shops_notice"));
		public static final StreamCodec<ByteBuf, ShopsNet.Notice> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(48), ShopsNet.Notice::key, ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)), ShopsNet.Notice::args, ShopsNet.Notice::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Open(String what, String arg) implements CustomPacketPayload {
		public static final String BROWSE = "browse";
		public static final String MINE = "mine";
		public static final String WALLET = "wallet";
		public static final String ADMIN = "admin";
		public static final String SHOP = "shop";
		public static final Type<ShopsNet.Open> TYPE = new Type(CFHGui.id("shops_open"));
		public static final StreamCodec<ByteBuf, ShopsNet.Open> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), ShopsNet.Open::what, ByteBufCodecs.stringUtf8(64), ShopsNet.Open::arg, ShopsNet.Open::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Result(int request, boolean ok, String code, List<String> args) implements CustomPacketPayload {
		public static final Type<ShopsNet.Result> TYPE = new Type(CFHGui.id("shops_result"));
		public static final StreamCodec<ByteBuf, ShopsNet.Result> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ShopsNet.Result::request,
			ByteBufCodecs.BOOL,
			ShopsNet.Result::ok,
			ByteBufCodecs.stringUtf8(48),
			ShopsNet.Result::code,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			ShopsNet.Result::args,
			ShopsNet.Result::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record State(String kind, byte[] data) implements CustomPacketPayload {
		public static final String HELLO = "hello";
		public static final String CATALOGUE = "catalogue";
		public static final String SELF = "self";
		public static final Type<ShopsNet.State> TYPE = new Type(CFHGui.id("shops_state"));
		public static final StreamCodec<ByteBuf, ShopsNet.State> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), ShopsNet.State::kind, ByteBufCodecs.byteArray(1048576), ShopsNet.State::data, ShopsNet.State::new
		);

		public static ShopsNet.State of(String kind, Object value) {
			return new ShopsNet.State(kind, JsonWire.encode(value));
		}

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
