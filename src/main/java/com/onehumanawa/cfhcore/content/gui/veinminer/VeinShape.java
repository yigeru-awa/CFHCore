package com.onehumanawa.cfhcore.content.gui.veinminer;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum VeinShape {
	SHAPELESS("shapeless", Icon.SPARKLE),
	TUNNEL_1X2("tunnel_1x2", Icon.SHAPE_VSLAB),
	TUNNEL_3X3("tunnel_3x3", Icon.SQUARE),
	STAIRCASE("staircase", Icon.SHAPE_STAIRS),
	SQUARE_3X3("square_3x3", Icon.GRID),
	COLUMN("column", Icon.SHAPE_PILLAR);

	private static final VeinShape[] VALUES = values();
	private final String id;
	private final Icon icon;

	private VeinShape(final String id, final Icon icon) {
		this.id = id;
		this.icon = icon;
	}

	public String id() {
		return this.id;
	}

	public Icon icon() {
		return this.icon;
	}

	public Component label() {
		return Component.translatable("cfhgui_veinminer.shape." + this.id);
	}

	public Component description() {
		return Component.translatable("cfhgui_veinminer.shape." + this.id + ".tip");
	}

	public static VeinShape byOrdinal(int ordinal) {
		return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : SHAPELESS;
	}

	public static VeinShape parse(String s, VeinShape fallback) {
		if (s == null) {
			return fallback;
		} else {
			String t = s.trim().toLowerCase(Locale.ROOT);

			for (VeinShape v : VALUES) {
				if (v.id.equals(t) || v.name().toLowerCase(Locale.ROOT).equals(t)) {
					return v;
				}
			}

			return fallback;
		}
	}

	public static VeinShape[] all() {
		return (VeinShape[])VALUES.clone();
	}

	public static int count() {
		return VALUES.length;
	}
}
