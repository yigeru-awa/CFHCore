package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

public final class AudioPage extends OptionPageBase {
   public AudioPage() {
      super("audio", Component.translatable("cfhgui_config.page.audio"), Icon.VOLUME);
   }

   protected List<Section> sections() {
      List<Section> out = new ArrayList();
      Section vol = Section.of("volume", Component.translatable("cfhgui_config.audio.volume"));
      vol.add(Binding.of("audio:music_enabled", OptionType.BOOLEAN, (Component)Component.translatable("cfhgui_config.audio.music_enabled")).tooltip((Component)Component.translatable("cfhgui_config.audio.music_enabled.tip")).getter(() -> (Double)music().get() > (double)0.0F).setter((v) -> {
         OptionInstance<Double> m = music();
         if (Boolean.TRUE.equals(v)) {
            double restore = ConfigSettings.get().musicVolumeBeforeMute;
            m.set(restore > (double)0.0F ? restore : (double)1.0F);
         } else {
            double cur = (Double)m.get();
            if (cur > (double)0.0F) {
               ConfigSettings.file().update((c) -> c.musicVolumeBeforeMute = cur);
            }

            m.set((double)0.0F);
         }

         Minecraft.getInstance().options.save();
      }).def(Boolean.TRUE).searchWords("music mute"));

      for(SoundSource src : SoundSource.values()) {
         Optional<OptionBinding> var10000 = VanillaOptions.get("soundCategory_" + src.getName());
         Objects.requireNonNull(vol);
         var10000.ifPresent(vol::add);
      }

      out.add(vol);
      out.add(Section.of("output", Component.translatable("cfhgui_config.audio.output"), VanillaOptions.all("soundDevice", "directionalAudio", "showSubtitles")));

      return out;
   }

   private static OptionInstance<Double> music() {
      return Minecraft.getInstance().options.getSoundSourceOptionInstance(SoundSource.MUSIC);
   }
}
