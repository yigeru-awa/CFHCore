package com.onehumanawa.cfhcore.content.gui.shops.service;

import com.onehumanawa.cfhcore.content.gui.shops.net.ShopsNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.MutableComponent;

public record OpResult(boolean ok, String code, List<String> args) {
	private static final int MAX_ARGS = 8;
	private static final int MAX_ARG = 256;
	public static final OpResult DONE = new OpResult(true, "ok", List.of());

	public static OpResult ok(Object... args) {
		return new OpResult(true, "ok", strings(args));
	}

	public static OpResult fail(String code, Object... args) {
		return new OpResult(false, code, strings(args));
	}

	public ShopsNet.Result payload(int request) {
		return new ShopsNet.Result(request, this.ok, this.code, this.args);
	}

	public MutableComponent message(String op) {
		return this.ok ? Msg.tr("cfhgui_shops.done." + op, this.args.toArray()) : Msg.tr("cfhgui_shops.result." + this.code, this.args.toArray());
	}

	public String arg(int index) {
		return index >= 0 && index < this.args.size() ? this.args.get(index) : "";
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
