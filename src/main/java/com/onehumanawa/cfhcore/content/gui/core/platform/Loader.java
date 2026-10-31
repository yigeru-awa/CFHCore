package com.onehumanawa.cfhcore.content.gui.core.platform;

public enum Loader {
   NEOFORGE("NeoForge"),
   FABRIC("Fabric");

   public final String displayName;

   private Loader(final String displayName) {
      this.displayName = displayName;
   }
}
