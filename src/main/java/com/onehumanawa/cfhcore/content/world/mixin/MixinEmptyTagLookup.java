package com.onehumanawa.cfhcore.content.world.mixin;

import net.minecraft.core.HolderOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Renamed from CFHWorldGen's {@code MixinRegistrySetBuilder$EmptyTagLookup}; a {@code $} in a
 * source level type name is legal but confuses tooling, and nothing depends on the old name.
 */
@Mixin(
   targets = {"net/minecraft/core/RegistrySetBuilder$EmptyTagLookup"}
)
public interface MixinEmptyTagLookup<T> {
   @Accessor("owner")
   HolderOwner<T> getOwner();
}
