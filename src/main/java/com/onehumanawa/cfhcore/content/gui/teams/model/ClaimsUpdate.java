package com.onehumanawa.cfhcore.content.gui.teams.model;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ClaimsUpdate(String dimension, boolean reset, List<UUID> teams, long[] chunks, int[] owners, byte[] flags, long[] removed) {
	public static final int FORCED = 1;
	private static final int MAX_ENTRIES = 1048576;
	private static final int MAX_TEAMS = 65536;
	public static final StreamCodec<ByteBuf, ClaimsUpdate> CODEC = new StreamCodec<ByteBuf, ClaimsUpdate>() {
		public ClaimsUpdate decode(ByteBuf buf) {
			String dimension = (String)ByteBufCodecs.stringUtf8(128).decode(buf);
			boolean reset = buf.readBoolean();
			int teamCount = ClaimsUpdate.bounded((Integer)ByteBufCodecs.VAR_INT.decode(buf), 65536);
			List<UUID> teams = new ArrayList<>(teamCount);

			for (int i = 0; i < teamCount; i++) {
				teams.add((UUID)UUIDUtil.STREAM_CODEC.decode(buf));
			}

			int n = ClaimsUpdate.bounded((Integer)ByteBufCodecs.VAR_INT.decode(buf), 1048576);
			long[] chunks = new long[n];
			int[] owners = new int[n];
			byte[] flags = new byte[n];

			for (int i = 0; i < n; i++) {
				chunks[i] = buf.readLong();
				owners[i] = (Integer)ByteBufCodecs.VAR_INT.decode(buf);
				flags[i] = buf.readByte();
				if (owners[i] < 0 || owners[i] >= teamCount) {
					throw new DecoderException("claim names team " + owners[i] + " of " + teamCount);
				}
			}

			int r = ClaimsUpdate.bounded((Integer)ByteBufCodecs.VAR_INT.decode(buf), 1048576);
			long[] removed = new long[r];

			for (int ix = 0; ix < r; ix++) {
				removed[ix] = buf.readLong();
			}

			return new ClaimsUpdate(dimension, reset, teams, chunks, owners, flags, removed);
		}

		public void encode(ByteBuf buf, ClaimsUpdate u) {
			ByteBufCodecs.stringUtf8(128).encode(buf, u.dimension);
			buf.writeBoolean(u.reset);
			ByteBufCodecs.VAR_INT.encode(buf, u.teams.size());

			for (UUID t : u.teams) {
				UUIDUtil.STREAM_CODEC.encode(buf, t);
			}

			ByteBufCodecs.VAR_INT.encode(buf, u.chunks.length);

			for (int i = 0; i < u.chunks.length; i++) {
				buf.writeLong(u.chunks[i]);
				ByteBufCodecs.VAR_INT.encode(buf, u.owners[i]);
				buf.writeByte(u.flags[i]);
			}

			ByteBufCodecs.VAR_INT.encode(buf, u.removed.length);

			for (long c : u.removed) {
				buf.writeLong(c);
			}
		}
	};

	private static int bounded(int n, int max) {
		if (n >= 0 && n <= max) {
			return n;
		} else {
			throw new DecoderException("claims update of " + n + " entries");
		}
	}
}
