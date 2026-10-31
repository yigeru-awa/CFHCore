package com.onehumanawa.cfhcore.content.gui.core.net;

public enum Flow {
   C2S,
   S2C,
   BOTH;

   public boolean toServer() {
      return this != S2C;
   }

   public boolean toClient() {
      return this != C2S;
   }
}
