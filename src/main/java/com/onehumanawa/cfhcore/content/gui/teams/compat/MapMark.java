package com.onehumanawa.cfhcore.content.gui.teams.compat;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class MapMark {
	public final MapMark.Kind kind;
	public final String key;
	String dimension = "minecraft:overworld";
	double x;
	double y;
	double z;
	private double fromX;
	private double fromZ;
	private long movedAt;
	private long glideMs;
	Component label = Component.empty();
	int color = 16777215;
	@Nullable
	Icon icon;
	@Nullable
	UUID player;
	@Nullable
	Player entity;
	boolean ally;
	boolean dead;
	long bornMs;
	long lifeMs;

	MapMark(MapMark.Kind kind, String key) {
		this.kind = kind;
		this.key = key;
	}

	public String dimension() {
		return this.dimension;
	}

	public Component label() {
		return this.label;
	}

	public int color() {
		return this.color;
	}

	@Nullable
	public Icon icon() {
		return this.icon;
	}

	@Nullable
	public UUID player() {
		return this.player;
	}

	public boolean ally() {
		return this.ally;
	}

	public boolean dead() {
		return this.dead;
	}

	public double x() {
		return this.x;
	}

	public double y() {
		return this.y;
	}

	public double z() {
		return this.z;
	}

	public double drawX(float partialTick) {
		Player p = this.loadedPlayer();
		return p != null ? Mth.lerp((double)partialTick, p.xo, p.getX()) : this.glide(this.fromX, this.x);
	}

	public double drawZ(float partialTick) {
		Player p = this.loadedPlayer();
		return p != null ? Mth.lerp((double)partialTick, p.zo, p.getZ()) : this.glide(this.fromZ, this.z);
	}

	public float age() {
		return this.bornMs > 0L && this.lifeMs > 0L ? Mth.clamp((float)(System.currentTimeMillis() - this.bornMs) / (float)this.lifeMs, 0.0F, 1.0F) : 0.0F;
	}

	public ResourceLocation skin() {
		UUID id = this.player == null ? new UUID(0L, 0L) : this.player;
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			PlayerInfo info = connection.getPlayerInfo(id);
			if (info != null) {
				return info.getSkin().texture();
			}
		}

		return DefaultPlayerSkin.get(id).texture();
	}

	@Nullable
	private Player loadedPlayer() {
		Player p = this.entity;
		return p != null && !p.isRemoved() ? p : null;
	}

	private double glide(double from, double to) {
		if (this.glideMs <= 0L) {
			return to;
		} else {
			float t = (float)(System.currentTimeMillis() - this.movedAt) / (float)this.glideMs;
			return t >= 1.0F ? to : Mth.lerp((double)Math.max(0.0F, t), from, to);
		}
	}

	void moveTo(String dimension, double x, double y, double z, long overMs) {
		boolean jump = overMs <= 0L || !dimension.equals(this.dimension) || Math.abs(x - this.x) > 96.0 || Math.abs(z - this.z) > 96.0;
		if (jump) {
			this.glideMs = 0L;
		} else {
			this.fromX = this.glide(this.fromX, this.x);
			this.fromZ = this.glide(this.fromZ, this.z);
			this.movedAt = System.currentTimeMillis();
			this.glideMs = overMs;
		}

		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static enum Kind {
		TEAMMATE,
		WAYPOINT,
		PING;
	}
}
