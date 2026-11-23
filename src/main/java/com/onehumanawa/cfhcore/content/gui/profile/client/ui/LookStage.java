package com.onehumanawa.cfhcore.content.gui.profile.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Pose;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import com.onehumanawa.cfhcore.content.gui.profile.client.ProfileStore;
import com.onehumanawa.cfhcore.content.gui.profile.client.SkinComposer;
import com.onehumanawa.cfhcore.content.gui.profile.client.render.LookNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class LookStage {
	private static final float STIFFNESS = 58.0F;
	private static final float DAMPING = 15.0F;
	private static final float FAINT = 0.55F;
	private float pace = 1.62F;
	private float target;
	final Stage stage;
	private final boolean theatre;
	private final List<LookStage.Stand> stands = new ArrayList<>();
	private final List<FxNode> places = new ArrayList<>();
	private float turned;
	private boolean turnHome;
	@Nullable
	private FxNode floor;
	@Nullable
	private MotesNode motes;
	private final PivotNode driver = new PivotNode();
	private int viewed;
	private float position;
	private float velocity;
	private String worn = "";
	private final Anim edit = new Anim(0.0F, 560, Ease.IN_OUT_CUBIC);
	private final Anim zoom = new Anim(0.0F, 460, Ease.IN_OUT_CUBIC);
	private final Anim turn = new Anim(0.0F, 520, Ease.IN_OUT_CUBIC);
	@Nullable
	private Slot focus;
	private final Vector3f focusAt = new Vector3f(0.0F, 1.0F, 0.0F);
	private final Vector3f eye = new Vector3f();
	private final Vector3f aim = new Vector3f();
	private float focusDistance = 3.0F;
	private float aspect = 2.0F;
	@Nullable
	private IntConsumer onPick;

	LookStage(Screen owner, boolean theatre) {
		this.theatre = theatre;
		Stage s = new Stage().bind(owner);
		this.stage = s;
		s.background(0);
		s.soft(true);
		s.finish(theatre ? StageFinish.cinematic().bloom(0.5F).vignette(0.3F) : StageFinish.glow());
		s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
		s.focusOutline(false);
		s.camera().idle(IdleMotion.sway(24000.0F, 1.6F, 0.3F, 0.01F));
		if (theatre) {
			s.lighting()
				.key(1.4F, 4.6F, 4.2F)
				.keyColor(0.7F, 0.64F, 0.56F)
				.ambientColor(0.26F, 0.26F, 0.3F)
				.fill(-1.0F, 0.4F, 0.4F, 0.1F, 0.13F, 0.2F)
				.rim(1.0F, 0.9F, 0.76F, 0.3F)
				.wrap(0.5F)
				.sun(0.3F, 1.0F, 0.6F);
			this.floor = FxNode.glow(9.0F, -803464422).fade(2.2F);
			((FxNode)s.add(this.floor)).at(0.0F, -0.005F, -0.5F);
			this.motes = new MotesNode(46, 7.0F, 3.2F, 3.0F, 0.022F, 1627383746, 12L);
			((MotesNode)s.add(this.motes)).at(0.0F, 1.7F, 0.2F);
			Theme t = Theme.current();
			int warm = Colors.lerp(-5688, t.accent(), t.isVanilla() ? 0.0F : 0.22F);

			for (int i = 0; i < 4; i++) {
				FxNode place = FxNode.glow(1.25F, Colors.withAlpha(warm, 112)).fade(1.7F);
				place.visible(false);
				s.add(place);
				this.places.add(place);
			}
		} else {
			s.lighting().key(2.0F, 4.0F, 5.0F).sun(0.3F, 1.0F, 0.6F);
			((FxNode)s.add(FxNode.glow(3.0F, -1071899878).fade(2.0F))).at(0.0F, -0.005F, 0.0F);
		}

		s.add(this.driver);
		this.driver.onFrame(this::frame);
	}

	void onPick(IntConsumer listener) {
		this.onPick = listener;
	}

	List<LookStage.Stand> stands() {
		return this.stands;
	}

	int viewed() {
		return this.viewed;
	}

	@Nullable
	LookStage.Stand viewedStand() {
		return this.stands.isEmpty() ? null : this.stands.get(Mth.clamp(this.viewed, 0, this.stands.size() - 1));
	}

	float editing() {
		return this.edit.get();
	}

	float zoomed() {
		return this.zoom.get();
	}

	void show(List<ProfileStore.Look> looks, boolean blank, @Nullable String keep) {
		this.show(looks, blank, keep, -1);
	}

	void show(List<ProfileStore.Look> looks, boolean blank, @Nullable String keep, int at0) {
		for (LookStage.Stand st : this.stands) {
			this.stage.remove(st.node);
			this.stage.remove(st.beam);
			this.stage.remove(st.pool);
			this.stage.remove(st.root);
		}

		this.stands.clear();
		Theme t = Theme.current();
		int warm = Colors.lerp(-5688, t.accent(), t.isVanilla() ? 0.0F : 0.22F);
		int i = 0;
		List<ProfileStore.Look> all = new ArrayList<>(looks);
		if (blank || looks.isEmpty()) {
			all.add(null);
		}

		for (ProfileStore.Look look : all) {
			LookNode node = look != null ? new LookNode(look) : LookNode.ofTexture(SkinComposer.defaultSkin(i % 2 == 0 ? "steve" : "alex", i % 2 != 0), i % 2 != 0);
			if (look == null) {
				node.muted(0.85F);
			}

			FxNode beam = FxNode.cone(0.16F, 1.05F, 4.4F, Colors.withAlpha(warm, 60));
			FxNode pool = FxNode.glow(1.25F, Colors.withAlpha(warm, 112)).fade(1.7F);
			LookStage.Stand st = new LookStage.Stand(look, node, beam, pool);
			this.stage.add(st.root);
			((FxNode)this.stage.add(pool)).at(0.0F, 0.01F, 0.0F).attachTo(st.root);
			((LookNode)this.stage.add(node)).attachTo(st.root);
			((FxNode)this.stage.add(beam)).attachTo(st.root);
			node.hoverFeel(0.02F, 1.03F);
			int index = i;
			node.onClick(() -> {
				if (this.onPick != null) {
					this.onPick.accept(index);
				}
			});
			beam.visible(this.theatre);
			pool.visible(this.theatre);
			this.stands.add(st);
			i++;
		}

		int at = 0;
		if (keep != null) {
			for (int k = 0; k < this.stands.size(); k++) {
				if (this.stands.get(k).look != null && this.stands.get(k).look.id().equals(keep)) {
					at = k;
				}
			}
		}

		if (at0 >= 0) {
			at = Math.min(at0, Math.max(0, this.stands.size() - 1));
		}

		this.viewed = at;
		this.position = (float)at;
		this.target = (float)at;
		this.velocity = 0.0F;
	}

	private static float wrap(float a, int n) {
		if (n <= 0) {
			return 0.0F;
		} else {
			float r = a % (float)n;
			if (r < 0.0F) {
				r += (float)n;
			}

			if (r > (float)n / 2.0F) {
				r -= (float)n;
			}

			return r;
		}
	}

	void view(int index) {
		if (!this.stands.isEmpty()) {
			this.viewed = Math.floorMod(index, this.stands.size());
			this.target = this.position + wrap((float)this.viewed - this.position, this.stands.size());
		}
	}

	void worn(String id) {
		this.worn = id == null ? "" : id;
	}

	void edit(boolean on) {
		this.edit.set(on);
		if (!on) {
			this.focus(null);
		}
	}

	void turnBy(float degrees) {
		this.turned += degrees;
		this.turnHome = false;
	}

	void focus(@Nullable Slot slot) {
		this.focus = slot;
		this.turned = Mth.wrapDegrees(this.turned);
		this.turnHome = true;
		if (slot != null && slot != Slot.SKIN) {
			this.turn.set(slot == Slot.BACK ? 1.0F : 0.0F);
			switch (slot) {
				case HAT:
					this.focusAt.set(0.0F, 1.86F, 0.0F);
					this.focusDistance = 2.5F;
					break;
				case FACE:
					this.focusAt.set(0.0F, 1.64F, 0.0F);
					this.focusDistance = 1.9F;
					break;
				case SHIRT:
					this.focusAt.set(0.0F, 1.14F, 0.0F);
					this.focusDistance = 2.7F;
					break;
				case PANTS:
					this.focusAt.set(0.0F, 0.42F, 0.0F);
					this.focusDistance = 2.7F;
					break;
				case BACK:
					this.focusAt.set(0.0F, 1.2F, 0.0F);
					this.focusDistance = 3.0F;
					break;
				case RIGHT_ARM:
					this.focusAt.set(-0.36F, 1.1F, 0.0F);
					this.focusDistance = 2.3F;
					break;
				case LEFT_ARM:
					this.focusAt.set(0.36F, 1.1F, 0.0F);
					this.focusDistance = 2.3F;
					break;
				case RIGHT_LEG:
					this.focusAt.set(-0.13F, 0.4F, 0.0F);
					this.focusDistance = 2.3F;
					break;
				case LEFT_LEG:
					this.focusAt.set(0.13F, 0.4F, 0.0F);
					this.focusDistance = 2.3F;
					break;
				default:
					this.focusAt.set(-0.2F, 1.55F, 0.0F);
					this.focusDistance = 2.7F;
			}

			this.zoom.set(1.0F);
		} else {
			this.zoom.set(0.0F);
			this.turn.set(0.0F);
		}
	}

	@Nullable
	Slot focus() {
		return this.focus;
	}

	void shape(float widthOverHeight) {
		this.aspect = Math.max(0.6F, widthOverHeight);
	}

	private void frame(StageRenderContext ctx) {
		float motion = Theme.current().motion();
		if (motion <= 0.0F) {
			this.position = this.target;
			this.velocity = 0.0F;
		} else {
			float left = Math.min(0.1F, ctx.deltaMs / 1000.0F) / motion;

			while (left > 0.0F) {
				float dt = Math.min(left, 0.008333334F);
				this.velocity = this.velocity + (58.0F * (this.target - this.position) - 15.0F * this.velocity) * dt;
				this.position = this.position + this.velocity * dt;
				left -= dt;
			}
		}

		float e = this.edit.get();
		if (this.turnHome) {
			this.turned = this.turned * (float)Math.exp((double)(-Math.min(0.1F, ctx.deltaMs / 1000.0F) * (motion <= 0.0F ? 1000.0F : 9.0F / motion)));
			if (Math.abs(this.turned) < 0.4F) {
				this.turned = 0.0F;
				this.turnHome = false;
			}
		}

		for (int i = 0; i < this.stands.size(); i++) {
			LookStage.Stand st = this.stands.get(i);
			float d = this.stands.size() >= 5 ? wrap((float)i - this.position, this.stands.size()) : (float)i - this.position;
			float far = Math.abs(d);
			float inView = Math.max(0.0F, 1.0F - far);
			float out = 1.0F + e * 1.4F * (1.0F - inView);
			float x = d * this.pace * out;
			float z = -((float)Math.pow((double)Math.min(far, 3.2F), 1.25)) * 0.78F;
			float size = 1.0F - 0.13F * Math.min(far, 2.4F);
			float seen = Mth.clamp(2.7F - far, 0.0F, 1.0F) * (1.0F - e * (1.0F - inView));
			st.root.at(x, 0.0F, z);
			st.root.scale(size);
			st.root.visible(seen > 0.01F);
			st.root.alpha(seen);
			st.node.yaw(-d * 13.0F * (1.0F - e) + 180.0F * this.turn.get() * inView + this.turned * inView * e);
			st.node.pickable(seen > 0.6F && e < 0.5F);
			st.node.lookAtCursor(false);
			boolean isWorn = st.look != null && st.look.id().equals(this.worn);
			st.lit.set(isWorn ? 1.0F : (inView > 0.5F ? 0.72F : 0.0F));
			float lit = st.lit.get();
			st.beam.alpha(lit * (1.0F - 0.55F * this.zoom.get()));
			st.beam.visible(this.theatre && lit > 0.01F);
			float pool = Math.max(lit, 0.55F);
			st.pool.alpha(pool);
			st.pool.visible(this.theatre && pool > 0.01F);
		}

		int used = 0;
		if (this.theatre && this.stands.size() < 5 && e < 0.99F) {
			for (int j = Mth.floor(this.position - 2.7F); j <= Mth.ceil(this.position + 2.7F) && used < this.places.size(); j++) {
				if (j < 0 || j >= this.stands.size()) {
					float d = (float)j - this.position;
					float far = Math.abs(d);
					float seen = Mth.clamp(2.7F - far, 0.0F, 1.0F) * (1.0F - e);
					if (!(seen <= 0.01F)) {
						FxNode place = this.places.get(used++);
						place.at(d * this.pace, 0.01F, -((float)Math.pow((double)Math.min(far, 3.2F), 1.25)) * 0.78F);
						place.scale(1.0F - 0.13F * Math.min(far, 2.4F));
						place.alpha(seen * 0.55F * 0.75F);
						place.visible(true);
					}
				}
			}
		}

		while (used < this.places.size()) {
			this.places.get(used).visible(false);
			used++;
		}

		if (this.floor != null) {
			this.floor.alpha(1.0F - e);
			this.floor.visible(e < 0.99F);
		}

		if (this.motes != null) {
			this.motes.strength(1.0F - 0.8F * this.zoom.get());
		}

		this.camera(e);
	}

	private void camera(float e) {
		float fov = 30.0F;
		float tan = (float)Math.tan(Math.toRadians(15.0));
		float upright = 2.95F / (2.0F * tan);
		this.pace = Mth.clamp(2.0F * upright * tan * this.aspect * 0.8F / 4.6F, 1.45F, 2.3F);
		float across = this.pace * 4.6F / (2.0F * this.aspect * tan * 0.86F);
		float row = this.theatre ? Math.max(across, upright) : upright * 1.02F;
		float one = Math.max(3.0F / (2.0F * tan), 1.5F / (2.0F * this.aspect * tan));
		float dist = Mth.lerp(e, row, one);
		this.eye.set(0.0F, 1.18F + dist * 0.06F - 0.06F * e, dist);
		this.aim.set(0.0F, 1.04F - 0.06F * e, 0.0F);
		float zm = this.zoom.get();
		if (zm > 0.001F) {
			float side = this.turn.get() > 0.5F ? -1.0F : 1.0F;
			this.eye.lerp(new Vector3f(this.focusAt.x * side, this.focusAt.y + 0.12F, this.focusDistance), zm);
			this.aim.lerp(new Vector3f(this.focusAt.x * side, this.focusAt.y, 0.0F), zm);
		}

		this.stage.camera().at(this.eye.x, this.eye.y, this.eye.z).lookAt(this.aim.x, this.aim.y, this.aim.z).fov(30.0F).clip(0.05F, 80.0F);
		LookStage.Stand v = this.viewedStand();
		if (v != null) {
			v.node.pose(Pose.STAND);
		}
	}

	void close() {
		this.stands.clear();
		this.stage.close();
	}

	static final class Stand {
		@Nullable
		final ProfileStore.Look look;
		final PivotNode root = new PivotNode();
		final LookNode node;
		final FxNode beam;
		final FxNode pool;
		private final Anim lit = new Anim(0.0F, 420, Ease.OUT_CUBIC);

		Stand(@Nullable ProfileStore.Look look, LookNode node, FxNode beam, FxNode pool) {
			this.look = look;
			this.node = node;
			this.beam = beam;
			this.pool = pool;
		}
	}
}
