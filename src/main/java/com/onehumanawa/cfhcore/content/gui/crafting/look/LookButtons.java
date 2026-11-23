package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.emi.EmiRenderHelper;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSide;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.client.invrow.InventoryRow;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeDockSeam;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class LookButtons {
	private static final Map<Object, Long> HOVER_SINCE = new WeakHashMap<>();
	private static final Map<Object, Anim[]> EASED = new WeakHashMap<>();
	private static final int FOOT_GAP = 2;
	private static final int CLEAR = 2;
	private static final int SEARCH_MIN = 72;
	private static final int FOOT_H = 22;
	private static boolean rowRight;
	private static int rowX;
	private static int rowW;
	private static int rowTop;
	private static int searchX = Integer.MIN_VALUE;
	private static int searchY;
	private static int searchW;

	public static Icon kindIcon(SidebarType type) {
		return switch (type) {
			case INDEX -> Icon.GRID;
			case CRAFTABLES -> Icon.HAMMER;
			case FAVORITES -> Icon.STAR;
			case LOOKUP_HISTORY -> Icon.HISTORY;
			case CRAFT_HISTORY -> Icon.CLOCK;
			case CHESS -> Icon.CROWN;
			case TEAM -> Icon.TEAM;
			case EMPTY, NONE -> Icon.SELECTION;
		};
	}

	public static boolean sized(
		AbstractWidget w, GuiGraphics g, int mouseX, int mouseY, ResourceLocation texture, int u, int v, int vOffset, @Nullable Supplier<List<Component>> tip
	) {
		LookButtons.Role role = role(texture, u, v, w.getWidth(), w.getHeight());
		if (role == null) {
			return false;
		} else {
			g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			boolean cfh = Look.cfh();
			boolean hovered = w.active && w.isHovered();
			if (w.active || !cfh || role != LookButtons.Role.STEP_PREV && role != LookButtons.Role.STEP_NEXT) {
				float hover = ReskinDraw.hover(w);
				float press = ReskinDraw.press(w);
				int x = w.getX();
				int y = w.getY() + (role == LookButtons.Role.KIND ? 1 : 0);
				int width = w.getWidth();
				int height = w.getHeight();
				LookDraw.Tone tone = role.tone;
				Icon icon = role.icon;
				boolean dead = false;
				Component label = null;
				switch (role) {
					case TREE:
						if (TreeDockSeam.isOpen()) {
							tone = LookDraw.Tone.ON;
						}

						label = Component.translatable("cfhgui_crafting.look.tree");
						break;
					case GLOBAL:
						if (vOffset >= 40) {
							tone = LookDraw.Tone.ON;
						}
						break;
					case KIND:
						EmiScreenManager.SidebarPanel panel = panelOf(w);
						icon = kindIcon(panel == null ? SidebarType.NONE : panel.getType());
						dead = panel == null || panel.pages.pages.size() <= 1;
						break;
					case JUMP:
						icon = jumpIcon(u, v);
				}

				if (!cfh && tone == LookDraw.Tone.GHOST && role != LookButtons.Role.KIND && role != LookButtons.Role.JUMP) {
					tone = LookDraw.Tone.SOLID;
				}

				int fg = LookDraw.face(g, x, y, width, height, tone, dead ? 0.0F : hover, dead ? 0.0F : press, w.active, 1.0F);
				int ny = y + (dead ? 0 : LookDraw.nudge(press));
				if (label != null && width > height) {
					int size = 12;
					int textW = CFHGuiDraw.width(label);
					int cx = x + (width - (16 + textW)) / 2;
					LookDraw.glyph(g, icon, cx, ny, 12, height, 12, fg);
					g.drawString(CFHGuiDraw.font(), label, cx + 12 + 4, CFHGuiDraw.textY(ny, height), fg, !cfh);
				} else {
					int size = role == LookButtons.Role.KIND ? 12 : Math.min(LookDraw.glyphSize(Math.min(width, height)), 12);
					LookDraw.glyph(g, icon, x, ny, width, height, size, fg);
				}

				if (cfh && w.active && w.isFocused() && !hovered) {
					CFHGuiDraw.focusRing(g, x, y, width, height, 1.0F);
				}

				tooltip(w, hovered, role, tip);
				return true;
			} else {
				return true;
			}
		}
	}

	@Nullable
	private static LookButtons.Role role(ResourceLocation texture, int u, int v, int w, int h) {
		if (EmiRenderHelper.BUTTONS.equals(texture)) {
			if (v == 0) {
				if (w == 12 && h == 12) {
					return u == 0 ? LookButtons.Role.STEP_PREV : (u == 12 ? LookButtons.Role.STEP_NEXT : null);
				}

				if (u == 224) {
					return LookButtons.Role.PAGE_PREV;
				}

				if (u == 240) {
					return LookButtons.Role.PAGE_NEXT;
				}

				if (u == 204) {
					return LookButtons.Role.SETTINGS;
				}

				if (u == 184) {
					return LookButtons.Role.TREE;
				}

				if (u == 164) {
					return LookButtons.Role.GLOBAL;
				}
			} else if (v == 48) {
				if (u == 232) {
					return LookButtons.Role.NUMBER_UP;
				}

				if (u == 244) {
					return LookButtons.Role.NUMBER_DOWN;
				}
			}

			return null;
		} else if (!EmiRenderHelper.WIDGETS.equals(texture)) {
			return EmiRenderHelper.CONFIG.equals(texture) ? LookButtons.Role.JUMP : null;
		} else {
			return w != 16 || h != 16 || v != 146 && v != 0 ? null : LookButtons.Role.KIND;
		}
	}

	@Nullable
	private static EmiScreenManager.SidebarPanel panelOf(AbstractWidget w) {
		for (SidebarSide side : SidebarSide.values()) {
			EmiScreenManager.SidebarPanel panel = EmiScreenManager.getPanelFor(side);
			if (panel != null && panel.cycle == w) {
				return panel;
			}
		}

		return null;
	}

	private static Icon jumpIcon(int u, int v) {
		int col = u / 16;

		return switch (v / 16) {
			case 0 -> col == 0 ? Icon.SLIDERS : Icon.SEARCH;
			case 1 -> {
				switch (col) {
					case 0:
						yield Icon.PANEL;
					case 1:
						yield Icon.ALIGN_LEFT;
					case 2:
						yield Icon.ALIGN_RIGHT;
					case 3:
						yield Icon.ALIGN_TOP;
					default:
						yield Icon.ALIGN_BOTTOM;
				}
			}
			case 2 -> col == 0 ? Icon.KEYBOARD : (col == 1 ? Icon.HAMMER : Icon.CHEATS);
			default -> Icon.CODE;
		};
	}

	public static void footRow(AbstractWidget tree, AbstractWidget settings, int screenWidth, int screenHeight, Bounds container, List<Bounds> others) {
		tree.setWidth(22 + CFHGuiDraw.width(Component.translatable("cfhgui_crafting.look.tree")) + 6);
		int y = screenHeight - 22;
		rowRight = false;
		if (blocked(2, y, rowWidth(tree, settings), 22, container, others)) {
			int full = tree.getWidth();

			for (int form = 0; form < 3 && !rowRight; form++) {
				if (form > 0) {
					tree.setWidth(tree.getHeight());
				}

				boolean stacked = form == 2 && tree.visible && settings.visible;
				int w = stacked ? Math.max(tree.getWidth(), settings.getWidth()) : rowWidth(tree, settings);
				int top = screenHeight - (stacked ? 44 : 22);
				int x = screenWidth - 2 - w;
				if (x >= container.right() + 2 && !blocked(x, top, w, screenHeight - top, container, others)) {
					rowRight = true;
					rowX = x;
					rowW = w;
					rowTop = top;
					if (stacked) {
						tree.setX(x + (w - tree.getWidth()) / 2);
						tree.setY(top);
						settings.setX(x + (w - settings.getWidth()) / 2);
						settings.setY(top + 22);
						return;
					}
				}
			}

			if (!rowRight) {
				tree.setWidth(full);
			}
		}

		int x = rowRight ? rowX : 2;
		if (tree.visible) {
			tree.setX(x);
			tree.setY(y);
			x += tree.getWidth() + 2;
		}

		if (settings.visible) {
			settings.setX(x);
			settings.setY(y);
		}
	}

	private static boolean blocked(int x, int y, int w, int h, Bounds container, List<Bounds> others) {
		if (touches(x, y, w, h, container)) {
			return true;
		} else {
			for (Bounds b : others) {
				if (touches(x, y, w, h, b)) {
					return true;
				}
			}

			return false;
		}
	}

	private static boolean touches(int x, int y, int w, int h, Bounds b) {
		return !b.empty() && x < b.right() + 2 && x + w > b.left() - 2 && y < b.bottom() + 2 && y + h > b.top() - 2;
	}

	private static int rowWidth(AbstractWidget tree, AbstractWidget settings) {
		int w = (tree.visible ? tree.getWidth() + 2 : 0) + (settings.visible ? settings.getWidth() + 2 : 0);
		return Math.max(0, w - 2);
	}

	public static int footEnd(AbstractWidget tree, AbstractWidget settings) {
		int x = 2;
		if (tree.visible) {
			x += tree.getWidth() + 2;
		}

		if (settings.visible) {
			x += settings.getWidth() + 2;
		}

		return x + 2;
	}

	public static Bounds footBounds(AbstractWidget tree, AbstractWidget settings, int screenHeight, Bounds container) {
		return !rowRight ? new Bounds(0, screenHeight - 22, container.left(), 22) : new Bounds(rowX - 2, rowTop, rowW + 4, screenHeight - rowTop);
	}

	public static void searchHome(AbstractWidget search) {
		searchX = search.getX();
		searchY = search.getY();
		searchW = search.getWidth();
	}

	public static void footSearch(AbstractWidget search, Bounds container, Screen screen, int sideX, int sideW) {
		if (searchX != Integer.MIN_VALUE) {
			int screenHeight = screen.height;
			int x = searchX;
			int y = searchY;
			int w = searchW;
			boolean onContainer = y < container.bottom() + 2
				&& y + search.getHeight() > container.top() - 2
				&& x < container.right() + 2
				&& x + w > container.left() - 2;
			if (onContainer && sideW >= 72 && sideX >= container.right() + 2) {
				x = sideX;
				w = sideW;
				y = screenHeight - 22 + 1;
			} else if (onContainer) {
				int sx = container.right() + 2 + 2;
				int sw = screen.width - 2 - sx;
				if (sw >= 54) {
					int sy = screenHeight - 22 + 1;
					int[] row = InventoryRow.bounds(screen);
					if (row != null && row[1] + row[3] + 2 > sy && row[0] < sx + sw && row[0] + row[2] > sx) {
						sy = row[1] + row[3] + 2 + 1;
					}

					if (sy + search.getHeight() <= screenHeight - 1) {
						x = sx;
						w = sw;
						y = sy;
					}
				}
			}

			if (rowRight && x < rowX + rowW && x + w > rowX && y + search.getHeight() > rowTop) {
				y = rowTop - 20;
			}

			if (search.getX() != x) {
				search.setX(x);
			}

			if (search.getY() != y) {
				search.setY(y);
			}

			if (search.getWidth() != w) {
				search.setWidth(w);
			}
		}
	}

	public static boolean recipe(Object button, GuiGraphics g, int x, int y, int u, int v, int offset, boolean hovered) {
		if (v != 0) {
			return false;
		} else {
			LookDraw.Tone tone = LookDraw.Tone.SOLID;
			boolean enabled = true;
			boolean marked = false;
			Icon icon;
			switch (u) {
				case 24:
					icon = Icon.PLUS;
					enabled = offset != 24;
					break;
				case 36:
					icon = Icon.LAN;
					if (offset >= 36) {
						tone = LookDraw.Tone.ON;
					}
					break;
				case 48:
					icon = offset >= 36 ? Icon.HEART_FILLED : Icon.HEART;
					if (offset >= 60) {
						marked = true;
					} else if (offset >= 36) {
						tone = LookDraw.Tone.ON;
					}
					break;
				case 60:
					icon = Icon.CAMERA;
					break;
				default:
					return false;
			}

			small(button, g, x, y, icon, tone, enabled, hovered, marked ? (Look.cfh() ? Theme.current().accent() : -96) : 0);
			return true;
		}
	}

	public static void smallButton(Object button, GuiGraphics g, int x, int y, Icon icon, boolean on, boolean enabled, boolean hovered) {
		small(button, g, x, y, icon, on ? LookDraw.Tone.ON : LookDraw.Tone.SOLID, enabled, hovered);
	}

	static void small(Object button, GuiGraphics g, int x, int y, Icon icon, LookDraw.Tone tone, boolean enabled, boolean hovered) {
		small(button, g, x, y, icon, tone, enabled, hovered, 0);
	}

	private static void small(Object button, GuiGraphics g, int x, int y, Icon icon, LookDraw.Tone tone, boolean enabled, boolean hovered, int glyphColor) {
		Anim[] eased = EASED.computeIfAbsent(button, k -> new Anim[]{new Anim(0.0F, 140, Ease.OUT_CUBIC), new Anim(0.0F, 90, Ease.OUT_CUBIC)});
		eased[0].set(hovered && enabled);
		eased[1].set(hovered && enabled && Minecraft.getInstance().mouseHandler.isLeftPressed());
		float hover = eased[0].get();
		float press = eased[1].get();
		int fg = LookDraw.face(g, x, y, 12, 12, tone, hover, press, enabled, 1.0F);
		LookDraw.glyph(g, icon, x, y + LookDraw.nudge(press), 12, 12, 8, glyphColor != 0 ? glyphColor : fg);
	}

	private static void tooltip(AbstractWidget w, boolean hovered, LookButtons.Role role, @Nullable Supplier<List<Component>> lines) {
		if (!hovered) {
			HOVER_SINCE.remove(w);
		} else {
			long now = Clock.nowMs();
			long since = HOVER_SINCE.computeIfAbsent(w, k -> now);
			if (now - since >= 350L) {
				List<Component> text = switch (role) {
					case SETTINGS -> List.of(Component.translatable("cfhgui_crafting.look.settings"));
					case TREE -> List.of(
					Component.translatable("cfhgui_crafting.look.tree.title"), hint(TreeDockSeam.isOpen() ? "cfhgui_crafting.look.tree.close" : "cfhgui_crafting.look.tree.open")
				);
					default -> lines == null ? List.of() : (List)lines.get();
				};
				if (text != null && !text.isEmpty()) {
					if (role != LookButtons.Role.SETTINGS && role != LookButtons.Role.TREE) {
						CFHGuiTooltips.request(text, w);
					} else {
						int rows = 0;

						for (Component line : text) {
							rows += Math.max(1, CFHGuiDraw.font().split(line, 220).size());
						}

						CFHGuiTooltips.requestAt(text, w.getX(), w.getY() - (rows * 10 + 7) - 3);
					}
				}
			}
		}
	}

	public static Component hint(String key, Object... args) {
		int color = Theme.current().isVanilla() ? 10526880 : Theme.current().palette().textMuted() & 16777215;
		return Component.translatable(key, args).withStyle(s -> s.withColor(color));
	}

	private LookButtons() {
	}

	private static enum Role {
		PAGE_PREV(Icon.CHEVRON_LEFT, LookDraw.Tone.GHOST),
		PAGE_NEXT(Icon.CHEVRON_RIGHT, LookDraw.Tone.GHOST),
		STEP_PREV(Icon.CHEVRON_LEFT, LookDraw.Tone.GHOST),
		STEP_NEXT(Icon.CHEVRON_RIGHT, LookDraw.Tone.GHOST),
		NUMBER_UP(Icon.CHEVRON_UP, LookDraw.Tone.SOLID),
		NUMBER_DOWN(Icon.CHEVRON_DOWN, LookDraw.Tone.SOLID),
		SETTINGS(Icon.SETTINGS, LookDraw.Tone.SOLID),
		TREE(Icon.LAN, LookDraw.Tone.SOLID),
		GLOBAL(Icon.WORLD, LookDraw.Tone.SOLID),
		KIND(Icon.GRID, LookDraw.Tone.GHOST),
		JUMP(Icon.DOT, LookDraw.Tone.GHOST);

		final Icon icon;
		final LookDraw.Tone tone;

		private Role(final Icon icon, final LookDraw.Tone tone) {
			this.icon = icon;
			this.tone = tone;
		}
	}
}
