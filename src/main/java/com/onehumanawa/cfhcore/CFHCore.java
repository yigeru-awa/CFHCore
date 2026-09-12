package com.onehumanawa.cfhcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.onehumanawa.cfhcore.content.world.neoforge.CFHWorldGenNeoForge;
import com.onehumanawa.cfhcore.foundation.ModLangProvider;
import com.onehumanawa.cfhcore.foundation.ModificationHelper;
import com.onehumanawa.cfhcore.foundation.RegistryHelper;
import com.onehumanawa.cfhcore.foundation.TooltipHelper;
import com.onehumanawa.cfhcore.foundation.registry.LangCollector;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

@Mod(CFHCore.ID)
public class CFHCore {
    public static final String ID = "cfhcore";
    public static final String NAME = "CFHCore";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final LangCollector LANG_COLLECTOR = new LangCollector();

    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .setTooltipModifierFactory(item ->
                    TooltipHelper.modifierFor(item)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );

    public CFHCore(IEventBus eventBus, ModContainer modContainer) {
        onCreateMod(eventBus, modContainer);
    }

    private void onCreateMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} Initializing!", NAME);
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        REGISTRATE.registerEventListeners(modEventBus);
        CFHAllCreativeModeTabs.register(modEventBus);

        TooltipHelper.init();
        RegistryHelper.init();
        ModificationHelper.register(modEventBus);
        ModificationHelper.init();

        CFHWorldGenNeoForge.register(modEventBus);

        modEventBus.addListener(this::gatherData);
    }

    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        if (event.includeClient()) {
            generator.addProvider(true, new ModLangProvider(output, "en_us"));
            generator.addProvider(true, new ModLangProvider(output, "zh_cn"));
        }
        CFHWorldGenNeoForge.gatherData(event);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static ResourceLocation asResource(String modId, String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    public static LangCollector langCollector() {
        return LANG_COLLECTOR;
    }
}