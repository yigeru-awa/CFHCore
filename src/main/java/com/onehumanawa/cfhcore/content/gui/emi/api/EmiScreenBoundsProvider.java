package com.onehumanawa.cfhcore.content.gui.emi.api;

import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public interface EmiScreenBoundsProvider<T extends Screen> {
	@Nullable
	Bounds getBounds(T var1);
}
