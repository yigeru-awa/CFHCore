package com.onehumanawa.cfhcore.content.gui.server.model;

public final class Place {
	public String dimension = "minecraft:overworld";
	public double x;
	public double y;
	public double z;
	public float yaw;
	public float pitch;

	public Place() {
	}

	public Place(String dimension, double x, double y, double z, float yaw, float pitch) {
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public int blockX() {
		return (int)Math.floor(this.x);
	}

	public int blockY() {
		return (int)Math.floor(this.y);
	}

	public int blockZ() {
		return (int)Math.floor(this.z);
	}

	public Place copy() {
		return new Place(this.dimension, this.x, this.y, this.z, this.yaw, this.pitch);
	}

	public Place sanitize() {
		if (this.dimension == null || this.dimension.isBlank()) {
			this.dimension = "minecraft:overworld";
		}

		if (!Double.isFinite(this.x)) {
			this.x = 0.0;
		}

		if (!Double.isFinite(this.y)) {
			this.y = 64.0;
		}

		if (!Double.isFinite(this.z)) {
			this.z = 0.0;
		}

		if (!Float.isFinite(this.yaw)) {
			this.yaw = 0.0F;
		}

		if (!Float.isFinite(this.pitch)) {
			this.pitch = 0.0F;
		}

		return this;
	}
}
