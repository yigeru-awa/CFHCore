package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiSounds {
   private static final float CLICK_MS = 160.0F;
   private static final float CHIME_MS = 1103.0F;
   private static long clickOver;
   private static long chimeOver;
   public static @Nullable Consumer<String> trace;

   private static void trace(String what) {
      if (trace != null) {
         trace.accept(what);
      }

   }

   public static void click(SoundManager sounds) {
      boolean vanilla = Theme.current().isVanilla();
      click(sounds, "click", vanilla ? 1.0F : 0.85F, vanilla ? 1.0F : 0.6F);
   }

   public static void click() {
      click(Minecraft.getInstance().getSoundManager());
   }

   public static void tick() {
      click(Minecraft.getInstance().getSoundManager(), "tick", 1.25F, 0.3F);
   }

   private static void click(SoundManager sounds, String name, float pitch, float volume) {
      if (Theme.current().uiSounds()) {
         long now = Util.getMillis();
         if (now < clickOver) {
            trace(name + ": dropped, the last one sounds for " + (clickOver - now) + " ms more");
         } else {
            clickOver = now + (long)Math.round(160.0F / pitch);
            trace(name + ": plays");
            sounds.play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
         }
      }
   }

   public static void chime() {
      if (Theme.current().uiSounds()) {
         long now = Util.getMillis();
         if (now < chimeOver) {
            long var10000 = chimeOver - now;
            trace("chime: dropped, the last one sounds for " + var10000 + " ms more");
         } else {
            chimeOver = now + (long)Math.round(787.8572F);
            trace("chime: plays");
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI((SoundEvent)SoundEvents.NOTE_BLOCK_CHIME.value(), 1.4F, 0.5F));
         }
      }
   }

   private CFHGuiSounds() {
   }
}
