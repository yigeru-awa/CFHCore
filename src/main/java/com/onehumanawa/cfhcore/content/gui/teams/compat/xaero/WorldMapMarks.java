package com.onehumanawa.cfhcore.content.gui.teams.compat.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.compat.Linkage;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MapMark;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MapMarks;
import com.onehumanawa.cfhcore.content.gui.teams.compat.MarkPainter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

final class WorldMapMarks extends ElementRenderer<MapMark, WorldMapMarks.View, WorldMapMarks> {
	private boolean broken;

	WorldMapMarks() {
		super(new WorldMapMarks.View(), new WorldMapMarks.Provider(), new WorldMapMarks.Reader());
	}

	static boolean fits() {
		return Linkage.complete(WorldMapMarks.class) && Linkage.complete(WorldMapMarks.Provider.class) && Linkage.complete(WorldMapMarks.Reader.class);
	}

	static float unit() {
		return (float)Mth.clamp(Minecraft.getInstance().getWindow().getGuiScale(), 2.0, 3.0);
	}

	public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
		return !shadow && !this.broken && !XaeroWorldMapBridge.off() && location == ElementRenderLocation.WORLD_MAP && !MapMarks.marks().isEmpty();
	}

	public int getOrder() {
		return 250;
	}

	public boolean shouldBeDimScaled() {
		return false;
	}

	public void preRender(ElementRenderInfo info, BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers, boolean shadow) {
		WorldMapMarks.View view = (WorldMapMarks.View)this.getContext();
		ResourceKey<Level> key = info == null ? null : info.mapDimension;
		if (key == null) {
			view.key = null;
			view.dimension = MapMarks.dimensionNow();
		} else if (key != view.key) {
			view.key = key;
			view.dimension = key.location().toString();
		}
	}

	public void postRender(ElementRenderInfo info, BufferSource buffers, MultiTextureRenderTypeRendererProvider renderers, boolean shadow) {
	}

	public void renderElementShadow(
		MapMark mark,
		boolean hovered,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo info,
		GuiGraphics g,
		BufferSource buffers,
		MultiTextureRenderTypeRendererProvider renderers
	) {
	}

	public boolean renderElement(
		MapMark mark,
		boolean hovered,
		double optionalDepth,
		float optionalScale,
		double partialX,
		double partialY,
		ElementRenderInfo info,
		GuiGraphics g,
		BufferSource buffers,
		MultiTextureRenderTypeRendererProvider renderers
	) {
		if (this.broken) {
			return false;
		} else {
			PoseStack pose = g.pose();
			pose.pushPose();

			try {
				float s = unit() * optionalScale;
				pose.translate(0.0, 0.0, optionalDepth);
				pose.scale(s, s, 1.0F);
				boolean named = hovered || mark.kind != MapMark.Kind.WAYPOINT;
				MarkPainter.paint(g, mark, hovered, named ? 1.0F : 0.0F, 1.0F);
			} catch (Throwable var20) {
				this.broken = true;
				CFHGuiTeams.LOGGER.warn("[CFHGui Teams] drawing on Xaero's World Map failed, the marks are off there: {}", var20.toString());
			} finally {
				pose.popPose();
			}

			return true;
		}
	}

	private static final class Provider extends ElementRenderProvider<MapMark, WorldMapMarks.View> {
		private List<MapMark> marks = List.of();
		private int next;

		public void begin(ElementRenderLocation location, WorldMapMarks.View view) {
			this.marks = MapMarks.marks();
			this.next = 0;
		}

		public boolean hasNext(ElementRenderLocation location, WorldMapMarks.View view) {
			return this.next < this.marks.size();
		}

		public MapMark getNext(ElementRenderLocation location, WorldMapMarks.View view) {
			return this.marks.get(this.next++);
		}

		public void end(ElementRenderLocation location, WorldMapMarks.View view) {
			this.marks = List.of();
		}
	}

	private static final class Reader extends ElementReader<MapMark, WorldMapMarks.View, WorldMapMarks> {
		public boolean isHidden(MapMark mark, WorldMapMarks.View view) {
			return !mark.dimension().equals(view.dimension);
		}

		public double getRenderX(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return mark.drawX(partialTick);
		}

		public double getRenderZ(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return mark.drawZ(partialTick);
		}

		public int getInteractionBoxLeft(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return -7;
		}

		public int getInteractionBoxRight(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return 7;
		}

		public int getInteractionBoxTop(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return -7;
		}

		public int getInteractionBoxBottom(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return 7;
		}

		public int getRenderBoxLeft(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return -64;
		}

		public int getRenderBoxRight(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return 64;
		}

		public int getRenderBoxTop(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return -18;
		}

		public int getRenderBoxBottom(MapMark mark, WorldMapMarks.View view, float partialTick) {
			return 24;
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

		public float getBoxScale(ElementRenderLocation location, MapMark mark, WorldMapMarks.View view) {
			return WorldMapMarks.unit();
		}

		public boolean isInteractable(ElementRenderLocation location, MapMark mark) {
			return true;
		}

		public boolean isRightClickValid(MapMark mark) {
			return MapMarks.marks().contains(mark);
		}

		public ArrayList<RightClickOption> getRightClickOptions(MapMark mark, IRightClickableElement target) {
			try {
				ArrayList<RightClickOption> options = new ArrayList<>();
				options.add(new RightClickOption(mark.label().getString(), options.size(), target) {
					public void onAction(Screen screen) {
					}
				});
				return options;
			} catch (Throwable var4) {
				return null;
			}
		}
	}

	static final class View {
		String dimension = "";
		@Nullable
		ResourceKey<Level> key;
	}
}
