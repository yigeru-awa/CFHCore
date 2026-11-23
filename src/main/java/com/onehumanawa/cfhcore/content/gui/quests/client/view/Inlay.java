package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class Inlay {
	public static final float PAD = 0.5F;
	public static final float HEAD = 0.75F;
	public static final float GAP = 0.3F;
	private static final int PLATE_MS = 260;
	private static final int FIRST_MS = 90;
	private static final int STEP_MS = 70;
	private static final int RISE_MS = 240;
	private static final int FOLD_TREE_MS = 140;
	private static final int FOLD_PLATE_MS = 230;
	public final NodeView owner;
	public final Chapter chapter;
	public final GraphModel graph;
	public final Component title;
	public final int level;
	final int base;
	public final List<LinkView> stems = new ArrayList<>();
	public boolean[] fromAbove = new boolean[0];
	public final List<LinkView> cross = new ArrayList<>();
	public float x;
	public float y;
	public float w;
	public float h;
	public float restX;
	public float restY;
	public float restW;
	public float restH;
	public float anchorX;
	@Nullable
	public QuestPainter.View view;
	public int ground;
	float dx;
	float dy;
	float px;
	float py;
	float gx0;
	float gy0;
	float gx1;
	float gy1;
	int order;
	private final int totalMs;
	private float fromMs;
	private long sinceMs;
	private boolean closing;

	Inlay(NodeView owner, Chapter chapter, GraphModel graph, int level) {
		this.owner = owner;
		this.chapter = chapter;
		this.graph = graph;
		this.level = level;
		this.base = owner.depth + 1;
		this.title = QuestText.title(chapter);
		int deepest = 0;

		for (NodeView n : graph.all) {
			deepest = Math.max(deepest, n.depth);
		}

		this.totalMs = Math.round(turn(deepest)) + 240;
		this.sinceMs = Clock.nowMs();
	}

	public void open() {
		if (this.closing) {
			float at = this.ms();
			float p = this.plate();
			this.closing = false;
			this.fromMs = at < 260.0F ? (1.0F - (float)Math.cbrt((double)(1.0F - p))) * 260.0F : at;
			this.sinceMs = Clock.nowMs();
		}
	}

	public void close() {
		if (!this.closing) {
			float at = this.ms();
			float p = this.plate();
			this.closing = true;
			this.fromMs = at < 260.0F ? (p < 0.5F ? (float)Math.cbrt((double)(p / 4.0F)) : 1.0F - (float)Math.cbrt((double)((1.0F - p) / 4.0F))) * 260.0F : at;
			this.sinceMs = Clock.nowMs();
		}
	}

	public void snapOpen() {
		this.fromMs = (float)this.totalMs;
		this.sinceMs = 0L;
		this.closing = false;
	}

	private float ms() {
		float motion = Theme.current().motion();
		if (motion <= 0.0F) {
			return this.closing ? 0.0F : (float)this.totalMs;
		} else if (this.sinceMs == 0L) {
			return this.fromMs;
		} else {
			float dt = (float)(Clock.nowMs() - this.sinceMs) / motion;
			if (!this.closing) {
				return Math.min((float)this.totalMs, this.fromMs + dt);
			} else {
				float above = Math.max(0.0F, this.fromMs - 260.0F);
				float span = Math.max(1.0F, (float)(this.totalMs - 260));
				float aboveMs = above / span * 140.0F;
				return dt < aboveMs ? this.fromMs - dt / 140.0F * span : Math.max(0.0F, Math.min(this.fromMs, 260.0F) - (dt - aboveMs) / 230.0F * 260.0F);
			}
		}
	}

	public boolean closing() {
		return this.closing;
	}

	public boolean gone() {
		return this.closing && this.ms() <= 0.5F;
	}

	public boolean moving() {
		return this.closing || this.ms() < (float)this.totalMs;
	}

	public float plate() {
		float t = Mth.clamp(this.ms() / 260.0F, 0.0F, 1.0F);
		return this.closing ? Ease.IN_OUT_CUBIC.apply(t) : Ease.OUT_CUBIC.apply(t);
	}

	private static float turn(int depth) {
		return 285.0F + (float)(Math.min(depth, 9) * 70);
	}

	public float risen(int depth) {
		return Mth.clamp((this.ms() - turn(depth)) / 240.0F, 0.0F, 1.0F);
	}

	public float drawn(int depth) {
		return Mth.clamp((this.ms() - turn(depth) + 70.0F) / 70.0F, 0.0F, 1.0F);
	}

	public float words() {
		return Mth.clamp((this.ms() - 156.0F) / 160.0F, 0.0F, 1.0F);
	}

	public int depthOf(NodeView n) {
		return Math.max(0, n.depth - this.base);
	}

	public float outer() {
		return this.owner.inlay == null ? 1.0F : this.owner.inlay.presence(this.owner);
	}

	public float presence(NodeView n) {
		return Math.min(this.risen(this.depthOf(n)), this.outer());
	}

	public float shown() {
		return Math.min(1.0F, this.plate() * 1.6F) * this.outer();
	}
}
