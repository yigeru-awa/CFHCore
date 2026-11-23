package com.onehumanawa.cfhcore.content.gui.quests.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichTextView;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.Tips;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.GuidePages;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.CanvasDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestPainter;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestState;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class QuestPage extends AbstractContainerWidget implements RichText.Host {
	private static final int BAR = 26;
	private static final int PAD = 6;
	private static final int FOOT_ROW = 22;
	private final QuestPage.Host host;
	private final CFHGuiScrollPanel body = new CFHGuiScrollPanel(0, 0, 10, 10);
	private final PageParts.Cross close;
	private final List<GuiEventListener> children = new ArrayList<>();
	private final List<List<Button>> foot = new ArrayList<>();
	private final Anim open = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private boolean wanted;
	@Nullable
	private String key;
	@Nullable
	private String about;
	private int ax;
	private int ay;
	private int aw;
	private int ah;
	private Rect clip = new Rect(0, 0, 0, 0);
	private final Map<String, Float> shown = new HashMap<>();
	@Nullable
	private BookIndex.Entry entry;
	private Rules.Status status = Rules.Status.LOCKED;
	private Component title = Component.empty();
	private int accent;
	private boolean secret;
	@Nullable
	private QuestIcons.Drawable headIcon;
	private Quest.Frame headFrame = Quest.Frame.TASK;
	private final Anim progress = new Anim(0.0F, 480, Ease.OUT_CUBIC);
	private boolean folder;
	private boolean folderUnfolded;

	QuestPage(QuestPage.Host host) {
		super(0, 0, 10, 10, Component.translatable("cfhgui_quests.sheet"));
		this.host = host;
		this.close = new PageParts.Cross(Component.translatable("cfhgui_quests.sheet.close"), host::closePage);
		this.body.edgeFades(false);
		this.body.scrollStep(20);
	}

	static int widthFor(int insideWidth) {
		return Mth.clamp(Math.round((float)insideWidth * 0.4F), 176, 226);
	}

	void place(Rect inside) {
		this.clip = inside;
		this.aw = Math.max(60, Math.min(widthFor(inside.w()), inside.w() - 36));
		this.ah = inside.h();
		this.ax = inside.right() - this.aw;
		this.ay = inside.y();
		this.setX(this.ax);
		this.setY(this.ay);
		this.setWidth(this.aw);
		this.setHeight(this.ah);
		if (this.key != null || this.about != null) {
			this.build(false);
		}
	}

	void show(String questKey) {
		boolean other = !questKey.equals(this.key);
		this.key = questKey;
		this.about = null;
		if (other) {
			this.shown.clear();
			this.body.snapScroll(0.0);
		}

		if (!this.wanted) {
			this.wanted = true;
			this.open.set(1.0F);
		}

		this.build(true);
	}

	void showChapter(String chapterId) {
		boolean other = this.key != null || !chapterId.equals(this.about);
		this.key = null;
		this.about = chapterId;
		if (other) {
			this.shown.clear();
			this.body.snapScroll(0.0);
		}

		if (!this.wanted) {
			this.wanted = true;
			this.open.set(1.0F);
		}

		this.build(true);
	}

	void refresh() {
		if (this.key != null) {
			if (QuestsState.entry(this.key) == null) {
				this.host.closePage();
				return;
			}

			this.build(false);
		} else if (this.about != null) {
			QuestBook book = QuestsState.book();
			if (book == null || book.chapter(this.about) == null) {
				this.host.closePage();
				return;
			}

			this.build(false);
		}
	}

	void close() {
		if (this.wanted) {
			this.wanted = false;
			this.open.set(0.0F);
			this.setFocused((GuiEventListener)null);
		}
	}

	boolean isOpen() {
		return this.wanted;
	}

	boolean showing() {
		return this.wanted || this.open.get() > 0.01F;
	}

	int cover() {
		return this.wanted ? this.aw : 0;
	}

	int openWidth() {
		return this.aw;
	}

	int left() {
		return this.showing() ? this.slideX() : Integer.MAX_VALUE;
	}

	@Nullable
	String key() {
		return this.wanted ? this.key : null;
	}

	@Nullable
	String aboutChapter() {
		return this.wanted && this.key == null ? this.about : null;
	}

	void scrollToEnd() {
		this.body.snapScroll((double)this.body.maxScroll());
	}

	private void build(boolean entrance) {
		double keep = this.body.scrollAmount();
		this.body.clear();
		this.foot.clear();
		this.children.clear();
		this.children.add(this.close);
		this.children.add(this.body);
		this.headIcon = null;
		this.entry = this.key == null ? null : QuestsState.entry(this.key);
		if (this.key == null) {
			this.chapter(entrance, keep);
		} else if (this.entry != null) {
			BookIndex.Entry e = this.entry;
			Quest q = e.quest();
			String k = e.key();
			QuestState state = QuestsState.state(k);
			this.status = QuestsState.status(k);
			this.accent = QuestPainter.accentOf(e.chapter());
			this.headIcon = QuestIcons.of(e);
			this.headFrame = q.frame;
			if (entrance) {
				this.progress.snap(0.0F);
			}

			this.progress.set(this.status.done() ? 1.0F : (this.status == Rules.Status.LOCKED ? 0.0F : QuestsState.fraction(k)));
			this.secret = this.status == Rules.Status.LOCKED && q.visibility == Quest.Visibility.SECRET;
			this.title = this.secret ? QuestText.secret() : QuestText.title(e);
			boolean game = AdvancementChapters.is(e.chapter());
			boolean pinned = QuestsState.pinned(k);
			Font font = CFHGuiDraw.font();
			QuestBook book = QuestsState.book();
			Chapter sub = q.isGateway() && book != null ? book.chapter(q.subtree) : null;
			boolean claims = this.status.done() && !q.autoClaim && !q.isAdvancement();
			int waiting = 0;
			boolean choice = false;

			for (int i = 0; i < q.rewards.size() && claims; i++) {
				if (!state.claimed(i) && !q.rewards.get(i).atOnce()) {
					waiting++;
					if (q.rewards.get(i).is("choice")) {
						choice = true;
					}
				}
			}

			this.folder = sub != null && !this.secret && this.host.opensInPlace(k);
			if (this.folder) {
				this.folderUnfolded = this.host.unfolded(k);
				Button fold = Button.builder(
						Component.translatable(this.folderUnfolded ? "cfhgui_quests.inline.fold" : "cfhgui_quests.inline.open"), bx -> this.host.unfold(k, !this.host.unfolded(k))
					)
					.size(100, 20)
					.build();
				fold.setTooltip(Tooltip.create(Component.translatable(this.folderUnfolded ? "cfhgui_quests.inline.fold.tip" : "cfhgui_quests.inline.open.tip")));
				this.foot.add(List.of(fold));
			} else if (sub != null && !this.secret) {
				boolean subOpen = BookStats.open(sub);
				Button into = Button.builder(Component.translatable("cfhgui_quests.open_tree"), bx -> this.host.enterTree(k)).size(100, 20).build();
				into.active = subOpen;
				if (!subOpen) {
					into.setTooltip(Tooltip.create(Component.translatable("cfhgui_quests.sheet.tree_locked")));
				}

				this.foot.add(List.of(into));
			}

			List<Button> row = new ArrayList<>();
			if (!this.secret && (pinned || !this.status.done())) {
				Button pin = Button.builder(
						Component.translatable(pinned ? "cfhgui_quests.unpin" : "cfhgui_quests.vanilla.pin"), bx -> QuestsState.pin(k, !QuestsState.pinned(k))
					)
					.size(60, 20)
					.build();
				pin.setTooltip(Tooltip.create(Component.translatable(pinned ? "cfhgui_quests.vanilla.unpin.tip" : "cfhgui_quests.vanilla.pin.tip")));
				row.add(pin);
			}

			if (waiting >= 1 && !choice) {
				row.add(
					Button.builder(Component.translatable(waiting > 1 ? "cfhgui_quests.claim" : "cfhgui_quests.vanilla.claim_one"), bx -> QuestActions.claim(k, -1, 0, null))
						.size(60, 20)
						.build()
				);
			}

			if (!row.isEmpty()) {
				this.foot.add(row);
			}

			for (List<Button> buttons : this.foot) {
				this.children.addAll(buttons);
			}

			this.body.setWidth(this.aw - 6 - 3);
			this.body.setHeight(Math.max(20, this.ah - 26 - 4 - this.footHeight() - 3));
			int w = this.body.innerWidth() - 2;
			int y = 1;
			if (this.secret) {
				y = this.add(new PageParts.Words(w, Component.translatable("cfhgui_quests.card.secret"), PageParts.Ink.MUTED, 4), 0, y) + 6;
				y = this.links(e, w, y);
				this.finish(y, keep, entrance);
			} else {
				String told = game ? AdvancementChapters.description(k) : "";
				Component about = !told.isEmpty() ? Component.literal(told) : (q.subtitle.isBlank() ? null : QuestText.text(q.subtitle));
				if (about != null) {
					y = this.add(new PageParts.Words(w, about, q.frame == Quest.Frame.CHALLENGE ? PageParts.Ink.SAID_CHALLENGE : PageParts.Ink.SAID, 8), 0, y) + 3;
				}

				Component standing = this.standing(e, q, state);
				y = this.add(
						new PageParts.Words(
							w,
							standing,
							this.status == Rules.Status.COMPLETE
								? PageParts.Ink.MUTED
								: (this.status == Rules.Status.CLAIMABLE ? PageParts.Ink.GOLD : (this.status == Rules.Status.LOCKED ? PageParts.Ink.DIM : PageParts.Ink.TEXT)),
							3
						),
						0,
						y
					)
					+ 5;
				if (q.hideDetails && this.status == Rules.Status.LOCKED && !QuestsState.canEdit()) {
					y = this.add(new PageParts.Words(w, Component.translatable("cfhgui_quests.sheet.details_hidden"), PageParts.Ink.DIM, 3), 0, y) + 6;
					y = this.links(e, w, y);
					this.finish(y, keep, entrance);
				} else {
					if (!q.description.isEmpty() && told.isEmpty() && q.hideText && !this.status.done() && !QuestsState.canEdit()) {
						y = this.add(new PageParts.Words(w, Component.translatable("cfhgui_quests.sheet.text_hidden"), PageParts.Ink.DIM, 3), 0, y) + 6;
					} else if (!q.description.isEmpty() && told.isEmpty()) {
						RichText text = RichText.of(q.description);
						if (!text.isEmpty()) {
							y = this.add(new RichTextView(0, 0, w, text, this), 0, y) + 7;
						}
					}

					if (!q.guide.isBlank()) {
						boolean there = GuidePages.available();
						String words = GuidePages.words(q.guide);
						Button guide = Button.builder(Component.translatable("cfhgui_quests.sheet.guide"), button -> GuidePages.open(q.guide)).size(Math.min(w, 150), 20).build();
						guide.active = there;
						guide.setTooltip(Tooltip.create(Component.translatable(there ? "cfhgui_quests.sheet.guide.tip" : "cfhgui_quests.sheet.guide.missing", new Object[]{words})));
						y = this.add(guide, 0, y) + 6;
					}

					if (sub != null) {
						BookStats.Stat stat = BookStats.of(sub);
						y = this.add(
								new PageParts.Words(
									w, Component.translatable("cfhgui_quests.vanilla.page.tree", new Object[]{QuestText.title(sub), stat.done(), stat.total()}), PageParts.Ink.MUTED, 2
								),
								0,
								y
							)
							+ 6;
					}

					if (!q.tasks.isEmpty() && (!game || q.tasks.size() != 1 || q.tasks.get(0).count > 1L)) {
						int done = 0;

						for (int ix = 0; ix < q.tasks.size(); ix++) {
							if (this.status.done() || Rules.taskDone(q.tasks.get(ix), state.task(ix))) {
								done++;
							}
						}

						Component note = q.anyTask && q.tasks.size() > 1
							? Component.translatable("cfhgui_quests.sheet.any_task")
							: Component.literal(QuestText.count(done, q.tasks.size()));
						y = this.add(new PageParts.Caption(w, Component.translatable("cfhgui_quests.tasks"), note), 0, y) + 2;

						for (int ixx = 0; ixx < q.tasks.size(); ixx++) {
							Task task = q.tasks.get(ixx);
							int index = ixx;
							long counted = this.status.done() ? task.count : state.task(ixx);
							boolean taskDone = Rules.taskDone(task, counted);
							boolean waits = !taskDone && q.sequential && !this.status.done() && !Rules.taskOpen(q, state.tasks, ixx);
							boolean act = task.manual() && !taskDone && this.status.open() && !waits;
							float fraction = Rules.taskFraction(task, counted);
							float from = this.shown.getOrDefault(k + "#" + ixx, entrance ? 0.0F : fraction);
							this.shown.put(k + "#" + ixx, fraction);
							Component label = Component.translatable(task.is("checkmark") ? "cfhgui_quests.task.tick" : "cfhgui_quests.task.submit");
							int buttonW = act ? Math.max(44, font.width(label) + 12) : 0;
							Component what = taskDone ? null : QuestText.note(q, task, ixx, state);
							Component named = (Component)(what == null
								? QuestText.label(task)
								: Component.empty().append(QuestText.label(task)).append(" ").append(what.copy().withStyle(s -> s.withColor(10526880))));
							PageParts.TaskRow taskRow = new PageParts.TaskRow(
								w,
								QuestIcons.of(task),
								named,
								QuestText.count(task, counted).getString(),
								from,
								fraction,
								taskDone,
								this.status == Rules.Status.LOCKED || waits,
								task.count > 1L && !game,
								act ? buttonW + 4 : 0,
								taskTip(task)
							);
							this.add(taskRow, 0, y);
							if (act) {
								Button b = Button.builder(label, button -> QuestActions.submit(k, index, null)).size(buttonW, 18).build();
								b.setTooltip(Tooltip.create(Component.translatable(task.is("checkmark") ? "cfhgui_quests.task.tick.tip" : "cfhgui_quests.task.submit.tip")));
								this.add(b, w - buttonW, y + 2);
							}

							y += taskRow.getHeight() + 2;
						}

						y += 5;
					}

					if (!q.rewards.isEmpty()) {
						Component note = !this.status.done()
							? null
							: (
								waiting > 0
									? Component.translatable("cfhgui_quests.sheet.rewards.waiting", new Object[]{waiting})
									: Component.translatable("cfhgui_quests.sheet.rewards.claimed")
							);
						y = this.add(new PageParts.Caption(w, Component.translatable("cfhgui_quests.rewards"), note), 0, y) + 2;
						Component claimLabel = Component.translatable("cfhgui_quests.sheet.claim");
						Component chooseLabel = Component.translatable("cfhgui_quests.sheet.choose");

						for (int ixx = 0; ixx < q.rewards.size(); ixx++) {
							Reward reward = q.rewards.get(ixx);
							if (!reward.hidden || QuestsState.canEdit()) {
								int index = ixx;
								boolean claimed = this.status.done() && (!claims || state.claimed(ixx) || reward.atOnce());
								boolean canClaim = claims && !state.claimed(ixx) && !reward.atOnce();
								boolean pick = reward.is("choice");
								int buttonW = canClaim && !pick ? Math.max(40, font.width(claimLabel) + 12) : 0;
								Component mark = reward.team
									? Component.translatable("cfhgui_quests.sheet.reward.team")
									: (
										reward.hidden
											? Component.translatable("cfhgui_quests.sheet.reward.hidden")
											: (reward.auto && !q.autoClaim ? Component.translatable("cfhgui_quests.sheet.reward.auto") : null)
									);
								PageParts.RewardRow rewardRow = new PageParts.RewardRow(
									w, QuestIcons.of(reward), QuestText.label(reward), mark, claimed, canClaim, buttonW > 0 ? buttonW + 4 : 0, 0, () -> rewardTip(reward)
								);
								this.add(rewardRow, 0, y);
								if (buttonW > 0) {
									this.add(Button.builder(claimLabel, button -> QuestActions.claim(k, index, 0, null)).size(buttonW, 18).build(), w - buttonW, y + 2);
								}

								y += rewardRow.getHeight() + 2;
								if (pick && !claimed) {
									int pickW = canClaim ? Math.max(44, font.width(chooseLabel) + 12) : 0;

									for (int c = 0; c < reward.choices.size(); c++) {
										Reward offered = reward.choices.get(c);
										int which = c;
										PageParts.RewardRow offer = new PageParts.RewardRow(
											w, QuestIcons.of(offered), QuestText.label(offered), null, false, false, pickW > 0 ? pickW + 4 : 0, 10, () -> rewardTip(offered)
										);
										this.add(offer, 0, y);
										if (pickW > 0) {
											this.add(Button.builder(chooseLabel, button -> QuestActions.claim(k, index, which, null)).size(pickW, 18).build(), w - pickW, y + 2);
										}

										y += offer.getHeight() + 2;
									}
								}
							}
						}

						y += 5;
					}

					y = this.links(e, w, y);
					if (this.status.done()) {
						String by = state.doneBy;
						long at = state.doneAt;
						if (at > 0L) {
							y = this.add(
									new PageParts.Fact(
										w,
										PageParts.Ink.MUTED,
										() -> !by.isBlank() && !game
												? Component.translatable("cfhgui_quests.sheet.done_by", new Object[]{by, QuestText.ago(at)})
												: Component.translatable("cfhgui_quests.sheet.done", new Object[]{QuestText.ago(at)})
									),
									0,
									y
								)
								+ 2;
						}
					}

					if (q.repeatable) {
						if (state.times > 0) {
							y = this.add(
									new PageParts.Fact(
										w,
										PageParts.Ink.MUTED,
										() -> Component.translatable(state.times == 1 ? "cfhgui_quests.sheet.times.one" : "cfhgui_quests.sheet.times", new Object[]{state.times})
									),
									0,
									y
								)
								+ 2;
						}

						if (state.readyAt > System.currentTimeMillis()) {
							y = this.add(
									new PageParts.Fact(
										w,
										PageParts.Ink.GOLD,
										() -> {
											long left = state.readyAt - System.currentTimeMillis();
											return left > 0L
												? Component.translatable("cfhgui_quests.sheet.resting", new Object[]{QuestText.span(left)})
												: Component.translatable("cfhgui_quests.sheet.ready");
										}
									),
									0,
									y
								)
								+ 2;
						}
					}

					this.finish(y, keep, entrance);
				}
			}
		}
	}

	private void chapter(boolean entrance, double keep) {
		QuestBook book = QuestsState.book();
		Chapter c = book == null ? null : book.chapter(this.about);
		if (c != null) {
			boolean isOpen = BookStats.open(c);
			BookStats.Stat own = BookStats.of(c);
			BookStats.Stat deep = BookStats.deep(c);
			this.status = !isOpen
				? Rules.Status.LOCKED
				: (own.complete() ? Rules.Status.COMPLETE : (own.done() > 0 ? Rules.Status.IN_PROGRESS : Rules.Status.AVAILABLE));
			this.accent = QuestPainter.accentOf(c);
			if (entrance) {
				this.progress.snap(0.0F);
			}

			this.progress.set(isOpen ? own.fraction() : 0.0F);
			this.secret = false;
			this.title = QuestText.title(c);
			this.headIcon = QuestIcons.of(c.icon);
			this.headFrame = Quest.Frame.TASK;
			this.body.setWidth(this.aw - 6 - 3);
			this.body.setHeight(Math.max(20, this.ah - 26 - 4 - 3));
			int w = this.body.innerWidth() - 2;
			int y = 1;
			if (!c.subtitle.isBlank()) {
				y = this.add(new PageParts.Words(w, QuestText.text(c.subtitle), PageParts.Ink.SAID, 4), 0, y) + 3;
			}

			if (!isOpen) {
				y = this.add(new PageParts.Words(w, Component.translatable("cfhgui_quests.rail.locked"), PageParts.Ink.GOLD, 3), 0, y) + 4;
				List<String> opens = new ArrayList<>(BookStats.needs(c));
				if (!opens.isEmpty()) {
					y = this.chips(opens, w, y) + 6;
				}
			} else if (own.total() > 0) {
				MutableComponent standing = Component.translatable("cfhgui_quests.rail.progress", new Object[]{own.done(), own.total()});
				int waiting = deep.claimable();
				if (waiting > 0) {
					standing.append("  ·  ")
						.append(Component.translatable(waiting == 1 ? "cfhgui_quests.rail.waiting.one" : "cfhgui_quests.rail.waiting", new Object[]{waiting}));
				}

				y = this.add(new PageParts.Words(w, standing, waiting > 0 ? PageParts.Ink.GOLD : (own.complete() ? PageParts.Ink.GOOD : PageParts.Ink.MUTED), 3), 0, y) + 5;
			}

			RichText text = RichText.of(c.description);
			if (!text.isEmpty()) {
				y = this.add(new RichTextView(0, 0, w, text, this), 0, y) + 7;
			}

			this.finish(y, keep, entrance);
		}
	}

	private Component standing(BookIndex.Entry e, Quest q, QuestState state) {
		MutableComponent out = Component.empty().append(QuestText.status(this.status));
		String dot = "  ·  ";
		if (q.isAdvancement()) {
			out.append("  ·  ").append(Component.translatable("cfhgui_quests.sheet.frame." + q.frame.name().toLowerCase(Locale.ROOT)));
		} else {
			out.append("  ·  ").append(QuestText.sync(e));
		}

		if (q.optional) {
			out.append("  ·  ").append(Component.translatable("cfhgui_quests.sheet.optional"));
		}

		if (q.repeatable) {
			out.append("  ·  ").append(Component.translatable("cfhgui_quests.sheet.repeatable"));
		}

		return out;
	}

	private int links(BookIndex.Entry e, int w, int y) {
		BookIndex index = QuestsState.index();
		if (index == null) {
			return y;
		} else {
			Quest q = e.quest();
			if (!e.deps().isEmpty()) {
				Component note = q.depMode == Quest.DepMode.ANY && e.deps().size() > 1
					? Component.translatable("cfhgui_quests.sheet.needs.any", new Object[]{Math.min(e.deps().size(), Math.max(1, q.minDeps)), e.deps().size()})
					: null;
				y = this.add(new PageParts.Caption(w, Component.translatable(q.looseDeps ? "cfhgui_quests.vanilla.page.after" : "cfhgui_quests.sheet.needs"), note), 0, y)
					+ 3;
				y = this.chips(e.deps(), w, y) + 6;
			}

			if (this.status == Rules.Status.LOCKED || this.status == Rules.Status.HIDDEN) {
				List<String> opens = new ArrayList<>(BookStats.needs(e.chapter()));
				opens.removeAll(e.deps());
				if (!opens.isEmpty()) {
					y = this.add(
							new PageParts.Caption(w, Component.translatable(e.chapter().isSubtree() ? "cfhgui_quests.sheet.tree_needs" : "cfhgui_quests.sheet.chapter_needs"), null),
							0,
							y
						)
						+ 3;
					y = this.chips(opens, w, y) + 6;
				}
			}

			List<String> next = new ArrayList<>();

			for (String d : index.dependents(e.key())) {
				if (QuestsState.status(d).visible()) {
					next.add(d);
				}
			}

			if (!next.isEmpty()) {
				y = this.add(new PageParts.Caption(w, Component.translatable("cfhgui_quests.sheet.leads"), null), 0, y) + 3;
				y = this.chips(next, w, y) + 6;
			}

			return y;
		}
	}

	private int chips(List<String> keys, int w, int top) {
		Font font = CFHGuiDraw.font();
		int x = 0;
		int y = top;
		int more = 0;
		int made = 0;

		for (String k : keys) {
			BookIndex.Entry e = QuestsState.entry(k);
			if (e != null) {
				if (made >= 12) {
					more++;
				} else {
					Rules.Status s = QuestsState.status(k);
					boolean hush = s == Rules.Status.HIDDEN || s == Rules.Status.LOCKED && e.quest().visibility == Quest.Visibility.SECRET;
					Component name = hush ? QuestText.secret() : QuestText.title(e);
					int tone = s == Rules.Status.COMPLETE
						? PageParts.Ink.GOOD.color()
						: (
							s == Rules.Status.CLAIMABLE
								? PageParts.Ink.GOLD.color()
								: (s != Rules.Status.LOCKED && s != Rules.Status.HIDDEN ? PageParts.Ink.TEXT.color() : PageParts.Ink.DIM.color())
						);
					Component label = Component.empty().append(Component.literal("■ ").withStyle(st -> st.withColor(tone & 16777215))).append(name);
					int bw = Math.min(w, font.width(label) + 10);
					if (x > 0 && x + bw > w) {
						x = 0;
						y += 18;
					}

					this.body.add(Button.builder(label, b -> this.host.goTo(k)).size(bw, 16).build(), x, y);
					x += bw + 3;
					made++;
				}
			}
		}

		y += made > 0 ? 16 : 0;
		if (more > 0) {
			int n = more;
			y = this.add(new PageParts.Fact(w, PageParts.Ink.DIM, () -> Component.translatable("cfhgui_quests.card.more", new Object[]{n})), 0, y + 3);
		}

		return y;
	}

	private int add(AbstractWidget widget, int x, int y) {
		this.body.add(widget, x, y);
		return y + widget.getHeight();
	}

	private void finish(int height, double keep, boolean entrance) {
		this.body.setContentHeight(height + 4);
		if (entrance) {
			int i = 2;

			for (GuiEventListener child : this.body.children()) {
				if (child instanceof Renderable r) {
					i = CFHGuiScreen.entrance(r, i);
				}
			}
		} else {
			this.body.snapScroll(keep);
		}
	}

	private static List<Component> taskTip(Task task) {
		return Tips.task(task, 10526880);
	}

	private static List<Component> rewardTip(Reward reward) {
		if (!reward.is("choice")) {
			return Tips.reward(reward, 10526880);
		} else {
			List<Component> out = new ArrayList<>();
			out.add(QuestText.label(reward));
			out.add(Component.translatable("cfhgui_quests.reward.how." + reward.type).withStyle(s -> s.withColor(10526880)));
			return out;
		}
	}

	@Override
	public void quest(String questKey) {
		this.host.goTo(questKey);
	}

	@Override
	public void url(String url) {
		try {
			URI uri = new URI(url.trim());
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!scheme.equals("http") && !scheme.equals("https")) {
				return;
			}

			ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, uri, false);
		} catch (Exception var4) {
		}
	}

	public List<? extends GuiEventListener> children() {
		return this.children;
	}

	boolean holds(@Nullable GuiEventListener focus) {
		return focus == this || focus != null && (this.children.contains(focus) || this.body.children().contains(focus));
	}

	private int footHeight() {
		return this.foot.isEmpty() ? 0 : this.foot.size() * 22 + 5;
	}

	private int slideX() {
		return this.ax + Math.round((1.0F - this.open.get()) * (float)(this.aw + 4));
	}

	private boolean within(double mx, double my) {
		return this.showing() && mx >= (double)this.slideX() && mx < (double)(this.ax + this.aw) && my >= (double)this.ay && my < (double)(this.ay + this.ah);
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.within(mouseX, mouseY);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.wanted && this.within(mouseX, mouseY)) {
			this.layout();
			if (!super.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused((GuiEventListener)null);
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!this.within(mouseX, mouseY)) {
			return false;
		} else {
			this.layout();
			this.body.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
			return true;
		}
	}

	private void layout() {
		int x = this.slideX();
		this.setX(x);
		this.close.setX(x + this.aw - 19);
		this.close.setY(this.ay + 6);
		this.body.setX(x + 6);
		this.body.setY(this.ay + 26 + 3);
		int by = this.ay + this.ah - this.footHeight() + 2;

		for (List<Button> row : this.foot) {
			int room = this.aw - 12;
			int gap = 4;
			int each = (room - 4 * (row.size() - 1)) / row.size();
			int bx = x + 6;

			for (Button b : row) {
				b.setWidth(each);
				b.setX(bx);
				b.setY(by);
				bx += each + 4;
			}

			by += 22;
		}
	}

	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (this.showing()) {
			if (this.folder && this.key != null && this.host.unfolded(this.key) != this.folderUnfolded) {
				this.build(false);
			}

			this.layout();
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			int x = this.slideX();
			g.enableScissor(this.clip.x(), this.clip.y(), this.clip.right(), this.clip.bottom());

			try {
				CanvasDraw.layer(g);
				if (van) {
					g.fill(x, this.ay, x + this.aw, this.ay + this.ah, -16777216);
					WindowArt.box(g, x, this.ay - 3, this.aw, this.ah + 6, 1.0F);
				} else {
					g.fill(x - 2, this.ay, x, this.ay + this.ah, t.palette().shadow());
					g.fill(x, this.ay, x + this.aw, this.ay + this.ah, t.chrome(t.palette().bg2()));
					CFHGuiDraw.outline(g, x, this.ay - 1, this.aw + 1, this.ah + 2, t.palette().borderStrong(), 0);
				}

				Theme.enterPanel();

				try {
					this.head(g, x, van);
					this.close.render(g, mouseX, mouseY, partialTick);
					this.body.render(g, mouseX, mouseY, partialTick);
					if (!this.foot.isEmpty()) {
						int line = this.ay + this.ah - this.footHeight() - 1;
						g.fill(x + 6, line, x + this.aw - 6, line + 1, van ? -11184811 : t.palette().border());

						for (List<Button> row : this.foot) {
							for (Button b : row) {
								b.render(g, mouseX, mouseY, partialTick);
							}
						}
					}
				} finally {
					Theme.exitPanel();
				}
			} finally {
				g.disableScissor();
			}
		}
	}

	private void head(GuiGraphics g, int x, boolean van) {
		QuestIcons.Drawable icon = this.headIcon;
		if (icon != null) {
			Theme t = Theme.current();
			boolean locked = this.status == Rules.Status.LOCKED || this.status == Rules.Status.HIDDEN;
			WindowArt.titleBar(g, x + 1, this.ay, this.aw - 2, this.progress.get(), this.status, this.accent, locked ? 0.6F : 1.0F, 1.0F);
			WindowArt.frame(g, this.headFrame, this.status, this.accent, x + 17, this.ay + 13, 13, locked ? 0.7F : 1.0F, 0.0F, 1.0F);
			if (this.secret) {
				g.drawString(CFHGuiDraw.font(), "?", x + 17 - CFHGuiDraw.font().width("?") / 2 + 1, this.ay + 9, van ? -12566464 : t.palette().textDim(), false);
			} else {
				icon.draw(g, (float)(x + 17), (float)(this.ay + 13), 16.0F, van ? -13949406 : t.palette().text());
			}

			Component name = (Component)(van ? this.title : Fonts.heading(this.title));
			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 200.0F);
			CFHGuiDraw.drawScrollingText(g, name, x + 33, this.ay + 9, this.aw - 33 - 23, Colors.withAlpha(van ? 16777215 : t.palette().text(), 255), true);
			g.pose().popPose();
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
	}

	interface Host {
		void goTo(String var1);

		void enterTree(String var1);

		void closePage();

		default boolean opensInPlace(String questKey) {
			return false;
		}

		default boolean unfolded(String questKey) {
			return false;
		}

		default void unfold(String questKey, boolean open) {
		}
	}
}
