package com.onehumanawa.cfhcore.content.gui.emi.bom;

import com.google.common.collect.Maps;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiPlayerInventory;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import java.util.List;
import java.util.Map;

public class TreeCost {
	public Map<EmiIngredient, FlatMaterialCost> costs = Maps.newHashMap();
	public Map<EmiIngredient, ChanceMaterialCost> chanceCosts = Maps.newHashMap();
	public Map<EmiStack, FlatMaterialCost> remainders = Maps.newHashMap();
	public Map<EmiStack, ChanceMaterialCost> chanceRemainders = Maps.newHashMap();
	private Map<EmiStack, Long> stock = null;

	public void calculate(MaterialNode node, long batches) {
		this.costs.clear();
		this.chanceCosts.clear();
		this.remainders.clear();
		this.chanceRemainders.clear();
		this.calculateCost(node, batches * node.amount, ChanceState.DEFAULT, false);
	}

	public void calculateProgress(MaterialNode node, long batches, EmiPlayerInventory inventory) {
		this.costs.clear();
		this.chanceCosts.clear();
		this.remainders.clear();
		this.chanceRemainders.clear();
		this.stock = Maps.newHashMap();

		for (EmiStack stack : inventory.inventory.values()) {
			stack = stack.copy();
			this.remainders.put(stack, new FlatMaterialCost(stack, stack.getAmount()));
			this.stock.merge(stack, stack.getAmount(), Long::sum);
		}

		this.calculateCost(node, batches * node.amount, ChanceState.DEFAULT, true);
		this.stock = null;
	}

	private long fromStock(EmiStack stack, double given, boolean catalyst) {
		if (this.stock != null && !(given <= 0.0)) {
			Long left = this.stock.get(stack);
			if (left != null && left > 0L) {
				long took = Math.min(left, (long)Math.ceil(given));
				if (!catalyst) {
					this.stock.put(stack, left - took);
				}

				return took;
			} else {
				return 0L;
			}
		} else {
			return 0L;
		}
	}

	private void addCost(EmiIngredient stack, long amount, long minBatch, ChanceState chance) {
		if (chance.chanced()) {
			if (this.chanceCosts.containsKey(stack)) {
				this.chanceCosts.get(stack).merge(amount, chance.chance());
			} else {
				this.chanceCosts.put(stack, new ChanceMaterialCost(stack, amount, chance.chance()));
			}

			this.chanceCosts.get(stack).minBatch(minBatch);
		} else if (this.costs.containsKey(stack)) {
			this.costs.get(stack).amount += amount;
		} else {
			this.costs.put(stack, new FlatMaterialCost(stack, amount));
		}
	}

	private void addRemainder(EmiStack stack, long amount, ChanceState chance) {
		if (amount > 0L) {
			stack = stack.copy().setAmount(1L);
			if (chance.chanced()) {
				if (this.chanceRemainders.containsKey(stack)) {
					this.chanceRemainders.get(stack).merge(amount, chance.chance());
				} else {
					this.chanceRemainders.put(stack, new ChanceMaterialCost(stack, amount, chance.chance()));
				}
			} else if (this.remainders.containsKey(stack)) {
				this.remainders.get(stack).amount += amount;
			} else {
				this.remainders.put(stack, new FlatMaterialCost(stack, amount));
			}
		}
	}

	private double getChancedRemainder(EmiStack stack, double desired, boolean catalyst, ChanceState chance) {
		double given = 0.0;
		ChanceMaterialCost chancedRemainder = this.chanceRemainders.get(stack);
		if (chancedRemainder != null) {
			double remainderEff = (double)((float)chancedRemainder.amount * chancedRemainder.chance);
			if (remainderEff >= desired) {
				if (!catalyst) {
					chancedRemainder.amount = 1L;
					chancedRemainder.chance = (float)(remainderEff - desired);
					if (chancedRemainder.chance == 0.0F) {
						this.chanceRemainders.remove(stack);
					}
				}

				return desired;
			}

			given = remainderEff;
			if (!catalyst) {
				double leftover = remainderEff - remainderEff * (double)chance.chance();
				if (leftover == 0.0) {
					this.chanceRemainders.remove(stack);
				} else {
					chancedRemainder.amount = 1L;
					chancedRemainder.chance = (float)leftover;
				}
			}
		} else {
			FlatMaterialCost remainder = this.remainders.get(stack);
			if (remainder != null) {
				if ((double)remainder.amount >= desired) {
					if (!catalyst) {
						remainder.amount = (long)((double)remainder.amount - desired);
						if (remainder.amount == 0L) {
							this.remainders.remove(stack);
						}
					}

					return desired;
				}

				if (!catalyst) {
					this.remainders.remove(stack);
				}

				given += (double)remainder.amount;
			}
		}

		return given;
	}

	private long getRemainder(EmiStack stack, long desired, boolean catalyst) {
		FlatMaterialCost remainder = this.remainders.get(stack);
		if (remainder != null) {
			if (remainder.amount >= desired) {
				if (!catalyst) {
					remainder.amount -= desired;
					if (remainder.amount == 0L) {
						this.remainders.remove(stack);
					}
				}

				return desired;
			} else {
				if (!catalyst) {
					this.remainders.remove(stack);
				}

				return remainder.amount;
			}
		} else {
			return 0L;
		}
	}

	private void complete(MaterialNode node) {
		node.progress = ProgressState.COMPLETED;
		node.totalNeeded = 0L;
		node.neededBatches = 0L;
		node.missing = 0L;
		if (node.children != null) {
			for (MaterialNode child : node.children) {
				child.wanted = 0L;
				child.held = 0L;
				this.complete(child);
			}
		}
	}

	private void idle(MaterialNode node) {
		if (node.children != null) {
			for (MaterialNode child : node.children) {
				child.progress = ProgressState.UNSTARTED;
				child.totalNeeded = 0L;
				child.neededBatches = 0L;
				child.wanted = 0L;
				child.missing = 0L;
				child.held = 0L;
				this.idle(child);
			}
		}
	}

	private void calculateCost(MaterialNode node, long amount, ChanceState chance, boolean trackProgress) {
		if (trackProgress) {
			node.progress = ProgressState.UNSTARTED;
			node.totalNeeded = 0L;
			node.neededBatches = 0L;
		}

		boolean catalyst = node.catalyst;
		if (catalyst) {
			amount = node.amount;
		}

		if (trackProgress) {
			node.wanted = amount;
			node.missing = amount;
			node.held = 0L;
			node.wantedChance = chance.chanced() ? chance.chance() : 1.0F;
		}

		EmiRecipe recipe = node.recipe;
		if (recipe instanceof EmiResolutionRecipe err) {
			this.calculateCost(node.children.get(0), amount, chance, trackProgress);
			if (trackProgress) {
				node.missing = node.children.get(0).missing;
				node.held = node.children.get(0).held;
				node.progress = node.children.get(0).progress;
			}

			if (catalyst) {
				this.addRemainder(err.stack, amount, chance);
			}
		} else {
			long original = amount;
			List<EmiStack> ingredientStacks = node.ingredient.getEmiStacks();

			for (int i = 0; i < ingredientStacks.size(); i++) {
				if (chance.chanced()) {
					double desired = (double)((float)amount * chance.chance());
					double given = this.getChancedRemainder(ingredientStacks.get(i), desired, catalyst, chance);
					if (trackProgress && given > 0.0) {
						node.held = node.held + (long)Math.round((float)this.fromStock(ingredientStacks.get(i), given, catalyst) / chance.chance());
					}

					if (given > 0.0) {
						double scaled = given / (double)chance.chance();
						amount -= (long)scaled;
						if (amount > 0L) {
							chance = new ChanceState((float)(((double)amount - scaled % 1.0) * (double)chance.chance() / (double)amount), true);
						}
					}
				} else {
					long before = amount;
					amount -= this.getRemainder(ingredientStacks.get(i), amount, catalyst);
					if (trackProgress) {
						node.held = node.held + this.fromStock(ingredientStacks.get(i), (double)(before - amount), catalyst);
					}
				}
			}

			if (trackProgress) {
				node.missing = amount;
				node.held = Math.min(node.held, node.wanted);
			}

			if (amount == 0L) {
				if (trackProgress) {
					this.complete(node);
				}
			} else {
				if (trackProgress && amount != original) {
					node.progress = ProgressState.PARTIAL;
				}

				boolean takenAsGiven = node.state == FoldState.COLLAPSED;
				if (recipe != null && !takenAsGiven) {
					long minBatches = (long)Math.ceil((double)amount / (double)node.divisor);
					long var21 = minBatches * node.divisor;
					if (trackProgress) {
						node.totalNeeded = amount;
						node.neededBatches = minBatches;
					}

					ChanceState produced = chance.produce(node.produceChance);

					for (MaterialNode n : node.children) {
						this.calculateCost(n, minBatches * n.amount, produced.consume(n.consumeChance), trackProgress);
					}

					EmiStack stack = node.ingredient.getEmiStacks().get(0);
					this.addRemainder(stack, var21 - amount, produced);

					for (EmiStack es : recipe.getOutputs()) {
						if (!stack.equals(es)) {
							this.addRemainder(es, minBatches * es.getAmount(), produced.consume(es.getChance()));
						}
					}

					for (MaterialNode n : node.children) {
						if (!n.remainder.isEmpty() && n.remainderAmount > 0L) {
							if (n.catalyst) {
								this.addRemainder(n.remainder, n.remainderAmount, produced.consume(n.consumeChance));
							} else {
								this.addRemainder(n.remainder, minBatches * n.remainderAmount, produced.consume(n.consumeChance));
							}
						}
					}
				} else {
					if (trackProgress && takenAsGiven) {
						this.idle(node);
					}

					this.addCost(node.ingredient, amount, node.amount, chance);
				}
			}
		}
	}

	public long getIdealBatch(MaterialNode node, long total, long amount) {
		if (node.recipe != null && node.state != FoldState.COLLAPSED) {
			if (node.divisor > 0L) {
				long mod = node.divisor / this.gcd(node.divisor, amount);
				total *= mod / this.gcd(total, mod);
			}

			if (node.children != null) {
				for (MaterialNode n : node.children) {
					total = this.getIdealBatch(n, total, amount * n.amount);
				}
			}

			return total;
		} else {
			return total;
		}
	}

	public long gcd(long a, long b) {
		return b == 0L ? a : this.gcd(b, a % b);
	}
}
