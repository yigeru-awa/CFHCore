package com.onehumanawa.cfhcore.content.gui.config.doc;

import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record DocSection(String path, Component title, @Nullable Component comment, int depth, List<OptionBinding> options) {
}
