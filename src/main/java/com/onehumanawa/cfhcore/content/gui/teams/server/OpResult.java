package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.MutableComponent;

public record OpResult(boolean ok, String code, List<String> args) {
	private static final int MAX_ARGS = 8;
	private static final int MAX_ARG = 256;
	public static final OpResult OK = new OpResult(true, "ok", List.of());

	public static OpResult ok(Object... args) {
		return new OpResult(true, "ok", strings(args));
	}

	public static OpResult fail(String code, Object... args) {
		return new OpResult(false, code, strings(args));
	}

	public TeamsNet.Result payload(int request) {
		return new TeamsNet.Result(request, this.ok, this.code, this.args);
	}

	public MutableComponent message() {
		return Msg.tr("cfhgui_teams.result." + this.code, this.args.toArray());
	}

	public int number(int index) {
		if (index >= 0 && index < this.args.size()) {
			try {
				return Integer.parseInt(this.args.get(index));
			} catch (NumberFormatException var3) {
				return 0;
			}
		} else {
			return 0;
		}
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
