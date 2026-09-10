package com.onehumanawa.cfhcore.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.foundation.ingredient.ItemSpec;
import com.onehumanawa.cfhcore.foundation.ingredient.ItemTagBuilder;

@SuppressWarnings({"unused"})
public final class TooltipHelper {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Palette> PALETTES = new HashMap<>();

    private TooltipHelper() {}

    public static void init() {
    }

    public static String itemOf(String id, String dataComponent) {
        return ItemSpec.of(id, dataComponent);
    }
    public static ItemTagBuilder itemTagOf(String tagId) {
        return new ItemTagBuilder(tagId);
    }
    public static TooltipBuilder tooltip(String itemId) {
        return new TooltipBuilder(itemId, false);
    }
    public static TooltipBuilder tooltip(DeferredHolder<Item, ?> item) {
        return tooltip(item.getId().toString());
    }
    public static TooltipBuilder tooltip(ItemTagBuilder tag) {
        return new TooltipBuilder(tag.tagKey().location().toString(), true);
    }
    public static TooltipBuilder tooltip(TagKey<Item> tag) {
        return new TooltipBuilder(tag.location().toString(), true);
    }
    public static void summary(String itemId, String en, String zh) {
        tooltip(itemId).summary(en, zh).register();
    }
    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }
    public static TooltipModifier modifierFor(Item item) {
        return new TooltipModifier() {
            private String cachedLanguage;
            private ItemDescription description;

            @Override
            public void modify(ItemTooltipEvent context) {
                String currentLanguage = Minecraft.getInstance().getLanguageManager().getSelected();
                if (!currentLanguage.equals(cachedLanguage)) {
                    cachedLanguage = currentLanguage;
                    description = ItemDescription.create(item, paletteOf(item));
                }
                if (description != null) {
                    context.getToolTip().addAll(1, description.getCurrentLines());
                }
            }
        };
    }
    public static Palette paletteOf(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        Palette palette = PALETTES.get(id.toString());
        return palette != null ? palette : Palette.STANDARD_CREATE;
    }
    public static Palette paletteFor(Entry entry) {
        Palette palette = PALETTES.get(entry.paletteKey());
        return palette != null ? palette : Palette.STANDARD_CREATE;
    }

    static void register(Entry entry, @Nullable Palette palette) {
        ENTRIES.add(entry);
        if (palette != null) {
            PALETTES.put(entry.paletteKey(), palette);
        }
    }

    @EventBusSubscriber(value = Dist.CLIENT, modid = CFHCore.ID)
    public static final class GlobalTooltips {

        private static final Map<Entry, Optional<ItemDescription>> CACHE = new HashMap<>();
        private static String cachedLanguage;

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            String language = Minecraft.getInstance().getLanguageManager().getSelected();
            if (!language.equals(cachedLanguage)) {
                cachedLanguage = language;
                CACHE.clear();
            }
            for (Entry entry : ENTRIES) {
                if (!entry.tag() && entry.targetId().startsWith(CFHCore.ID + ":")) {
                    continue;
                }
                if (!entry.matches(stack)) {
                    continue;
                }
                CACHE
                        .computeIfAbsent(entry, e -> Optional.ofNullable(
                                ItemDescription.create(e.baseKey(), paletteFor(e)))).ifPresent(description -> event.getToolTip().addAll(1, description.getCurrentLines()));
            }
        }
    }
    public record Entry(String targetId, boolean tag, String summaryEn, String summaryZh,
                        List<String[]> behaviours, List<String[]> controls) {

        public String paletteKey() {
            return tag ? "tag:" + targetId : targetId;
        }

        public String baseKey() {
            ResourceLocation id = ResourceLocation.parse(targetId);
            String prefix = tag ? "tag" : "item";
            return prefix + "." + id.getNamespace() + "." + id.getPath() + ".tooltip";
        }

        public boolean matches(ItemStack stack) {
            if (tag) {
                return stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(targetId)));
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(targetId);
        }

        public Map<String, String> translations(String locale) {
            String base = baseKey();
            boolean zh = locale.equals("zh_cn");
            Map<String, String> out = new LinkedHashMap<>();
            String summary = zh ? summaryZh : summaryEn;
            if (summary != null) {
                out.put(base + ".summary", summary);
            }
            int i = 1;
            for (String[] behaviour : behaviours) {
                out.put(base + ".condition" + i, zh ? behaviour[2] : behaviour[0]);
                out.put(base + ".behaviour" + i, zh ? behaviour[3] : behaviour[1]);
                i++;
            }
            i = 1;
            for (String[] control : controls) {
                out.put(base + ".control" + i, zh ? control[2] : control[0]);
                out.put(base + ".action" + i, zh ? control[3] : control[1]);
                i++;
            }
            return out;
        }
    }

    public static final class TooltipBuilder {
        private final String targetId;
        private final boolean tag;
        private final boolean valid;
        private String summaryEn;
        private String summaryZh;
        private Palette palette;
        private final List<String[]> behaviours = new ArrayList<>();
        private final List<String[]> controls = new ArrayList<>();

        private TooltipBuilder(String targetId, boolean tag) {
            if (!ID_PATTERN.matcher(targetId).matches()) {
                CFHCore.LOGGER.error("[Tooltip] Invalid {} id: '{}' - expected \"modid:path\" (pass itemTagOf(...) to tooltip() directly, without toString()); entry skipped",
                        tag ? "tag" : "item", targetId);
                this.valid = false;
                this.targetId = targetId;
                this.tag = tag;
                return;
            }
            this.valid = true;
            this.targetId = targetId;
            this.tag = tag;
        }

        public TooltipBuilder summary(String en, String zh) {
            this.summaryEn = en;
            this.summaryZh = zh;
            return this;
        }

        public TooltipBuilder behaviour(String conditionEn, String behaviourEn,
                                        String conditionZh, String behaviourZh) {
            behaviours.add(new String[]{conditionEn, behaviourEn, conditionZh, behaviourZh});
            return this;
        }

        public TooltipBuilder control(String controlEn, String actionEn,
                                      String controlZh, String actionZh) {
            controls.add(new String[]{controlEn, actionEn, controlZh, actionZh});
            return this;
        }

        public TooltipBuilder palette(Palette palette) {
            this.palette = palette;
            return this;
        }

        public void register() {
            if (!valid) return;
            TooltipHelper.register(
                    new Entry(targetId, tag, summaryEn, summaryZh,
                            List.copyOf(behaviours), List.copyOf(controls)),
                    palette);
        }
    }
}
