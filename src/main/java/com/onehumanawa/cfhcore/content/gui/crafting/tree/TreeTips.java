package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.HelpLevel;
import com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip.RecipeTooltipComponent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

final class TreeTips {
	private static ClientTooltipComponent line(Component text) {
		return ClientTooltipComponent.create(text.getVisualOrderText());
	}

	private static MutableComponent colored(String key, int color, Object... args) {
		return Component.translatable(key, args).withStyle(s -> s.withColor(color & 16777215));
	}

	static MutableComponent counted(String key, long count, Object... args) {
		return Component.translatable(count == 1L ? key + ".one" : key, args);
	}

	static String amount(@Nullable EmiIngredient ingredient, long amount, float chance) {
		long shown = chance == 1.0F ? amount : (long)Math.max(amount > 0L ? 1 : 0, Math.round((float)amount * chance));
		String text = ingredient == null ? Long.toString(shown) : (shown == 0L ? "0" : EmiRenderHelper.getAmountText(ingredient, shown).getString());
		return chance == 1.0F ? text : "~" + text;
	}

	static List<ClientTooltipComponent> node(TreeSession session, TreeNode node) {
		List<ClientTooltipComponent> out = new ArrayList<>();
		EmiIngredient shown = node.ingredient();
		if (shown == null) {
			return out;
		} else {
			out.addAll(shown.getTooltip());
			TreeFigures f = session.figures(node.path);
			String have = amount(shown, f.have(), f.chance);
			String wanted = amount(shown, f.wanted, f.chance);
			String missing = amount(shown, f.missing, f.chance);
			switch (f.state) {
				case DONE:
					out.add(
						line(
							colored(
								f.wanted > 0L ? "cfhgui_crafting.tree.tip.done" : "cfhgui_crafting.tree.tip.unasked", f.wanted > 0L ? TreePaint.success() : TreePaint.muted(), wanted
							)
						)
					);
					break;
				case COVERED:
					out.add(line(colored("cfhgui_crafting.tree.tip.covered", TreePaint.muted(), amount(shown, f.leftOver(), f.chance))));
					if (f.have() > 0L) {
						out.add(line(colored("cfhgui_crafting.tree.tip.have", TreePaint.muted(), have, wanted)));
					}
					break;
				case READY:
					out.add(
						line(
							colored(
								!f.readyForAll && f.crafts > 1L ? "cfhgui_crafting.tree.tip.ready_some" : "cfhgui_crafting.tree.tip.ready",
								TreePaint.vanilla() ? -171 : TreePaint.accent(),
								f.crafts
							)
						)
					);
					out.add(line(colored("cfhgui_crafting.tree.tip.have", TreePaint.muted(), have, wanted)));
					break;
				case WAITING:
					out.add(line(colored("cfhgui_crafting.tree.tip.waiting", TreePaint.muted())));
					out.add(line(colored("cfhgui_crafting.tree.tip.have", TreePaint.muted(), have, wanted)));
					break;
				case MISSING:
					out.add(line(colored("cfhgui_crafting.tree.tip.missing", TreePaint.warning(), missing, have, wanted)));
					break;
				case GIVEN:
					out.add(line(colored("cfhgui_crafting.tree.tip.given", TreePaint.muted())));
					out.add(line(colored("cfhgui_crafting.tree.tip.missing", TreePaint.warning(), missing, have, wanted)));
			}

			if (f.state != TreeFigures.State.COVERED && f.state != TreeFigures.State.DONE && f.leftOver() > 0L) {
				out.add(line(colored("cfhgui_crafting.tree.tip.left_over", TreePaint.dim(), amount(shown, f.leftOver(), f.chance))));
			}

			EmiRecipe recipe = node.recipe();
			if (recipe != null && node.step()) {
				out.add(line(colored("cfhgui_crafting.tree.tip.made_by", TreePaint.dim(), recipe.getCategory().getName())));
				out.add(new RecipeTooltipComponent(recipe, f.state != TreeFigures.State.DONE));
			}

			if (EmiConfig.helpLevel.has(HelpLevel.NORMAL)) {
				if (session.foldMode) {
					out.add(line(colored(session.firstPoint == null ? "cfhgui_crafting.tree.tip.fold_first" : "cfhgui_crafting.tree.tip.fold_second", TreePaint.dim())));
				} else {
					if (f.state == TreeFigures.State.READY && f.here) {
						out.add(line(colored(f.crafts > 1L ? "cfhgui_crafting.tree.tip.keys.craft_all" : "cfhgui_crafting.tree.tip.keys.craft", TreePaint.dim(), f.crafts)));
					}

					out.add(line(colored("cfhgui_crafting.tree.tip.keys", TreePaint.dim())));
				}
			}

			return out;
		}
	}

	static List<ClientTooltipComponent> island(TreeSession session, TreeRow row) {
		List<ClientTooltipComponent> out = new ArrayList<>();
		TreeFigures.Fold fold = session.fold(row);
		out.add(line(counted("cfhgui_crafting.tree.island.title", (long)fold.steps, fold.steps).withStyle(s -> s.withColor(TreePaint.text() & 16777215))));
		out.add(line(colored("cfhgui_crafting.tree.island.done", fold.complete() ? TreePaint.success() : TreePaint.muted(), fold.done, fold.steps)));
		if (fold.ready) {
			out.add(line(colored("cfhgui_crafting.tree.island.ready", TreePaint.vanilla() ? -171 : TreePaint.accent())));
		}

		if (!fold.raws.isEmpty()) {
			out.add(line(colored("cfhgui_crafting.tree.island.raws", TreePaint.dim())));
			int listed = 0;

			for (TreeFigures.Raw raw : fold.raws) {
				if (listed++ == 8) {
					out.add(line(colored("cfhgui_crafting.tree.island.more", TreePaint.dim(), fold.raws.size() - 8)));
					break;
				}

				String count = amount(raw.ingredient, raw.have(), 1.0F) + " / " + (raw.estimated ? "~" : "") + amount(raw.ingredient, raw.wanted, 1.0F);
				out.add(
					line(
						Component.literal("  " + count + "  ")
							.withStyle(s -> s.withColor((raw.missing > 0L ? TreePaint.warning() : TreePaint.success()) & 16777215))
							.append(TreeActions.name(raw.ingredient).copy().withStyle(s -> s.withColor(TreePaint.muted() & 16777215)))
					)
				);
			}
		}

		if (EmiConfig.helpLevel.has(HelpLevel.NORMAL)) {
			out.add(line(colored("cfhgui_crafting.tree.island.keys", TreePaint.dim())));
		}

		return out;
	}

	private TreeTips() {
	}
}
