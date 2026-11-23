package com.onehumanawa.cfhcore.content.gui.emi;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiCraftContext;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.StandardRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Widget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.WidgetHolder;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.mixin.accessor.DrawContextAccessor;
import com.onehumanawa.cfhcore.content.gui.emi.mixin.accessor.OrderedTextTooltipComponentAccessor;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.crafting.look.Look;
import com.onehumanawa.cfhcore.content.gui.crafting.look.RecipeLook;
import java.text.DecimalFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;
import org.joml.Vector2i;

public class EmiRenderHelper {
	public static final DecimalFormat TEXT_FORMAT = new DecimalFormat("#,###.##");
	public static final Component EMPTY_TEXT = EmiPort.literal("");
	public static final Minecraft CLIENT = Minecraft.getInstance();
	public static final ResourceLocation WIDGETS = EmiPort.id("emi", "textures/gui/widgets.png");
	public static final ResourceLocation BUTTONS = EmiPort.id("emi", "textures/gui/buttons.png");
	public static final ResourceLocation BACKGROUND = EmiPort.id("emi", "textures/gui/background.png");
	public static final ResourceLocation GRID = EmiPort.id("emi", "textures/gui/grid.png");
	public static final ResourceLocation DASH = EmiPort.id("emi", "textures/gui/dash.png");
	public static final ResourceLocation CONFIG = EmiPort.id("emi", "textures/gui/config.png");
	public static final ResourceLocation PIECES = EmiPort.id("emi", "textures/gui/pieces.png");

	public static void drawNinePatch(
		EmiDrawContext context, ResourceLocation texture, int x, int y, int w, int h, int u, int v, int cornerLength, int centerLength
	) {
		int corcen = cornerLength + centerLength;
		int innerWidth = w - cornerLength * 2;
		int innerHeight = h - cornerLength * 2;
		int coriw = cornerLength + innerWidth;
		int corih = cornerLength + innerHeight;
		context.drawTexture(texture, x, y, cornerLength, cornerLength, (float)u, (float)v, cornerLength, cornerLength, 256, 256);
		context.drawTexture(texture, x + cornerLength, y, innerWidth, cornerLength, (float)(u + cornerLength), (float)v, centerLength, cornerLength, 256, 256);
		context.drawTexture(texture, x + coriw, y, cornerLength, cornerLength, (float)(u + corcen), (float)v, cornerLength, cornerLength, 256, 256);
		context.drawTexture(texture, x, y + cornerLength, cornerLength, innerHeight, (float)u, (float)(v + cornerLength), cornerLength, centerLength, 256, 256);
		context.drawTexture(
			texture,
			x + cornerLength,
			y + cornerLength,
			innerWidth,
			innerHeight,
			(float)(u + cornerLength),
			(float)(v + cornerLength),
			centerLength,
			centerLength,
			256,
			256
		);
		context.drawTexture(
			texture, x + coriw, y + cornerLength, cornerLength, innerHeight, (float)(u + corcen), (float)(v + cornerLength), cornerLength, centerLength, 256, 256
		);
		context.drawTexture(texture, x, y + corih, cornerLength, cornerLength, (float)u, (float)(v + corcen), cornerLength, cornerLength, 256, 256);
		context.drawTexture(
			texture, x + cornerLength, y + corih, innerWidth, cornerLength, (float)(u + cornerLength), (float)(v + corcen), centerLength, cornerLength, 256, 256
		);
		context.drawTexture(texture, x + coriw, y + corih, cornerLength, cornerLength, (float)(u + corcen), (float)(v + corcen), cornerLength, cornerLength, 256, 256);
	}

	public static void drawTintedSprite(PoseStack matrices, TextureAtlasSprite sprite, int color, int x, int y, int xOff, int yOff, int width, int height) {
		if (sprite != null) {
			EmiPort.setPositionColorTexShader();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShaderTexture(0, sprite.atlasLocation());
			RenderSystem.enableBlend();
			float r = (float)(color >> 16 & 0xFF) / 256.0F;
			float g = (float)(color >> 8 & 0xFF) / 256.0F;
			float b = (float)(color & 0xFF) / 256.0F;
			BufferBuilder bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			float xMin = (float)x;
			float yMin = (float)y;
			float xMax = xMin + (float)width;
			float yMax = yMin + (float)height;
			float uSpan = sprite.getU1() - sprite.getU0();
			float vSpan = sprite.getV1() - sprite.getV0();
			float uMin = sprite.getU0() + uSpan / 16.0F * (float)xOff;
			float vMin = sprite.getV0() + vSpan / 16.0F * (float)yOff;
			float uMax = sprite.getU1() - uSpan / 16.0F * (float)(16 - (width + xOff));
			float vMax = sprite.getV1() - vSpan / 16.0F * (float)(16 - (height + yOff));
			Matrix4f model = matrices.last().pose();
			bufferBuilder.addVertex(model, xMin, yMax, 1.0F).setColor(r, g, b, 1.0F).setUv(uMin, vMax);
			bufferBuilder.addVertex(model, xMax, yMax, 1.0F).setColor(r, g, b, 1.0F).setUv(uMax, vMax);
			bufferBuilder.addVertex(model, xMax, yMin, 1.0F).setColor(r, g, b, 1.0F).setUv(uMax, vMin);
			bufferBuilder.addVertex(model, xMin, yMin, 1.0F).setColor(r, g, b, 1.0F).setUv(uMin, vMin);
			EmiPort.draw(bufferBuilder);
		}
	}

	public static void drawScroll(EmiDrawContext context, int x, int y, int width, int height, int progress, int total, int color) {
		if (total > 1) {
			int start = x + width * progress / total;
			int end = start + Math.max(width / total, 1);
			if (progress == total - 1) {
				end = x + width;
				start = end - Math.max(width / total, 1);
			}

			context.fill(start, y, end - start, height, color);
		}
	}

	public static Component getEmiText() {
		return EmiPort.append(
			EmiPort.append(EmiPort.literal("E", Style.EMPTY.withColor(15432700)), EmiPort.literal("M", Style.EMPTY.withColor(8125602))),
			EmiPort.literal("I", Style.EMPTY.withColor(8121340))
		);
	}

	public static Component getPageText(int page, int total, int maxWidth) {
		Component text = EmiPort.translatable("emi.page", page, total);
		if (CLIENT.font.width(text) > maxWidth) {
			text = EmiPort.translatable("emi.page.short", page, total);
			if (CLIENT.font.width(text) > maxWidth) {
				text = EmiPort.literal(page + "");
				if (CLIENT.font.width(text) > maxWidth) {
					text = EmiPort.literal("");
				}
			}
		}

		return text;
	}

	public static void drawLeftTooltip(Screen screen, EmiDrawContext context, List<ClientTooltipComponent> components, int x, int y) {
		drawTooltip(screen, context, components, x, y, screen.width / 2 - 16, (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
			Vector2i pos = new Vector2i(mouseX, mouseY).add(12, -12);
			pos.x = Math.max(pos.x - 24 - tooltipWidth, 4);
			if (pos.y + tooltipHeight + 3 > screenHeight) {
				pos.y = screenHeight - tooltipHeight - 3;
			}

			return pos;
		});
	}

	public static void drawTooltip(Screen screen, EmiDrawContext context, List<ClientTooltipComponent> components, int x, int y) {
		drawTooltip(screen, context, components, x, y, screen.width / 2 - 16);
	}

	public static void drawTooltip(Screen screen, EmiDrawContext context, List<ClientTooltipComponent> components, int x, int y, int maxWidth) {
		drawTooltip(screen, context, components, x, y, maxWidth, DefaultTooltipPositioner.INSTANCE);
	}

	public static void drawTooltip(
		Screen screen, EmiDrawContext context, List<ClientTooltipComponent> components, int x, int y, int maxWidth, ClientTooltipPositioner positioner
	) {
		if (!components.isEmpty()) {
			y = Math.max(16, y);
			List<ClientTooltipComponent> mutable = Lists.newArrayList();
			int wrapWidth = Math.max(
				components.stream().map(c -> c instanceof ClientTextTooltip ? 0 : c.getWidth(CLIENT.font)).max(Integer::compare).orElse(0), maxWidth
			);

			for (ClientTooltipComponent comp : components) {
				if (comp instanceof ClientTextTooltip ottc && ottc.getWidth(CLIENT.font) > wrapWidth) {
					try {
						FormattedCharSequence ordered = ((OrderedTextTooltipComponentAccessor)ottc).getText();
						MutableComponent text = Component.empty();
						ordered.accept((var1x, style, codepoint) -> {
							text.append(EmiPort.literal(String.valueOf(Character.toChars(codepoint)), style));
							return true;
						});

						for (FormattedCharSequence o : CLIENT.font.split(text, wrapWidth)) {
							mutable.add(ClientTooltipComponent.create(o));
						}
					} catch (Exception var20) {
						EmiLog.error("Error converting text", var20);
						mutable.add(comp);
					}
					continue;
				}

				mutable.add(comp);
			}

			context.enableDepthTest();
			EmiPort.setPositionTexShader();
			context.resetColor();
			Look.beginTooltip();

			try {
				((DrawContextAccessor)context.raw()).invokeDrawTooltip(CLIENT.font, mutable, x, y, positioner);
			} finally {
				Look.endTooltip();
			}
		}
	}

	public static void drawSlotHightlight(EmiDrawContext context, int x, int y, int w, int h, int z) {
		context.push();
		context.matrices().translate(0.0F, 0.0F, (float)z);
		RenderSystem.colorMask(true, true, true, false);
		context.fill(x, y, w, h, -2130706433);
		RenderSystem.colorMask(true, true, true, true);
		context.pop();
	}

	public static Component getAmountText(EmiIngredient stack) {
		return getAmountText(stack, stack.getAmount());
	}

	public static Component getAmountText(EmiIngredient stack, long amount) {
		if (!stack.isEmpty() && amount != 0L) {
			return (Component)(stack.getEmiStacks().get(0).getKey() instanceof Fluid ? getFluidAmount(amount) : EmiPort.literal(TEXT_FORMAT.format(amount)));
		} else {
			return EMPTY_TEXT;
		}
	}

	public static Component getAmountText(EmiIngredient stack, double amount) {
		if (!stack.isEmpty() && amount != 0.0) {
			return (Component)(stack.getEmiStacks().get(0).getKey() instanceof Fluid
				? EmiConfig.fluidUnit.translate(amount)
				: EmiPort.literal(TEXT_FORMAT.format(amount)));
		} else {
			return EMPTY_TEXT;
		}
	}

	public static Component getFluidAmount(long amount) {
		return EmiConfig.fluidUnit.translate((double)amount);
	}

	public static int getAmountOverflow(Component amount) {
		int width = CLIENT.font.width(amount);
		return width > 14 ? width - 14 : 0;
	}

	public static void renderAmount(EmiDrawContext context, int x, int y, Component amount) {
		context.push();
		context.matrices().translate(0.0F, 0.0F, 200.0F);
		int tx = x + 17 - Math.min(14, CLIENT.font.width(amount));
		context.drawTextWithShadow(amount, tx, y + 9, -1);
		context.pop();
	}

	public static void renderIngredient(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		context.enableDepthTest();
		context.push();
		context.matrices().translate(0.0F, 0.0F, 200.0F);
		RenderSystem.setShaderTexture(0, WIDGETS);
		context.drawTexture(WIDGETS, x, y, 8, 252, 4, 4);
		context.pop();
	}

	public static void renderTag(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		if (ingredient.getEmiStacks().size() > 1) {
			context.enableDepthTest();
			context.push();
			context.matrices().translate(0.0F, 0.0F, 200.0F);
			context.drawTexture(WIDGETS, x, y + 12, 0, 252, 4, 4);
			context.pop();
		}
	}

	public static void renderRemainder(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		for (EmiStack stack : ingredient.getEmiStacks()) {
			EmiStack remainder = stack.getRemainder();
			if (!remainder.isEmpty()) {
				if (remainder.equals(ingredient)) {
					renderCatalyst(ingredient, context, x, y);
				} else {
					context.push();
					context.matrices().translate(0.0F, 0.0F, 200.0F);
					context.enableDepthTest();
					context.drawTexture(WIDGETS, x + 12, y, 4, 252, 4, 4);
					context.pop();
				}

				return;
			}
		}
	}

	public static void renderCatalyst(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		context.enableDepthTest();
		context.push();
		context.matrices().translate(0.0F, 0.0F, 200.0F);
		context.drawTexture(WIDGETS, x + 12, y, 12, 252, 4, 4);
		context.pop();
	}

	public static void renderRecipeFavorite(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		context.push();
		context.matrices().translate(0.0F, 0.0F, 200.0F);
		context.enableDepthTest();
		context.drawTexture(WIDGETS, x + 12, y, 16, 252, 4, 4);
		context.pop();
	}

	public static void renderRecipeBackground(EmiRecipe recipe, EmiDrawContext context, int x, int y) {
		context.resetColor();
		if (!RecipeLook.card(context.raw(), x, y, recipe.getDisplayWidth() + 8, recipe.getDisplayHeight() + 8)) {
			drawNinePatch(context, BACKGROUND, x, y, recipe.getDisplayWidth() + 8, recipe.getDisplayHeight() + 8, 27, 0, 4, 1);
		}
	}

	public static void renderRecipe(final EmiRecipe recipe, EmiDrawContext context, int x, int y, boolean showMissing, int overlayColor) {
		try {
			renderRecipeBackground(recipe, context, x, y);
			final List<Widget> widgets = Lists.newArrayList();
			WidgetHolder holder = new WidgetHolder() {
				@Override
				public int getWidth() {
					return recipe.getDisplayWidth();
				}

				@Override
				public int getHeight() {
					return recipe.getDisplayHeight();
				}

				@Override
				public <T extends Widget> T add(T widget) {
					widgets.add(widget);
					return widget;
				}
			};
			context.push();
			context.translate((float)(x + 4), (float)(y + 4));
			recipe.addWidgets(holder);
			float delta = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);

			for (Widget widget : widgets) {
				widget.render(context.raw(), -1000, -1000, delta);
			}

			if (overlayColor != -1) {
				context.fill(-1, -1, recipe.getDisplayWidth() + 2, recipe.getDisplayHeight() + 2, overlayColor);
			}

			if (showMissing) {
				AbstractContainerScreen hs = EmiApi.getHandledScreen();
				EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, hs);
				if (handler != null) {
					handler.render(recipe, new EmiCraftContext(hs, handler.getInventory(hs), EmiCraftContext.Type.FILL_BUTTON), widgets, context.raw());
				} else if (EmiScreenManager.lastPlayerInventory != null) {
					StandardRecipeHandler.renderMissing(recipe, EmiScreenManager.lastPlayerInventory, widgets, context.raw());
				}
			}

			context.pop();
			context.disableBlend();
			RenderSystem.colorMask(false, false, false, true);
			context.disableDepthTest();
			renderRecipeBackground(recipe, context, x, y);
			context.enableDepthTest();
			RenderSystem.colorMask(true, true, true, true);
		} catch (Throwable var11) {
			EmiLog.error("Error rendering recipe", var11);
		}
	}
}
