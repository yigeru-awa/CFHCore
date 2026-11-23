package com.onehumanawa.cfhcore.content.gui.teams.net;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.ClaimsUpdate;
import com.onehumanawa.cfhcore.content.gui.teams.model.PingData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import com.onehumanawa.cfhcore.content.gui.teams.server.TeamsServer;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public final class TeamsNet {
	private static final int MAX_STATE_BYTES = 1048576;
	private static boolean registered;

	public static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(TeamsNet.Action.TYPE, TeamsNet.Action.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					TeamsServer.onAction(ctx.sender(), p);
				}
			});
			net.register(TeamsNet.Hello.TYPE, TeamsNet.Hello.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.hello(p);
				}
			});
			net.register(TeamsNet.MyTeam.TYPE, TeamsNet.MyTeam.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.myTeam(p);
				}
			});
			net.register(TeamsNet.Directory.TYPE, TeamsNet.Directory.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.directory(p);
				}
			});
			net.register(TeamsNet.Invites.TYPE, TeamsNet.Invites.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.invites(p);
				}
			});
			net.register(TeamsNet.Teammates.TYPE, TeamsNet.Teammates.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.teammates(p);
				}
			});
			net.register(TeamsNet.Claims.TYPE, TeamsNet.Claims.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.claims(p);
				}
			});
			net.register(TeamsNet.Ping.TYPE, TeamsNet.Ping.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.ping(p);
				}
			});
			net.register(TeamsNet.Result.TYPE, TeamsNet.Result.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.result(p);
				}
			});
			net.register(TeamsNet.Notice.TYPE, TeamsNet.Notice.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamsNet.ClientSide.notice(p);
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
		return CFHGuiNetwork.get().canSendToPlayer(player, TeamsNet.Hello.TYPE);
	}

	private TeamsNet() {
	}

	public static record Action(int request, String op, CompoundTag args) implements CustomPacketPayload {
		public static final Type<TeamsNet.Action> TYPE = new Type(CFHGui.id("teams_action"));
		public static final StreamCodec<ByteBuf, TeamsNet.Action> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			TeamsNet.Action::request,
			ByteBufCodecs.stringUtf8(48),
			TeamsNet.Action::op,
			ByteBufCodecs.COMPOUND_TAG,
			TeamsNet.Action::args,
			TeamsNet.Action::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Claims(ClaimsUpdate update) implements CustomPacketPayload {
		public static final Type<TeamsNet.Claims> TYPE = new Type(CFHGui.id("teams_claims"));
		public static final StreamCodec<ByteBuf, TeamsNet.Claims> CODEC = ClaimsUpdate.CODEC.map(TeamsNet.Claims::new, TeamsNet.Claims::update);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class ClientSide {
		static void hello(TeamsNet.Hello p) {
			TeamsState.onHello(p);
		}

		static void myTeam(TeamsNet.MyTeam p) {
			TeamsState.onMyTeam(p);
		}

		static void directory(TeamsNet.Directory p) {
			TeamsState.onDirectory(p);
		}

		static void invites(TeamsNet.Invites p) {
			TeamsState.onInvites(p);
		}

		static void teammates(TeamsNet.Teammates p) {
			TeamsState.onTeammates(p);
		}

		static void claims(TeamsNet.Claims p) {
			TeamsState.onClaims(p);
		}

		static void ping(TeamsNet.Ping p) {
			TeamsState.onPing(p);
		}

		static void result(TeamsNet.Result p) {
			TeamActions.onResult(p);
		}

		static void notice(TeamsNet.Notice p) {
			TeamsState.onNotice(p);
		}
	}

	public static record Directory(byte[] teams) implements CustomPacketPayload {
		public static final Type<TeamsNet.Directory> TYPE = new Type(CFHGui.id("teams_directory"));
		public static final StreamCodec<ByteBuf, TeamsNet.Directory> CODEC = ByteBufCodecs.byteArray(1048576).map(TeamsNet.Directory::new, TeamsNet.Directory::teams);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Hello(byte[] info) implements CustomPacketPayload {
		public static final Type<TeamsNet.Hello> TYPE = new Type(CFHGui.id("teams_hello"));
		public static final StreamCodec<ByteBuf, TeamsNet.Hello> CODEC = ByteBufCodecs.byteArray(1048576).map(TeamsNet.Hello::new, TeamsNet.Hello::info);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Invites(byte[] invites, byte[] requested) implements CustomPacketPayload {
		public static final Type<TeamsNet.Invites> TYPE = new Type(CFHGui.id("teams_invites"));
		public static final StreamCodec<ByteBuf, TeamsNet.Invites> CODEC = StreamCodec.composite(
			ByteBufCodecs.byteArray(1048576), TeamsNet.Invites::invites, ByteBufCodecs.byteArray(1048576), TeamsNet.Invites::requested, TeamsNet.Invites::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record MyTeam(byte[] team) implements CustomPacketPayload {
		public static final Type<TeamsNet.MyTeam> TYPE = new Type(CFHGui.id("teams_my_team"));
		public static final StreamCodec<ByteBuf, TeamsNet.MyTeam> CODEC = ByteBufCodecs.byteArray(1048576).map(TeamsNet.MyTeam::new, TeamsNet.MyTeam::team);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Notice(String key, List<String> args, int color) implements CustomPacketPayload {
		public static final Type<TeamsNet.Notice> TYPE = new Type(CFHGui.id("teams_notice"));
		public static final StreamCodec<ByteBuf, TeamsNet.Notice> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64),
			TeamsNet.Notice::key,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			TeamsNet.Notice::args,
			ByteBufCodecs.INT,
			TeamsNet.Notice::color,
			TeamsNet.Notice::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Ping(PingData ping) implements CustomPacketPayload {
		public static final Type<TeamsNet.Ping> TYPE = new Type(CFHGui.id("teams_ping"));
		public static final StreamCodec<ByteBuf, TeamsNet.Ping> CODEC = PingData.CODEC.map(TeamsNet.Ping::new, TeamsNet.Ping::ping);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Result(int request, boolean ok, String code, List<String> args) implements CustomPacketPayload {
		public static final Type<TeamsNet.Result> TYPE = new Type(CFHGui.id("teams_result"));
		public static final StreamCodec<ByteBuf, TeamsNet.Result> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			TeamsNet.Result::request,
			ByteBufCodecs.BOOL,
			TeamsNet.Result::ok,
			ByteBufCodecs.stringUtf8(64),
			TeamsNet.Result::code,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			TeamsNet.Result::args,
			TeamsNet.Result::new
		);

		public static TeamsNet.Result ok(int request) {
			return new TeamsNet.Result(request, true, "ok", List.of());
		}

		public static TeamsNet.Result fail(int request, String code, String... args) {
			return new TeamsNet.Result(request, false, code, List.of(args));
		}

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Teammates(List<TeammateState> states) implements CustomPacketPayload {
		public static final Type<TeamsNet.Teammates> TYPE = new Type(CFHGui.id("teams_teammates"));
		public static final StreamCodec<ByteBuf, TeamsNet.Teammates> CODEC = TeammateState.CODEC
			.apply(ByteBufCodecs.list(512))
			.map(TeamsNet.Teammates::new, TeamsNet.Teammates::states);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
