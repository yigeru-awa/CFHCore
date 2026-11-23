package com.onehumanawa.cfhcore.content.gui.emi.mixin;

import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MouseMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private double xpos;
	@Shadow
	private double ypos;
	@Shadow
	private int activeButton = -1;
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(
		at = {@At(
			value = "INVOKE",
			ordinal = 0,
			target = "net/minecraft/client/gui/screens/Screen.wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"
		)},
		method = {"onPress(JIII)V"},
		cancellable = true
	)
	private void onMouseDown(long window, int button, int action, int mods, CallbackInfo info) {
		try {
			if (this.minecraft.screen instanceof AbstractContainerScreen<?> hs) {
				double mx = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
				double my = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
				if (EmiScreenManager.mouseClicked(mx, my, button)) {
					info.cancel();
				}
			}
		} catch (Exception var13) {
			EmiLog.error("Error while handling mouse press", var13);
		}
	}

	@Inject(
		at = {@At(
			value = "INVOKE",
			ordinal = 1,
			target = "net/minecraft/client/gui/screens/Screen.wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"
		)},
		method = {"onPress(JIII)V"},
		cancellable = true
	)
	private void onMouseUp(long window, int button, int action, int mods, CallbackInfo info) {
		try {
			if (this.minecraft.screen instanceof AbstractContainerScreen<?> hs) {
				double mx = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
				double my = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
				if (EmiScreenManager.mouseReleased(mx, my, button)) {
					info.cancel();
				}
			}
		} catch (Exception var13) {
			EmiLog.error("Error while handling mouse release", var13);
		}
	}

	@Inject(
		at = {@At(
			value = "INVOKE",
			ordinal = 1,
			target = "net/minecraft/client/gui/screens/Screen.wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"
		)},
		method = {"handleAccumulatedMovement"},
		cancellable = true
	)
	private void onMouseDragged(CallbackInfo info) {
		try {
			if (this.minecraft.screen instanceof AbstractContainerScreen<?> hs) {
				double mx = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
				double my = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
				double dx = this.accumulatedDX * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
				double dy = this.accumulatedDY * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
				EmiScreenManager.mouseDragged(mx, my, this.activeButton, dx, dy);
			}
		} catch (Exception var12) {
			EmiLog.error("Error while handling mouse drag", var12);
		}
	}

	@Inject(
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"
		)},
		method = {"onScroll(JDD)V"},
		cancellable = true
	)
	private void onMouseScrolled(long window, double horizontal, double vertical, CallbackInfo info) {
		try {
			if (this.minecraft.screen instanceof AbstractContainerScreen<?> hs) {
				double amount = (this.minecraft.options.discreteMouseScroll().get() ? Math.signum(vertical) : vertical)
					* (Double)(Object)this.minecraft.options.mouseWheelSensitivity().get();
				double mx = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
				double my = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
				if (EmiScreenManager.mouseScrolled(mx, my, amount)) {
					info.cancel();
				}
			}
		} catch (Exception var16) {
			EmiLog.error("Error while handling mouse scroll", var16);
		}
	}
}
