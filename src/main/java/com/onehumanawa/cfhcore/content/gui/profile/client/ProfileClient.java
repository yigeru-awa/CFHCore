package com.onehumanawa.cfhcore.content.gui.profile.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.client.PlayerSkins;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.profile.CFHGuiProfile;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import com.onehumanawa.cfhcore.content.gui.profile.client.render.LookNode;
import com.onehumanawa.cfhcore.content.gui.profile.client.ui.ProfileScreen;
import com.onehumanawa.cfhcore.content.gui.profile.net.ProfileNet;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.User.Type;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class ProfileClient {
	private static boolean initialised;
	private static boolean dirty = true;
	private static boolean untold;
	private static long toldAt;
	private static String wornSignature = "";
	private static final long TELL_EVERY_MS = 1000L;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			ScreenIds.register(ProfileScreen.class, "cfhgui_profile:profile", "Profile");
			MenuSlots.provide("cfhgui_profile:profile", Layout.CUSTOM, p -> new ProfileScreen(p, false));
			MenuSlots.provide("cfhgui_profile:profile", Layout.OVERHAUL, p -> new ProfileScreen(p, true));
			CoreActions.SCREEN_FACTORIES.put("cfhgui_profile:profile", p -> new ProfileScreen(p, MenuSlots.effective("cfhgui_profile:profile") == Layout.OVERHAUL));
			CoreActions.SCREEN_FACTORIES
				.put("cfhgui_profile:profile/new", p -> new ProfileScreen(p, MenuSlots.effective("cfhgui_profile:profile") == Layout.OVERHAUL).startBlank());

			for (Slot s : Slot.values()) {
				CoreActions.SCREEN_FACTORIES
					.put("cfhgui_profile:profile/" + s.key(), p -> new ProfileScreen(p, MenuSlots.effective("cfhgui_profile:profile") == Layout.OVERHAUL).startEditing(s));
			}

			PlayerNode.localFactory(() -> {
				ProfileStore.Look worn = account().worn();
				return worn == null ? null : new LookNode(worn);
			});
			PlayerSkins.worn(AppliedLooks::skinOf);
			CFHGuiEvents.CLIENT_TICK_END.register(ProfileClient::tick);
			CFHGuiEvents.CLIENT_JOINED_SERVER.register((Runnable)() -> {
				untold = true;
				toldAt = 0L;
			});
			CFHGuiEvents.CLIENT_LEFT_SERVER.register((Runnable)() -> AppliedLooks.removeAllBut(self()));
			User user = Minecraft.getInstance().getUser();

			try {
				ProfileStore.seen(user.getProfileId(), user.getName(), user.getType() == Type.MSA);
				if (DevHarness.sampleData()) {
					SampleLooks.ensure(account());
				}
			} catch (Exception var4) {
				CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot read the profile folder {}: {}", ProfileStore.root(), var4.toString());
			}

			CFHGuiProfile.LOGGER
				.info("[CFHGui Profile] client ready ({} look(s) for {}, kept in {})", new Object[]{account().data().looks.size(), user.getName(), ProfileStore.root()});
		}
	}

	public static UUID self() {
		return Minecraft.getInstance().getUser().getProfileId();
	}

	public static ProfileStore.Account account() {
		User user = Minecraft.getInstance().getUser();
		return ProfileStore.account(user.getProfileId(), user.getName());
	}

	public static void changed() {
		dirty = true;
	}

	public static void wear(@Nullable ProfileStore.Look look) {
		account().wear(look);
		changed();
	}

	private static void tick() {
		Minecraft mc = Minecraft.getInstance();
		ProfileStore.Look worn = account().worn();
		String signature = worn == null ? "" : worn.signature() + SkinComposer.sourceKey(worn);
		if (!signature.equals(wornSignature)) {
			dirty = true;
		}

		if (dirty) {
			dirty = false;
			wornSignature = signature;

			try {
				if (worn == null) {
					AppliedLooks.remove(self());
				} else {
					List<Cosmetic> models = new ArrayList<>();

					for (Cosmetic c : worn.items()) {
						if (c.isModel()) {
							models.add(c);
						}
					}

					AppliedLooks.put(self(), SkinComposer.compose(worn), worn.slim(), models);
				}
			} catch (Exception var6) {
				CFHGuiProfile.LOGGER.error("[CFHGui Profile] cannot dress the player", var6);
			}

			untold = true;
		}

		if (untold && mc.getConnection() != null && System.currentTimeMillis() - toldAt >= 1000L) {
			untold = false;
			toldAt = System.currentTimeMillis();
			tell();
		}
	}

	private static void tell() {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.serverHasChannel(ProfileNet.LookUpdate.TYPE)) {
			ProfileStore.Look worn = account().worn();
			if (worn != null) {
				try {
					NativeImage skin = SkinComposer.compose(worn);

					try {
						List<String> models = new ArrayList<>();

						for (Cosmetic c : worn.items()) {
							if (c.isModel()) {
								models.add(c.id());
							}
						}

						ProfileNet.LookData data = new ProfileNet.LookData(SkinComposer.png(skin), worn.slim(), models);
						if (data.valid()) {
							net.sendToServer(new ProfileNet.LookUpdate(data));
						}
					} catch (Throwable var7) {
						if (skin != null) {
							try {
								skin.close();
							} catch (Throwable var6) {
								var7.addSuppressed(var6);
							}
						}

						throw var7;
					}

					if (skin != null) {
						skin.close();
					}
				} catch (Exception var8) {
					CFHGuiProfile.LOGGER.warn("[CFHGui Profile] cannot send the look: {}", var8.toString());
				}
			}
		}
	}

	public static void lookOf(ProfileNet.LookOf message) {
		UUID who = message.player();
		if (!who.equals(self())) {
			if (message.look().isEmpty()) {
				AppliedLooks.remove(who);
			} else {
				ProfileNet.LookData data = message.look().get();
				if (data.valid()) {
					try {
						NativeImage image = NativeImage.read(new ByteArrayInputStream(data.skin()));
						if (image.getWidth() != 64 || image.getHeight() != 64) {
							image.close();
							return;
						}

						List<Cosmetic> models = new ArrayList<>();

						for (String id : data.models()) {
							Cosmetic c = Cosmetics.get(id);
							if (c != null && c.isModel()) {
								models.add(c);
							}
						}

						AppliedLooks.put(who, image, data.slim(), models);
					} catch (Exception var8) {
						CFHGuiProfile.LOGGER.warn("[CFHGui Profile] the look of {} cannot be read: {}", who, var8.toString());
					}
				}
			}
		}
	}

	public static List<HubEntry> hubEntries() {
		return List.of(
			new HubEntry(Component.translatable("cfhgui_profile.name.short"), Icon.USER, () -> MenuSlots.open("cfhgui_profile:profile", Minecraft.getInstance().screen))
		);
	}

	private ProfileClient() {
	}
}
