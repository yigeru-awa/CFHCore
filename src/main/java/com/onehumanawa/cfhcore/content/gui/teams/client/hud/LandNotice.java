package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.Banners;
import com.onehumanawa.cfhcore.content.gui.teams.client.LandNames;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class LandNotice {
	private static final int HOLD_MS = 3000;
	private static final int QUIET_AFTER_JOIN_MS = 2500;
	@Nullable
	private static UUID current;
	private static String dimension = "";
	private static boolean known;
	private static long quietUntil;
	@Nullable
	private static UUID said;
	private static boolean saidAnything;
	private static Component title = Component.empty();
	@Nullable
	private static Component sub;
	@Nullable
	private static BannerData banner;
	private static boolean server;
	private static boolean personalLand;
	private static int color;
	private static long saidAt;
	private static final Anim IN = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private static final Anim SWAP = new Anim(1.0F, 180, Ease.OUT_CUBIC);
	private static final Anim WIDTH = new Anim(0.0F, 200, Ease.OUT_CUBIC);

	static void joined() {
		known = false;
		quietUntil = Clock.nowMs() + 2500L;
		IN.snap(0.0F);
		saidAnything = false;
	}

	static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.level != null && TeamsState.available()) {
			String dim = TeamsState.currentDimension();
			TeamsState.Claim claim = TeamsState.claimAt(dim, mc.player.chunkPosition().x, mc.player.chunkPosition().z);
			UUID owner = claim == null ? null : claim.team();
			if (known && Clock.nowMs() >= quietUntil) {
				boolean moved = !dim.equals(dimension);
				dimension = dim;
				if (!Objects.equals(owner, current) || moved) {
					current = owner;
					if (!moved || owner != null) {
						if (TeamsConfig.get().landNotice) {
							say(owner);
						}
					}
				}
			} else {
				known = true;
				dimension = dim;
				current = owner;
			}
		} else {
			known = false;
		}
	}

	private static void say(@Nullable UUID owner) {
		long now = Clock.nowMs();
		boolean up = saidAnything && now - saidAt < 3000L && IN.target() > 0.0F;
		saidAt = now;
		if (!up || !Objects.equals(owner, said)) {
			said = owner;
			saidAnything = true;
			server = TeamsState.isServerLand(owner);
			personalLand = false;
			if (owner == null) {
				title = Fonts.heading(Component.translatable("cfhgui_teams.hud.land.wilderness"));
				sub = null;
				banner = null;
				color = 0;
			} else if (server) {
				TeamSummary summary = TeamsState.summary(owner);
				String name = summary != null ? summary.name : "";
				title = Fonts.heading(name.isEmpty() ? Component.translatable("cfhgui_teams.hud.land.server_land") : Component.literal(name));
				sub = Component.translatable("cfhgui_teams.hud.land.server");
				banner = null;
				color = summary != null ? summary.color : 0;
			} else {
				TeamData mine = TeamsState.land();
				TeamSummary summary = TeamsState.summary(owner);
				boolean own = TeamsState.isOwn(owner);
				boolean personal = LandNames.isPersonal(owner);
				Component name = own && mine != null ? LandNames.of(mine) : (summary != null ? LandNames.of(summary) : null);
				title = Fonts.heading((Component)(name != null && !name.getString().isEmpty() ? name : Component.translatable("cfhgui_teams.hud.land.claimed")));
				sub = Component.translatable(
					own
						? (personal ? "cfhgui_teams.hud.land.own_personal" : "cfhgui_teams.hud.land.own")
						: (TeamsState.isAlly(owner) ? "cfhgui_teams.hud.land.ally" : (personal ? "cfhgui_teams.hud.land.personal" : "cfhgui_teams.hud.land.other"))
				);
				banner = personal ? null : (own && mine != null ? mine.banner : (summary != null ? summary.banner : null));
				color = own && mine != null ? mine.color : (summary != null ? summary.color : 0);
				personalLand = personal;
			}

			if (up) {
				SWAP.snap(0.0F);
				SWAP.set(1.0F);
			} else {
				SWAP.snap(1.0F);
				WIDTH.snap((float)width());
			}

			IN.set(1.0F, 240);
		}
	}

	private static int width() {
		return 29 + Math.max(CFHGuiDraw.width(title), sub == null ? 0 : CFHGuiDraw.width(sub)) + 9;
	}

	static void reserve(GuiGraphics g) {
		if (saidAnything && !(IN.get() <= 0.01F)) {
			int w = Math.round(WIDTH.get());
			int h = sub == null ? 22 : 28;
			LabelStack.place((g.guiWidth() - w) / 2, Math.round((float)g.guiHeight() * 0.15F), w, h, false);
		}
	}

	static void render(GuiGraphics g) {
		if (saidAnything) {
			if (Clock.nowMs() - saidAt >= 3000L) {
				IN.set(0.0F, 320);
			}

			float in = IN.get();
			if (!(in <= 0.01F)) {
				WIDTH.set((float)width());
				int w = Math.round(WIDTH.get());
				int h = sub == null ? 22 : 28;
				int sw = g.guiWidth();
				int sh = g.guiHeight();
				int x = (sw - w) / 2;
				int y = Math.round((float)sh * 0.15F) - Math.round((1.0F - in) * 8.0F);
				float a = Mth.clamp(in, 0.0F, 1.0F);
				float words = a * (0.2F + 0.8F * SWAP.get());
				boolean wild = said == null;
				int mark = wild ? HudDraw.dimColor() : HudDraw.readable(color);
				int textX = x + 7 + 16 + 6;
				HudDraw.batch(g, () -> {
					HudDraw.notice(g, x, y, w, h, a);
					int line = Math.round((float)(w - 12) * Ease.OUT_CUBIC.apply(a));
					CFHGuiDraw.rect(g, x + (w - line) / 2, y + h - 3, line, 1, Colors.scaleAlpha(mark, a * (wild ? 0.5F : 0.9F)));
					HudDraw.text(g, title, textX, y + (sub == null ? 7 : 5), wild ? HudDraw.textColor() : mark, words);
					if (sub != null) {
						HudDraw.text(g, sub, textX, y + 15, HudDraw.mutedColor(), words);
					}
				});
				int ix = x + 7;
				int iy = y + (h - 16) / 2 - 1;
				if (!wild && banner != null) {
					float s = 0.6F + 0.4F * words;
					g.pose().pushPose();
					g.pose().translate((float)ix + 8.0F, (float)iy + 8.0F, 0.0F);
					g.pose().scale(s, s, 1.0F);
					g.pose().translate(-8.0F, -8.0F, 0.0F);
					g.renderItem(Banners.stack(banner), 0, 0);
					g.pose().popPose();
				} else {
					Icons.draw(g, wild ? Icon.WORLD : (server ? Icon.SHIELD : (personalLand ? Icon.USER : Icon.TEAM)), ix, iy, 16, Colors.scaleAlpha(mark, words));
				}
			}
		}
	}

	static void clear() {
		known = false;
		saidAnything = false;
		IN.snap(0.0F);
	}

	static void demo(@Nullable UUID owner) {
		say(owner);
	}

	private LandNotice() {
	}
}
