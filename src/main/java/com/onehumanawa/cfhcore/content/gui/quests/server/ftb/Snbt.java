package com.onehumanawa.cfhcore.content.gui.quests.server.ftb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class Snbt {
	private final String text;
	private int at;

	private Snbt(String text) {
		this.text = text;
	}

	public static Object parse(String text) {
		Snbt reader = new Snbt(text);
		reader.gap();
		Object value = reader.value();
		reader.gap();
		if (reader.at < text.length()) {
			throw new Snbt.SyntaxException("something is left after the end of the value", text, reader.at);
		} else {
			return value;
		}
	}

	private void gap() {
		while (this.at < this.text.length()) {
			char c = this.text.charAt(this.at);
			if (c == ',' || Character.isWhitespace(c) || c == '\ufeff') {
				this.at++;
				continue;
			}
			break;
		}
	}

	private Object value() {
		if (this.at >= this.text.length()) {
			throw new Snbt.SyntaxException("a value is missing", this.text, this.at);
		} else {
			char c = this.text.charAt(this.at);
			if (c == '{') {
				return this.compound();
			} else if (c == '[') {
				return this.list();
			} else {
				return c != '"' && c != '\'' ? this.bare() : this.quoted();
			}
		}
	}

	private Map<String, Object> compound() {
		int start = this.at++;
		Map<String, Object> out = new LinkedHashMap<>();

		while (true) {
			this.gap();
			if (this.at >= this.text.length()) {
				throw new Snbt.SyntaxException("a { is never closed", this.text, start);
			}

			if (this.text.charAt(this.at) == '}') {
				this.at++;
				return out;
			}

			String key = this.key();
			this.gap();
			if (this.at >= this.text.length() || this.text.charAt(this.at) != ':') {
				throw new Snbt.SyntaxException("the key \"" + key + "\" has no value", this.text, this.at);
			}

			this.at++;
			this.gap();
			out.put(key, this.value());
		}
	}

	private String key() {
		char c = this.text.charAt(this.at);
		if (c != '"' && c != '\'') {
			int start;
			for (start = this.at; this.at < this.text.length(); this.at++) {
				char k = this.text.charAt(this.at);
				if (k == ':' || Character.isWhitespace(k) || k == '}' || k == ',') {
					break;
				}
			}

			if (this.at == start) {
				throw new Snbt.SyntaxException("a key is missing", this.text, this.at);
			} else {
				return this.text.substring(start, this.at);
			}
		} else {
			return this.quoted();
		}
	}

	private List<Object> list() {
		int start = this.at++;
		if (this.at + 1 < this.text.length() && this.text.charAt(this.at + 1) == ';' && "BILbil".indexOf(this.text.charAt(this.at)) >= 0) {
			this.at += 2;
		}

		List<Object> out = new ArrayList<>();

		while (true) {
			this.gap();
			if (this.at >= this.text.length()) {
				throw new Snbt.SyntaxException("a [ is never closed", this.text, start);
			}

			if (this.text.charAt(this.at) == ']') {
				this.at++;
				return out;
			}

			out.add(this.value());
		}
	}

	private String quoted() {
		int start = this.at;
		char quote = this.text.charAt(this.at++);
		StringBuilder sb = new StringBuilder();

		while (this.at < this.text.length()) {
			char c = this.text.charAt(this.at++);
			if (c == quote) {
				return sb.toString();
			}

			if (c == '\\' && this.at < this.text.length()) {
				char e = this.text.charAt(this.at++);
				switch (e) {
					case 'n':
						sb.append('\n');
					case 'r':
						break;
					case 't':
						sb.append('\t');
						break;
					default:
						sb.append(e);
				}
			} else {
				sb.append(c);
			}
		}

		throw new Snbt.SyntaxException("a string is never closed", this.text, start);
	}

	private Object bare() {
		int start;
		for (start = this.at; this.at < this.text.length(); this.at++) {
			char c = this.text.charAt(this.at);
			if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
				break;
			}
		}

		if (this.at == start) {
			throw new Snbt.SyntaxException("a value is missing", this.text, this.at);
		} else {
			String word = this.text.substring(start, this.at);
			if (word.equals("true")) {
				return Boolean.TRUE;
			} else if (word.equals("false")) {
				return Boolean.FALSE;
			} else {
				Snbt.Num number = number(word);
				return number != null ? number : word;
			}
		}
	}

	@Nullable
	private static Snbt.Num number(String word) {
		String digits = word;
		char last = Character.toLowerCase(word.charAt(word.length() - 1));
		if (word.length() > 1 && "bslfd".indexOf(last) >= 0) {
			digits = word.substring(0, word.length() - 1);
		}

		if (digits.isEmpty()) {
			return null;
		} else {
			char first = digits.charAt(0);
			if ((first < '0' || first > '9') && first != '-' && first != '+' && first != '.') {
				return null;
			} else {
				try {
					return new Snbt.Num(word, Double.parseDouble(digits));
				} catch (NumberFormatException var5) {
					return null;
				}
			}
		}
	}

	public static Map<String, Object> map(@Nullable Object value) {
		return value instanceof Map ? (Map)value : Map.of();
	}

	public static List<Object> list(@Nullable Object value) {
		return value instanceof List ? (List)value : List.of();
	}

	public static String string(Map<String, Object> map, String key, String fallback) {
		Object v = map.get(key);
		if (v instanceof String) {
			return (String)v;
		} else {
			return v instanceof Snbt.Num n ? n.raw() : fallback;
		}
	}

	public static long number(Map<String, Object> map, String key, long fallback) {
		Object v = map.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		} else if (v instanceof Boolean b) {
			return b ? 1L : 0L;
		} else {
			return fallback;
		}
	}

	public static double decimal(Map<String, Object> map, String key, double fallback) {
		return map.get(key) instanceof Number n ? n.doubleValue() : fallback;
	}

	public static boolean flag(Map<String, Object> map, String key, boolean fallback) {
		Object v = map.get(key);
		if (v instanceof Boolean b) {
			return b;
		} else {
			return v instanceof Number n ? n.doubleValue() != 0.0 : fallback;
		}
	}

	public static String write(@Nullable Object value) {
		StringBuilder sb = new StringBuilder();
		write(sb, value);
		return sb.toString();
	}

	private static void write(StringBuilder sb, @Nullable Object value) {
		if (value instanceof Map<?, ?> map) {
			sb.append('{');
			boolean first = true;

			for (Entry<?, ?> e : map.entrySet()) {
				if (!first) {
					sb.append(',');
				}

				first = false;
				String key = String.valueOf(e.getKey());
				if (plain(key)) {
					sb.append(key);
				} else {
					quote(sb, key);
				}

				sb.append(':');
				write(sb, e.getValue());
			}

			sb.append('}');
		} else if (value instanceof List<?> list) {
			sb.append('[');

			for (int i = 0; i < list.size(); i++) {
				if (i > 0) {
					sb.append(',');
				}

				write(sb, list.get(i));
			}

			sb.append(']');
		} else if (value instanceof Snbt.Num n) {
			sb.append(n.raw());
		} else if (value instanceof Boolean b) {
			sb.append(b ? "true" : "false");
		} else {
			quote(sb, String.valueOf(value));
		}
	}

	private static boolean plain(String key) {
		if (key.isEmpty()) {
			return false;
		} else {
			for (int i = 0; i < key.length(); i++) {
				char c = key.charAt(i);
				if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_' && c != '-' && c != '.' && c != '+') {
					return false;
				}
			}

			return true;
		}
	}

	private static void quote(StringBuilder sb, String s) {
		sb.append('"');

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"' || c == '\\') {
				sb.append('\\');
			}

			sb.append(c);
		}

		sb.append('"');
	}

	public static final class Num extends Number {
		private final String raw;
		private final double value;

		Num(String raw, double value) {
			this.raw = raw;
			this.value = value;
		}

		public String raw() {
			return this.raw;
		}

		@Override
		public int intValue() {
			return (int)this.longValue();
		}

		@Override
		public long longValue() {
			try {
				return Long.parseLong(digits(this.raw));
			} catch (NumberFormatException var2) {
				return (long)this.value;
			}
		}

		@Override
		public float floatValue() {
			return (float)this.value;
		}

		@Override
		public double doubleValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return this.raw;
		}

		private static String digits(String raw) {
			char last = raw.isEmpty() ? 48 : Character.toLowerCase(raw.charAt(raw.length() - 1));
			return last != 'l' && last != 'b' && last != 's' ? raw : raw.substring(0, raw.length() - 1);
		}
	}

	public static final class SyntaxException extends RuntimeException {
		SyntaxException(String message, String text, int at) {
			super(message + " at line " + line(text, at));
		}

		private static int line(String text, int at) {
			int line = 1;

			for (int i = 0; i < at && i < text.length(); i++) {
				if (text.charAt(i) == '\n') {
					line++;
				}
			}

			return line;
		}
	}
}
