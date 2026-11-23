package com.onehumanawa.cfhcore.content.gui.quests.client.editor;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.BookExtensions;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.UiBits;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestCanvas;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class EditorPanel implements BookExtensions.SidePanel {
	static final int RAIL = 26;
	private static final int TOOL = 18;
	private static final int TOP = 28;
	private static final int SUBJECT = 26;
	private final EditSession s;
	@Nullable
	private EditorPanel.Root root;
	private int width = 200;
	@Nullable
	private Form form;
	private int builtStructure = -1;
	private int builtSelection = -1;
	private boolean builtBook;
	private boolean stale = true;
	private boolean wantFirst;
	private String builtSubject = "";

	EditorPanel(EditSession session) {
		this.s = session;
	}

	int widthOnScreen() {
		return this.width;
	}

	void invalidate() {
		this.stale = true;
	}

	void flush() {
		if (this.form != null) {
			this.form.flush();
		}

		this.s.flushSoon();
	}

	void focusFirst() {
		this.wantFirst = true;
		this.stale = true;
		if (EditSession.collapsed) {
			this.fold(false);
		}
	}

	void showChapter() {
		this.s.selection.clear();
		EditSession.bookTab = false;
		this.stale = true;
		if (EditSession.collapsed) {
			this.fold(false);
		}
	}

	void renamed(String from, String to) {
		if (this.builtSubject.equals("quest:" + from)) {
			this.builtSubject = "quest:" + to;
		}
	}

	void pressForTest(String name) {
		AbstractWidget w = this.form == null ? null : this.form.named(name);
		if (w != null) {
			w.keyPressed(257, 0, 0);
		}
	}

	boolean hasKeyboard() {
		EditorPanel.Root r = this.root;
		return r != null && this.s.screen.getFocused() == r && r.getFocused() != null;
	}

	void fold(boolean away) {
		if (EditSession.collapsed != away) {
			this.flush();
			QuestCanvas canvas = this.s.canvas();
			int before = this.width;
			boolean whole = EditSession.wholeInView(canvas);
			EditSession.collapsed = away;
			this.s.screen.rebuild();
			EditSession.room(canvas, whole, this.width - before);
		}
	}

	@Override
	public int width(int available) {
		return EditSession.collapsed ? 26 : Mth.clamp(Math.round((float)available * 0.46F), 184, 240);
	}

	@Override
	public void build(QuestBookScreen screen, Rect area, Consumer<AbstractWidget> add) {
		Rect plate = new Rect(area.x() - 8, area.y() - 8, area.w() + 16, area.h() + 16);
		this.width = plate.w();
		EditorPanel.Root old = this.root;
		double scroll = old == null ? 0.0 : old.body.scrollAmount();
		this.root = new EditorPanel.Root(plate);
		add.accept(this.root);
		this.form = null;
		this.fill();
		this.root.body.snapScroll(scroll);
	}

	@Override
	public void render(GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		EditorPanel.Root r = this.root;
		if (r != null) {
			this.s.tick();
			if (!EditSession.collapsed
				&& (this.stale || this.builtStructure != this.s.structure() || this.builtSelection != this.s.selection.revision() || this.builtBook != EditSession.bookTab)
				)
			 {
				this.fill();
			}

			r.undo.active = this.s.canUndo();
			r.redo.active = this.s.canRedo();
			r.snap.toggled(EditSession.snapping);
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			g.fill(area.x(), area.y(), area.right(), area.y() + 2, van ? -22016 : t.accent());
			if (!EditSession.collapsed) {
				g.fill(area.x() + 6, area.y() + 28 - 3, area.right() - 6, area.y() + 28 - 2, van ? -9474193 : p.border());
				this.subject(g, area, van);
			}
		}
	}

	@Override
	public void closed() {
		this.flush();
	}

	@Override
	public boolean keyPressed(QuestBookScreen screen, int keyCode, int scanCode, int modifiers) {
		EditorPanel.Root r = this.root;
		return r != null && r.key(keyCode, scanCode, modifiers);
	}

	private EditorPanel.Scope scope() {
		if (this.s.work() == null) {
			return EditorPanel.Scope.BOOK;
		} else {
			return switch (this.s.selection.kind()) {
				case QUEST -> EditorPanel.Scope.QUEST;
				case SEVERAL -> EditorPanel.Scope.SEVERAL;
				case DECORATION -> EditorPanel.Scope.DECORATION;
				case LINK -> EditorPanel.Scope.LINE;
				default -> EditSession.bookTab ? EditorPanel.Scope.BOOK : EditorPanel.Scope.CHAPTER;
			};
		}
	}

	private String subject(EditorPanel.Scope scope) {
		Chapter work = this.s.work();

		return switch (scope) {
			case QUEST -> "quest:" + this.s.selection.quest();
			case SEVERAL -> "several";
			case DECORATION -> "decoration:" + this.s.selection.decoration();
			case LINE -> "line:" + this.s.selection.linkFrom() + ">" + this.s.selection.linkTo();
			case CHAPTER -> "chapter:" + (work == null ? "" : work.id);
			case BOOK -> "book";
		};
	}

	private void fill() {
		EditorPanel.Root r = this.root;
		if (r != null) {
			this.stale = false;
			this.builtStructure = this.s.structure();
			this.builtSelection = this.s.selection.revision();
			this.builtBook = EditSession.bookTab;
			if (!EditSession.collapsed) {
				EditorPanel.Scope scope;
				boolean same;
				boolean keyboard;
				String had;
				String var10000;
				label96: {
					scope = this.scope();
					String subject = this.subject(scope);
					same = subject.equals(this.builtSubject);
					this.builtSubject = subject;
					keyboard = this.s.screen.getFocused() == r;
					had = this.form == null ? null : this.form.nameOf(r.field());
					if (r.field() instanceof EditField e && e.dirty()) {
						var10000 = e.getValue();
						break label96;
					}

					if (r.field() instanceof TextArea a && a.dirty()) {
						var10000 = a.getValue();
						break label96;
					}

					var10000 = null;
				}

				String typed = var10000;
				double scroll = same ? r.body.scrollAmount() : 0.0;
				r.subject(scope);
				r.body.clear();
				Form f = new Form(r.body.innerWidth() - 1, (widget, x, y) -> r.body.add(widget, x, y), this::invalidate);
				this.form = f;
				Chapter work = this.s.work();
				switch (scope) {
					case QUEST:
						Quest q = this.s.quest(this.s.selection.quest());
						if (q != null) {
							QuestForm.build(f, this.s, q);
						}
						break;
					case SEVERAL:
						OtherForms.several(f, this.s);
						break;
					case DECORATION:
						OtherForms.decoration(f, this.s, this.s.selection.decoration());
						break;
					case LINE:
						OtherForms.line(f, this.s, this.s.selection.linkFrom(), this.s.selection.linkTo());
						break;
					case CHAPTER:
						if (work != null) {
							ChapterForm.chapter(f, this.s, work);
						}
						break;
					case BOOK:
						ChapterForm.book(f, this.s);
				}

				r.body.setContentHeight(f.height() + 4);
				r.body.snapScroll(scroll);
				if (!same) {
					int i = 2;

					for (GuiEventListener child : r.body.children()) {
						if (child instanceof Renderable renderable) {
							i = CFHGuiScreen.entrance(renderable, i);
						}
					}
				}

				if (this.wantFirst) {
					this.wantFirst = false;
					r.focusField(f.first(), true);
				} else if (same && keyboard && had != null) {
					AbstractWidget again = f.named(had);
					r.focusField(again, false);
					if (typed != null && again instanceof EditField e) {
						e.setValue(typed);
					} else if (typed != null && again instanceof TextArea a) {
						a.setValue(typed);
					}
				}
			}
		}
	}

	private void subject(GuiGraphics g, Rect area, boolean van) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		Chapter work = this.s.work();
		int x = area.x() + 8;
		int y = area.y() + 28;
		int w = area.w() - 16;
		int text = van ? -1 : p.text();
		int dim = van ? -6250336 : p.textDim();
		switch (this.scope()) {
			case QUEST:
				String id = this.s.selection.quest();
				Quest q = this.s.quest(id);
				BookIndex.Entry e = id == null ? null : QuestsState.entry(this.s.key(id));
				if (q == null || work == null) {
					return;
				}

				if (e != null) {
					UiBits.medallion(g, e, QuestsState.status(e.key()), QuestPainter.accentOf(e.chapter()), x + 11, y + 12, 0.86F, 1.0F);
				}

				g.drawString(font, CFHGuiDraw.truncate(Fonts.heading(QuestText.title(q)), w - 28), x + 26, y + 2, text, van);
				g.drawString(font, CFHGuiDraw.truncate(Component.literal(work.id + "/" + q.id), w - 28 - 20), x + 26, y + 13, dim, van);
				break;
			case SEVERAL:
				head(g, Icon.SELECTION, Tr.t("subject.several", this.s.selection.size()), Tr.t("subject.several.sub"), x, y, w, van);
				break;
			case DECORATION:
				int i = this.s.selection.decoration();
				boolean image = work != null && i >= 0 && i < work.decorations.size() && "image".equals(work.decorations.get(i).type);
				head(g, image ? Icon.IMAGE : Icon.TEXT, Tr.t(image ? "subject.image" : "subject.label"), Tr.t("subject.decoration.sub"), x, y, w, van);
				break;
			case LINE:
				head(g, Icon.LINK, Tr.t("subject.line"), Tr.t("subject.line.sub"), x, y, w, van);
			case CHAPTER:
			default:
				break;
			case BOOK:
				if (work == null) {
					head(g, Icon.QUEST, Tr.t("subject.book"), Tr.t("subject.book.sub"), x, y, w, van);
				}
		}
	}

	private static void head(GuiGraphics g, Icon icon, Component title, Component sub, int x, int y, int w, boolean van) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Font font = CFHGuiDraw.font();
		Icons.draw(g, icon, x + 2, y + 4, 16, van ? -1 : t.accentInk());
		g.drawString(font, CFHGuiDraw.truncate(Fonts.heading(title), w - 26), x + 26, y + 2, van ? -1 : p.text(), van);
		g.drawString(font, CFHGuiDraw.truncate(sub, w - 26), x + 26, y + 13, van ? -6250336 : p.textDim(), van);
	}

	private void addMenu(AbstractWidget from) {
		List<Item> items = new ArrayList<>();
		QuestCanvas canvas = this.s.canvas();
		if (this.s.work() != null && canvas != null) {
			Rect free = canvas.free();
			float[] at = this.s.tool.place(canvas.tx((double)free.centerX()), canvas.ty((double)free.centerY()));
			items.add(Item.of(Tr.t("menu.new_quest"), Icon.PLUS, () -> this.s.newQuest(at[0], at[1], null)));
			items.add(Item.of(Tr.t("menu.new_label"), Icon.TEXT, () -> this.s.newDecoration(false, at[0], at[1])));
			items.add(Item.of(Tr.t("menu.new_image"), Icon.IMAGE, () -> this.s.newDecoration(true, at[0], at[1])));
			if (this.s.canPaste()) {
				items.add(Item.of(Tr.t("menu.paste"), Icon.COPY, () -> this.s.paste(at[0], at[1])));
			}

			items.add(Item.sep());
		}

		items.add(Item.of(Tr.t("chapter.new"), Icon.QUEST, () -> ChapterForm.newChapter(this.s)));
		Popups.open(new MenuPopup(from.getX(), from.getY() + from.getHeight() + 2, items, 130));
	}

	private void renameQuest() {
		Chapter work = this.s.work();
		String id = this.s.selection.quest();
		if (work != null && id != null) {
			new Prompt(
					Tr.t("quest.rename.title"),
					Icon.EDIT,
					Tr.t("quest.rename.body"),
					id,
					Tr.t("rename.ok"),
					typed -> ChapterForm.wrongId(typed, id, other -> this.s.work() != null && this.s.work().quest(other) != null),
					to -> this.s.renameQuest(id, to)
				)
				.show();
		}
	}

	private final class Root extends AbstractContainerWidget {
		private final Rect plate;
		private final List<AbstractWidget> children = new ArrayList<>();
		private final List<AbstractWidget> tools = new ArrayList<>();
		private final List<AbstractWidget> heads = new ArrayList<>();
		final EditorDialog.Body body = new EditorDialog.Body();
		final CFHGuiIconButton undo;
		final CFHGuiIconButton redo;
		final CFHGuiIconButton snap;

		Root(final Rect plate) {
			super(plate.x() + 8, plate.y() + 8, Math.max(1, plate.w() - 16), Math.max(1, plate.h() - 16), Tr.t("panel"));
			this.plate = plate;
			boolean rail = EditSession.collapsed;
			this.undo = this.tool(Icon.UNDO, "tool.undo", EditorPanel.this.s::undo);
			this.redo = this.tool(Icon.REDO, "tool.redo", EditorPanel.this.s::redo);
			this.snap = this.tool(Icon.SNAP, "tool.snap", () -> {
				EditSession.snapping = !EditSession.snapping;
				EditorPanel.this.s.notice(Tr.t(EditSession.snapping ? "snap.on" : "snap.off"));
			});
			CFHGuiIconButton[] add = new CFHGuiIconButton[1];
			add[0] = this.tool(Icon.PLUS, "tool.add", () -> EditorPanel.this.addMenu(add[0]));
			CFHGuiIconButton fold = this.tool(
				rail ? Icon.CHEVRON_LEFT : Icon.CHEVRON_RIGHT, rail ? "tool.unfold" : "tool.fold", () -> EditorPanel.this.fold(!EditSession.collapsed)
			);
			if (!rail) {
				int x = plate.x() + 6;

				for (AbstractWidget w : List.of(this.undo, this.redo, this.snap, add[0])) {
					w.setX(x);
					w.setY(plate.y() + 6);
					x += 18 + (w == this.redo ? 7 : 2);
				}

				fold.setX(plate.right() - 6 - 18);
				fold.setY(plate.y() + 6);
				int top = plate.y() + 28 + 26 + 2;
				this.body.setX(plate.x() + 8);
				this.body.setY(top);
				this.body.setWidth(plate.w() - 12);
				this.body.setHeight(Math.max(20, plate.bottom() - 5 - top));
				this.body.edgeFades(true);
				this.children.add(this.body);
			} else {
				int y = plate.y() + 7;

				for (AbstractWidget w : List.of(fold, this.undo, this.redo, this.snap, add[0])) {
					w.setX(plate.x() + (plate.w() - 18) / 2);
					w.setY(y);
					y += 20;
				}

				this.body.visible = false;
			}
		}

		private CFHGuiIconButton tool(Icon icon, String name, Runnable press) {
			CFHGuiIconButton b = new CFHGuiIconButton(0, 0, 18, icon, Tr.t(name), press);
			this.children.add(b);
			this.tools.add(b);
			return b;
		}

		void subject(EditorPanel.Scope scope) {
			this.children.removeAll(this.heads);
			this.heads.clear();
			int x = this.plate.x() + 8;
			int y = this.plate.y() + 28;
			int w = this.plate.w() - 16;
			if ((scope == EditorPanel.Scope.CHAPTER || scope == EditorPanel.Scope.BOOK) && EditorPanel.this.s.work() != null) {
				CFHGuiSegmented<Boolean> tab = new CFHGuiSegmented<>(
					x, y + 3, w, List.of(Boolean.FALSE, Boolean.TRUE), EditSession.bookTab, book -> Tr.t(book ? "tab.book" : "tab.chapter"), book -> {
						EditorPanel.this.flush();
						EditSession.bookTab = book;
					}
				);
				tab.setHeight(18);
				tab.optionTip(book -> Tr.t(book ? "tab.book.tip" : "tab.chapter.tip"));
				this.heads.add(tab);
			} else if (scope == EditorPanel.Scope.QUEST) {
				CFHGuiIconButton rename = new CFHGuiIconButton(x + w - 18, y + 3, 18, Icon.EDIT, Tr.t("quest.rename"), EditorPanel.this::renameQuest);
				rename.iconSize(8);
				this.heads.add(rename);
			}

			int at = this.children.indexOf(this.body);
			this.children.addAll(at < 0 ? this.children.size() : at, this.heads);
		}

		public List<? extends GuiEventListener> children() {
			return this.children;
		}

		@Nullable
		AbstractWidget field() {
			return this.getFocused() == this.body && this.body.getFocused() instanceof AbstractWidget w ? w : null;
		}

		void focusField(@Nullable AbstractWidget widget, boolean select) {
			if (widget != null) {
				EditorPanel.this.s.screen.setFocused(this);
				this.setFocused(this.body);
				this.body.setFocused(widget);
				if (select && widget instanceof EditField e) {
					e.moveCursorToEnd(false);
					e.setHighlightPos(0);
				}
			}
		}

		private List<AbstractWidget> stops() {
			List<AbstractWidget> out = new ArrayList<>();

			for (AbstractWidget w : this.tools) {
				if (w.visible && w.active) {
					out.add(w);
				}
			}

			for (AbstractWidget wx : this.heads) {
				if (wx.visible && wx.active) {
					out.add(wx);
				}
			}

			if (EditorPanel.this.form != null && this.body.visible) {
				for (AbstractWidget wxx : EditorPanel.this.form.stops()) {
					if (wxx.visible && wxx.active) {
						out.add(wxx);
					}
				}
			}

			return out;
		}

		private void step(int direction) {
			List<AbstractWidget> stops = this.stops();
			if (!stops.isEmpty()) {
				AbstractWidget at = this.getFocused() == this.body ? this.field() : (this.getFocused() instanceof AbstractWidget w ? w : null);
				int i = at == null ? (direction > 0 ? -1 : 0) : stops.indexOf(at);
				i = Math.floorMod(i + direction, stops.size());
				AbstractWidget to = stops.get(i);
				if (EditorPanel.this.form != null && EditorPanel.this.form.stops().contains(to)) {
					this.focusField(to, true);
				} else {
					this.body.setFocused((GuiEventListener)null);
					this.setFocused(to);
				}
			}
		}

		boolean key(int keyCode, int scanCode, int modifiers) {
			GuiEventListener at = WidgetPopup.deep(this.getFocused());
			boolean typing = WidgetPopup.typing(this.getFocused());
			if (keyCode == 256) {
				if (at == null) {
					return false;
				} else {
					if (at instanceof EditField e) {
						e.revert();
					} else if (at instanceof TextArea a) {
						a.revert();
					}

					this.setFocused((GuiEventListener)null);
					return true;
				}
			} else if (keyCode == 258) {
				this.step((modifiers & 1) != 0 ? -1 : 1);
				return true;
			} else if (typing) {
				boolean ctrl = (modifiers & (Minecraft.ON_OSX ? 8 : 2)) != 0;
				if (ctrl && (keyCode == 90 || keyCode == 89)) {
					boolean var10000;
					label126: {
						if (at instanceof EditField e && e.dirty() || at instanceof TextArea a && a.dirty()) {
							var10000 = true;
							break label126;
						}

						var10000 = false;
					}

					boolean dirty = var10000;
					if (!dirty) {
						return false;
					} else {
						if (keyCode == 90 && (modifiers & 1) == 0) {
							if (at instanceof EditField e) {
								e.revert();
							} else if (at instanceof TextArea a) {
								a.revert();
							}
						}

						return true;
					}
				} else if (ctrl && keyCode == 69) {
					return false;
				} else {
					if (at != null) {
						at.keyPressed(keyCode, scanCode, modifiers);
					}

					return true;
				}
			} else if (at != null && at.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			} else if (at != null && (modifiers & 6) == 0 && (keyCode == 264 || keyCode == 265)) {
				this.step(keyCode == 264 ? 1 : -1);
				return true;
			} else {
				return false;
			}
		}

		public void setFocused(boolean focused) {
			if (!focused) {
				this.setFocused((GuiEventListener)null);
			}
		}

		public boolean isMouseOver(double mouseX, double mouseY) {
			return this.plate.contains(mouseX, mouseY);
		}

		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!this.plate.contains(mouseX, mouseY)) {
				return false;
			} else {
				boolean taken = false;

				for (AbstractWidget child : new ArrayList<>(this.children)) {
					if (child.visible && child.mouseClicked(mouseX, mouseY, button)) {
						this.setFocused(child);
						if (button == 0) {
							this.setDragging(true);
						}

						taken = true;
						break;
					}
				}

				if (!taken) {
					this.setFocused((GuiEventListener)null);
				}

				return true;
			}
		}

		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			if (!this.plate.contains(mouseX, mouseY)) {
				return false;
			} else {
				if (this.body.visible && this.body.isMouseOver(mouseX, mouseY)) {
					this.body.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
				}

				return true;
			}
		}

		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			int slide = EditorPanel.this.s.screen.sidePanelOffset();
			if (slide != 0) {
				g.enableScissor(this.plate.x(), this.plate.y(), this.plate.right(), this.plate.bottom());
				g.pose().pushPose();
				g.pose().translate((float)slide, 0.0F, 0.0F);
			}

			Theme.enterPanel();

			try {
				for (AbstractWidget child : this.children) {
					if (child.visible) {
						child.render(g, mouseX - slide, mouseY, partialTick);
					}
				}
			} finally {
				Theme.exitPanel();
				if (slide != 0) {
					g.pose().popPose();
					g.disableScissor();
				}
			}
		}

		protected void updateWidgetNarration(NarrationElementOutput out) {
		}
	}

	private static enum Scope {
		QUEST,
		SEVERAL,
		DECORATION,
		LINE,
		CHAPTER,
		BOOK;
	}
}
