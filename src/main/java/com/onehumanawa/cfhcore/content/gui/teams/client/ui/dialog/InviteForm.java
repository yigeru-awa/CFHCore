package com.onehumanawa.cfhcore.content.gui.teams.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList.RowRenderer;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.HintField;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class InviteForm implements FormPart {
	private static final int ROW = 18;
	private static final int ROWS = 5;
	private CFHGuiTextField field;
	private CFHGuiList<InviteForm.Candidate> list;
	private final List<InviteForm.Candidate> all = new ArrayList<>();
	private FormPart.Host host;
	private boolean choosing;

	@Override
	public Component title() {
		return TeamsText.t("invite.title");
	}

	@Override
	public Icon icon() {
		return Icon.INVITE;
	}

	@Override
	public int preferredWidth() {
		return 216;
	}

	@Override
	public int build(int width, FormPart.Placer placer) {
		this.collect();
		this.field = new HintField(0, 0, width, TeamsText.t("invite.name")).icon(Icon.USER).maxLength(16);
		this.field.onChange(s -> {
			if (!this.choosing) {
				this.filter();
			}
		});
		placer.place(this.field, 0, 0);
		this.list = new CFHGuiList(0, 0, width, 90, 16, new InviteForm.Row()).gap(2);
		this.list.emptyText(TeamsText.t(this.all.isEmpty() ? "invite.nobody" : "invite.no_match"));
		this.list.onSelect(this::choose);
		this.list.onActivate(c -> {
			this.choose(c);
			if (this.host != null) {
				this.submit(this.host);
			}
		});
		placer.place(this.list, 0, 36);
		this.filter();
		return 126;
	}

	private void collect() {
		this.all.clear();
		TeamData team = TeamsView.team();
		Set<String> taken = new HashSet<>();
		if (team != null) {
			for (TeamData.Member m : team.members) {
				taken.add(m.name.toLowerCase(Locale.ROOT));
			}

			for (TeamData.Invite i : team.invites) {
				taken.add(i.playerName.toLowerCase(Locale.ROOT));
			}
		}

		taken.add(TeamsView.selfName().toLowerCase(Locale.ROOT));
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() != null) {
			for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
				String n = info.getProfile().getName();
				if (n != null && !n.isEmpty() && taken.add(n.toLowerCase(Locale.ROOT))) {
					this.all.add(new InviteForm.Candidate(n, info.getProfile().getId(), false, true));
				}
			}
		} else if (TeamsView.preview()) {
			for (TeamSummary s : TeamsView.directory()) {
				if (!s.ownerName.isEmpty() && taken.add(s.ownerName.toLowerCase(Locale.ROOT))) {
					this.all.add(new InviteForm.Candidate(s.ownerName, s.owner, false, true));
				}
			}
		}

		if (Modules.isLoaded("cfhgui_multiplayer")) {
			InviteForm.Friends.each(c -> {
				if (taken.add(c.name().toLowerCase(Locale.ROOT))) {
					this.all.add(c);
				}
			});
		}

		this.all.sort((a, b) -> a.online() != b.online() ? (a.online() ? -1 : 1) : a.name().compareToIgnoreCase(b.name()));
	}

	private void choose(InviteForm.Candidate c) {
		this.choosing = true;
		this.field.setValue(c.name());
		this.choosing = false;
	}

	private void filter() {
		String q = this.field.getValue().trim().toLowerCase(Locale.ROOT);
		List<InviteForm.Candidate> shown = new ArrayList<>();

		for (InviteForm.Candidate c : this.all) {
			if (q.isEmpty() || c.name().toLowerCase(Locale.ROOT).contains(q)) {
				shown.add(c);
			}
		}

		shown.sort((a, b) -> Boolean.compare(!a.name().toLowerCase(Locale.ROOT).startsWith(q), !b.name().toLowerCase(Locale.ROOT).startsWith(q)));
		this.list.clearSelection();
		this.list.items(shown);
		this.list.emptyText(TeamsText.t(this.all.isEmpty() ? "invite.nobody" : "invite.no_match"));
	}

	@Override
	public void render(GuiGraphics g, int x, int y, int width, float alpha, int mouseX, int mouseY) {
		TeamsDraw.text(g, TeamsText.t("invite.suggestions"), x, y + 25, TeamsDraw.caption(), alpha);
	}

	@Override
	public Component submitLabel() {
		return TeamsText.t("invite.submit");
	}

	@Override
	public boolean ready() {
		return !this.field.getValue().isBlank();
	}

	@Override
	public void submit(FormPart.Host host) {
		String typed = this.field.getValue().trim();
		if (!typed.isEmpty()) {
			InviteForm.Candidate known = null;

			for (InviteForm.Candidate c : this.all) {
				if (c.name().equalsIgnoreCase(typed)) {
					known = c;
					break;
				}
			}

			Consumer<TeamsNet.Result> then = result -> {
				if (!result.ok()) {
					host.fail(TeamFlows.reason(result));
				} else {
					TeamFlows.tell(TeamsText.t("invite.sent", typed));
					host.done();
				}
			};
			if (known != null && known.id() != null) {
				TeamActions.invite(known.id(), then);
			} else {
				TeamActions.invite(typed, then);
			}
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return (keyCode == 264 || keyCode == 265) && !this.list.items().isEmpty() ? this.list.keyPressed(keyCode, scanCode, modifiers) : false;
	}

	public InviteForm host(FormPart.Host host) {
		this.host = host;
		return this;
	}

	@Nullable
	@Override
	public AbstractWidget firstFocus() {
		return this.field;
	}

	private static record Candidate(String name, @Nullable UUID id, boolean friend, boolean online) {
	}

	private static final class Friends {
		static void each(Consumer<InviteForm.Candidate> out) {
			try {
				Class<?> client = Class.forName("com.onehumanawa.cfhcore.content.gui.multiplayer.client.SocialClient");
				Object social = client.getMethod("get").invoke(null);
				Object friends = client.getMethod("friends").invoke(social);
				if (!(friends instanceof Collection)) {
					return;
				}

				for (Object f : (Collection)friends) {
					Field uuid = f.getClass().getField("uuid");
					Field name = f.getClass().getField("name");
					Method online = f.getClass().getMethod("online");
					Object n = name.get(f);
					Object id = uuid.get(f);
					if (n instanceof String) {
						String s = (String)n;
						if (!s.isEmpty()) {
							out.accept(new InviteForm.Candidate(s, id instanceof UUID u ? u : null, true, Boolean.TRUE.equals(online.invoke(f))));
						}
					}
				}
			} catch (RuntimeException | ReflectiveOperationException var14) {
				CFHGuiTeams.LOGGER.debug("[CFHGui Teams] the friends of CFHGui Multiplayer could not be read: {}", var14.toString());
			}
		}
	}

	private static final class Row implements RowRenderer<InviteForm.Candidate> {
		public void render(GuiGraphics g, InviteForm.Candidate c, int index, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
			Palette p = Theme.current().palette();
			TeamsDraw.head(g, c.id() == null ? new UUID(0L, (long)c.name().hashCode()) : c.id(), c.name(), x + 4, y + (h - 10) / 2, 10, null, 1.0F);
			Component tag = TeamsText.t(c.friend() ? "invite.friend" : "invite.online");
			int tagW = CFHGuiDraw.width(tag);
			TeamsDraw.textFit(g, Component.literal(c.name()), x + 18, y + (h - 8) / 2, w - 30 - tagW - 12, !hovered && !selected ? p.textMuted() : p.text(), 1.0F);
			if (c.friend()) {
				Icons.draw(g, Icon.FRIENDS, x + w - tagW - 16, y + (h - 8) / 2, 8, p.textDim());
			}

			TeamsDraw.textRight(g, tag, x + w - 5, y + (h - 8) / 2, TeamsDraw.caption(), 1.0F);
		}
	}
}
