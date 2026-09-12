package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.TemplateContext;

public record DecoratorConfig<T extends TemplateContext>(
   List<TemplateDecorator<T>> defaultDecorator, Map<ResourceLocation, List<TemplateDecorator<T>>> biomeDecorators
) {
   // The raw types below are load bearing. DecoratorConfig<T> is invariant in T, so a
   // DecoratorConfig<?> getter hands back List<TemplateDecorator<CAP>>, which never widens to
   // List<TemplateDecorator<?>> by itself, and the two wildcard captures produced while decoding
   // can't be unified by the diamond constructor either.
   @SuppressWarnings({"unchecked", "rawtypes"})
   public static final Codec<DecoratorConfig<?>> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               TemplateDecorator.CODEC.listOf().fieldOf("default_decorator").forGetter(c -> (List)c.defaultDecorator()),
               Codec.unboundedMap(ResourceLocation.CODEC, TemplateDecorator.CODEC.listOf()).fieldOf("biome_decorators").forGetter(c -> (Map)c.biomeDecorators())
            )
            .apply(instance, (defaultDecorator, biomeDecorators) -> (DecoratorConfig<?>)new DecoratorConfig(defaultDecorator, biomeDecorators))
   );

   public List<TemplateDecorator<T>> getDecorators(ResourceLocation biome) {
      return biome == null ? this.defaultDecorator : this.biomeDecorators.getOrDefault(biome, this.defaultDecorator);
   }
}
