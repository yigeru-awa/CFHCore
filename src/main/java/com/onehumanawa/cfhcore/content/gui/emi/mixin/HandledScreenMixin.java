package com.onehumanawa.cfhcore.content.gui.emi.mixin;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.platform.EmiAgnos;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractContainerScreen.class})
public abstract class HandledScreenMixin extends Screen {
	@Shadow
	protected int imageWidth;
	@Shadow
	protected int imageHeight;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;

	private HandledScreenMixin() {
		super(null);
	}

	@Inject(
		at = {@At(
			value = "INVOKE",
			target = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
			shift = Shift.AFTER
		)},
		method = {"renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"}
	)
	@Dynamic
	private void renderBackground(GuiGraphics raw, int mouseX, int mouseY, float delta, CallbackInfo info) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		EmiScreenManager.drawBackground(context, mouseX, mouseY, delta);
	}

	@Inject(
		at = {@At(
			value = "INVOKE",
			target = "net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
			shift = Shift.AFTER
		)},
		method = {"render"}
	)
	private void renderForeground(GuiGraphics raw, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (!EmiAgnos.isForge()) {
			EmiDrawContext context = EmiDrawContext.wrap(raw);
			context.push();
			context.translate((float)(-this.leftPos), (float)(-this.topPos));
			EmiPort.setPositionTexShader();
			EmiScreenManager.render(context, mouseX, mouseY, delta);
			EmiScreenManager.drawForeground(context, mouseX, mouseY, delta);
			context.pop();
		}
	}
}
