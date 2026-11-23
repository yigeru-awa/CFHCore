package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.text.QuestText;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.logic.Rules;
import com.onehumanawa.cfhcore.content.gui.quests.model.Chapter;
import com.onehumanawa.cfhcore.content.gui.quests.model.QuestBook;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class GraphModel {
	private static final int ENTRANCE_STEP_MS = 55;
	private static final int ENTRANCE_MAX_MS = 700;
	private static final int MAX_LEVEL = 6;
	public final Chapter chapter;
	public final List<NodeView> all = new ArrayList<>();
	public final List<NodeView> nodes = new ArrayList<>();
	public final List<LinkView> links = new ArrayList<>();
	private final Map<String, NodeView> byKey = new HashMap<>();
	private boolean showHidden;
	private TreeFilter filter = TreeFilter.ALL;
	private boolean dimDone;
	public float minX;
	public float minY;
	public float maxX;
	public float maxY;
	public float questMinX;
	public float questMinY;
	public float questMaxX;
	public float questMaxY;
	public int deepest;
	public int open;
	public int claimable;
	public int done;
	public final List<Inlay> inlays = new ArrayList<>();
	@Nullable
	private Inlay within;
	private boolean folding;
	private int ownCount;
	private boolean laid;
	private boolean moved;
	private float anchor;
	private boolean turned;
	@Nullable
	private float[] decoX;
	@Nullable
	private float[] decoY;
	@Nullable
	private float[] decoHalfW;
	@Nullable
	private float[] decoHalfH;
	private static final float UNIT_PX = 30.0F;

	private GraphModel(Chapter chapter) {
		this.chapter = chapter;
	}

	public static GraphModel of(Chapter chapter) {
		GraphModel m = new GraphModel(chapter);
		BookIndex index = QuestsState.index();
		if (index != null) {
			for (BookIndex.Entry e : index.of(chapter)) {
				NodeView n = new NodeView(e);
				n.index = m.all.size();
				m.all.add(n);
				m.byKey.put(n.key, n);
			}

			m.depths();
		}

		m.refresh(false);
		return m;
	}

	public GraphModel showHidden(boolean on) {
		if (this.showHidden != on) {
			this.showHidden = on;
			this.refresh(false);
		}

		return this;
	}

	public boolean showsHidden() {
		return this.showHidden;
	}

	@Nullable
	public NodeView node(@Nullable String key) {
		if (key == null) {
			return null;
		} else {
			NodeView n = this.byKey.get(key);
			if (n == null && !this.inlays.isEmpty()) {
				for (Inlay in : this.inlays) {
					NodeView inner = in.graph.node(key);
					if (inner != null) {
						return inner;
					}
				}

				return null;
			} else {
				return n;
			}
		}
	}

	@Nullable
	public NodeView shown(@Nullable String key) {
		NodeView n = this.node(key);
		return n != null && n.visible ? n : null;
	}

	public boolean isEmpty() {
		return this.nodes.isEmpty();
	}

	public float width() {
		return this.maxX - this.minX;
	}

	public float height() {
		return this.maxY - this.minY;
	}

	public float questWidth() {
		return this.questMaxX - this.questMinX;
	}

	public float questHeight() {
		return this.questMaxY - this.questMinY;
	}

	private void depths() {
		for (int round = 0; round < this.all.size(); round++) {
			boolean changed = false;

			for (NodeView n : this.all) {
				int d = 0;

				for (String dep : n.entry.deps()) {
					NodeView from = this.byKey.get(dep);
					if (from != null) {
						d = Math.max(d, from.depth + 1);
					}
				}

				if (d != n.depth && d <= this.all.size()) {
					n.depth = d;
					changed = true;
				}
			}

			if (!changed) {
				break;
			}
		}
	}

	public void refresh(boolean live) {
		long now = Clock.nowMs();
		this.nodes.clear();
		this.links.clear();
		this.deepest = 0;
		this.open = this.claimable = this.done = 0;

		for (NodeView n : this.all) {
			Rules.Status before = n.status;
			boolean was = n.visible;
			n.status = QuestsState.status(n.key);
			n.progress = QuestsState.fraction(n.key);
			n.pinned = QuestsState.pinned(n.key);
			n.visible = n.status.visible() || this.showHidden;
			n.folds = this.folding && n.inline() && !n.secret();
			if (live) {
				n.shownProgress.set(n.progress);
				if (!before.done() && n.status.done()) {
					n.doneAtMs = now;
				} else if ((before == Rules.Status.LOCKED || before == Rules.Status.HIDDEN) && n.status.open()) {
					n.openedAtMs = now;
				}

				if (!was && n.visible) {
					n.appearIn(0);
				}
			} else {
				n.shownProgress.snap(n.progress);
			}

			if (n.visible) {
				this.nodes.add(n);
				this.deepest = Math.max(this.deepest, n.depth);
				if (n.status.open()) {
					this.open++;
				}

				if (n.status == Rules.Status.CLAIMABLE) {
					this.claimable++;
				}

				if (n.status.done()) {
					this.done++;
				}
			}
		}

		for (NodeView to : this.nodes) {
			if (!to.quest.hideLines) {
				for (String dep : to.entry.deps()) {
					NodeView from = this.byKey.get(dep);
					if (from != null && from.visible && !from.quest.hideOutLines) {
						this.links.add(new LinkView(from, to));
					}
				}
			}
		}

		this.ownCount = this.nodes.size();
		this.anchor();
		Iterator<Inlay> it = this.inlays.iterator();

		while (it.hasNext()) {
			Inlay in = it.next();
			if (in.owner.folds && in.owner.visible) {
				GraphModel sub = in.graph;
				sub.refresh(live);
				in.stems.clear();

				for (int i = 0; i < sub.ownCount; i++) {
					if (sub.first(sub.nodes.get(i))) {
						in.stems.add(new LinkView(in.owner, sub.nodes.get(i)));
					}
				}

				in.fromAbove = new boolean[in.stems.size()];

				for (int ix = 0; ix < in.fromAbove.length; ix++) {
					in.fromAbove[ix] = sub.clearAbove(in.stems.get(ix).to());
				}

				this.nodes.addAll(sub.nodes);
				this.deepest = Math.max(this.deepest, sub.deepest);
				this.open = this.open + sub.open;
				this.claimable = this.claimable + sub.claimable;
				this.done = this.done + sub.done;
			} else {
				in.owner.unfolded.snap(0.0F);
				it.remove();
			}
		}

		if (this.within == null) {
			this.cross();
			this.bounds();
		}

		this.light(live);
	}

	private boolean first(NodeView n) {
		for (String dep : n.entry.deps()) {
			NodeView from = this.byKey.get(dep);
			if (from != null && from.visible) {
				return false;
			}
		}

		return true;
	}

	private boolean clearAbove(NodeView quest) {
		for (int i = 0; i < this.ownCount; i++) {
			NodeView n = this.nodes.get(i);
			if (n != quest && n.quest.y < quest.quest.y && Math.abs(n.quest.x - quest.quest.x) < 0.5F * n.size + 0.2F) {
				return false;
			}
		}

		float[] hw = this.decoHalfW;
		if (hw == null) {
			return true;
		} else {
			for (int ix = 0; ix < hw.length && ix < this.chapter.decorations.size(); ix++) {
				Chapter.Decoration d = this.chapter.decorations.get(ix);
				if (hw[ix] >= 0.0F && d.y < quest.quest.y && Math.abs(d.x - quest.quest.x) < hw[ix] + 0.2F) {
					return false;
				}
			}

			return true;
		}
	}

	private void anchor() {
		float sum = 0.0F;
		int count = 0;

		for (int i = 0; i < this.ownCount; i++) {
			NodeView n = this.nodes.get(i);
			if (this.first(n)) {
				sum += n.quest.x;
				count++;
			}
		}

		if (count == 0) {
			for (int ix = 0; ix < this.ownCount; count++) {
				sum += this.nodes.get(ix).quest.x;
				ix++;
			}
		}

		this.anchor = count == 0 ? 0.0F : sum / (float)count;
	}

	public boolean canTurn() {
		if (this.within == null && !this.laid && this.inlays.isEmpty()) {
			for (NodeView n : this.all) {
				if (n.inline()) {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public boolean turned() {
		return this.turned;
	}

	public boolean turn(boolean on) {
		if (on == this.turned) {
			return true;
		} else if (on && !this.canTurn()) {
			return false;
		} else {
			this.turned = on;

			for (NodeView n : this.all) {
				float t = n.x;
				n.x = n.y;
				n.y = t;
				t = n.restX;
				n.restX = n.restY;
				n.restY = t;
			}

			this.bounds();
			return true;
		}
	}

	public void bounds() {
		if (this.within == null) {
			if (this.inlays.isEmpty() && !this.laid) {
				this.measure();
				this.box(true);
				this.rooms();
			} else {
				this.lay();
			}
		}
	}

	private void measure() {
		int count = this.chapter.decorations.size();
		if (this.decoX == null || this.decoX.length != count) {
			this.decoX = new float[count];
			this.decoY = new float[count];
			this.decoHalfW = new float[count];
			this.decoHalfH = new float[count];
		}

		for (int i = 0; i < count; i++) {
			Chapter.Decoration d = this.chapter.decorations.get(i);
			this.decoX[i] = this.turned ? d.y : d.x;
			this.decoY[i] = this.turned ? d.x : d.y;
			if ("image".equals(d.type)) {
				this.decoHalfW[i] = Math.max(0.0F, d.w) / 2.0F;
				this.decoHalfH[i] = Math.max(0.0F, d.h) / 2.0F;
			} else if (d.text.isBlank()) {
				this.decoHalfW[i] = this.decoHalfH[i] = -1.0F;
			} else {
				float s = Math.max(0.1F, d.scale);
				this.decoHalfW[i] = (float)CFHGuiDraw.font().width(QuestText.text(d.text)) * s / 30.0F / 2.0F;
				this.decoHalfH[i] = 9.0F * s / 30.0F / 2.0F;
			}
		}
	}

	private void box(boolean rest) {
		boolean any = false;
		this.questMinX = this.questMinY = Float.MAX_VALUE;
		this.questMaxX = this.questMaxY = -Float.MAX_VALUE;

		for (int i = 0; i < this.ownCount; i++) {
			NodeView n = this.nodes.get(i);
			float half = 0.5F * n.size;
			this.questMinX = Math.min(this.questMinX, n.x - half);
			this.questMinY = Math.min(this.questMinY, n.y - half);
			this.questMaxX = Math.max(this.questMaxX, n.x + half);
			this.questMaxY = Math.max(this.questMaxY, n.y + half);
			any = true;
		}

		for (Inlay in : this.inlays) {
			if ((!rest || !in.closing()) && !(in.gx1 - in.gx0 < 0.01F) && !(in.gy1 - in.gy0 < 0.01F)) {
				this.questMinX = Math.min(this.questMinX, in.gx0);
				this.questMinY = Math.min(this.questMinY, in.gy0);
				this.questMaxX = Math.max(this.questMaxX, in.gx1);
				this.questMaxY = Math.max(this.questMaxY, in.gy1);
				any = true;
			}
		}

		if (!any) {
			this.questMinX = this.questMinY = -1.0F;
			this.questMaxX = this.questMaxY = 1.0F;
		}

		this.minX = this.questMinX;
		this.minY = this.questMinY;
		this.maxX = this.questMaxX;
		this.maxY = this.questMaxY;
		float[] hw = this.decoHalfW;
		float[] hh = this.decoHalfH;
		float[] dx = this.decoX;
		float[] dy = this.decoY;
		if (hw != null && hh != null && dx != null && dy != null) {
			for (int i = 0; i < hw.length; i++) {
				if (!(hw[i] < 0.0F)) {
					this.minX = Math.min(this.minX, dx[i] - hw[i]);
					this.minY = Math.min(this.minY, dy[i] - hh[i]);
					this.maxX = Math.max(this.maxX, dx[i] + hw[i]);
					this.maxY = Math.max(this.maxY, dy[i] + hh[i]);
				}
			}
		}
	}

	private void rooms() {
		NodeView[] byX = this.nodes.toArray(new NodeView[0]);
		Arrays.sort(byX, (ax, b) -> Float.compare(ax.x, b.x));

		for (int i = 0; i < byX.length; i++) {
			NodeView a = byX[i];
			float room = 3.0F;

			for (int j = i + 1; j < byX.length && byX[j].x - a.x < room; j++) {
				if (Math.abs(byX[j].y - a.y) < 0.8F) {
					room = byX[j].x - a.x;
				}
			}

			for (int jx = i - 1; jx >= 0 && a.x - byX[jx].x < room; jx--) {
				if (Math.abs(byX[jx].y - a.y) < 0.8F) {
					room = a.x - byX[jx].x;
				}
			}

			a.room = Math.max(0.6F, room);
		}
	}

	public void filter(TreeFilter f, boolean dimDone) {
		if (this.filter != f || this.dimDone != dimDone) {
			this.filter = f;
			this.dimDone = dimDone;

			for (Inlay in : this.inlays) {
				in.graph.filter(f, dimDone);
			}

			this.light(true);
		}
	}

	public TreeFilter filter() {
		return this.filter;
	}

	private void light(boolean glide) {
		for (NodeView n : this.nodes) {
			float to = !this.filter.shows(n.status) ? 0.0F : (this.dimDone && this.filter == TreeFilter.ALL && n.status == Rules.Status.COMPLETE ? 0.45F : 1.0F);
			if (glide) {
				n.lit.set(to);
			} else {
				n.lit.snap(to);
			}
		}
	}

	public void playEntrance() {
		int step = this.deepest <= 0 ? 0 : Math.min(55, 700 / this.deepest);
		float span = Math.max(1.0F, this.height());

		for (NodeView n : this.nodes) {
			n.appearIn(n.depth * step + Math.round(40.0F * (n.y - this.minY) / span));
		}
	}

	public void playEntrance(float x0, float y0, float x1, float y1) {
		int first = Integer.MAX_VALUE;
		int last = 0;

		for (NodeView n : this.nodes) {
			if (!(n.x < x0) && !(n.x > x1) && !(n.y < y0) && !(n.y > y1)) {
				first = Math.min(first, n.depth);
				last = Math.max(last, n.depth);
			}
		}

		if (first == Integer.MAX_VALUE) {
			this.settle();
		} else {
			int step = Math.min(55, 700 / Math.max(1, last - first));
			float span = Math.max(1.0F, y1 - y0);

			for (NodeView nx : this.nodes) {
				if (!(nx.x < x0) && !(nx.x > x1) && !(nx.y < y0) && !(nx.y > y1)) {
					nx.appearIn((nx.depth - first) * step + Math.round(40.0F * (nx.y - y0) / span));
				} else {
					nx.appearNow();
				}
			}
		}
	}

	public void settle() {
		for (NodeView n : this.nodes) {
			n.appearNow();
		}
	}

	public GraphModel folding(boolean on) {
		if (this.folding == on) {
			return this;
		} else {
			this.folding = on;
			if (!on) {
				for (Inlay in : this.inlays) {
					in.owner.unfolded.snap(0.0F);
				}

				this.inlays.clear();
			}

			this.refresh(false);
			return this;
		}
	}

	public boolean folding() {
		return this.folding;
	}

	@Nullable
	public Inlay inlay(@Nullable String ownerKey) {
		if (ownerKey == null) {
			return null;
		} else {
			for (Inlay in : this.inlays) {
				if (in.owner.key.equals(ownerKey)) {
					return in;
				}

				Inlay inner = in.graph.inlay(ownerKey);
				if (inner != null) {
					return inner;
				}
			}

			return null;
		}
	}

	@Nullable
	public Inlay unfold(String ownerKey, boolean atOnce) {
		Inlay in = this.add(ownerKey);
		if (in == null) {
			return null;
		} else {
			if (atOnce) {
				in.snapOpen();
			} else {
				in.open();
			}

			if (atOnce) {
				in.owner.unfolded.snap(1.0F);
			} else {
				in.owner.unfolded.set(1.0F);
			}

			this.refresh(false);
			return in;
		}
	}

	public void unfoldAll(Collection<String> ownerKeys) {
		boolean any = false;
		boolean more = true;

		while (more) {
			more = false;

			for (String key : ownerKeys) {
				if (this.inlay(key) == null) {
					Inlay in = this.add(key);
					if (in != null) {
						in.snapOpen();
						in.owner.unfolded.snap(1.0F);
						more = true;
						any = true;
					}
				}
			}
		}

		if (any) {
			this.refresh(false);
		}
	}

	public boolean fold(String ownerKey, boolean atOnce) {
		Inlay in = this.inlay(ownerKey);
		if (in == null) {
			return false;
		} else {
			if (atOnce) {
				in.owner.unfolded.snap(0.0F);
				(in.owner.inlay == null ? this : in.owner.inlay.graph).inlays.remove(in);
				this.refresh(false);
			} else {
				in.owner.unfolded.set(0.0F);
				in.close();
				this.lay();
			}

			return true;
		}
	}

	@Nullable
	private Inlay add(String ownerKey) {
		NodeView o = this.node(ownerKey);
		QuestBook book = QuestsState.book();
		if (this.folding && o != null && book != null && o.visible && o.folds) {
			GraphModel holder = o.inlay == null ? this : o.inlay.graph;

			for (Inlay in : holder.inlays) {
				if (in.owner == o) {
					return in;
				}
			}

			Chapter sub = book.chapter(o.quest.subtree);
			if (sub != null && !sub.id.equals(this.chapter.id)) {
				int level = 1;

				for (Inlay up = o.inlay; up != null; level++) {
					if (up.chapter.id.equals(sub.id)) {
						return null;
					}

					up = up.owner.inlay;
				}

				if (level > 6) {
					return null;
				} else {
					GraphModel tree = of(sub);
					Inlay inx = new Inlay(o, sub, tree, level);
					tree.within = inx;
					tree.folding = true;
					tree.filter = this.filter;
					tree.dimDone = this.dimDone;

					for (NodeView n : tree.all) {
						n.inlay = inx;
						n.depth = n.depth + inx.base;
					}

					tree.refresh(false);
					int at = 0;

					while (at < holder.inlays.size() && !below(holder.inlays.get(at).owner, o)) {
						at++;
					}

					holder.inlays.add(at, inx);
					return inx;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static boolean below(NodeView a, NodeView b) {
		return a.quest.y > b.quest.y || a.quest.y == b.quest.y && a.quest.x > b.quest.x;
	}

	public boolean animating() {
		for (Inlay in : this.inlays) {
			if (in.moving() || in.graph.animating()) {
				return true;
			}
		}

		return false;
	}

	public boolean tick() {
		if (this.inlays.isEmpty() || this.within != null) {
			return false;
		} else if (this.sweep()) {
			this.refresh(false);
			return true;
		} else {
			if (this.animating()) {
				this.arrange(false);
				this.place(0.0F, 0.0F);
				this.moved = true;
			} else if (this.moved) {
				this.lay();
			}

			return false;
		}
	}

	private boolean sweep() {
		boolean any = false;
		Iterator<Inlay> it = this.inlays.iterator();

		while (it.hasNext()) {
			Inlay in = it.next();
			if (in.gone()) {
				it.remove();
				any = true;
			} else {
				any |= in.graph.sweep();
			}
		}

		return any;
	}

	public boolean laid() {
		return this.laid;
	}

	public float restX(NodeView n) {
		return this.laid ? n.restX : n.x;
	}

	public float restY(NodeView n) {
		return this.laid ? n.restY : n.y;
	}

	public float decorationX(int i) {
		float[] at = this.decoX;
		return (this.laid || this.within != null) && at != null && i < at.length ? at[i] : this.chapter.decorations.get(i).x;
	}

	public float decorationY(int i) {
		float[] at = this.decoY;
		return (this.laid || this.within != null) && at != null && i < at.length ? at[i] : this.chapter.decorations.get(i).y;
	}

	public float decorationHalfW(int i) {
		float[] hw = this.decoHalfW;
		return hw != null && i < hw.length ? hw[i] : -1.0F;
	}

	public float decorationHalfH(int i) {
		float[] hh = this.decoHalfH;
		return hh != null && i < hh.length ? hh[i] : -1.0F;
	}

	private void lay() {
		this.measureAll();
		this.arrange(true);
		this.place(0.0F, 0.0F);
		this.box(true);

		for (NodeView n : this.nodes) {
			n.restX = n.x;
			n.restY = n.y;
		}

		rest(this.inlays);
		this.rooms();
		this.laid = !this.inlays.isEmpty();
		this.moved = false;
		if (this.animating()) {
			this.arrange(false);
			this.place(0.0F, 0.0F);
			this.moved = true;
		}
	}

	private void measureAll() {
		this.measure();

		for (Inlay in : this.inlays) {
			in.graph.measureAll();
		}
	}

	private static void rest(List<Inlay> inlays) {
		for (Inlay in : inlays) {
			in.restX = in.x;
			in.restY = in.y;
			in.restW = in.w;
			in.restH = in.h;
			rest(in.graph.inlays);
		}
	}

	private void arrange(boolean rest) {
		for (NodeView n : this.all) {
			n.x = n.quest.x;
			n.y = n.quest.y;
		}

		float[] dx = this.decoX;
		float[] dy = this.decoY;
		float[] hw = this.decoHalfW;
		float[] hh = this.decoHalfH;
		int decos = dx != null && dy != null && hw != null && hh != null ? Math.min(dx.length, this.chapter.decorations.size()) : 0;

		for (int i = 0; i < decos; i++) {
			dx[i] = this.chapter.decorations.get(i).x;
			dy[i] = this.chapter.decorations.get(i).y;
		}

		for (int k = 0; k < this.inlays.size(); k++) {
			Inlay in = this.inlays.get(k);
			GraphModel sub = in.graph;
			sub.arrange(rest);
			sub.box(rest);
			float p = rest ? (in.closing() ? 0.0F : 1.0F) : in.plate();
			NodeView o = in.owner;
			float w = sub.width() + 1.0F;
			float h = sub.height() + 1.0F + 0.75F;
			in.dx = o.x - sub.anchor;
			float x0 = in.dx + sub.minX - 0.5F;
			float x1 = x0 + w;
			float c0 = x0 - 0.3F;
			float c1 = x1 + 0.3F;
			float top = o.y + 0.5F * o.size + 0.3F;

			for (NodeView n : this.all) {
				if (n != o && n.visible && !(n.y > o.y)) {
					float half = 0.5F * n.size;
					if (n.x + half > c0 && n.x - half < c1) {
						top = Math.max(top, n.y + half + 0.3F);
					}
				}
			}

			for (int i = 0; i < decos; i++) {
				if (!(hw[i] < 0.0F) && !(dy[i] > o.y) && dx[i] + hw[i] > c0 && dx[i] - hw[i] < c1) {
					top = Math.max(top, dy[i] + hh[i] + 0.3F);
				}
			}

			for (int e = 0; e < k; e++) {
				Inlay earlier = this.inlays.get(e);
				if (earlier.gx1 > c0 && earlier.gx0 < c1) {
					top = Math.max(top, earlier.gy1 + 0.3F);
				}
			}

			float first = Float.MAX_VALUE;

			for (NodeView nx : this.all) {
				if (nx.visible && !(nx.y <= o.y)) {
					float half = 0.5F * nx.size;
					if (nx.x + half > c0 && nx.x - half < c1) {
						first = Math.min(first, nx.y - half);
					}
				}
			}

			for (int ix = 0; ix < decos; ix++) {
				if (!(hw[ix] < 0.0F) && !(dy[ix] <= o.y) && dx[ix] + hw[ix] > c0 && dx[ix] - hw[ix] < c1) {
					first = Math.min(first, dy[ix] - hh[ix]);
				}
			}

			float need = first == Float.MAX_VALUE ? 0.0F : Math.max(0.0F, top + h + 0.3F - first);
			float shift = need * p;
			if (shift > 0.0F) {
				for (NodeView nxx : this.all) {
					if (nxx.y > o.y) {
						nxx.y += shift;
					}
				}

				for (int ixx = 0; ixx < decos; ixx++) {
					if (dy[ixx] > o.y) {
						dy[ixx] += shift;
					}
				}
			}

			in.px = x0;
			in.py = top;
			in.w = w;
			in.h = h;
			in.dy = top + 0.75F + 0.5F - sub.minY;
			float inset = Math.min(0.6F, w / 2.0F);
			in.anchorX = Mth.clamp(o.x, x0 + inset, x1 - inset);
			in.gx0 = Mth.lerp(p, in.anchorX, x0);
			in.gx1 = Mth.lerp(p, in.anchorX, x1);
			in.gy0 = top;
			in.gy1 = top + h * p;
		}
	}

	private void place(float ox, float oy) {
		if (ox != 0.0F || oy != 0.0F) {
			for (NodeView n : this.all) {
				n.x += ox;
				n.y += oy;
			}

			float[] dx = this.decoX;
			float[] dy = this.decoY;
			if (dx != null && dy != null) {
				for (int i = 0; i < dx.length; i++) {
					dx[i] += ox;
					dy[i] += oy;
				}
			}
		}

		for (Inlay in : this.inlays) {
			in.x = ox + in.px;
			in.y = oy + in.py;
			in.gx0 += ox;
			in.gx1 += ox;
			in.gy0 += oy;
			in.gy1 += oy;
			in.anchorX += ox;
			in.graph.place(ox + in.dx, oy + in.dy);
		}
	}

	private void cross() {
		if (!this.inlays.isEmpty()) {
			List<Inlay> order = new ArrayList<>();
			order(this.inlays, order);
			this.cross(this, 0, order);

			for (Inlay in : order) {
				this.cross(in.graph, in.order, order);
			}
		}
	}

	private static void order(List<Inlay> inlays, List<Inlay> into) {
		for (Inlay in : inlays) {
			into.add(in);
			in.order = into.size();
			in.cross.clear();
			order(in.graph.inlays, into);
		}
	}

	private void cross(GraphModel tree, int place, List<Inlay> order) {
		for (int i = 0; i < tree.ownCount; i++) {
			NodeView to = tree.nodes.get(i);
			if (!to.quest.hideLines) {
				for (String dep : to.entry.deps()) {
					if (!tree.byKey.containsKey(dep)) {
						NodeView from = this.shown(dep);
						if (from != null && !from.quest.hideOutLines) {
							int other = from.inlay == null ? 0 : from.inlay.order;
							int later = Math.max(place, other);
							if (later > 0) {
								order.get(later - 1).cross.add(new LinkView(from, to));
							}
						}
					}
				}
			}
		}
	}
}
