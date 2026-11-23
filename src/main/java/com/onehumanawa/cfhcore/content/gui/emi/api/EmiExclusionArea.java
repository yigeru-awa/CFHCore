package com.onehumanawa.cfhcore.content.gui.emi.api;

import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public interface EmiExclusionArea<T extends Screen> {
	void addExclusionArea(T var1, Consumer<Bounds> var2);
}
