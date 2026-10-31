package com.onehumanawa.cfhcore.content.gui.menu.client.stats.table;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.UUID;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public sealed interface RowIcon {
   RowIcon NONE = new None();

   public static record None() implements RowIcon {
   }

   public static record Stack(ItemStack stack) implements RowIcon {
   }

   public static record Mob(@Nullable EntityType<?> type, ItemStack egg) implements RowIcon {
   }

   public static record Face(UUID id, String name, @Nullable Boolean online) implements RowIcon {
   }

   public static record Glyph(Icon icon) implements RowIcon {
   }
}
