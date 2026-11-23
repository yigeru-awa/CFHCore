package com.onehumanawa.cfhcore.content.gui.crafting.team;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamPinsClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public final class TeamPinsNet {
	private static final int MAX_BYTES = 1048576;
	public static final String OP_PIN_RECIPE = "pin_recipe";
	public static final String OP_PIN_TREE = "pin_tree";
	public static final String OP_UNPIN = "unpin";
	private static boolean registered;

	static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(TeamPinsNet.Ask.TYPE, TeamPinsNet.Ask.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					TeamPinsServer.onAsk(ctx.sender(), p);
				}
			});
			net.register(TeamPinsNet.Reset.TYPE, TeamPinsNet.Reset.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamPinsNet.ClientSide.reset(p);
				}
			});
			net.register(TeamPinsNet.Put.TYPE, TeamPinsNet.Put.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamPinsNet.ClientSide.put(p);
				}
			});
			net.register(TeamPinsNet.Gone.TYPE, TeamPinsNet.Gone.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamPinsNet.ClientSide.gone(p);
				}
			});
			net.register(TeamPinsNet.Answer.TYPE, TeamPinsNet.Answer.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					TeamPinsNet.ClientSide.answer(p);
				}
			});
		}
	}

	private static boolean client() {
		return CFHGuiPlatform.get().isClient();
	}

	static void send(ServerPlayer player, CustomPacketPayload payload) {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.canSendToPlayer(player, payload.type())) {
			net.sendToPlayer(player, payload);
		}
	}

	static boolean has(ServerPlayer player) {
		return CFHGuiNetwork.get().canSendToPlayer(player, TeamPinsNet.Reset.TYPE);
	}

	private TeamPinsNet() {
	}

	public static record Answer(int request, boolean ok, String code) implements CustomPacketPayload {
		public static final Type<TeamPinsNet.Answer> TYPE = new Type(CFHGui.id("crafting_pins_answer"));
		public static final StreamCodec<ByteBuf, TeamPinsNet.Answer> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			TeamPinsNet.Answer::request,
			ByteBufCodecs.BOOL,
			TeamPinsNet.Answer::ok,
			ByteBufCodecs.stringUtf8(32),
			TeamPinsNet.Answer::code,
			TeamPinsNet.Answer::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Ask(int request, String op, String team, String subject, byte[] tree, int steps) implements CustomPacketPayload {
		public static final Type<TeamPinsNet.Ask> TYPE = new Type(CFHGui.id("crafting_pins_ask"));
		public static final StreamCodec<ByteBuf, TeamPinsNet.Ask> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			TeamPinsNet.Ask::request,
			ByteBufCodecs.stringUtf8(16),
			TeamPinsNet.Ask::op,
			ByteBufCodecs.stringUtf8(64),
			TeamPinsNet.Ask::team,
			ByteBufCodecs.stringUtf8(256),
			TeamPinsNet.Ask::subject,
			ByteBufCodecs.byteArray(30000),
			TeamPinsNet.Ask::tree,
			ByteBufCodecs.VAR_INT,
			TeamPinsNet.Ask::steps,
			TeamPinsNet.Ask::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class ClientSide {
		static void reset(TeamPinsNet.Reset p) {
			TeamPinsClient.onReset(p);
		}

		static void put(TeamPinsNet.Put p) {
			TeamPinsClient.onPut(p);
		}

		static void gone(TeamPinsNet.Gone p) {
			TeamPinsClient.onGone(p);
		}

		static void answer(TeamPinsNet.Answer p) {
			TeamPinsClient.onAnswer(p);
		}
	}

	public static record Gone(String id) implements CustomPacketPayload {
		public static final Type<TeamPinsNet.Gone> TYPE = new Type(CFHGui.id("crafting_pins_gone"));
		public static final StreamCodec<ByteBuf, TeamPinsNet.Gone> CODEC = ByteBufCodecs.stringUtf8(64).map(TeamPinsNet.Gone::new, TeamPinsNet.Gone::id);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Put(boolean sync, byte[] pins) implements CustomPacketPayload {
		public static final Type<TeamPinsNet.Put> TYPE = new Type(CFHGui.id("crafting_pins_put"));
		public static final StreamCodec<ByteBuf, TeamPinsNet.Put> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, TeamPinsNet.Put::sync, ByteBufCodecs.byteArray(1048576), TeamPinsNet.Put::pins, TeamPinsNet.Put::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Reset(String world, String team) implements CustomPacketPayload {
		public static final Type<TeamPinsNet.Reset> TYPE = new Type(CFHGui.id("crafting_pins_reset"));
		public static final StreamCodec<ByteBuf, TeamPinsNet.Reset> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64), TeamPinsNet.Reset::world, ByteBufCodecs.stringUtf8(64), TeamPinsNet.Reset::team, TeamPinsNet.Reset::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
