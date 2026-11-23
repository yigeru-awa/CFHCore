package com.onehumanawa.cfhcore.content.gui.teams.model;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PingData(UUID by, String dimension, double x, double y, double z, int kind, int entity) {
	public static final int KIND_HERE = 0;
	public static final int KIND_DANGER = 1;
	public static final int KIND_LOOT = 2;
	public static final int KIND_GOING = 3;
	public static final int KIND_HELP = 4;
	public static final int KINDS = 5;
	public static final long LIFE_MS = 9000L;
	public static final StreamCodec<ByteBuf, PingData> CODEC = new StreamCodec<ByteBuf, PingData>() {
		public PingData decode(ByteBuf buf) {
			return new PingData(
				(UUID)UUIDUtil.STREAM_CODEC.decode(buf),
				(String)ByteBufCodecs.stringUtf8(128).decode(buf),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readUnsignedByte(),
				(Integer)ByteBufCodecs.VAR_INT.decode(buf) - 1
			);
		}

		public void encode(ByteBuf buf, PingData p) {
			UUIDUtil.STREAM_CODEC.encode(buf, p.by);
			ByteBufCodecs.stringUtf8(128).encode(buf, p.dimension);
			buf.writeDouble(p.x).writeDouble(p.y).writeDouble(p.z).writeByte(p.kind & 0xFF);
			ByteBufCodecs.VAR_INT.encode(buf, Math.max(-1, p.entity) + 1);
		}
	};

	public static String kindKey(int kind) {
		return switch (kind) {
			case 1 -> "danger";
			case 2 -> "loot";
			case 3 -> "going";
			case 4 -> "help";
			default -> "here";
		};
	}
}
