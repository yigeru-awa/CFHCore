package com.onehumanawa.cfhcore.content.gui.menu.api;

import java.nio.file.Path;

@FunctionalInterface
public interface ScreenshotShareProvider {
   void share(Path var1);
}
