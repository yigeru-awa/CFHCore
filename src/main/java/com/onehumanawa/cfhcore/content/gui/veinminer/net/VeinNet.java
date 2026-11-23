package com.onehumanawa.cfhcore.content.gui.veinminer.net;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.veinminer.server.VeinServer;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import org.jetbrains.annotations.Nullable;

public final class VeinNet {
	@Nullable
	private static volatile Consumer<VeinNet.Rules> clientRules;

	public static void onClientRules(Consumer<VeinNet.Rules> handler) {
		clientRules = handler;
	}

	public static void register() {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		net.register(VeinNet.State.TYPE, VeinNet.State.CODEC, Flow.C2S, (p, ctx) -> {
			if (ctx.sender() != null) {
				VeinServer.onState(ctx.sender(), p);
			}
		});
		net.register(VeinNet.Rules.TYPE, VeinNet.Rules.CODEC, Flow.S2C, (p, ctx) -> {
			Consumer<VeinNet.Rules> h = clientRules;
			if (h != null) {
				h.accept(p);
			}
		});
	}

	private VeinNet() {
	}

	public static record Rules(byte[] data, boolean allowed) implements CustomPacketPayload {
		public static final Type<VeinNet.Rules> TYPE = new Type(CFHGui.id("veinminer/rules"));
		public static final StreamCodec<ByteBuf, VeinNet.Rules> CODEC = StreamCodec.composite(
			ByteBufCodecs.byteArray(65536), VeinNet.Rules::data, ByteBufCodecs.BOOL, VeinNet.Rules::allowed, VeinNet.Rules::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record State(boolean active, int shape, boolean diagonals, boolean treeDiagonals, int facing, boolean lookingUp) implements CustomPacketPayload {
		public static final Type<VeinNet.State> TYPE = new Type(CFHGui.id("veinminer/state"));
		public static final StreamCodec<ByteBuf, VeinNet.State> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			VeinNet.State::active,
			ByteBufCodecs.VAR_INT,
			VeinNet.State::shape,
			ByteBufCodecs.BOOL,
			VeinNet.State::diagonals,
			ByteBufCodecs.BOOL,
			VeinNet.State::treeDiagonals,
			ByteBufCodecs.VAR_INT,
			VeinNet.State::facing,
			ByteBufCodecs.BOOL,
			VeinNet.State::lookingUp,
			VeinNet.State::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
