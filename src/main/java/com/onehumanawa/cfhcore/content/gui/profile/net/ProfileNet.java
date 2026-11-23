package com.onehumanawa.cfhcore.content.gui.profile.net;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import com.onehumanawa.cfhcore.content.gui.profile.client.ProfileClient;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ProfileNet {
	private static final long MIN_INTERVAL_MS = 750L;
	private static final Map<UUID, ProfileNet.LookData> LOOKS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
	private static final Map<UUID, ProfileNet.LookData> WAITING = new ConcurrentHashMap<>();
	private static boolean registered;

	public static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(ProfileNet.LookUpdate.TYPE, ProfileNet.LookUpdate.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					received(ctx.sender(), p.look());
				}
			});
			net.register(ProfileNet.LookOf.TYPE, ProfileNet.LookOf.CODEC, Flow.S2C, (p, ctx) -> {
				if (CFHGuiPlatform.get().isClient()) {
					ProfileNet.ClientSide.lookOf(p);
				}
			});
			CFHGuiEvents.PLAYER_JOINED.register(ProfileNet::joined);
			CFHGuiEvents.PLAYER_LEFT.register(ProfileNet::left);
			CFHGuiEvents.SERVER_TICK_END.register(ProfileNet::tick);
			CFHGuiEvents.SERVER_STOPPING.register((Consumer<MinecraftServer>)server -> {
				LOOKS.clear();
				LAST.clear();
				WAITING.clear();
			});
		}
	}

	public static int held() {
		return LOOKS.size();
	}

	private static void received(ServerPlayer sender, ProfileNet.LookData look) {
		UUID id = sender.getUUID();
		long now = System.currentTimeMillis();
		Long before = LAST.get(id);
		if (before != null && now - before < 750L) {
			WAITING.put(id, look);
		} else {
			take(sender, look, now);
		}
	}

	private static void tick(MinecraftServer server) {
		if (!WAITING.isEmpty()) {
			long now = System.currentTimeMillis();

			for (UUID id : new ArrayList<>(WAITING.keySet())) {
				Long before = LAST.get(id);
				if (before == null || now - before >= 750L) {
					ProfileNet.LookData look = WAITING.remove(id);
					ServerPlayer player = server.getPlayerList().getPlayer(id);
					if (look != null && player != null) {
						take(player, look, now);
					}
				}
			}
		}
	}

	private static void take(ServerPlayer sender, ProfileNet.LookData look, long now) {
		UUID id = sender.getUUID();
		LAST.put(id, now);
		WAITING.remove(id);
		if (!look.valid()) {
			CFHGuiProfile.LOGGER
				.warn(
					"[CFHGui Profile] {} sent a look that is not one ({} bytes, {} models): dropped",
					new Object[]{sender.getGameProfile().getName(), look.skin().length, look.models().size()}
				);
		} else {
			if (LOOKS.put(id, look) == null) {
				CFHGuiProfile.LOGGER
					.info(
						"[CFHGui Profile] {} wears a look ({} bytes of skin, {} thing(s) in 3D)",
						new Object[]{sender.getGameProfile().getName(), look.skin().length, look.models().size()}
					);
			}

			tellOthers(sender, new ProfileNet.LookOf(id, Optional.of(look)));
		}
	}

	private static void tellOthers(ServerPlayer about, ProfileNet.LookOf message) {
		CFHGuiNetwork net = CFHGuiNetwork.get();

		for (ServerPlayer other : new ArrayList<ServerPlayer>(about.server.getPlayerList().getPlayers())) {
			if (!other.getUUID().equals(about.getUUID()) && net.canSendToPlayer(other, ProfileNet.LookOf.TYPE)) {
				net.sendToPlayer(other, message);
			}
		}
	}

	private static void joined(ServerPlayer player) {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.canSendToPlayer(player, ProfileNet.LookOf.TYPE)) {
			for (ServerPlayer other : new ArrayList<ServerPlayer>(player.server.getPlayerList().getPlayers())) {
				if (!other.getUUID().equals(player.getUUID())) {
					ProfileNet.LookData look = LOOKS.get(other.getUUID());
					if (look != null) {
						net.sendToPlayer(player, new ProfileNet.LookOf(other.getUUID(), Optional.of(look)));
					}
				}
			}
		}
	}

	private static void left(ServerPlayer player) {
		LAST.remove(player.getUUID());
		WAITING.remove(player.getUUID());
		if (LOOKS.remove(player.getUUID()) != null) {
			tellOthers(player, new ProfileNet.LookOf(player.getUUID(), Optional.empty()));
		}
	}

	private ProfileNet() {
	}

	private static final class ClientSide {
		static void lookOf(ProfileNet.LookOf message) {
			ProfileClient.lookOf(message);
		}
	}

	public static record LookData(byte[] skin, boolean slim, List<String> models) {
		public static final int MAX_SKIN_BYTES = 49152;
		public static final int MAX_MODELS = 16;
		public static final int MAX_ID = 64;
		public static final StreamCodec<ByteBuf, ProfileNet.LookData> CODEC = StreamCodec.composite(
			ByteBufCodecs.byteArray(49152),
			ProfileNet.LookData::skin,
			ByteBufCodecs.BOOL,
			ProfileNet.LookData::slim,
			ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(16)),
			ProfileNet.LookData::models,
			ProfileNet.LookData::new
		);

		public LookData(byte[] skin, boolean slim, List<String> models) {
			if (skin == null) {
				skin = new byte[0];
			}

			models = models == null ? List.of() : List.copyOf(models);
			this.skin = skin;
			this.slim = slim;
			this.models = models;
		}

		public boolean valid() {
			if (this.skin.length >= 33 && this.skin.length <= 49152 && this.models.size() <= 16) {
				int[] signature = new int[]{137, 80, 78, 71, 13, 10, 26, 10};

				for (int i = 0; i < signature.length; i++) {
					if ((this.skin[i] & 255) != signature[i]) {
						return false;
					}
				}

				if (this.skin[12] == 73 && this.skin[13] == 72 && this.skin[14] == 68 && this.skin[15] == 82) {
					int width = (this.skin[16] & 255) << 24 | (this.skin[17] & 255) << 16 | (this.skin[18] & 255) << 8 | this.skin[19] & 255;
					int height = (this.skin[20] & 255) << 24 | (this.skin[21] & 255) << 16 | (this.skin[22] & 255) << 8 | this.skin[23] & 255;
					if (width == 64 && height == 64) {
						for (String id : this.models) {
							if (id == null || id.isEmpty() || id.length() > 64) {
								return false;
							}

							for (int ix = 0; ix < id.length(); ix++) {
								char c = id.charAt(ix);
								if ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_' && c != ':' && c != '/' && c != '.' && c != '-') {
									return false;
								}
							}
						}

						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	public static record LookOf(UUID player, Optional<ProfileNet.LookData> look) implements CustomPacketPayload {
		public static final Type<ProfileNet.LookOf> TYPE = new Type(CFHGui.id("profile_look_of"));
		public static final StreamCodec<ByteBuf, ProfileNet.LookOf> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, ProfileNet.LookOf::player, ByteBufCodecs.optional(ProfileNet.LookData.CODEC), ProfileNet.LookOf::look, ProfileNet.LookOf::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record LookUpdate(ProfileNet.LookData look) implements CustomPacketPayload {
		public static final Type<ProfileNet.LookUpdate> TYPE = new Type(CFHGui.id("profile_look"));
		public static final StreamCodec<ByteBuf, ProfileNet.LookUpdate> CODEC = ProfileNet.LookData.CODEC
			.map(ProfileNet.LookUpdate::new, ProfileNet.LookUpdate::look);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
