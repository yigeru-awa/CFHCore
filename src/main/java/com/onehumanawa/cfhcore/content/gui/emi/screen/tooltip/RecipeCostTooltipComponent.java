package com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.bom.ChanceMaterialCost;
import com.onehumanawa.cfhcore.content.gui.emi.bom.FlatMaterialCost;
import com.onehumanawa.cfhcore.content.gui.emi.bom.MaterialTree;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackList;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class RecipeCostTooltipComponent implements EmiTooltipComponent {
	private static final Component COST = EmiPort.translatable("emi.cost_per");
	private final List<RecipeCostTooltipComponent.Node> nodes = Lists.newArrayList();
	public final MaterialTree tree;
	private int maxWidth = 0;

	public RecipeCostTooltipComponent(EmiRecipe recipe) {
		this.tree = new MaterialTree(recipe);
		this.tree.batches = this.tree.cost.getIdealBatch(this.tree.goal, 1L, 1L);
		this.tree.calculateCost();
		this.addNodes();
	}

	public boolean shouldDisplay() {
		return !this.nodes.isEmpty();
	}

	public void addNodes() {
		double batches = (double)this.tree.batches;

		for (FlatMaterialCost cost : Stream.concat(this.tree.cost.costs.values().stream(), this.tree.cost.chanceCosts.values().stream())
			.sorted((a, b) -> Integer.compare(EmiStackList.getIndex(a.ingredient.getEmiStacks().get(0)), EmiStackList.getIndex(b.ingredient.getEmiStacks().get(0))))
			.toList()) {
			if (cost instanceof ChanceMaterialCost cmc) {
				this.nodes.add(new RecipeCostTooltipComponent.Node(cost.ingredient, (double)cost.amount / batches * (double)cmc.chance, true));
			} else {
				this.nodes.add(new RecipeCostTooltipComponent.Node(cost.ingredient, (double)cost.amount / batches, false));
			}
		}

		this.positionNodes();
	}

	public void positionNodes() {
		int wrapWidth = this.getWrapWidth();
		int padding = 8;
		int x = 0;
		int y = 10;
		this.maxWidth = 0;

		for (RecipeCostTooltipComponent.Node node : this.nodes) {
			int width = 16 + EmiRenderHelper.getAmountOverflow(node.text);
			if (x + width > wrapWidth) {
				x = 0;
				y += 18;
			}

			this.maxWidth = Math.max(this.maxWidth, x + width);
			node.x = x;
			node.y = y;
			x += width + padding;
		}
	}

	public int getWrapWidth() {
		return 160;
	}

	public int getHeight() {
		return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1).y + 18 : 10;
	}

	public int getWidth(Font textRenderer) {
		return Math.max(textRenderer.width(COST), this.maxWidth);
	}

	@Override
	public void drawTooltip(EmiDrawContext context, EmiTooltipComponent.TooltipRenderData render) {
		for (RecipeCostTooltipComponent.Node node : this.nodes) {
			context.drawStack(node.stack, node.x, node.y, -3);
			EmiRenderHelper.renderAmount(context, node.x, node.y, node.text);
		}
	}

	@Override
	public void drawTooltipText(EmiTooltipComponent.TextRenderData text) {
		text.draw(COST, 0, 0, ChatFormatting.GRAY.getColor(), true);
	}

	private static class Node {
		public final EmiIngredient stack;
		public final Component text;
		public int x;
		public int y;

		public Node(EmiIngredient stack, double amount, boolean chanced) {
			this.stack = stack;
			if (chanced) {
				this.text = EmiPort.append(EmiPort.literal("≈"), EmiRenderHelper.getAmountText(stack, amount)).withStyle(ChatFormatting.GOLD);
			} else {
				this.text = EmiRenderHelper.getAmountText(stack, amount);
			}
		}
	}
}
