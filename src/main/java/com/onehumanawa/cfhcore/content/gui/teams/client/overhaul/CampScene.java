package com.onehumanawa.cfhcore.content.gui.teams.client.overhaul;

import com.mojang.authlib.GameProfile;
import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageLighting;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.BlockNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.FxNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PivotNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SkyNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Pose;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

final class CampScene {
	private static final float FOV = 30.0F;
	private static final float PART_DELAY = 520.0F;
	private static final float PART_STEP = 150.0F;
	private static final float PART_MS = 560.0F;
	private static final float ARRIVE_MS = 2300.0F;
	private static final float PACE = 1.6F;
	private static final float PACE_WIDE = 2.4F;
	private static final float GAP = 2.7F;
	private static final float FOOT = 0.75F;
	private static final float BODY = 2.85F;
	private static final float NEARER = 1.12F;
	private static final float EDGE = 1.3F;
	private static boolean seen;
	final Stage stage;
	private final PivotNode driver = new PivotNode();
	private final SkyNode sky;
	@Nullable
	private CampBuild.Plan plan;
	private String built = "";
	private boolean camp;
	private final Map<String, Float> rising = new HashMap<>();
	private final List<StageNode> pools = new ArrayList<>();
	private final List<CampScene.Figure> figures = new ArrayList<>();
	@Nullable
	private UUID selected;
	@Nullable
	private Consumer<CampScene.Person> onPick;
	private final PivotNode bannerPivot = new PivotNode();
	@Nullable
	private BlockNode banner;
	private String bannerLook = "";
	private float bannerFrom = -1.0E9F;
	private boolean hoist;
	private final PivotNode towerPivot = new PivotNode();
	@Nullable
	private BlockNode towerBanner;
	private final FxNode fireGlow;
	private final FxNode fireCore;
	private final MotesNode embers;
	private final MotesNode fireflies;
	private final SmokeNode fireSmoke;
	private final SmokeNode chimneySmoke;
	private boolean viewed;
	private final Anim viewMix = new Anim(0.0F, 900, Ease.IN_OUT_CUBIC);
	private final Anim grown = new Anim(1.0F, 900, Ease.IN_OUT_CUBIC);
	private float aspect = 2.4F;
	private int viewH = 160;
	private float span = 4.4F;
	private float spanNow = -1.0F;
	private float clock;
	private final float arriveMs;
	private final boolean full;
	private long ticks;
	private final Vector3f eye = new Vector3f();
	private final Vector3f aim = new Vector3f();
	private final Vector3f eyeYard = new Vector3f();
	private final Vector3f aimYard = new Vector3f();
	private final Vector3f ndc = new Vector3f();

	CampScene(Screen owner) {
		Stage s = new Stage().bind(owner);
		this.stage = s;
		this.full = !seen || DevHarness.replayIntros();
		this.arriveMs = this.full ? 2300.0F : 900.0F;
		seen = true;
		s.soft(true);
		s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
		s.focusOutline(false);
		s.background(-9810322);
		this.sky = (SkyNode)s.add(
			new SkyNode(220, 61L).colors(-10729882, -743312, -2982264, -10792040, -15065528).sun(-26.0F, 2.5F, -15746, 0.95F, 3.2F, 38.0F).stars(0.6F)
		);
		s.finish(
			StageFinish.cinematic()
				.bloom(0.56F)
				.vignette(0.3F)
				.warmth(0.02F)
				.contrast(1.06F)
				.saturation(1.14F)
				.grain(1.2F)
				.shadows(0.0F, 0.004F, 0.022F)
				.highlights(0.03F, 0.012F, -0.01F)
		);
		s.lighting()
			.sun(-0.55F, 0.5F, -0.35F)
			.ambient(0.55F)
			.key(-26.0F, 11.0F, -10.0F)
			.keyColor(0.92F, 0.6F, 0.36F)
			.ambientColor(0.3F, 0.3F, 0.41F)
			.fill(0.35F, 0.5F, 0.8F, 0.17F, 0.2F, 0.3F)
			.rim(1.0F, 0.74F, 0.48F, 0.3F)
			.wrap(0.55F);
		s.camera().idle(IdleMotion.sway(28000.0F, 2.2F, 0.35F, 0.03F));
		s.fog(true, 58.0F, 200.0F, -2122108);
		CampBuild.farLand(s, 7, 4, 1).at(-52.0F, -5.0F, -78.0F).scale(1.5F);
		CampBuild.farLand(s, 9, 5, 2).at(58.0F, 2.0F, -104.0F).scale(1.7F);
		CampBuild.farLand(s, 5, 3, 3).at(-14.0F, 9.0F, -150.0F).scale(1.8F);
		((FxNode)s.add(FxNode.clouds(9, 150.0F, 30.0F, 0.0F, 5.0F, 5L, 0.3F).color(-1002332).opacity(0.5F))).at(0.0F, 13.0F, -78.0F);
		((FxNode)s.add(FxNode.clouds(8, 120.0F, 34.0F, 0.0F, 4.0F, 7L, 0.22F).color(-6651744).opacity(0.36F))).at(0.0F, -10.0F, -40.0F);
		this.fireGlow = (FxNode)s.add(FxNode.glow(4.6F, 1459591744).fade(1.6F));
		this.fireGlow.at(0.0F, 0.02F, 3.0F);
		this.fireCore = (FxNode)s.add(FxNode.glow(1.5F, 1962920048).fade(1.4F));
		this.fireCore.at(0.0F, 0.03F, 3.0F);
		s.add(this.bannerPivot);
		s.add(this.towerPivot);
		this.fireSmoke = (SmokeNode)s.add(
			new SmokeNode(com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode.Kind.COSY, 16, 4L)
				.rise(5.5F, 0.8F)
				.spread(0.55F)
				.wind(0.28F, -0.06F)
				.size(0.4F, 1.7F)
				.colors(-2033932144, 5853272)
		);
		this.fireSmoke.at(0.0F, 0.7F, 3.0F);
		this.chimneySmoke = (SmokeNode)s.add(
			new SmokeNode(com.onehumanawa.cfhcore.content.gui.core.stage.node.SmokeNode.Kind.COSY, 14, 9L)
				.rise(6.0F, 0.7F)
				.spread(0.7F)
				.wind(0.32F, -0.05F)
				.size(0.5F, 2.1F)
				.colors(1889967272, 5262428)
		);
		this.embers = (MotesNode)s.add(new MotesNode(22, 0.8F, 2.6F, 0.8F, 0.032F, -654331824, 2L).rise(5.0F).sway(1.5F));
		this.embers.at(0.0F, 0.6F, 3.0F);
		this.fireflies = (MotesNode)s.add(new MotesNode(40, 26.0F, 4.5F, 15.0F, 0.04F, 1895819700, 9L).sway(2.2F));
		this.fireflies.at(0.0F, 0.5F, -1.5F);
		s.add(new CampScene.Clock());
		s.add(this.driver);
		this.driver.onFrame(this::frame);
	}

	void show(boolean camp, CampBuild.Tier tier, int colour, @Nullable BannerData flag) {
		DyeColor cloth = Build.dye(colour);
		String key = camp + "/" + tier + "/" + cloth.getName();
		if (this.built.isEmpty()) {
			this.grown.snap(camp ? 1.0F : 0.0F);
		} else {
			this.grown.set(camp ? 1.0F : 0.0F);
		}

		if (!key.equals(this.built)) {
			this.rebuild(camp, tier, cloth, this.built.isEmpty());
		}

		this.built = key;
		this.camp = camp;
		String look = camp ? (flag == null ? "-" : flag.signature()) : "";
		if (!look.equals(this.bannerLook)) {
			this.bannerLook = look;
			if (!camp) {
				if (this.banner != null) {
					this.stage.remove(this.banner);
					this.banner = null;
				}

				if (this.towerBanner != null) {
					this.stage.remove(this.towerBanner);
					this.towerBanner = null;
				}
			} else {
				if (this.banner == null) {
					this.banner = (BlockNode)this.stage.add(BannerNodes.hanging(this.stage.level(), flag));
					this.banner.at(0.0F, -0.94F, 0.38F).attachTo(this.bannerPivot);
					this.banner.tint(-864848);
					if (this.rising.containsKey("mast")) {
						this.bannerFrom = this.rising.get("mast") + 560.0F;
					} else {
						this.bannerFrom = this.hoist ? this.clock : -1.0E9F;
					}

					this.hoist = false;
				} else {
					BannerNodes.dress(this.banner, flag);
				}

				if (this.towerBanner != null) {
					BannerNodes.dress(this.towerBanner, flag);
				}
			}
		}

		float[] tower = this.plan != null && camp ? this.plan.towerBanner : null;
		if (tower == null && this.towerBanner != null) {
			this.stage.remove(this.towerBanner);
			this.towerBanner = null;
		}

		if (tower != null && this.towerBanner == null) {
			this.towerBanner = (BlockNode)this.stage.add(BannerNodes.hanging(this.stage.level(), flag));
			this.towerBanner.at(0.0F, 0.0F, 0.0F).attachTo(this.towerPivot);
			this.towerBanner.tint(-2048856);
			this.towerPivot.at(tower[0], tower[1], tower[2]);
		}
	}

	private void rebuild(boolean camp, CampBuild.Tier tier, DyeColor cloth, boolean first) {
		List<String> had = new ArrayList<>();
		if (this.plan != null) {
			for (CampBuild.Part part : this.plan.parts) {
				had.add(part.key());
				this.stage.remove(part.node());
			}
		}

		for (StageNode pool : this.pools) {
			this.stage.remove(pool);
		}

		this.pools.clear();
		CampBuild.Plan made = CampBuild.build(this.stage, tier, cloth, camp);
		this.plan = made;
		Map<String, Float> kept = new HashMap<>(this.rising);
		this.rising.clear();
		int fresh = 0;

		for (CampBuild.Part part : made.parts) {
			if (part.order() != 0) {
				if (first) {
					if (this.full) {
						this.rising.put(part.key(), 520.0F + (float)part.order() * 150.0F);
					}
				} else if (!had.contains(part.key())) {
					this.rising.put(part.key(), this.clock + 120.0F + (float)(fresh++) * 150.0F);
				} else if (kept.containsKey(part.key())) {
					this.rising.put(part.key(), kept.get(part.key()));
				}
			}
		}

		this.hoist = !first && camp && this.banner == null;
		StageLighting light = this.stage.lighting();
		light.clearLamps();
		if (camp) {
			int fire = light.lamp(0.0F, 0.9F, 3.2F, 1.75F, 0.84F, 0.34F, 11.0F);
			light.lampFlicker(fire, 0.34F);
		}

		for (CampBuild.Lantern l : made.lanterns) {
			int lamp = light.lamp(l.x(), l.y(), l.z(), 0.95F, 0.64F, 0.33F, 5.0F);
			if (lamp < 0) {
				break;
			}

			light.lampFlicker(lamp, 0.07F);
			FxNode pool = (FxNode)this.stage.add(FxNode.glow(2.1F, 956286074).fade(1.7F));
			pool.at(l.x(), 0.025F, l.z());
			this.pools.add(pool);
		}

		this.bannerPivot.at(-2.0F, made.mastTop - 0.1F, -4.8F);
		if (made.chimney != null) {
			this.chimneySmoke.at(made.chimney[0], made.chimney[1], made.chimney[2]);
		}

		this.airLast();
	}

	private void airLast() {
		for (StageNode air : new StageNode[]{this.fireSmoke, this.chimneySmoke, this.embers, this.fireflies}) {
			this.stage.nodes().remove(air);
			this.stage.nodes().add(air);
		}
	}

	private float risen(String key) {
		Float from = this.rising.get(key);
		return from == null ? 1.0F : Mth.clamp((this.clock - from) / 560.0F, 0.0F, 1.0F);
	}

	private float settled() {
		float last = 0.0F;

		for (float from : this.rising.values()) {
			last = Math.max(last, from + 560.0F);
		}

		return last;
	}

	void onPick(@Nullable Consumer<CampScene.Person> listener) {
		this.onPick = listener;
	}

	void select(@Nullable UUID id) {
		this.selected = id;
	}

	void people(List<CampScene.Person> people) {
		boolean same = people.size() == this.figures.size();

		for (int i = 0; same && i < people.size(); i++) {
			CampScene.Person a = people.get(i);
			CampScene.Person b = this.figures.get(i).person;
			same = a.id().equals(b.id()) && a.kind() == b.kind() && a.standing() == b.standing();
		}

		if (same) {
			for (int i = 0; i < people.size(); i++) {
				retitle(this.figures.get(i), people.get(i));
			}
		} else {
			for (CampScene.Figure f : this.figures) {
				this.stage.remove(f.body);
				this.stage.remove(f.pool);
				this.stage.remove(f.root);
			}

			boolean again = !this.figures.isEmpty();
			this.figures.clear();
			Theme t = Theme.current();
			float first = again ? this.clock : Math.max(this.clock, this.settled() - 250.0F);
			int n = people.size();

			for (int i = 0; i < n; i++) {
				CampScene.Person p = people.get(i);
				PlayerNode body = body(p.id(), p.name());
				body.pose(p.kind() == CampScene.Kind.REQUEST ? Pose.WAVE : (p.standing() ? Pose.STAND : Pose.SIT));
				body.lookAtCursor(false);
				body.hoverFeel(0.03F, 1.04F);
				body.onClick(() -> {
					if (this.onPick != null) {
						this.onPick.accept(p);
					}
				});
				FxNode pool = FxNode.glow(0.85F, Colors.withAlpha(Colors.lerp(-5688, t.accent(), t.isVanilla() ? 0.0F : 0.5F), 112)).fade(1.6F);
				CampScene.Figure f = new CampScene.Figure(p, body, pool, first + (float)i * 75.0F);
				this.stage.add(f.root);
				((FxNode)this.stage.add(pool)).at(0.0F, 0.03F, 0.0F).attachTo(f.root);
				((PlayerNode)this.stage.add(body)).attachTo(f.root);
				this.figures.add(f);
				retitle(f, p);
			}

			this.arrange();
			this.airLast();
		}
	}

	private void arrange() {
		int n = this.figures.size();
		int wing = Math.max(1, (n + 1) / 2);
		this.span = Mth.clamp(spanFor(wing, this.aspect), spanNear(this.viewH), spanFar(this.viewH));
		float half = (this.span * this.aspect / 2.0F - 1.3F) / 1.12F;
		float pace = wing > 1 ? Mth.clamp((half - 1.35F) / (float)(wing - 1), 1.6F, 2.4F) : 1.6F;

		for (int i = 0; i < n; i++) {
			CampScene.Figure f = this.figures.get(i);
			f.x = (i % 2 == 0 ? -1.0F : 1.0F) * (1.35F + (float)(i / 2) * pace);
			f.z = 1.95F - 1.55F * Mth.cos(Mth.clamp(f.x / 7.5F, -1.0F, 1.0F) * (float) (Math.PI / 2)) + (i % 4 < 2 ? 0.1F : -0.14F);
			f.root.at(f.x, 0.0F, f.z).rotate(-Mth.clamp(f.x, -7.0F, 7.0F) * 4.6F + (float)(i % 3 - 1) * 5.0F, 0.0F, 0.0F);
		}
	}

	private static void retitle(CampScene.Figure f, CampScene.Person p) {
		f.body.named(p.name());
	}

	static PlayerNode body(UUID id, String name) {
		Minecraft mc = Minecraft.getInstance();
		if (id.equals(mc.getUser().getProfileId())) {
			return PlayerNode.local();
		} else {
			Supplier<PlayerSkin> looked;
			try {
				looked = mc.getSkinManager().lookupInsecure(new GameProfile(id, name != null && !name.isEmpty() ? name : "?"));
			} catch (Exception var6) {
				PlayerSkin plain = DefaultPlayerSkin.get(id);
				looked = () -> plain;
			}

			Supplier<PlayerSkin> fallback = looked;
			return new PlayerNode(() -> {
				ClientPacketListener connection = mc.getConnection();
				PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
				return info != null ? info.getSkin() : fallback.get();
			});
		}
	}

	void view(CampScene.View v) {
		if (this.viewed) {
			this.viewMix.set(v == CampScene.View.YARD ? 1.0F : 0.0F);
		} else {
			this.viewMix.snap(v == CampScene.View.YARD ? 1.0F : 0.0F);
		}

		this.viewed = true;
	}

	void shape(Rect view) {
		float shape = aspect(view);
		if (shape != this.aspect || view.h() != this.viewH) {
			this.aspect = shape;
			this.viewH = view.h();
			this.arrange();
		}
	}

	private static float aspect(Rect view) {
		return Math.max(0.8F, (float)view.w() / (float)Math.max(1, view.h()));
	}

	private static boolean ranked(int viewH) {
		return viewH >= 132;
	}

	private static int headroom(int viewH) {
		return 13 + 2 * ((ranked(viewH) ? 22 : 12) + 1);
	}

	private static float spanNear(int viewH) {
		float perBlock = Math.max(12.0F, (float)(viewH - headroom(viewH)) / 2.85F);
		return Mth.clamp((float)viewH / perBlock, 3.6F, 7.5F);
	}

	private static float spanFar(int viewH) {
		return Math.max(spanNear(viewH), (float)viewH / 30.0F);
	}

	private static float spanFor(int wing, float aspect) {
		return 2.0F * ((1.35F + (float)(wing - 1) * 1.6F) * 1.12F + 1.3F) / aspect;
	}

	static int fitting(Rect view) {
		float shape = aspect(view);
		float far = spanFar(view.h());

		for (int wing = 5; wing > 1; wing--) {
			if (spanFor(wing, shape) <= far) {
				return wing * 2;
			}
		}

		return 2;
	}

	private void frame(StageRenderContext ctx) {
		float motion = Theme.current().motion();
		this.clock = motion <= 0.0F ? 1.0E7F : this.clock + ctx.deltaMs / motion;
		boolean moving = motion > 0.0F;
		float seconds = ctx.seconds();
		CampBuild.Plan made = this.plan;
		if (made != null) {
			for (CampBuild.Part part : made.parts) {
				float p = this.risen(part.key());
				part.node().visible(p > 0.001F);
				part.node().scale(1.0F, Math.max(0.001F, Ease.OUT_BACK.apply(p)), 1.0F);
			}

			int lamp = this.camp ? 1 : 0;

			for (int i = 0; i < made.lanterns.size() && i < this.pools.size(); lamp++) {
				float on = this.risen(made.lanterns.get(i).part());
				this.stage.lighting().lampStrength(lamp, on);
				this.pools.get(i).alpha(on);
				i++;
			}

			this.chimneySmoke.strength(made.chimney == null ? 0.0F : this.risen("hall"));
			this.chimneySmoke.visible(made.chimney != null);
		}

		float burning = this.camp ? this.risen("fire") : 0.0F;
		if (this.camp) {
			this.stage.lighting().lampStrength(0, burning);
		}

		this.fireGlow.alpha(burning);
		this.fireCore.alpha(burning);
		this.fireGlow.visible(burning > 0.01F);
		this.fireCore.visible(burning > 0.01F);
		this.embers.strength(burning);
		this.fireSmoke.strength(burning);
		BlockNode flag = this.banner;
		if (flag != null && made != null) {
			float up = Mth.clamp((this.clock - this.bannerFrom) / 1000.0F, 0.0F, 1.0F);
			float down = Mth.clamp((this.clock - this.bannerFrom - 280.0F) / 700.0F, 0.0F, 1.0F);
			float top = made.mastTop - 0.1F;
			float foot = top - 4.4F;
			this.bannerPivot.at(-2.0F, Mth.lerp(Ease.OUT_CUBIC.apply(up), foot, top), -4.8F);
			this.bannerPivot.scale(1.5F, 1.5F * Math.max(0.02F, Ease.OUT_BACK.apply(down)), 1.5F);
			this.bannerPivot.visible(up > 0.0F);
			this.bannerPivot
				.rotate(
					moving ? 3.5F * Mth.sin(seconds * 0.52F) + 1.6F * Mth.sin(seconds * 1.31F + 0.7F) : 0.0F,
					moving ? 2.0F + 2.2F * Mth.sin(seconds * 0.93F + 1.1F) : 0.0F,
					0.0F
				);
		}

		if (this.towerBanner != null) {
			this.towerPivot.visible(this.risen("tower") >= 1.0F);
			this.towerPivot.rotate(0.0F, moving ? 1.5F + 1.5F * Mth.sin(seconds * 0.8F) : 0.0F, 0.0F);
		}

		for (CampScene.Figure f : this.figures) {
			f.in = Mth.clamp((this.clock - f.from) / 380.0F, 0.0F, 1.0F);
			f.chosen.set(f.person.id().equals(this.selected));
			float c = f.chosen.get();
			f.root.scale(Math.max(0.001F, Ease.OUT_BACK.apply(f.in)));
			f.root.visible(f.in > 0.0F);
			float yaw = (float)Math.toRadians((double)f.root.yaw());
			f.root.at(f.x + Mth.sin(yaw) * 0.3F * c, 0.0F, f.z + Mth.cos(yaw) * 0.3F * c);
			f.pool.alpha(c);
			f.pool.visible(c > 0.02F);
			CampScene.Person p = f.person;
			f.body.muted(p.kind() == CampScene.Kind.INVITE ? 0.8F : (p.kind() == CampScene.Kind.MEMBER && !p.standing() ? 0.42F : 0.0F));
			if (p.kind() == CampScene.Kind.MEMBER && p.standing()) {
				f.body.pose(f.body.hover() > 0.4F ? Pose.WAVE : Pose.STAND);
			}
		}

		this.spanNow = !(this.spanNow < 0.0F) && moving
			? this.spanNow + (this.span - this.spanNow) * (1.0F - (float)Math.exp((double)(-ctx.deltaMs / 190.0F)))
			: this.span;
		this.camera();
	}

	private void camera() {
		float tan = (float)Math.tan(Math.toRadians(15.0));
		float g = this.grown.get();
		float far = Math.max(16.4F / (tan * this.aspect), Mth.lerp(g, 12.6F, 15.6F) / (2.0F * tan));
		float down = (float)Math.toRadians(6.5);
		this.aim.set(0.0F, Mth.lerp(g, -0.2F, 1.1F), -2.0F);
		this.eye.set(0.0F, this.aim.y + far * Mth.sin(down), this.aim.z + far * Mth.cos(down));
		float near = Math.max(3.6F, this.spanNow) / (2.0F * tan);
		float level = (float)Math.toRadians(5.0);
		this.aimYard.set(0.0F, Math.max(3.6F, this.spanNow) / 2.0F - 0.75F, 1.1F);
		this.eyeYard.set(0.0F, this.aimYard.y + near * Mth.sin(level), this.aimYard.z + near * Mth.cos(level));
		float m = this.viewMix.get();
		this.eye.lerp(this.eyeYard, m);
		this.aim.lerp(this.aimYard, m);
		float away = 1.0F - Ease.OUT_CUBIC.apply(Mth.clamp(this.clock / this.arriveMs, 0.0F, 1.0F));
		if (away > 0.0F) {
			float reach = this.full ? 1.0F : 0.3F;
			this.eye.add(-8.0F * away * reach, 5.5F * away * reach, 12.0F * away * reach);
			this.aim.add(0.0F, 0.8F * away * reach, 0.0F);
		}

		this.stage.camera().at(this.eye.x, this.eye.y, this.eye.z).lookAt(this.aim.x, this.aim.y, this.aim.z).fov(30.0F).clip(0.1F, 420.0F);
	}

	void drawTags(GuiGraphics g, Rect view) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		Font font = CFHGuiDraw.font();
		float yard = this.viewMix.get();
		List<CampScene.Tag> tags = new ArrayList<>();

		for (CampScene.Figure f : this.figures) {
			float in = Mth.clamp(f.in, 0.0F, 1.0F);
			if (!(in < 0.5F)) {
				float lit = Math.max(Math.max(f.chosen.get(), f.body.hover()), f.body.focus());
				float show = Math.max(yard, lit);
				if (!(show < 0.02F)) {
					Vector3f at = f.root.position();
					float top = (f.person.kind() == CampScene.Kind.MEMBER && !f.person.standing() ? 1.42F : 1.95F) * in + 0.14F;
					if (this.stage.camera().project(at.x, top, at.z, this.ndc)) {
						CampScene.Tag tag = new CampScene.Tag();
						tag.figure = f;
						tag.alpha = (in - 0.5F) * 2.0F * show;
						tag.depth = this.ndc.z;
						tag.headX = view.x() + Math.round((this.ndc.x * 0.5F + 0.5F) * (float)view.w());
						tag.headY = view.y() + Math.round((0.5F - this.ndc.y * 0.5F) * (float)view.h());
						tags.add(tag);
					}
				}
			}
		}

		if (!tags.isEmpty()) {
			int room = 116;

			for (CampScene.Tag a : tags) {
				for (CampScene.Tag b : tags) {
					if (a != b) {
						room = Math.min(room, Math.abs(a.headX - b.headX) * 2 - 6);
					}
				}
			}

			room = Math.max(40, room);
			boolean ranks = yard > 0.5F && ranked(view.h());

			for (CampScene.Tag tag : tags) {
				CampScene.Person who = tag.figure.person;
				tag.name = CFHGuiDraw.truncate(Fonts.heading(who.name()), room - 6);
				tag.line = ranks ? CFHGuiDraw.truncate(who.line(), room - 13) : FormattedCharSequence.EMPTY;
				tag.nameW = font.width(tag.name);
				tag.lineW = font.width(tag.line);
				tag.w = Math.max(tag.nameW, tag.lineW > 0 ? tag.lineW + 7 : 0) + 6;
				tag.h = tag.lineW > 0 ? 22 : 12;
				tag.x = Mth.clamp(tag.headX - tag.w / 2, view.x() + 2, Math.max(view.x() + 2, view.right() - 2 - tag.w));
				tag.y = tag.headY - tag.h - 1;
			}

			tags.sort((ax, bx) -> Float.compare(ax.depth, bx.depth));

			for (int i = 0; i < tags.size(); i++) {
				CampScene.Tag tag = tags.get(i);
				boolean moved = true;

				for (int guard = 0; moved && guard < 12; guard++) {
					moved = false;

					for (int j = 0; j < i; j++) {
						CampScene.Tag o = tags.get(j);
						if (tag.x < o.x + o.w + 1 && o.x < tag.x + tag.w + 1 && tag.y < o.y + o.h + 1 && o.y < tag.y + tag.h + 1) {
							tag.y = o.y - tag.h - 1;
							moved = true;
						}
					}
				}

				tag.y = Math.max(view.y() + (tag.figure.person.crown() ? 13 : 2), tag.y);
			}

			g.enableScissor(view.x(), view.y(), view.right(), view.bottom());

			for (CampScene.Tag tag : tags) {
				CampScene.Figure fx = tag.figure;
				CampScene.Person who = fx.person;
				float a = tag.alpha;
				boolean sel = who.id().equals(this.selected);
				boolean dim = who.kind() == CampScene.Kind.INVITE || who.kind() == CampScene.Kind.MEMBER && !who.standing();
				float lit = Math.max(Math.max(fx.chosen.get(), fx.body.hover()), fx.body.focus());
				int x = tag.x;
				int y = tag.y;
				int w = tag.w;
				int h = tag.h;
				int cx = x + w / 2;
				if (tag.headY - (y + h) > 3) {
					CFHGuiDraw.vline(g, tag.headX, y + h, tag.headY - (y + h) - 1, Colors.scaleAlpha(van ? -1862270977 : Colors.withAlpha(p.textMuted(), 144), a));
				}

				int plate = van ? Colors.withAlpha(-16777216, Math.round(96.0F + 72.0F * lit)) : Colors.withAlpha(p.bg(), Math.round(140.0F + 84.0F * lit));
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(plate, a), van ? 0 : 2);
				if (sel) {
					CFHGuiDraw.rect(g, x + 2, y + h - 1, w - 4, 1, Colors.scaleAlpha(van ? -1 : p.accent(), a));
				}

				if (fx.body.focused() && CFHGuiDraw.focusShown()) {
					CFHGuiDraw.outline(g, x - 1, y - 1, w + 2, h + 2, Colors.scaleAlpha(van ? -1 : p.accent(), a * fx.body.focus()), van ? 0 : 2);
				}

				int ink = dim ? (van ? -4144960 : p.textMuted()) : (van ? -1 : p.text());
				g.drawString(font, tag.name, cx - tag.nameW / 2, y + 2, Colors.scaleAlpha(Colors.lerp(ink, van ? -1 : p.accentInk(), sel ? 0.55F : 0.0F), a), true);
				if (tag.lineW > 0) {
					int lx = cx - (tag.lineW + 7) / 2;
					CFHGuiDraw.rect(g, lx, y + 14, 4, 4, Colors.scaleAlpha(who.lineColor(), a));
					g.drawString(font, tag.line, lx + 7, y + 12, Colors.scaleAlpha(van ? -3092272 : p.textMuted(), a), false);
				}

				if (who.crown()) {
					Icons.draw(g, Icon.CROWN, cx - 5, y - 10, 12, Colors.scaleAlpha(-1073741824, a));
					Icons.draw(g, Icon.CROWN, cx - 6, y - 11, 12, Colors.scaleAlpha(van ? -10402 : p.warning(), a));
				}
			}

			g.disableScissor();
		}
	}

	void close() {
		this.figures.clear();
		this.stage.close();
	}

	private final class Clock extends PivotNode {
		public void tick(StageRenderContext ctx) {
			CampScene.this.ticks++;
			if (CampScene.this.stage.level().getLevelData() instanceof ClientLevelData data) {
				data.setGameTime(CampScene.this.ticks);
			}
		}
	}

	static final class Figure {
		final CampScene.Person person;
		final PivotNode root = new PivotNode();
		final PlayerNode body;
		final FxNode pool;
		private final Anim chosen = new Anim(0.0F, 200, Ease.OUT_CUBIC);
		private float x;
		private float z;
		private final float from;
		private float in;

		private Figure(CampScene.Person person, PlayerNode body, FxNode pool, float from) {
			this.person = person;
			this.body = body;
			this.pool = pool;
			this.from = from;
		}
	}

	static enum Kind {
		MEMBER,
		REQUEST,
		INVITE,
		ALONE;
	}

	static record Person(UUID id, String name, CampScene.Kind kind, boolean standing, Component line, int lineColor, boolean crown) {
	}

	private static final class Tag {
		CampScene.Figure figure;
		int headX;
		int headY;
		int x;
		int y;
		int w;
		int h;
		int nameW;
		int lineW;
		float depth;
		float alpha;
		FormattedCharSequence name;
		FormattedCharSequence line;
	}

	static enum View {
		CAMP,
		YARD;
	}
}
