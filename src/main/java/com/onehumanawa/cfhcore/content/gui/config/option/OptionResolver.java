package com.onehumanawa.cfhcore.content.gui.config.option;

import java.util.Optional;

public interface OptionResolver {
   String prefix();

   Optional<OptionBinding> resolve(String var1);
}
