package com.onehumanawa.cfhcore.content.gui.teams.model;

import java.util.ArrayList;
import java.util.List;

public final class BannerData {
	public static final int MAX_LAYERS = 6;
	public String base = "white";
	public List<BannerData.Layer> layers = new ArrayList<>();

	public BannerData() {
	}

	public BannerData(String base) {
		this.base = base;
	}

	public BannerData layer(String pattern, String color) {
		if (this.layers.size() < 6) {
			this.layers.add(new BannerData.Layer(pattern, color));
		}

		return this;
	}

	public void sanitize() {
		if (this.base == null || this.base.isBlank()) {
			this.base = "white";
		}

		if (this.layers == null) {
			this.layers = new ArrayList<>();
		}

		this.layers.removeIf(l -> l == null || l.pattern == null || l.color == null);

		while (this.layers.size() > 6) {
			this.layers.remove(this.layers.size() - 1);
		}
	}

	public String signature() {
		StringBuilder sb = new StringBuilder(this.base);

		for (BannerData.Layer l : this.layers) {
			sb.append('|').append(l.pattern).append('@').append(l.color);
		}

		return sb.toString();
	}

	public static final class Layer {
		public String pattern = "minecraft:base";
		public String color = "black";

		public Layer() {
		}

		public Layer(String pattern, String color) {
			this.pattern = pattern;
			this.color = color;
		}
	}
}
