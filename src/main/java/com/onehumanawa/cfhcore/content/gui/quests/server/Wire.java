package com.onehumanawa.cfhcore.content.gui.quests.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.quests.model.BookJson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;

final class Wire {
	private static final int STORED_UP_TO = 4096;

	static byte[] pack(Object value) {
		byte[] json = BookJson.GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
		return json.length <= 4096 ? stored(json) : deflated(json);
	}

	static byte[] stored(byte[] data) {
		int n = data.length;
		byte[] out = new byte[15 + n + 8];
		out[0] = 31;
		out[1] = -117;
		out[2] = 8;
		out[9] = -1;
		int at = 10;
		out[at++] = 1;
		out[at++] = (byte)n;
		out[at++] = (byte)(n >>> 8);
		out[at++] = (byte)(~n);
		out[at++] = (byte)(~n >>> 8);
		System.arraycopy(data, 0, out, at, n);
		at += n;
		CRC32 crc = new CRC32();
		crc.update(data, 0, n);
		long sum = crc.getValue();

		for (int i = 0; i < 4; i++) {
			out[at++] = (byte)((int)(sum >>> 8 * i));
		}

		for (int i = 0; i < 4; i++) {
			out[at++] = (byte)(n >>> 8 * i);
		}

		return out;
	}

	private static byte[] deflated(byte[] data) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 4 + 64);

		try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
			gz.write(data);
		} catch (IOException var7) {
			throw new IllegalStateException("cannot pack progress", var7);
		}

		return out.toByteArray();
	}

	static <T> T unpack(byte[] bytes, Class<T> type) {
		return (T)JsonWire.decode(BookJson.GSON, bytes, type);
	}

	private Wire() {
	}
}
