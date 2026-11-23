package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class TreePaths {
	public static final char SEP = '\n';
	public static final String GOAL = "";

	public static String child(String parent, String key) {
		return parent + "\n" + key.replace('\n', ' ');
	}

	@Nullable
	public static String parent(String path) {
		int cut = path.lastIndexOf(10);
		return cut < 0 ? null : path.substring(0, cut);
	}

	public static int depth(String path) {
		int n = 0;

		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '\n') {
				n++;
			}
		}

		return n;
	}

	public static String last(String path) {
		return path.substring(path.lastIndexOf(10) + 1);
	}

	public static boolean at(String above, String path) {
		return path.length() == above.length() ? path.equals(above) : under(above, path);
	}

	public static boolean under(String above, String path) {
		return path.length() > above.length() && path.charAt(above.length()) == '\n' && path.startsWith(above);
	}

	public static String common(String a, String b) {
		int n = Math.min(a.length(), b.length());
		int end = 0;

		for (int i = 0; i <= n; i++) {
			boolean aEnds = i == a.length() || a.charAt(i) == '\n';
			boolean bEnds = i == b.length() || b.charAt(i) == '\n';
			if (aEnds && bEnds) {
				end = i;
			}

			if (i == n || a.charAt(i) != b.charAt(i)) {
				break;
			}
		}

		return a.substring(0, end);
	}

	public static List<String> keys(String path) {
		List<String> out = new ArrayList<>();
		int from = 1;

		while (from <= path.length()) {
			int to = path.indexOf(10, from);
			if (to < 0) {
				to = path.length();
			}

			out.add(path.substring(from, to));
			from = to + 1;
		}

		return out;
	}

	public static String of(List<String> keys) {
		StringBuilder b = new StringBuilder();

		for (String key : keys) {
			b.append('\n').append(key.replace('\n', ' '));
		}

		return b.toString();
	}

	private TreePaths() {
	}
}
