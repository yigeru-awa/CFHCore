package com.onehumanawa.cfhcore.content.gui.teams.model;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TeammateState(UUID id, String dimension, float x, float y, float z, float yaw, float health, float maxHealth, int armor, int food, int flags) {
	public static final int DEAD = 1;
	public static final int SNEAKING = 2;
	public static final int ALLY = 4;
	public static final StreamCodec<ByteBuf, TeammateState> CODEC = new StreamCodec<ByteBuf, TeammateState>() {
		public TeammateState decode(ByteBuf buf) {
			return new TeammateState(
				(UUID)UUIDUtil.STREAM_CODEC.decode(buf),
				(String)ByteBufCodecs.stringUtf8(128).decode(buf),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readFloat(),
				buf.readUnsignedByte(),
				buf.readUnsignedByte(),
				buf.readUnsignedByte()
			);
		}

		public void encode(ByteBuf buf, TeammateState s) {
			UUIDUtil.STREAM_CODEC.encode(buf, s.id);
			ByteBufCodecs.stringUtf8(128).encode(buf, s.dimension);
			buf.writeFloat(s.x).writeFloat(s.y).writeFloat(s.z).writeFloat(s.yaw).writeFloat(s.health).writeFloat(s.maxHealth);
			buf.writeByte(Math.max(0, Math.min(255, s.armor))).writeByte(Math.max(0, Math.min(255, s.food))).writeByte(s.flags & 0xFF);
		}
	};

	public boolean dead() {
		return (this.flags & 1) != 0;
	}

	public boolean sneaking() {
		return (this.flags & 2) != 0;
	}

	public boolean ally() {
		return (this.flags & 4) != 0;
	}
}
