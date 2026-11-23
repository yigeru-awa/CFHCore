package com.onehumanawa.cfhcore.content.gui.teams.client.ui.map;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.LandNames;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import com.onehumanawa.cfhcore.content.gui.teams.client.WaypointIcons;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.model.ServerInfo;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeammateState;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class ClaimMap extends CFHGuiWidget {
	private static final float[] ZOOMS = new float[]{-2.0F, -1.0F, 0.0F, 1.0F};
	private static final int DEFAULT_ZOOM = 2;
	private static final int BUTTON = 14;
	private static final int BUTTON_GAP = 1;
	private static final int NOTE_MS = 4200;
	private static final int MAX_REQUEST = 1024;
	private static final int B_IN = 0;
	private static final int B_OUT = 1;
	private static final int B_CENTRE = 2;
	private static final int B_TOOL = 3;
	private static final int B_HELP = 4;
	private static final int B_SERVER = 5;
	private static final int[] BUTTONS = new int[]{0, 1, 2, 3, 4};
	private static final int[] OPERATOR_BUTTONS = new int[]{0, 1, 2, 3, 5, 4};
	private static long lastUsedMs;
	private double midX;
	private double midZ;
	private boolean placed;
	private boolean following = true;
	private int zoomStep = 2;
	private final Anim zoom = new Anim(ZOOMS[2], 220, Ease.OUT_CUBIC);
	private boolean anchored;
	private double anchorX;
	private double anchorZ;
	private int anchorSx;
	private int anchorSy;
	private ClaimMap.Tool tool = ClaimMap.Tool.CLAIM;
	private boolean forServer;
	private ClaimMap.Drag drag = ClaimMap.Drag.NONE;
	private int dragButton;
	private int fromCx;
	private int fromCz;
	private int toCx;
	private int toCz;
	private double dragMouseX;
	private double dragMouseY;
	private double dragMidX;
	private double dragMidZ;
	private boolean hasCursor;
	private int cursorCx;
	private int cursorCz;
	@Nullable
	private Component note;
	private boolean noteBad;
	private long noteAtMs;
	private final Anim[] buttonHover = new Anim[6];
	private int hoverButton = -1;
	private final Map<UUID, double[]> glide = new HashMap<>();
	private int[] owners = new int[0];
	private boolean[] forcedCells = new boolean[0];
	private final List<ClaimMap.Look> looks = new ArrayList<>();
	private final Map<UUID, Integer> lookIndex = new HashMap<>();

	public ClaimMap(int x, int y, int width, int height) {
		super(x, y, width, height, TeamsText.t("map.title"));

		for (int i = 0; i < this.buttonHover.length; i++) {
			this.buttonHover[i] = new Anim(0.0F, 130, Ease.OUT_CUBIC);
		}
	}

	public ClaimMap.Tool tool() {
		return this.tool;
	}

	public ClaimMap tool(ClaimMap.Tool tool) {
		this.tool = tool;
		return this;
	}

	public void centre() {
		this.following = true;
		this.anchored = false;
	}

	public void zoomBy(int steps) {
		this.zoomTo(this.zoomStep + steps, -1.0, -1.0);
	}

	public static long lastUsedMs() {
		return lastUsedMs;
	}

	public static void used() {
		lastUsedMs = System.currentTimeMillis();
	}

	@Nullable
	public static Component whyNot() {
		ServerInfo info = TeamsView.info();
		if (!TeamsView.available()) {
			return TeamsText.shared("state.no_server");
		} else if (!info.claims) {
			return TeamsText.shared("result.claims_disabled");
		} else if (!TeamsView.claimsLand()) {
			return TeamsText.t("map.why.no_team");
		} else if (!info.claimDimensions.isEmpty() && !info.claimDimensions.contains(TeamsView.dimension())) {
			return TeamsText.shared("result.wrong_dimension");
		} else if (!TeamsView.mayClaim()) {
			TeamData t = TeamsView.land();
			return TeamsText.t("map.why.rank", TeamsText.rank(t == null ? null : t.land.claimRank));
		} else {
			return null;
		}
	}

	private static boolean operator() {
		return TeamsView.available() && !TeamsView.preview() && TeamsView.info().admin;
	}

	private boolean serverMode() {
		return this.forServer && operator();
	}

	@Nullable
	private Component blocked() {
		return this.serverMode() ? null : whyNot();
	}

	private float scale() {
		return (float)Math.pow(2.0, (double)this.zoom.get());
	}

	private int innerX() {
		return this.getX() + 1;
	}

	private int innerY() {
		return this.getY() + 1;
	}

	private int innerW() {
		return this.getWidth() - 2;
	}

	private int innerH() {
		return this.getHeight() - 2;
	}

	private double viewX() {
		return (double)this.innerX() + (double)this.innerW() / 2.0;
	}

	private double viewY() {
		return (double)this.innerY() + (double)this.innerH() / 2.0;
	}

	private int px(double worldX, float s) {
		return (int)Math.round(this.viewX() + (worldX - this.midX) * (double)s);
	}

	private int py(double worldZ, float s) {
		return (int)Math.round(this.viewY() + (worldZ - this.midZ) * (double)s);
	}

	private double worldX(double sx, float s) {
		return this.midX + (sx - this.viewX()) / (double)s;
	}

	private double worldZ(double sy, float s) {
		return this.midZ + (sy - this.viewY()) / (double)s;
	}

	private boolean inside(double mx, double my) {
		return mx >= (double)this.innerX()
			&& mx < (double)(this.innerX() + this.innerW())
			&& my >= (double)this.innerY()
			&& my < (double)(this.innerY() + this.innerH());
	}

	private static Vec3 playerPosition() {
		Vec3 p = TeamsView.position();
		return p == null ? Vec3.ZERO : p;
	}

	public static boolean eligible(ClaimMap.Gesture kind, @Nullable TeamsState.Claim claim) {
		return eligible(kind, claim, false);
	}

	public static boolean eligible(ClaimMap.Gesture kind, @Nullable TeamsState.Claim claim, boolean server) {
		if (server) {
			return switch (kind) {
				case CLAIM -> claim == null;
				case UNCLAIM -> claim != null && TeamsState.isServerLand(claim.team());
				default -> false;
			};
		} else {
			TeamData team = TeamsView.land();
			boolean own = claim != null && team != null && team.id.equals(claim.team());

			return switch (kind) {
				case CLAIM -> claim == null;
				case UNCLAIM -> own;
				case LOAD -> own && !claim.forced();
				case UNLOAD -> own && claim.forced();
			};
		}
	}

	public static List<Long> chunks(ClaimMap.Gesture kind, int ax, int az, int bx, int bz) {
		return chunks(kind, ax, az, bx, bz, false);
	}

	public static List<Long> chunks(ClaimMap.Gesture kind, int ax, int az, int bx, int bz, boolean server) {
		String dim = TeamsView.dimension();
		List<Long> out = new ArrayList<>();
		int x0 = Math.min(ax, bx);
		int x1 = Math.max(ax, bx);
		int z0 = Math.min(az, bz);
		int z1 = Math.max(az, bz);

		for (int z = z0; z <= z1 && out.size() < 1024; z++) {
			for (int x = x0; x <= x1 && out.size() < 1024; x++) {
				if (eligible(kind, TeamsState.claimAt(dim, x, z), server)) {
					out.add(ChunkPos.asLong(x, z));
				}
			}
		}

		return out;
	}

	public static void send(ClaimMap.Gesture kind, List<Long> chunks, ClaimMap.Notes notes) {
		send(kind, chunks, notes, false);
	}

	public static void send(ClaimMap.Gesture kind, List<Long> chunks, ClaimMap.Notes notes, boolean server) {
		if (chunks.isEmpty()) {
			notes.say(TeamsText.t(switch (kind) {
				case CLAIM -> "map.none.claim";
				case UNCLAIM -> server ? "map.server.none.unclaim" : "map.none.unclaim";
				case LOAD -> "map.none.load";
				case UNLOAD -> "map.none.unload";
			}), true);
		} else {
			String dim = TeamsView.dimension();
			int asked = chunks.size();
			switch (kind) {
				case CLAIM:
					if (server) {
						TeamActions.claimServerLand(dim, chunks, r -> answered(kind, asked, r, notes, true));
					} else {
						TeamActions.claim(dim, chunks, r -> answered(kind, asked, r, notes, false));
					}
					break;
				case UNCLAIM:
					if (server) {
						TeamActions.unclaimServerLand(dim, chunks, r -> answered(kind, asked, r, notes, true));
					} else {
						TeamActions.unclaim(dim, chunks, r -> answered(kind, asked, r, notes, false));
					}
					break;
				case LOAD:
					TeamActions.forceLoad(dim, chunks, true, r -> answered(kind, asked, r, notes, false));
					break;
				case UNLOAD:
					TeamActions.forceLoad(dim, chunks, false, r -> answered(kind, asked, r, notes, false));
			}

			CFHGuiSounds.click();
		}
	}

	private static void answered(ClaimMap.Gesture kind, int asked, TeamsNet.Result result, ClaimMap.Notes notes, boolean server) {
		if (!result.ok()) {
			notes.say(TeamActions.describe(result), true);
		} else {
			int done = asked;
			int skipped = 0;

			try {
				if (!result.args().isEmpty()) {
					done = Integer.parseInt(result.args().get(0).trim());
				}

				if (result.args().size() > 1) {
					skipped = Integer.parseInt(result.args().get(1).trim());
				}
			} catch (NumberFormatException var8) {
			}
			String key = switch (kind) {
				case CLAIM -> server ? "map.server.done.claim" : "map.done.claim";
				case UNCLAIM -> server ? "map.server.done.unclaim" : "map.done.unclaim";
				case LOAD -> "map.done.load";
				case UNLOAD -> "map.done.unload";
			};
			notes.say(skipped > 0 ? TeamsText.t(key + ".skipped", TeamsText.chunks(done), skipped) : TeamsText.t(key, TeamsText.chunks(done)), false);
		}
	}

	@Nullable
	public static Component overLimit(ClaimMap.Gesture kind, int count) {
		ServerInfo info = TeamsView.info();
		if (kind == ClaimMap.Gesture.CLAIM && TeamsView.held() + count > info.maxClaims) {
			return TeamsText.t("map.over", count, Math.max(0, info.maxClaims - TeamsView.held()));
		} else {
			return kind == ClaimMap.Gesture.LOAD && TeamsView.forced() + count > info.maxForceLoads
				? TeamsText.t("map.over", count, Math.max(0, info.maxForceLoads - TeamsView.forced()))
				: null;
		}
	}

	public static float claimOpacity(TeamsView.Relation relation) {
		return claimOpacity(relation, relation.ally());
	}

	public static float claimOpacity(TeamsView.Relation relation, boolean marked) {
		float opacity = (float)Mth.clamp(TeamsConfig.get().mapClaimOpacity, 0.1, 0.9);
		return relation == TeamsView.Relation.OWN ? Math.min(0.9F, opacity + 0.14F) : (marked ? opacity : opacity * 0.72F);
	}

	@Nullable
	private static ClaimMap.Gesture gesture(ClaimMap.Drag kind) {
		return switch (kind) {
			case CLAIM -> ClaimMap.Gesture.CLAIM;
			case UNCLAIM -> ClaimMap.Gesture.UNCLAIM;
			case LOAD -> ClaimMap.Gesture.LOAD;
			case UNLOAD -> ClaimMap.Gesture.UNLOAD;
			default -> null;
		};
	}

	private boolean eligible(ClaimMap.Drag kind, @Nullable TeamsState.Claim claim) {
		ClaimMap.Gesture g = gesture(kind);
		return g != null && eligible(g, claim, this.serverMode());
	}

	private List<Long> selected(ClaimMap.Drag kind) {
		ClaimMap.Gesture g = gesture(kind);
		return (List<Long>)(g == null ? new ArrayList<>() : chunks(g, this.fromCx, this.fromCz, this.toCx, this.toCz, this.serverMode()));
	}

	private void commit(ClaimMap.Drag kind) {
		ClaimMap.Gesture g = gesture(kind);
		if (g != null) {
			send(g, this.selected(kind), this::say, this.serverMode());
		}
	}

	private void say(Component text, boolean bad) {
		this.note = text;
		this.noteBad = bad;
		this.noteAtMs = Clock.nowMs();
	}

	private void zoomTo(int step, double mx, double my) {
		int s = Mth.clamp(step, 0, ZOOMS.length - 1);
		if (s != this.zoomStep) {
			if (!this.following && mx >= 0.0 && this.inside(mx, my)) {
				float now = this.scale();
				this.anchorX = this.worldX(mx, now);
				this.anchorZ = this.worldZ(my, now);
				this.anchorSx = (int)mx;
				this.anchorSy = (int)my;
				this.anchored = true;
			} else {
				this.anchored = false;
			}

			this.zoomStep = s;
			this.zoom.set(ZOOMS[s]);
			CFHGuiSounds.tick();
		}
	}

	private static int[] buttons() {
		return operator() ? OPERATOR_BUTTONS : BUTTONS;
	}

	private int buttonAt(double mx, double my) {
		int[] shown = buttons();

		for (int i = 0; i < shown.length; i++) {
			int bx = this.buttonX();
			int by = this.buttonY(i);
			if (mx >= (double)bx && mx < (double)(bx + 14) && my >= (double)by && my < (double)(by + 14)) {
				return shown[i];
			}
		}

		return -1;
	}

	private int buttonX() {
		return this.getX() + this.getWidth() - 14 - 4;
	}

	private int buttonY(int i) {
		return this.getY() + 4 + i * 15 + (i >= 3 ? 3 : 0);
	}

	private void press(int button) {
		switch (button) {
			case 0:
				this.zoomTo(this.zoomStep + 1, -1.0, -1.0);
				break;
			case 1:
				this.zoomTo(this.zoomStep - 1, -1.0, -1.0);
				break;
			case 2:
				this.centre();
				CFHGuiSounds.tick();
				break;
			case 3:
				if (!this.serverMode()) {
					this.tool = this.tool == ClaimMap.Tool.CLAIM ? ClaimMap.Tool.KEEP_LOADED : ClaimMap.Tool.CLAIM;
					CFHGuiSounds.tick();
				}
			case 4:
			default:
				break;
			case 5:
				this.forServer = !this.forServer;
				CFHGuiSounds.tick();
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.active && this.visible && this.contains(mouseX, mouseY)) {
			if (button == 0) {
				int b = this.buttonAt(mouseX, mouseY);
				if (b >= 0) {
					this.press(b);
					return true;
				}
			}

			if (this.inside(mouseX, mouseY) && button <= 2) {
				float s = this.scale();
				this.fromCx = this.toCx = Mth.floor(this.worldX(mouseX, s)) >> 4;
				this.fromCz = this.toCz = Mth.floor(this.worldZ(mouseY, s)) >> 4;
				this.dragButton = button;
				this.dragMouseX = mouseX;
				this.dragMouseY = mouseY;
				this.dragMidX = this.midX;
				this.dragMidZ = this.midZ;
				this.hasCursor = false;
				boolean may = this.blocked() == null;
				boolean load = !this.serverMode() && (this.tool == ClaimMap.Tool.KEEP_LOADED || Screen.hasShiftDown());
				if (button == 2 || !may) {
					this.drag = ClaimMap.Drag.PAN;
				} else if (button == 0) {
					this.drag = load ? ClaimMap.Drag.LOAD : ClaimMap.Drag.CLAIM;
				} else {
					this.drag = load ? ClaimMap.Drag.UNLOAD : ClaimMap.Drag.UNCLAIM;
				}

				return true;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.drag != ClaimMap.Drag.NONE;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return this.drag != ClaimMap.Drag.NONE;
	}

	public void onClick(double mouseX, double mouseY) {
	}

	public void playDownSound(SoundManager handler) {
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.visible && this.inside(mouseX, mouseY) && scrollY != 0.0) {
			this.zoomTo(this.zoomStep + (scrollY > 0.0 ? 1 : -1), mouseX, mouseY);
			return true;
		} else {
			return false;
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			switch (keyCode) {
				case 32:
				case 76:
				case 257:
				case 259:
				case 261:
				case 335:
					if (!this.hasCursor) {
						return false;
					} else {
						Component why = this.blocked();
						if (why != null) {
							this.say(why, true);
							return true;
						} else {
							if (keyCode == 76 && this.serverMode()) {
								this.say(TeamsText.t("map.server.no_load"), true);
								return true;
							}

							TeamsState.Claim at = TeamsState.claimAt(TeamsView.dimension(), this.cursorCx, this.cursorCz);
							this.fromCx = this.toCx = this.cursorCx;
							this.fromCz = this.toCz = this.cursorCz;
							ClaimMap.Drag kind = keyCode == 76
								? (at != null && at.forced() ? ClaimMap.Drag.UNLOAD : ClaimMap.Drag.LOAD)
								: (keyCode != 261 && keyCode != 259 ? ClaimMap.Drag.CLAIM : ClaimMap.Drag.UNCLAIM);
							this.commit(kind);
							return true;
						}
					}
				case 45:
				case 333:
					this.zoomTo(this.zoomStep - 1, -1.0, -1.0);
					return true;
				case 61:
				case 334:
					this.zoomTo(this.zoomStep + 1, -1.0, -1.0);
					return true;
				case 67:
				case 268:
					this.centre();
					this.hasCursor = false;
					return true;
				case 262:
				case 263:
				case 264:
				case 265:
					if (!this.hasCursor) {
						Vec3 p = playerPosition();
						this.cursorCx = Mth.floor(this.following ? p.x : this.midX) >> 4;
						this.cursorCz = Mth.floor(this.following ? p.z : this.midZ) >> 4;
						this.hasCursor = true;
					} else {
						this.cursorCx += keyCode == 262 ? 1 : (keyCode == 263 ? -1 : 0);
						this.cursorCz += keyCode == 264 ? 1 : (keyCode == 265 ? -1 : 0);
					}

					this.following = false;
					this.anchored = false;
					this.midX = (double)(this.cursorCx * 16 + 8);
					this.midZ = (double)(this.cursorCz * 16 + 8);
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.hasCursor = false;
		}
	}

	private static boolean held(int button) {
		long window = Minecraft.getInstance().getWindow().getWindow();
		return GLFW.glfwGetMouseButton(window, button == 0 ? 0 : (button == 1 ? 1 : 2)) == 1;
	}

	private void followDrag(int mouseX, int mouseY, float s) {
		if (this.drag != ClaimMap.Drag.NONE) {
			boolean down = held(this.dragButton);
			if (this.drag == ClaimMap.Drag.PAN) {
				if (Math.abs((double)mouseX - this.dragMouseX) + Math.abs((double)mouseY - this.dragMouseY) > 2.0) {
					this.following = false;
					this.anchored = false;
					this.midX = this.dragMidX - ((double)mouseX - this.dragMouseX) / (double)s;
					this.midZ = this.dragMidZ - ((double)mouseY - this.dragMouseY) / (double)s;
				}

				if (!down) {
					this.drag = ClaimMap.Drag.NONE;
				}
			} else {
				int mx = Mth.clamp(mouseX, this.innerX(), this.innerX() + this.innerW() - 1);
				int my = Mth.clamp(mouseY, this.innerY(), this.innerY() + this.innerH() - 1);
				this.toCx = Mth.floor(this.worldX((double)mx, s)) >> 4;
				this.toCz = Mth.floor(this.worldZ((double)my, s)) >> 4;
				if (!down) {
					ClaimMap.Drag kind = this.drag;
					this.drag = ClaimMap.Drag.NONE;
					this.commit(kind);
				}
			}
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, false);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, true);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, boolean van) {
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			Theme t = Theme.current();
			Palette p = t.palette();
			lastUsedMs = System.currentTimeMillis();
			Minecraft mc = Minecraft.getInstance();
			int x = this.getX();
			int y = this.getY();
			int w = this.getWidth();
			int h = this.getHeight();
			int ix = this.innerX();
			int iy = this.innerY();
			int iw = this.innerW();
			int ih = this.innerH();
			Vec3 me = playerPosition();
			if (!this.placed) {
				this.midX = me.x;
				this.midZ = me.z;
				this.placed = true;
			}

			float s = this.scale();
			if (this.following && this.drag != ClaimMap.Drag.PAN) {
				float k = t.motion() <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-Clock.frameDelta()) / 90.0);
				this.midX = this.midX + (me.x - this.midX) * (double)k;
				this.midZ = this.midZ + (me.z - this.midZ) * (double)k;
			} else if (this.anchored) {
				this.midX = this.anchorX - ((double)this.anchorSx - this.viewX()) / (double)s;
				this.midZ = this.anchorZ - ((double)this.anchorSy - this.viewY()) / (double)s;
				if (!this.zoom.isAnimating()) {
					this.anchored = false;
				}
			}

			this.followDrag(mouseX, mouseY, s);
			CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(van ? -536870912 : Colors.brighten(p.bg(), -0.3F), a));
			boolean over = this.isHovered() && this.inside((double)mouseX, (double)mouseY) && g.containsPointInScissor(mouseX, mouseY);
			this.hoverButton = this.isHovered() ? this.buttonAt((double)mouseX, (double)mouseY) : -1;
			double left = this.worldX((double)ix, s);
			double top = this.worldZ((double)iy, s);
			double right = this.worldX((double)(ix + iw), s);
			double bottom = this.worldZ((double)(iy + ih), s);
			int cMinX = Mth.floor(left) >> 4;
			int cMinZ = Mth.floor(top) >> 4;
			int cMaxX = Mth.floor(right) >> 4;
			int cMaxZ = Mth.floor(bottom) >> 4;
			float cell = 16.0F * s;
			CFHGuiDraw.scissor(g, ix, iy, iw, ih);
			this.drawLand(g, mc, s, cMinX, cMinZ, cMaxX, cMaxZ, a);
			if (cell >= 6.0F) {
				this.drawGrid(g, s, cMinX, cMinZ, cMaxX, cMaxZ, ix, iy, iw, ih, cell, van, a);
			}

			String dim = TeamsView.dimension();
			this.drawClaims(g, dim, s, cMinX, cMinZ, cMaxX, cMaxZ, cell, a);
			if (this.drag != ClaimMap.Drag.NONE && this.drag != ClaimMap.Drag.PAN) {
				this.drawSelection(g, dim, s, cell, a);
			}

			if (this.hasCursor && this.isFocused()) {
				this.drawCursor(g, s, a);
			}

			int hoverCx = Mth.floor(this.worldX((double)mouseX, s)) >> 4;
			int hoverCz = Mth.floor(this.worldZ((double)mouseY, s)) >> 4;
			int[] counters = countersBox(ix, iy, iw, ih, this.serverMode());
			boolean overCounters = over
				&& counters != null
				&& mouseX >= counters[0]
				&& mouseX < counters[0] + counters[2]
				&& mouseY >= counters[1]
				&& mouseY < counters[1] + counters[3];
			boolean overLand = over && this.hoverButton < 0 && !overCounters;
			if (overLand && this.drag == ClaimMap.Drag.NONE) {
				CFHGuiDraw.outline(
					g,
					this.px((double)(hoverCx * 16), s),
					this.py((double)(hoverCz * 16), s),
					this.px((double)(hoverCx * 16 + 16), s) - this.px((double)(hoverCx * 16), s) + 1,
					this.py((double)(hoverCz * 16 + 16), s) - this.py((double)(hoverCz * 16), s) + 1,
					Colors.scaleAlpha(-1325400065, a),
					0
				);
			}

			Component pointed = this.drawMarks(g, mc, dim, s, me, mouseX, mouseY, overLand, a);
			CFHGuiDraw.unscissor(g);
			this.drawNorth(g, ix, iy, van, a);
			this.drawButtons(g, van, a);
			drawCounters(g, counters, ix, iy, iw, ih, van, a, this.serverMode());
			this.drawPlace(g, ix, iy, iw, ih, overLand ? hoverCx : Mth.floor(this.midX) >> 4, overLand ? hoverCz : Mth.floor(this.midZ) >> 4, van, a);
			this.drawNote(g, ix, iy, iw, van, a);
			if (van) {
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-6250336, -1, Math.max(this.hover(), this.focus())), a), 0);
			} else {
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.border(), p.borderStrong(), this.hover()), a), 0);
				if (this.focus() > 0.02F && CFHGuiDraw.focusShown()) {
					CFHGuiDraw.outline(g, x - 1, y - 1, w + 2, h + 2, Colors.scaleAlpha(p.accent(), this.focus() * a), 0);
				}
			}

			if (over || overCounters) {
				if (this.hoverButton >= 0) {
					CFHGuiTooltips.request(this.buttonTip(this.hoverButton), this);
				} else if (overCounters) {
					CFHGuiTooltips.request(counterTip(this.serverMode()), this);
				} else if (overLand && this.drag == ClaimMap.Drag.NONE) {
					this.tipFor(dim, hoverCx, hoverCz, pointed);
				}
			}
		}
	}

	private void drawLand(GuiGraphics g, Minecraft mc, float s, int cMinX, int cMinZ, int cMaxX, int cMaxZ, float a) {
		if (mc.level != null) {
			TerrainTiles.smooth(s * (float)mc.getWindow().getGuiScale() < 1.0F);
			TerrainTiles.look(cMinX, cMinZ, cMaxX, cMaxZ, Mth.floor(this.midX) >> 4, Mth.floor(this.midZ) >> 4);

			for (int tz = cMinZ >> 3; tz <= cMaxZ >> 3; tz++) {
				for (int tx = cMinX >> 3; tx <= cMaxX >> 3; tx++) {
					ResourceLocation tex = TerrainTiles.texture(tx, tz);
					if (tex != null) {
						int x0 = this.px((double)tx * 128.0, s);
						int y0 = this.py((double)tz * 128.0, s);
						int x1 = this.px((double)(tx + 1) * 128.0, s);
						int y1 = this.py((double)(tz + 1) * 128.0, s);
						CFHGuiDraw.blit(g, tex, x0, y0, x1 - x0, y1 - y0, 0.0F, 0.0F, 128, 128, 128, 128, a);
					}
				}
			}
		}
	}

	private void drawGrid(GuiGraphics g, float s, int cMinX, int cMinZ, int cMaxX, int cMaxZ, int ix, int iy, int iw, int ih, float cell, boolean van, float a) {
		float strength = Mth.clamp((cell - 5.0F) / 11.0F, 0.25F, 1.0F);
		int line = Colors.scaleAlpha(van ? 822083583 : 587202559, a * strength);

		for (int cx = cMinX; cx <= cMaxX + 1; cx++) {
			CFHGuiDraw.vline(g, this.px((double)(cx * 16), s), iy, ih, line);
		}

		for (int cz = cMinZ; cz <= cMaxZ + 1; cz++) {
			CFHGuiDraw.hline(g, ix, this.py((double)(cz * 16), s), iw, line);
		}
	}

	private int lookOf(UUID team, float a) {
		Integer known = this.lookIndex.get(team);
		if (known != null) {
			return known;
		} else {
			TeamSummary summary = TeamsView.summary(team);
			TeamsView.Relation relation = TeamsView.relation(team);
			int rgb = summary == null ? 9079434 : summary.color & 16777215;
			boolean marked = relation.ally() || TeamsState.isServerLand(team);
			float fill = claimOpacity(relation, marked);
			int ink = TeamsDraw.ink(rgb);
			int edge = relation == TeamsView.Relation.OWN
				? Colors.brighten(ink, 0.3F)
				: (marked ? Colors.withAlpha(Colors.brighten(ink, 0.12F), 216) : Colors.withAlpha(ink, 168));
			this.looks
				.add(
					new ClaimMap.Look(
						Colors.scaleAlpha(Colors.withAlpha(rgb, Math.round(255.0F * fill)), a),
						Colors.scaleAlpha(edge, a),
						Colors.scaleAlpha(Colors.withAlpha(ink, 88), a),
						relation
					)
				);
			this.lookIndex.put(team, this.looks.size() - 1);
			return this.looks.size() - 1;
		}
	}

	private void drawClaims(GuiGraphics g, String dim, float s, int cMinX, int cMinZ, int cMaxX, int cMaxZ, float cell, float a) {
		Long2ObjectMap<TeamsState.Claim> claims = TeamsState.claims(dim);
		if (!claims.isEmpty() && TeamsView.available()) {
			int cols = cMaxX - cMinX + 1;
			int rows = cMaxZ - cMinZ + 1;
			if (this.owners.length < cols * rows) {
				this.owners = new int[cols * rows];
				this.forcedCells = new boolean[cols * rows];
			}

			this.looks.clear();
			this.lookIndex.clear();
			boolean any = false;

			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					TeamsState.Claim claim = (TeamsState.Claim)claims.get(ChunkPos.asLong(cMinX + c, cMinZ + r));
					int i = r * cols + c;
					if (claim != null && claim.team() != null) {
						this.owners[i] = this.lookOf(claim.team(), a);
						this.forcedCells[i] = claim.forced();
						any = true;
					} else {
						this.owners[i] = -1;
					}
				}
			}

			if (any) {
				for (int r = 0; r < rows; r++) {
					int y0 = this.py((double)((cMinZ + r) * 16), s);
					int y1 = this.py((double)((cMinZ + r + 1) * 16), s);
					int start = 0;

					for (int cx = 1; cx <= cols; cx++) {
						if (cx >= cols || this.owners[r * cols + cx] != this.owners[r * cols + start]) {
							int o = this.owners[r * cols + start];
							if (o >= 0) {
								g.fill(this.px((double)((cMinX + start) * 16), s), y0, this.px((double)((cMinX + cx) * 16), s), y1, this.looks.get(o).fill());
							}

							start = cx;
						}
					}
				}

				for (int r = 0; r < rows; r++) {
					for (int cxx = 0; cxx < cols; cxx++) {
						int i = r * cols + cxx;
						int o = this.owners[i];
						if (o >= 0) {
							ClaimMap.Look look = this.looks.get(o);
							int x0 = this.px((double)((cMinX + cxx) * 16), s);
							int x1 = this.px((double)((cMinX + cxx + 1) * 16), s);
							int y0 = this.py((double)((cMinZ + r) * 16), s);
							int y1 = this.py((double)((cMinZ + r + 1) * 16), s);
							boolean own = look.relation() == TeamsView.Relation.OWN && cell >= 7.0F;
							if (r > 0 && this.owners[i - cols] != o) {
								g.fill(x0, y0, x1, y0 + 1, look.edge());
								if (own) {
									g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, look.glow());
								}
							}

							if (r < rows - 1 && this.owners[i + cols] != o) {
								g.fill(x0, y1 - 1, x1, y1, look.edge());
								if (own) {
									g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, look.glow());
								}
							}

							if (cxx > 0 && this.owners[i - 1] != o) {
								g.fill(x0, y0, x0 + 1, y1, look.edge());
								if (own) {
									g.fill(x0 + 1, y0 + 1, x0 + 2, y1 - 1, look.glow());
								}
							}

							if (cxx < cols - 1 && this.owners[i + 1] != o) {
								g.fill(x1 - 1, y0, x1, y1, look.edge());
								if (own) {
									g.fill(x1 - 2, y0 + 1, x1 - 1, y1 - 1, look.glow());
								}
							}

							if (this.forcedCells[i]) {
								int mx = (x0 + x1) / 2;
								int my = (y0 + y1) / 2;
								if (cell >= 12.0F) {
									Icons.draw(g, Icon.BOLT, mx - 4, my - 4, 8, Colors.scaleAlpha(-251658241, a));
								} else {
									g.fill(mx - 1, my - 1, mx + 1, my + 1, Colors.scaleAlpha(-251658241, a));
								}
							}
						}
					}
				}
			}
		}
	}

	private int dragColor(Palette p) {
		TeamData team = TeamsView.land();
		if (this.drag == ClaimMap.Drag.CLAIM && this.serverMode()) {
			TeamSummary land = TeamsState.serverLand();
			return TeamsDraw.ink(land == null ? p.accent() & 16777215 : land.color);
		} else {
			return switch (this.drag) {
				case CLAIM -> TeamsDraw.ink(team == null ? p.accent() & 16777215 : team.color);
				case UNCLAIM, UNLOAD -> p.danger();
				default -> p.warning();
			};
		}
	}

	private void drawSelection(GuiGraphics g, String dim, float s, float cell, float a) {
		Palette p = Theme.current().palette();
		int color = this.dragColor(p);
		int x0 = Math.min(this.fromCx, this.toCx);
		int x1 = Math.max(this.fromCx, this.toCx);
		int z0 = Math.min(this.fromCz, this.toCz);
		int z1 = Math.max(this.fromCz, this.toCz);
		float pulse = Theme.current().motion() <= 0.0F ? 0.5F : 0.5F + 0.5F * (float)Math.sin((double)Clock.nowMs() / 170.0);
		int fill = Colors.scaleAlpha(Colors.withAlpha(color, 72 + Math.round(48.0F * pulse)), a);
		int count = 0;

		for (int z = z0; z <= z1; z++) {
			for (int x = x0; x <= x1; x++) {
				if (this.eligible(this.drag, TeamsState.claimAt(dim, x, z))) {
					count++;
					g.fill(this.px((double)(x * 16), s) + 1, this.py((double)(z * 16), s) + 1, this.px((double)(x * 16 + 16), s), this.py((double)(z * 16 + 16), s), fill);
				}
			}
		}

		int sx0 = this.px((double)(x0 * 16), s);
		int sy0 = this.py((double)(z0 * 16), s);
		int sx1 = this.px((double)((x1 + 1) * 16), s);
		int sy1 = this.py((double)((z1 + 1) * 16), s);
		CFHGuiDraw.outline(g, sx0 - 1, sy0 - 1, sx1 - sx0 + 3, sy1 - sy0 + 3, Colors.scaleAlpha(-1073741824, a), 0);
		CFHGuiDraw.outline(g, sx0, sy0, sx1 - sx0 + 1, sy1 - sy0 + 1, Colors.scaleAlpha(Colors.brighten(color, 0.25F), a), 0);
		ClaimMap.Gesture asking = gesture(this.drag);
		Component over = asking != null && !this.serverMode() ? overLimit(asking, count) : null;
		Component label = (Component)(over != null
			? over
			: Component.literal((this.drag != ClaimMap.Drag.CLAIM && this.drag != ClaimMap.Drag.LOAD ? "-" : "+") + count));
		int labelColor = over != null ? p.warning() : color;
		int lw = CFHGuiDraw.width(label) + 8;
		int lx = sx1 + 3;
		if (lx + lw > this.innerX() + this.innerW() - 2) {
			lx = sx0 - lw - 2;
		}

		if (lx < this.innerX() + 2) {
			lx = this.innerX() + 2;
		}

		int ly = Mth.clamp(sy0, this.innerY() + 2, this.innerY() + this.innerH() - 14);
		CFHGuiDraw.rect(g, lx, ly, lw, 12, Colors.scaleAlpha(-804253680, a));
		CFHGuiDraw.chip(g, label, lx, ly, labelColor, a);
	}

	private void drawCursor(GuiGraphics g, float s, float a) {
		int x0 = this.px((double)(this.cursorCx * 16), s);
		int y0 = this.py((double)(this.cursorCz * 16), s);
		int x1 = this.px((double)(this.cursorCx * 16 + 16), s);
		int y1 = this.py((double)(this.cursorCz * 16 + 16), s);
		CFHGuiDraw.outline(g, x0 - 1, y0 - 1, x1 - x0 + 3, y1 - y0 + 3, Colors.scaleAlpha(-805306368, a), 0);
		CFHGuiDraw.outline(g, x0, y0, x1 - x0 + 1, y1 - y0 + 1, Colors.scaleAlpha(Theme.current().accent(), a), 0);
	}

	@Nullable
	private Component drawMarks(GuiGraphics g, Minecraft mc, String dim, float s, Vec3 me, int mouseX, int mouseY, boolean overLand, float a) {
		Component pointed = null;
		TeamData team = TeamsView.team();
		if (team != null) {
			for (TeamData.Waypoint wp : team.waypoints) {
				if (dim.equals(wp.dimension)) {
					int sx = this.px((double)wp.x + 0.5, s);
					int sy = this.py((double)wp.z + 0.5, s);
					if (this.near(sx, sy)) {
						pin(g, WaypointIcons.get(wp.icon), sx, sy, wp.color, a);
						if (overLand && Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5) {
							pointed = Component.literal(wp.name);
						}
					}
				}
			}

			if (team.home != null && dim.equals(team.home.dimension)) {
				int sx = this.px(team.home.x, s);
				int sy = this.py(team.home.z, s);
				if (this.near(sx, sy)) {
					pin(g, Icon.HOME, sx, sy, team.color, a);
					if (overLand && Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5) {
						pointed = TeamsText.t("map.home");
					}
				}
			}
		}

		if (TeamsView.available() && team != null) {
			float k = Theme.current().motion() <= 0.0F ? 1.0F : 1.0F - (float)Math.exp((double)(-Clock.frameDelta()) / 160.0);

			for (TeammateState mate : TeamsState.teammates()) {
				if (dim.equals(mate.dimension())) {
					double[] at = this.glide.computeIfAbsent(mate.id(), id -> new double[]{(double)mate.x(), (double)mate.z()});
					at[0] += ((double)mate.x() - at[0]) * (double)k;
					at[1] += ((double)mate.z() - at[1]) * (double)k;
					int sx = Mth.clamp(this.px(at[0], s), this.innerX() + 6, this.innerX() + this.innerW() - 7);
					int sy = Mth.clamp(this.py(at[1], s), this.innerY() + 6, this.innerY() + this.innerH() - 7);
					String name = TeamsView.nameOf(mate.id());
					int ring = mate.ally() ? -7354369 : TeamsDraw.ink(team.color);
					head(g, mate.id(), name, sx, sy, 8, ring, mate.dead() ? 0.5F * a : a);
					if (overLand && Math.abs(mouseX - sx) <= 5 && Math.abs(mouseY - sy) <= 5) {
						pointed = name.isEmpty()
							? TeamsText.t(mate.ally() ? "map.ally" : "map.teammate")
							: (mate.ally() ? TeamsText.t("map.ally.named", name) : Component.literal(name));
					}
				}
			}
		}

		if (mc.player != null || TeamsView.position() != null) {
			int sx = this.px(me.x, s);
			int sy = this.py(me.z, s);
			if (this.near(sx, sy)) {
				float breath = Theme.current().motion() <= 0.0F ? 0.5F : 0.5F + 0.5F * (float)Math.sin((double)Clock.nowMs() / 420.0);
				CFHGuiDraw.pixelRing(g, sx, sy, 8, Colors.scaleAlpha(Colors.withAlpha(16777215, 48 + Math.round(80.0F * breath)), a));
				head(g, TeamsView.self(), TeamsView.selfName(), sx, sy, 8, -1, a);
				if (mc.player != null) {
					double yaw = Math.toRadians((double)mc.player.getYRot());
					int fx = sx + (int)Math.round(-Math.sin(yaw) * 9.0);
					int fy = sy + (int)Math.round(Math.cos(yaw) * 9.0);
					g.fill(fx - 1, fy - 1, fx + 1, fy + 1, Colors.scaleAlpha(-1, a));
				}

				if (overLand && Math.abs(mouseX - sx) <= 6 && Math.abs(mouseY - sy) <= 6) {
					pointed = TeamsText.t("map.you");
				}
			}
		}

		return pointed;
	}

	private boolean near(int sx, int sy) {
		return sx > this.innerX() - 12 && sx < this.innerX() + this.innerW() + 12 && sy > this.innerY() - 12 && sy < this.innerY() + this.innerH() + 12;
	}

	private static void head(GuiGraphics g, UUID id, String name, int cx, int cy, int size, int ring, float a) {
		int x = cx - size / 2;
		int y = cy - size / 2;
		g.fill(x - 2, y - 2, x + size + 2, y + size + 2, Colors.scaleAlpha(-1342177280, a));
		g.fill(x - 1, y - 1, x + size + 1, y + size + 1, Colors.scaleAlpha(ring, a));
		CFHGuiDraw.playerHead(g, CFHGuiAvatar.skinFor(id, name), x, y, size, a);
	}

	private static void pin(GuiGraphics g, Icon icon, int cx, int cy, int rgb, float a) {
		CFHGuiDraw.pixelCircle(g, cx, cy, 5, Colors.scaleAlpha(-804253680, a));
		CFHGuiDraw.pixelRing(g, cx, cy, 5, Colors.scaleAlpha(TeamsDraw.ink(rgb), a));
		Icons.draw(g, icon, cx - 4, cy - 4, 8, Colors.scaleAlpha(TeamsDraw.ink(rgb), a));
	}

	private void tipFor(String dim, int cx, int cz, @Nullable Component pointed) {
		List<Component> lines = claimLines(dim, cx, cz, pointed);
		if (!lines.isEmpty()) {
			CFHGuiTooltips.request(lines, this);
		}
	}

	public static List<Component> claimLines(String dim, int cx, int cz, @Nullable Component pointed) {
		List<Component> lines = new ArrayList<>();
		if (pointed != null) {
			lines.add(pointed);
		}

		TeamsState.Claim claim = TeamsView.available() ? TeamsState.claimAt(dim, cx, cz) : null;
		if (claim == null) {
			if (pointed == null) {
				lines.add(TeamsText.t("map.unclaimed").withStyle(ChatFormatting.GRAY));
			}
		} else {
			TeamSummary owner = TeamsView.summary(claim.team());
			boolean server = TeamsState.isServerLand(claim.team());
			Component name = owner == null
				? TeamsText.t(server ? "map.server" : "map.unknown_team")
				: LandNames.of(owner).withStyle(st -> st.withColor(TeamsDraw.ink(owner.color) & 16777215));
			lines.add(name);
			if (server) {
				lines.add(TeamsText.t("map.server.land").withStyle(ChatFormatting.GRAY));
			}

			TeamsView.Relation relation = TeamsView.relation(claim.team());

			String rel = switch (relation) {
				case OWN -> {
					if (owner != null && owner.solo) {
						yield "map.rel.own_land";
					}

					yield "map.rel.own";
				}
				case MUTUAL -> "map.rel.mutual";
				case OURS -> "map.rel.ours";
				case THEIRS -> "map.rel.theirs";
				default -> null;
			};
			if (rel != null) {
				lines.add(TeamsText.t(rel).withStyle(ChatFormatting.GRAY));
			}

			if (claim.forced()) {
				lines.add(TeamsText.t("map.kept_loaded").withStyle(ChatFormatting.GRAY));
			}
		}

		return lines;
	}

	private List<Component> buttonTip(int button) {
		return switch (button) {
			case 0 -> List.of(TeamsText.t("map.zoom_in"));
			case 1 -> List.of(TeamsText.t("map.zoom_out"));
			case 2 -> List.of(TeamsText.t("map.centre"));
			case 3 -> this.serverMode()
			? List.of(TeamsText.t("map.tool.claim"), TeamsText.t("map.server.tool.desc").withStyle(ChatFormatting.GRAY))
			: List.of(
				TeamsText.t(this.tool == ClaimMap.Tool.KEEP_LOADED ? "map.tool.load" : "map.tool.claim"),
				TeamsText.t(this.tool == ClaimMap.Tool.KEEP_LOADED ? "map.tool.load.desc" : "map.tool.claim.desc").withStyle(ChatFormatting.GRAY)
			);
			default -> List.of(
			TeamsText.t("map.help"),
			TeamsText.t("map.help.drag").withStyle(ChatFormatting.GRAY),
			TeamsText.t("map.help.right").withStyle(ChatFormatting.GRAY),
			TeamsText.t("map.help.shift").withStyle(ChatFormatting.GRAY),
			TeamsText.t("map.help.view").withStyle(ChatFormatting.GRAY),
			TeamsText.t("map.help.keys").withStyle(ChatFormatting.GRAY)
		);
			case 5 -> List.of(
			TeamsText.t(this.forServer ? "map.server.on" : "map.server.off"),
			TeamsText.t(this.forServer ? "map.server.on.desc" : "map.server.off.desc").withStyle(ChatFormatting.GRAY)
		);
		};
	}

	private static void plate(GuiGraphics g, int x, int y, int w, int h, boolean van, float a) {
		if (van) {
			CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(-1073741824, a));
		} else {
			Theme t = Theme.current();
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(t.palette().bg(), 216), a), 2);
			CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(t.palette().border(), 224), a), 2);
		}
	}

	private void drawNorth(GuiGraphics g, int ix, int iy, boolean van, float a) {
		Palette p = Theme.current().palette();
		Component n = TeamsText.t("map.north");
		int w = CFHGuiDraw.width(n) + 6;
		plate(g, ix + 3, iy + 3, w, 12, van, a);
		g.drawString(CFHGuiDraw.font(), n, ix + 6, iy + 5, Colors.scaleAlpha(van ? -1 : p.textMuted(), a), false);
	}

	private void drawButtons(GuiGraphics g, boolean van, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean server = this.serverMode();
		int[] shown = buttons();

		for (int i = 0; i < shown.length; i++) {
			int id = shown[i];

			Icon icon = switch (id) {
				case 0 -> Icon.PLUS;
				case 1 -> Icon.MINUS;
				case 2 -> Icon.COMPASS;
				case 3 -> this.tool == ClaimMap.Tool.KEEP_LOADED && !server ? Icon.BOLT : Icon.SELECTION;
				default -> Icon.QUESTION;
				case 5 -> Icon.SHIELD;
			};
			Anim hover = this.buttonHover[id];
			hover.set(id == this.hoverButton);
			float hv = hover.get();
			int bx = this.buttonX();
			int by = this.buttonY(i);
			boolean off = id == 0 && this.zoomStep >= ZOOMS.length - 1 || id == 1 && this.zoomStep <= 0 || id == 3 && server;
			boolean on = id == 2 && this.following || id == 3 && this.tool == ClaimMap.Tool.KEEP_LOADED && !server || id == 5 && server;
			if (van) {
				CFHGuiDraw.vanillaButton(g, bx, by, 14, 14, off ? 0.0F : hv, !off, a);
			} else {
				CFHGuiDraw.pixelRound(g, bx, by, 14, 14, Colors.scaleAlpha(Colors.withAlpha(Colors.lerp(p.bg(), p.surfaceHover(), hv), 230), a), 2);
				CFHGuiDraw.outline(g, bx, by, 14, 14, Colors.scaleAlpha(on ? p.accent() : Colors.lerp(p.border(), p.borderStrong(), hv), a), 2);
			}

			int fg = off ? p.textDim() : (on ? (van ? -96 : p.accentInk()) : Colors.lerp(van ? -2039584 : p.textMuted(), van ? -1 : p.text(), hv));
			Icons.draw(g, icon, bx + 3, by + 3, 8, Colors.scaleAlpha(fg, a));
		}
	}

	private static String landText() {
		return TeamsView.held() + " / " + TeamsView.info().maxClaims;
	}

	private static String loadedText() {
		return TeamsView.forced() + " / " + TeamsView.info().maxForceLoads;
	}

	private static Component serverText() {
		TeamSummary land = TeamsState.serverLand();
		return land == null ? TeamsText.t("map.server.counter.none") : TeamsText.t("map.server.counter", land.name, TeamsText.chunks(land.claims));
	}

	@Nullable
	public static int[] countersBox(int ix, int iy, int iw, int ih) {
		return countersBox(ix, iy, iw, ih, false);
	}

	@Nullable
	public static int[] countersBox(int ix, int iy, int iw, int ih, boolean server) {
		if (server) {
			return new int[]{ix + 3, iy + ih - 17, Math.min(iw - 8, CFHGuiDraw.width(serverText()) + 20), 14};
		} else if (!TeamsView.claimsLand()) {
			return null;
		} else {
			int colW = Math.max(CFHGuiDraw.width(landText()), CFHGuiDraw.width(loadedText())) + 12;
			int w = colW * 2 + 9 + (whyNot() != null ? 12 : 0);
			int h = 19;
			return new int[]{ix + 3, iy + ih - 19 - 3, w, 19};
		}
	}

	public static void drawCounters(GuiGraphics g, @Nullable int[] box, int ix, int iy, int iw, int ih, boolean van, float a) {
		drawCounters(g, box, ix, iy, iw, ih, van, a, false);
	}

	public static void drawCounters(GuiGraphics g, @Nullable int[] box, int ix, int iy, int iw, int ih, boolean van, float a, boolean server) {
		Palette p = Theme.current().palette();
		if (server && box != null) {
			TeamSummary land = TeamsState.serverLand();
			int mark = land == null ? (van ? -96 : p.accent()) : TeamsDraw.ink(land.color);
			plate(g, box[0], box[1], box[2], box[3], van, a);
			Icons.draw(g, Icon.SHIELD, box[0] + 3, box[1] + 3, 8, Colors.scaleAlpha(mark, a));
			g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(serverText(), box[2] - 18), box[0] + 14, box[1] + 3, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
		} else {
			TeamData team = TeamsView.land();
			Component why = whyNot();
			if (box == null) {
				if (why != null) {
					int w = Math.min(iw - 8, CFHGuiDraw.width(why) + 20);
					plate(g, ix + 3, iy + ih - 17, w, 14, van, a);
					Icons.draw(g, Icon.INFO, ix + 6, iy + ih - 14, 8, Colors.scaleAlpha(p.textMuted(), a));
					g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(why, w - 18), ix + 17, iy + ih - 14, Colors.scaleAlpha(van ? -1 : p.textMuted(), a), van);
				}
			} else {
				ServerInfo info = TeamsView.info();
				int x = box[0];
				int y = box[1];
				int w = box[2];
				int colW = (w - 9 - (why != null ? 12 : 0)) / 2;
				plate(g, x, y, w, box[3], van, a);
				int ink = team != null ? TeamsDraw.ink(team.color) : (van ? -96 : p.accentInk());
				counter(g, Icon.MAP, landText(), x + 3, y + 3, colW, (float)TeamsView.held() / (float)Math.max(1, info.maxClaims), ink, van, a);
				counter(g, Icon.BOLT, loadedText(), x + 6 + colW, y + 3, colW, (float)TeamsView.forced() / (float)Math.max(1, info.maxForceLoads), p.warning(), van, a);
				if (why != null) {
					Icons.draw(g, Icon.LOCK, x + w - 11, y + 5, 8, Colors.scaleAlpha(p.textDim(), a));
				}
			}
		}
	}

	public static List<Component> counterTip() {
		return counterTip(false);
	}

	public static List<Component> counterTip(boolean server) {
		if (server) {
			return List.of(TeamsText.t("map.server.on"), TeamsText.t("map.server.on.desc").withStyle(ChatFormatting.GRAY));
		} else {
			ServerInfo info = TeamsView.info();
			List<Component> lines = new ArrayList<>();
			lines.add(TeamsText.t("map.counter.land", TeamsView.held(), info.maxClaims));
			lines.add(TeamsText.t("map.counter.loaded", TeamsView.forced(), info.maxForceLoads));
			Component why = whyNot();
			if (why != null) {
				lines.add(why.copy().withStyle(ChatFormatting.GRAY));
			}

			return lines;
		}
	}

	private static void counter(GuiGraphics g, Icon icon, String text, int x, int y, int w, float fraction, int color, boolean van, float a) {
		Palette p = Theme.current().palette();
		Icons.draw(g, icon, x, y, 8, Colors.scaleAlpha(color, a));
		g.drawString(CFHGuiDraw.font(), text, x + 10, y, Colors.scaleAlpha(van ? -1 : p.text(), a), van);
		TeamsDraw.bar(g, x, y + 11, w - 2, 2, fraction, fraction >= 1.0F ? p.danger() : color, a);
	}

	private void drawPlace(GuiGraphics g, int ix, int iy, int iw, int ih, int cx, int cz, boolean van, float a) {
		Palette p = Theme.current().palette();
		Component text = TeamsText.t("map.chunk", cx, cz);
		int w = CFHGuiDraw.width(text) + 8;
		if (w <= iw / 2) {
			int x = ix + iw - w - 3;
			int y = iy + ih - 15;
			plate(g, x, y, w, 12, van, a);
			g.drawString(CFHGuiDraw.font(), text, x + 4, y + 2, Colors.scaleAlpha(van ? -2039584 : p.textMuted(), a), van);
		}
	}

	private void drawNote(GuiGraphics g, int ix, int iy, int iw, boolean van, float a) {
		if (this.note != null) {
			long age = Clock.nowMs() - this.noteAtMs;
			if (age > 4200L) {
				this.note = null;
			} else {
				Palette p = Theme.current().palette();
				float in = Theme.current().motion() <= 0.0F ? 1.0F : Ease.OUT_CUBIC.apply(Math.min(1.0F, (float)age / 180.0F));
				float out = Theme.current().motion() <= 0.0F ? 1.0F : Math.min(1.0F, (float)(4200L - age) / 300.0F);
				float na = a * in * out;
				int color = this.noteBad ? p.warning() : p.success();
				int maxW = iw - 22 - 14 - 8;
				int w = Math.min(maxW, CFHGuiDraw.width(this.note) + 20);
				int x = ix + 17;
				int y = iy + 3 - Math.round((1.0F - in) * 6.0F);
				plate(g, x, y, w, 14, van, na);
				if (!van) {
					CFHGuiDraw.rect(g, x, y + 2, 2, 10, Colors.scaleAlpha(color, na));
				}

				Icons.draw(g, this.noteBad ? Icon.WARNING : Icon.CHECK, x + 4, y + 3, 8, Colors.scaleAlpha(color, na));
				g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(this.note, w - 18), x + 15, y + 3, Colors.scaleAlpha(van ? -1 : p.text(), na), van);
			}
		}
	}

	protected void updateWidgetNarration(NarrationElementOutput out) {
		out.add(NarratedElementType.TITLE, this.getMessage());
		out.add(NarratedElementType.USAGE, TeamsText.t("map.help.keys"));
	}

	private static enum Drag {
		NONE,
		CLAIM,
		UNCLAIM,
		LOAD,
		UNLOAD,
		PAN;
	}

	public static enum Gesture {
		CLAIM,
		UNCLAIM,
		LOAD,
		UNLOAD;
	}

	private static record Look(int fill, int edge, int glow, TeamsView.Relation relation) {
	}

	@FunctionalInterface
	public interface Notes {
		void say(Component var1, boolean var2);
	}

	public static enum Tool {
		CLAIM,
		KEEP_LOADED;
	}
}
