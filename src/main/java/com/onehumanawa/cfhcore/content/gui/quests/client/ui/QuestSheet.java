package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichText;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.RichTextView;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.Tips;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.BookStats;
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
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class QuestSheet extends AbstractContainerWidget implements RichText.Host {
	private static final int HEAD = 52;
	private static final int PAD = 8;
	private final QuestSheet.Host host;
	private final CFHGuiScrollPanel body = new CFHGuiScrollPanel(0, 0, 10, 10);
	private final CFHGuiIconButton close;
	private final CFHGuiIconButton pin;
	private final List<GuiEventListener> children = new ArrayList<>();
	private final Anim open = new Anim(0.0F, 240, Ease.OUT_CUBIC);
	private final Anim swap = new Anim(1.0F, 180, Ease.OUT_CUBIC);
	private boolean wanted;
	@Nullable
	private String key;
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
	private Component subtitle = Component.empty();
	private Component standing = Component.empty();
	private long restingUntil;
	private long restingSecond = -1L;
	private Component resting = Component.empty();
	private int accent;
	private final Anim progress = new Anim(0.0F, 480, Ease.OUT_CUBIC);
	@Nullable
	private CFHGuiButton folder;
	private boolean folderUnfolded;

	public QuestSheet(QuestSheet.Host host) {
		super(0, 0, 10, 10, Component.translatable("cfhgui_quests.sheet"));
		this.host = host;
		this.close = new CFHGuiIconButton(0, 0, 16, Icon.CLOSE, Component.translatable("cfhgui_quests.sheet.close"), host::closeSheet);
		this.pin = new CFHGuiIconButton(0, 0, 16, Icon.PIN, Component.translatable("cfhgui_quests.pin"), this::togglePin);
		this.children.add(this.pin);
		this.children.add(this.close);
		this.children.add(this.body);
		this.body.edgeFades(true);
	}

	public static int widthFor(int treeWidth) {
		return Mth.clamp(Math.round((float)treeWidth * 0.44F), 172, 240);
	}

	public void place(Rect tree) {
		this.clip = tree;
		this.aw = Math.min(widthFor(tree.w()), Math.max(120, tree.w() - 40));
		this.ah = tree.h();
		this.ax = tree.right() - this.aw;
		this.ay = tree.y();
		this.setX(this.ax);
		this.setY(this.ay);
		this.setWidth(this.aw);
		this.setHeight(this.ah);
		this.body.setWidth(this.aw - 8 - 3);
		this.body.setHeight(this.ah - 52 - 4);
		if (this.key != null) {
			this.build(false);
		}
	}

	public void show(String questKey) {
		boolean other = !questKey.equals(this.key);
		this.key = questKey;
		if (other) {
			this.shown.clear();
			this.body.snapScroll(0.0);
		}

		if (!this.wanted) {
			this.wanted = true;
			this.open.set(1.0F);
		} else if (other) {
			this.swap.snap(0.0F);
			this.swap.set(1.0F);
		}

		this.build(true);
	}

	public void refresh() {
		if (this.key != null) {
			if (QuestsState.entry(this.key) == null) {
				this.host.closeSheet();
			} else {
				this.build(false);
			}
		}
	}

	public void close() {
		if (this.wanted) {
			this.wanted = false;
			this.open.set(0.0F);
			this.setFocused((GuiEventListener)null);
		}
	}

	public boolean isOpen() {
		return this.wanted;
	}

	public boolean showing() {
		return this.wanted || this.open.get() > 0.01F;
	}

	public int cover() {
		return this.wanted ? this.aw : 0;
	}

	public int openWidth() {
		return this.aw;
	}

	@Nullable
	public String key() {
		return this.wanted ? this.key : null;
	}

	void scrollToEnd() {
		this.body.snapScroll((double)this.body.maxScroll());
	}

	private void togglePin() {
		if (this.key != null) {
			QuestsState.pin(this.key, !QuestsState.pinned(this.key));
		}
	}

	private void build(boolean entrance) {
		double keep = this.body.scrollAmount();
		this.body.clear();
		this.folder = null;
		this.entry = QuestsState.entry(this.key);
		if (this.entry != null) {
			BookIndex.Entry e = this.entry;
			Quest q = e.quest();
			String k = e.key();
			QuestState state = QuestsState.state(k);
			this.status = QuestsState.status(k);
			this.accent = QuestPainter.accentOf(e.chapter());
			if (entrance) {
				this.progress.snap(0.0F);
			}

			this.progress.set(this.status.done() ? 1.0F : QuestsState.fraction(k));
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			boolean secret = this.status == Rules.Status.LOCKED && q.visibility == Quest.Visibility.SECRET;
			this.title = secret ? QuestText.secret() : QuestText.title(e);
			this.subtitle = secret ? Component.empty() : QuestText.text(q.subtitle);
			this.standing = QuestText.status(this.status);
			this.restingUntil = q.repeatable && state.readyAt > System.currentTimeMillis() ? state.readyAt : 0L;
			this.restingSecond = -1L;
			boolean pinned = QuestsState.pinned(k);
			this.pin.toggled(pinned);
			this.pin.tip(Component.translatable(pinned ? "cfhgui_quests.unpin" : "cfhgui_quests.pin"));
			this.pin.visible = !secret && (pinned || !this.status.done());
			int w = this.body.innerWidth() - 2;
			int y = 2;
			if (secret) {
				y = this.add(new SheetParts.Fact(w, Icon.LOCK, null, 0, () -> Component.translatable("cfhgui_quests.card.secret")), 0, y) + 6;
				y = this.links(e, w, y);
				this.finish(y, keep, entrance);
			} else {
				List<SheetParts.Tags.Tag> tags = SheetParts.tags();
				int quiet = van ? -6250336 : p.textMuted();
				boolean team = e.sync() == Quest.Sync.TEAM;
				tags.add(
					new SheetParts.Tags.Tag(
						QuestText.sync(e),
						team ? (van ? -8406785 : -10770208) : quiet,
						Component.translatable(team ? "cfhgui_quests.sheet.sync.team" : "cfhgui_quests.sheet.sync.player")
					)
				);
				if (q.isAdvancement()) {
					tags.add(
						new SheetParts.Tags.Tag(
							Component.translatable("cfhgui_quests.sheet.frame." + q.frame.name().toLowerCase(Locale.ROOT)),
							q.frame == Quest.Frame.CHALLENGE ? -3115319 : (van ? -171 : -2055080),
							Component.translatable("cfhgui_quests.sheet.advancement")
						)
					);
				}

				if (q.optional) {
					tags.add(new SheetParts.Tags.Tag(Component.translatable("cfhgui_quests.sheet.optional"), quiet, Component.translatable("cfhgui_quests.sheet.optional.tip")));
				}

				if (q.repeatable) {
					tags.add(
						new SheetParts.Tags.Tag(
							Component.translatable("cfhgui_quests.sheet.repeatable"),
							van ? -11141291 : p.success(),
							q.cooldownSeconds > 0
								? Component.translatable("cfhgui_quests.sheet.repeatable.rest", new Object[]{QuestText.span((long)q.cooldownSeconds * 1000L)})
								: Component.translatable("cfhgui_quests.sheet.repeatable.tip")
						)
					);
				}

				if (q.autoClaim && !q.rewards.isEmpty() && !q.isAdvancement()) {
					tags.add(
						new SheetParts.Tags.Tag(Component.translatable("cfhgui_quests.sheet.auto_claim"), quiet, Component.translatable("cfhgui_quests.sheet.auto_claim.tip"))
					);
				}

				if (q.sequential && q.tasks.size() > 1) {
					tags.add(
						new SheetParts.Tags.Tag(Component.translatable("cfhgui_quests.sheet.sequential"), quiet, Component.translatable("cfhgui_quests.sheet.sequential.tip"))
					);
				}

				y = this.add(new SheetParts.Tags(w, tags), 0, y) + 6;
				if (q.hideDetails && this.status == Rules.Status.LOCKED && !QuestsState.canEdit()) {
					y = this.add(new SheetParts.Fact(w, Icon.LOCK, null, 0, () -> Component.translatable("cfhgui_quests.sheet.details_hidden")), 0, y) + 6;
					y = this.links(e, w, y);
					this.finish(y, keep, entrance);
				} else {
					if (!q.description.isEmpty() && q.hideText && !this.status.done() && !QuestsState.canEdit()) {
						y = this.add(new SheetParts.Fact(w, Icon.EYE, null, 0, () -> Component.translatable("cfhgui_quests.sheet.text_hidden")), 0, y) + 6;
					} else if (!q.description.isEmpty()) {
						RichText text = RichText.of(q.description);
						if (!text.isEmpty()) {
							y = this.add(new RichTextView(0, 0, w, text, this), 0, y) + 8;
						}
					}

					if (!q.guide.isBlank()) {
						boolean there = GuidePages.available();
						CFHGuiButton guide = new CFHGuiButton(0, 0, w, 20, Component.translatable("cfhgui_quests.sheet.guide"), () -> GuidePages.open(q.guide))
							.variant(Variant.SECONDARY)
							.icon(Icon.BOOKMARK);
						guide.active = there;
						guide.tip(
							there
								? Component.translatable("cfhgui_quests.sheet.guide.tip", new Object[]{GuidePages.words(q.guide)})
								: Component.translatable("cfhgui_quests.sheet.guide.missing", new Object[]{GuidePages.words(q.guide)})
						);
						y = this.add(guide, 0, y) + 8;
					}

					QuestBook book = QuestsState.book();
					Chapter sub = q.isGateway() && book != null ? book.chapter(q.subtree) : null;
					if (sub != null && this.host.opensInPlace(k)) {
						Runnable toggle = () -> this.host.unfold(k, !this.host.unfolded(k));
						y = this.add(new SheetParts.TreeCard(w, sub, true, toggle), 0, y) + 4;
						this.folder = new CFHGuiButton(0, 0, w, 20, Component.empty(), toggle);
						this.folderUnfolded = !this.host.unfolded(k);
						this.folder();
						y = this.add(this.folder, 0, y) + 8;
					} else if (sub != null) {
						boolean subOpen = BookStats.open(sub);
						y = this.add(new SheetParts.TreeCard(w, sub, subOpen, () -> this.host.enterTree(k)), 0, y) + 4;
						CFHGuiButton into = new CFHGuiButton(0, 0, w, 20, Component.translatable("cfhgui_quests.open_tree"), () -> this.host.enterTree(k))
							.variant(Variant.PRIMARY)
							.icon(Icon.COMPASS);
						into.active = subOpen;
						if (!subOpen) {
							into.tip(Component.translatable("cfhgui_quests.sheet.tree_locked"));
						}

						y = this.add(into, 0, y) + 8;
					}

					if (!q.tasks.isEmpty()) {
						int done = 0;

						for (int i = 0; i < q.tasks.size(); i++) {
							if (this.status.done() || Rules.taskDone(q.tasks.get(i), state.task(i))) {
								done++;
							}
						}

						Component note = q.anyTask && q.tasks.size() > 1
							? Component.translatable("cfhgui_quests.sheet.any_task")
							: Component.literal(QuestText.count(done, q.tasks.size()));
						y = this.add(new SheetParts.Caption(w, Component.translatable("cfhgui_quests.tasks"), note), 0, y) + 3;

						for (int ixx = 0; ixx < q.tasks.size(); ixx++) {
							final int ix = ixx;
							Task task = q.tasks.get(ixx);
							long counted = this.status.done() ? task.count : state.task(ixx);
							boolean taskDone = Rules.taskDone(task, counted);
							boolean waits = !taskDone && q.sequential && !this.status.done() && !Rules.taskOpen(q, state.tasks, ixx);
							boolean act = task.manual() && !taskDone && this.status.open() && !waits;
							float fraction = Rules.taskFraction(task, counted);
							float from = this.shown.getOrDefault(k + "#" + ixx, entrance ? 0.0F : fraction);
							this.shown.put(k + "#" + ixx, fraction);
							Component what = QuestText.note(q, task, ixx, state);
							Component label = (Component)(what != null && !taskDone
								? Component.empty().append(QuestText.label(task)).append(" ").append(what.copy().withStyle(s -> s.withColor(quiet & 16777215)))
								: QuestText.label(task));
							SheetParts.TaskRow row = new SheetParts.TaskRow(
								w,
								QuestIcons.of(task),
								label,
								QuestText.count(task, counted).getString(),
								from,
								fraction,
								taskDone,
								this.status == Rules.Status.LOCKED || waits,
								act ? 58 : 0,
								taskTip(task)
							);
							this.add(row, 0, y);
							if (act) {
								CFHGuiButton b = new CFHGuiButton(
										0,
										0,
										54,
										16,
										Component.translatable(task.is("checkmark") ? "cfhgui_quests.task.tick" : "cfhgui_quests.task.submit"),
										() -> QuestActions.submit(k, ix, null)
									)
									.variant(Variant.PRIMARY);
								b.tip(Component.translatable(task.is("checkmark") ? "cfhgui_quests.task.tick.tip" : "cfhgui_quests.task.submit.tip"));
								this.add(b, w - 54, y + 3);
							}

							y += row.getHeight() + 2;
						}

						y += 6;
					}

					if (!q.rewards.isEmpty()) {
						boolean claims = this.status.done() && !q.autoClaim && !q.isAdvancement();
						int waiting = 0;
						boolean choice = false;
						boolean listed = false;

						for (int ix = 0; ix < q.rewards.size(); ix++) {
							Reward r = q.rewards.get(ix);
							if (!r.hidden || QuestsState.canEdit()) {
								listed = true;
							}

							if (claims && !state.claimed(ix) && !r.atOnce()) {
								waiting++;
								if (r.is("choice")) {
									choice = true;
								}
							}
						}

						Component note = !this.status.done()
							? null
							: (
								waiting > 0
									? Component.translatable("cfhgui_quests.sheet.rewards.waiting", new Object[]{waiting})
									: Component.translatable("cfhgui_quests.sheet.rewards.claimed")
							);
						if (listed) {
							y = this.add(new SheetParts.Caption(w, Component.translatable("cfhgui_quests.rewards"), note), 0, y) + 3;
						}

						for (int ix = 0; ix < q.rewards.size(); ix++) {
							Reward reward = q.rewards.get(ix);
							if (!reward.hidden || QuestsState.canEdit()) {
								int index = ix;
								boolean claimed = this.status.done() && (!claims || state.claimed(ix) || reward.atOnce());
								boolean canClaim = claims && !state.claimed(ix) && !reward.atOnce();
								Component mark = reward.team
									? Component.translatable("cfhgui_quests.sheet.reward.team")
									: (
										reward.hidden
											? Component.translatable("cfhgui_quests.sheet.reward.hidden")
											: (reward.auto && !q.autoClaim ? Component.translatable("cfhgui_quests.sheet.reward.auto") : null)
									);
								SheetParts.RewardRow row = new SheetParts.RewardRow(
									w, QuestIcons.of(reward), QuestText.label(reward), mark, claimed, canClaim, canClaim ? 54 : 0, () -> rewardTip(reward)
								);
								this.add(row, 0, y);
								if (canClaim) {
									boolean pick = reward.is("choice");
									CFHGuiButton[] holder = new CFHGuiButton[1];
									holder[0] = new CFHGuiButton(
											0,
											0,
											50,
											16,
											Component.translatable(pick ? "cfhgui_quests.sheet.choose" : "cfhgui_quests.sheet.claim"),
											() -> {
												if (pick) {
													Popups.open(
														new RewardChoicePopup(
															holder[0].getX() + holder[0].getWidth(), holder[0].getY() + holder[0].getHeight() + 2, reward, c -> QuestActions.claim(k, index, c, null)
														)
													);
												} else {
													QuestActions.claim(k, index, 0, null);
												}
											}
										)
										.variant(Variant.SECONDARY);
									this.add(holder[0], w - 50, y + 3);
								}

								y += row.getHeight() + 2;
							}
						}

						if (waiting >= 2 && !choice) {
							y = this.add(
									new CFHGuiButton(0, 0, w, 20, Component.translatable("cfhgui_quests.claim"), () -> QuestActions.claim(k, -1, 0, null))
										.variant(Variant.PRIMARY)
										.icon(Icon.STAR_FILLED),
									0,
									y + 2
								)
								+ 2;
						}

						y += 6;
					}

					y = this.links(e, w, y);
					if (this.status.done()) {
						String by = state.doneBy;
						long at = state.doneAt;
						Component who = by.isBlank()
							? Component.translatable("cfhgui_quests.sheet.done", new Object[]{QuestText.ago(at)})
							: Component.translatable("cfhgui_quests.sheet.done_by", new Object[]{by, QuestText.ago(at)});
						y = this.add(new SheetParts.Fact(w, by.isBlank() ? Icon.CHECK : null, by.isBlank() ? null : face(by), 0, () -> who), 0, y) + 2;
					}

					if (q.repeatable) {
						if (state.times > 0) {
							y = this.add(
									new SheetParts.Fact(
										w,
										Icon.REFRESH,
										null,
										0,
										() -> Component.translatable(state.times == 1 ? "cfhgui_quests.sheet.times.one" : "cfhgui_quests.sheet.times", new Object[]{state.times})
									),
									0,
									y
								)
								+ 2;
						}

						if (state.readyAt > System.currentTimeMillis()) {
							y = this.add(
									new SheetParts.Fact(
										w,
										Icon.CLOCK,
										null,
										van ? -22016 : p.warning(),
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

	private void folder() {
		CFHGuiButton b = this.folder;
		String k = this.key;
		if (b != null && k != null) {
			boolean unfolded = this.host.unfolded(k);
			if (unfolded != this.folderUnfolded) {
				this.folderUnfolded = unfolded;
				b.setMessage(Component.translatable(unfolded ? "cfhgui_quests.inline.fold" : "cfhgui_quests.inline.open"));
				b.tip(Component.translatable(unfolded ? "cfhgui_quests.inline.fold.tip" : "cfhgui_quests.inline.open.tip"));
				b.variant(unfolded ? Variant.SECONDARY : Variant.PRIMARY).icon(unfolded ? Icon.MINUS : Icon.PLUS);
			}
		}
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
				y = this.add(new SheetParts.Caption(w, Component.translatable("cfhgui_quests.sheet.needs"), note), 0, y) + 3;
				y = this.chips(e.deps(), w, y) + 8;
			}

			if (this.status == Rules.Status.LOCKED || this.status == Rules.Status.HIDDEN) {
				List<String> opens = new ArrayList<>(BookStats.needs(e.chapter()));
				opens.removeAll(e.deps());
				if (!opens.isEmpty()) {
					y = this.add(
							new SheetParts.Caption(w, Component.translatable(e.chapter().isSubtree() ? "cfhgui_quests.sheet.tree_needs" : "cfhgui_quests.sheet.chapter_needs"), null),
							0,
							y
						)
						+ 3;
					y = this.chips(opens, w, y) + 8;
				}
			}

			List<String> next = new ArrayList<>();

			for (String d : index.dependents(e.key())) {
				if (QuestsState.status(d).visible()) {
					next.add(d);
				}
			}

			if (!next.isEmpty()) {
				y = this.add(new SheetParts.Caption(w, Component.translatable("cfhgui_quests.sheet.leads"), null), 0, y) + 3;
				y = this.chips(next, w, y) + 8;
			}

			return y;
		}
	}

	private int chips(List<String> keys, int w, int top) {
		List<SheetParts.QuestChip> chips = new ArrayList<>();
		int more = 0;

		for (String k : keys) {
			BookIndex.Entry e = QuestsState.entry(k);
			if (e != null) {
				if (chips.size() >= 12) {
					more++;
				} else {
					Rules.Status s = QuestsState.status(k);
					boolean secret = s == Rules.Status.HIDDEN || s == Rules.Status.LOCKED && e.quest().visibility == Quest.Visibility.SECRET;
					chips.add(
						new SheetParts.QuestChip(
							secret ? QuestText.secret() : QuestText.title(e), w, SheetParts.dot(s, QuestPainter.accentOf(e.chapter())), () -> this.host.goTo(k)
						)
					);
				}
			}
		}

		int y = SheetParts.flow(chips, w, top, (chip, cx, cy) -> this.body.add(chip, cx, cy));
		if (more > 0) {
			int n = more;
			y = this.add(new SheetParts.Fact(w, null, null, 0, () -> Component.translatable("cfhgui_quests.card.more", new Object[]{n})), 0, y + 3);
		}

		return y;
	}

	private int add(AbstractWidget widget, int x, int y) {
		this.body.add(widget, x, y);
		return y + widget.getHeight();
	}

	private void finish(int height, double keep, boolean entrance) {
		this.body.setContentHeight(height + 6);
		if (entrance) {
			int i = 3;

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
		return Tips.task(task, 10592151);
	}

	private static List<Component> rewardTip(Reward reward) {
		return Tips.reward(reward, 10592151);
	}

	private static ResourceLocation face(String name) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(name);
			if (info != null) {
				return info.getSkin().texture();
			}
		}

		return mc.player != null && mc.player.getGameProfile().getName().equals(name)
			? mc.player.getSkin().texture()
			: DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)).texture();
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

	public boolean holds(@Nullable GuiEventListener focus) {
		return focus == this || focus != null && (focus == this.pin || focus == this.close || focus == this.body || this.body.children().contains(focus));
	}

	private int slideX() {
		return this.ax + Math.round((1.0F - this.open.get()) * (float)(this.aw + 6));
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
			this.body.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
			return true;
		}
	}

	private void layout() {
		int x = this.slideX();
		this.setX(x);
		this.body.setX(x + 8);
		this.body.setY(this.ay + 52);
		this.close.setX(x + this.aw - 21);
		this.close.setY(this.ay + 5);
		this.pin.setX(x + this.aw - 39);
		this.pin.setY(this.ay + 5);
	}

	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (this.showing()) {
			this.layout();
			this.folder();
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			Font font = CFHGuiDraw.font();
			int x = this.slideX();
			int y = this.ay;
			int w = this.aw;
			int h = this.ah;
			float in = Mth.clamp(this.open.get() * 1.4F, 0.0F, 1.0F);
			g.enableScissor(this.clip.x(), this.clip.y(), this.clip.right(), this.clip.bottom());

			try {
				if (van) {
					g.fill(x, y, x + w, y + h, -15987700);
					CFHGuiDraw.vanillaDialog(g, x, y, w, h, 1.0F);
				} else {
					g.fill(x - 2, y + 3, x, y + h, Colors.scaleAlpha(p.shadow(), in));
					CFHGuiDraw.popupPlate(g, x, y, w, h, p.bg2(), p.borderStrong(), 0.0F, 1.0F);
					CFHGuiDraw.hline(g, x + t.radius() + 1, y + 1, w - 2 * t.radius() - 2, 352321535);
				}

				Theme.enterPanel();

				try {
					this.head(g, x, y, w, van);
					this.close.render(g, mouseX, mouseY, partialTick);
					if (this.pin.visible) {
						this.pin.render(g, mouseX, mouseY, partialTick);
					}

					this.body.render(g, mouseX, mouseY, partialTick);
				} finally {
					Theme.exitPanel();
				}
			} finally {
				g.disableScissor();
			}
		}
	}

	private void head(GuiGraphics g, int x, int y, int w, boolean van) {
		BookIndex.Entry e = this.entry;
		if (e != null) {
			Palette p = Theme.current().palette();
			Font font = CFHGuiDraw.font();
			float a = Mth.clamp(this.swap.get(), 0.0F, 1.0F);
			UiBits.medallion(g, e, this.status, this.accent, x + 27, y + 26, 1.36F, a);
			int tx = x + 50;
			int tw = w - 50 - 44;
			CFHGuiDraw.drawScrollingText(g, Fonts.heading(this.title), tx, y + 9, tw, Colors.scaleAlpha(van ? -1 : p.text(), Math.max(0.03F, a)), van);
			int ty = y + 21;
			if (!this.subtitle.getString().isEmpty()) {
				CFHGuiDraw.drawScrollingText(g, this.subtitle, tx, ty, w - 50 - 8, Colors.scaleAlpha(van ? -6250336 : p.textMuted(), Math.max(0.03F, a)), van);
				ty += 11;
			}

			int sc = UiBits.statusColor(this.status, this.accent);
			Component stands = this.standing;
			long left = this.restingUntil == 0L ? 0L : this.restingUntil - System.currentTimeMillis();
			if (left > 0L) {
				if (left / 1000L != this.restingSecond) {
					this.restingSecond = left / 1000L;
					this.resting = Component.translatable("cfhgui_quests.sheet.resting", new Object[]{QuestText.span(left)});
				}

				stands = this.resting;
				sc = van ? -22016 : p.warning();
			}

			g.fill(tx, ty + 2, tx + 4, ty + 6, Colors.scaleAlpha(sc, a));
			g.drawString(font, CFHGuiDraw.truncate(stands, w - 50 - 8 - 8), tx + 8, ty, Colors.scaleAlpha(sc, Math.max(0.03F, a)), van);
			int ly = y + 52 - 5;
			g.fill(x + 8, ly, x + w - 8, ly + 1, van ? -13158601 : p.border());
			float frac = this.progress.get();
			if (frac > 0.0F) {
				g.fill(x + 8, ly, x + 8 + Math.round((float)(w - 16) * frac), ly + 1, Colors.scaleAlpha(sc, a));
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
	}

	public interface Host {
		void goTo(String var1);

		void enterTree(String var1);

		void closeSheet();

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
