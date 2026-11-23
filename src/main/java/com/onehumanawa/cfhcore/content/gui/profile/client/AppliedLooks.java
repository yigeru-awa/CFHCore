package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class AppliedLooks {
	private static final Map<UUID, AppliedLooks.Applied> LOOKS = new ConcurrentHashMap<>();
	private static final AtomicInteger COUNTER = new AtomicInteger();

	@Nullable
	public static AppliedLooks.Applied of(@Nullable UUID player) {
		return player != null && !LOOKS.isEmpty() ? LOOKS.get(player) : null;
	}

	@Nullable
	public static PlayerSkin skinOf(@Nullable UUID player) {
		AppliedLooks.Applied look = of(player);
		return look == null ? null : look.skin();
	}

	public static void put(UUID player, NativeImage skin, boolean slim, List<Cosmetic> models) {
		ResourceLocation id = ResourceLocation.withDefaultNamespace("skins/cfhgui_profile/look_" + COUNTER.incrementAndGet());
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(skin));
		AppliedLooks.Applied old = LOOKS.put(player, new AppliedLooks.Applied(id, slim ? Model.SLIM : Model.WIDE, models));
		if (old != null) {
			Minecraft.getInstance().getTextureManager().release(old.texture);
		}
	}

	public static void remove(UUID player) {
		AppliedLooks.Applied old = LOOKS.remove(player);
		if (old != null) {
			Minecraft.getInstance().getTextureManager().release(old.texture);
		}
	}

	public static void removeAllBut(@Nullable UUID keep) {
		for (UUID id : List.copyOf(LOOKS.keySet())) {
			if (!id.equals(keep)) {
				remove(id);
			}
		}
	}

	public static int count() {
		return LOOKS.size();
	}

	private AppliedLooks() {
	}

	public static final class Applied {
		private final ResourceLocation texture;
		private final Model model;
		private final List<Cosmetic> models;
		@Nullable
		private PlayerSkin last;
		@Nullable
		private PlayerSkin made;
		private final PlayerSkin plain;

		private Applied(ResourceLocation texture, Model model, List<Cosmetic> models) {
			this.texture = texture;
			this.model = model;
			this.models = List.copyOf(models);
			this.plain = new PlayerSkin(texture, null, null, null, model, true);
		}

		public ResourceLocation texture() {
			return this.texture;
		}

		public Model model() {
			return this.model;
		}

		public List<Cosmetic> models() {
			return this.models;
		}

		public PlayerSkin over(PlayerSkin original) {
			if (this.made == null || this.last != original) {
				this.last = original;
				this.made = new PlayerSkin(this.texture, original.textureUrl(), original.capeTexture(), original.elytraTexture(), this.model, original.secure());
			}

			return this.made;
		}

		public PlayerSkin skin() {
			return this.plain;
		}
	}
}
