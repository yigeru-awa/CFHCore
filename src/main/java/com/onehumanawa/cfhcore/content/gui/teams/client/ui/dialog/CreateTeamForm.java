package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette.AccentPreset;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSwatches;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.BannerArt;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.BannerChoice;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.HintField;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

public final class CreateTeamForm implements FormPart {
	private static final int PREVIEW_W = 36;
	private CFHGuiTextField name;
	private CFHGuiTextField tag;
	private BannerChoice banners;
	private int color = ((AccentPreset)Palette.ACCENTS.get(0)).color();
	private final List<BannerData> presets = new ArrayList<>();
	private int colorY;
	private int bannerY;
	private int height;

	@Override
	public Component title() {
		return TeamsText.t("create.title");
	}

	@Override
	public Icon icon() {
		return Icon.TEAM;
	}

	@Override
	public int preferredWidth() {
		return 252;
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		int fields = width - 36;
		int tagW = 50;
		int nameW = fields - 50 - 4;
		this.name = new HintField(0, 0, nameW, TeamsText.t("create.name")).maxLength(24);
		this.tag = new HintField(0, 0, 50, TeamsText.t("create.tag")).maxLength(5);
		placer.place(this.name, 36, 0);
		placer.place(this.tag, 36 + nameW + 4, 0);
		int y = 26;
		this.colorY = y;
		y += 11;
		CFHGuiSwatches swatches = new CFHGuiSwatches(0, 0, fields, Palette.ACCENTS, this.color, c -> {
			this.color = c;
			this.presets();
		});
		placer.place(swatches, 36, y);
		y += swatches.getHeight() + 7;
		this.bannerY = y;
		y += 11;
		this.presets();
		this.banners = new BannerChoice(0, 0, this.presets, 1, i -> {
		});
		placer.place(this.banners, 36, y);
		y += this.banners.getHeight();
		this.height = Math.max(y, TeamsDraw.bannerHeight(20) + 22);
		return this.height;
	}

	private void presets() {
		DyeColor dye = nearestDye(this.color);
		String c = dye.getName();
		String ink = Colors.luminance(BannerArt.rgb(dye)) > 0.55F ? "black" : "white";
		this.presets.clear();
		this.presets.add(new BannerData(c));
		this.presets.add(new BannerData(c).layer("minecraft:border", ink));
		this.presets.add(new BannerData(c).layer("minecraft:gradient_up", "black").layer("minecraft:border", ink));
		this.presets.add(new BannerData(c).layer("minecraft:straight_cross", ink));
		this.presets.add(new BannerData(c).layer("minecraft:rhombus", ink));
		this.presets.add(new BannerData(ink).layer("minecraft:stripe_center", c).layer("minecraft:border", c));
		this.presets.add(new BannerData("black").layer("minecraft:triangle_bottom", c).layer("minecraft:triangle_top", c));
		if (this.banners != null) {
			this.banners.options(this.presets);
		}
	}

	public static DyeColor nearestDye(int rgb) {
		DyeColor best = DyeColor.WHITE;
		long nearest = Long.MAX_VALUE;

		for (DyeColor d : DyeColor.values()) {
			int c = d.getTextureDiffuseColor();
			long dr = (long)(Colors.red(c) - Colors.red(rgb));
			long dg = (long)(Colors.green(c) - Colors.green(rgb));
			long db = (long)(Colors.blue(c) - Colors.blue(rgb));
			long dist = dr * dr * 3L + dg * dg * 4L + db * db * 2L;
			if (dist < nearest) {
				nearest = dist;
				best = d;
			}
		}

		return best;
	}

	private BannerData banner() {
		return this.presets.get(Math.min(this.presets.size() - 1, this.banners.selected()));
	}

	@Override
	public void render(GuiGraphics g, int x, int y, int width, float alpha, int mouseX, int mouseY) {
		Palette p = Theme.current().palette();
		this.name.setInvalid(!this.name.getValue().isBlank() && !TeamFlows.validName(this.name.getValue()));
		this.tag.setInvalid(!TeamFlows.validTag(this.tag.getValue()));
		TeamsDraw.banner(g, this.banner(), x + 4, y + 2, 20, 1.0F, 1.2F, alpha);
		String t = this.tag.getValue().trim();
		if (!t.isEmpty() && TeamFlows.validTag(t)) {
			Component chip = Component.literal(t.toUpperCase(Locale.ROOT));
			int cw = CFHGuiDraw.width(chip);
			g.drawString(CFHGuiDraw.font(), chip, x + 14 - cw / 2, y + 48, Colors.scaleAlpha(TeamsDraw.ink(this.color & 16777215), alpha), Theme.current().isVanilla());
		}

		TeamsDraw.text(g, TeamsText.t("create.colour"), x + 36, y + this.colorY, TeamsDraw.caption(), alpha);
		TeamsDraw.text(g, TeamsText.t("create.banner"), x + 36, y + this.bannerY, TeamsDraw.caption(), alpha);
	}

	@Override
	public Component submitLabel() {
		return TeamsText.t("create.submit");
	}

	@Override
	public boolean ready() {
		return TeamFlows.validName(this.name.getValue()) && TeamFlows.validTag(this.tag.getValue());
	}

	@Override
	public void submit(FormPart.Host host) {
		if (!TeamFlows.validName(this.name.getValue())) {
			host.fail(TeamsText.shared("result.name_invalid"));
		} else if (!TeamFlows.validTag(this.tag.getValue())) {
			host.fail(TeamsText.shared("result.tag_invalid"));
		} else {
			BannerData banner = this.banner();
			TeamActions.create(this.name.getValue().trim(), this.tag.getValue().trim(), this.color & 16777215, result -> {
				if (!result.ok()) {
					host.fail(TeamFlows.reason(result));
				} else {
					TeamActions.setBanner(banner);
					host.done();
				}
			});
		}
	}

	@Nullable
	@Override
	public AbstractWidget firstFocus() {
		return this.name;
	}
}
