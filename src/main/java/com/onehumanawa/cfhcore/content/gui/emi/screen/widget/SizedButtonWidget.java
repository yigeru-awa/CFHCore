package com.onehumanawa.cfhcore.content.gui.emi.screen.widget;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.crafting.look.LookButtons;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public class SizedButtonWidget extends Button {
	private final BooleanSupplier isActive;
	private final IntSupplier vOffset;
	protected ResourceLocation texture = EmiRenderHelper.BUTTONS;
	protected Supplier<List<Component>> text;
	protected int u;
	protected int v;

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, OnPress action) {
		this(x, y, width, height, u, v, isActive, action, () -> 0);
	}

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, OnPress action, List<Component> text) {
		this(x, y, width, height, u, v, isActive, action, () -> 0, () -> text);
	}

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, OnPress action, IntSupplier vOffset) {
		this(x, y, width, height, u, v, isActive, action, vOffset, null);
	}

	public SizedButtonWidget(
		int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, OnPress action, IntSupplier vOffset, Supplier<List<Component>> text
	) {
		super(x, y, width, height, EmiPort.literal(""), action, s -> (MutableComponent)s.get());
		this.u = u;
		this.v = v;
		this.isActive = isActive;
		this.vOffset = vOffset;
		this.text = text;
	}

	protected int getU(int mouseX, int mouseY) {
		return this.u;
	}

	protected int getV(int mouseX, int mouseY) {
		int v = this.v + this.vOffset.getAsInt();
		this.active = this.isActive.getAsBoolean();
		if (!this.active) {
			v += this.height * 2;
		} else if (this.isMouseOver((double)mouseX, (double)mouseY)) {
			v += this.height;
		}

		return v;
	}

	public void renderWidget(GuiGraphics raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.enableDepthTest();
		int u = this.getU(mouseX, mouseY);
		int v = this.getV(mouseX, mouseY);
		if (!LookButtons.sized(this, raw, mouseX, mouseY, this.texture, u, this.v, this.vOffset.getAsInt(), this.text)) {
			context.drawTexture(this.texture, this.x, this.y, u, v, this.width, this.height);
			if (this.isMouseOver((double)mouseX, (double)mouseY) && this.text != null && this.active) {
				context.push();
				context.disableDepthTest();
				Minecraft client = Minecraft.getInstance();
				EmiRenderHelper.drawTooltip(
					client.screen,
					context,
					this.text.get().stream().map(EmiPort::ordered).<ClientTooltipComponent>map(ClientTooltipComponent::create).toList(),
					mouseX,
					mouseY
				);
				context.pop();
			}
		}
	}

	public void playDownSound(SoundManager soundManager) {
		CFHGuiSounds.click(soundManager);
	}
}
