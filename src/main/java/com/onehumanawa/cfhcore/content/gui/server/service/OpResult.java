package com.onehumanawa.cfhcore.content.gui.server.service;

import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public record OpResult(boolean ok, String code, List<String> args, @Nullable Object data) {
	private static final int MAX_ARGS = 8;
	private static final int MAX_ARG = 256;
	public static final OpResult OK = new OpResult(true, "ok", List.of(), null);

	public static OpResult ok(Object... args) {
		return new OpResult(true, "ok", strings(args), null);
	}

	public static OpResult fail(String code, Object... args) {
		return new OpResult(false, code, strings(args), null);
	}

	public static OpResult warmup(int seconds) {
		return new OpResult(true, "warmup", List.of(Integer.toString(seconds)), null);
	}

	public static OpResult answer(Object data) {
		return new OpResult(true, "ok", List.of(), data);
	}

	public ServerNet.Result payload(int request) {
		return new ServerNet.Result(request, this.ok, this.code, this.args);
	}

	public MutableComponent message() {
		return Msg.tr("cfhgui_server.result." + this.code, this.args.toArray());
	}

	public String arg(int index) {
		return index >= 0 && index < this.args.size() ? this.args.get(index) : "";
	}

	@Nullable
	public <T> T data(Class<T> type) {
		return type.isInstance(this.data) ? type.cast(this.data) : null;
	}

	private static List<String> strings(Object[] args) {
		if (args.length == 0) {
			return List.of();
		} else {
			List<String> out = new ArrayList<>(Math.min(args.length, 8));

			for (Object a : args) {
				if (out.size() >= 8) {
					break;
				}

				String s = String.valueOf(a);
				out.add(s.length() > 256 ? s.substring(0, 256) : s);
			}

			return List.copyOf(out);
		}
	}
}
