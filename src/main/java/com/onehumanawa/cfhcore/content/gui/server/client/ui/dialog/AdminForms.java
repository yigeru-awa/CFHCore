package com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.ChipChoice;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.MarkChoice;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.ServerDialog;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.model.Kit;
import com.onehumanawa.cfhcore.content.gui.server.model.Punishment;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class AdminForms {
	private static final int MINUTE = 60;
	private static final int HOUR = 3600;
	private static final int DAY = 86400;

	private AdminForms() {
	}

	public static final class GrantForm extends ServerDialog {
		private final ChipChoice<Role> role;
		private final ChipChoice<AdminForms.Span> span;
		private final CFHGuiTextField custom;
		private final CFHGuiButton ok;
		private final Runnable done;

		public GrantForm(UUID player, String playerName, List<Role> roles, Runnable done) {
			super(Txt.t("grant.title", playerName), Icon.CROWN, 320);
			this.done = done;
			this.role = this.row(
				Txt.t("grant.role"), new ChipChoice<>(this.bodyWidth(), roles, roles.isEmpty() ? null : roles.get(0), r -> Component.literal(r.name), r -> {
				})
			);
			List<AdminForms.Span> spans = List.of(
				new AdminForms.Span(0, Txt.t("punish.for_good")),
				AdminForms.Span.of(86400),
				AdminForms.Span.of(604800),
				AdminForms.Span.of(2592000),
				AdminForms.Span.of(7776000)
			);
			this.span = this.row(Txt.t("grant.span"), new ChipChoice<>(this.bodyWidth(), spans, spans.get(0), AdminForms.Span::label, s -> this.clearCustom()));
			this.custom = this.row(
				Txt.t("punish.span.custom"), new CFHGuiTextField(0, 0, 100, Txt.t("punish.span.custom")).maxLength(16).placeholder(Txt.t("punish.span.hint"))
			);
			this.custom.setResponder(s -> {
				if (!s.isBlank()) {
					this.span.clear();
				}
			});
			this.ok = this.buttons(Txt.t("grant.ok"), Variant.PRIMARY, () -> {
				Role r = this.role.value();
				if (r != null) {
					this.send(then -> ServerActions.roleGrant(player, r.id, this.seconds(), then));
				}
			});
		}

		private void clearCustom() {
			this.custom.setValue("");
		}

		private int seconds() {
			if (!this.custom.getValue().isBlank()) {
				return (int)Math.min(2147483647L, Txt.parseSpan(this.custom.getValue()));
			} else {
				AdminForms.Span s = this.span.value();
				return s == null ? -1 : s.seconds();
			}
		}

		@Override
		protected void update() {
			this.custom.setInvalid(!this.custom.getValue().isBlank() && this.seconds() < 0);
			this.ok.active = this.role.value() != null && this.seconds() >= 0 && !this.waiting;
		}

		@Override
		protected void onDone() {
			this.done.run();
		}
	}

	public static final class KitForm extends ServerDialog {
		@Nullable
		private final Kit edit;
		private final Set<String> taken;
		private final CFHGuiTextField name;
		private final CFHGuiTextField wait;
		private final MarkChoice mark;
		private final CFHGuiToggle once;
		private final CFHGuiToggle first;
		private final CFHGuiToggle restricted;
		private final CFHGuiButton ok;
		private final Runnable done;
		private String clash = "";

		public KitForm(@Nullable Kit edit, Collection<Kit> kits, Runnable done) {
			super(Txt.t(edit == null ? "kit.new.title" : "kit.edit.title"), Icon.PACK, 320);
			this.edit = edit;
			this.done = done;
			this.taken = new HashSet<>();

			for (Kit k : kits) {
				this.taken.add(k.id);
			}

			if (edit == null) {
				this.words(Txt.t("kit.new.words"));
			}

			this.name = this.row(Txt.t("form.name"), new CFHGuiTextField(0, 0, 100, Txt.t("form.name")).maxLength(32).placeholder(Txt.t("kit.name.hint")));
			this.name.setValue(edit == null ? "" : edit.name);
			this.wait = this.row(Txt.t("kit.wait.caption"), new CFHGuiTextField(0, 0, 100, Txt.t("kit.wait.caption")).maxLength(16).placeholder(Txt.t("kit.wait.hint")));
			this.wait.setValue(edit != null && edit.cooldownSeconds != 0 ? Txt.spanTyped((long)edit.cooldownSeconds) : "");
			this.mark = this.row(Txt.t("form.icon"), new MarkChoice(this.bodyWidth(), edit == null ? "" : edit.icon, Icon.PACK, () -> 14254174, m -> {
			}));
			this.once = this.line(new CFHGuiToggle(0, 0, this.bodyWidth(), Txt.t("kit.once"), edit != null && edit.oneTime, v -> {
			}));
			this.first = this.line(new CFHGuiToggle(0, 0, this.bodyWidth(), Txt.t("kit.first_join"), edit != null && edit.firstJoin, v -> {
			}));
			this.restricted = this.line(new CFHGuiToggle(0, 0, this.bodyWidth(), Txt.t("kit.restricted"), edit != null && edit.restricted, v -> {
			}));
			this.restricted.tip(Txt.t("kit.restricted.tip"));
			this.ok = this.buttons(Txt.t(edit == null ? "kit.new.ok" : "form.save"), Variant.PRIMARY, this::go);
			this.firstFocus(this.name);
		}

		private long seconds() {
			return this.wait.getValue().isBlank() ? 0L : Txt.parseSpan(this.wait.getValue());
		}

		@Override
		protected void update() {
			String id = Role.idOf(this.name.getValue());
			String now = this.edit == null && this.taken.contains(id) ? id : "";
			if (!now.equals(this.clash)) {
				this.clash = now;
				if (now.isEmpty()) {
					this.clearError();
				} else {
					this.fail(Txt.t("kit.name.taken", now));
				}
			}

			this.name.setInvalid(!this.clash.isEmpty());
			this.wait.setInvalid(this.seconds() < 0L);
			this.ok.active = !id.isEmpty() && this.clash.isEmpty() && this.seconds() >= 0L && !this.waiting;
		}

		private void go() {
			Kit kit = new Kit();
			kit.id = this.edit == null ? Role.idOf(this.name.getValue()) : this.edit.id;
			kit.name = this.name.getValue().trim();
			kit.icon = this.mark.value();
			kit.cooldownSeconds = (int)Math.min(2147483647L, this.seconds());
			kit.oneTime = this.once.value();
			kit.firstJoin = this.first.value();
			kit.restricted = this.restricted.value();
			if (this.edit != null) {
				kit.items = this.edit.items;
			}

			this.send(then -> ServerActions.kitSave(kit, this.edit == null, then));
		}

		@Override
		protected void onDone() {
			this.done.run();
		}
	}

	public static final class LinesForm extends ServerDialog {
		private static final int ROW = 22;
		private static final int MOST = 6;
		private final CFHGuiScrollPanel panel;
		private final List<CFHGuiTextField> fields = new ArrayList<>();
		private final Predicate<String> valid;
		private final CFHGuiButton ok;

		public LinesForm(
			Component title,
			@Nullable Component hint,
			List<String> lines,
			int maxLength,
			Predicate<String> valid,
			BiConsumer<List<String>, Consumer<ServerNet.Result>> sender
		) {
			super(title, Icon.LIST, 360);
			this.valid = valid;
			if (hint != null) {
				this.words(hint);
			}

			this.panel = new CFHGuiScrollPanel(0, 0, this.bodyWidth(), 132);
			this.panel.background(false);
			this.add(this.panel, 0, this.cursor);
			this.cursor += 136;

			for (String line : lines) {
				this.addLine(line, maxLength);
			}

			if (lines.isEmpty()) {
				this.addLine("", maxLength);
			}

			CFHGuiButton more = new CFHGuiButton(0, 0, 90, 18, Txt.t("lines.add"), () -> {
				this.addLine("", maxLength);
				this.panel.scrollTo((double)this.panel.maxScroll());
			}).icon(Icon.PLUS).iconSize(8);
			more.setWidth(more.preferredWidth() + 12);
			this.add(more, 0, this.cursor);
			this.cursor += 25;
			this.ok = this.buttons(Txt.t("form.save"), Variant.PRIMARY, () -> this.send(then -> sender.accept(this.values(), then)));
		}

		private void addLine(String value, int maxLength) {
			CFHGuiTextField f = new CFHGuiTextField(0, 0, this.panel.innerWidth() - 22, 18, Txt.t("lines.line")).maxLength(maxLength);
			f.setValue(value);
			f.moveCursorToStart(false);
			this.fields.add(f);
			this.relay();
		}

		private void relay() {
			this.panel.clear();

			for (int i = 0; i < this.fields.size(); i++) {
				CFHGuiTextField f = this.fields.get(i);
				this.panel.add(f, 0, i * 22 + 1);
				this.panel.add(new CFHGuiIconButton(0, 0, 18, Icon.CLOSE, Txt.t("lines.remove"), () -> {
					this.fields.remove(f);
					this.relay();
				}), this.panel.innerWidth() - 20, i * 22 + 1);
			}

			this.panel.setContentHeight(this.fields.size() * 22);
		}

		private List<String> values() {
			List<String> out = new ArrayList<>();

			for (CFHGuiTextField f : this.fields) {
				if (!f.getValue().isBlank()) {
					out.add(f.getValue().trim());
				}
			}

			return out;
		}

		@Override
		protected void update() {
			boolean good = true;

			for (CFHGuiTextField f : this.fields) {
				boolean bad = !f.getValue().isBlank() && !this.valid.test(f.getValue().trim());
				f.setInvalid(bad);
				good &= !bad;
			}

			this.ok.active = good && !this.waiting;
		}
	}

	public static final class PickForm<T> extends ServerDialog {
		private final ChipChoice<T> choice;
		private final CFHGuiButton ok;

		public PickForm(
			Component title,
			Icon icon,
			Component caption,
			List<T> options,
			Function<T, Component> labeler,
			Component okLabel,
			BiConsumer<T, Consumer<ServerNet.Result>> sender
		) {
			super(title, icon, 300);
			this.choice = this.row(caption, new ChipChoice<>(this.bodyWidth(), options, options.isEmpty() ? null : options.get(0), labeler, v -> {
			}));
			this.ok = this.buttons(okLabel, Variant.PRIMARY, () -> {
				T v = this.choice.value();
				if (v != null) {
					this.send(then -> sender.accept(v, then));
				}
			});
		}

		@Override
		protected void update() {
			this.ok.active = this.choice.value() != null && !this.waiting;
		}
	}

	public static final class PunishForm extends ServerDialog {
		private final Punishment.Kind kind;
		private final boolean mayForGood;
		@Nullable
		private ChipChoice<AdminForms.Span> span;
		@Nullable
		private CFHGuiTextField custom;
		private final CFHGuiTextField reason;
		private final CFHGuiButton ok;
		private final Runnable done;

		public PunishForm(UUID player, String playerName, Punishment.Kind kind, Runnable done) {
			super(Txt.t("punish." + kind.key() + ".title", playerName), Icon.WARNING, 320);
			this.kind = kind;
			this.mayForGood = kind != Punishment.Kind.BAN || ServerState.may(Nodes.MOD_BAN_PERMANENT);
			this.done = done;
			if (kind.lasts()) {
				List<AdminForms.Span> spans = new ArrayList<>();

				for (int s : kind == Punishment.Kind.BAN ? new int[]{86400, 259200, 604800, 2592000} : new int[]{600, 3600, 21600, 86400, 604800}) {
					spans.add(AdminForms.Span.of(s));
				}

				if (this.mayForGood) {
					spans.add(new AdminForms.Span(0, Txt.t(kind == Punishment.Kind.BAN ? "punish.for_good" : "punish.until_lifted")));
				}

				this.span = this.row(
					Txt.t("punish.span"), new ChipChoice<>(this.bodyWidth(), spans, spans.get(kind == Punishment.Kind.BAN ? 2 : 1), AdminForms.Span::label, sx -> {
						if (this.custom != null) {
							this.custom.setValue("");
						}
					})
				);
				this.custom = this.row(
					Txt.t("punish.span.custom"), new CFHGuiTextField(0, 0, 100, Txt.t("punish.span.custom")).maxLength(16).placeholder(Txt.t("punish.span.hint"))
				);
				this.custom.setResponder(sx -> {
					if (!sx.isBlank() && this.span != null) {
						this.span.clear();
					}
				});
			}

			this.reason = this.row(Txt.t("punish.reason"), new CFHGuiTextField(0, 0, 100, Txt.t("punish.reason")).maxLength(160).placeholder(Txt.t("punish.reason.hint")));
			List<String> quick = List.of("spam", "grief", "rude", "cheat", "ads");
			this.line(new ChipChoice<String>(this.bodyWidth(), quick, null, k -> Txt.t("punish.quick." + k), k -> this.reason.setValue(Txt.s("punish.quick." + k))));
			this.cursor += 2;
			this.ok = this.buttons(
				Txt.t("punish." + kind.key() + ".ok"),
				Variant.DANGER,
				() -> this.send(then -> ServerActions.punish(player, kind, this.reason.getValue().trim(), this.seconds(), then))
			);
			this.firstFocus(this.reason);
		}

		private int seconds() {
			if (!this.kind.lasts() || this.span == null || this.custom == null) {
				return 0;
			} else if (!this.custom.getValue().isBlank()) {
				return (int)Math.min(2147483647L, Txt.parseSpan(this.custom.getValue()));
			} else {
				AdminForms.Span s = this.span.value();
				return s == null ? -1 : s.seconds();
			}
		}

		@Override
		protected void update() {
			int s = this.seconds();
			boolean typed = this.custom != null && !this.custom.getValue().isBlank();
			if (this.custom != null) {
				this.custom.setInvalid(typed && s < 0);
			}

			boolean reasoned = this.kind != Punishment.Kind.WARN || !this.reason.getValue().isBlank();
			this.ok.active = s >= 0 && reasoned && !this.waiting;
		}

		@Override
		protected void onDone() {
			this.done.run();
		}
	}

	public static record Span(int seconds, Component label) {
		static AdminForms.Span of(int seconds) {
			return new AdminForms.Span(seconds, Component.literal(Txt.span((long)seconds)));
		}
	}
}
