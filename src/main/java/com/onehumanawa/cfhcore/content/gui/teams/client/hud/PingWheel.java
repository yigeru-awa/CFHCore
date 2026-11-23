package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class PingWheel {
	private static final int HOLD_MS = 190;
	private static final int REPEAT_MS = 350;
	private static final double BLOCK_REACH = 256.0;
	private static final double ENTITY_REACH = 96.0;
	private static final double OPEN_AIR = 64.0;
	private static final int OUTER = 46;
	private static final int INNER = 19;
	private static final int GAP = 2;
	private static final int PUSH = 3;
	private static final int NONE = -1;
	private static final int[] KINDS = new int[]{0, 1, 2, 3, 4};
	private static final String[] DIGITS = new String[]{"1", "2", "3", "4", "5"};
	private static final float OVER_THE_HUD = 7000.0F;
	private static boolean open;
	private static boolean closing;
	private static boolean pinned;
	private static boolean wasDown;
	private static boolean armed;
	private static long pressedAt;
	private static long lastPing;
	@Nullable
	private static PingWheel.Target target;
	private static float cursorX;
	private static float cursorY;
	private static int hovered = -1;
	private static int picked = -1;
	private static final Anim OPEN = new Anim(0.0F, 180, Ease.OUT_BACK);
	private static final Anim FADE = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	private static final Anim FLASH = new Anim(0.0F, 320, Ease.OUT_CUBIC);
	private static final Anim[] HOVER = new Anim[KINDS.length];
	private static final PingWheel.Ring RING = new PingWheel.Ring(19, 46, KINDS.length, 2);

	static void tick() {
		Minecraft mc = Minecraft.getInstance();
		boolean down = TeamsClient.PING_KEY.isDown();

		while (TeamsClient.PING_KEY.consumeClick()) {
		}

		boolean playing = mc.screen == null && mc.player != null && mc.level != null;
		long now = Clock.nowMs();
		if (open) {
			if (!playing) {
				close(false);
			} else if (!down && !pinned) {
				close(true);
			}
		} else if (!playing) {
			armed = false;
		} else if (down && !wasDown) {
			if (!TeamsState.available()) {
				armed = false;
			} else if (!TeamsState.hasTeam()) {
				armed = false;
				mc.gui.setOverlayMessage(Component.translatable("cfhgui_teams.hud.ping.no_team"), false);
			} else {
				armed = true;
				pressedAt = now;
				target = pick(mc);
			}
		} else if (armed && down && now - pressedAt >= 190L) {
			armed = false;
			open(false);
		} else if (armed && !down) {
			armed = false;
			send(0);
		}

		wasDown = down;
	}

	private static void open(boolean pin) {
		open = true;
		closing = false;
		pinned = pin;
		cursorY = 0.0F;
		cursorX = 0.0F;
		picked = -1;
		hovered = -1;
		OPEN.snap(0.001F);
		OPEN.set(1.0F);
		FADE.snap(0.0F);
		FADE.set(1.0F, 140);

		for (Anim a : HOVER) {
			a.snap(0.0F);
		}
	}

	private static void close(boolean send) {
		if (open) {
			open = false;
			pinned = false;
			closing = true;
			picked = send ? hovered : -1;
			if (picked != -1) {
				send(KINDS[picked]);
				FLASH.snap(1.0F);
				FLASH.set(0.0F);
			}

			FADE.set(0.0F, picked != -1 ? 220 : 110);
		}
	}

	private static void send(int kind) {
		PingWheel.Target t = target;
		long now = Clock.nowMs();
		if (t != null && now - lastPing >= 350L) {
			lastPing = now;
			TeamActions.ping(t.x(), t.y(), t.z(), kind, t.entity());
		}
	}

	@Nullable
	private static PingWheel.Target pick(Minecraft mc) {
		Entity camera = mc.getCameraEntity();
		if (camera != null && mc.level != null) {
			float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
			Vec3 eye = camera.getEyePosition(partial);
			Vec3 look = camera.getViewVector(partial);
			BlockHitResult block = mc.level.clip(new ClipContext(eye, eye.add(look.scale(256.0)), Block.OUTLINE, Fluid.NONE, camera));
			boolean hit = block.getType() != Type.MISS;
			double blockAt = hit ? block.getLocation().distanceTo(eye) : 256.0;
			double reach = Math.min(blockAt, 96.0);
			Vec3 end = eye.add(look.scale(reach));
			AABB box = camera.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
			EntityHitResult entity = ProjectileUtil.getEntityHitResult(camera, eye, end, box, e -> !e.isSpectator() && e.isPickable(), reach * reach);
			if (entity != null) {
				Entity e = entity.getEntity();
				return new PingWheel.Target(e.getX(), e.getY(), e.getZ(), e.getId());
			} else if (hit) {
				return new PingWheel.Target(block.getLocation().x, block.getLocation().y, block.getLocation().z, -1);
			} else {
				Vec3 far = eye.add(look.scale(64.0));
				return new PingWheel.Target(far.x, far.y, far.z, -1);
			}
		} else {
			return null;
		}
	}

	public static boolean isOpen() {
		return open;
	}

	public static boolean hidesCrosshair() {
		return open || closing && FADE.get() > 0.2F;
	}

	public static boolean onMouseLook(double dx, double dy) {
		if (!open) {
			return false;
		} else {
			Minecraft mc = Minecraft.getInstance();
			double perPixel = (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth());
			double sensitivity = 0.6 + (Double)mc.options.sensitivity().get() * 0.8;
			cursorX += (float)(dx * perPixel * sensitivity);
			cursorY += (float)(dy * perPixel * sensitivity);
			float max = 52.0F;
			float d = (float)Math.sqrt((double)(cursorX * cursorX + cursorY * cursorY));
			if (d > 52.0F) {
				cursorX *= 52.0F / d;
				cursorY *= 52.0F / d;
			}

			hover(d >= 11.400001F ? RING.sliceAt(cursorX, cursorY) : -1);
			return true;
		}
	}

	public static boolean onMousePress(int button) {
		if (!open) {
			return false;
		} else {
			if (button == 0) {
				close(true);
			} else if (button == 1) {
				close(false);
			}

			return true;
		}
	}

	public static boolean onKeyPress(int key, int scancode) {
		if (!open) {
			return false;
		} else if (key == 256) {
			close(false);
			return false;
		} else {
			Minecraft mc = Minecraft.getInstance();

			for (int i = 0; i < 9; i++) {
				if (mc.options.keyHotbarSlots[i].matches(key, scancode)) {
					if (i < KINDS.length) {
						hover(i);
						pointAt(i);
						close(true);
					}

					return true;
				}
			}

			return false;
		}
	}

	private static void hover(int slice) {
		if (slice != hovered) {
			hovered = slice;
			if (slice != -1 && open) {
				CFHGuiSounds.tick();
			}
		}
	}

	private static void pointAt(int slice) {
		float a = RING.angle(slice);
		float r = 32.5F;
		cursorX = Mth.cos(a) * 32.5F;
		cursorY = Mth.sin(a) * 32.5F;
	}

	static void render(GuiGraphics g) {
		if (open || closing) {
			Minecraft mc = Minecraft.getInstance();
			float fade = FADE.get();
			if (closing && fade <= 0.01F) {
				closing = false;
			} else if (mc.player != null) {
				Theme t = HudDraw.theme();
				Palette p = t.palette();
				boolean vanilla = t.isVanilla();
				int w = g.guiWidth();
				int h = g.guiHeight();
				int cx = w / 2;
				int cy = h / 2;
				float grown = Mth.clamp(OPEN.get(), 0.0F, 1.2F);
				float scale = closing ? 0.95F + 0.05F * fade : 0.86F + 0.14F * grown;
				int shown = closing ? picked : hovered;

				for (int i = 0; i < HOVER.length; i++) {
					HOVER[i].set(i == shown ? 1.0F : 0.0F);
				}

				float veil = fade * Math.min(1.0F, grown);
				g.fillGradient(0, 0, w, h, Colors.scaleAlpha(805306368, veil), Colors.scaleAlpha(1207959552, veil));
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 7000.0F);
				g.pose().pushPose();
				g.pose().translate((float)cx, (float)cy, 0.0F);
				g.pose().scale(scale, scale, 1.0F);
				float flash = FLASH.get();
				float mid = 32.5F;
				HudDraw.batch(
					g,
					() -> {
						for (int i = 0; i < KINDS.length; i++) {
							float hov = HOVER[i].get();
							float in = closing ? 1.0F : Mth.clamp(grown * 1.7F - (float)i * 0.14F, 0.0F, 1.0F);
							float fa = fade * in;
							int colorx = Pings.color(KINDS[i]);
							float a = RING.angle(i);
							float push = hov * 3.0F - (1.0F - in) * 5.0F;
							int ox = Math.round(Mth.cos(a) * push);
							int oy = Math.round(Mth.sin(a) * push);
							int body;
							int edge;
							if (vanilla) {
								body = Colors.mix(Colors.lerp(-265935322, -264224704, hov), colorx, 0.22F * hov + (i == picked ? 0.4F * flash : 0.0F));
								edge = Colors.lerp(-16777216, -1, hov);
							} else {
								body = Colors.mix(
									Colors.lerp(Colors.withAlpha(p.surface(), 236), Colors.withAlpha(p.surfaceHover(), 244), hov),
									colorx,
									0.2F * hov + (i == picked ? 0.45F * flash : 0.0F)
								);
								edge = Colors.lerp(Colors.lerp(p.border(), p.borderStrong(), 0.4F), colorx, hov);
								RING.draw(g, i, ox + 2, oy + 2, Colors.scaleAlpha(p.shadow(), fa), 0);
							}

							RING.draw(g, i, ox, oy, Colors.scaleAlpha(body, fa), Colors.scaleAlpha(edge, fa));
							int kx = Math.round(Mth.cos(a) * (56.0F + push));
							int ky = Math.round(Mth.sin(a) * (56.0F + push));
							HudDraw.pill(g, kx - 4, ky - 5, 9, 11, fa * 0.9F);
							HudDraw.text(g, DIGITS[i], kx - 2, ky - 3, hov > 0.5F ? HudDraw.textColor() : HudDraw.mutedColor(), fa);
						}

						if (open && shown == -1) {
							int px = Math.round(cursorX);
							int py = Math.round(cursorY);
							CFHGuiDraw.pixelCircle(g, px, py, 2, Colors.scaleAlpha(vanilla ? -16777216 : Colors.withAlpha(p.bg(), 255), fade));
							CFHGuiDraw.pixelCircle(g, px, py, 1, Colors.scaleAlpha(HudDraw.textColor(), fade));
						}
					}
				);

				for (int i = 0; i < KINDS.length; i++) {
					float hov = HOVER[i].get();
					float in = closing ? 1.0F : Mth.clamp(grown * 1.7F - (float)i * 0.14F, 0.0F, 1.0F);
					float a = RING.angle(i);
					float r = 32.5F + hov * 3.0F - (1.0F - in) * 5.0F;
					int color = HudDraw.readable(Pings.color(KINDS[i]));
					int ix = Math.round(Mth.cos(a) * r) - 8;
					int iy = Math.round(Mth.sin(a) * r) - 8;
					Icons.draw(
						g, Pings.icon(KINDS[i]), ix, iy, 16, Colors.scaleAlpha(Colors.lerp(Colors.mix(color, vanilla ? -6645094 : p.textMuted(), 0.25F), color, hov), fade * in)
					);
				}

				g.pose().popPose();
				Component label = (Component)(shown == -1 ? Component.translatable("cfhgui_teams.hud.ping.choose") : Pings.words(KINDS[shown]));
				int lw = CFHGuiDraw.width(label) + (shown == -1 ? 12 : 26);
				int lx = cx - lw / 2;
				int ly = cy + Math.round(68.0F * scale);
				HudDraw.batch(g, () -> {
					HudDraw.notice(g, lx, ly, lw, 16, fade);
					HudDraw.text(g, label, lx + (shown == -1 ? 6 : 20), ly + 4, shown == -1 ? HudDraw.mutedColor() : HudDraw.textColor(), fade);
				});
				if (shown != -1) {
					Icons.draw(g, Pings.icon(KINDS[shown]), lx + 6, ly + 4, 8, Colors.scaleAlpha(HudDraw.readable(Pings.color(KINDS[shown])), fade));
				}

				g.pose().popPose();
			}
		}
	}

	static void reset() {
		armed = false;
		pinned = false;
		closing = false;
		open = false;
		target = null;
		FADE.snap(0.0F);
	}

	static void demo(int slice) {
		Minecraft mc = Minecraft.getInstance();
		target = pick(mc);
		open(true);
		if (slice >= 0 && slice < KINDS.length) {
			pointAt(slice);
			hovered = slice;
		}
	}

	static void demoClose(boolean send) {
		close(send);
	}

	private PingWheel() {
	}

	static {
		for (int i = 0; i < HOVER.length; i++) {
			HOVER[i] = new Anim(0.0F, 120, Ease.OUT_CUBIC);
		}
	}

	private static final class Ring {
		private final int slices;
		private final float step;
		private final int[][] body;
		private final int[][] rim;

		Ring(int inner, int outer, int slices, int gap) {
			this.slices = slices;
			this.step = (float) (Math.PI * 2) / (float)slices;
			int size = outer * 2;
			byte[][] at = new byte[size][size];

			for (int j = 0; j < size; j++) {
				for (int i = 0; i < size; i++) {
					float x = (float)(i - outer) + 0.5F;
					float y = (float)(j - outer) + 0.5F;
					float d = (float)Math.sqrt((double)(x * x + y * y));
					at[j][i] = -1;
					if (!(d < (float)inner) && !(d > (float)outer)) {
						float rel = (float)Math.atan2((double)x, (double)(-y)) + this.step / 2.0F;
						if (rel < 0.0F) {
							rel += (float) (Math.PI * 2);
						}

						int k = Math.min(slices - 1, (int)(rel / this.step));
						float in = rel - (float)k * this.step;
						if (!(d * Mth.sin(Math.min(in, this.step - in)) < (float)gap / 2.0F)) {
							at[j][i] = (byte)k;
						}
					}
				}
			}

			this.body = new int[slices][];
			this.rim = new int[slices][];

			for (int k = 0; k < slices; k++) {
				List<Integer> rows = new ArrayList<>();
				List<Integer> edges = new ArrayList<>();

				for (int j = 0; j < size; j++) {
					int start = -1;
					int edgeStart = -1;

					for (int ix = 0; ix <= size; ix++) {
						boolean in = ix < size && at[j][ix] == k;
						boolean edge = in
							&& (ix == 0 || at[j][ix - 1] != k || ix == size - 1 || at[j][ix + 1] != k || j == 0 || at[j - 1][ix] != k || j == size - 1 || at[j + 1][ix] != k);
						if (in && start < 0) {
							start = ix;
						}

						if (!in && start >= 0) {
							rows.add(j - outer);
							rows.add(start - outer);
							rows.add(ix - outer);
							start = -1;
						}

						if (edge && edgeStart < 0) {
							edgeStart = ix;
						}

						if (!edge && edgeStart >= 0) {
							edges.add(j - outer);
							edges.add(edgeStart - outer);
							edges.add(ix - outer);
							edgeStart = -1;
						}
					}
				}

				this.body[k] = rows.stream().mapToInt(Integer::intValue).toArray();
				this.rim[k] = edges.stream().mapToInt(Integer::intValue).toArray();
			}
		}

		float angle(int k) {
			return (float)k * this.step - (float) (Math.PI / 2);
		}

		int sliceAt(float x, float y) {
			float rel = (float)Math.atan2((double)x, (double)(-y)) + this.step / 2.0F;
			if (rel < 0.0F) {
				rel += (float) (Math.PI * 2);
			}

			return Math.min(this.slices - 1, (int)(rel / this.step));
		}

		void draw(GuiGraphics g, int k, int ox, int oy, int fill, int edge) {
			if (Colors.alpha(fill) > 0) {
				int[] rows = this.body[k];

				for (int n = 0; n < rows.length; n += 3) {
					g.fill(rows[n + 1] + ox, rows[n] + oy, rows[n + 2] + ox, rows[n] + oy + 1, fill);
				}
			}

			if (Colors.alpha(edge) > 0) {
				int[] rows = this.rim[k];

				for (int n = 0; n < rows.length; n += 3) {
					g.fill(rows[n + 1] + ox, rows[n] + oy, rows[n + 2] + ox, rows[n] + oy + 1, edge);
				}
			}
		}
	}

	private static record Target(double x, double y, double z, int entity) {
	}
}
