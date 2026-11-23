package com.onehumanawa.cfhcore.content.gui.server.net;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.service.ServerRuntime;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public final class ServerNet {
	private static final int MAX_BYTES = 1048576;
	private static boolean registered;

	public static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(ServerNet.Action.TYPE, ServerNet.Action.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					ServerRuntime.onAction(ctx.sender(), p);
				}
			});
			net.register(ServerNet.Result.TYPE, ServerNet.Result.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ServerNet.ClientSide.result(p);
				}
			});
			net.register(ServerNet.State.TYPE, ServerNet.State.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ServerNet.ClientSide.state(p);
				}
			});
			net.register(ServerNet.Reply.TYPE, ServerNet.Reply.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ServerNet.ClientSide.reply(p);
				}
			});
			net.register(ServerNet.Notice.TYPE, ServerNet.Notice.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ServerNet.ClientSide.notice(p);
				}
			});
			net.register(ServerNet.Open.TYPE, ServerNet.Open.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					ServerNet.ClientSide.open(p);
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
		return CFHGuiNetwork.get().canSendToPlayer(player, ServerNet.State.TYPE);
	}

	private ServerNet() {
	}

	public static record Action(int request, String op, CompoundTag args) implements CustomPacketPayload {
		public static final Type<ServerNet.Action> TYPE = new Type(CFHGui.id("server_action"));
		public static final StreamCodec<ByteBuf, ServerNet.Action> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ServerNet.Action::request,
			ByteBufCodecs.stringUtf8(48),
			ServerNet.Action::op,
			ByteBufCodecs.COMPOUND_TAG,
			ServerNet.Action::args,
			ServerNet.Action::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class ClientSide {
		static void result(ServerNet.Result p) {
			ServerActions.onResult(p);
		}

		static void reply(ServerNet.Reply p) {
			ServerActions.onReply(p);
		}

		static void state(ServerNet.State p) {
			ServerState.onState(p);
		}

		static void notice(ServerNet.Notice p) {
			ServerState.onNotice(p);
		}

		static void open(ServerNet.Open p) {
			ServerClient.onOpen(p);
		}
	}

	public static record Notice(String key, List<String> args, int color) implements CustomPacketPayload {
		public static final String TPA_RECEIVED = "tpa_received";
		public static final String TPAHERE_RECEIVED = "tpahere_received";
		public static final String TPA_ACCEPTED = "tpa_accepted";
		public static final String TPA_DENIED = "tpa_denied";
		public static final String TPA_EXPIRED = "tpa_expired";
		public static final String CANCELLED_MOVED = "cancelled_moved";
		public static final String CANCELLED_HURT = "cancelled_hurt";
		public static final String MAIL_RECEIVED = "mail_received";
		public static final String ROLE_GRANTED = "role_granted";
		public static final String ROLE_REVOKED = "role_revoked";
		public static final String KIT_RECEIVED = "kit_received";
		public static final String MUTED = "muted";
		public static final String UNMUTED = "unmuted";
		public static final String WARNED = "warned";
		public static final String FROZEN = "frozen";
		public static final String UNFROZEN = "unfrozen";
		public static final String DEATH_PLACE = "death_place";
		public static final Type<ServerNet.Notice> TYPE = new Type(CFHGui.id("server_notice"));
		public static final StreamCodec<ByteBuf, ServerNet.Notice> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64),
			ServerNet.Notice::key,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			ServerNet.Notice::args,
			ByteBufCodecs.INT,
			ServerNet.Notice::color,
			ServerNet.Notice::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Open(String what, String arg) implements CustomPacketPayload {
		public static final String HUB = "hub";
		public static final String ADMIN = "admin";
		public static final String STATS = "stats";
		public static final String RULES = "rules";
		public static final Type<ServerNet.Open> TYPE = new Type(CFHGui.id("server_open"));
		public static final StreamCodec<ByteBuf, ServerNet.Open> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), ServerNet.Open::what, ByteBufCodecs.stringUtf8(128), ServerNet.Open::arg, ServerNet.Open::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Reply(int request, String op, byte[] data) implements CustomPacketPayload {
		public static final Type<ServerNet.Reply> TYPE = new Type(CFHGui.id("server_reply"));
		public static final StreamCodec<ByteBuf, ServerNet.Reply> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ServerNet.Reply::request,
			ByteBufCodecs.stringUtf8(48),
			ServerNet.Reply::op,
			ByteBufCodecs.byteArray(1048576),
			ServerNet.Reply::data,
			ServerNet.Reply::new
		);

		public static ServerNet.Reply of(int request, String op, Object value) {
			return new ServerNet.Reply(request, op, JsonWire.encode(value));
		}

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Result(int request, boolean ok, String code, List<String> args) implements CustomPacketPayload {
		public static final Type<ServerNet.Result> TYPE = new Type(CFHGui.id("server_result"));
		public static final StreamCodec<ByteBuf, ServerNet.Result> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			ServerNet.Result::request,
			ByteBufCodecs.BOOL,
			ServerNet.Result::ok,
			ByteBufCodecs.stringUtf8(64),
			ServerNet.Result::code,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			ServerNet.Result::args,
			ServerNet.Result::new
		);

		public static ServerNet.Result ok(int request) {
			return new ServerNet.Result(request, true, "ok", List.of());
		}

		public static ServerNet.Result fail(int request, String code, String... args) {
			return new ServerNet.Result(request, false, code, List.of(args));
		}

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record State(String kind, byte[] data) implements CustomPacketPayload {
		public static final String HELLO = "hello";
		public static final String SELF = "self";
		public static final String WARPS = "warps";
		public static final String REQUESTS = "requests";
		public static final String ONLINE = "online";
		public static final String MAIL = "mail";
		public static final String DASHBOARD = "dashboard";
		public static final Type<ServerNet.State> TYPE = new Type(CFHGui.id("server_state"));
		public static final StreamCodec<ByteBuf, ServerNet.State> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), ServerNet.State::kind, ByteBufCodecs.byteArray(1048576), ServerNet.State::data, ServerNet.State::new
		);

		public static ServerNet.State of(String kind, Object value) {
			return new ServerNet.State(kind, JsonWire.encode(value));
		}

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
