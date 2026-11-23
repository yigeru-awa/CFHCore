package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class NodeView {
	public static final int APPEAR_MS = 300;
	public final BookIndex.Entry entry;
	public final String key;
	public final Chapter chapter;
	public final Quest quest;
	public float x;
	public float y;
	public float size;
	public Rules.Status status = Rules.Status.LOCKED;
	public float progress;
	public boolean pinned;
	public int depth;
	public int index;
	public boolean visible;
	public final Anim hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	public final Anim selected = new Anim(0.0F, 180, Ease.OUT_CUBIC);
	public final Anim shownProgress = new Anim(0.0F, 420, Ease.OUT_CUBIC);
	public final Anim lit = new Anim(1.0F, 220, Ease.OUT_CUBIC);
	private long appearAtMs;
	public long openedAtMs;
	public long doneAtMs;
	@Nullable
	private QuestIcons.Drawable icon;
	public float room = 3.0F;
	@Nullable
	private FormattedCharSequence caption;
	private int captionFor = -1;
	private int captionWidth;
	@Nullable
	public Inlay inlay;
	public float restX;
	public float restY;
	public boolean folds;
	public final Anim unfolded = new Anim(0.0F, 220, Ease.OUT_CUBIC);
	private int treeAccent;

	NodeView(BookIndex.Entry entry) {
		this.entry = entry;
		this.key = entry.key();
		this.chapter = entry.chapter();
		this.quest = entry.quest();
		this.x = this.restX = this.quest.x;
		this.y = this.restY = this.quest.y;
		this.size = this.quest.size;
	}

	public Quest.Shape shape() {
		return this.entry.shape();
	}

	public boolean gateway() {
		return this.quest.isGateway();
	}

	public boolean inline() {
		return this.quest.isInline();
	}

	public int treeAccent() {
		if (this.treeAccent == 0) {
			QuestBook book = QuestsState.book();
			this.treeAccent = QuestPainter.accentOf(book != null && this.gateway() ? book.chapter(this.quest.subtree) : null) | 0xFF000000;
		}

		return this.treeAccent;
	}

	public boolean secret() {
		return this.status == Rules.Status.LOCKED && this.quest.visibility == Quest.Visibility.SECRET;
	}

	public boolean locked() {
		return this.status == Rules.Status.LOCKED || this.status == Rules.Status.HIDDEN;
	}

	public boolean done() {
		return this.status.done();
	}

	public boolean advancement() {
		return this.quest.isAdvancement();
	}

	public QuestIcons.Drawable icon() {
		if (this.icon == null) {
			this.icon = QuestIcons.of(this.entry);
		}

		return this.icon;
	}

	public FormattedCharSequence caption(int maxWidth) {
		int bucket = Math.max(0, maxWidth) / 6 * 6;
		if (this.caption == null || bucket != this.captionFor) {
			this.captionFor = bucket;
			this.caption = CFHGuiDraw.truncate(QuestText.title(this.entry), bucket);
			this.captionWidth = CFHGuiDraw.width(this.caption);
		}

		return this.caption;
	}

	public int captionWidth() {
		return this.captionWidth;
	}

	public void appearIn(int delayMs) {
		float motion = Theme.current().motion();
		this.appearAtMs = motion <= 0.0F ? 0L : Clock.nowMs() + (long)Math.round((float)Math.max(0, delayMs) * motion);
	}

	public void appearNow() {
		this.appearAtMs = 0L;
	}

	public float appear() {
		if (this.appearAtMs == 0L) {
			return 1.0F;
		} else {
			float motion = Theme.current().motion();
			if (motion <= 0.0F) {
				this.appearAtMs = 0L;
				return 1.0F;
			} else {
				float t = (float)(Clock.nowMs() - this.appearAtMs) / (300.0F * motion);
				if (t >= 1.0F) {
					this.appearAtMs = 0L;
					return 1.0F;
				} else {
					return Math.max(0.0F, t);
				}
			}
		}
	}
}
