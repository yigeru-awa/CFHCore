package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.map.ClaimMap;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class LandTableView extends StageWidget {
	private static final int NOTE_MS = 4200;
	@Nullable
	private static final int[] DEMO_SELECT = readDemoSelect();
	private static ClaimMap.Tool tool = ClaimMap.Tool.CLAIM;
	private final LandTable table;
	private final List<AbstractWidget> over = new ArrayList<>();
	private LandTableView.Drag drag = LandTableView.Drag.NONE;
	private int dragButton;
	private int fromX;
	private int fromZ;
	private int toX;
	private int toZ;
	private int lastX;
	private int lastY;
	private boolean hasCursor;
	private int cursorX;
	private int cursorZ;
	@Nullable
	private Component note;
	private boolean noteBad;
	private long noteAtMs;
	private int count;
	private int countedX = Integer.MIN_VALUE;
	private int countedZ;
	private final int[] spot = new int[2];
	private boolean pretending;
	@Nullable
	private static int[] pretend;

	@Nullable
	private static int[] readDemoSelect() {
		String v = System.getProperty("cfh.teams.demoSelect");
		if (v == null) {
			return null;
		} else {
			try {
				String[] s = v.split("[,;:]");
				int[] out = new int[s.length];

				for (int i = 0; i < s.length; i++) {
					out[i] = Integer.parseInt(s[i].trim());
				}

				return out;
			} catch (NumberFormatException var4) {
				return null;
			}
		}
	}

	LandTableView(Rect frame, LandTable table, Component label) {
		super(frame.x(), frame.y(), frame.w(), frame.h(), table.stage, label);
		this.table = table;
		int[] asked = pretend;
		if (asked != null) {
			pretend = null;
			this.pretending = true;
			this.drag = LandTableView.Drag.CLAIM;
			this.fromX = asked[0];
			this.fromZ = asked[1];
			this.toX = asked[2];
			this.toZ = asked[3];
		}
	}

	static void pretendDrag(int ax, int az, int bx, int bz) {
		pretend = new int[]{ax, az, bx, bz};
	}

	void cover(AbstractWidget widget) {
		this.over.add(widget);
	}

	static ClaimMap.Tool tool() {
		return tool;
	}

	static void toggleTool() {
		tool = tool == ClaimMap.Tool.CLAIM ? ClaimMap.Tool.KEEP_LOADED : ClaimMap.Tool.CLAIM;
	}

	private boolean covered(double mouseX, double mouseY) {
		for (AbstractWidget w : this.over) {
			if (w.visible
				&& mouseX >= (double)w.getX()
				&& mouseX < (double)(w.getX() + w.getWidth())
				&& mouseY >= (double)w.getY()
				&& mouseY < (double)(w.getY() + w.getHeight())) {
				return true;
			}
		}

		return false;
	}

	private void say(Component text, boolean bad) {
		this.note = text;
		this.noteBad = bad;
		this.noteAtMs = Clock.nowMs();
	}

	@Nullable
	private static ClaimMap.Gesture gesture(LandTableView.Drag kind) {
		return switch (kind) {
			case CLAIM -> ClaimMap.Gesture.CLAIM;
			case UNCLAIM -> ClaimMap.Gesture.UNCLAIM;
			case LOAD -> ClaimMap.Gesture.LOAD;
			case UNLOAD -> ClaimMap.Gesture.UNLOAD;
			default -> null;
		};
	}

	private void commit(LandTableView.Drag kind) {
		ClaimMap.Gesture asking = gesture(kind);
		if (asking != null) {
			ClaimMap.send(asking, ClaimMap.chunks(asking, this.fromX, this.fromZ, this.toX, this.toZ), this::say);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.active && this.visible && this.isMouseOver(mouseX, mouseY) && button <= 2 && !this.covered(mouseX, mouseY)) {
			this.table.touch();
			this.hasCursor = false;
			this.pretending = false;
			this.dragButton = button;
			this.lastX = (int)mouseX;
			this.lastY = (int)mouseY;
			if (button != 2 && this.table.pointing() && ClaimMap.whyNot() == null) {
				boolean load = tool == ClaimMap.Tool.KEEP_LOADED || Screen.hasShiftDown();
				this.drag = button == 0 ? (load ? LandTableView.Drag.LOAD : LandTableView.Drag.CLAIM) : (load ? LandTableView.Drag.UNLOAD : LandTableView.Drag.UNCLAIM);
				this.fromX = this.toX = this.table.pointedChunkX();
				this.fromZ = this.toZ = this.table.pointedChunkZ();
				this.countedX = Integer.MIN_VALUE;
				return true;
			} else {
				this.drag = LandTableView.Drag.TURN;
				return true;
			}
		} else {
			return false;
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.drag != LandTableView.Drag.NONE;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return this.drag != LandTableView.Drag.NONE;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.visible && scrollY != 0.0 && this.isMouseOver(mouseX, mouseY) && !this.covered(mouseX, mouseY)) {
			this.zoomBy(scrollY > 0.0 ? 1 : -1);
			return true;
		} else {
			return false;
		}
	}

	void zoomBy(int steps) {
		if (this.table.canZoom(steps)) {
			this.table.zoom(steps);
			CFHGuiSounds.tick();
		}
	}

	private static boolean held(int button) {
		long window = Minecraft.getInstance().getWindow().getWindow();
		return GLFW.glfwGetMouseButton(window, button == 0 ? 0 : (button == 1 ? 1 : 2)) == 1;
	}

	private void followDrag(int mouseX, int mouseY) {
		if (this.drag != LandTableView.Drag.NONE && !this.pretending) {
			boolean down = held(this.dragButton);
			this.table.touch();
			if (this.drag != LandTableView.Drag.TURN) {
				if (this.table.pointing()) {
					this.toX = this.table.pointedChunkX();
					this.toZ = this.table.pointedChunkZ();
				}

				if (!down) {
					LandTableView.Drag kind = this.drag;
					this.drag = LandTableView.Drag.NONE;
					this.commit(kind);
				}
			} else {
				if (mouseX != this.lastX || mouseY != this.lastY) {
					this.table.turn((float)(this.lastX - mouseX) * 0.45F, (float)(mouseY - this.lastY) * 0.32F);
				}

				this.lastX = mouseX;
				this.lastY = mouseY;
				if (!down) {
					this.drag = LandTableView.Drag.NONE;
				}
			}
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			LandModel model = this.table.model;
			switch (keyCode) {
				case 32:
				case 76:
				case 257:
				case 259:
				case 261:
				case 335:
					if (!this.hasCursor) {
						return false;
					} else {
						Component why = ClaimMap.whyNot();
						if (why != null) {
							this.say(why, true);
							return true;
						} else {
							TeamsState.Claim at = TeamsState.claimAt(TeamsView.dimension(), this.cursorX, this.cursorZ);
							this.fromX = this.toX = this.cursorX;
							this.fromZ = this.toZ = this.cursorZ;
							this.commit(
								keyCode == 76
									? (at != null && at.forced() ? LandTableView.Drag.UNLOAD : LandTableView.Drag.LOAD)
									: (keyCode != 261 && keyCode != 259 ? LandTableView.Drag.CLAIM : LandTableView.Drag.UNCLAIM)
							);
							return true;
						}
					}
				case 45:
				case 333:
					this.zoomBy(-1);
					return true;
				case 61:
				case 334:
					this.zoomBy(1);
					return true;
				case 67:
				case 268:
					this.table.centre();
					this.hasCursor = false;
					return true;
				case 262:
				case 263:
				case 264:
				case 265:
					if (!this.hasCursor) {
						Vec3 me = TeamsView.position();
						this.cursorX = this.table.pointing() ? this.table.pointedChunkX() : (me != null ? Mth.floor(me.x) >> 4 : model.originX() + 12);
						this.cursorZ = this.table.pointing() ? this.table.pointedChunkZ() : (me != null ? Mth.floor(me.z) >> 4 : model.originZ() + 12);
						this.hasCursor = true;
					} else {
						int[] step = this.table.step(keyCode == 262 ? 1 : (keyCode == 263 ? -1 : 0), keyCode == 265 ? 1 : (keyCode == 264 ? -1 : 0));
						this.cursorX = this.cursorX + step[0];
						this.cursorZ = this.cursorZ + step[1];
					}

					this.cursorX = Mth.clamp(this.cursorX, model.originX(), model.originX() + 25 - 1);
					this.cursorZ = Mth.clamp(this.cursorZ, model.originZ(), model.originZ() + 25 - 1);
					this.table.lookAt(this.cursorX, this.cursorZ);
					return true;
				case 266:
					this.table.turn(-30.0F, 0.0F);
					return true;
				case 267:
					this.table.turn(30.0F, 0.0F);
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.hasCursor = false;
		}
	}

	private static int dragColor(LandTableView.Drag kind, Palette p) {
		TeamData team = TeamsView.land();

		return switch (kind) {
			case CLAIM -> TeamsDraw.ink(team == null ? p.accent() & 16777215 : team.color);
			case UNCLAIM, UNLOAD -> p.danger();
			default -> p.warning();
		};
	}

	private boolean demoSelect(LandSlabs slabs, Palette p) {
		if (DEMO_SELECT != null && DEMO_SELECT.length == 4) {
			int mx = this.table.model.originX() + 12 + DEMO_SELECT[0];
			int mz = this.table.model.originZ() + 12 + DEMO_SELECT[1];
			slabs.select(ClaimMap.Gesture.CLAIM, mx, mz, mx + DEMO_SELECT[2] - 1, mz + DEMO_SELECT[3] - 1, dragColor(LandTableView.Drag.UNCLAIM, p));
			return true;
		} else {
			return false;
		}
	}

	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Rect frame = new Rect(this.getX(), this.getY(), this.getWidth(), this.getHeight());
		Rect inner = new Rect(frame.x() + 1, frame.y() + 1, frame.w() - 2, frame.h() - 2);
		SceneFrame.plate(g, frame);
		this.table.shape(inner);
		this.stage().focusVisible(false);
		if (gesture(this.drag) == null) {
			this.demoSelect(this.table.slabs(), p);
		}

		boolean mouse = this.isHovered() && g.containsPointInScissor(mouseX, mouseY) && !this.covered((double)mouseX, (double)mouseY);
		this.stage().render(g, inner.x(), inner.y(), inner.w(), inner.h(), mouse ? (double)mouseX : -1.0E9, mouse ? (double)mouseY : -1.0E9, partialTick);
		this.followDrag(mouseX, mouseY);
		boolean onLand = mouse && this.table.pointing();
		int cx = this.table.pointedChunkX();
		int cz = this.table.pointedChunkZ();
		ClaimMap.Gesture asking = gesture(this.drag);
		LandSlabs slabs = this.table.slabs();
		slabs.hover(onLand && this.drag == LandTableView.Drag.NONE, cx, cz);
		if (asking != null || !this.demoSelect(slabs, p)) {
			slabs.select(asking, this.fromX, this.fromZ, this.toX, this.toZ, asking == null ? 0 : dragColor(this.drag, p));
		}

		slabs.cursor(this.hasCursor && this.isFocused(), this.cursorX, this.cursorZ);
		g.enableScissor(inner.x(), inner.y(), inner.right(), inner.bottom());
		this.drawNorth(g, inner, van);
		if (asking != null) {
			this.drawSelection(g, inner, asking, mouseX, mouseY, p);
		}

		int[] counters = ClaimMap.countersBox(inner.x(), inner.y(), inner.w(), inner.h());
		ClaimMap.drawCounters(g, counters, inner.x(), inner.y(), inner.w(), inner.h(), van, 1.0F);
		Vec3 me = TeamsView.position();
		LandModel model = this.table.model;
		drawPlace(
			g,
			inner,
			onLand ? cx : (this.hasCursor ? this.cursorX : (me != null ? Mth.floor(me.x) >> 4 : model.originX() + 12)),
			onLand ? cz : (this.hasCursor ? this.cursorZ : (me != null ? Mth.floor(me.z) >> 4 : model.originZ() + 12)),
			van
		);
		this.drawNote(g, inner, van);
		g.disableScissor();
		CFHGuiDraw.outline(g, frame.x(), frame.y(), frame.w(), frame.h(), van ? -16777216 : p.borderStrong(), van ? 0 : t.radius());
		if (this.isFocused() && CFHGuiDraw.focusShown()) {
			CFHGuiDraw.outline(g, frame.x() - 1, frame.y() - 1, frame.w() + 2, frame.h() + 2, van ? -1 : p.accent(), van ? 0 : t.radius());
		}

		if (mouse && this.drag == LandTableView.Drag.NONE) {
			boolean overCounters = counters != null
				&& mouseX >= counters[0]
				&& mouseX < counters[0] + counters[2]
				&& mouseY >= counters[1]
				&& mouseY < counters[1] + counters[3];
			if (overCounters) {
				CFHGuiTooltips.request(ClaimMap.counterTip(), this);
			} else if (onLand) {
				List<Component> lines = ClaimMap.claimLines(model.dimension(), cx, cz, this.pointed(inner, mouseX, mouseY));
				if (!lines.isEmpty()) {
					CFHGuiTooltips.request(lines, this);
				}
			}
		}
	}

	@Nullable
	private Component pointed(Rect inner, int mouseX, int mouseY) {
		Component name = null;
		int nearest = 65;

		for (LandTable.Mark mark : this.table.marks()) {
			for (int part = 0; part <= 2; part++) {
				if (this.table.screen(mark.x(), Mth.lerp((float)part / 2.0F, mark.foot(), mark.top()), mark.z(), inner, this.spot)) {
					int dx = this.spot[0] - mouseX;
					int dy = this.spot[1] - mouseY;
					int far = dx * dx + dy * dy;
					if (far < nearest) {
						nearest = far;
						name = mark.name();
					}
				}
			}
		}

		return name;
	}

	private void drawNorth(GuiGraphics g, Rect inner, boolean van) {
		float rim = (float)this.table.model.top(50, 0) + 1.5F;
		if (this.table.risen() && this.table.screen(0.0F, rim, -52.5F, inner, this.spot)) {
			Component n = TeamsText.t("map.north");
			int w = CFHGuiDraw.width(n) + 6;
			int x = this.spot[0] - w / 2;
			int y = this.spot[1] - 6;
			if (x >= inner.x() + 2 && y >= inner.y() + 2 && x + w <= inner.right() - 2 && y + 12 <= inner.bottom() - 2) {
				SceneFrame.label(g, x, y, w, 12, 1.0F);
				g.drawString(CFHGuiDraw.font(), n, x + 3, y + 2, van ? -1 : Theme.current().palette().textMuted(), false);
			}
		}
	}

	private void drawSelection(GuiGraphics g, Rect inner, ClaimMap.Gesture asking, int mouseX, int mouseY, Palette p) {
		if (this.countedX != this.toX || this.countedZ != this.toZ) {
			this.countedX = this.toX;
			this.countedZ = this.toZ;
			this.count = ClaimMap.chunks(asking, this.fromX, this.fromZ, this.toX, this.toZ).size();
		}

		Component over = ClaimMap.overLimit(asking, this.count);
		boolean adds = asking == ClaimMap.Gesture.CLAIM || asking == ClaimMap.Gesture.LOAD;
		Component label = (Component)(over != null ? over : Component.literal((adds ? "+" : "-") + this.count));
		int w = CFHGuiDraw.width(label) + 8;
		int ax = mouseX;
		int ay = mouseY;
		if (this.pretending
			&& this.table
				.screen(
					this.table.model.localX((double)(this.toX * 16 + 16)),
					(float)this.table.model.chunkTop(this.toX - this.table.model.originX(), this.toZ - this.table.model.originZ()) + 1.0F,
					this.table.model.localZ((double)(this.toZ * 16 + 16)),
					inner,
					this.spot
				)) {
			ax = this.spot[0];
			ay = this.spot[1];
		}

		int x = Mth.clamp(ax + 10, inner.x() + 2, Math.max(inner.x() + 2, inner.right() - 2 - w));
		int y = Mth.clamp(ay - 16, inner.y() + 2, Math.max(inner.y() + 2, inner.bottom() - 14));
		CFHGuiDraw.rect(g, x, y, w, 12, -804253680);
		CFHGuiDraw.chip(g, label, x, y, over != null ? p.warning() : dragColor(this.drag, p), 1.0F);
	}

	private static void drawPlace(GuiGraphics g, Rect inner, int chunkX, int chunkZ, boolean van) {
		Component text = TeamsText.t("map.chunk", chunkX, chunkZ);
		int w = CFHGuiDraw.width(text) + 8;
		if (w <= inner.w() / 2) {
			int x = inner.right() - w - 3;
			int y = inner.bottom() - 15;
			SceneFrame.label(g, x, y, w, 12, 1.0F);
			g.drawString(CFHGuiDraw.font(), text, x + 4, y + 2, van ? -2039584 : Theme.current().palette().textMuted(), van);
		}
	}

	private void drawNote(GuiGraphics g, Rect inner, boolean van) {
		if (this.note != null) {
			long age = Clock.nowMs() - this.noteAtMs;
			if (age > 4200L) {
				this.note = null;
			} else {
				Palette p = Theme.current().palette();
				boolean still = Theme.current().motion() <= 0.0F;
				float in = still ? 1.0F : Ease.OUT_CUBIC.apply(Math.min(1.0F, (float)age / 180.0F));
				float out = still ? 1.0F : Math.min(1.0F, (float)(4200L - age) / 300.0F);
				float a = in * out;
				int color = this.noteBad ? p.warning() : p.success();
				int w = Math.min(inner.w() - 30, CFHGuiDraw.width(this.note) + 20);
				int x = inner.x() + 4;
				int y = inner.y() + 4 - Math.round((1.0F - in) * 6.0F);
				SceneFrame.label(g, x, y, w, 14, a);
				if (!van) {
					CFHGuiDraw.rect(g, x, y + 2, 2, 10, Colors.scaleAlpha(color, a));
				}

				Icons.draw(g, this.noteBad ? Icon.WARNING : Icon.CHECK, x + 4, y + 3, 8, Colors.scaleAlpha(color, a));
				g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.note, w - 18), x + 15, y + 3, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
		out.add(NarratedElementType.USAGE, OverhaulTeams.t("table.help.keys"));
	}

	private static enum Drag {
		NONE,
		CLAIM,
		UNCLAIM,
		LOAD,
		UNLOAD,
		TURN;
	}
}
