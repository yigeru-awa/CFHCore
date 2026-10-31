package com.onehumanawa.cfhcore.content.gui.menu.api;

import org.jetbrains.annotations.Nullable;

public final class CFHGuiMenuApi {
   private static volatile @Nullable ServerPresenceProvider presence;
   private static volatile @Nullable ScreenshotShareProvider share;

   public static void setPresenceProvider(@Nullable ServerPresenceProvider provider) {
      presence = provider;
   }

   public static @Nullable ServerPresenceProvider presenceProvider() {
      return presence;
   }

   public static void setScreenshotShareProvider(@Nullable ScreenshotShareProvider provider) {
      share = provider;
   }

   public static @Nullable ScreenshotShareProvider screenshotShareProvider() {
      return share;
   }

   public static int friendsOn(String address) {
      ServerPresenceProvider p = presence;
      if (p != null && address != null) {
         try {
            return Math.max(0, p.friendsOn(address));
         } catch (Exception var3) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private CFHGuiMenuApi() {
   }
}
