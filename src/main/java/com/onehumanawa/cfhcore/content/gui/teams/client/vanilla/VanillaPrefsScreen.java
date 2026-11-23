package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings.TeamsSettings;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class VanillaPrefsScreen extends Screen {
	@Nullable
	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	@Nullable
	private VanillaPrefsScreen.PrefList list;

	public VanillaPrefsScreen(@Nullable Screen parent) {
		super(TeamsText.t("settings.display"));
		this.parent = parent;
	}

	protected void init() {
		this.layout.addTitleHeader(this.title, this.font);
		this.list = new VanillaPrefsScreen.PrefList(this.minecraft);

		for (TeamsSettings.Group group : TeamsSettings.groups()) {
			this.list.add(new VanillaPrefsScreen.PrefRow(group.title(), null));

			for (TeamsSettings.Setting s : group.settings()) {
				this.list.add(new VanillaPrefsScreen.PrefRow(null, control(s)));
			}
		}

		this.layout.addToContents(this.list);
		this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose()).width(200).build());
		this.layout.visitWidgets(x$0 -> {
			AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(x$0);
		});
		this.repositionElements();
	}

	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.list != null) {
			this.list.updateSize(this.width, this.layout);
		}
	}

	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}

	private static AbstractWidget control(TeamsSettings.Setting s) {
		AbstractWidget w = (AbstractWidget)(switch (s.type()) {
			case BOOL -> CycleButton.onOffBuilder(Boolean.TRUE.equals(s.get().get())).create(0, 0, 310, 20, s.label(), (button, value) -> s.set().accept(value));
			case CHOICE -> CycleButton.builder(s::choiceLabel)
			.withValues(s.choices())
			.withInitialValue(String.valueOf(s.get().get()))
			.create(0, 0, 310, 20, s.label(), (button, value) -> s.set().accept(value));
			default -> new VanillaPrefsScreen.Slider(s);
		});
		w.setTooltip(Tooltip.create(s.tooltip()));
		return w;
	}

	private static final class PrefList extends ContainerObjectSelectionList<VanillaPrefsScreen.PrefRow> {
		PrefList(Minecraft mc) {
			super(mc, 100, 100, 0, 24);
		}

		void add(VanillaPrefsScreen.PrefRow entry) {
			this.addEntry(entry);
		}

		public int getRowWidth() {
			return 310;
		}
	}

	private static final class PrefRow extends Entry<VanillaPrefsScreen.PrefRow> {
		@Nullable
		private final Component caption;
		@Nullable
		private final AbstractWidget control;

		PrefRow(@Nullable Component caption, @Nullable AbstractWidget control) {
			this.caption = caption;
			this.control = control;
		}

		public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			if (this.caption != null) {
				TeamsDraw.textCentered(g, this.caption, left + width / 2, top + (height - 8) / 2 + 2, Theme.current().palette().text(), 1.0F);
			}

			if (this.control != null) {
				this.control.setX(left);
				this.control.setY(top);
				this.control.setWidth(width);
				this.control.render(g, mouseX, mouseY, partialTick);
			}
		}

		public List<? extends GuiEventListener> children() {
			return this.control == null ? List.of() : List.of(this.control);
		}

		public List<? extends NarratableEntry> narratables() {
			return this.control == null ? List.of() : List.of(this.control);
		}
	}

	private static final class Slider extends AbstractSliderButton {
		private final TeamsSettings.Setting setting;

		Slider(TeamsSettings.Setting setting) {
			super(0, 0, 310, 20, Component.empty(), fraction(setting, ((Number)setting.get().get()).doubleValue()));
			this.setting = setting;
			this.updateMessage();
		}

		private static double fraction(TeamsSettings.Setting s, double v) {
			return s.max() <= s.min() ? 0.0 : Mth.clamp((v - s.min()) / (s.max() - s.min()), 0.0, 1.0);
		}

		private double current() {
			double v = this.setting.min() + this.value * (this.setting.max() - this.setting.min());
			if (this.setting.step() > 0.0) {
				v = (double)Math.round((v - this.setting.min()) / this.setting.step()) * this.setting.step() + this.setting.min();
			}

			return Mth.clamp(v, this.setting.min(), this.setting.max());
		}

		protected void updateMessage() {
			double v = this.current();
			Component text = (Component)(this.setting.format() != null
				? this.setting.format().apply(v)
				: Component.literal(this.setting.type() == TeamsSettings.Type.INT ? Integer.toString((int)Math.round(v)) : String.format(Locale.ROOT, "%.2f", v)));
			this.setMessage(Component.empty().append(this.setting.label()).append(": ").append(text));
		}

		protected void applyValue() {
			this.setting.set().accept(this.current());
		}
	}
}
