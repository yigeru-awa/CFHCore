package com.onehumanawa.cfhcore.content.gui.quests.server.adv;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.dev.AdvClient;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import com.onehumanawa.cfhcore.content.gui.quests.server.BookDoor;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AdvNet {
	public static final String TREES = "trees";
	public static final String PREVIEW = "preview";
	public static final String CONVERT = "convert";
	public static final String UNDO = "undo";
	private static final int MAX_BYTES = 2097152;
	private static final int MAX_SEND = 1032192;
	private static boolean registered;

	public static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(AdvNet.Ask.TYPE, AdvNet.Ask.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					onAsk(ctx.sender(), p);
				}
			});
			net.register(AdvNet.Answer.TYPE, AdvNet.Answer.CODEC, Flow.S2C, (p, ctx) -> {
				if (CFHGuiPlatform.get().isClient()) {
					AdvNet.ClientSide.answer(p);
				}
			});
		}
	}

	private static void onAsk(ServerPlayer player, AdvNet.Ask ask) {
		AdvService.Result result;
		try {
			result = act(player, ask.op(), ask.args());
		} catch (Exception var8) {
			CFHGuiQuests.LOGGER
				.error("[CFHGui Quests] the request \"{}\" of {} about advancement trees failed", new Object[]{ask.op(), player.getGameProfile().getName(), var8});
			result = AdvService.Result.fail("invalid");
		}

		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.canSendToPlayer(player, AdvNet.Answer.TYPE)) {
			byte[] data = result.data() == null ? new byte[0] : JsonWire.encode(BookJson.GSON, result.data());
			if (data.length > 1032192) {
				CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the answer to \"{}\" about advancement trees is {} bytes, more than can be sent", ask.op(), data.length);
				result = AdvService.Result.fail("too_big");
				data = new byte[0];
			}

			List<String> args = new ArrayList<>();

			for (String a : result.args()) {
				if (args.size() < 8) {
					args.add(a.length() > 256 ? a.substring(0, 256) : a);
				}
			}

			net.sendToPlayer(player, new AdvNet.Answer(ask.id(), result.ok(), result.code(), args, data));
		}
	}

	private static AdvService.Result act(ServerPlayer player, String op, CompoundTag args) {
		MinecraftServer server = BookDoor.server();
		if (server == null || !BookDoor.running()) {
			return AdvService.Result.fail("disabled");
		} else if (!BookDoor.mayEdit(player)) {
			return AdvService.Result.fail("no_permission");
		} else {
			switch (op) {
				case "trees":
					return AdvService.Result.ok(AdvService.trees(server));
				case "preview":
					return AdvService.preview(server, args.getString("root"), options(args));
				case "convert":
					List<String> roots = new ArrayList<>();
					ListTag list = args.getList("roots", 8);

					for (int i = 0; i < list.size() && i < 512; i++) {
						roots.add(list.getString(i));
					}

					return AdvService.convert(server, roots, options(args));
				case "undo":
					return AdvService.undo(server);
				default:
					return AdvService.Result.fail("unknown");
			}
		}
	}

	private static AdvOptions options(CompoundTag args) {
		AdvOptions options = null;
		String json = args.getString("options");
		if (!json.isEmpty() && json.length() < 200000) {
			try {
				options = (AdvOptions)BookJson.GSON.fromJson(json, AdvOptions.class);
			} catch (RuntimeException var4) {
				options = null;
			}
		}

		return (options == null ? new AdvOptions() : options).sanitize();
	}

	private AdvNet() {
	}

	public static record Answer(int id, boolean ok, String code, List<String> args, byte[] data) implements CustomPacketPayload {
		public static final Type<AdvNet.Answer> TYPE = new Type(CFHGui.id("quests_adv_answer"));
		public static final StreamCodec<ByteBuf, AdvNet.Answer> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			AdvNet.Answer::id,
			ByteBufCodecs.BOOL,
			AdvNet.Answer::ok,
			ByteBufCodecs.stringUtf8(64),
			AdvNet.Answer::code,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			AdvNet.Answer::args,
			ByteBufCodecs.byteArray(2097152),
			AdvNet.Answer::data,
			AdvNet.Answer::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Ask(int id, String op, CompoundTag args) implements CustomPacketPayload {
		public static final Type<AdvNet.Ask> TYPE = new Type(CFHGui.id("quests_adv_ask"));
		public static final StreamCodec<ByteBuf, AdvNet.Ask> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AdvNet.Ask::id, ByteBufCodecs.stringUtf8(32), AdvNet.Ask::op, ByteBufCodecs.COMPOUND_TAG, AdvNet.Ask::args, AdvNet.Ask::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class ClientSide {
		static void answer(AdvNet.Answer p) {
			AdvClient.onAnswer(p);
		}
	}
}
