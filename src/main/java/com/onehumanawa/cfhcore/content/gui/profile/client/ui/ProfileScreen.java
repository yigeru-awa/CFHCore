package com.onehumanawa.cfhcore.content.gui.profile.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures.Loaded;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.media.FilePicker;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageWidget;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import com.onehumanawa.cfhcore.content.gui.profile.client.AppliedLooks;
import com.onehumanawa.cfhcore.content.gui.profile.client.Cosmetic;
import com.onehumanawa.cfhcore.content.gui.profile.client.Cosmetics;
import com.onehumanawa.cfhcore.content.gui.profile.client.ProfileClient;
import com.onehumanawa.cfhcore.content.gui.profile.client.ProfileStore;
import com.onehumanawa.cfhcore.content.gui.profile.client.SkinComposer;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class ProfileScreen extends CFHGuiScreen {
	private final boolean overhaul;
	private ProfileStore.Account account;
	@Nullable
	private LookStage looks;
	@Nullable
	private Shelf shelf;
	private boolean editing;
	@Nullable
	private Slot slot;
	private boolean closeUp = true;
	private String query = "";
	private String thingQuery = "";
	@Nullable
	private String viewedId;
	private boolean viewingBlank;
	private Rect stageRect = new Rect(0, 0, 0, 0);
	private int nameY;
	private int captionY = -1;
	private float nameScale = 1.0F;
	private Rect cardRect = new Rect(0, 0, 0, 0);
	private int groupsY = -1;
	private int groupsW;
	private boolean turning;
	private static boolean turnedOnce;
	@Nullable
	private Loaded picture;
	@Nullable
	private Path pictureOf;
	@Nullable
	private MultiLineEditBox bio;
	private boolean bioDirty;
	private int bioTicks;
	@Nullable
	private Slot startWith;
	@Nullable
	private StageWidget deferred;

	public ProfileScreen(@Nullable Screen parent, boolean overhaul) {
		super(Component.translatable("cfhgui_profile.title"), parent);
		this.overhaul = overhaul;
		this.account = ProfileClient.account();
		ProfileStore.Look worn = this.account.worn();
		this.viewedId = worn == null ? null : worn.id();
	}

	public ProfileScreen startEditing(@Nullable Slot slot) {
		this.startWith = slot == null ? Slot.SKIN : slot;
		return this;
	}

	public ProfileScreen startBlank() {
		this.viewingBlank = true;
		return this;
	}

	private boolean own() {
		return this.account.uuid().equals(ProfileClient.self());
	}

	public Component getTitle() {
		if (!this.editing) {
			return super.getTitle();
		} else {
			ProfileStore.Look look = this.viewed();
			String of = look == null ? "" : look.name();
			return Component.translatable(
				"cfhgui_profile.title.edit", new Object[]{of, this.slot == null ? Component.translatable("cfhgui_profile.slot.all") : slotName(this.slot)}
			);
		}
	}

	static Component slotName(Slot slot) {
		return Component.translatable("cfhgui_profile.slot." + slot.key());
	}

	private List<ProfileStore.Look> listed() {
		List<ProfileStore.Look> out = new ArrayList<>();
		String q = this.query.trim().toLowerCase(Locale.ROOT);

		for (ProfileStore.Look l : this.account.looks()) {
			if (q.isEmpty() || l.name().toLowerCase(Locale.ROOT).contains(q)) {
				out.add(l);
			}
		}

		return out;
	}

	@Nullable
	private ProfileStore.Look viewed() {
		if (this.looks != null && this.overhaul) {
			LookStage.Stand st = this.looks.viewedStand();
			return st == null ? null : st.look;
		} else {
			return this.account.look(this.viewedId);
		}
	}

	private void fillStage() {
		if (this.looks != null) {
			if (this.overhaul) {
				if (this.viewingBlank) {
					this.looks.show(this.listed(), true, null, Integer.MAX_VALUE);
				} else {
					this.looks.show(this.listed(), this.query.isBlank(), this.viewedId);
				}
			} else {
				ProfileStore.Look one = this.account.look(this.viewedId);
				this.looks.show(one == null ? List.of() : List.of(one), false, this.viewedId);
			}

			ProfileStore.Look worn = this.account.worn();
			this.looks.worn(worn == null ? "" : worn.id());
		}
	}

	private void view(int index) {
		if (this.looks != null) {
			this.looks.view(index);
			LookStage.Stand st = this.looks.viewedStand();
			this.viewingBlank = st != null && st.look == null;
			this.viewedId = st != null && st.look != null ? st.look.id() : this.viewedId;
			this.rebuildWidgets();
		}
	}

	protected void build() {
		if (this.looks == null || this.looks.stage.isClosed()) {
			this.looks = new LookStage(this, this.overhaul);
			this.looks.onPick(i -> {
				if (this.looks == null || i != this.looks.viewed()) {
					this.view(i);
				} else if (!this.editing) {
					this.edit(true);
				}
			});
			if (this.viewedId == null && !this.account.looks().isEmpty()) {
				this.viewedId = this.account.looks().get(0).id();
			}

			this.fillStage();
		}

		if (this.startWith != null) {
			Slot s = this.startWith;
			this.startWith = null;
			if (this.viewed() != null) {
				this.editing = true;
				this.slot = s;
				this.looks.edit(true);
				this.looks.focus(this.closeUp ? s : null);
			}
		}

		this.bio = null;
		if (!this.overhaul) {
			this.buildLists();
		} else if (this.editing) {
			this.buildEdit();
		} else {
			this.buildBrowse();
		}
	}

	private void stageWidget(Rect r) {
		this.stageRect = r;
		if (this.looks != null) {
			this.looks.shape((float)r.w() / (float)Math.max(1, r.h()));
			StageWidget view = new StageWidget(r.x(), r.y(), r.w(), r.h(), this.looks.stage, this.getTitle());
			this.addRenderableOnly(view);
			this.deferred = view;
		}
	}

	private void finishStage() {
		if (this.deferred != null) {
			this.addWidget(this.deferred);
		}

		this.deferred = null;
	}

	private void buildBrowse() {
		ProfileStore.Look look = this.viewed();
		CFHGuiSearchField search = new CFHGuiSearchField(0, 0, this.width >= 520 ? 140 : 96, q -> {
			if (!q.equals(this.query)) {
				this.query = q;
				this.fillStage();
			}
		});
		search.setValue(this.query);
		this.addHeaderAction(search);
		this.slashFocusTarget = search;
		int mid = this.width / 2;
		int cardH = Mth.clamp(Math.round((float)this.height * 0.17F), 56, 78);
		int cardY = this.height - 12 - cardH;
		int by = cardY - 8 - 20;
		int places = this.looks == null ? 0 : this.looks.stands().size();
		int y = by;
		if (this.height >= 280 && places >= 2 && places <= 24) {
			y = by - 13;
			this.add(new ProfileScreen.LookDots(mid, y, places));
		}

		this.captionY = -1;
		if (this.height >= 300) {
			y -= 13;
			this.captionY = y;
		}

		this.nameScale = this.height >= 400 ? 2.0F : (this.height >= 300 ? 1.5F : 1.0F);
		y -= Math.round(9.0F * this.nameScale) + 4;
		this.nameY = y;
		int top = 32;
		int stageH = Math.max(60, this.nameY - 3 - 32);
		this.stageWidget(new Rect(0, 32, this.width, stageH));
		int arrow = this.height >= 330 ? 24 : 20;
		this.add(
			new CFHGuiIconButton(12, 32 + stageH / 2 - arrow / 2, arrow, Icon.CHEVRON_LEFT, Component.translatable("cfhgui_profile.previous"), () -> this.step(-1))
		);
		this.add(
			new CFHGuiIconButton(
				this.width - 12 - arrow, 32 + stageH / 2 - arrow / 2, arrow, Icon.CHEVRON_RIGHT, Component.translatable("cfhgui_profile.next"), () -> this.step(1)
			)
		);
		if (look == null) {
			int w = Math.min(150, this.width - 24);
			this.add(
				new CFHGuiButton(mid - w / 2, by, w, Component.translatable("cfhgui_profile.create"), this::create)
					.variant(Variant.PRIMARY)
					.icon(Icon.PLUS)
					.enabled(this.own())
			);
		} else {
			boolean worn = this.account.worn() != null && this.account.worn().id().equals(look.id());
			int bw = Mth.clamp((this.width - 24 - 80) / 2, 56, 110);
			CFHGuiButton select = new CFHGuiButton(mid - bw - 2, by, bw, Component.translatable(worn ? "cfhgui_profile.worn" : "cfhgui_profile.select"), () -> {
				ProfileClient.wear(worn ? null : look);
				if (this.looks != null) {
					this.looks.worn(worn ? "" : look.id());
				}

				this.rebuildWidgets();
			}).variant(worn ? Variant.SECONDARY : Variant.PRIMARY).icon(worn ? Icon.CHECK : null);
			select.tip(Component.translatable(!this.own() ? "cfhgui_profile.not_signed_in" : (worn ? "cfhgui_profile.worn.tip" : "cfhgui_profile.select.tip")));
			select.active = this.own();
			this.add(select);
			this.add(new CFHGuiButton(mid + 2, by, bw, Component.translatable("cfhgui_profile.edit"), () -> this.edit(true)).icon(Icon.EDIT));
			int moreX = mid + bw + 6;
			this.add(new CFHGuiIconButton(moreX, by, 20, Icon.DOTS, Component.translatable("cfhgui_profile.more"), () -> this.lookMenu(look, moreX, by)));
		}

		this.profile(new Rect(12, cardY, this.width - 24, cardH));
		this.finishStage();
	}

	private Component madeOf(@Nullable ProfileStore.Look look) {
		if (look == null) {
			return Component.translatable("cfhgui_profile.new_look.caption");
		} else {
			List<Cosmetic> things = look.items();
			if (things.isEmpty()) {
				return thingName(Slot.SKIN, look.skin());
			} else {
				MutableComponent line = Component.empty();

				for (int k = 0; k < things.size(); k++) {
					if (k > 0) {
						line.append(" · ");
					}

					line.append(things.get(k).name());
				}

				return line;
			}
		}
	}

	private void step(int by) {
		if (this.looks != null && !this.looks.stands().isEmpty()) {
			this.view(this.looks.viewed() + by);
		}
	}

	private void create() {
		if (this.own()) {
			int n = this.account.data().looks.size() + 1;
			ProfileStore.Look made = this.account
				.create(
					Component.translatable("cfhgui_profile.look_n", new Object[]{n}).getString(), "account", SkinComposer.accountSlim(this.account.uuid(), this.account.name())
				);
			this.viewedId = made.id();
			this.viewingBlank = false;
			this.query = "";
			this.fillStage();
			this.edit(true);
		}
	}

	private void lookMenu(ProfileStore.Look look, int x, int y) {
		List<Item> items = new ArrayList<>();
		items.add(
			Item.of(
				Component.translatable("cfhgui_profile.rename"),
				Icon.EDIT,
				() -> CFHGuiModal.prompt(Component.translatable("cfhgui_profile.rename"), null, look.name(), name -> {
						if (!name.isBlank()) {
							look.name(name.trim());
						}

						this.fillStage();
						this.rebuildWidgets();
					})
			)
		);
		items.add(Item.of(Component.translatable("cfhgui_profile.duplicate"), Icon.DUPLICATE, () -> {
			ProfileStore.Look copy = this.account.duplicate(look, Component.translatable("cfhgui_profile.copy_of", new Object[]{look.name()}).getString());
			this.viewedId = copy.id();
			this.fillStage();
			this.rebuildWidgets();
		}));
		items.add(Item.sep());
		items.add(
			Item.danger(
				Component.translatable("cfhgui_profile.delete"),
				Icon.TRASH,
				() -> CFHGuiModal.confirmDanger(
						Component.translatable("cfhgui_profile.delete"),
						Component.translatable("cfhgui_profile.delete.body", new Object[]{look.name()}),
						Component.translatable("cfhgui_profile.delete"),
						() -> {
							this.account.remove(look);
							if (this.own()) {
								ProfileClient.changed();
							}

							this.viewedId = this.account.looks().isEmpty() ? null : this.account.looks().get(0).id();
							this.fillStage();
							this.rebuildWidgets();
						}
					)
			)
		);
		CFHGuiContextMenu.open((double)x, (double)(y - 4), items);
	}

	private void edit(boolean on) {
		if (!on || this.viewed() != null) {
			this.editing = on;
			if (!on) {
				this.slot = null;
			}

			if (this.looks != null) {
				this.looks.edit(on);
				this.looks.focus(on && this.closeUp ? this.slot : null);
				LookStage.Stand st = this.looks.viewedStand();
				if (st != null) {
					st.node.tryOn(null);
				}
			}

			this.rebuildWidgets();
		}
	}

	private void choose(Slot s) {
		this.slot = s;
		this.thingQuery = "";
		if (this.looks != null) {
			this.looks.focus(this.closeUp ? s : null);
		}

		this.rebuildWidgets();
	}

	private void buildEdit() {
		ProfileStore.Look look = this.viewed();
		if (look == null) {
			this.editing = false;
			this.buildBrowse();
		} else {
			this.addHeaderAction(new CFHGuiButton(0, 0, 64, Component.translatable("gui.done"), () -> this.edit(false)).variant(Variant.PRIMARY).icon(Icon.CHECK));
			int colW = Mth.clamp(Math.round((float)this.width * 0.24F), 78, 170);
			int box = Shelf.boxFor(this.height);
			int shelfY = this.height - 12 - (box + 12) + 2;
			this.groupsY = this.height >= 300 ? 38 : -1;
			this.groupsW = colW;
			int top = this.groupsY >= 0 ? this.groupsY + 13 : 37;
			int rows = Math.max(Slot.body().size(), Slot.wear().size());
			int rowH = Mth.clamp((shelfY - 6 - top) / rows - 2, 15, 30);
			int y = top;

			for (Slot s : Slot.body()) {
				this.add(new ProfileScreen.SlotButton(12, y, colW, rowH, s, look));
				y += rowH + 2;
			}

			y = top;

			for (Slot s : Slot.wear()) {
				this.add(new ProfileScreen.SlotButton(this.width - 12 - colW, y, colW, rowH, s, look));
				y += rowH + 2;
			}

			int sx = 12 + colW + 6;
			int sw = this.width - 2 * (12 + colW + 6);
			int searchY = shelfY - 25;
			this.stageWidget(new Rect(sx, 33, sw, Math.max(40, searchY - 3 - 33)));
			this.add(
				new CFHGuiIconButton(sx + sw - 22, 37, 18, Icon.SEARCH, Component.translatable(this.closeUp ? "cfhgui_profile.zoom.off" : "cfhgui_profile.zoom.on"), () -> {
					this.closeUp = !this.closeUp;
					if (this.looks != null) {
						this.looks.focus(this.closeUp ? this.slot : null);
					}

					this.rebuildWidgets();
				}).toggled(this.closeUp)
			);
			boolean skin = this.slot == Slot.SKIN;
			int modelW = skin ? Math.min(96, sw / 2 - 2) : 0;
			int fieldW = Math.min(150, sw - modelW - (skin ? 4 : 0));
			int fx = sx + (sw - fieldW - modelW - (skin ? 4 : 0)) / 2;
			CFHGuiSearchField search = new CFHGuiSearchField(fx, searchY, fieldW, q -> {
				if (!q.equals(this.thingQuery)) {
					this.thingQuery = q;
					this.fillShelf(look);
				}
			});
			search.setValue(this.thingQuery);
			this.add(search);
			this.slashFocusTarget = search;
			if (skin) {
				this.add(
					new CFHGuiSegmented<>(
						fx + fieldW + 4,
						searchY,
						modelW,
						List.of(Boolean.FALSE, Boolean.TRUE),
						look.slim(),
						slim -> Component.translatable(slim ? "cfhgui_profile.model.slim" : "cfhgui_profile.model.wide"),
						slim -> {
							look.slim(slim);
							this.touched(look);
							this.fillShelf(look);
						}
					)
				);
			}

			if (this.shelf != null) {
				this.shelf.close();
			}

			this.shelf = (Shelf)this.add(new Shelf(this, 12, shelfY, this.width - 24, box, e -> this.pick(look, e), e -> {
				LookStage.Stand st = this.looks == null ? null : this.looks.viewedStand();
				if (st != null) {
					st.node.tryOn(e == null ? null : e.cosmetic);
				}
			}));
			this.fillShelf(look);
			this.finishStage();
		}
	}

	private void fillShelf(ProfileStore.Look look) {
		if (this.shelf != null) {
			List<Shelf.Entry> out = new ArrayList<>();
			String q = this.thingQuery.trim().toLowerCase(Locale.ROOT);
			if (this.slot == null) {
				this.shelf.show(out);
			} else {
				if (this.slot == Slot.SKIN) {
					Shelf.Entry bring = Shelf.sign(
						"upload", Component.translatable("file".equals(look.skin()) ? "cfhgui_profile.skin.yours" : "cfhgui_profile.skin.upload"), Icon.UPLOAD
					);
					bring.chosen = "file".equals(look.skin());
					out.add(bring);
					ResourceLocation own = SkinComposer.accountSkin(this.account.uuid(), this.account.name()).texture();
					Shelf.Entry acc = Shelf.skin("account", Component.translatable("cfhgui_profile.skin.account"), own, look.slim());
					acc.chosen = "account".equals(look.skin());
					out.add(acc);

					for (String name : ProfileStore.Look.DEFAULTS) {
						Component label = Component.literal(Character.toUpperCase(name.charAt(0)) + name.substring(1));
						if (q.isEmpty() || label.getString().toLowerCase(Locale.ROOT).contains(q)) {
							Shelf.Entry e = Shelf.skin("default:" + name, label, SkinComposer.defaultSkin(name, look.slim()), look.slim());
							e.chosen = ("default:" + name).equals(look.skin());
							out.add(e);
						}
					}
				} else {
					Cosmetic in = look.in(this.slot);
					Shelf.Entry none = Shelf.sign("none", Component.translatable("cfhgui_profile.slot." + this.slot.key() + ".none"), Icon.CLOSE);
					none.chosen = in == null;
					out.add(none);

					for (Cosmetic c : Cosmetics.of(this.slot)) {
						if (q.isEmpty() || c.name().getString().toLowerCase(Locale.ROOT).contains(q)) {
							Shelf.Entry e = c.isModel() ? Shelf.thing(c) : Shelf.painted(c, look.slim());
							e.chosen = in == c;
							out.add(e);
						}
					}
				}

				this.shelf.show(out);
			}
		}
	}

	private void pick(ProfileStore.Look look, Shelf.Entry e) {
		if (this.slot != null) {
			if (this.slot == Slot.SKIN) {
				if ("upload".equals(e.id)) {
					this.bringSkin(look);
					return;
				}

				look.skin(e.id);
				if ("account".equals(e.id)) {
					look.slim(SkinComposer.accountSlim(this.account.uuid(), this.account.name()));
				}
			} else {
				look.put(this.slot, e.cosmetic);
			}

			this.touched(look);
			this.fillShelf(look);
		}
	}

	private void touched(ProfileStore.Look look) {
		ProfileStore.Look worn = this.account.worn();
		if (this.own() && worn != null && worn.id().equals(look.id())) {
			ProfileClient.changed();
		}
	}

	private void bringSkin(ProfileStore.Look look) {
		if (!FilePicker.available()) {
			CFHGuiToasts.show(Component.translatable("cfhgui_profile.skin.upload"), Component.translatable("cfhgui_profile.skin.windowed"), Icon.WARNING);
		} else {
			FilePicker.pickFile(Component.translatable("cfhgui_profile.skin.upload").getString(), List.of("*.png"), "Minecraft skins (PNG)", path -> {
				boolean fits = false;

				try (InputStream in = Files.newInputStream(path)) {
					NativeImage image = NativeImage.read(in);

					try {
						fits = image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32);
					} catch (Throwable var10) {
						if (image != null) {
							try {
								image.close();
							} catch (Throwable var9) {
								var10.addSuppressed(var9);
							}
						}

						throw var10;
					}

					if (image != null) {
						image.close();
					}
				} catch (Exception var12) {
				}

				if (fits && look.upload(path)) {
					this.touched(look);
					if (this.slot == Slot.SKIN) {
						this.fillShelf(look);
					}
				} else {
					CFHGuiToasts.show(Component.translatable("cfhgui_profile.skin.upload"), Component.translatable("cfhgui_profile.skin.not_a_skin"), Icon.ERROR);
				}
			});
		}
	}

	private void buildLists() {
		ProfileStore.Look look = this.account.look(this.viewedId);
		int top = 38;
		int profileH = Mth.clamp(Math.round((float)this.height * 0.24F), 46, 70);
		int bodyH = this.height - 38 - 12 - profileH - 6;
		int listW = Mth.clamp(Math.round((float)this.width * 0.26F), 90, 150);
		CFHGuiList<ProfileStore.Look> list = new CFHGuiList<ProfileStore.Look>(12, 38, listW, Math.max(20, bodyH - 24), 22, (g, item, index, x, yx, w, h, hovered, selected, mx, my) -> {
			Theme t = Theme.current();
			boolean wornx = this.account.worn() != null && this.account.worn().id().equals(item.id());
			Icons.draw(g, wornx ? Icon.CHECK : Icon.USER, x + 5, yx + (h - 12) / 2, 12, wornx ? t.accentInk() : t.muted());
			g.drawString(this.font, CFHGuiDraw.truncate(Component.literal(item.name()), w - 26), x + 21, yx + (h - 8) / 2, t.isVanilla() ? -1 : t.text(), t.isVanilla());
		}).gap(2).emptyText(Component.translatable("cfhgui_profile.no_looks"));
		List<ProfileStore.Look> all = this.account.looks();
		list.items(all);

		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).id().equals(this.viewedId)) {
				list.select(i);
			}
		}

		list.onSelect(l -> {
			this.viewedId = l.id();
			this.fillStage();
			this.rebuildWidgets();
		});
		this.add(list);
		this.add(
			new CFHGuiButton(
					12,
					38 + bodyH - 20,
					listW,
					Component.translatable("cfhgui_profile.create"),
					() -> {
						if (this.own()) {
							ProfileStore.Look made = this.account
								.create(
									Component.translatable("cfhgui_profile.look_n", new Object[]{this.account.data().looks.size() + 1}).getString(),
									"account",
									SkinComposer.accountSlim(this.account.uuid(), this.account.name())
								);
							this.viewedId = made.id();
							this.fillStage();
							this.rebuildWidgets();
						}
					}
				)
				.icon(Icon.PLUS)
				.enabled(this.own())
		);
		int slotsW = Mth.clamp(Math.round((float)this.width * 0.36F), 120, 220);
		int px = 12 + listW + 6;
		int pw = this.width - 12 - slotsW - 6 - px;
		this.stageWidget(new Rect(px, 38, Math.max(40, pw), Math.max(40, bodyH - 24)));
		if (look != null) {
			boolean worn = this.account.worn() != null && this.account.worn().id().equals(look.id());
			int bw = Math.max(40, (pw - 24) / 2);
			CFHGuiButton select = new CFHGuiButton(px, 38 + bodyH - 20, bw, Component.translatable(worn ? "cfhgui_profile.worn" : "cfhgui_profile.select"), () -> {
				ProfileClient.wear(worn ? null : look);
				this.fillStage();
				this.rebuildWidgets();
			}).variant(worn ? Variant.SECONDARY : Variant.PRIMARY).icon(worn ? Icon.CHECK : null);
			select.active = this.own();
			this.add(select);
			int moreX = px + pw - 20;
			this.add(
				new CFHGuiIconButton(moreX, 38 + bodyH - 20, 20, Icon.DOTS, Component.translatable("cfhgui_profile.more"), () -> this.lookMenu(look, moreX, 38 + bodyH - 20))
			);
			CFHGuiScrollPanel panel = new CFHGuiScrollPanel(this.width - 12 - slotsW, 38, slotsW, bodyH);
			int inner = panel.innerWidth();
			int y = 0;

			for (Slot s : Slot.values()) {
				List<String> ids = new ArrayList<>();
				if (s != Slot.SKIN) {
					ids.add("");

					for (Cosmetic c : Cosmetics.of(s)) {
						ids.add(c.id());
					}
				} else {
					ids.add("account");

					for (String d : ProfileStore.Look.DEFAULTS) {
						ids.add("default:" + d);
					}

					if ("file".equals(look.skin())) {
						ids.add("file");
					}
				}

				String now = s == Slot.SKIN ? look.skin() : (look.in(s) == null ? "" : look.in(s).id());
				int labelW = Math.min(64, inner / 3);
				panel.add(new CFHGuiLabel(0, 0, labelW, slotName(s)).style(Style.MUTED), 0, y + 6);
				int bring = s == Slot.SKIN ? 24 : 0;
				panel.add(new CFHGuiDropdown<>(0, 0, inner - labelW - 4 - bring, ids, now, id -> thingName(s, id), id -> {
					if (s == Slot.SKIN) {
						if (!"file".equals(id)) {
							look.skin(id);
						}
					} else {
						look.put(s, Cosmetics.get(id));
					}

					this.touched(look);
				}), labelW + 4, y);
				if (s == Slot.SKIN) {
					panel.add(new CFHGuiIconButton(0, 0, 20, Icon.UPLOAD, Component.translatable("cfhgui_profile.skin.upload"), () -> this.bringSkin(look)), inner - 20, y);
				}

				y += 23;
			}

			panel.setContentHeight(y);
			this.add(panel);
		}

		this.profile(new Rect(12, this.height - 12 - profileH, this.width - 24, profileH));
		this.finishStage();
	}

	private static Component thingName(Slot s, String id) {
		if (s == Slot.SKIN) {
			return id.startsWith("default:")
				? Component.literal(Character.toUpperCase(id.charAt(8)) + id.substring(9))
				: Component.translatable("file".equals(id) ? "cfhgui_profile.skin.yours" : "cfhgui_profile.skin.account");
		} else {
			Cosmetic c = Cosmetics.get(id);
			return (Component)(c == null ? Component.translatable("cfhgui_profile.slot." + s.key() + ".none") : c.name());
		}
	}

	private void profile(Rect r) {
		this.cardRect = r;
		this.addRenderableOnly((g, mouseX, mouseY, partialTick) -> CFHGuiDraw.floatingPanel(g, r.x(), r.y(), r.w(), r.h(), 1.0F));
		int pad = r.h() >= 60 ? 7 : 5;
		int box = r.h() - pad * 2;
		this.add(new ProfileScreen.PictureBox(r.x() + pad, r.y() + pad, box));
		int x = r.x() + pad + box + 8;
		int w = r.right() - pad - x;
		List<ProfileStore.KnownAccount> known = ProfileStore.known();
		ProfileStore.KnownAccount current = null;

		for (ProfileStore.KnownAccount k : known) {
			if (k.uuid.equalsIgnoreCase(this.account.uuid().toString())) {
				current = k;
			}
		}

		int labelW = Math.min(this.font.width(Component.translatable("cfhgui_profile.account")) + 6, w / 2);
		CFHGuiDropdown<ProfileStore.KnownAccount> accounts = new CFHGuiDropdown<>(
			x + labelW, r.y() + pad, Math.min(170, w - labelW), known, current, kx -> Component.literal(kx.name.isEmpty() ? kx.uuid : kx.name), kx -> {
				try {
					this.account = ProfileStore.account(UUID.fromString(kx.uuid), kx.name);
				} catch (IllegalArgumentException var3x) {
					return;
				}

				ProfileStore.Look worn = this.account.worn();
				this.viewedId = worn != null ? worn.id() : (this.account.looks().isEmpty() ? null : this.account.looks().get(0).id());
				this.viewingBlank = false;
				this.editing = false;
				this.slot = null;
				this.picture = null;
				this.pictureOf = null;
				if (this.looks != null) {
					this.looks.edit(false);
				}

				this.fillStage();
				this.rebuildWidgets();
			}
		);
		accounts.tip(Component.translatable("cfhgui_profile.account.tip"));
		this.add(accounts);
		int bioY = r.y() + pad + 23;
		int bioH = r.bottom() - pad - bioY;
		if (bioH >= 18) {
			MultiLineEditBox box2 = new ProfileScreen.BioBox(x, bioY, w, bioH);
			box2.setCharacterLimit(240);
			box2.setValue(this.account.bio());
			box2.setValueListener(text -> this.bioDirty = true);
			this.bio = (MultiLineEditBox)this.add(box2);
		}
	}

	public void tick() {
		super.tick();
		if (this.bioDirty && ++this.bioTicks >= 20) {
			this.saveBio();
		}
	}

	private void saveBio() {
		this.bioDirty = false;
		this.bioTicks = 0;
		if (this.bio != null) {
			this.account.bio(this.bio.getValue());
		}
	}

	public void back() {
		if (this.overhaul && this.editing) {
			this.edit(false);
		} else {
			super.back();
		}
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean typing = this.getFocused() instanceof EditBox || this.bio != null && this.bio.isFocused() || this.getFocused() instanceof ContainerEventHandler;
		if (super.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (typing || !this.overhaul || this.editing) {
			return false;
		} else if (keyCode == 263) {
			this.step(-1);
			return true;
		} else if (keyCode == 262) {
			this.step(1);
			return true;
		} else {
			return false;
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else if (this.overhaul && this.editing && button == 0 && this.stageRect.contains(mouseX, mouseY)) {
			this.turning = true;
			return true;
		} else {
			return false;
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.turning && this.looks != null) {
			this.looks.turnBy((float)dragX * 0.9F);
			turnedOnce = true;
			return true;
		} else {
			return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.turning = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		} else if (this.overhaul && !this.editing && scrollY != 0.0 && this.stageRect.contains(mouseX, mouseY)) {
			this.step(scrollY > 0.0 ? -1 : 1);
			return true;
		} else {
			return false;
		}
	}

	public void removed() {
		if (this.bioDirty) {
			this.saveBio();
		}

		super.removed();
		Popups.closeAll();
		if (this.shelf != null) {
			this.shelf.close();
			this.shelf = null;
		}

		if (this.looks != null) {
			this.looks.close();
			this.looks = null;
		}
	}

	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		Theme t = Theme.current();
		if (this.overhaul && !t.isVanilla() && !MenuBackdrop.worldBehind()) {
			CFHGuiDraw.vgradient(g, 0, 32, this.width, this.height - 32, -16382199, -15198700);
			CFHGuiDraw.vignette(g, 0, 32, this.width, this.height - 32, 0.5F);
		}
	}

	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		if (this.overhaul && !this.editing) {
			ProfileStore.Look look = this.viewed();
			Component name = Fonts.heading(look == null ? Component.translatable("cfhgui_profile.new_look") : Component.literal(look.name()));
			boolean worn = look != null && this.account.worn() != null && this.account.worn().id().equals(look.id());
			Component badge = Component.translatable("cfhgui_profile.worn");
			int badgeW = worn ? this.font.width(badge) + 8 + 6 : 0;
			FormattedCharSequence cut = CFHGuiDraw.truncate(name, Math.round((float)(this.width - 24 - 80 - badgeW) / this.nameScale));
			int nameW = Math.round((float)this.font.width(cut) * this.nameScale);
			int nx = this.width / 2 - (nameW + badgeW) / 2;
			g.pose().pushPose();
			g.pose().translate((float)nx, (float)this.nameY, 0.0F);
			g.pose().scale(this.nameScale, this.nameScale, 1.0F);
			g.drawString(this.font, cut, 0, 0, van ? -1 : p.text(), van);
			g.pose().popPose();
			if (worn) {
				CFHGuiDraw.chip(g, badge, nx + nameW + 6, this.nameY + Math.round((9.0F * this.nameScale - 12.0F) / 2.0F), van ? -1 : p.accent(), 1.0F);
			}

			if (this.captionY >= 0) {
				FormattedCharSequence made = CFHGuiDraw.truncate(this.madeOf(look), this.width - 24 - 80);
				g.drawString(this.font, made, this.width / 2 - this.font.width(made) / 2, this.captionY, van ? -4144960 : p.textMuted(), van);
			}
		}

		if (this.overhaul && this.editing && this.groupsY >= 0) {
			CFHGuiDraw.sectionRule(g, Component.translatable("cfhgui_profile.group.body"), 12, this.groupsY, this.groupsW, 1.0F);
			CFHGuiDraw.sectionRule(g, Component.translatable("cfhgui_profile.group.wear"), this.width - 12 - this.groupsW, this.groupsY, this.groupsW, 1.0F);
		}

		if (this.overhaul && this.editing && !van && !MenuBackdrop.worldBehind() && this.stageRect.h() >= 60) {
			int fy = this.stageRect.bottom() - 12;
			int dark = Colors.lerp(-16382199, -15198700, Mth.clamp((float)(this.stageRect.bottom() - 32) / (float)Math.max(1, this.height - 32), 0.0F, 1.0F));
			CFHGuiDraw.vgradient(g, this.stageRect.x(), fy, this.stageRect.w(), 12, Colors.withAlpha(dark, 0), dark);
		}

		if (this.overhaul && this.editing && !turnedOnce && this.stageRect.h() >= 90 && this.stageRect.w() >= 260) {
			Component hint = Component.translatable("cfhgui_profile.turn.hint");
			Icons.draw(g, Icon.REFRESH, this.stageRect.x() + 5, this.stageRect.y() + 6, 10, van ? -4144960 : p.textDim());
			g.drawString(this.font, hint, this.stageRect.x() + 18, this.stageRect.y() + 7, van ? -4144960 : p.textDim(), van);
		}

		if (!this.overhaul && this.account.look(this.viewedId) == null) {
			CFHGuiDraw.textCentered(
				g, Component.translatable("cfhgui_profile.no_looks"), this.stageRect.centerX(), this.stageRect.centerY() - 4, van ? -4144960 : p.textMuted()
			);
		}

		if (!this.editing || !this.overhaul) {
			for (GuiEventListener child : this.children()) {
				if (child instanceof ProfileScreen.PictureBox) {
					ProfileScreen.PictureBox box = (ProfileScreen.PictureBox)child;
					g.drawString(
						this.font, Component.translatable("cfhgui_profile.account"), box.getX() + box.getWidth() + 8, box.getY() + 6, van ? -4144960 : p.textMuted(), van
					);
					if (this.bio != null && this.bio.isFocused()) {
						int used = this.bio.getValue().length();
						CFHGuiDraw.textRight(
							g,
							Component.literal(used + " / 240"),
							this.cardRect.right() - 8,
							box.getY() + 6,
							used >= 230 ? (van ? -22016 : p.warning()) : (van ? -4144960 : p.textDim()),
							van
						);
					}

					if (!this.own()) {
						Component note = Component.translatable("cfhgui_profile.not_signed_in");
						int nxx = box.getX() + box.getWidth() + 8 + this.font.width(Component.translatable("cfhgui_profile.account")) + 6 + 176;
						if (nxx + 40 < this.width - 12) {
							g.drawString(this.font, CFHGuiDraw.truncate(note, this.width - 12 - nxx), nxx, box.getY() + 6, van ? -22016 : p.warning(), van);
						}
					}
				}
			}
		}

		if (this.overhaul && this.editing && this.slot == null && this.shelf != null) {
			CFHGuiDraw.textCentered(
				g, Component.translatable("cfhgui_profile.choose_slot"), this.width / 2, this.shelf.getY() + this.shelf.getHeight() / 2 - 8, van ? -4144960 : p.textMuted()
			);
		}
	}

	private final class BioBox extends MultiLineEditBox {
		BioBox(final int x, final int y, final int w, final int h) {
			super(ProfileScreen.this.font, x, y, w, h, Component.translatable("cfhgui_profile.bio.hint"), Component.translatable("cfhgui_profile.bio"));
		}

		protected void renderBackground(GuiGraphics g) {
			Theme t = Theme.current();
			if (t.isVanilla()) {
				super.renderBackground(g);
			} else {
				Palette p = t.palette();
				CFHGuiDraw.pixelRound(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), p.bg2(), t.radius());
				CFHGuiDraw.outline(
					g,
					this.getX(),
					this.getY(),
					this.getWidth(),
					this.getHeight(),
					this.isFocused() ? p.accent() : (this.isHovered() ? p.borderStrong() : p.border()),
					t.radius()
				);
			}
		}

		protected void renderDecorations(GuiGraphics g) {
			g.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth() + 9, this.getY() + this.getHeight());
			super.renderDecorations(g);
			g.disableScissor();
		}
	}

	private final class LookDots extends CFHGuiWidget {
		private static final int STEP = 9;
		private final int count;

		LookDots(final int centreX, final int y, final int count) {
			super(centreX - count * 9 / 2, y, count * 9, 9, Component.translatable("cfhgui_profile.places"));
			this.count = count;
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			ProfileScreen.this.view(Mth.clamp((int)((mouseX - (double)this.getX()) / 9.0), 0, this.count - 1));
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, mouseX, false);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, mouseX, true);
		}

		private void draw(GuiGraphics g, int mouseX, boolean van) {
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F) && ProfileScreen.this.looks != null) {
				Palette p = Theme.current().palette();
				int lit = van ? -1 : p.accent();
				int dim = van ? -7303024 : p.textDim();
				int over = van ? -2039584 : p.textMuted();
				List<LookStage.Stand> stands = ProfileScreen.this.looks.stands();
				ProfileStore.Look worn = ProfileScreen.this.account.worn();
				int y = this.getY() + this.enterOffset() + 4;

				for (int k = 0; k < this.count && k < stands.size(); k++) {
					int cx = this.getX() + k * 9 + 4;
					boolean here = k == ProfileScreen.this.looks.viewed();
					boolean pointed = this.isHovered() && mouseX >= this.getX() + k * 9 && mouseX < this.getX() + (k + 1) * 9;
					int ink = Colors.scaleAlpha(here ? lit : (pointed ? over : dim), a);
					ProfileStore.Look of = stands.get(k).look;
					if (of == null) {
						CFHGuiDraw.rect(g, cx - 2, y, 5, 1, ink);
						CFHGuiDraw.rect(g, cx, y - 2, 1, 5, ink);
					} else if (here) {
						CFHGuiDraw.pixelCircle(g, cx, y, 2, ink);
					} else if (worn != null && worn.id().equals(of.id())) {
						CFHGuiDraw.pixelRing(g, cx, y, 2, Colors.scaleAlpha(lit, a));
					} else {
						CFHGuiDraw.rect(g, cx - 1, y - 1, 3, 3, ink);
					}
				}
			}
		}
	}

	private final class PictureBox extends CFHGuiWidget {
		PictureBox(final int x, final int y, final int size) {
			super(x, y, size, size, Component.translatable("cfhgui_profile.picture"));
			this.tip(Component.translatable("cfhgui_profile.picture.tip"));
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			List<Item> items = new ArrayList<>();
			items.add(Item.of(Component.translatable("cfhgui_profile.picture.choose"), Icon.IMAGE, () -> {
				if (FilePicker.available()) {
					FilePicker.pickFile(Component.translatable("cfhgui_profile.picture.choose").getString(), List.of("*.png", "*.jpg", "*.jpeg"), "Pictures", path -> {
						ProfileScreen.this.account.picture(path);
						ProfileScreen.this.picture = null;
						ProfileScreen.this.pictureOf = null;
					});
				}
			}));
			items.add(Item.of(Component.translatable("cfhgui_profile.picture.face"), Icon.USER, () -> {
				ProfileScreen.this.account.picture(null);
				ProfileScreen.this.picture = null;
				ProfileScreen.this.pictureOf = null;
			}));
			CFHGuiContextMenu.open((double)this.getX(), (double)(this.getY() + this.getHeight() + 2), items);
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, false);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, true);
		}

		private void draw(GuiGraphics g, boolean van) {
			Theme t = Theme.current();
			Palette p = t.palette();
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int s = this.getWidth();
				CFHGuiDraw.shadow(g, x, y, s, s, 0.4F * a);
				CFHGuiDraw.pixelRound(g, x, y, s, s, Colors.scaleAlpha(van ? -15724528 : p.bg2(), a), van ? 0 : t.radius());
				Path chosen = ProfileScreen.this.account.picture();
				if (chosen != null) {
					if (!chosen.equals(ProfileScreen.this.pictureOf)) {
						ProfileScreen.this.pictureOf = chosen;
						ProfileScreen.this.picture = null;
						Textures.invalidate(chosen);
						Textures.load(chosen, loaded -> ProfileScreen.this.picture = loaded);
					}

					Loaded pic = ProfileScreen.this.picture;
					if (pic != null) {
						g.enableScissor(x + 1, y + 1, x + s - 1, y + s - 1);
						CFHGuiDraw.blitFit(g, pic.id(), x + 1, y + 1, s - 2, s - 2, pic.width(), pic.height(), true, a);
						g.disableScissor();
					}
				} else {
					AppliedLooks.Applied worn = ProfileScreen.this.own() ? AppliedLooks.of(ProfileScreen.this.account.uuid()) : null;
					ResourceLocation skin = worn != null
						? worn.texture()
						: SkinComposer.accountSkin(ProfileScreen.this.account.uuid(), ProfileScreen.this.account.name()).texture();
					CFHGuiDraw.playerHead(g, skin, x + 3, y + 3, s - 6, a);
				}

				if (this.hover() > 0.01F) {
					CFHGuiDraw.rect(g, x + 1, y + s - 13, s - 2, 12, Colors.scaleAlpha(-1073741824, a * this.hover()));
					Icons.draw(g, Icon.EDIT, x + s / 2 - 5, y + s - 12, 10, Colors.scaleAlpha(-1, a * this.hover()));
				}

				CFHGuiDraw.outline(g, x, y, s, s, Colors.scaleAlpha(van ? -16777216 : Colors.lerp(p.border(), p.accent(), this.hover()), a), van ? 0 : t.radius());
				CFHGuiDraw.focusRing(g, x, y, s, s, this.focus() * a);
			}
		}
	}

	private final class SlotButton extends CFHGuiWidget {
		private final Slot of;
		private final ProfileStore.Look look;

		SlotButton(final int x, final int y, final int w, final int h, final Slot of, final ProfileStore.Look look) {
			super(x, y, w, h, ProfileScreen.slotName(of));
			this.of = of;
			this.look = look;
		}

		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX, mouseY);
			ProfileScreen.this.choose(this.of);
		}

		private Component holds() {
			if (this.of == Slot.SKIN) {
				String s = this.look.skin();
				return s.startsWith("default:")
					? Component.literal(Character.toUpperCase(s.charAt(8)) + s.substring(9))
					: Component.translatable("file".equals(s) ? "cfhgui_profile.skin.yours" : "cfhgui_profile.skin.account");
			} else {
				Cosmetic c = this.look.in(this.of);
				return (Component)(c == null ? Component.translatable("cfhgui_profile.empty") : c.name());
			}
		}

		protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, false);
		}

		protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			this.draw(g, true);
		}

		private void draw(GuiGraphics g, boolean van) {
			Theme t = Theme.current();
			Palette p = t.palette();
			float a = this.effectiveAlpha();
			if (!(a <= 0.004F)) {
				int x = this.getX();
				int y = this.getY() + this.enterOffset();
				int w = this.getWidth();
				int h = this.getHeight();
				boolean sel = this.of == ProfileScreen.this.slot;
				float hv = this.hover();
				boolean filled = this.of == Slot.SKIN || this.look.in(this.of) != null;
				if (van) {
					g.fill(x, y, x + w, y + h, Colors.scaleAlpha(sel ? -1862270977 : Colors.lerp(1610612736, -2144325584, hv), a));
					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(sel ? -1 : -16777216, a), 0);
				} else {
					CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(Colors.lerp(p.surface(), p.surfaceHover(), hv)), a), t.radius());
					if (sel) {
						CFHGuiDraw.hgradient(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 88), a), Colors.withAlpha(p.accent(), 0));
					}

					CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(sel ? p.accent() : Colors.lerp(p.border(), p.borderStrong(), hv), a), t.radius());
					if (sel) {
						CFHGuiDraw.rect(g, x + 1, y + 3, 2, h - 6, Colors.scaleAlpha(p.accent(), a));
					}
				}

				Font font = CFHGuiDraw.font();
				int ink = van ? (sel ? -14671840 : -1) : p.text();
				Component name = this.getMessage();
				Component holds = this.holds();
				if (h >= 26) {
					int lines = y + (h - 19) / 2;
					g.drawString(font, CFHGuiDraw.truncate(name, w - 20), x + 8, lines, Colors.scaleAlpha(ink, a), van && !sel);
					int second = van ? (sel ? -12566464 : -5197648) : (filled ? Colors.lerp(p.textMuted(), p.accent(), sel ? 0.6F : 0.0F) : p.textDim());
					g.drawString(font, CFHGuiDraw.truncate(holds, w - 20), x + 8, lines + 11, Colors.scaleAlpha(second, a), false);
					if (filled && this.of != Slot.SKIN) {
						CFHGuiDraw.rect(g, x + w - 9, y + h / 2 - 1, 3, 3, Colors.scaleAlpha(van ? -1 : p.accent(), a));
					}

					CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
				} else {
					int nameW = font.width(name);
					int ty = y + (h - 8) / 2;
					g.drawString(font, CFHGuiDraw.truncate(name, w - 12), x + 7, ty, Colors.scaleAlpha(ink, a), van && !sel);
					int room = w - 7 - nameW - 12;
					if (room > 26) {
						FormattedCharSequence cut = CFHGuiDraw.truncate(holds, room);
						int dim = van ? (sel ? -12566464 : -5197648) : (filled ? p.textMuted() : p.textDim());
						g.drawString(
							font, cut, x + w - 6 - font.width(cut), ty, Colors.scaleAlpha(filled && !van ? Colors.lerp(dim, p.accentInk(), sel ? 0.5F : 0.0F) : dim, a), false
						);
					} else if (filled && this.of != Slot.SKIN) {
						CFHGuiDraw.rect(g, x + w - 8, y + h / 2 - 1, 3, 3, Colors.scaleAlpha(van ? -1 : p.accent(), a));
					}

					CFHGuiDraw.focusRing(g, x, y, w, h, this.focus() * a);
				}
			}
		}
	}
}
