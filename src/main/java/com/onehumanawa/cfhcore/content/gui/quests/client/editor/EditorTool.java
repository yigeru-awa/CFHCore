package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasTool;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.GraphModel;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.LinkView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.NodeView;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Ids;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class EditorTool implements CanvasTool {
	private static final float RIM_OUT = 7.0F;
	private static final float RIM_IN = 2.0F;
	private final EditSession s;
	private EditorTool.Gesture gesture = EditorTool.Gesture.NONE;
	private int button;
	private boolean simulated;
	private boolean sticky;
	private double pressX;
	private double pressY;
	private float pressTx;
	private float pressTy;
	private EditorTool.Hit pressHit = EditorTool.Hit.NOTHING;
	private boolean pressWasChosen;
	private boolean moved;
	private final Map<String, float[]> questOrigins = new LinkedHashMap<>();
	private final Map<Integer, float[]> decorationOrigins = new LinkedHashMap<>();
	private float heldX;
	private float heldY;
	private float dragDx;
	private float dragDy;
	private float guideX = Float.NaN;
	private float guideX0;
	private float guideX1;
	private float guideY = Float.NaN;
	private float guideY0;
	private float guideY1;
	private boolean bandAdds;
	private final Set<String> bandQuests = new LinkedHashSet<>();
	private final Set<Integer> bandDecorations = new LinkedHashSet<>();
	private double bandX;
	private double bandY;
	@Nullable
	private String linkFrom;
	@Nullable
	private String linkOnto;
	private EditorOps.Link linkCan = EditorOps.Link.OK;
	private float linkX;
	private float linkY;
	private EditorTool.Hit hover = EditorTool.Hit.NOTHING;
	private int hoverAtX = Integer.MIN_VALUE;
	private int hoverAtY = Integer.MIN_VALUE;
	private long hoverSinceMs;
	@Nullable
	private List<Component> tag;
	private final List<Integer> blank = new ArrayList<>();
	@Nullable
	private GraphModel blankOf;
	private final List<NodeView> chosenNodes = new ArrayList<>();
	@Nullable
	private GraphModel chosenOf;
	private int chosenAt = -1;
	private final Anim ring = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	@Nullable
	private String ringOf;
	private final Anim shown = new Anim(0.0F, 260, Ease.OUT_CUBIC);
	private long lastClickMs;
	private long chipSinceMs;
	@Nullable
	private String lastClickQuest;

	EditorTool(EditSession session) {
		this.s = session;
	}

	static double mouseX() {
		Minecraft mc = Minecraft.getInstance();
		return mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / Math.max(1.0, (double)mc.getWindow().getScreenWidth());
	}

	static double mouseY() {
		Minecraft mc = Minecraft.getInstance();
		return mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / Math.max(1.0, (double)mc.getWindow().getScreenHeight());
	}

	float[] pointer() {
		QuestCanvas canvas = this.s.canvas();
		if (canvas == null) {
			return new float[]{0.0F, 0.0F};
		} else {
			double mx = mouseX();
			double my = mouseY();
			Rect free = canvas.free();
			return free.contains(mx, my) ? new float[]{canvas.tx(mx), canvas.ty(my)} : new float[]{canvas.tx((double)free.centerX()), canvas.ty((double)free.centerY())};
		}
	}

	float[] place(float x, float y) {
		return EditSession.snapping ? new float[]{EditorOps.snap(x), EditorOps.snap(y)} : new float[]{EditorOps.tidy(x), EditorOps.tidy(y)};
	}

	private String key(String questId) {
		return this.s.key(questId);
	}

	@Nullable
	private NodeView node(QuestCanvas canvas, @Nullable String questId) {
		return questId == null ? null : canvas.graph().shown(this.key(questId));
	}

	EditorTool.Hit hit(QuestCanvas canvas, double mx, double my) {
		Chapter work = this.s.work();
		if (work == null) {
			return EditorTool.Hit.NOTHING;
		} else {
			GraphModel graph = canvas.graph();
			NodeView on = canvas.nodeAt(mx, my);
			if (on != null && work.quest(on.quest.id) != null) {
				float r = canvas.radiusOf(on);
				double d = Math.hypot(mx - (double)canvas.sx(on.x), my - (double)canvas.sy(on.y));
				return EditorTool.Hit.quest(on.quest.id, d >= (double)(r - Math.min(2.0F, r * 0.25F)));
			} else if (!canvas.contains(mx, my)) {
				return EditorTool.Hit.NOTHING;
			} else {
				for (int i = graph.nodes.size() - 1; i >= 0; i--) {
					NodeView n = graph.nodes.get(i);
					float reach = canvas.radiusOf(n) + 7.0F;
					double dx = mx - (double)canvas.sx(n.x);
					double dy = my - (double)canvas.sy(n.y);
					if (!(Math.abs(dx) > (double)reach) && !(Math.abs(dy) > (double)reach) && dx * dx + dy * dy <= (double)(reach * reach) && work.quest(n.quest.id) != null) {
						return EditorTool.Hit.quest(n.quest.id, true);
					}
				}

				List<Chapter.Decoration> decorations = graph.chapter.decorations;

				for (int ix = Math.min(decorations.size(), work.decorations.size()) - 1; ix >= 0; ix--) {
					float[] box = this.box(canvas, decorations.get(ix));
					if (mx >= (double)(box[0] - 2.0F) && mx < (double)(box[2] + 2.0F) && my >= (double)(box[1] - 2.0F) && my < (double)(box[3] + 2.0F)) {
						return EditorTool.Hit.decoration(ix);
					}
				}

				float r0 = canvas.painter().radius() * canvas.zoom();

				for (LinkView l : graph.links) {
					float x1 = canvas.sx(l.from().x);
					float y1 = canvas.sy(l.from().y);
					float x2 = canvas.sx(l.to().x);
					float y2 = canvas.sy(l.to().y);
					float len = Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
					if (!(len < 2.0F)) {
						double t = ((mx - (double)x1) * (double)(x2 - x1) + (my - (double)y1) * (double)(y2 - y1)) / (double)(len * len);
						if (!(t * (double)len < (double)(r0 * l.from().size)) && !((1.0 - t) * (double)len < (double)(r0 * l.to().size))) {
							double px = (double)x1 + t * (double)(x2 - x1);
							double py = (double)y1 + t * (double)(y2 - y1);
							if (Math.hypot(mx - px, my - py) <= 3.5) {
								return EditorTool.Hit.link(l.from().quest.id, l.to().quest.id);
							}
						}
					}
				}

				return EditorTool.Hit.NOTHING;
			}
		}
	}

	float[] box(QuestCanvas canvas, Chapter.Decoration d) {
		float cx = canvas.sx(d.x);
		float cy = canvas.sy(d.y);
		if ("image".equals(d.type)) {
			float w = Math.max(0.2F, d.w) * canvas.unit();
			float h = Math.max(0.2F, d.h) * canvas.unit();
			return new float[]{cx - w / 2.0F, cy - h / 2.0F, cx + w / 2.0F, cy + h / 2.0F};
		} else {
			float scale = Math.max(d.heading ? 0.75F : 0.6F, Math.min(1.5F, canvas.zoom())) * Math.max(0.1F, d.scale);
			Font font = CFHGuiDraw.font();
			float w = Math.max(12.0F, (float)(d.text.isBlank() ? 24 : font.width(QuestText.text(d.text))) * scale);
			float h = 9.0F * scale;
			return new float[]{cx - w / 2.0F, cy - 4.0F * scale, cx + w / 2.0F, cy - 4.0F * scale + h};
		}
	}

	@Override
	public boolean mouseClicked(QuestCanvas canvas, @Nullable NodeView node, double mouseX, double mouseY, int btn) {
		if (!this.s.active()) {
			return false;
		} else if (this.sticky) {
			if (btn == 0) {
				this.linkTo(canvas, mouseX, mouseY);
				this.finishLink(canvas, mouseX, mouseY);
			}

			this.reset();
			return true;
		} else if (this.gesture != EditorTool.Gesture.NONE) {
			return true;
		} else if (btn == 2) {
			return false;
		} else if (btn != 0 && btn != 1) {
			return true;
		} else {
			this.press(canvas, mouseX, mouseY, btn, Screen.hasShiftDown(), Screen.hasControlDown());
			return true;
		}
	}

	@Override
	public boolean mouseDragged(QuestCanvas canvas, double mouseX, double mouseY, int btn, double dragX, double dragY) {
		if (this.gesture == EditorTool.Gesture.NONE || this.sticky) {
			return false;
		} else if (btn != this.button) {
			return true;
		} else {
			this.drag(canvas, mouseX, mouseY, Screen.hasAltDown());
			return true;
		}
	}

	@Override
	public boolean mouseReleased(QuestCanvas canvas, double mouseX, double mouseY, int btn) {
		if (this.gesture == EditorTool.Gesture.NONE || this.sticky) {
			return false;
		} else if (btn != this.button) {
			return true;
		} else {
			this.release(canvas, mouseX, mouseY, Screen.hasShiftDown());
			return true;
		}
	}

	@Override
	public boolean quiet() {
		return this.s.active();
	}

	boolean busy() {
		return this.gesture != EditorTool.Gesture.NONE;
	}

	void press(QuestCanvas canvas, double mx, double my, int btn, boolean shift, boolean ctrl) {
		Selection sel = this.s.selection;
		EditorTool.Hit hit = this.hit(canvas, mx, my);
		this.button = btn;
		this.pressX = mx;
		this.pressY = my;
		this.pressTx = canvas.tx(mx);
		this.pressTy = canvas.ty(my);
		this.pressHit = hit;
		this.moved = false;
		this.simulated = false;
		this.clearGuides();
		if (btn != 1) {
			switch (hit.kind()) {
				case 1:
					if (ctrl) {
						this.startLink(hit.quest(), false);
						return;
					}

					this.pressWasChosen = sel.has(hit.quest());
					if (shift) {
						sel.toggle(hit.quest());
					} else if (!this.pressWasChosen) {
						sel.only(hit.quest());
					}

					this.gesture = EditorTool.Gesture.PRESS;
					break;
				case 2:
					this.startLink(hit.quest(), false);
					break;
				case 3:
					this.pressWasChosen = sel.hasDecoration(hit.decoration());
					if (shift) {
						sel.toggleDecoration(hit.decoration());
					} else if (!this.pressWasChosen) {
						sel.onlyDecoration(hit.decoration());
					}

					this.gesture = EditorTool.Gesture.PRESS;
					break;
				case 4:
					sel.link(hit.from(), hit.to());
					CFHGuiSounds.tick();
					this.gesture = EditorTool.Gesture.NONE;
					break;
				default:
					this.bandAdds = shift;
					this.bandQuests.clear();
					this.bandDecorations.clear();
					if (shift) {
						this.bandQuests.addAll(sel.quests());
						this.bandDecorations.addAll(sel.decorations());
					}

					this.bandX = mx;
					this.bandY = my;
					this.gesture = EditorTool.Gesture.BAND;
			}
		} else {
			if (hit.quest() != null && !sel.has(hit.quest())) {
				sel.only(hit.quest());
			} else if (hit.kind() == 3 && !sel.hasDecoration(hit.decoration())) {
				sel.onlyDecoration(hit.decoration());
			} else if (hit.kind() == 4) {
				sel.link(hit.from(), hit.to());
			}

			this.gesture = EditorTool.Gesture.RIGHT;
		}
	}

	void drag(QuestCanvas canvas, double mx, double my, boolean alt) {
		if (!this.moved && Math.abs(mx - this.pressX) + Math.abs(my - this.pressY) >= 3.0) {
			this.moved = true;
		}

		switch (this.gesture) {
			case RIGHT:
				if (!this.moved) {
					return;
				}

				this.gesture = EditorTool.Gesture.PAN;
				this.pressX = mx;
				this.pressY = my;
				break;
			case PAN:
				canvas.pan((float)((this.pressX - mx) / (double)canvas.unit()), (float)((this.pressY - my) / (double)canvas.unit()));
				this.pressX = mx;
				this.pressY = my;
				break;
			case PRESS:
				if (!this.moved) {
					return;
				}

				if (!this.startDrag(canvas)) {
					this.gesture = EditorTool.Gesture.NONE;
					return;
				}

				this.dragTo(canvas, mx, my, alt);
				break;
			case DRAG:
				this.dragTo(canvas, mx, my, alt);
				break;
			case BAND:
				this.bandTo(canvas, mx, my);
				break;
			case LINK:
				this.linkTo(canvas, mx, my);
		}
	}

	void release(QuestCanvas canvas, double mx, double my, boolean shift) {
		EditorTool.Gesture was = this.gesture;
		EditorTool.Hit hit = this.pressHit;
		this.gesture = EditorTool.Gesture.NONE;
		switch (was) {
			case RIGHT:
				EditorMenus.open(this.s, hit, mx, my, canvas.tx(mx), canvas.ty(my));
			case PAN:
			default:
				break;
			case PRESS:
				this.clicked(canvas, hit, shift);
				break;
			case DRAG:
				this.finishDrag(canvas);
				break;
			case BAND:
				if (!this.moved && !this.bandAdds) {
					this.s.selection.clear();
				}
				break;
			case LINK:
				this.finishLink(canvas, mx, my);
		}

		if (was != EditorTool.Gesture.LINK) {
			this.clearGesture();
		}
	}

	private void clicked(QuestCanvas canvas, EditorTool.Hit hit, boolean shift) {
		Selection sel = this.s.selection;
		if (hit.quest() != null) {
			if (!shift && this.pressWasChosen && sel.size() > 1) {
				sel.only(hit.quest());
			}

			long now = Clock.nowMs();
			boolean twice = !shift && hit.quest().equals(this.lastClickQuest) && now - this.lastClickMs < 320L;
			this.lastClickQuest = shift ? null : hit.quest();
			this.lastClickMs = now;
			CFHGuiSounds.tick();
			if (twice) {
				this.lastClickQuest = null;
				Quest q = this.s.quest(hit.quest());
				if (q != null && q.isGateway()) {
					this.s.enter(this.key(hit.quest()));
				} else {
					this.s.panel.focusFirst();
				}
			}
		} else if (hit.kind() == 3) {
			if (!shift && this.pressWasChosen && sel.size() > 1) {
				sel.onlyDecoration(hit.decoration());
			}

			CFHGuiSounds.tick();
		}
	}

	private void clearGesture() {
		this.gesture = EditorTool.Gesture.NONE;
		this.sticky = false;
		this.simulated = false;
		this.questOrigins.clear();
		this.decorationOrigins.clear();
		this.clearGuides();
		this.linkFrom = this.linkOnto = null;
		this.dragDx = this.dragDy = 0.0F;
	}

	private void clearGuides() {
		this.guideX = this.guideY = Float.NaN;
	}

	boolean cancel() {
		if (this.gesture == EditorTool.Gesture.NONE) {
			return false;
		} else {
			QuestCanvas canvas = this.s.canvas();
			if (this.gesture == EditorTool.Gesture.DRAG && canvas != null) {
				this.placeDragged(canvas, 0.0F, 0.0F);
			}

			if (this.gesture == EditorTool.Gesture.BAND) {
				this.s.selection.set(this.bandQuests, this.bandDecorations);
			}

			if (canvas != null) {
				canvas.graph().bounds();
			}

			this.clearGesture();
			return true;
		}
	}

	void reset() {
		this.clearGesture();
		this.hover = EditorTool.Hit.NOTHING;
		this.hoverAtX = this.hoverAtY = Integer.MIN_VALUE;
		this.tag = null;
		this.ringOf = null;
		this.lastClickQuest = null;
	}

	void rebind() {
		this.hoverAtX = this.hoverAtY = Integer.MIN_VALUE;
		this.tag = null;
		QuestCanvas canvas = this.s.canvas();
		Chapter work = this.s.work();
		if (canvas != null && work != null) {
			if (this.gesture == EditorTool.Gesture.DRAG) {
				this.questOrigins.keySet().removeIf(id -> work.quest(id) == null);

				for (Entry<String, float[]> e : this.questOrigins.entrySet()) {
					Quest q = work.quest(e.getKey());
					e.setValue(new float[]{q.x, q.y});
				}

				this.decorationOrigins.keySet().removeIf(i -> i >= work.decorations.size());
				if (this.questOrigins.isEmpty() && this.decorationOrigins.isEmpty()) {
					this.clearGesture();
					return;
				}

				this.placeDragged(canvas, this.dragDx, this.dragDy);
			} else if (this.gesture == EditorTool.Gesture.LINK && (this.linkFrom == null || work.quest(this.linkFrom) == null)) {
				this.clearGesture();
			}
		} else {
			this.clearGesture();
		}
	}

	void pop(String questId) {
		QuestCanvas canvas = this.s.canvas();
		NodeView n = canvas == null ? null : canvas.graph().node(this.key(questId));
		if (n != null) {
			n.appearIn(0);
		}
	}

	private boolean startDrag(QuestCanvas canvas) {
		Chapter work = this.s.work();
		if (work == null) {
			return false;
		} else {
			this.questOrigins.clear();
			this.decorationOrigins.clear();

			for (String id : this.s.selection.quests()) {
				Quest q = work.quest(id);
				if (q != null) {
					this.questOrigins.put(id, new float[]{q.x, q.y});
				}
			}

			for (int i : this.s.selection.decorations()) {
				if (i < work.decorations.size()) {
					this.decorationOrigins.put(i, new float[]{work.decorations.get(i).x, work.decorations.get(i).y});
				}
			}

			if (this.questOrigins.isEmpty() && this.decorationOrigins.isEmpty()) {
				return false;
			} else {
				float[] held = this.pressHit.quest() != null ? this.questOrigins.get(this.pressHit.quest()) : this.decorationOrigins.get(this.pressHit.decoration());
				float[] first = held != null
					? held
					: (!this.questOrigins.isEmpty() ? this.questOrigins.values().iterator().next() : this.decorationOrigins.values().iterator().next());
				this.heldX = first[0];
				this.heldY = first[1];
				this.dragDx = this.dragDy = 0.0F;
				this.gesture = EditorTool.Gesture.DRAG;
				return true;
			}
		}
	}

	private void dragTo(QuestCanvas canvas, double mx, double my, boolean alt) {
		GraphModel graph = canvas.graph();
		float unit = Math.max(1.0F, canvas.unit());
		float rawX = this.heldX + canvas.tx(mx) - this.pressTx;
		float rawY = this.heldY + canvas.ty(my) - this.pressTy;
		boolean snap = EditSession.snapping != alt;
		float toX = snap ? EditorOps.snap(rawX) : rawX;
		float toY = snap ? EditorOps.snap(rawY) : rawY;
		float reach = 4.5F / unit;
		float nearX = snap ? Math.abs(toX - rawX) : reach;
		float nearY = snap ? Math.abs(toY - rawY) : reach;

		for (NodeView n : graph.nodes) {
			if (!this.questOrigins.containsKey(n.quest.id)) {
				float ax = Math.abs(n.x - rawX);
				float ay = Math.abs(n.y - rawY);
				if (ax < reach && ax <= nearX) {
					nearX = ax;
					toX = n.x;
				}

				if (ay < reach && ay <= nearY) {
					nearY = ay;
					toY = n.y;
				}
			}
		}

		this.placeDragged(canvas, EditorOps.tidy(toX - this.heldX), EditorOps.tidy(toY - this.heldY));
		this.guides(graph);
	}

	private void placeDragged(QuestCanvas canvas, float dx, float dy) {
		this.dragDx = dx;
		this.dragDy = dy;
		GraphModel graph = canvas.graph();

		for (Entry<String, float[]> e : this.questOrigins.entrySet()) {
			NodeView n = graph.node(this.key(e.getKey()));
			if (n != null) {
				n.x = e.getValue()[0] + dx;
				n.y = e.getValue()[1] + dy;
			}
		}

		List<Chapter.Decoration> shown = graph.chapter.decorations;

		for (Entry<Integer, float[]> ex : this.decorationOrigins.entrySet()) {
			if (ex.getKey() < shown.size()) {
				shown.get(ex.getKey()).x = ex.getValue()[0] + dx;
				shown.get(ex.getKey()).y = ex.getValue()[1] + dy;
			}
		}
	}

	private void guides(GraphModel graph) {
		this.clearGuides();
		float x = this.heldX + this.dragDx;
		float y = this.heldY + this.dragDy;
		float x0 = x;
		float x1 = x;
		float y0 = y;
		float y1 = y;
		boolean down = false;
		boolean across = false;

		for (NodeView n : graph.nodes) {
			if (!this.questOrigins.containsKey(n.quest.id)) {
				if (Math.abs(n.x - x) < 0.006F) {
					down = true;
					y0 = Math.min(y0, n.y);
					y1 = Math.max(y1, n.y);
				}

				if (Math.abs(n.y - y) < 0.006F) {
					across = true;
					x0 = Math.min(x0, n.x);
					x1 = Math.max(x1, n.x);
				}
			}
		}

		if (down) {
			this.guideX = x;
			this.guideX0 = y0;
			this.guideX1 = y1;
		}

		if (across) {
			this.guideY = y;
			this.guideY0 = x0;
			this.guideY1 = x1;
		}
	}

	private void finishDrag(QuestCanvas canvas) {
		float dx = this.dragDx;
		float dy = this.dragDy;
		Map<String, float[]> quests = new LinkedHashMap<>(this.questOrigins);
		Map<Integer, float[]> decorations = new LinkedHashMap<>(this.decorationOrigins);
		this.placeDragged(canvas, 0.0F, 0.0F);
		if (dx != 0.0F || dy != 0.0F) {
			this.s.change(null, () -> {
				Chapter work = this.s.work();

				for (Entry<String, float[]> e : quests.entrySet()) {
					Quest q = work.quest(e.getKey());
					if (q != null) {
						q.x = EditorOps.tidy(e.getValue()[0] + dx);
						q.y = EditorOps.tidy(e.getValue()[1] + dy);
					}
				}

				for (Entry<Integer, float[]> ex : decorations.entrySet()) {
					if (ex.getKey() < work.decorations.size()) {
						work.decorations.get(ex.getKey()).x = EditorOps.tidy(ex.getValue()[0] + dx);
						work.decorations.get(ex.getKey()).y = EditorOps.tidy(ex.getValue()[1] + dy);
					}
				}
			});
		}
	}

	private void bandTo(QuestCanvas canvas, double mx, double my) {
		this.bandX = mx;
		this.bandY = my;
		if (this.moved) {
			Chapter work = this.s.work();
			if (work != null) {
				float ax = canvas.sx(this.pressTx);
				float ay = canvas.sy(this.pressTy);
				double x0 = Math.min((double)ax, mx);
				double x1 = Math.max((double)ax, mx);
				double y0 = Math.min((double)ay, my);
				double y1 = Math.max((double)ay, my);
				Set<String> quests = new LinkedHashSet<>(this.bandQuests);
				Set<Integer> decorations = new LinkedHashSet<>(this.bandDecorations);
				GraphModel graph = canvas.graph();

				for (NodeView n : graph.nodes) {
					float x = canvas.sx(n.x);
					float y = canvas.sy(n.y);
					float r = canvas.radiusOf(n);
					if ((double)(x + r) >= x0 && (double)(x - r) <= x1 && (double)(y + r) >= y0 && (double)(y - r) <= y1 && work.quest(n.quest.id) != null) {
						quests.add(n.quest.id);
					}
				}

				List<Chapter.Decoration> shown = graph.chapter.decorations;

				for (int i = 0; i < Math.min(shown.size(), work.decorations.size()); i++) {
					float[] box = this.box(canvas, shown.get(i));
					if ((double)box[2] >= x0 && (double)box[0] <= x1 && (double)box[3] >= y0 && (double)box[1] <= y1) {
						decorations.add(i);
					}
				}

				this.s.selection.set(quests, decorations);
			}
		}
	}

	void startLink(@Nullable String questId, boolean follows) {
		if (questId != null && this.s.quest(questId) != null) {
			this.clearGesture();
			this.linkFrom = questId;
			this.linkOnto = null;
			this.linkCan = EditorOps.Link.OK;
			this.sticky = follows;
			this.gesture = EditorTool.Gesture.LINK;
			QuestCanvas canvas = this.s.canvas();
			NodeView n = canvas == null ? null : this.node(canvas, questId);
			if (n != null) {
				this.linkX = n.x;
				this.linkY = n.y;
			}

			if (canvas != null && follows) {
				this.linkTo(canvas, mouseX(), mouseY());
			}
		}
	}

	private void linkTo(QuestCanvas canvas, double mx, double my) {
		Chapter work = this.s.work();
		if (work != null && this.linkFrom != null) {
			this.linkX = canvas.tx(mx);
			this.linkY = canvas.ty(my);
			NodeView on = canvas.nodeAt(mx, my);
			this.linkOnto = on != null && work.quest(on.quest.id) != null ? on.quest.id : null;
			this.linkCan = this.linkOnto == null ? EditorOps.Link.OK : EditorOps.canLink(work, this.linkFrom, this.linkOnto);
		}
	}

	private void finishLink(QuestCanvas canvas, double mx, double my) {
		String from = this.linkFrom;
		String onto = this.linkOnto;
		this.clearGesture();
		if (from != null) {
			if (onto != null) {
				if (!onto.equals(from)) {
					this.s.link(from, onto);
				}
			} else {
				NodeView source = this.node(canvas, from);
				if (source != null && canvas.free().contains(mx, my)) {
					if (!(Math.hypot(mx - (double)canvas.sx(source.x), my - (double)canvas.sy(source.y)) < (double)(canvas.radiusOf(source) + 14.0F))) {
						float[] at = this.place(canvas.tx(mx), canvas.ty(my));
						this.s.newQuest(at[0], at[1], from);
					}
				}
			}
		}
	}

	void menuByKey() {
		QuestCanvas canvas = this.s.canvas();
		Chapter work = this.s.work();
		if (canvas != null && work != null && this.gesture == EditorTool.Gesture.NONE) {
			Selection sel = this.s.selection;
			Rect free = canvas.free();
			float x = canvas.tx((double)free.centerX());
			float y = canvas.ty((double)free.centerY());
			EditorTool.Hit hit = EditorTool.Hit.NOTHING;
			if (sel.kind() == Selection.Kind.LINK) {
				Quest a = work.quest(sel.linkFrom());
				Quest b = work.quest(sel.linkTo());
				if (a != null && b != null) {
					hit = EditorTool.Hit.link(a.id, b.id);
					x = (a.x + b.x) / 2.0F;
					y = (a.y + b.y) / 2.0F;
				}
			} else if (!sel.quests().isEmpty()) {
				Quest q = work.quest(sel.quests().iterator().next());
				if (q != null) {
					hit = EditorTool.Hit.quest(q.id, false);
					x = q.x;
					y = q.y;
				}
			} else if (!sel.decorations().isEmpty()) {
				int i = sel.decorations().iterator().next();
				if (i < work.decorations.size()) {
					hit = EditorTool.Hit.decoration(i);
					x = work.decorations.get(i).x;
					y = work.decorations.get(i).y;
				}
			}

			double mx = (double)Mth.clamp(canvas.sx(x) + 6.0F, (float)free.x() + 4.0F, (float)free.right() - 4.0F);
			double my = (double)Mth.clamp(canvas.sy(y) + 6.0F, (float)free.y() + 4.0F, (float)free.bottom() - 4.0F);
			EditorMenus.open(this.s, hit, mx, my, x, y);
		}
	}

	void demoDrag(QuestCanvas canvas, String questId, float toX, float toY) {
		NodeView n = this.node(canvas, questId);
		if (n != null) {
			this.press(canvas, (double)canvas.sx(n.x), (double)canvas.sy(n.y), 0, false, false);
			this.simulated = true;
			this.drag(canvas, (double)canvas.sx(toX), (double)canvas.sy(toY), false);
			this.simulated = true;
		}
	}

	void demoLink(String questId) {
		this.startLink(questId, true);
		this.simulated = true;
	}

	void demoBand(QuestCanvas canvas, float x0, float y0, float x1, float y1) {
		this.press(canvas, (double)canvas.sx(x0), (double)canvas.sy(y0), 0, false, false);
		if (this.gesture == EditorTool.Gesture.BAND) {
			this.simulated = true;
			this.drag(canvas, (double)canvas.sx(x1), (double)canvas.sy(y1), false);
		}
	}

	void gesture(QuestCanvas canvas, double x0, double y0, double x1, double y1, boolean shift, boolean ctrl, boolean alt) {
		this.press(canvas, x0, y0, 0, shift, ctrl);
		this.simulated = true;
		if (Math.abs(x1 - x0) + Math.abs(y1 - y0) >= 3.0) {
			this.drag(canvas, (x0 + x1) / 2.0, (y0 + y1) / 2.0, alt);
			this.drag(canvas, x1, y1, alt);
		}

		if (this.gesture != EditorTool.Gesture.NONE) {
			this.release(canvas, x1, y1, shift);
		}
	}

	@Override
	public void renderUnder(GuiGraphics g, QuestCanvas canvas, int mouseX, int mouseY) {
		if (!this.s.editing()) {
			this.shown.snap(0.0F);
		} else {
			this.shown.set(1.0F);
			float in = this.shown.get();
			if (!(in <= 0.01F) && this.s.work() != null) {
				Theme t = Theme.current();
				boolean van = t.isVanilla();
				int x0 = canvas.getX();
				int y0 = canvas.getY();
				int x1 = x0 + canvas.getWidth();
				int y1 = y0 + canvas.getHeight();
				float step = 0.5F;

				while (step * canvas.unit() < 7.0F) {
					step *= 2.0F;
				}

				int ink = van ? -1 : t.palette().text();
				int fine = Colors.withAlpha(ink, Math.round((float)(EditSession.snapping ? 11 : 6) * in));
				int whole = Colors.withAlpha(ink, Math.round((float)(EditSession.snapping ? 24 : 12) * in));
				int origin = Colors.withAlpha(van ? -1 : t.accent(), Math.round(56.0F * in));
				float left = canvas.tx((double)x0);
				float top = canvas.ty((double)y0);

				for (float v = (float)Math.ceil((double)(left / step)) * step; canvas.sx(v) < (float)x1; v += step) {
					int x = Math.round(canvas.sx(v));
					boolean unit = Math.abs(v - (float)Math.round(v)) < 0.01F;
					g.fill(x, y0, x + 1, y1, Math.abs(v) < 0.01F ? origin : (unit ? whole : fine));
				}

				for (float v = (float)Math.ceil((double)(top / step)) * step; canvas.sy(v) < (float)y1; v += step) {
					int y = Math.round(canvas.sy(v));
					boolean unit = Math.abs(v - (float)Math.round(v)) < 0.01F;
					g.fill(x0, y, x1, y + 1, Math.abs(v) < 0.01F ? origin : (unit ? whole : fine));
				}

				if (this.gesture == EditorTool.Gesture.DRAG) {
					this.drawGuides(g, canvas, van);
				}
			}
		}
	}

	@Override
	public void renderOver(GuiGraphics g, QuestCanvas canvas, int mouseX, int mouseY) {
		if (this.s.editing()) {
			float in = this.shown.get();
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			int accent = van ? -1 : t.accent();
			Chapter work = this.s.work();
			if (work != null) {
				this.follow(canvas, mouseX, mouseY);
				GraphModel graph = canvas.graph();
				float time = t.motion() > 0.0F ? (float)(Clock.nowMs() % 3600000L) / 1000.0F : 0.0F;
				this.decorations(g, canvas, graph, work, accent, van);
				this.lines(g, canvas, graph, accent, van);
				this.chosen(g, canvas, graph, accent, time);
				this.rim(g, canvas, accent, van);
				if (this.gesture == EditorTool.Gesture.BAND && this.moved) {
					this.band(g, canvas, accent);
				}

				if (this.gesture == EditorTool.Gesture.LINK) {
					this.pulled(g, canvas, accent, van, time);
				}

				if (this.gesture == EditorTool.Gesture.DRAG) {
					this.readout(g, canvas, van);
				}

				if (work.quests.isEmpty() && this.gesture == EditorTool.Gesture.NONE) {
					this.hint(g, canvas, van);
				}
			}

			this.strip(g, canvas, accent, van, in);
		}
	}

	private void follow(QuestCanvas canvas, int overX, int overY) {
		double mx = mouseX();
		double my = mouseY();
		if (this.gesture == EditorTool.Gesture.NONE) {
			if (overX != this.hoverAtX || overY != this.hoverAtY) {
				this.hoverAtX = overX;
				this.hoverAtY = overY;
				EditorTool.Hit now = !Popups.any() && canvas.free().contains((double)overX, (double)overY)
					? this.hit(canvas, (double)overX, (double)overY)
					: EditorTool.Hit.NOTHING;
				if (!now.equals(this.hover)) {
					this.hover = now;
					this.hoverSinceMs = Clock.nowMs();
					this.tag = null;
				}
			}

			long rested = Clock.nowMs() - this.hoverSinceMs;
			if (this.hover.kind() == 2 && rested > 700L) {
				CFHGuiTooltips.request(Tr.t("hint.rim"), canvas);
			} else if (this.hover.kind() == 4 && rested > 700L) {
				CFHGuiTooltips.request(Tr.t("hint.line"), canvas);
			} else if (this.hover.kind() == 1 && rested > 350L && (this.s.selection.kind() != Selection.Kind.QUEST || !this.s.selection.has(this.hover.quest()))) {
				this.nameTag(canvas, this.hover.quest());
			}
		} else {
			this.hover = EditorTool.Hit.NOTHING;
			if (this.simulated) {
				if (this.gesture == EditorTool.Gesture.LINK) {
					this.linkTo(canvas, mx, my);
				}
			} else if (this.sticky) {
				this.linkTo(canvas, mx, my);
			} else {
				long window = Minecraft.getInstance().getWindow().getWindow();
				if (GLFW.glfwGetMouseButton(window, this.button) != 1) {
					this.release(canvas, mx, my, Screen.hasShiftDown());
				} else {
					if (this.gesture == EditorTool.Gesture.DRAG || this.gesture == EditorTool.Gesture.BAND || this.gesture == EditorTool.Gesture.LINK) {
						Rect free = canvas.free();
						float margin = 12.0F;
						float vx = 0.0F;
						float vy = 0.0F;
						if (mx < (double)((float)free.x() + 12.0F)) {
							vx = -((float)Math.min(1.0, ((double)((float)free.x() + 12.0F) - mx) / 12.0));
						} else if (mx > (double)((float)free.right() - 12.0F)) {
							vx = (float)Math.min(1.0, (mx - (double)free.right() + 12.0) / 12.0);
						}

						if (my < (double)((float)free.y() + 12.0F)) {
							vy = -((float)Math.min(1.0, ((double)((float)free.y() + 12.0F) - my) / 12.0));
						} else if (my > (double)((float)free.bottom() - 12.0F)) {
							vy = (float)Math.min(1.0, (my - (double)free.bottom() + 12.0) / 12.0);
						}

						if (vx != 0.0F || vy != 0.0F) {
							float speed = 9.0F / Math.max(0.4F, canvas.zoom()) * Clock.frameDelta() / 1000.0F;
							canvas.pan(vx * speed, vy * speed);
							this.drag(canvas, mx, my, Screen.hasAltDown());
						}
					}
				}
			}
		}
	}

	private void nameTag(QuestCanvas canvas, @Nullable String questId) {
		Quest q = this.s.quest(questId);
		if (q != null && questId != null) {
			if (this.tag == null) {
				List<Component> lines = new ArrayList<>();
				lines.add(QuestText.title(q));
				lines.add(Component.literal(this.key(questId)).withStyle(style -> style.withColor(10592151)));
				if (q.isGateway()) {
					lines.add(Tr.t("hint.gateway").copy().withStyle(style -> style.withColor(10592151)));
				}

				this.tag = lines;
			}

			CFHGuiTooltips.request(this.tag, canvas);
		}
	}

	private void decorations(GuiGraphics g, QuestCanvas canvas, GraphModel graph, Chapter work, int accent, boolean van) {
		List<Chapter.Decoration> shown = graph.chapter.decorations;
		int n = Math.min(shown.size(), work.decorations.size());

		for (int i : this.s.selection.decorations()) {
			if (i < n) {
				float[] b = this.box(canvas, shown.get(i));
				outline(g, b, 3, accent);
			}
		}

		if (this.hover.kind() == 3 && this.hover.decoration() < n && !this.s.selection.hasDecoration(this.hover.decoration())) {
			outline(g, this.box(canvas, shown.get(this.hover.decoration())), 3, Colors.withAlpha(accent, 128));
		}

		if (this.blankOf != graph) {
			this.blankOf = graph;
			this.blank.clear();

			for (int ix = 0; ix < shown.size(); ix++) {
				Chapter.Decoration d = shown.get(ix);
				if ("image".equals(d.type) ? d.image.isBlank() : d.text.isBlank()) {
					this.blank.add(ix);
				}
			}
		}

		for (int k = 0; k < this.blank.size(); k++) {
			int ixx = this.blank.get(k);
			if (ixx < n) {
				float[] b = this.box(canvas, shown.get(ixx));
				if (!(b[2] < (float)canvas.getX()) && !(b[0] > (float)(canvas.getX() + canvas.getWidth()))) {
					outline(g, b, 1, Colors.withAlpha(van ? -1 : Theme.current().palette().textDim(), 160));
				}
			}
		}
	}

	private static void outline(GuiGraphics g, float[] box, int grow, int color) {
		int x = Math.round(box[0]) - grow;
		int y = Math.round(box[1]) - grow;
		int w = Math.round(box[2] - box[0]) + grow * 2;
		int h = Math.round(box[3] - box[1]) + grow * 2;
		CFHGuiDraw.outline(g, x, y, w, h, color, 0);
	}

	private void lines(GuiGraphics g, QuestCanvas canvas, GraphModel graph, int accent, boolean van) {
		Selection sel = this.s.selection;
		if (sel.kind() == Selection.Kind.LINK) {
			this.line(g, canvas, sel.linkFrom(), sel.linkTo(), van ? -171 : Theme.current().palette().warning(), true);
		}

		if (this.hover.kind() == 4 && !sel.hasLink(this.hover.from(), this.hover.to())) {
			this.line(g, canvas, this.hover.from(), this.hover.to(), Colors.withAlpha(accent, 192), false);
		}
	}

	private void line(GuiGraphics g, QuestCanvas canvas, @Nullable String from, @Nullable String to, int color, boolean mark) {
		NodeView a = this.node(canvas, from);
		NodeView b = this.node(canvas, to);
		if (a != null && b != null) {
			float x1 = canvas.sx(a.x);
			float y1 = canvas.sy(a.y);
			float x2 = canvas.sx(b.x);
			float y2 = canvas.sy(b.y);
			float len = Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
			if (!(len < 2.0F)) {
				float ta = Math.min(0.45F, canvas.radiusOf(a) / len);
				float tb = Math.min(0.45F, canvas.radiusOf(b) / len);
				float sx = Mth.lerp(ta, x1, x2);
				float sy = Mth.lerp(ta, y1, y2);
				float ex = Mth.lerp(1.0F - tb, x1, x2);
				float ey = Mth.lerp(1.0F - tb, y1, y2);
				CanvasDraw.line(g, sx, sy, ex, ey, 3.0F, color);
				CanvasDraw.arrow(g, ex, ey, ex - sx, ey - sy, 4.0F, 1.1F, color);
				if (mark) {
					int mx = Math.round((sx + ex) / 2.0F);
					int my = Math.round((sy + ey) / 2.0F);
					CFHGuiDraw.pixelCircle(g, mx, my, 4, -15724528);
					CFHGuiDraw.pixelCircle(g, mx, my, 3, color);
				}
			}
		}
	}

	private void drawGuides(GuiGraphics g, QuestCanvas canvas, boolean van) {
		int color = van ? -171 : -10696472;
		float reach = 0.6F;
		if (!Float.isNaN(this.guideX)) {
			int x = Math.round(canvas.sx(this.guideX));
			g.fill(x, Math.round(canvas.sy(this.guideX0 - 0.6F)), x + 1, Math.round(canvas.sy(this.guideX1 + 0.6F)), color);
		}

		if (!Float.isNaN(this.guideY)) {
			int y = Math.round(canvas.sy(this.guideY));
			g.fill(Math.round(canvas.sx(this.guideY0 - 0.6F)), y, Math.round(canvas.sx(this.guideY1 + 0.6F)), y + 1, color);
		}
	}

	private void chosen(GuiGraphics g, QuestCanvas canvas, GraphModel graph, int accent, float time) {
		Set<String> ids = this.s.selection.quests();
		if (ids.isEmpty()) {
			this.chosenOf = null;
			this.chosenNodes.clear();
		} else {
			if (this.chosenOf != graph || this.chosenAt != this.s.selection.revision()) {
				this.chosenOf = graph;
				this.chosenAt = this.s.selection.revision();
				this.chosenNodes.clear();

				for (String id : ids) {
					NodeView n = graph.shown(this.key(id));
					if (n != null) {
						this.chosenNodes.add(n);
					}
				}
			}

			int left = canvas.getX() - 24;
			int right = canvas.getX() + canvas.getWidth() + 24;
			int top = canvas.getY() - 24;
			int bottom = canvas.getY() + canvas.getHeight() + 24;
			int breath = ids.size() == 1 ? Math.round(1.2F * Mth.sin(time * 3.0F)) : 0;

			for (int i = 0; i < this.chosenNodes.size(); i++) {
				NodeView n = this.chosenNodes.get(i);
				int x = Math.round(canvas.sx(n.x));
				int y = Math.round(canvas.sy(n.y));
				if (x >= left && x <= right && y >= top && y <= bottom) {
					int d = Math.round(canvas.radiusOf(n)) + 5 + breath;
					corners(g, x, y, d, Math.max(3, Math.min(5, d / 3)), accent);
				}
			}
		}
	}

	private static void corners(GuiGraphics g, int cx, int cy, int d, int arm, int color) {
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				int x = cx + sx * d;
				int y = cy + sy * d;
				g.fill(Math.min(x, x - sx * arm), y, Math.max(x, x - sx * arm) + 1, y + 1, color);
				g.fill(x, Math.min(y, y - sy * arm), x + 1, Math.max(y, y - sy * arm) + 1, color);
			}
		}
	}

	private void rim(GuiGraphics g, QuestCanvas canvas, int accent, boolean van) {
		String over = this.gesture == EditorTool.Gesture.NONE && this.hover.quest() != null ? this.hover.quest() : null;
		if (over != null) {
			this.ringOf = over;
		}

		this.ring.set(over != null);
		float shown = this.ring.get();
		NodeView n = this.node(canvas, this.ringOf);
		if (!(shown <= 0.02F) && n != null) {
			float x = canvas.sx(n.x);
			float y = canvas.sy(n.y);
			float r = canvas.radiusOf(n) + 4.0F;
			boolean on = this.hover.kind() == 2 && over != null;
			CanvasDraw.ring(g, x, y, r, Colors.withAlpha(accent, Math.round((float)(on ? 240 : 112) * shown)));
			if (on) {
				CanvasDraw.ring(g, x, y, r + 1.0F, Colors.withAlpha(accent, Math.round(96.0F * shown)));
				double a = Math.atan2(mouseY() - (double)y, mouseX() - (double)x);
				int kx = Math.round(x + (float)Math.cos(a) * r);
				int ky = Math.round(y + (float)Math.sin(a) * r);
				CFHGuiDraw.pixelCircle(g, kx, ky, 3, van ? -16777216 : -15724528);
				CFHGuiDraw.pixelCircle(g, kx, ky, 2, accent);
			}
		}
	}

	private void band(GuiGraphics g, QuestCanvas canvas, int accent) {
		int ax = Math.round(canvas.sx(this.pressTx));
		int ay = Math.round(canvas.sy(this.pressTy));
		int x0 = Math.min(ax, (int)this.bandX);
		int x1 = Math.max(ax, (int)this.bandX);
		int y0 = Math.min(ay, (int)this.bandY);
		int y1 = Math.max(ay, (int)this.bandY);
		g.fill(x0, y0, x1, y1, Colors.withAlpha(accent, 34));
		CFHGuiDraw.outline(g, x0, y0, x1 - x0, y1 - y0, Colors.withAlpha(accent, 208), 0);
	}

	private void pulled(GuiGraphics g, QuestCanvas canvas, int accent, boolean van, float time) {
		NodeView from = this.node(canvas, this.linkFrom);
		if (from != null) {
			Palette p = Theme.current().palette();
			NodeView onto = this.linkOnto != null && !this.linkOnto.equals(this.linkFrom) ? this.node(canvas, this.linkOnto) : null;
			boolean bad = onto != null && this.linkCan != EditorOps.Link.OK;
			int color = bad ? (van ? -43691 : p.danger()) : accent;
			float x1 = canvas.sx(from.x);
			float y1 = canvas.sy(from.y);
			float x2 = canvas.sx(this.linkX);
			float y2 = canvas.sy(this.linkY);
			float[] spot = null;
			if (onto != null) {
				x2 = canvas.sx(onto.x);
				y2 = canvas.sy(onto.y);
			} else if (canvas.free().contains((double)x2, (double)y2) && Math.hypot((double)(x2 - x1), (double)(y2 - y1)) >= (double)(canvas.radiusOf(from) + 14.0F)) {
				spot = this.place(this.linkX, this.linkY);
				x2 = canvas.sx(spot[0]);
				y2 = canvas.sy(spot[1]);
			}

			float len = Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
			if (!(len < canvas.radiusOf(from) + 2.0F)) {
				float ta = canvas.radiusOf(from) / len;
				float end = onto != null ? canvas.radiusOf(onto) + 2.0F : (spot != null ? canvas.painter().radius() * canvas.zoom() + 2.0F : 0.0F);
				float tb = Math.min(0.9F, end / len);
				float sx = Mth.lerp(ta, x1, x2);
				float sy = Mth.lerp(ta, y1, y2);
				float ex = Mth.lerp(1.0F - tb, x1, x2);
				float ey = Mth.lerp(1.0F - tb, y1, y2);
				CanvasDraw.line(g, sx, sy, ex, ey, 2.0F, Colors.withAlpha(color, 112));
				CanvasDraw.dashes(g, sx, sy, ex, ey, 2.0F, 5.0F, 4.0F, time * 26.0F, color);
				CanvasDraw.arrow(g, ex, ey, ex - sx, ey - sy, 4.0F, 1.1F, color);
				if (onto != null) {
					float r = canvas.radiusOf(onto) + 3.0F;
					CanvasDraw.ring(g, x2, y2, r, color);
					CanvasDraw.ring(g, x2, y2, r + 1.0F, Colors.withAlpha(color, 112));
					if (bad) {
						label(g, canvas, Tr.t(this.linkCan == EditorOps.Link.CIRCLE ? "link.circle" : "link.exists"), Math.round(x2), Math.round(y2 - r) - 14, color, van);
					}
				} else if (spot != null) {
					float r = canvas.painter().radius() * canvas.zoom();
					CanvasDraw.ring(g, x2, y2, r, Colors.withAlpha(color, 192));
					int cx = Math.round(x2);
					int cy = Math.round(y2);
					int arm = Math.max(2, Math.round(r * 0.4F));
					g.fill(cx - arm, cy, cx + arm + 1, cy + 1, color);
					g.fill(cx, cy - arm, cx + 1, cy + arm + 1, color);
				}
			}
		}
	}

	private static void label(GuiGraphics g, QuestCanvas canvas, Component words, int cx, int cy, int color, boolean van) {
		Font font = CFHGuiDraw.font();
		int w = font.width(words) + 8;
		Palette p = Theme.current().palette();
		Rect free = canvas.free();
		int x = Mth.clamp(cx - w / 2, free.x() + 3, Math.max(free.x() + 3, free.right() - 3 - w));
		int y = Mth.clamp(cy, free.y() + 3, Math.max(free.y() + 3, free.bottom() - 15));
		if (van) {
			g.fill(x, y, x + w, y + 12, -536870912);
		} else {
			CFHGuiDraw.popupPlate(g, x, y, w, 12, p.bg(), p.border(), 0.0F, 1.0F);
		}

		g.drawString(font, words, x + 4, y + 2, color, van);
	}

	private void readout(GuiGraphics g, QuestCanvas canvas, boolean van) {
		Palette p = Theme.current().palette();
		String text = fmt(this.heldX + this.dragDx) + ", " + fmt(this.heldY + this.dragDy);
		int x = Math.round(canvas.sx(this.heldX + this.dragDx));
		int y = Math.round(canvas.sy(this.heldY + this.dragDy));
		label(g, canvas, Component.literal(text), x, y + 26, van ? -1 : p.textMuted(), van);
	}

	static String fmt(float v) {
		float r = EditorOps.tidy(v);
		return r == (float)Math.round(r) ? Integer.toString(Math.round(r)) : Float.toString(r);
	}

	private void hint(GuiGraphics g, QuestCanvas canvas, boolean van) {
		Rect free = canvas.free();
		Palette p = Theme.current().palette();
		CFHGuiDraw.textCentered(g, Tr.t("hint.empty"), free.centerX(), free.centerY() - 10, van ? -1 : p.textMuted(), van);
		CFHGuiDraw.textCentered(g, Tr.t("hint.empty.how"), free.centerX(), free.centerY() + 2, van ? -6250336 : p.textDim(), van);
	}

	private void strip(GuiGraphics g, QuestCanvas canvas, int accent, boolean van, float in) {
		if (!(in <= 0.02F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			Font font = CFHGuiDraw.font();
			int x = canvas.getX();
			int y = canvas.getY();
			int w = canvas.getWidth();
			int h = canvas.getHeight();
			int tone = van ? -22016 : accent;
			g.fill(x, y, x + w, y + 2, Colors.scaleAlpha(tone, in));
			g.fill(x, y + 2, x + 1, y + h, Colors.scaleAlpha(Colors.withAlpha(tone, 112), in));
			g.fill(x, y + h - 1, x + w, y + h, Colors.scaleAlpha(Colors.withAlpha(tone, 112), in));
			EditSession.Save save = this.s.save();
			Component notice = this.s.notice();
			Component state;
			int stateColor;
			if (notice != null) {
				state = notice;
				stateColor = van ? -1 : p.text();
			} else if (save == EditSession.Save.REFUSED) {
				state = Tr.t("save.refused");
				stateColor = van ? -43691 : p.danger();
			} else if (save == EditSession.Save.SAVING) {
				state = Tr.t("save.saving");
				stateColor = van ? -22016 : p.warning();
			} else {
				state = Tr.t("save.saved");
				stateColor = van ? -11141291 : p.success();
			}

			Component title = Tr.t("strip");
			int tw = font.width(title);
			int sw = font.width(state);
			int cw = 16 + tw + 7 + 6 + sw + 6;
			int cx = x + 5 + Math.round((1.0F - in) * -6.0F);
			int cy = y + 6;
			if (van) {
				g.fill(cx, cy, cx + cw, cy + 14, Colors.scaleAlpha(-671088640, in));
				CFHGuiDraw.outline(g, cx, cy, cw, 14, Colors.scaleAlpha(tone, in), 0);
			} else {
				CFHGuiDraw.popupPlate(g, cx, cy, cw, 14, p.bg(), Colors.mix(p.borderStrong(), tone, 0.6F), 0.4F, in);
			}

			g.flush();
			Icons.draw(g, Icon.EDIT, cx + 5, cy + 3, 8, Colors.scaleAlpha(tone, in));
			g.drawString(font, title, cx + 16, cy + 3, Colors.scaleAlpha(van ? -1 : p.text(), Math.max(0.03F, in)), van);
			int sx = cx + 16 + tw + 7;
			g.fill(sx - 4, cy + 3, sx - 3, cy + 11, Colors.scaleAlpha(van ? -8355712 : p.border(), in));
			float beat = save == EditSession.Save.SAVING && t.motion() > 0.0F ? 0.55F + 0.45F * Mth.sin((float)Clock.nowMs() / 140.0F) : 1.0F;
			g.fill(sx, cy + 5, sx + 4, cy + 9, Colors.scaleAlpha(stateColor, in * beat));
			g.drawString(
				font,
				state,
				sx + 6,
				cy + 3,
				Colors.scaleAlpha(notice != null ? stateColor : Colors.mix(stateColor, van ? -1 : p.textMuted(), 0.35F), Math.max(0.03F, in)),
				van
			);
			double mx = mouseX();
			double my = mouseY();
			boolean over = mx >= (double)cx && mx < (double)(cx + cw) && my >= (double)cy && my < (double)(cy + 14) && !Popups.any();
			if (!over) {
				this.chipSinceMs = 0L;
			} else {
				if (this.chipSinceMs == 0L) {
					this.chipSinceMs = Clock.nowMs();
				}

				if (Clock.nowMs() - this.chipSinceMs >= 350L) {
					if (save == EditSession.Save.REFUSED && this.s.refusal() != null) {
						CFHGuiTooltips.request(this.s.refusal(), canvas);
					} else {
						CFHGuiTooltips.request(Tr.t("strip.tip"), canvas);
					}
				}
			}
		}
	}

	List<NodeView> inView(QuestCanvas canvas) {
		List<NodeView> out = new ArrayList<>();

		for (NodeView n : canvas.graph().nodes) {
			float x = canvas.sx(n.x);
			float y = canvas.sy(n.y);
			if (x >= (float)canvas.getX()
				&& x < (float)(canvas.getX() + canvas.getWidth())
				&& y >= (float)canvas.getY()
				&& y < (float)(canvas.getY() + canvas.getHeight())) {
				out.add(n);
			}
		}

		return out;
	}

	String keyOf(String questId) {
		return Ids.key(this.s.work() == null ? "" : this.s.work().id, questId);
	}

	private static enum Gesture {
		NONE,
		RIGHT,
		PAN,
		PRESS,
		DRAG,
		BAND,
		LINK;
	}

	static record Hit(int kind, @Nullable String quest, int decoration, @Nullable String from, @Nullable String to) {
		static final int GROUND = 0;
		static final int QUEST = 1;
		static final int RIM = 2;
		static final int DECORATION = 3;
		static final int LINK = 4;
		static final EditorTool.Hit NOTHING = new EditorTool.Hit(0, null, -1, null, null);

		static EditorTool.Hit quest(String id, boolean rim) {
			return new EditorTool.Hit(rim ? 2 : 1, id, -1, null, null);
		}

		static EditorTool.Hit decoration(int index) {
			return new EditorTool.Hit(3, null, index, null, null);
		}

		static EditorTool.Hit link(String from, String to) {
			return new EditorTool.Hit(4, null, -1, from, to);
		}

		boolean ground() {
			return this.kind == 0;
		}
	}
}
