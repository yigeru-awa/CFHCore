package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.module.KnownModules;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiColorField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList.RowRenderer;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import com.onehumanawa.cfhcore.content.gui.server.model.Role;
import com.onehumanawa.cfhcore.content.gui.server.net.ServerNet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class RolesPage extends ServerPage {
	private static final String GENERAL = "general";
	private static final String PERMISSIONS = "permissions";
	private static final String LIMITS = "limits";
	private static final int ROW = 24;
	@Nullable
	static String pendingRole;
	@Nullable
	static String pendingTab;
	@Nullable
	private AdminData.RoleSet set;
	@Nullable
	private String selected;
	@Nullable
	private Role draft;
	private boolean dirty;
	private boolean fresh;
	private String tab = "general";
	private String nodeQuery = "";
	private boolean onlySet;
	private Rect listArea = new Rect(0, 0, 0, 0);
	private Rect editorArea = new Rect(0, 0, 0, 0);
	private CFHGuiButton save;
	private CFHGuiButton revert;
	@Nullable
	private CFHGuiList<RolesPage.Line> nodes;
	private int pressRow = -1;
	private int dropRow = -1;
	private double pressY;
	private double dragY;
	private boolean dragging;
	private final Anim scroll = new Anim(0.0F, 180, Ease.OUT_CUBIC);
	private boolean grabbed;
	private double grabY;
	private double grabScroll;
	private boolean reveal = true;
	private static final int CELL = 16;

	RolesPage(AdminScreen screen) {
		super(screen, "roles", Txt.adminPage("roles"), Icon.CROWN);
	}

	@Override
	protected Node node() {
		return Nodes.ADMIN_ROLES;
	}

	@Override
	public void stateChanged() {
	}

	private void ask() {
		ServerActions.adminRoles(answer -> {
			this.set = answer.map(AdminData.RoleSet::sanitize).orElseGet(AdminData.RoleSet::new);
			if (pendingRole != null) {
				this.selected = pendingRole;
				pendingRole = null;
				this.draft = null;
			}

			if (pendingTab != null) {
				this.tab = pendingTab;
				pendingTab = null;
			}

			if (!this.fresh && (this.selected == null || this.role(this.selected) == null)) {
				this.selected = this.set.roles.isEmpty() ? null : this.set.roles.get(0).id;
				this.draft = null;
			}

			if (this.draft == null && this.selected != null) {
				this.draft = copy(this.role(this.selected));
			}

			this.reveal = true;
			this.screen.refreshPage();
		});
	}

	@Nullable
	private Role role(@Nullable String id) {
		if (this.set != null && id != null) {
			for (Role r : this.set.roles) {
				if (r.id.equals(id)) {
					return r;
				}
			}

			return null;
		} else {
			return null;
		}
	}

	@Nullable
	private static Role copy(@Nullable Role role) {
		return role == null ? null : ((Role)JsonWire.GSON.fromJson(JsonWire.GSON.toJson(role), Role.class)).sanitize();
	}

	private boolean editable(Role role) {
		return this.set != null && role.priority < this.set.editableBelow;
	}

	private boolean mayEdit() {
		return this.draft != null && (this.fresh || this.role(this.draft.id) == null || this.editable(this.role(this.draft.id)));
	}

	private void touch() {
		this.dirty = true;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.nodes = null;
		if (this.set == null) {
			((PaintedCard)s.addPageWidget(
					new PaintedCard(area.w(), area.h(), (g, f) -> CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 6, 12, Ui.soft())).bare()
				))
				.setPosition(area.x(), area.y());
			this.ask();
		} else {
			int leftW = Math.max(120, Math.min(176, area.w() * 32 / 100));
			this.listArea = new Rect(area.x(), area.y(), leftW, area.h() - 26);
			this.editorArea = new Rect(area.x() + leftW + 8, area.y(), area.w() - leftW - 8, area.h());
			CFHGuiButton add = new CFHGuiButton(this.listArea.x(), area.bottom() - 20, leftW, Txt.t("roles.new"), this::newRole).icon(Icon.PLUS);
			s.addPageWidget(add);
			Role d = this.draft;
			if (d != null) {
				Rect e = this.editorArea;
				boolean may = this.mayEdit();
				this.save = new CFHGuiButton(0, 0, 70, Txt.t("form.save"), this::save).variant(Variant.PRIMARY).icon(Icon.CHECK);
				this.save.setWidth(this.save.preferredWidth() + 14);
				this.save.setPosition(e.right() - this.save.getWidth(), e.bottom() - 20);
				s.addPageWidget(this.save);
				this.revert = new CFHGuiButton(0, 0, 70, Txt.t("roles.revert"), () -> {
					if (this.fresh) {
						this.fresh = false;
						this.selected = this.set != null && !this.set.roles.isEmpty() ? this.set.roles.get(0).id : null;
					}

					this.draft = copy(this.role(this.selected));
					this.dirty = false;
					this.screen.refreshPage();
				}).variant(Variant.GHOST);
				this.revert.setWidth(this.revert.preferredWidth() + 10);
				this.revert.setPosition(this.save.getX() - this.revert.getWidth() - 4, e.bottom() - 20);
				s.addPageWidget(this.revert);
				if (may && !this.fresh && !"default".equals(d.id)) {
					CFHGuiButton delete = new CFHGuiButton(
							0,
							0,
							70,
							Txt.t("form.delete"),
							() -> Forms.confirm(
									Txt.t("roles.delete.title", d.name), Txt.t("roles.delete.body"), Txt.t("form.delete"), () -> ServerActions.roleDelete(d.id, AdminScreen.after(() -> {
											this.selected = null;
											this.draft = null;
											this.dirty = false;
											this.ask();
										}))
								)
						)
						.variant(Variant.DANGER);
					delete.setWidth(delete.preferredWidth() + 10);
					delete.setPosition(e.x(), e.bottom() - 20);
					s.addPageWidget(delete);
				}

				List<String> tabs = List.of("general", "permissions", "limits");
				s.addPageWidget(new CFHGuiSegmented<>(e.right() - 222, e.y() + 2, 222, tabs, this.tab, t -> Txt.t("roles.tab." + t), t -> {
					if (!t.equals(this.tab)) {
						this.tab = t;
						this.screen.refreshPage();
					}
				}));
				Rect body = new Rect(e.x(), e.y() + 28, e.w(), e.h() - 28 - 26);
				String var10 = this.tab;
				switch (var10) {
					case "permissions":
						this.permissions(s, body, d, may);
						break;
					case "limits":
						this.limits(s, body, d, may);
						break;
					default:
						this.general(s, body, d, may);
				}
			}
		}
	}

	private void general(SidebarScreen s, Rect body, Role d, boolean may) {
		CFHGuiScrollPanel p = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(body.x(), body.y(), body.w(), body.h()));
		int w = p.innerWidth();
		int half = (w - 8) / 2;
		int y = 2;
		if (!may) {
			p.add(new CFHGuiLabel(0, 0, w, Txt.t("roles.readonly")).style(Style.MUTED).wrap(true), 0, y);
			y += CFHGuiLabel.heightFor(Txt.t("roles.readonly"), w, Style.MUTED) + 6;
		}

		p.add(caption(Txt.t("form.name"), half), 0, y);
		p.add(caption(Txt.t("roles.prefix"), half), half + 8, y);
		CFHGuiTextField name = new CFHGuiTextField(0, 0, half, Txt.t("form.name")).maxLength(24);
		name.setValue(d.name);
		name.onChange(v -> {
			d.name = v.trim();
			this.touch();
		});
		CFHGuiTextField prefix = new CFHGuiTextField(0, 0, half, Txt.t("roles.prefix")).maxLength(16).placeholder(Txt.t("roles.prefix.hint"));
		prefix.setValue(d.prefix);
		prefix.onChange(v -> {
			d.prefix = v.trim();
			this.touch();
		});
		((CFHGuiTextField)p.add(name, 0, y + 11)).active = may;
		((CFHGuiTextField)p.add(prefix, half + 8, y + 11)).active = may;
		y += 38;
		p.add(caption(Txt.t("form.color"), half), 0, y);
		p.add(caption(Txt.t("roles.priority"), half), half + 8, y);
		CFHGuiColorField color = new CFHGuiColorField(0, 0, half, Ui.opaque(d.color), c -> {
			d.color = c & 16777215;
			this.touch();
		});
		((CFHGuiColorField)p.add(color, 0, y + 11)).active = may;
		CFHGuiTextField priority = new CFHGuiTextField(0, 0, half, Txt.t("roles.priority")).maxLength(9);
		priority.setFilter(v -> v.matches("-?[0-9]{0,8}"));
		priority.setValue(Integer.toString(d.priority));
		priority.onChange(v -> {
			try {
				int n = Integer.parseInt(v);
				boolean ok = this.set == null || n < this.set.editableBelow;
				priority.setInvalid(!ok);
				if (ok) {
					d.priority = n;
					this.touch();
				}
			} catch (NumberFormatException var6x) {
				priority.setInvalid(true);
			}
		});
		priority.setTooltip(Tooltip.create(Txt.t("roles.priority.tip")));
		((CFHGuiTextField)p.add(priority, half + 8, y + 11)).active = may;
		y += 38;
		p.add(caption(Txt.t("roles.parent"), w), 0, y);
		List<String> parents = new ArrayList<>();
		parents.add("");
		if (this.set != null) {
			for (Role r : this.set.roles) {
				if (!r.id.equals(d.id) && !r.inherits.contains(d.id)) {
					parents.add(r.id);
				}
			}
		}

		String parent = d.inherits.isEmpty() ? "" : d.inherits.get(0);
		if (!parents.contains(parent)) {
			parents.add(parent);
		}

		CFHGuiDropdown<String> inherits = new CFHGuiDropdown<>(0, 0, w, parents, parent, id -> {
			Role rx = this.role(id);
			return id.isEmpty() ? Txt.t("roles.parent.none") : Component.literal(rx == null ? id : rx.name);
		}, id -> {
			if (!d.inherits.isEmpty()) {
				d.inherits.remove(0);
			}

			if (!id.isEmpty()) {
				d.inherits.add(0, id);
			}

			this.touch();
		});
		inherits.tip(Txt.t("roles.parent.tip"));
		((CFHGuiDropdown)p.add(inherits, 0, y + 11)).active = may;
		y += 40;
		CFHGuiToggle everyone = new CFHGuiToggle(0, 0, w, Txt.t("roles.everyone"), d.everyone, v -> {
			d.everyone = v;
			this.touch();
		});
		everyone.tip(Txt.t("roles.everyone.tip"));
		((CFHGuiToggle)p.add(everyone, 0, y)).active = may;
		y += 22;
		CFHGuiToggle operators = new CFHGuiToggle(0, 0, w, Txt.t("roles.operators"), d.operators, v -> {
			d.operators = v;
			this.touch();
		});
		operators.tip(Txt.t("roles.operators.tip"));
		((CFHGuiToggle)p.add(operators, 0, y)).active = may;
		y += 24;
		p.setContentHeight(y);
	}

	private static CFHGuiLabel caption(Component text, int w) {
		return new CFHGuiLabel(0, 0, w, text).style(Style.MUTED);
	}

	private void limits(SidebarScreen s, Rect body, Role d, boolean may) {
		CFHGuiScrollPanel p = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(body.x(), body.y(), body.w(), body.h()));
		int w = p.innerWidth();
		int y = 2;
		p.add(new CFHGuiLabel(0, 0, w, Txt.t("roles.limits.about")).style(Style.MUTED).wrap(true), 0, y);
		y += CFHGuiLabel.heightFor(Txt.t("roles.limits.about"), w, Style.MUTED) + 8;
		if (this.set != null) {
			for (AdminData.NodeInfo info : this.set.nodes) {
				if (info.number) {
					Integer own = d.numbers.get(info.id);
					Integer inherited = this.inheritedNumber(d, info.id, new HashSet<>());
					p.add(new CFHGuiLabel(0, 0, w - 78, describe(info)).style(Style.BODY), 0, y + 5);
					CFHGuiTextField field = new CFHGuiTextField(0, 0, 70, describe(info))
						.maxLength(7)
						.placeholder(Component.literal(Integer.toString(inherited != null ? inherited : info.defaultNumber)));
					field.setFilter(v -> v.matches("[0-9]{0,6}"));
					field.setValue(own == null ? "" : Integer.toString(own));
					field.onChange(v -> {
						if (v.isEmpty()) {
							d.numbers.remove(info.id);
						} else {
							d.numbers.put(info.id, Integer.parseInt(v));
						}

						this.touch();
					});
					field.setTooltip(
						Tooltip.create(
							Component.literal(info.id + "\n")
								.append(
									Txt.t(inherited != null ? "roles.limits.inherited" : "roles.limits.default", inherited != null ? inherited : info.defaultNumber)
										.withStyle(ChatFormatting.GRAY)
								)
						)
					);
					((CFHGuiTextField)p.add(field, w - 70, y)).active = may;
					y += 24;
				}
			}
		}

		p.setContentHeight(y);
	}

	private void permissions(SidebarScreen s, Rect body, final Role d, final boolean may) {
		CFHGuiToggle only = new CFHGuiToggle(0, 0, 80, Txt.t("roles.only_set"), this.onlySet, v -> {
			this.onlySet = v;
			if (this.nodes != null) {
				this.nodes.items(this.lines(d));
			}
		});
		only.setWidth(CFHGuiDraw.width(Txt.t("roles.only_set")) + 36);
		only.setPosition(body.right() - only.getWidth(), body.y());
		s.addPageWidget(only);
		CFHGuiSearchField search = new CFHGuiSearchField(body.x(), body.y(), Math.max(60, body.w() - only.getWidth() - 6), q -> {
			String typed = q.trim().toLowerCase(Locale.ROOT);
			if (!typed.equals(this.nodeQuery)) {
				this.nodeQuery = typed;
				if (this.nodes != null) {
					this.nodes.items(this.lines(d));
					this.nodes.snapScroll(0.0);
				}
			}
		});
		search.setValue(this.nodeQuery);
		search.placeholder(Txt.t("roles.search"));
		s.addPageWidget(search);
		CFHGuiList<RolesPage.Line> list = new CFHGuiList(body.x(), body.y() + 24, body.w(), body.h() - 24, 22, new RowRenderer<RolesPage.Line>() {
			public void render(GuiGraphics g, RolesPage.Line line, int index, int x, int y, int w, int h, boolean hovered, boolean sel, int mouseX, int mouseY) {
				RolesPage.this.drawLine(g, d, line, x, y, w, h, hovered, mouseX, mouseY, may);
				if (sel && RolesPage.this.nodes != null && RolesPage.this.nodes.isFocused()) {
					CFHGuiDraw.rect(g, x, y + 3, 2, h - 6, Theme.current().palette().accent());
				}
			}

			public boolean click(RolesPage.Line line, int index, int x, int y, int w, int h, double mouseX, double mouseY, int button) {
				if (may && button == 0) {
					int cell = RolesPage.cellAt(x, w, mouseX);
					if (cell < 0) {
						return false;
					} else {
						RolesPage.this.set(d, line.pattern(), cell == 0 ? Boolean.TRUE : (cell == 2 ? Boolean.FALSE : null));
						return true;
					}
				} else {
					return false;
				}
			}
		});
		list.plainRows().emptyText(Txt.t("roles.no_match"));
		list.onActivate(line -> {
			if (may) {
				Boolean now = own(d, line.pattern());
				this.set(d, line.pattern(), now == null ? Boolean.TRUE : (now ? Boolean.FALSE : null));
			}
		});
		list.items(this.lines(d));
		this.nodes = (CFHGuiList<RolesPage.Line>)s.addPageWidget(list);
	}

	private static String module(String node) {
		int dot = node.indexOf(46);
		return dot < 0 ? node : node.substring(0, dot);
	}

	private static String groupOf(String node) {
		String module = module(node);
		if (!module.equals("cfhgui_server")) {
			return module;
		} else {
			int second = node.indexOf(46, module.length() + 1);
			return second < 0 ? node : node.substring(0, second);
		}
	}

	private static Component groupName(String group) {
		String module = module(group);
		if (!module.equals("cfhgui_server")) {
			return KnownModules.name(module);
		} else {
			String word = group.substring(module.length() + 1);
			String key = "cfhgui_server.ui.perm.group." + word;
			return I18n.exists(key) ? Component.translatable(key) : Component.literal(Character.toUpperCase(word.charAt(0)) + word.substring(1));
		}
	}

	private static Component describe(AdminData.NodeInfo info) {
		String module = module(info.id);
		String path = info.id.length() > module.length() ? info.id.substring(module.length() + 1) : info.id;
		String key = module + ".perm." + path;
		if (I18n.exists(key)) {
			return Component.translatable(key);
		} else if (info.id.startsWith("cfhgui_server.warp.to.")) {
			return Component.translatable("cfhgui_server.perm.warp.to");
		} else {
			return info.id.startsWith("cfhgui_server.kit.use.")
				? Component.translatable("cfhgui_server.perm.kit.use.restricted")
				: Component.literal(info.description.isEmpty() ? path : info.description);
		}
	}

	private List<RolesPage.Line> lines(Role d) {
		List<RolesPage.Line> out = new ArrayList<>();
		if (this.set == null) {
			return out;
		} else {
			String group = null;
			List<AdminData.NodeInfo> sorted = new ArrayList<>(this.set.nodes);
			sorted.removeIf(nx -> nx.number);
			sorted.sort((a, b) -> {
				boolean oa = module(a.id).equals("cfhgui_server");
				boolean ob = module(b.id).equals("cfhgui_server");
				return oa != ob ? (oa ? -1 : 1) : 0;
			});
			List<String> groups = new ArrayList<>();

			for (AdminData.NodeInfo n : sorted) {
				if (!groups.contains(groupOf(n.id))) {
					groups.add(groupOf(n.id));
				}
			}

			for (String g : groups) {
				List<RolesPage.Line> members = new ArrayList<>();

				for (AdminData.NodeInfo nx : sorted) {
					if (groupOf(nx.id).equals(g)
						&& (!this.onlySet || d.says(nx.id) != null)
						&& (
							this.nodeQuery.isEmpty()
								|| nx.id.toLowerCase(Locale.ROOT).contains(this.nodeQuery)
								|| describe(nx).getString().toLowerCase(Locale.ROOT).contains(this.nodeQuery)
						)) {
						members.add(new RolesPage.Line(g, nx.id, nx));
					}
				}

				if (!members.isEmpty()) {
					out.add(new RolesPage.Line(g, g + ".*", null));
					out.addAll(members);
				}
			}

			return out;
		}
	}

	@Nullable
	private static Boolean own(Role d, String pattern) {
		if (d.deny.contains(pattern)) {
			return Boolean.FALSE;
		} else {
			return d.allow.contains(pattern) ? Boolean.TRUE : null;
		}
	}

	private void set(Role d, String pattern, @Nullable Boolean value) {
		d.allow.remove(pattern);
		d.deny.remove(pattern);
		if (Boolean.TRUE.equals(value)) {
			d.allow.add(pattern);
		} else if (Boolean.FALSE.equals(value)) {
			d.deny.add(pattern);
		}

		this.touch();
	}

	@Nullable
	private Boolean inherited(Role d, String node, Set<String> seen, String[] from) {
		for (String id : d.inherits) {
			if (seen.add(id)) {
				Role r = this.role(id);
				if (r != null) {
					Boolean says = r.says(node);
					if (says != null) {
						from[0] = r.name;
						return says;
					}

					Boolean deeper = this.inherited(r, node, seen, from);
					if (deeper != null) {
						return deeper;
					}
				}
			}
		}

		return null;
	}

	@Nullable
	private Integer inheritedNumber(Role d, String node, Set<String> seen) {
		for (String id : d.inherits) {
			if (seen.add(id)) {
				Role r = this.role(id);
				if (r != null) {
					if (r.numbers.containsKey(node)) {
						return r.numbers.get(node);
					}

					Integer deeper = this.inheritedNumber(r, node, seen);
					if (deeper != null) {
						return deeper;
					}
				}
			}
		}

		return null;
	}

	private static int cellAt(int x, int w, double mouseX) {
		int left = x + w - 6 - 48;
		return !(mouseX < (double)left) && !(mouseX >= (double)(left + 48)) ? (int)((mouseX - (double)left) / 16.0) : -1;
	}

	private void drawLine(GuiGraphics g, Role d, RolesPage.Line line, int x, int y, int w, int h, boolean hovered, int mouseX, int mouseY, boolean may) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean head = line.info() == null;
		Boolean own = own(d, line.pattern());
		String[] from = new String[]{""};
		Boolean wider = null;
		if (own == null && !head) {
			Boolean says = d.says(line.pattern());
			if (says != null) {
				wider = says;
				from[0] = d.name;
			} else {
				wider = this.inherited(d, line.pattern(), new HashSet<>(), from);
			}
		}

		int left = x + w - 6 - 48;
		if (head) {
			CFHGuiDraw.rect(g, x, y + 2, w, h - 3, t.isVanilla() ? 1073741824 : Colors.withAlpha(p.surfaceActive(), 144));
			g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(Fonts.heading(groupName(line.group())), left - x - 12), x + 6, y + 7, Ui.strong(), t.isVanilla());
		} else {
			AdminData.NodeInfo info = line.info();
			Component about = describe(info);
			int used = Ui.textFit(g, about, x + 6, y + 7, left - x - 14, own != null ? Ui.strong() : Ui.soft(), 1.0F);
			String path = info.id.substring(Math.min(info.id.length(), module(info.id).length() + 1));
			if (left - x - 26 - used > 40) {
				Ui.textFit(g, Component.literal(path), x + 12 + used, y + 7, left - x - 20 - used, Ui.caption(), 1.0F);
			}
		}

		int hot = hovered && may ? cellAt(x, w, (double)mouseX) : -1;
		Icon[] glyphs = new Icon[]{Icon.CHECK, Icon.MINUS, Icon.CLOSE};
		int[] colors = new int[]{p.success(), p.textMuted(), p.danger()};
		int state = own == null ? 1 : (own ? 0 : 2);

		for (int c = 0; c < 3; c++) {
			int cx = left + c * 16;
			int cy = y + (h - 14) / 2;
			boolean on = c == state;
			boolean ghost = !on && wider != null && c == (wider ? 0 : 2);
			int fill = on ? Colors.withAlpha(colors[c], c == 1 ? 48 : 112) : (c == hot ? Colors.withAlpha(colors[c], 64) : 0);
			if (fill != 0) {
				CFHGuiDraw.pixelRound(g, cx + 1, cy, 14, 14, fill, t.isVanilla() ? 0 : 2);
			}

			CFHGuiDraw.outline(
				g, cx + 1, cy, 14, 14, on ? colors[c] : (ghost ? Colors.withAlpha(colors[c], 144) : (t.isVanilla() ? 1627389951 : p.border())), t.isVanilla() ? 0 : 2
			);
			Icons.draw(
				g,
				glyphs[c],
				cx + 4,
				cy + 3,
				8,
				on
					? (c == 1 ? p.textMuted() : Colors.brighten(colors[c], 0.3F))
					: (ghost ? Colors.withAlpha(colors[c], 192) : Colors.withAlpha(p.textDim(), may ? 160 : 80))
			);
		}

		if (hovered) {
			List<Component> tip = new ArrayList<>();
			if (head) {
				tip.add(Txt.t("roles.branch", line.pattern()));
			} else {
				tip.add(describe(line.info()));
				tip.add(Component.literal(line.info().id).withStyle(ChatFormatting.DARK_GRAY));
			}

			if (own != null) {
				tip.add(Txt.t(own ? "roles.state.allow" : "roles.state.deny").withStyle(own ? ChatFormatting.GREEN : ChatFormatting.RED));
			} else if (wider != null) {
				tip.add(Txt.t(wider ? "roles.state.inherit.allow" : "roles.state.inherit.deny", from[0]).withStyle(ChatFormatting.GRAY));
			} else if (!head) {
				tip.add(Txt.t(line.info().level <= 0 ? "roles.state.default.everyone" : "roles.state.default.level", line.info().level).withStyle(ChatFormatting.GRAY));
			}

			CFHGuiTooltips.request(tip, this.nodes);
		}
	}

	private int rows() {
		return this.set == null ? 0 : this.set.roles.size() + (this.fresh && this.draft != null ? 1 : 0);
	}

	private int maxScroll() {
		return Math.max(0, this.rows() * 24 + 2 - this.listArea.h());
	}

	private void scrollTo(double y, boolean atOnce) {
		float to = (float)Math.max(0.0, Math.min((double)this.maxScroll(), y));
		if (atOnce) {
			this.scroll.snap(to);
		} else {
			this.scroll.set(to);
		}
	}

	private int thumbHeight() {
		int h = this.listArea.h() - 4;
		return Math.max(12, Math.min(h, (int)((long)h * (long)this.listArea.h() / (long)Math.max(1, this.rows() * 24 + 2))));
	}

	private int thumbY() {
		return this.listArea.y() + 2 + (int)((float)(this.listArea.h() - 4 - this.thumbHeight()) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
	}

	private boolean onBar(double mx, double my) {
		return this.maxScroll() > 0
			&& mx >= (double)(this.listArea.right() - 8)
			&& mx < (double)this.listArea.right()
			&& my >= (double)this.listArea.y()
			&& my < (double)this.listArea.bottom();
	}

	private void show(int index) {
		float at = this.scroll.target();
		if ((float)(index * 24) < at) {
			this.scrollTo((double)(index * 24), false);
		} else if ((float)((index + 1) * 24 + 2) > at + (float)this.listArea.h()) {
			this.scrollTo((double)((index + 1) * 24 + 2 - this.listArea.h()), false);
		}
	}

	private int rowAt(double mx, double my) {
		if (this.set != null && this.listArea.contains(mx, my) && !this.onBar(mx, my)) {
			int i = (int)((my - (double)this.listArea.y() + (double)this.scroll.get()) / 24.0);
			return i >= 0 && i < this.set.roles.size() ? i : -1;
		} else {
			return -1;
		}
	}

	private void leave(Runnable then) {
		if (!this.dirty) {
			then.run();
		} else {
			CFHGuiModal.confirmDanger(
				Txt.t("roles.discard.title"), Txt.t("roles.discard.body", this.draft == null ? "" : this.draft.name), Txt.t("roles.discard.ok"), () -> {
					this.dirty = false;
					this.fresh = false;
					then.run();
				}
			);
		}
	}

	private void select(String id) {
		if (!id.equals(this.selected) || this.fresh) {
			this.leave(() -> {
				this.selected = id;
				this.fresh = false;
				this.draft = copy(this.role(id));
				this.reveal = true;
				this.screen.refreshPage();
			});
		}
	}

	private void newRole() {
		this.leave(
			() -> new Forms.Prompt(
						Txt.t("roles.new.title"),
						Icon.CROWN,
						Txt.t("form.name"),
						"",
						24,
						Txt.t("roles.new.ok"),
						v -> !Role.idOf(v).isEmpty() && this.role(Role.idOf(v)) == null,
						(v, then) -> {
							Role r = new Role();
							r.id = Role.idOf(v);
							r.name = v.trim();
							r.color = 10134964;
							r.inherits.add("default");
							int lowest = 0;
							if (this.set != null) {
								for (Role other : this.set.roles) {
									if (other.everyone) {
										lowest = Math.max(lowest, other.priority);
									}
								}
							}

							r.priority = lowest + 5;
							this.draft = r;
							this.selected = r.id;
							this.fresh = true;
							this.dirty = true;
							this.reveal = true;
							then.accept(new ServerNet.Result(0, true, "ok", List.of()));
							this.screen.refreshPage();
						}
					)
					.hint(Txt.t("roles.new.hint"))
					.show()
		);
	}

	private void save() {
		Role d = this.draft;
		if (d != null && !d.name.isBlank()) {
			ServerActions.roleSave(d, AdminScreen.after(() -> {
				this.dirty = false;
				this.fresh = false;
				this.selected = d.id;
				this.draft = null;
				this.ask();
			}));
		}
	}

	private void move(int from, int to) {
		if (this.set != null && from != to && from >= 0 && from < this.set.roles.size()) {
			List<Role> order = new ArrayList<>(this.set.roles);
			Role moved = order.remove(from);
			if (this.editable(moved)) {
				int first = 0;

				while (first < order.size() && !this.editable(order.get(first))) {
					first++;
				}

				int at = Math.max(first, Math.min(order.size(), to));
				order.add(at, moved);
				int above = at == 0 ? Math.min(this.set.editableBelow, order.size() > 1 ? order.get(1).priority + 21 : 11) : order.get(at - 1).priority;
				int below = at == order.size() - 1 ? (at == 0 ? 0 : Math.min(order.get(at - 1).priority - 20, -1)) : order.get(at + 1).priority;
				List<Role> changed = new ArrayList<>();
				if (above - below >= 2) {
					Role c = copy(moved);
					c.priority = below + (above - below) / 2;
					changed.add(c);
				} else {
					List<Role> mine = new ArrayList<>();

					for (Role r : order) {
						if (this.editable(r)) {
							mine.add(r);
						}
					}

					int step = Math.max(1, Math.min(10, (int)Math.min(2147483647L, ((long)this.set.editableBelow - 1L) / (long)Math.max(1, mine.size()))));

					for (int i = 0; i < mine.size(); i++) {
						int priority = (mine.size() - 1 - i) * step;
						if (mine.get(i).priority != priority) {
							Role c = copy(mine.get(i));
							c.priority = priority;
							changed.add(c);
						}
					}
				}

				if (!changed.isEmpty()) {
					int[] left = new int[]{changed.size()};

					for (Role c : changed) {
						ServerActions.roleSave(c, AdminScreen.after(() -> {
							if (--left[0] == 0) {
								if (!this.dirty) {
									this.draft = null;
								}

								this.ask();
							}
						}));
					}
				}
			}
		}
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
		AdminData.RoleSet rs = this.set;
		if (rs != null && this.listArea.w() > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			Rect l = this.listArea;
			Ui.well(g, l.x(), l.y(), l.w(), l.h(), 1.0F);
			boolean down = GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), 0) == 1;
			boolean held = this.pressRow >= 0 && down;
			if (this.pressRow >= 0 && !held) {
				if (this.dragging && this.dropRow >= 0) {
					this.move(this.pressRow, this.dropRow > this.pressRow ? this.dropRow - 1 : this.dropRow);
				}

				this.pressRow = -1;
				this.dragging = false;
			}

			if (held && !this.dragging && Math.abs((double)mouseY - this.pressY) > 4.0 && this.pressRow < rs.roles.size() && this.editable(rs.roles.get(this.pressRow))) {
				this.dragging = true;
			}

			this.dragY = (double)mouseY;
			int max = this.maxScroll();
			if (this.scroll.target() > (float)max) {
				this.scroll.snap((float)max);
			}

			if (this.reveal) {
				this.reveal = false;
				if (this.fresh && this.draft != null) {
					this.show(rs.roles.size());
				} else {
					for (int i = 0; i < rs.roles.size(); i++) {
						if (rs.roles.get(i).id.equals(this.selected)) {
							this.show(i);
						}
					}
				}
			}

			if (this.grabbed && !down) {
				this.grabbed = false;
			}

			if (this.grabbed) {
				this.scrollTo(this.grabScroll + ((double)mouseY - this.grabY) * (double)max / (double)Math.max(1, l.h() - 4 - this.thumbHeight()), true);
			}

			if (this.dragging && max > 0) {
				int edge = mouseY < l.y() + 12 ? -1 : (mouseY > l.bottom() - 12 ? 1 : 0);
				if (edge != 0) {
					this.scrollTo((double)this.scroll.get() + (double)((float)edge * Clock.frameDelta()) * 0.2, true);
				}
			}

			int sy = Math.round(this.scroll.get());
			int rowW = l.w() - 4 - (max > 0 ? 6 : 0);
			this.dropRow = this.dragging ? Math.max(0, Math.min(rs.roles.size(), (int)Math.round((double)(mouseY - l.y() + sy) / 24.0))) : -1;
			int hover = !this.dragging && !this.grabbed ? this.rowAt((double)mouseX, (double)mouseY) : -1;
			CFHGuiDraw.scissor(g, l.x() + 1, l.y() + 1, l.w() - 2, l.h() - 2);

			for (int ix = 0; ix < rs.roles.size(); ix++) {
				if (!this.dragging || ix != this.pressRow) {
					int y = l.y() + 2 + ix * 24 - sy;
					if (y + 24 > l.y() && y < l.bottom()) {
						this.roleRow(g, rs, ix, l.x() + 2, y, rowW, ix == hover, 1.0F);
					}
				}
			}

			if (this.fresh && this.draft != null) {
				int y = l.y() + 2 + rs.roles.size() * 24 - sy;
				CFHGuiDraw.pixelRound(g, l.x() + 2, y, rowW, 22, p.surfaceActive(), t.isVanilla() ? 0 : 2);
				CFHGuiDraw.pixelCircle(g, l.x() + 17, y + 11, 3, Ui.opaque(this.draft.color));
				Ui.textFit(g, Component.literal(this.draft.name), l.x() + 26, y + 7, rowW - 66, Ui.strong(), 1.0F);
				Ui.chipRight(g, Txt.t("roles.unsaved"), l.x() + rowW - 2, y + 5, p.warning(), 1.0F);
			}

			if (this.dragging && this.dropRow >= 0) {
				CFHGuiDraw.rect(g, l.x() + 4, l.y() + 1 + this.dropRow * 24 - sy, rowW - 4, 2, p.accent());
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 100.0F);
				this.roleRow(g, rs, this.pressRow, l.x() + 6, (int)this.dragY - 12, rowW - 4, true, 0.92F);
				g.pose().popPose();
			}

			CFHGuiDraw.unscissor(g);
			if (max > 0) {
				int bx = l.right() - 4 - 2;
				int th = this.thumbHeight();
				int ty = this.thumbY();
				boolean lit = this.grabbed || !this.dragging && this.onBar((double)mouseX, (double)mouseY);
				if (t.isVanilla()) {
					g.fill(bx, l.y() + 2, bx + 4, l.bottom() - 2, -16777216);
					g.fill(bx, ty, bx + 4, ty + th, lit ? -6250336 : -8355712);
					g.fill(bx, ty, bx + 4 - 1, ty + th - 1, lit ? -2039584 : -4144960);
				} else {
					CFHGuiDraw.pixelRound(g, bx, l.y() + 2, 4, l.h() - 4, Colors.withAlpha(p.surface(), 128), 1);
					CFHGuiDraw.pixelRound(g, bx, ty, 4, th, this.grabbed ? p.accent() : (lit ? p.textMuted() : p.borderStrong()), 1);
				}
			}

			if (hover >= 0 && hover < rs.roles.size()) {
				Role r = rs.roles.get(hover);
				List<Component> tip = new ArrayList<>();
				tip.add(Component.literal(r.name).withStyle(st -> st.withColor(Ui.ink(r.color) & 16777215)));
				tip.add(Txt.t("roles.tip.priority", r.priority).withStyle(ChatFormatting.GRAY));
				tip.add(
					Txt.t(r.everyone ? "roles.tip.everyone" : "roles.tip.holders", hover < rs.holders.size() ? rs.holders.get(hover) : 0).withStyle(ChatFormatting.GRAY)
				);
				tip.add(Txt.t(this.editable(r) ? "roles.tip.drag" : "roles.tip.locked").withStyle(ChatFormatting.DARK_GRAY));
				CFHGuiTooltips.request(tip, null);
			}

			Role d = this.draft;
			if (d == null) {
				Ui.empty(g, this.editorArea, Icon.CROWN, Txt.t("roles.none.title"), Txt.t("roles.none"), 1.0F);
			} else {
				Rect e = this.editorArea;
				CFHGuiDraw.pixelCircle(g, e.x() + 6, e.y() + 11, 4, Ui.opaque(d.color));
				int used = Ui.textFit(g, Fonts.heading(d.name.isEmpty() ? d.id : d.name), e.x() + 16, e.y() + 7, e.w() - 250, Ui.ink(d.color), 1.0F);
				if (!d.prefix.isEmpty() && used + 60 < e.w() - 250) {
					Ui.chip(g, Component.literal(d.prefix), e.x() + 22 + used, e.y() + 5, Ui.ink(d.color), 1.0F);
				}

				if (this.save != null) {
					this.save.active = this.dirty && this.mayEdit() && !d.name.isBlank();
					this.revert.active = this.dirty;
					if (this.dirty) {
						Ui.textRight(g, Txt.t("roles.unsaved"), this.revert.getX() - 8, e.bottom() - 14, p.warning(), 1.0F);
					}
				}

				Ui.rule(g, e.x(), e.y() + 25, e.w(), 1.0F);
			}
		}
	}

	private void roleRow(GuiGraphics g, AdminData.RoleSet rs, int index, int x, int y, int w, boolean hot, float a) {
		Theme t = Theme.current();
		Palette p = t.palette();
		Role r = rs.roles.get(index);
		boolean sel = r.id.equals(this.selected) && !this.fresh;
		if (sel || hot) {
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, w, 22, Colors.scaleAlpha(sel ? 1627389951 : 822083583, a));
				if (sel) {
					CFHGuiDraw.outline(g, x, y, w, 22, Colors.scaleAlpha(-1, a), 0);
				}
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, 22, Colors.scaleAlpha(sel ? p.surfaceActive() : p.surfaceHover(), a), 2);
				if (sel) {
					CFHGuiDraw.rect(g, x, y + 4, 2, 14, Colors.scaleAlpha(p.accent(), a));
				}
			}
		}

		boolean can = this.editable(r);
		Icons.draw(g, can ? Icon.DRAG : Icon.LOCK, x + 4, y + 7, 8, Colors.scaleAlpha(hot && can ? p.textMuted() : p.textDim(), a * (can ? 1.0F : 0.7F)));
		CFHGuiDraw.pixelCircle(g, x + 20, y + 11, 3, Colors.scaleAlpha(Ui.opaque(r.color), a));
		String count = r.everyone ? "∞" : Txt.number(index < rs.holders.size() ? (long)rs.holders.get(index).intValue() : 0L);
		Ui.textRight(g, Component.literal(count), x + w - 6, y + 7, sel ? Ui.soft() : Ui.caption(), a);
		Ui.textFit(g, Component.literal(r.name), x + 29, y + 7, w - 40 - CFHGuiDraw.width(count), sel ? Ui.strong() : Ui.ink(r.color), a);
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.set != null && this.onBar(mouseX, mouseY)) {
			int th = this.thumbHeight();
			int ty = this.thumbY();
			if (mouseY < (double)ty || mouseY >= (double)(ty + th)) {
				this.scrollTo(
					(mouseY - (double)this.listArea.y() - 2.0 - (double)th / 2.0) / (double)Math.max(1, this.listArea.h() - 4 - th) * (double)this.maxScroll(), true
				);
			}

			this.grabbed = true;
			this.grabY = mouseY;
			this.grabScroll = (double)this.scroll.target();
			return true;
		} else {
			int row = this.rowAt(mouseX, mouseY);
			if (row >= 0 && button == 0 && this.set != null) {
				this.pressRow = row;
				this.pressY = mouseY;
				this.dragging = false;
				this.select(this.set.roles.get(row).id);
				return true;
			} else {
				return false;
			}
		}
	}

	boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (this.set != null && this.listArea.contains(mouseX, mouseY) && this.maxScroll() > 0) {
			this.scrollTo((double)this.scroll.target() - scrollY * 24.0, false);
			return true;
		} else {
			return false;
		}
	}

	boolean keyPressed(int keyCode, int modifiers) {
		boolean alt = (modifiers & 4) != 0;
		boolean ctrl = (modifiers & 2) != 0;
		if (this.set != null && this.selected != null && !this.fresh && alt != ctrl && (keyCode == 264 || keyCode == 265)) {
			for (int i = 0; i < this.set.roles.size(); i++) {
				if (this.set.roles.get(i).id.equals(this.selected)) {
					int to = keyCode == 265 ? i - 1 : i + 1;
					if (alt) {
						this.move(i, to);
					} else if (to >= 0 && to < this.set.roles.size()) {
						this.select(this.set.roles.get(to).id);
					}

					return true;
				}
			}

			return false;
		} else {
			return false;
		}
	}

	private static record Line(String group, String pattern, @Nullable AdminData.NodeInfo info) {
	}
}
