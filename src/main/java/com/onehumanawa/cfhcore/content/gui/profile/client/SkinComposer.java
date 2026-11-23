package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class SkinComposer {
	private static final List<Slot> ORDER = List.of(
		Slot.RIGHT_ARM, Slot.LEFT_ARM, Slot.RIGHT_LEG, Slot.LEFT_LEG, Slot.PANTS, Slot.SHIRT, Slot.FACE, Slot.HAT, Slot.BACK, Slot.PET
	);
	private static final Map<UUID, SkinComposer.Account> ACCOUNTS = new HashMap<>();

	public static NativeImage compose(ProfileStore.Look look) {
		NativeImage skin = base(look);

		for (Slot slot : ORDER) {
			Cosmetic c = look.in(slot);
			if (c != null && !c.isModel()) {
				NativeImage layer = resource(c.texture());

				try {
					if (layer != null) {
						lay(skin, layer);
					}
				} catch (Throwable var9) {
					if (layer != null) {
						try {
							layer.close();
						} catch (Throwable var8) {
							var9.addSuppressed(var8);
						}
					}

					throw var9;
				}

				if (layer != null) {
					layer.close();
				}
			}
		}

		return skin;
	}

	public static String sourceKey(ProfileStore.Look look) {
		return !"account".equals(look.skin()) ? "" : "|account#" + account(look.account().uuid(), look.account().name()).version;
	}

	public static NativeImage sample(Cosmetic layer, boolean slim) {
		NativeImage skin = mannequin();
		NativeImage over = resource(layer.texture());

		try {
			if (over != null) {
				lay(skin, over);
			}
		} catch (Throwable var7) {
			if (over != null) {
				try {
					over.close();
				} catch (Throwable var6) {
					var7.addSuppressed(var6);
				}
			}

			throw var7;
		}

		if (over != null) {
			over.close();
		}

		return skin;
	}

	public static NativeImage mannequin() {
		NativeImage image = new NativeImage(64, 64, true);
		image.fillRect(0, 0, 64, 64, 0);
		fill(image, 0, 0, 32, 16, -6646644);
		fill(image, 16, 16, 24, 16, -7567746);
		fill(image, 40, 16, 16, 16, -7107195);
		fill(image, 0, 16, 16, 16, -8028296);
		fill(image, 16, 48, 16, 16, -8028296);
		fill(image, 32, 48, 16, 16, -7107195);
		return image;
	}

	private static void fill(NativeImage image, int x, int y, int w, int h, int argb) {
		int abgr = argb & -16711936 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
		image.fillRect(x, y, w, h, abgr);
	}

	public static NativeImage base(ProfileStore.Look look) {
		String source = look.skin();
		NativeImage image = null;
		if ("file".equals(source) && Files.isRegularFile(look.skinFile())) {
			try (InputStream in = Files.newInputStream(look.skinFile())) {
				image = normalised(NativeImage.read(in));
			} catch (Exception var8) {
				CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot read the skin of {}: {}", look.name(), var8.toString());
			}
		} else if (source.startsWith("default:")) {
			image = resource(defaultSkin(source.substring(8), look.slim()));
		} else if ("account".equals(source)) {
			image = ofAccount(look.account().uuid(), look.account().name());
		}

		if (image == null) {
			image = resource(DefaultPlayerSkin.get(look.account().uuid()).texture());
		}

		if (image == null) {
			image = mannequin();
		}

		return image;
	}

	public static ResourceLocation defaultSkin(String name, boolean slim) {
		String n = ProfileStore.Look.DEFAULTS.contains(name) ? name : "steve";
		return ResourceLocation.withDefaultNamespace("textures/entity/player/" + (slim ? "slim" : "wide") + "/" + n + ".png");
	}

	public static boolean accountSlim(UUID uuid, String name) {
		return accountSkin(uuid, name).model() == Model.SLIM;
	}

	public static PlayerSkin accountSkin(UUID uuid, String name) {
		PlayerSkin seen = account(uuid, name).seen;
		return seen != null ? seen : DefaultPlayerSkin.get(uuid);
	}

	@Nullable
	private static NativeImage ofAccount(UUID uuid, String name) {
		NativeImage kept = account(uuid, name).pixels;
		if (kept == null) {
			return null;
		} else {
			NativeImage copy = new NativeImage(kept.getWidth(), kept.getHeight(), false);
			copy.copyFrom(kept);
			return copy;
		}
	}

	private static SkinComposer.Account account(UUID uuid, String name) {
		SkinComposer.Account a = ACCOUNTS.get(uuid);
		if (a == null) {
			a = new SkinComposer.Account(
				CompletableFuture.<GameProfile>supplyAsync(() -> PlayerNode.profile(uuid, name), Util.ioPool())
					.exceptionally(e -> new GameProfile(uuid, name != null && !name.isEmpty() ? name : "?"))
			);
			ACCOUNTS.put(uuid, a);
		}

		try {
			a.refresh();
		} catch (Exception var4) {
			CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot look up the skin of the account {}: {}", uuid, var4.toString());
		}

		return a;
	}

	private static boolean same(NativeImage a, NativeImage b) {
		if (a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight()) {
			for (int y = 0; y < a.getHeight(); y++) {
				for (int x = 0; x < a.getWidth(); x++) {
					if (a.getPixelRGBA(x, y) != b.getPixelRGBA(x, y)) {
						return false;
					}
				}
			}

			return true;
		} else {
			return false;
		}
	}

	@Nullable
	public static NativeImage resource(ResourceLocation id) {
		try {
			Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(id);
			if (found.isEmpty()) {
				return texture(id);
			} else {
				NativeImage var3;
				try (InputStream in = found.get().open()) {
					var3 = normalised(NativeImage.read(in));
				}

				return var3;
			}
		} catch (Exception var7) {
			CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot read {}: {}", id, var7.toString());
			return null;
		}
	}

	@Nullable
	private static NativeImage texture(ResourceLocation id) {
		try {
			AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(id);
			RenderSystem.bindTexture(t.getId());
			int w = GL11.glGetTexLevelParameteri(3553, 0, 4096);
			int h = GL11.glGetTexLevelParameteri(3553, 0, 4097);
			if (w == 64 && (h == 64 || h == 32)) {
				NativeImage image = new NativeImage(w, h, false);
				image.downloadTexture(0, false);
				return normalised(image);
			} else {
				return null;
			}
		} catch (Exception var5) {
			return null;
		}
	}

	@Nullable
	private static NativeImage normalised(@Nullable NativeImage image) {
		if (image == null) {
			return null;
		} else if (image.getWidth() == 64 && image.getHeight() == 64) {
			return image;
		} else if (image.getWidth() == 64 && image.getHeight() == 32) {
			return PlayerNode.upgradeLegacySkin(image);
		} else {
			image.close();
			return null;
		}
	}

	private static void lay(NativeImage skin, NativeImage over) {
		int w = Math.min(skin.getWidth(), over.getWidth());
		int h = Math.min(skin.getHeight(), over.getHeight());

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int top = over.getPixelRGBA(x, y);
				int a = top >>> 24;
				if (a != 0) {
					if (a <= 2) {
						skin.setPixelRGBA(x, y, 0);
					} else if (a >= 250) {
						skin.setPixelRGBA(x, y, top | 0xFF000000);
					} else {
						int under = skin.getPixelRGBA(x, y);
						int ua = under >>> 24;
						float t = (float)a / 255.0F;
						int r = Math.round((float)(top & 0xFF) * t + (float)(under & 0xFF) * (1.0F - t));
						int g = Math.round((float)(top >> 8 & 0xFF) * t + (float)(under >> 8 & 0xFF) * (1.0F - t));
						int b = Math.round((float)(top >> 16 & 0xFF) * t + (float)(under >> 16 & 0xFF) * (1.0F - t));
						int outA = ua == 0 ? a : Math.max(a, ua);
						skin.setPixelRGBA(x, y, outA << 24 | b << 16 | g << 8 | r);
					}
				}
			}
		}
	}

	public static byte[] png(NativeImage image) {
		try {
			return image.asByteArray();
		} catch (Exception var2) {
			CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot write a skin: {}", var2.toString());
			return new byte[0];
		}
	}

	private SkinComposer() {
	}

	private static final class Account {
		private static final long LOOK_AGAIN_MS = 250L;
		private static final long LOOK_AGAIN_LATER_MS = 5000L;
		private static final long SOON_MS = 60000L;
		private final CompletableFuture<GameProfile> profile;
		@Nullable
		private Supplier<PlayerSkin> lookup;
		@Nullable
		PlayerSkin seen;
		@Nullable
		NativeImage pixels;
		int version;
		private boolean reading;
		private long readAt;
		private long readSince;

		Account(CompletableFuture<GameProfile> profile) {
			this.profile = profile;
		}

		void refresh() {
			if (this.lookup == null) {
				GameProfile known = this.profile.getNow(null);
				if (known == null) {
					return;
				}

				this.lookup = Minecraft.getInstance().getSkinManager().lookupInsecure(known);
			}

			PlayerSkin now = this.lookup.get();
			long time = System.currentTimeMillis();
			if (this.seen == null || !now.texture().equals(this.seen.texture())) {
				this.reading = true;
				this.readAt = 0L;
				this.readSince = time;
			}

			this.seen = now;
			if (this.reading && time >= this.readAt) {
				boolean downloaded = !now.texture().getPath().startsWith("textures/");
				NativeImage read = SkinComposer.resource(now.texture());
				if (read != null && downloaded) {
					NativeImage standIn = SkinComposer.resource(DefaultPlayerSkin.getDefaultTexture());

					try {
						if (standIn != null && SkinComposer.same(read, standIn)) {
							read.close();
							read = null;
						}
					} catch (Throwable var10) {
						if (standIn != null) {
							try {
								standIn.close();
							} catch (Throwable var9) {
								var10.addSuppressed(var9);
							}
						}

						throw var10;
					}

					if (standIn != null) {
						standIn.close();
					}
				}

				if (read == null) {
					this.readAt = time + (time - this.readSince < 60000L ? 250L : 5000L);
				} else {
					this.reading = false;
					if (this.pixels != null) {
						this.pixels.close();
					}

					this.pixels = read;
					this.version++;
				}
			}
		}
	}
}
