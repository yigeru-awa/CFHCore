package com.onehumanawa.cfhcore.content.gui.earlywindow.scene;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
	private final String text;
	private int at;

	private Json(String text) {
		this.text = text;
	}

	static Object parse(String text) {
		Json j = new Json(text);
		j.space();
		Object value = j.value();
		j.space();
		if (j.at < text.length()) {
			throw j.wrong("something after the end");
		} else {
			return value;
		}
	}

	static Map<String, Object> object(Object value) {
		return value instanceof Map ? (Map)value : Map.of();
	}

	static List<Object> list(Object value) {
		return value instanceof List ? (List)value : List.of();
	}

	static float number(Object value, float fallback) {
		return value instanceof Number n ? n.floatValue() : fallback;
	}

	static String string(Object value, String fallback) {
		return value instanceof String s ? s : fallback;
	}

	private Object value() {
		if (this.at >= this.text.length()) {
			throw this.wrong("nothing where a value should be");
		} else {
			char c = this.text.charAt(this.at);
			if (c == '{') {
				return this.object();
			} else if (c == '[') {
				return this.array();
			} else if (c == '"') {
				return this.string();
			} else if (this.text.startsWith("true", this.at)) {
				this.at += 4;
				return Boolean.TRUE;
			} else if (this.text.startsWith("false", this.at)) {
				this.at += 5;
				return Boolean.FALSE;
			} else if (this.text.startsWith("null", this.at)) {
				this.at += 4;
				return null;
			} else {
				return this.number();
			}
		}
	}

	private Map<String, Object> object() {
		Map<String, Object> map = new LinkedHashMap<>();
		this.at++;
		this.space();
		if (this.peek() == '}') {
			this.at++;
			return map;
		} else {
			do {
				this.space();
				if (this.peek() != '"') {
					throw this.wrong("a name expected");
				}

				String name = this.string();
				this.space();
				if (this.peek() != ':') {
					throw this.wrong("a colon expected");
				}

				this.at++;
				this.space();
				map.put(name, this.value());
				this.space();
				char c = this.peek();
				this.at++;
				if (c == '}') {
					return map;
				}

				if (c != ',') {
					throw this.wrong("a comma expected");
				}

				this.space();
			} while (this.peek() != '}');

			this.at++;
			return map;
		}
	}

	private List<Object> array() {
		List<Object> list = new ArrayList<>();
		this.at++;
		this.space();
		if (this.peek() == ']') {
			this.at++;
			return list;
		} else {
			do {
				this.space();
				list.add(this.value());
				this.space();
				char c = this.peek();
				this.at++;
				if (c == ']') {
					return list;
				}

				if (c != ',') {
					throw this.wrong("a comma expected");
				}

				this.space();
			} while (this.peek() != ']');

			this.at++;
			return list;
		}
	}

	private String string() {
		StringBuilder s = new StringBuilder();
		this.at++;

		while (this.at < this.text.length()) {
			char c = this.text.charAt(this.at++);
			if (c == '"') {
				return s.toString();
			}

			if (c != '\\') {
				s.append(c);
			} else {
				if (this.at >= this.text.length()) {
					break;
				}

				char e = this.text.charAt(this.at++);
				switch (e) {
					case 'b':
						s.append('\b');
						break;
					case 'c':
					case 'd':
					case 'e':
					case 'g':
					case 'h':
					case 'i':
					case 'j':
					case 'k':
					case 'l':
					case 'm':
					case 'o':
					case 'p':
					case 'q':
					case 's':
					default:
						s.append(e);
						break;
					case 'f':
						s.append('\f');
						break;
					case 'n':
						s.append('\n');
						break;
					case 'r':
						s.append('\r');
						break;
					case 't':
						s.append('\t');
						break;
					case 'u':
						if (this.at + 4 > this.text.length()) {
							throw this.wrong("a short escape");
						}

						s.append((char)Integer.parseInt(this.text.substring(this.at, this.at + 4), 16));
						this.at += 4;
				}
			}
		}

		throw this.wrong("a string without an end");
	}

	private Double number() {
		int from = this.at;

		while (this.at < this.text.length() && "+-0123456789.eE".indexOf(this.text.charAt(this.at)) >= 0) {
			this.at++;
		}

		if (this.at == from) {
			throw this.wrong("a value expected");
		} else {
			try {
				return Double.valueOf(this.text.substring(from, this.at));
			} catch (NumberFormatException var3) {
				throw this.wrong("not a number");
			}
		}
	}

	private char peek() {
		return this.at < this.text.length() ? this.text.charAt(this.at) : '\u0000';
	}

	private void space() {
		while (this.at < this.text.length()) {
			char c = this.text.charAt(this.at);
			if (c != ' ' && c != '\n' && c != '\r' && c != '\t' && c != '\ufeff') {
				if (c == '/' && this.at + 1 < this.text.length() && this.text.charAt(this.at + 1) == '/') {
					while (this.at < this.text.length() && this.text.charAt(this.at) != '\n') {
						this.at++;
					}
				} else {
					if (c != '/' || this.at + 1 >= this.text.length() || this.text.charAt(this.at + 1) != '*') {
						return;
					}

					int end = this.text.indexOf("*/", this.at + 2);
					this.at = end < 0 ? this.text.length() : end + 2;
				}
			} else {
				this.at++;
			}
		}
	}

	private IllegalArgumentException wrong(String what) {
		return new IllegalArgumentException(what + " at " + this.at);
	}
}
