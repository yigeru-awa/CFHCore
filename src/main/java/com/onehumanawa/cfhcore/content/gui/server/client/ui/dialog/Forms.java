package com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSwatches;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.server.ServerClientConfig;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.MarkChoice;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.ServerDialog;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Home;
import com.onehumanawa.cfhcore.content.gui.server.model.OnlinePlayer;
import com.onehumanawa.cfhcore.content.gui.server.model.Warp;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Forms {
	private static final Pattern PLACE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,24}");

	public static boolean placeName(String s) {
		return PLACE_NAME.matcher(s).matches();
	}

	public static void confirmDelete(Component title, Component body, Runnable delete) {
		if (!ServerClientConfig.get().confirmDeletes) {
			delete.run();
		} else {
			CFHGuiModal.confirmDanger(title, body, Txt.t("form.delete"), delete);
		}
	}

	public static void confirm(Component title, Component body, Component ok, Runnable run) {
		CFHGuiModal.confirmDanger(title, body, ok, run);
	}

	private Forms() {
	}

	public static final class HomeForm extends ServerDialog {
		@Nullable
		private final Home edit;
		private final CFHGuiTextField name;
		private final MarkChoice mark;
		private int color;
		private final CFHGuiButton ok;

		public HomeForm(@Nullable Home edit) {
			super(Txt.t(edit == null ? "home.new.title" : "home.edit.title"), Icon.HOME, 300);
			this.edit = edit;
			this.color = edit == null ? 14254174 : edit.color;
			this.name = this.row(Txt.t("form.name"), new CFHGuiTextField(0, 0, 100, Txt.t("form.name")).maxLength(24).placeholder(Txt.t("form.name.hint")));
			this.name.setValue(edit == null ? freeName() : edit.name);
			this.mark = this.row(Txt.t("form.icon"), new MarkChoice(this.bodyWidth(), edit == null ? "HOME" : edit.icon, Icon.HOME, () -> this.color, m -> {
			}));
			this.row(Txt.t("form.color"), new CFHGuiSwatches(0, 0, this.bodyWidth(), Palette.ACCENTS, this.color, c -> this.color = c & 16777215));
			this.ok = this.buttons(Txt.t(edit == null ? "home.new.ok" : "form.save"), Variant.PRIMARY, this::go);
			this.firstFocus(this.name);
		}

		private static String freeName() {
			List<Home> homes = ServerState.self().homes;

			for (int i = 1; i < 100; i++) {
				String candidate = i == 1 ? "home" : "home" + i;
				if (homes.stream().noneMatch(h -> h.name.equalsIgnoreCase(candidate))) {
					return candidate;
				}
			}

			return "";
		}

		@Nullable
		private static Home existing(String name) {
			for (Home h : ServerState.self().homes) {
				if (h.name.equalsIgnoreCase(name)) {
					return h;
				}
			}

			return null;
		}

		@Override
		protected void update() {
			String n = this.name.getValue().trim();
			boolean valid = Forms.placeName(n);
			this.name.setInvalid(!valid && !this.name.getValue().isEmpty());
			this.ok.active = valid && !this.waiting;
			if (this.edit == null) {
				this.ok.setMessage(Txt.t(existing(n) != null ? "home.move" : "home.new.ok"));
			}
		}

		private void go() {
			String n = this.name.getValue().trim();
			if (this.edit == null) {
				Home taken = existing(n);
				if (taken != null) {
					this.close();
					Forms.confirm(
						Txt.t("home.move.title", taken.name), Txt.t("home.move.body", taken.name), Txt.t("home.move"), () -> ServerActions.homeSet(taken.name, null, -1, null)
					);
				} else {
					this.send(then -> ServerActions.homeSet(n, this.mark.value(), this.color, then));
				}
			} else {
				CompoundTag changes = new CompoundTag();
				if (!n.equals(this.edit.name)) {
					changes.putString("rename", n);
				}

				changes.putString("icon", this.mark.value());
				changes.putInt("color", this.color & 16777215);
				this.send(then -> ServerActions.homeEdit(this.edit.name, changes, then));
			}
		}
	}

	public static final class MailForm extends ServerDialog {
		private final CFHGuiTextField to;
		private final CFHGuiTextField text;
		private final CFHGuiButton ok;
		private final List<String> suggestions = new ArrayList<>();
		private int suggestY;

		public MailForm(@Nullable String toName) {
			super(Txt.t("mail.write.title"), Icon.SEND, 320);
			this.to = this.row(Txt.t("mail.to"), new CFHGuiTextField(0, 0, 100, Txt.t("mail.to")).maxLength(32).placeholder(Txt.t("mail.to.hint")));
			this.to.setValue(toName == null ? "" : toName);
			this.suggestY = this.cursor - 7 + 2;
			this.cursor += 14;
			this.text = this.row(Txt.t("mail.text"), new CFHGuiTextField(0, 0, 100, Txt.t("mail.text")).maxLength(256).placeholder(Txt.t("mail.text.hint")));
			this.ok = this.buttons(
				Txt.t("mail.send"), Variant.PRIMARY, () -> this.send(then -> ServerActions.mailSend(this.to.getValue().trim(), this.text.getValue().trim(), then))
			);
			this.ok.icon(Icon.SEND);
			this.firstFocus(toName == null ? this.to : this.text);
		}

		@Override
		protected void update() {
			this.ok.active = !this.to.getValue().isBlank() && !this.text.getValue().isBlank() && !this.waiting;
			this.suggestions.clear();
			String typed = this.to.getValue().trim().toLowerCase(Locale.ROOT);
			String self = Minecraft.getInstance().getUser().getName();

			for (OnlinePlayer p : ServerState.online()) {
				if (!p.name.equalsIgnoreCase(self) && !p.name.equalsIgnoreCase(typed)) {
					if (typed.isEmpty() || p.name.toLowerCase(Locale.ROOT).startsWith(typed)) {
						this.suggestions.add(p.name);
					}

					if (this.suggestions.size() >= 5) {
						break;
					}
				}
			}
		}

		@Override
		protected void renderBody(GuiGraphics g, int x, int y, int w, float alpha, int mouseX, int mouseY) {
			Ui.textRight(g, Component.literal(this.text.getValue().length() + " / 256"), x + w, y + this.suggestY + 14 + 7 - 2, Ui.caption(), alpha);
			int cx = x;

			for (String name : this.suggestions) {
				Component label = Component.literal(name);
				int cw = Ui.chipWidth(label);
				if (cx + cw > x + w) {
					break;
				}

				boolean hot = mouseX >= cx && mouseX < cx + cw && mouseY >= y + this.suggestY && mouseY < y + this.suggestY + 12;
				Ui.chip(g, label, cx, y + this.suggestY, hot ? Ui.strong() : Ui.soft(), alpha);
				cx += cw + 4;
			}
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			int cx = this.x + 12;
			int top = this.y + 12 + 18 + this.suggestY;

			for (String name : this.suggestions) {
				int cw = Ui.chipWidth(Component.literal(name));
				if (mouseX >= (double)cx && mouseX < (double)(cx + cw) && mouseY >= (double)top && mouseY < (double)(top + 12)) {
					this.to.setValue(name);
					this.focus(this.text);
					return true;
				}

				cx += cw + 4;
			}

			return super.mouseClicked(mouseX, mouseY, button);
		}
	}

	public static final class Prompt extends ServerDialog {
		private final CFHGuiTextField field;
		private final CFHGuiButton ok;
		private final Predicate<String> valid;
		private final BiConsumer<String, Consumer<ServerNet.Result>> sender;
		private final int max;
		private boolean allowEmpty;

		public Prompt(
			Component title,
			Icon icon,
			Component caption,
			String initial,
			int maxLength,
			Component okLabel,
			Predicate<String> valid,
			BiConsumer<String, Consumer<ServerNet.Result>> sender
		) {
			super(title, icon, 280);
			this.valid = valid;
			this.sender = sender;
			this.max = maxLength;
			this.field = this.row(caption, new CFHGuiTextField(0, 0, 100, caption).maxLength(maxLength));
			this.field.setValue(initial == null ? "" : initial);
			this.ok = this.buttons(okLabel, Variant.PRIMARY, () -> this.send(then -> sender.accept(this.field.getValue().trim(), then)));
			this.firstFocus(this.field);
		}

		public Forms.Prompt hint(Component text) {
			this.hint(text, 0, this.cursor - 7 + 2);
			this.cursor += 11;
			return this;
		}

		public Forms.Prompt allowEmpty() {
			this.allowEmpty = true;
			return this;
		}

		@Override
		protected void update() {
			String v = this.field.getValue().trim();
			boolean good = v.isEmpty() ? this.allowEmpty : this.valid.test(v);
			this.field.setInvalid(!good && !v.isEmpty());
			this.ok.active = good && !this.waiting;
		}

		@Override
		protected void renderBody(GuiGraphics g, int x, int y, int w, float alpha, int mouseX, int mouseY) {
			if (this.max >= 64) {
				Ui.textRight(g, Component.literal(this.field.getValue().length() + " / " + this.max), x + w, y, Ui.caption(), alpha);
			}
		}
	}

	public static final class WarpForm extends ServerDialog {
		@Nullable
		private final Warp edit;
		private final CFHGuiTextField name;
		private final CFHGuiTextField description;
		private final MarkChoice mark;
		private final CFHGuiToggle restricted;
		private int color;
		private final CFHGuiButton ok;

		public WarpForm(@Nullable Warp edit) {
			super(Txt.t(edit == null ? "warp.new.title" : "warp.edit.title"), Icon.PIN, 300);
			this.edit = edit;
			this.color = edit == null ? 7317724 : edit.color;
			this.name = this.row(Txt.t("form.name"), new CFHGuiTextField(0, 0, 100, Txt.t("form.name")).maxLength(24).placeholder(Txt.t("form.name.hint")));
			this.name.setValue(edit == null ? "" : edit.name);
			this.description = this.row(
				Txt.t("form.description"), new CFHGuiTextField(0, 0, 100, Txt.t("form.description")).maxLength(120).placeholder(Txt.t("form.description.hint"))
			);
			this.description.setValue(edit == null ? "" : edit.description);
			this.mark = this.row(Txt.t("form.icon"), new MarkChoice(this.bodyWidth(), edit == null ? "PIN" : edit.icon, Icon.PIN, () -> this.color, m -> {
			}));
			this.row(Txt.t("form.color"), new CFHGuiSwatches(0, 0, this.bodyWidth(), Palette.ACCENTS, this.color, c -> this.color = c & 16777215));
			this.restricted = this.line(new CFHGuiToggle(0, 0, this.bodyWidth(), Txt.t("warp.restricted"), edit != null && edit.restricted, v -> {
			}));
			this.restricted.tip(Txt.t("warp.restricted.tip"));
			this.ok = this.buttons(Txt.t(edit == null ? "warp.new.ok" : "form.save"), Variant.PRIMARY, this::go);
			this.firstFocus(this.name);
		}

		@Nullable
		private static Warp existing(String name) {
			for (Warp w : ServerState.warps().warps) {
				if (w.name.equalsIgnoreCase(name)) {
					return w;
				}
			}

			return null;
		}

		@Override
		protected void update() {
			String n = this.name.getValue().trim();
			boolean valid = Forms.placeName(n);
			this.name.setInvalid(!valid && !this.name.getValue().isEmpty());
			this.ok.active = valid && !this.waiting;
			if (this.edit == null) {
				this.ok.setMessage(Txt.t(existing(n) != null ? "home.move" : "warp.new.ok"));
			}
		}

		private void go() {
			String n = this.name.getValue().trim();
			if (this.edit == null) {
				Warp taken = existing(n);
				if (taken != null) {
					this.close();
					Forms.confirm(
						Txt.t("warp.move.title", taken.name),
						Txt.t("warp.move.body", taken.name),
						Txt.t("home.move"),
						() -> ServerActions.warpSet(taken.name, null, -1, null, taken.restricted, null)
					);
				} else {
					this.send(then -> ServerActions.warpSet(n, this.mark.value(), this.color, this.description.getValue().trim(), this.restricted.value(), then));
				}
			} else {
				CompoundTag changes = new CompoundTag();
				if (!n.equals(this.edit.name)) {
					changes.putString("rename", n);
				}

				changes.putString("icon", this.mark.value());
				changes.putInt("color", this.color & 16777215);
				changes.putString("description", this.description.getValue().trim());
				changes.putBoolean("restricted", this.restricted.value());
				this.send(then -> ServerActions.warpEdit(this.edit.name, changes, then));
			}
		}
	}
}
