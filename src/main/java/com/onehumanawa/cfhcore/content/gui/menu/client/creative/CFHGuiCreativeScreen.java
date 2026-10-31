package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.ItemBatch;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ContainerReskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiContextMenu;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.MenuPopup.Item;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.CreativeScreenAccessor;
import com.onehumanawa.cfhcore.content.gui.menu.mixin.SlotPositionAccessor;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.Type;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiCreativeScreen extends CreativeModeInventoryScreen {
   private static final int CELL = 18;
   private static final int MAX_COLS = 24;
   private static final int MAX_ROWS = 12;
   private static final int SIDE_X = 6;
   private static final int SIDE_MIN = 96;
   private static final int SIDE_MAX = 120;
   private static final int ROW_H = 20;
   private static final int CAPTION_H = 14;
   private static final int DRAG_SLOP = 4;
   private static final int DRAG_EDGE = 12;
   private static final int DRAG_TURN = 160;
   private static final int CHROME_W = 31;
   private static final int TOP = 7;
   private static final int HEAD_H = 16;
   private static final int GRID_Y = 29;
   private static final int BAND_H = 33;
   private static final int BAND_ROW = 15;
   private static final int BOTTOM = 7;
   private static final int CHROME_H = 69;
   private static final int PARK = -4000;
   private static @Nullable CreativeModeTab lastTab;
   private static final List<ItemStack> RECENT = new ArrayList();
   private static WeakReference<Object> recentWorld = new WeakReference(null);
   private static @Nullable Method pageOf;
   private static @Nullable Method turnTo;
   private static boolean pagesLooked;
   private static @Nullable Method sortedTabs;
   private static boolean sortedLooked;
   private int sideW;
   private int cols;
   private int rows;
   private int gridX;
   private int gridW;
   private int gridBottom;
   private int barX;
   private int fieldX;
   private int fieldW;
   private int hotbarX;
   private int hotbarY;
   private int midX;
   private int recentX;
   private int recentN;
   private int mainY;
   private int figX;
   private int figY;
   private int figW;
   private int figH;
   private int offX;
   private int offY;
   private int binX;
   private int binY;
   private final int[] armX = new int[4];
   private final int[] armY = new int[4];
   private @Nullable EditBox box;
   private boolean opened;
   private boolean armed;
   private boolean swallowChar;
   private boolean pressed;
   private @Nullable CreativeModeTab pressTab;
   private double pressY;
   private int pressInRow;
   private @Nullable CreativeModeTab dragTab;
   private List<CreativeModeTab> dragFrom = List.of();
   private @Nullable CreativeModeTab dragHome;
   private @Nullable CreativeModeTab dragBefore;
   private @Nullable List<String> dragOrder;
   private long dragTurnMs;
   private boolean menuPress;
   private @Nullable CreativeModeTab offered;
   private boolean taken;
   private final List<Entry> entries = new ArrayList();
   private List<CreativeModeTab> listed = List.of();
   private int sideH;
   private float sideTarget;
   private final Anim side;
   private final Anim mark;
   private boolean marked;
   private @Nullable CreativeModeTab tip;
   private int scrollRow;
   private @Nullable TabFlow flow;
   private double wheel;
   private boolean grabbed;
   private @Nullable CreativeModeTab shownTab;
   private String shownQuery;
   private boolean rendering;
   private boolean widgetPass;
   private boolean aimed;
   private final ItemBatch batch;
   private int aimX;
   private int aimY;

   public CFHGuiCreativeScreen(LocalPlayer player, FeatureFlagSet features, boolean operatorTab) {
      super(player, features, operatorTab);
      this.side = new Anim(0.0F, 220, Ease.OUT_CUBIC);
      this.mark = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.shownQuery = "";
      this.batch = new ItemBatch();
   }

   public static @Nullable Screen replacing(@Nullable Screen vanilla) {
      if (vanilla instanceof CreativeModeInventoryScreen && !(vanilla instanceof CFHGuiCreativeScreen)) {
         Minecraft mc = Minecraft.getInstance();
         return mc.player != null && mc.gameMode != null && mc.gameMode.hasInfiniteItems() ? new CFHGuiCreativeScreen(mc.player, mc.player.connection.enabledFeatures(), (Boolean)mc.options.operatorItemsTab().get()) : null;
      } else {
         return null;
      }
   }

   private CreativeScreenAccessor access() {
      return (CreativeScreenAccessor)(Object)this;
   }

   public boolean laidOut() {
      return this.box != null;
   }

   public int swapX() {
      return this.leftPos + this.gridX + this.font.width(Fonts.heading(name(current()))) + 8;
   }

   public int swapY() {
      return this.topPos + 7 - 1;
   }

   public int getGuiLeft() {
      return this.widgetPass && inventoryTab() ? this.leftPos + (this.figH > 0 ? this.figX - 73 : this.midX + 1 - 9) : this.leftPos;
   }

   public int getGuiTop() {
      return this.widgetPass && inventoryTab() ? this.topPos + (this.figH > 0 ? this.figY - 6 : this.mainY + 1 - 54) : this.topPos;
   }

   private static CreativeModeTab current() {
      return CreativeScreenAccessor.cfhgui_menu$selectedTab();
   }

   private static boolean inventoryTab() {
      return current().getType() == Type.INVENTORY;
   }

   private static boolean searching() {
      return current().getType() == Type.SEARCH;
   }

   protected void init() {
      if (this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.gameMode.hasInfiniteItems()) {
         this.measure();
         super.init();
         this.box = this.access().cfhgui_menu$searchBox();
         if (this.box != null) {
            for(GuiEventListener child : List.copyOf(this.children())) {
               if (child != this.box && pageButton(child)) {
                  this.removeWidget(child);
               }
            }

            if (!this.opened) {
               this.opened = true;
               Object world = this.minecraft.getConnection();
               if (recentWorld.get() != world) {
                  RECENT.clear();
                  recentWorld = new WeakReference(world);
               }

               if (searching()) {
                  this.select(fallback());
               }
            }

            this.listed = List.of();
            this.listTabs();
            if (this.dragTab == null) {
               this.reveal();
            }

            this.placeSlots();
            this.placeBox();
         }
      } else {
         super.init();
      }
   }

   private static boolean pageButton(GuiEventListener child) {
      if (child.getClass().getName().startsWith("net.fabricmc.fabric.")) {
         return true;
      } else if (!(child instanceof Button)) {
         return false;
      } else {
         Button button = (Button)child;
         String label = button.getMessage().getString();
         return label.equals("<") || label.equals(">");
      }
   }

   private void measure() {
      int roomW = this.width * 7 / 10;
      int roomH = this.height * 7 / 10;
      this.sideW = Mth.clamp(roomW / 4, 96, 120);
      this.cols = Mth.clamp((roomW - this.sideW - 31) / 18, 9, 24);
      this.rows = Mth.clamp((roomH - 69) / 18, 4, 12);
      this.gridX = 6 + this.sideW + 11;
      this.gridW = this.cols * 18;
      this.gridBottom = 29 + this.rows * 18;
      this.barX = this.gridX + this.gridW + 2;
      this.hotbarY = this.gridBottom + 15;
      this.midX = this.gridX + (this.cols - 9) / 2 * 18;
      this.recentN = this.cols - 10 >= 4 ? this.cols - 10 : 0;
      this.recentX = this.gridX + 180;
      this.hotbarX = this.recentN > 0 ? this.gridX : this.midX;
      this.fieldW = Mth.clamp(this.gridW * 2 / 5, 72, 150);
      this.fieldX = this.barX + 4 - this.fieldW;
      this.imageWidth = this.barX + 4 + 8;
      this.imageHeight = this.hotbarY + 18 + 7;
      int room = this.hotbarY - 15 - 29 - 6 - 54;
      this.figH = room < 36 ? 0 : Math.min(room, 96);
      int bandH = this.figH == 0 ? 18 : this.figH;
      int bandY = 29 + (room - bandH) / 2;
      int bandBottom = bandY + bandH;
      this.mainY = bandBottom + 6;
      this.binX = this.midX + 144;
      this.binY = this.offY = bandBottom - 18;
      if (this.figH == 0) {
         for(int i = 0; i < 4; ++i) {
            this.armX[i] = this.midX + i * 18;
            this.armY[i] = bandY;
         }

         this.offX = this.midX + 72 + 9;
      } else if (this.figH >= 72) {
         this.figW = Mth.clamp(this.figH * 3 / 4, 30, 96);
         this.figX = this.midX + 18 + 4;
         this.figY = bandY;

         for(int i = 0; i < 4; ++i) {
            this.armX[i] = this.midX;
            this.armY[i] = bandY + (bandH - 72) / 2 + i * 18;
         }

         this.offX = this.figX + this.figW + 4;
      } else {
         this.figW = Mth.clamp(this.figH * 3 / 4, 30, 64);
         this.figX = this.midX + 18 + 4;
         this.figY = bandY;
         int pairY = bandY + (bandH - 36) / 2;
         int rightX = this.figX + this.figW + 4;
         this.armX[0] = this.armX[1] = this.midX;
         this.armX[2] = this.armX[3] = rightX;
         this.armY[0] = this.armY[2] = pairY;
         this.armY[1] = this.armY[3] = pairY + 18;
         this.offX = rightX + 18 + 8;
      }

   }

   private void listTabs() {
      List<CreativeModeTab> shown = CreativeModeTabs.tabs();
      if (!shown.equals(this.listed)) {
         this.listed = shown;
         this.entries.clear();
         Map<String, List<CreativeModeTab>> groups = new LinkedHashMap();
         CreativeModeTab inventory = null;
         CreativeModeTab hotbars = null;

         for(CreativeModeTab tab : ordered(shown)) {
            switch (tab.getType()) {
               case INVENTORY:
                  inventory = tab;
                  break;
               case HOTBAR:
                  hotbars = tab;
               case SEARCH:
                  break;
               default:
                  ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                  ((List)groups.computeIfAbsent(id == null ? "" : id.getNamespace(), (k) -> new ArrayList())).add(tab);
            }
         }

         if (inventory != null) {
            this.entries.add(new Entry(inventory, (Component)null));
         }

         if (hotbars != null) {
            this.entries.add(new Entry(hotbars, (Component)null));
         }

         List<CreativeModeTab> tabs = new ArrayList();
         List<CreativeModeTab> single = new ArrayList();
         Map<CreativeModeTab, Component> captions = new IdentityHashMap();
         boolean captioned = groups.size() > 1;
         boolean named = false;

         for(Map.Entry<String, List<CreativeModeTab>> group : groups.entrySet()) {
            boolean game = ((String)group.getKey()).equals("minecraft");
            if (!game && ((List<CreativeModeTab>)group.getValue()).size() < 2) {
               single.addAll((Collection<CreativeModeTab>)group.getValue());
            } else {
               tabs.addAll((Collection<CreativeModeTab>)group.getValue());
               if (captioned) {
                  Component caption = modName((String)group.getKey());

                  for(CreativeModeTab tab : (List<CreativeModeTab>)group.getValue()) {
                     captions.put(tab, caption);
                  }
               }

               named |= !game;
            }
         }

         if (!single.isEmpty()) {
            tabs.addAll(single);
            if (captioned) {
               Component caption = Component.translatable(named ? "cfhgui.creative.other_mods" : "cfhgui.creative.mods");

               for(CreativeModeTab tab : single) {
                  captions.put(tab, caption);
               }
            }
         }

         Component over = null;

         for(CreativeModeTab tab : TabOrder.apply(tabs, this.dragOrder != null ? this.dragOrder : TabOrder.stored())) {
            Component caption = (Component)captions.get(tab);
            if (caption != null && caption != over) {
               this.entries.add(new Entry((CreativeModeTab)null, caption));
            }

            this.entries.add(new Entry(tab, (Component)null));
            over = caption;
         }

         this.sideH = 0;

         for(Entry e : this.entries) {
            this.sideH += e.height();
         }

      }
   }

   private void relist() {
      this.listed = List.of();
      this.listTabs();
   }

   private static List<CreativeModeTab> ordered(List<CreativeModeTab> shown) {
      if (!sortedLooked) {
         sortedLooked = true;

         try {
            sortedTabs = Class.forName("net.neoforged.neoforge.common.CreativeModeTabRegistry").getMethod("getSortedCreativeModeTabs");
         } catch (RuntimeException | LinkageError | ReflectiveOperationException var5) {
            sortedTabs = null;
         }
      }

      if (sortedTabs == null) {
         return shown;
      } else {
         try {
            List<CreativeModeTab> out = new ArrayList(shown.size());

            for(CreativeModeTab tab : (List<CreativeModeTab>)sortedTabs.invoke(null)) {
               if (shown.contains(tab) && !out.contains(tab)) {
                  out.add(tab);
               }
            }

            for(CreativeModeTab tab : shown) {
               if (!out.contains(tab)) {
                  out.add(tab);
               }
            }

            return out;
         } catch (RuntimeException | ReflectiveOperationException var4) {
            return shown;
         }
      }
   }

   private static Component modName(String namespace) {
      String name = (String)CFHGuiPlatform.get().modInfo(namespace).map(ModInfo::name).orElse("");
      return Component.literal(name.isBlank() ? namespace : name);
   }

   private void reveal() {
      int viewH = this.imageHeight - 7 - 7;
      CreativeModeTab tab = current();
      int y = 0;

      for(Entry e : this.entries) {
         if (e.tab() == tab) {
            if ((float)y < this.sideTarget) {
               this.sideTarget = (float)y;
            } else if ((float)(y + 20) > this.sideTarget + (float)viewH) {
               this.sideTarget = (float)(y + 20 - viewH);
            }
            break;
         }

         y += e.height();
      }

      this.sideTarget = Mth.clamp(this.sideTarget, 0.0F, (float)Math.max(0, this.sideH - viewH));
      this.side.snap(this.sideTarget);
   }

   public void containerTick() {
      super.containerTick();
      if (this.box != null) {
         this.listTabs();
      }

   }

   private static CreativeModeTab fallback() {
      CreativeModeTab tab = lastTab;
      return tab != null && tab.getType() != Type.SEARCH && tab.shouldDisplay() ? tab : CreativeModeTabs.getDefaultTab();
   }

   private void select(CreativeModeTab tab) {
      this.turnPage(tab);
      this.access().cfhgui_menu$selectTab(tab);
      if (this.box != null) {
         this.sync(this.pointerX(), this.pointerY());
      }

   }

   private void turnPage(CreativeModeTab tab) {
      if (!pagesLooked) {
         pagesLooked = true;

         try {
            pageOf = CreativeModeInventoryScreen.class.getMethod("getPage", CreativeModeTab.class);
            turnTo = CreativeModeInventoryScreen.class.getMethod("switchToPage", Integer.TYPE);
         } catch (RuntimeException | ReflectiveOperationException var4) {
            turnTo = null;
            pageOf = null;
         }
      }

      if (pageOf != null && turnTo != null) {
         try {
            turnTo.invoke(this, pageOf.invoke(this, tab));
         } catch (RuntimeException | ReflectiveOperationException var3) {
         }

      }
   }

   private double pointerX() {
      Minecraft mc = Minecraft.getInstance();
      return mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth());
   }

   private double pointerY() {
      Minecraft mc = Minecraft.getInstance();
      return mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight());
   }

   private void sync(double mouseX, double mouseY) {
      if (this.box != null) {
         CreativeModeTab tab = current();
         String query = this.box.getValue();
         if (tab != this.shownTab || !query.equals(this.shownQuery)) {
            this.shownTab = tab;
            this.shownQuery = query;
            this.scrollRow = 0;
            this.wheel = (double)0.0F;
            this.grabbed = false;
         }

         if (tab.getType() != Type.SEARCH) {
            lastTab = tab;
         }

         this.flow = tab.getType() == Type.CATEGORY ? TabFlow.of(((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).items, this.cols, this.flow) : null;
         this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScroll());
         this.placeSlots();
         this.placeBox();
         this.aim(mouseX, mouseY);
      }
   }

   private static void move(Slot slot, int cellX, int cellY) {
      if (slot.x != cellX + 1 || slot.y != cellY + 1) {
         SlotPositionAccessor at = (SlotPositionAccessor)slot;
         at.cfhgui_menu$setX(cellX + 1);
         at.cfhgui_menu$setY(cellY + 1);
      }
   }

   private void placeSlots() {
      List<Slot> slots = ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).slots;
      if (inventoryTab()) {
         Slot bin = this.access().cfhgui_menu$destroySlot();

         for(int i = 0; i < slots.size(); ++i) {
            Slot slot = (Slot)slots.get(i);
            if (slot == bin) {
               move(slot, this.binX, this.binY);
            } else if (i >= 5 && i < 9) {
               move(slot, this.armX[i - 5], this.armY[i - 5]);
            } else if (i >= 9 && i < 36) {
               move(slot, this.midX + (i - 9) % 9 * 18, this.mainY + (i - 9) / 9 * 18);
            } else if (i >= 36 && i < 45) {
               move(slot, this.midX + (i - 36) * 18, this.hotbarY);
            } else if (i == 45) {
               move(slot, this.offX, this.offY);
            }
         }

      } else {
         Inventory inventory = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getInventory() : null;
         Container picker = this.pickerContainer();

         for(int i = 0; i < slots.size(); ++i) {
            Slot slot = (Slot)slots.get(i);
            int hotbar = inventory != null && slot.container == inventory ? slot.getContainerSlot() : -1;
            if (hotbar >= 0 && hotbar < 9) {
               move(slot, this.hotbarX + hotbar * 18, this.hotbarY);
            } else if (picker != null && slot.container == picker) {
               move(slot, -4000, -4000);
               if (i > 0 && slot.hasItem()) {
                  slot.set(ItemStack.EMPTY);
               }
            }
         }

      }
   }

   private @Nullable Container pickerContainer() {
      if (!inventoryTab() && !((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).slots.isEmpty()) {
         Container first = ((Slot)((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).slots.get(0)).container;
         return first instanceof SimpleContainer ? first : null;
      } else {
         return null;
      }
   }

   private void placeBox() {
      if (this.box != null) {
         this.box.setX(this.leftPos + this.fieldX + 15);
         this.box.setY(this.topPos + 7 + 4);
         this.box.setWidth(this.fieldW - 20);
         this.box.setTextColor(CFHGuiCreativeScreen.Look.current().text());
      }
   }

   private int gridCols() {
      return current().getType() == Type.HOTBAR ? 9 : this.cols;
   }

   private int gridLeft() {
      return current().getType() == Type.HOTBAR ? this.hotbarX : this.gridX;
   }

   private int maxScroll() {
      if (inventoryTab()) {
         return 0;
      } else {
         TabFlow laid = this.flow;
         return Math.max(0, (laid != null ? laid.rows() : Mth.positiveCeilDiv(((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).items.size(), this.gridCols())) - this.rows);
      }
   }

   private @Nullable ItemStack stackAt(double mouseX, double mouseY) {
      int x = Mth.floor(mouseX) - this.leftPos;
      int y = Mth.floor(mouseY) - this.topPos;
      int left = this.gridLeft();
      int across = this.gridCols();
      if (x >= left && x < left + across * 18 && y >= 29 && y < this.gridBottom) {
         int c = (x - left) / 18;
         int r = (y - 29) / 18;
         TabFlow laid = this.flow;
         if (laid != null && laid.heading(this.scrollRow + r) >= 0) {
            return null;
         } else {
            this.aimX = left + c * 18;
            this.aimY = 29 + r * 18;
            int i = laid != null ? laid.entry(this.scrollRow + r, c) : (this.scrollRow + r) * across + c;
            return i >= 0 && i < ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).items.size() ? (ItemStack)((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).items.get(i) : ItemStack.EMPTY;
         }
      } else if (this.recentN > 0 && x >= this.recentX && x < this.recentX + this.recentN * 18 && y >= this.hotbarY && y < this.hotbarY + 18) {
         int c = (x - this.recentX) / 18;
         if (c >= RECENT.size()) {
            return null;
         } else {
            this.aimX = this.recentX + c * 18;
            this.aimY = this.hotbarY;
            return (ItemStack)RECENT.get(c);
         }
      } else {
         return null;
      }
   }

   private void aim(double mouseX, double mouseY) {
      this.aimed = false;
      if (this.pickerContainer() != null) {
         Slot slot = (Slot)((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).slots.get(0);
         ItemStack stack = this.dragTab != null ? null : this.stackAt(mouseX, mouseY);
         if (stack == null) {
            if (slot.hasItem()) {
               slot.set(ItemStack.EMPTY);
            }

         } else {
            if (slot.getItem() != stack) {
               slot.set(stack);
            }

            move(slot, this.aimX, this.aimY);
            this.aimed = true;
         }
      }
   }

   protected boolean checkTabClicked(CreativeModeTab tab, double relativeX, double relativeY) {
      if (this.offered != null && tab == this.offered) {
         this.taken = true;
         return true;
      } else {
         return false;
      }
   }

   protected boolean checkTabHovering(GuiGraphics g, CreativeModeTab tab, int mouseX, int mouseY) {
      return false;
   }

   protected void renderTabButton(GuiGraphics g, CreativeModeTab tab) {
   }

   protected boolean insideScrollbar(double mouseX, double mouseY) {
      return false;
   }

   private boolean inField(int x, int y) {
      return x >= this.fieldX && x < this.fieldX + this.fieldW && y >= 7 && y < 23;
   }

   private void startSearch() {
      this.armed = false;
      this.select(CreativeModeTabs.searchTab());
   }

   private void leaveSearch(boolean keepKeyboard) {
      this.select(fallback());
      this.armed = keepKeyboard;
   }

   private void settle() {
      if (this.box != null && searching() && this.box.getValue().isEmpty()) {
         this.leaveSearch(true);
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.box == null) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         this.sync(mouseX, mouseY);
         int x = Mth.floor(mouseX) - this.leftPos;
         int y = Mth.floor(mouseY) - this.topPos;
         boolean field = this.inField(x, y);
         if (!field) {
            this.armed = false;
         }

         if (button == 0) {
            this.pressed = false;
            this.putBack();
            if (field) {
               if (this.box.isVisible()) {
                  this.box.mouseClicked(mouseX, mouseY, button);
               } else {
                  this.armed = true;
               }

               this.pressed = true;
               return true;
            }

            this.pressTab = this.tabAt(x, y);
            if (this.pressTab != null) {
               this.pressed = true;
               this.pressY = mouseY;
               this.pressInRow = y - this.rowTop(this.pressTab);
               return true;
            }

            if (x >= this.barX - 2 && x < this.barX + 6 && y >= 29 && y < this.gridBottom && this.maxScroll() > 0) {
               this.grabbed = true;
               this.dragTo(y);
               this.pressed = true;
               return true;
            }
         } else if (button == 1 && this.dragTab == null && ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).getCarried().isEmpty() && this.inSidebar(x, y)) {
            this.menuPress = true;
            this.orderMenu(mouseX, mouseY);
            return true;
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.box == null) {
         return super.mouseReleased(mouseX, mouseY, button);
      } else {
         if (button == 0) {
            this.grabbed = false;
            if (this.pressed) {
               this.pressed = false;
               CreativeModeTab tab = this.pressTab;
               CreativeModeTab carried = this.dragTab;
               boolean moved = carried != null && this.drop(carried, mouseX, mouseY);
               this.putBack();
               if (!moved && tab != null && tab != current() && this.tabAt(Mth.floor(mouseX) - this.leftPos, Mth.floor(mouseY) - this.topPos) == tab) {
                  this.click(tab, mouseX, mouseY);
               }

               return true;
            }
         } else if (button == 1 && this.menuPress) {
            this.menuPress = false;
            return true;
         }

         this.sync(mouseX, mouseY);
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   private void click(CreativeModeTab tab, double mouseX, double mouseY) {
      CFHGuiSounds.tick();
      this.turnPage(tab);
      this.offered = tab;
      this.taken = false;

      try {
         super.mouseReleased(mouseX, mouseY, 0);
      } finally {
         this.offered = null;
      }

      if (this.taken) {
         this.sync(mouseX, mouseY);
      } else {
         this.select(tab);
      }

   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.box == null) {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      } else if (this.grabbed) {
         this.dragTo(Mth.floor(mouseY) - this.topPos);
         return true;
      } else if (this.pressed) {
         if (button == 0 && this.dragTab == null && this.pressTab != null && TabOrder.movable(this.pressTab) && Math.abs(mouseY - this.pressY) >= (double)4.0F) {
            this.takeUp(this.pressTab);
         }

         return true;
      } else {
         this.sync(mouseX, mouseY);
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   private void takeUp(CreativeModeTab tab) {
      List<CreativeModeTab> from = new ArrayList();
      CreativeModeTab home = null;
      boolean passed = false;

      for(Entry e : this.entries) {
         CreativeModeTab other = e.tab();
         if (other != null && TabOrder.movable(other)) {
            if (passed && home == null) {
               home = other;
            }

            passed |= other == tab;
            from.add(other);
         }
      }

      this.dragTab = tab;
      this.dragFrom = from;
      this.dragHome = this.dragBefore = home;
      this.dragOrder = null;
      this.dragTurnMs = Util.getMillis();
   }

   private void carry(CreativeModeTab carried, double mouseX, double mouseY) {
      CreativeModeTab before = this.dragHome;
      if (this.atList(mouseX)) {
         int y = Mth.floor(mouseY) - this.topPos - 7 + Math.round(this.side.get());
         before = null;
         int ry = 0;

         for(Entry e : this.entries) {
            int h = e.height();
            CreativeModeTab tab = e.tab();
            if (tab != null && tab != carried && TabOrder.movable(tab) && y < ry + h / 2) {
               before = tab;
               break;
            }

            ry += h;
         }
      }

      if (before != this.dragBefore) {
         this.dragBefore = before;
         this.dragOrder = before == this.dragHome ? null : TabOrder.moved(this.dragFrom, carried, before);
         this.relist();
      }
   }

   private boolean drop(CreativeModeTab carried, double mouseX, double mouseY) {
      this.carry(carried, mouseX, mouseY);
      List<String> order = this.dragOrder;
      if (order == null) {
         return false;
      } else {
         this.dragOrder = null;
         TabOrder.store(order);
         CFHGuiSounds.tick();
         return true;
      }
   }

   private boolean atList(double mouseX) {
      int x = Mth.floor(mouseX) - this.leftPos;
      return x >= -6 && x < 6 + this.sideW + 12;
   }

   private void putBack() {
      this.pressTab = null;
      this.dragTab = null;
      this.dragFrom = List.of();
      if (this.dragOrder != null) {
         this.dragOrder = null;
         this.relist();
      }

   }

   private void orderMenu(double mouseX, double mouseY) {
      List<MenuPopup.Item> items = new ArrayList();
      items.add(Item.disabled(Component.translatable("cfhgui.creative.order.hint"), Icon.MOVE));
      items.add(Item.sep());
      items.add(TabOrder.custom() ? Item.of(Component.translatable("cfhgui.creative.order.reset"), Icon.REFRESH, this::resetOrder) : Item.disabled(Component.translatable("cfhgui.creative.order.reset"), Icon.REFRESH));
      CFHGuiContextMenu.open(mouseX, mouseY, items);
   }

   private void resetOrder() {
      TabOrder.reset();
      this.relist();
      this.reveal();
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.box == null) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      } else {
         int x = Mth.floor(mouseX) - this.leftPos;
         int y = Mth.floor(mouseY) - this.topPos;
         if (x >= 0 && x < this.gridX - 6 && y >= 0 && y < this.imageHeight) {
            this.sideTarget -= (float)scrollY * 20.0F * 2.0F;
            return true;
         } else {
            int max = this.maxScroll();
            if (max <= 0) {
               return false;
            } else {
               this.wheel += scrollY * (double)Math.max(1, this.rows / 3);
               int whole = (int)this.wheel;
               this.wheel -= (double)whole;
               this.scrollRow = Mth.clamp(this.scrollRow - whole, 0, max);
               this.sync(mouseX, mouseY);
               return true;
            }
         }
      }
   }

   private int thumbH(int max) {
      int track = this.rows * 18;
      return Math.max(12, track * this.rows / (this.rows + max));
   }

   private void dragTo(int y) {
      int max = this.maxScroll();
      if (max > 0) {
         int track = this.rows * 18;
         int thumb = this.thumbH(max);
         float t = ((float)(y - 29) - (float)thumb / 2.0F) / (float)Math.max(1, track - thumb);
         this.scrollRow = Mth.clamp(Math.round(t * (float)max), 0, max);
      }
   }

   public boolean keyPressed(int key, int scan, int modifiers) {
      if (this.box == null) {
         return super.keyPressed(key, scan, modifiers);
      } else if (this.dragTab != null && key == 256) {
         this.putBack();
         return true;
      } else {
         this.swallowChar = false;
         this.sync(this.pointerX(), this.pointerY());
         if (this.box.isVisible()) {
            if (key == 256 && searching()) {
               this.leaveSearch(false);
               return true;
            } else {
               boolean handled = super.keyPressed(key, scan, modifiers);
               this.settle();
               return handled;
            }
         } else if (this.armed) {
            if (key == 256) {
               this.armed = false;
               return true;
            } else if (Screen.isPaste(key)) {
               this.startSearch();
               boolean handled = super.keyPressed(key, scan, modifiers);
               this.settle();
               return handled;
            } else {
               return true;
            }
         } else if (this.minecraft != null && this.minecraft.options.keyChat.matches(key, scan)) {
            this.armed = true;
            this.swallowChar = true;
            return true;
         } else {
            return super.keyPressed(key, scan, modifiers);
         }
      }
   }

   public boolean keyReleased(int key, int scan, int modifiers) {
      this.swallowChar = false;
      return super.keyReleased(key, scan, modifiers);
   }

   public boolean charTyped(char character, int modifiers) {
      if (this.box != null && !this.swallowChar) {
         if (!this.box.isVisible()) {
            if (!this.armed || !StringUtil.isAllowedChatCharacter(character)) {
               return false;
            }

            this.startSearch();
            if (!this.box.isVisible()) {
               return false;
            }
         }

         boolean handled = super.charTyped(character, modifiers);
         this.settle();
         return handled;
      } else {
         return false;
      }
   }

   protected void slotClicked(@Nullable Slot slot, int slotId, int button, ClickType type) {
      if (this.box != null && this.aimed && slot != null && !inventoryTab() && !((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).slots.isEmpty() && slot == ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).slots.get(0)) {
         this.remember(slot, type);
      }

      super.slotClicked(slot, slotId, button, type);
   }

   private void remember(Slot slot, ClickType type) {
      ItemStack stack = slot.getItem();
      if (!stack.isEmpty() && this.minecraft != null && this.minecraft.player != null && slot.mayPickup(this.minecraft.player)) {
         boolean empty = ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).getCarried().isEmpty();
         boolean taken = type == ClickType.SWAP || empty && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE || type == ClickType.CLONE);
         if (taken) {
            for(ItemStack known : RECENT) {
               if (ItemStack.isSameItemSameComponents(known, stack)) {
                  return;
               }
            }

            RECENT.add(0, stack.copyWithCount(1));

            while(RECENT.size() > 24) {
               RECENT.remove(RECENT.size() - 1);
            }

         }
      }
   }

   private boolean inSidebar(int x, int y) {
      return x >= 6 && x < 6 + this.sideW && y >= 7 && y < this.imageHeight - 7;
   }

   private int rowTop(CreativeModeTab tab) {
      int ry = 7 - Math.round(this.side.get());

      for(Entry e : this.entries) {
         if (e.tab() == tab) {
            break;
         }

         ry += e.height();
      }

      return ry;
   }

   @Nullable CreativeModeTab tabAt(int x, int y) {
      if (!this.inSidebar(x, y)) {
         return null;
      } else {
         int ry = 7 - Math.round(this.side.get());

         for(Entry e : this.entries) {
            int h = e.height();
            if (y >= ry && y < ry + h) {
               return e.tab();
            }

            ry += h;
         }

         return null;
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.box == null) {
         super.render(g, mouseX, mouseY, partialTick);
      } else {
         boolean covered = Popups.any();
         int pointX = covered ? -1 : mouseX;
         int pointY = covered ? -1 : mouseY;
         this.sync((double)pointX, (double)pointY);
         if (this.flow != null) {
            SimulatedSections.quiet();
         }

         Reskin.beginOwnTooltip();
         this.rendering = true;

         try {
            super.render(g, pointX, pointY, partialTick);
            if (this.tip != null && ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).getCarried().isEmpty()) {
               g.renderTooltip(this.font, name(this.tip), pointX, pointY);
            }
         } finally {
            this.rendering = this.widgetPass = false;
            Reskin.endOwnTooltip();
         }

      }
   }

   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(g, mouseX, mouseY, partialTick);
      if (this.box != null) {
         this.paint(g, partialTick, mouseX, mouseY);
      }

      this.widgetPass = this.rendering && this.box != null;
   }

   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      ContainerReskin.ownBackground();
   }

   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      this.widgetPass = false;
   }

   protected void renderSlot(GuiGraphics g, Slot slot) {
      this.widgetPass = false;
      super.renderSlot(g, slot);
   }

   private void paint(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      Look look = CFHGuiCreativeScreen.Look.current();
      CreativeModeTab tab = current();
      int x = this.leftPos;
      int y = this.topPos;
      int w = this.imageWidth;
      int h = this.imageHeight;
      if (look.vanilla()) {
         CFHGuiDraw.vanillaPanel(g, x, y, w, h, true);
         CFHGuiDraw.outline(g, x, y, w, h, -16777216, 0);
         CFHGuiDraw.outline(g, x + 1, y + 1, w - 2, h - 2, 1224736767, 0);
      } else {
         CFHGuiDraw.floatingPanel(g, x, y, w, h, 1.0F);
      }

      this.sidebar(g, look, tab, mouseX, mouseY);
      this.header(g, look, tab, mouseX, mouseY, partialTick);
      if (tab.getType() == Type.INVENTORY) {
         this.inventory(g, look, mouseX, mouseY);
      } else {
         this.grid(g, look, tab, mouseX, mouseY);
      }

      this.dock(g, look, tab);
   }

   private static Component name(CreativeModeTab tab) {
      return (Component)(tab.getType() == Type.INVENTORY ? Component.translatable("container.inventory") : tab.getDisplayName());
   }

   private void sidebar(GuiGraphics g, Look look, CreativeModeTab open, int mouseX, int mouseY) {
      Theme t = Theme.current();
      Palette p = t.palette();
      int x = this.leftPos + 6;
      int top = this.topPos + 7;
      int viewH = this.imageHeight - 7 - 7;
      CFHGuiDraw.vline(g, x + this.sideW + 4, top, viewH, look.rule());
      CreativeModeTab held = this.dragTab;
      if (held != null) {
         boolean there = false;

         for(Entry e : this.entries) {
            there |= e.tab() == held;
         }

         if (!there) {
            held = null;
            this.putBack();
         }
      }

      if (held != null) {
         this.carry(held, (double)mouseX, (double)mouseY);
         long now = Util.getMillis();
         float turn = this.atList((double)mouseX) ? (float)(160L * Math.min(50L, now - this.dragTurnMs)) / 1000.0F : 0.0F;
         this.dragTurnMs = now;
         if (mouseY < top + 12) {
            this.sideTarget -= turn;
         } else if (mouseY >= top + viewH - 12) {
            this.sideTarget += turn;
         }
      }

      int max = Math.max(0, this.sideH - viewH);
      this.sideTarget = Mth.clamp(this.sideTarget, 0.0F, (float)max);
      this.side.set(this.sideTarget);
      int offset = Math.round(this.side.get());
      boolean over = held == null && mouseX >= x && mouseX < x + this.sideW && mouseY >= top && mouseY < top + viewH;
      CreativeModeTab back = open.getType() == Type.SEARCH ? fallback() : null;
      this.tip = null;
      CFHGuiDraw.scissor(g, x, top, this.sideW, viewH);
      int chosen = -1;
      int at = 0;

      for(Entry e : this.entries) {
         if (e.tab() == open) {
            chosen = at;
            break;
         }

         at += e.height();
      }

      if (chosen >= 0) {
         if (this.marked) {
            this.mark.set((float)chosen);
         } else {
            this.mark.snap((float)chosen);
         }

         int my = top - offset + Math.round(this.mark.get());
         if (open != held) {
            if (look.vanilla()) {
               g.fill(x, my, x + this.sideW, my + 20, 1627389951);
               CFHGuiDraw.outline(g, x, my, this.sideW, 20, -1, 0);
            } else {
               CFHGuiDraw.pixelRound(g, x, my, this.sideW, 20, p.surfaceActive(), t.radius());
               CFHGuiDraw.rect(g, x, my + 3, 2, 14, p.accent());
            }
         }
      }

      this.marked = chosen >= 0;
      int ry = top - offset;

      for(Entry e : this.entries) {
         int h = e.height();
         if (ry + h > top && ry < top + viewH) {
            CreativeModeTab tab = e.tab();
            if (tab == null) {
               if (e.caption() != null) {
                  g.drawString(this.font, CFHGuiDraw.truncate(e.caption(), this.sideW - 8), x + 4, ry + 4, look.dim(), look.vanilla());
               }
            } else if (tab == held) {
               if (look.vanilla()) {
                  g.fill(x, ry, x + this.sideW, ry + 20, 419430399);
               } else {
                  CFHGuiDraw.pixelRound(g, x, ry, this.sideW, 20, Colors.withAlpha(p.surfaceHover(), 128), t.radius());
               }
            } else {
               boolean sel = tab == open;
               boolean hov = over && mouseY >= ry && mouseY < ry + 20;
               if (!sel && (hov || tab == back)) {
                  if (look.vanilla()) {
                     g.fill(x, ry, x + this.sideW, ry + 20, 822083583);
                  } else {
                     CFHGuiDraw.pixelRound(g, x, ry, this.sideW, 20, tab == back ? p.surfaceActive() : p.surfaceHover(), t.radius());
                  }
               }

               g.renderItem(tab.getIconItem(), x + 4, ry + 2);
               Component name = name(tab);
               int room = this.sideW - 23 - 4;
               g.drawString(this.font, CFHGuiDraw.truncate(name, room), x + 23, CFHGuiDraw.textY(ry, 20), !sel && !hov ? look.muted() : look.text(), look.vanilla());
               if (hov && this.font.width(name) > room) {
                  this.tip = tab;
               }
            }
         }

         ry += h;
      }

      CFHGuiDraw.unscissor(g);
      if (max > 0 && !look.vanilla()) {
         int ground = t.panel(p.bg());
         int clear = ground & 16777215;
         if (offset > 0) {
            g.fillGradient(x, top, x + this.sideW, top + 12, 300, ground, clear);
         }

         if (offset < max) {
            g.fillGradient(x, top + viewH - 12, x + this.sideW, top + viewH, 300, clear, ground);
         }
      }

      if (max > 0) {
         int thumb = Math.max(12, viewH * viewH / this.sideH);
         int ty = top + Math.round((float)(viewH - thumb) * ((float)offset / (float)max));
         CFHGuiDraw.rect(g, x + this.sideW + 1, ty, 2, thumb, look.vanilla() ? -7631989 : p.borderStrong());
      }

      if (held != null) {
         this.carriedRow(g, look, held, held == open, x, Mth.clamp(mouseY - this.pressInRow, top, top + viewH - 20));
      }

   }

   private void carriedRow(GuiGraphics g, Look look, CreativeModeTab tab, boolean open, int x, int y) {
      Theme t = Theme.current();
      Palette p = t.palette();
      g.pose().pushPose();
      g.pose().translate(0.0F, 0.0F, 350.0F);
      if (look.vanilla()) {
         g.fill(x, y, x + this.sideW, y + 20, -266330080);
         CFHGuiDraw.outline(g, x, y, this.sideW, 20, -1, 0);
      } else {
         CFHGuiDraw.pixelRound(g, x, y + 2, this.sideW, 20, p.shadow(), t.radius());
         CFHGuiDraw.pixelRound(g, x, y, this.sideW, 20, p.surfaceActive(), t.radius());
         CFHGuiDraw.outline(g, x, y, this.sideW, 20, p.borderStrong(), t.radius());
         if (open) {
            CFHGuiDraw.rect(g, x, y + 3, 2, 14, p.accent());
         }
      }

      g.renderItem(tab.getIconItem(), x + 4, y + 2);
      g.drawString(this.font, CFHGuiDraw.truncate(name(tab), this.sideW - 23 - 4), x + 23, CFHGuiDraw.textY(y, 20), look.text(), look.vanilla());
      g.pose().popPose();
   }

   private void header(GuiGraphics g, Look look, CreativeModeTab tab, int mouseX, int mouseY, float partialTick) {
      EditBox field = this.box;
      if (field != null) {
         int x = this.leftPos;
         int y = this.topPos;
         int textY = y + 7 + 4;
         Component title = Fonts.heading(name(tab));
         int titleRoom = this.fieldX - 8 - this.gridX;
         g.drawString(this.font, CFHGuiDraw.truncate(title, titleRoom), x + this.gridX, textY, look.text(), look.vanilla());
         Component line = this.line(tab);
         if (line != null) {
            int used = Math.min(this.font.width(title), titleRoom) + 8;
            int room = titleRoom - used;
            if (this.font.width(line) <= room) {
               g.drawString(this.font, line, x + this.gridX + used, textY, look.dim(), look.vanilla());
            } else if (room >= 60) {
               g.drawString(this.font, CFHGuiDraw.truncate(line, room), x + this.gridX + used, textY, look.dim(), look.vanilla());
            }
         }

         int fx = x + this.fieldX;
         int fy = y + 7;
         boolean live = field.isVisible();
         boolean focused = live ? field.isFocused() : this.armed;
         boolean hovered = this.inField(mouseX - x, mouseY - y);
         if (look.vanilla()) {
            CFHGuiDraw.vanillaTextField(g, fx, fy, this.fieldW, 16, focused);
         } else {
            ReskinDraw.textField(g, fx, fy, this.fieldW, 16, focused, hovered ? 1.0F : 0.0F, true);
         }

         Icons.draw(g, Icon.SEARCH, fx + 4, fy + 4, 8, focused ? look.text() : look.dim());
         if (live) {
            field.render(g, mouseX, mouseY, partialTick);
         } else if (this.armed) {
            if (Util.getMillis() / 300L % 2L == 0L) {
               g.drawString(this.font, "_", fx + 15, textY, look.text(), look.vanilla());
            }
         } else {
            g.drawString(this.font, CFHGuiDraw.truncate(Component.translatable("gui.recipebook.search_hint"), this.fieldW - 20), fx + 15, textY, look.dim(), false);
         }

      }
   }

   private @Nullable Component line(CreativeModeTab tab) {
      MutableComponent var10000;
      switch (tab.getType()) {
         case INVENTORY:
            var10000 = null;
            break;
         case HOTBAR:
            var10000 = this.minecraft == null ? null : Component.translatable("inventory.hotbarInfo", new Object[]{this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage(), Component.literal("1-9")});
            break;
         default:
            TabFlow laid = this.flow;
            int n = laid != null ? laid.stacks : ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).items.size();
            var10000 = Component.translatable(n == 1 ? "cfhgui.creative.item" : "cfhgui.creative.items", new Object[]{n});
      }

      return var10000;
   }

   private static void well(GuiGraphics g, Look look, int x, int y) {
      if (!look.vanilla()) {
         ReskinDraw.containerSlot(g, x, y);
      } else {
         g.fill(x, y, x + 18, y + 18, -7631989);
         g.fill(x, y, x + 17, y + 1, -13158601);
         g.fill(x, y, x + 1, y + 17, -13158601);
         g.fill(x + 1, y + 17, x + 18, y + 18, -1);
         g.fill(x + 17, y + 1, x + 18, y + 18, -1);
      }
   }

   private void item(ItemStack stack, int cellX, int cellY) {
      if (!stack.isEmpty() && (!this.aimed || cellX != this.leftPos + this.aimX || cellY != this.topPos + this.aimY)) {
         this.batch.add(stack, cellX + 1, cellY + 1);
      }
   }

   private void grid(GuiGraphics g, Look look, CreativeModeTab tab, int mouseX, int mouseY) {
      int across = this.gridCols();
      int gx = this.leftPos + this.gridLeft();
      int gy = this.topPos + 29;
      TabFlow laid = this.flow;
      g.drawManaged(() -> {
         for(int r = 0; r < this.rows; ++r) {
            if (laid == null || laid.heading(this.scrollRow + r) < 0) {
               for(int c = 0; c < across; ++c) {
                  well(g, look, gx + c * 18, gy + r * 18);
               }
            }
         }

      });
      List<ItemStack> items = ((CreativeModeInventoryScreen.ItemPickerMenu)(Object)this.menu).items;
      if (laid == null) {
         for(int r = 0; r < this.rows; ++r) {
            for(int c = 0; c < across; ++c) {
               int i = (this.scrollRow + r) * across + c;
               if (i >= items.size()) {
                  break;
               }

               this.item((ItemStack)items.get(i), gx + c * 18, gy + r * 18);
            }
         }
      } else {
         boolean sections = SimulatedSections.owns(tab);

         for(int r = 0; r < this.rows; ++r) {
            int heading = laid.heading(this.scrollRow + r);
            if (heading >= 0) {
               this.heading(g, look, sections ? SimulatedSections.banner(heading) : null, gx, gy + r * 18, across * 18, mouseX, mouseY);
            } else {
               for(int c = 0; c < across; ++c) {
                  int i = laid.entry(this.scrollRow + r, c);
                  if (i >= 0 && i < items.size()) {
                     this.item((ItemStack)items.get(i), gx + c * 18, gy + r * 18);
                  }
               }
            }
         }
      }

      this.batch.draw(g, this.font);
      int max = this.maxScroll();
      if (max > 0) {
         int bx = this.leftPos + this.barX;
         int track = this.rows * 18;
         int thumb = this.thumbH(max);
         int ty = gy + Math.round((float)(track - thumb) * ((float)this.scrollRow / (float)max));
         if (look.vanilla()) {
            g.fill(bx, gy, bx + 4, gy + track, Integer.MIN_VALUE);
            g.fill(bx, ty, bx + 4, ty + thumb, this.grabbed ? -1 : -4144960);
         } else {
            Palette p = Theme.current().palette();
            CFHGuiDraw.pixelRound(g, bx, gy, 4, track, Colors.withAlpha(p.surface(), 128), 1);
            CFHGuiDraw.pixelRound(g, bx, ty, 4, thumb, this.grabbed ? p.textDim() : p.borderStrong(), 1);
         }

      }
   }

   private void heading(GuiGraphics g, Look look, SimulatedSections.@Nullable Banner banner, int x, int y, int w, int mouseX, int mouseY) {
      int from = x;
      if (banner != null) {
         boolean pointed = mouseX >= x && mouseX < x + 162 && mouseY >= y && mouseY < y + 18;
         SimulatedSections.draw(g, this.font, banner, x, y, pointed);
         from = x + 162 + 6;
      }

      if (from < x + w) {
         CFHGuiDraw.hline(g, from, y + 9, x + w - from, look.rule());
      }

   }

   private void inventory(GuiGraphics g, Look look, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      g.drawManaged(() -> {
         for(int i = 0; i < 4; ++i) {
            well(g, look, x + this.armX[i], y + this.armY[i]);
         }

         well(g, look, x + this.offX, y + this.offY);
         well(g, look, x + this.binX, y + this.binY);

         for(int i = 0; i < 27; ++i) {
            well(g, look, x + this.midX + i % 9 * 18, y + this.mainY + i / 9 * 18);
         }

      });
      Icons.draw(g, Icon.TRASH, x + this.binX + 3, y + this.binY + 3, 12, look.dim());
      if (this.figH > 0 && this.minecraft != null && this.minecraft.player != null) {
         int fx = x + this.figX;
         int fy = y + this.figY;
         if (look.vanilla()) {
            g.fill(fx, fy, fx + this.figW, fy + this.figH, -16777216);
            CFHGuiDraw.outline(g, fx, fy, this.figW, this.figH, -7631989, 0);
         } else {
            ReskinDraw.containerInset(g, fx, fy, this.figW, this.figH);
         }

         int scale = Math.max(8, Math.min(Math.round((float)this.figH * 0.46F), Math.round((float)this.figW * 0.62F)));
         InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + 1, fy + 1, fx + this.figW - 1, fy + this.figH - 1, scale, 0.0625F, (float)mouseX, (float)mouseY, this.minecraft.player);
      }
   }

   private void dock(GuiGraphics g, Look look, CreativeModeTab tab) {
      boolean inventory = tab.getType() == Type.INVENTORY;
      int x = this.leftPos;
      int y = this.topPos + this.hotbarY;
      int hx = x + (inventory ? this.midX : this.hotbarX);
      g.drawString(this.font, Component.translatable("cfhgui.creative.hotbar"), hx, y - 11, look.dim(), look.vanilla());
      g.drawManaged(() -> {
         for(int c = 0; c < 9; ++c) {
            well(g, look, hx + c * 18, y);
         }

      });
      if (this.recentN > 0 && !inventory) {
         g.drawString(this.font, Component.translatable("cfhgui.creative.recent"), x + this.recentX, y - 11, look.dim(), look.vanilla());
         g.drawManaged(() -> {
            for(int c = 0; c < this.recentN; ++c) {
               well(g, look, x + this.recentX + c * 18, y);
            }

         });

         for(int c = 0; c < this.recentN && c < RECENT.size(); ++c) {
            this.item((ItemStack)RECENT.get(c), x + this.recentX + c * 18, y);
         }

         this.batch.draw(g, this.font);
      }
   }

   private static record Entry(@Nullable CreativeModeTab tab, @Nullable Component caption) {
      int height() {
         return this.tab == null ? 14 : 20;
      }
   }

   private static record Look(boolean vanilla, int text, int muted, int dim, int rule) {
      static Look current() {
         Theme t = Theme.current();
         if (t.isVanilla()) {
            return new Look(true, -1, -4144960, -6250336, 1090519039);
         } else {
            Palette p = t.palette();
            return new Look(false, p.text(), p.textMuted(), p.textDim(), p.border());
         }
      }
   }
}
