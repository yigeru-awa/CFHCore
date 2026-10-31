package com.onehumanawa.cfhcore.content.gui.core.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record ModInfo(String id, String name, String version, String description, List<String> authors, Optional<Path> iconPath) {
}
