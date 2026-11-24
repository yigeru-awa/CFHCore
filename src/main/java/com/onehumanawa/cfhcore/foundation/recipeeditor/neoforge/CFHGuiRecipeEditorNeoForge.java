package com.onehumanawa.cfhcore.foundation.recipeeditor.neoforge;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.foundation.recipeeditor.CFHGuiRecipeEditor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("cfhcore")
public final class CFHGuiRecipeEditorNeoForge {
	public CFHGuiRecipeEditorNeoForge(IEventBus modBus, ModContainer container) {
		Modules.register(CFHGuiRecipeEditor.MODULE);
		if (FMLEnvironment.dist.isClient()) {
			CFHGuiRecipeEditorNeoForgeClient.init(container);
		}
	}
}