package com.onehumanawa.cfhcore.foundation.ingredient;

import java.util.ArrayList;
import java.util.List;

import com.onehumanawa.cfhcore.CFHCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class ItemTagBuilder {

    private final TagKey<Item> tagKey;
    private final boolean valid;

    public ItemTagBuilder(String tagId) {
        ResourceLocation id = parseId(tagId);
        if (id == null) {
            CFHCore.LOGGER.error("[ItemTag] Invalid tag id: '{}' - expected \"modid:path\", modifications ignored", tagId);
            this.valid = false;
            this.tagKey = TagKey.create(Registries.ITEM, CFHCore.asResource("invalid_tag"));
            return;
        }
        this.valid = true;
        this.tagKey = TagKey.create(Registries.ITEM, id);
    }

    public TagKey<Item> tagKey() {
        return tagKey;
    }

    public ItemTagBuilder add(String... itemIds) {
        if (valid) {
            TagModificationHandler.addAdditions(tagKey, parse(itemIds));
        }
        return this;
    }

    public ItemTagBuilder remove(String... itemIds) {
        if (valid) {
            TagModificationHandler.addRemovals(tagKey, parse(itemIds));
        }
        return this;
    }

    private List<ResourceLocation> parse(String[] specs) {
        List<ResourceLocation> ids = new ArrayList<>(specs.length);
        for (String spec : specs) {
            ResourceLocation id = parseId(ItemSpec.decode(spec).id());
            if (id != null) {
                ids.add(id);
            } else {
                CFHCore.LOGGER.warn("[ItemTag] Invalid item id '{}' ignored for tag {}", spec, tagKey.location());
            }
        }
        return ids;
    }

    @Nullable
    private static ResourceLocation parseId(String s) {
        try {
            String[] parts = s.split(":", 2);
            return parts.length == 2
                    ? ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])
                    : ResourceLocation.withDefaultNamespace(s);
        } catch (Exception e) {
            return null;
        }
    }
}
