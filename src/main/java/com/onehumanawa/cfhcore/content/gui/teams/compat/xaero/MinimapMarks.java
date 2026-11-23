package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.compat.Linkage;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MapMark;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MapMarks;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MarkPainter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

final class MinimapMarks extends MinimapElementRenderer<MapMark, MinimapMarks.View> {
	private boolean broken;

	MinimapMarks() {
		super(new MinimapMarks.Reader(), new MinimapMarks.Provider(), new MinimapMarks.View());
	}

	static boolean fits() {
		return Linkage.complete(MinimapMarks.class) && Linkage.complete(MinimapMarks.Provider.class) && Linkage.complete(MinimapMarks.Reader.class);
	}

	public boolean shouldRender(MinimapElementRenderLocation location) {
		return !this.broken && !XaeroMinimapBridge.off() && location == MinimapElementRenderLocation.OVER_MINIMAP && !MapMarks.marks().isEmpty();
	}

	public int getOrder() {
		return 150;
	}

	public void preRender(MinimapElementRenderInfo info, BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers) {
		MinimapMarks.View view = (MinimapMarks.View)this.getContext();
		if (info.mapDimension != view.mapKey) {
			view.mapKey = info.mapDimension;
			view.mapDimension = info.mapDimension == null ? MapMarks.dimensionNow() : info.mapDimension.location().toString();
		}

		if (info.renderEntityDimension != view.entityKey) {
			view.entityKey = info.renderEntityDimension;
			view.entityDimension = info.renderEntityDimension == null ? MapMarks.dimensionNow() : info.renderEntityDimension.location().toString();
		}

		buffers.endBatch();
		RenderSystem.disableDepthTest();
	}

	public void postRender(MinimapElementRenderInfo info, BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers) {
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
	}

	public boolean renderElement(
		MapMark mark,
		boolean highlighted,
		boolean outOfBounds,
		double optionalDepth,
		float optionalScale,
		double partialX,
		double partialY,
		MinimapElementRenderInfo info,
		GuiGraphics g,
		BufferSource buffers
	) {
		if (this.broken) {
			return false;
		} else {
			PoseStack pose = g.pose();
			pose.pushPose();

			try {
				pose.translate(0.0, 0.0, optionalDepth);
				pose.scale(optionalScale, optionalScale, 1.0F);
				boolean named = mark.kind == MapMark.Kind.TEAMMATE && !outOfBounds;
				MarkPainter.paint(g, mark, false, named ? 0.5F : 0.0F, outOfBounds ? 0.8F : 1.0F);
			} catch (Throwable var19) {
				this.broken = true;
				CFHGuiTeams.LOGGER.warn("[CFHGui Teams] drawing on Xaero's Minimap failed, the marks are off there: {}", var19.toString());
			} finally {
				pose.popPose();
			}

			return true;
		}
	}

	private static final class Provider extends MinimapElementRenderProvider<MapMark, MinimapMarks.View> {
		private List<MapMark> marks = List.of();
		private int next;

		public void begin(MinimapElementRenderLocation location, MinimapMarks.View view) {
			this.marks = MapMarks.marks();
			this.next = 0;
		}

		public boolean hasNext(MinimapElementRenderLocation location, MinimapMarks.View view) {
			return this.next < this.marks.size();
		}

		public MapMark getNext(MinimapElementRenderLocation location, MinimapMarks.View view) {
			return this.marks.get(this.next++);
		}

		public void end(MinimapElementRenderLocation location, MinimapMarks.View view) {
			this.marks = List.of();
		}
	}

	private static final class Reader extends MinimapElementReader<MapMark, MinimapMarks.View> {
		public boolean isHidden(MapMark mark, MinimapMarks.View view) {
			return !mark.dimension().equals(view.mapDimension) && !mark.dimension().equals(view.entityDimension);
		}

		public double getRenderX(MapMark mark, MinimapMarks.View view, float partialTick) {
			return mark.drawX(partialTick);
		}

		public double getRenderY(MapMark mark, MinimapMarks.View view, float partialTick) {
			return mark.y();
		}

		public double getRenderZ(MapMark mark, MinimapMarks.View view, float partialTick) {
			return mark.drawZ(partialTick);
		}

		public double getCoordinateScale(MapMark mark, MinimapMarks.View view, MinimapElementRenderInfo info) {
			return mark.dimension().equals(view.entityDimension) ? info.renderEntityDimensionScale : info.backgroundCoordinateScale;
		}

		public int getInteractionBoxLeft(MapMark mark, MinimapMarks.View view, float partialTick) {
			return -7;
		}

		public int getInteractionBoxRight(MapMark mark, MinimapMarks.View view, float partialTick) {
			return 7;
		}

		public int getInteractionBoxTop(MapMark mark, MinimapMarks.View view, float partialTick) {
			return -7;
		}

		public int getInteractionBoxBottom(MapMark mark, MinimapMarks.View view, float partialTick) {
			return 7;
		}

		public int getRenderBoxLeft(MapMark mark, MinimapMarks.View view, float partialTick) {
			return -32;
		}

		public int getRenderBoxRight(MapMark mark, MinimapMarks.View view, float partialTick) {
			return 32;
		}

		public int getRenderBoxTop(MapMark mark, MinimapMarks.View view, float partialTick) {
			return -18;
		}

		public int getRenderBoxBottom(MapMark mark, MinimapMarks.View view, float partialTick) {
			return 18;
		}

		public int getLeftSideLength(MapMark mark, Minecraft mc) {
			return 9 + mc.font.width(mark.label());
		}

		public String getMenuName(MapMark mark) {
			return mark.label().getString();
		}

		public String getFilterName(MapMark mark) {
			return this.getMenuName(mark);
		}

		public int getMenuTextFillLeftPadding(MapMark mark) {
			return 0;
		}

		public int getRightClickTitleBackgroundColor(MapMark mark) {
			return 0xFF000000 | mark.color();
		}

		public boolean shouldScaleBoxWithOptionalScale() {
			return true;
		}
	}

	static final class View {
		String mapDimension = "";
		String entityDimension = "";
		@Nullable
		ResourceKey<Level> mapKey;
		@Nullable
		ResourceKey<Level> entityKey;
	}
}
