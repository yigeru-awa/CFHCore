package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSwatches;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.GlyphChoice;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.HintField;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class WaypointForm implements FormPart {
	public static final int NAME_MAX = 32;
	private static final int TILE = 20;
	@Nullable
	private final TeamData.Waypoint existing;
	private CFHGuiTextField name;
	private CFHGuiTextField fx;
	private CFHGuiTextField fy;
	private CFHGuiTextField fz;
	private CFHGuiDropdown<String> dimension;
	private int color;
	private String icon;
	private int placeY;
	private int hereW = 46;

	public WaypointForm(@Nullable TeamData.Waypoint existing) {
		this.existing = existing;
		TeamData team = TeamsView.team();
		this.color = existing != null ? existing.color : (team != null ? team.color : 14722136);
		this.icon = existing != null ? existing.icon : "flag";
	}

	@Override
	public Component title() {
		return TeamsText.t(this.existing == null ? "waypoint.add.title" : "waypoint.edit.title");
	}

	@Override
	public Icon icon() {
		return Icon.PIN;
	}

	@Override
	public int preferredWidth() {
		return 236;
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		Vec3 at = TeamsView.position();
		this.name = new HintField(0, 0, width - 20 - 6, TeamsText.t("waypoint.name")).maxLength(32);
		if (this.existing != null) {
			this.name.setValue(this.existing.name);
		}

		placer.place(this.name, 26, 0);
		this.placeY = 26;
		CFHGuiButton here = new CFHGuiButton(0, 0, 46, TeamsText.t("waypoint.here"), this::here).icon(Icon.COMPASS);
		here.setWidth(Math.max(46, here.preferredWidth() + 4));
		this.hereW = here.getWidth();
		here.tip(TeamsText.t(TeamsView.inWorld() ? "waypoint.here.desc" : "waypoint.here.no_world"));
		here.active = TeamsView.inWorld();
		int group = (width - here.getWidth() - 4 - 8) / 3;
		int fieldW = group - 10;
		this.fx = number(fieldW, "X", this.existing != null ? this.existing.x : (at == null ? 0 : Mth.floor(at.x)));
		this.fy = number(fieldW, "Y", this.existing != null ? this.existing.y : (at == null ? 64 : Mth.floor(at.y)));
		this.fz = number(fieldW, "Z", this.existing != null ? this.existing.z : (at == null ? 0 : Mth.floor(at.z)));
		placer.place(this.fx, 10, this.placeY);
		placer.place(this.fy, group + 4 + 10, this.placeY);
		placer.place(this.fz, (group + 4) * 2 + 10, this.placeY);
		placer.place(here, width - here.getWidth(), this.placeY);
		Set<String> dims = new LinkedHashSet<>();
		if (this.existing != null) {
			dims.add(this.existing.dimension);
		}

		if (TeamsView.inWorld() || this.existing == null) {
			dims.add(TeamsView.dimension());
		}

		dims.add("minecraft:overworld");
		dims.add("minecraft:the_nether");
		dims.add("minecraft:the_end");
		List<String> options = new ArrayList<>(dims);
		this.dimension = new CFHGuiDropdown<>(
			0, 0, width, options, this.existing != null ? this.existing.dimension : TeamsView.dimension(), TeamsText::dimension, d -> {
			}
		);
		this.dimension.label(TeamsText.t("waypoint.dimension"));
		placer.place(this.dimension, 0, 52);
		CFHGuiSwatches swatches = new CFHGuiSwatches(0, 0, width, Palette.ACCENTS, 0xFF000000 | this.color, c -> this.color = c & 16777215);
		placer.place(swatches, 0, 78);
		int glyphY = 78 + swatches.getHeight() + 6;
		GlyphChoice glyphs = new GlyphChoice(0, 0, width, this.icon, () -> this.color, i -> this.icon = i);
		placer.place(glyphs, 0, glyphY);
		return glyphY + glyphs.getHeight();
	}

	private static CFHGuiTextField number(int width, String axis, int value) {
		CFHGuiTextField f = new CFHGuiTextField(0, 0, width, Component.literal(axis));
		f.maxLength(9);
		f.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{1,8}"));
		f.setValue(Integer.toString(value));
		return f;
	}

	private void here() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			this.fx.setValue(Integer.toString(mc.player.getBlockX()));
			this.fy.setValue(Integer.toString(mc.player.getBlockY()));
			this.fz.setValue(Integer.toString(mc.player.getBlockZ()));
			this.dimension.setValue(TeamsView.dimension());
		}
	}

	@Nullable
	private static Integer parse(CFHGuiTextField f) {
		try {
			return Integer.parseInt(f.getValue().trim());
		} catch (NumberFormatException var2) {
			return null;
		}
	}

	@Override
	public void render(GuiGraphics g, int x, int y, int width, float alpha, int mouseX, int mouseY) {
		Palette p = Theme.current().palette();
		TeamsDraw.glyphTile(g, WaypointIcons.get(this.icon), x, y, 20, this.color, alpha);
		this.fx.setInvalid(parse(this.fx) == null);
		this.fy.setInvalid(parse(this.fy) == null);
		this.fz.setInvalid(parse(this.fz) == null);
		int group = (width - this.hereW - 4 - 8) / 3;
		TeamsDraw.text(g, Component.literal("X"), x + 1, y + this.placeY + 6, TeamsDraw.caption(), alpha);
		TeamsDraw.text(g, Component.literal("Y"), x + group + 4 + 1, y + this.placeY + 6, TeamsDraw.caption(), alpha);
		TeamsDraw.text(g, Component.literal("Z"), x + (group + 4) * 2 + 1, y + this.placeY + 6, TeamsDraw.caption(), alpha);
	}

	@Override
	public Component submitLabel() {
		return TeamsText.t(this.existing == null ? "waypoint.add.submit" : "waypoint.edit.submit");
	}

	@Override
	public boolean ready() {
		return !this.name.getValue().isBlank() && parse(this.fx) != null && parse(this.fy) != null && parse(this.fz) != null;
	}

	@Override
	public void submit(FormPart.Host host) {
		Integer x = parse(this.fx);
		Integer y = parse(this.fy);
		Integer z = parse(this.fz);
		if (x != null && y != null && z != null) {
			String n = this.name.getValue().trim();
			String dim = (String)this.dimension.value();
			Consumer<TeamsNet.Result> then = result -> {
				if (result.ok()) {
					host.done();
				} else {
					host.fail(TeamFlows.reason(result));
				}
			};
			if (this.existing == null) {
				TeamActions.addWaypoint(n, x, y, z, dim, this.color, this.icon, then);
			} else {
				CompoundTag changes = new CompoundTag();
				if (!n.equals(this.existing.name)) {
					changes.putString("name", n);
				}

				if (x != this.existing.x) {
					changes.putInt("x", x);
				}

				if (y != this.existing.y) {
					changes.putInt("y", y);
				}

				if (z != this.existing.z) {
					changes.putInt("z", z);
				}

				if (dim != null && !dim.equals(this.existing.dimension)) {
					changes.putString("dimension", dim);
				}

				if ((this.color & 16777215) != (this.existing.color & 16777215)) {
					changes.putInt("color", this.color & 16777215);
				}

				if (!this.icon.equals(this.existing.icon)) {
					changes.putString("icon", this.icon);
				}

				if (changes.isEmpty()) {
					host.done();
				} else {
					TeamActions.editWaypoint(this.existing.id, changes, then);
				}
			}
		} else {
			host.fail(TeamsText.t("waypoint.bad_place"));
		}
	}

	@Nullable
	@Override
	public AbstractWidget firstFocus() {
		return this.name;
	}
}
