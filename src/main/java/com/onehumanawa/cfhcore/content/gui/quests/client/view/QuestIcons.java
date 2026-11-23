package com.onehumanawa.cfhcore.content.gui.quests.client.view;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.quests.logic.BookIndex;
import com.onehumanawa.cfhcore.content.gui.quests.model.Quest;
import com.onehumanawa.cfhcore.content.gui.quests.model.Reward;
import com.onehumanawa.cfhcore.content.gui.quests.model.Task;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public final class QuestIcons {
	private static final QuestIcons.Drawable NONE = new QuestIcons.Drawable() {
		@Override
		public void draw(GuiGraphics g, float cx, float cy, float size, int tint) {
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	};
	private static final Map<String, QuestIcons.Drawable> CACHE = new HashMap<>();

	public static QuestIcons.Drawable of(@Nullable String spec) {
		return spec != null && !spec.isBlank() ? CACHE.computeIfAbsent(spec.trim(), QuestIcons::read) : NONE;
	}

	private static QuestIcons.Drawable read(String spec) {
		if (spec.startsWith("icon:")) {
			try {
				return new QuestIcons.Glyph(Icon.valueOf(spec.substring(5).trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException var2) {
				return new QuestIcons.Glyph(Icon.QUESTION);
			}
		} else if (spec.startsWith("texture:")) {
			ResourceLocation id = ResourceLocation.tryParse(spec.substring(8).trim());
			return (QuestIcons.Drawable)(id == null ? new QuestIcons.Glyph(Icon.QUESTION) : new QuestIcons.Picture(id));
		} else {
			return new QuestIcons.Items0(spec);
		}
	}

	public static QuestIcons.Drawable glyph(Icon icon) {
		return new QuestIcons.Glyph(icon);
	}

	public static QuestIcons.Drawable of(BookIndex.Entry entry) {
		Quest q = entry.quest();
		if (!q.icon.isBlank()) {
			return of(q.icon);
		} else {
			for (Task t : q.tasks) {
				QuestIcons.Drawable d = of(t);
				if (!d.isEmpty()) {
					return d;
				}
			}

			return of(q.isGateway() ? "icon:compass" : "icon:quest");
		}
	}

	public static QuestIcons.Drawable of(Task task) {
		if (!task.icon.isBlank()) {
			return of(task.icon);
		} else if (!task.item.isBlank()) {
			return of(task.item);
		} else if (!task.block.isBlank()) {
			return of(task.block);
		} else if (!task.entity.isBlank() && !task.entity.startsWith("#")) {
			ResourceLocation id = ResourceLocation.tryParse(task.entity);
			EntityType<?> type = id == null ? null : (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
			SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
			return egg != null ? of(BuiltInRegistries.ITEM.getKey(egg).toString()) : of("icon:sword");
		} else {
			if (!task.fluid.isBlank() && !task.fluid.startsWith("#")) {
				ResourceLocation id = ResourceLocation.tryParse(task.fluid);
				Fluid fluid = id == null ? null : (Fluid)BuiltInRegistries.FLUID.getOptional(id).orElse(null);
				if (fluid != null && fluid.getBucket() != Items.AIR) {
					return of(BuiltInRegistries.ITEM.getKey(fluid.getBucket()).toString());
				}
			}

			String var4 = task.type;

			return switch (var4) {
				case "fluid" -> of(BuiltInRegistries.ITEM.getKey(Items.BUCKET).toString());
				case "energy" -> of("icon:bolt");
				case "stage" -> of("icon:tag");
				case "counter" -> of("icon:hash");
				case "kill" -> of("icon:sword");
				case "advancement" -> of("icon:trophy");
				case "stat" -> of("icon:signal");
				case "xp" -> of(BuiltInRegistries.ITEM.getKey(Items.EXPERIENCE_BOTTLE).toString());
				case "location" -> of("icon:compass");
				case "observe" -> of("icon:eye");
				case "checkmark" -> of("icon:check");
				case "quest", "chapter" -> of("icon:quest");
				case "team" -> of("icon:team");
				default -> of("icon:dot");
			};
		}
	}

	public static QuestIcons.Drawable of(Reward reward) {
		if (!reward.icon.isBlank()) {
			return of(reward.icon);
		} else if (!reward.item.isBlank()) {
			return of(reward.item);
		} else {
			String var1 = reward.type;

			return switch (var1) {
				case "xp" -> of(BuiltInRegistries.ITEM.getKey(Items.EXPERIENCE_BOTTLE).toString());
				case "command" -> of("icon:terminal");
				case "loot", "random" -> of(BuiltInRegistries.ITEM.getKey(Items.CHEST).toString());
				case "choice" -> of("icon:list");
				case "all" -> of(BuiltInRegistries.ITEM.getKey(Items.BUNDLE).toString());
				case "toast" -> of("icon:bell");
				case "stage" -> of("icon:tag");
				case "counter" -> of("icon:hash");
				case "currency" -> of(BuiltInRegistries.ITEM.getKey(Items.GOLD_NUGGET).toString());
				case "custom" -> of("icon:sparkle");
				case "team_xp" -> of("icon:team");
				case "advancement" -> of("icon:trophy");
				default -> of("icon:star_filled");
			};
		}
	}

	public static void clear() {
		CACHE.clear();
	}

	private QuestIcons() {
	}

	public abstract static class Drawable {
		public abstract void draw(GuiGraphics var1, float var2, float var3, float var4, int var5);

		public boolean isEmpty() {
			return false;
		}

		public ItemStack stack() {
			return ItemStack.EMPTY;
		}
	}

	private static final class Glyph extends QuestIcons.Drawable {
		private final Icon icon;

		Glyph(Icon icon) {
			this.icon = icon;
		}

		@Override
		public void draw(GuiGraphics g, float cx, float cy, float size, int tint) {
			draw(g, this.icon, cx, cy, size, tint);
		}

		static void draw(GuiGraphics g, Icon icon, float cx, float cy, float size, int tint) {
			g.pose().pushPose();
			g.pose().translate(cx - size / 2.0F, cy - size / 2.0F, 0.0F);
			g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
			g.flush();
			Icons.draw(g, icon, 0, 0, tint);
			g.pose().popPose();
		}
	}

	private static final class Items0 extends QuestIcons.Drawable {
		private final String spec;

		Items0(String spec) {
			this.spec = spec;
		}

		@Override
		public ItemStack stack() {
			List<ItemStack> stacks = ItemSpecs.stacks(this.spec);
			return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get((int)(System.currentTimeMillis() / 1000L % (long)stacks.size()));
		}

		@Override
		public void draw(GuiGraphics g, float cx, float cy, float size, int tint) {
			ItemStack stack = this.stack();
			if (stack.isEmpty()) {
				QuestIcons.Glyph.draw(g, Icon.QUESTION, cx, cy, size, tint);
			} else {
				g.pose().pushPose();
				g.pose().translate(cx - size / 2.0F, cy - size / 2.0F, 0.0F);
				g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
				g.renderFakeItem(stack, 0, 0);
				g.pose().popPose();
			}
		}
	}

	private static final class Picture extends QuestIcons.Drawable {
		private final ResourceLocation texture;

		Picture(ResourceLocation texture) {
			this.texture = texture;
		}

		@Override
		public void draw(GuiGraphics g, float cx, float cy, float size, int tint) {
			g.pose().pushPose();
			g.pose().translate(cx - size / 2.0F, cy - size / 2.0F, 0.0F);
			g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
			g.flush();
			CFHGuiDraw.blit(g, this.texture, 0, 0, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16, 1.0F);
			g.pose().popPose();
		}
	}
}
