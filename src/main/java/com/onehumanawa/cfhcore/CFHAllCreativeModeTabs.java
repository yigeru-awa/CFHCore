package com.onehumanawa.cfhcore;

import com.onehumanawa.cfhcore.foundation.RegistryHelper;
import com.simibubi.create.AllItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CFHAllCreativeModeTabs {
    public static final String MAIN_TAB_TITLE_KEY = "itemGroup." + CFHCore.ID + ".main";
    public static final ResourceKey<CreativeModeTab> MAIN_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CFHCore.asResource("main"));

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CFHCore.ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            TABS.register(MAIN_TAB_KEY.location().getPath(), () -> CreativeModeTab.builder()
                    .title(Component.translatable(MAIN_TAB_TITLE_KEY))
                    .icon(() -> new ItemStack(AllItems.BUILDERS_TEA.get()))
                    .build());

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        RegistryHelper.collectLang(MAIN_TAB_TITLE_KEY, CFHCore.NAME, CFHCore.NAME);
        TABS.register(bus);
    }
}