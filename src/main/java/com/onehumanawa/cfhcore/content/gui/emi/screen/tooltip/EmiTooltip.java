package com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;

public class EmiTooltip {
	public static final DecimalFormat TEXT_FORMAT = new DecimalFormat("0.##");

	public static ClientTooltipComponent chance(String type, float chance) {
		return ClientTooltipComponent.create(
			EmiPort.ordered(EmiPort.translatable("tooltip.emi.chance." + type, TEXT_FORMAT.format((double)(chance * 100.0F))).withStyle(ChatFormatting.GOLD))
		);
	}

	public static List<ClientTooltipComponent> splitTranslate(String key) {
		return Arrays.stream(I18n.get(key, new Object[0]).split("\n")).map(s -> ClientTooltipComponent.create(EmiPort.ordered(EmiPort.literal(s)))).toList();
	}

	public static List<ClientTooltipComponent> splitTranslate(String key, Object... objects) {
		return Arrays.stream(I18n.get(key, objects).split("\n")).map(s -> ClientTooltipComponent.create(EmiPort.ordered(EmiPort.literal(s)))).toList();
	}
}
