package com.onehumanawa.cfhcore.content.gui.quests.client.text;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.ItemSpecs;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class RichTextView extends CFHGuiWidget {
	private final RichText text;
	private final RichText.Host host;
	private RichText.Layout layout;

	public RichTextView(int x, int y, int width, RichText text, RichText.Host host) {
		super(x, y, width, 10, Component.empty());
		this.text = text;
		this.host = host;
		this.layout = text.layout(width);
		this.setHeight(Math.max(1, this.layout.height()));
		this.silent();
	}

	public void setWidth(int width) {
		super.setWidth(width);
		this.layout = this.text.layout(width);
		this.setHeight(Math.max(1, this.layout.height()));
	}

	public void onClick(double mouseX, double mouseY) {
		if (this.layout.click(mouseX - (double)this.getX(), mouseY - (double)this.getY() - (double)this.enterOffset(), this.host)) {
			CFHGuiSounds.click();
		}
	}

	public ComponentPath nextFocusPath(FocusNavigationEvent event) {
		return null;
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY) {
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			boolean over = this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
			this.layout.draw(g, this.getX(), this.getY() + this.enterOffset(), over ? mouseX : -9999, over ? mouseY : -9999, a);
			RichText.Run hot = this.layout.hovered();
			if (hot != null) {
				List<Component> tip = tip(hot);
				if (!tip.isEmpty()) {
					CFHGuiTooltips.request(tip, null);
				}
			}
		}
	}

	private static List<Component> tip(RichText.Run run) {
		if (run.item() != null) {
			return itemTip(ItemSpecs.stack(run.item()), ItemSpecs.name(run.item()));
		} else {
			List<Component> out = new ArrayList<>();
			if (run.quest() != null) {
				BookIndex.Entry e = QuestsState.entry(run.quest());
				if (e != null) {
					out.add(Fonts.heading(QuestText.title(e)));
					out.add(Component.translatable("cfhgui_quests.text.goto", new Object[]{QuestText.title(e.chapter())}).withStyle(s -> s.withColor(10592151)));
				}
			} else if (run.url() != null) {
				out.add(Component.literal(run.url()));
				out.add(Component.translatable("cfhgui_quests.text.open_link").withStyle(s -> s.withColor(10592151)));
			}

			return out;
		}
	}

	public static List<Component> itemTip(ItemStack stack, @Nullable Component fallback) {
		if (!stack.isEmpty()) {
			try {
				return Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
			} catch (Exception var3) {
			}
		}

		return fallback != null && !fallback.getString().isEmpty() ? List.of(fallback) : List.of();
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
	}
}
