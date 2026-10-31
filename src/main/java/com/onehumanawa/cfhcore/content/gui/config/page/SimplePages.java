package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.resolver.CoreBindings;
import com.onehumanawa.cfhcore.content.gui.config.resolver.Resolvers;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class SimplePages {
   static Section cfhguiModule(String module, String modId, String titleKey) {
      Section s = Section.of("cfhgui_" + module, Component.translatable(titleKey));
      List<OptionBinding> bs = Resolvers.cfhguiModuleBindings(module);
      if (bs.isEmpty()) {
         s.add(Binding.of("cfhgui:" + module + ":_none", OptionType.INFO, (Component)Component.translatable("cfhgui_config.module.no_file")).getter(() -> "config/cfhgui/" + module + ".json").searchWords(modId));
      } else {
         s.addAll(bs);
      }

      return s;
   }

   private SimplePages() {
   }

   public static final class ChatPage extends OptionPageBase {
      public ChatPage() {
         super("chat", Component.translatable("cfhgui_config.page.chat"), Icon.CHAT);
      }

      protected List<Section> sections() {
         List<Section> out = new ArrayList();
         out.add(Section.of("chat_box", Component.translatable("cfhgui_config.chat.box"), VanillaOptions.all("chatVisibility", "chatColors", "chatOpacity", "textBackgroundOpacity", "backgroundForChatOnly", "chatScale", "chatWidth", "chatHeightFocused", "chatHeightUnfocused", "chatLineSpacing")));
         out.add(Section.of("messages", Component.translatable("cfhgui_config.chat.messages"), VanillaOptions.all("chatLinks", "chatLinksPrompt", "chatDelay", "autoSuggestions", "hideMatchedNames", "onlyShowSecureChat", "notificationDisplayTime")));
         if (Modules.isLoaded("cfhgui_chat")) {
            out.add(SimplePages.cfhguiModule("chat", "cfhgui_chat", "cfhgui_config.chat.cfhgui"));
         }

         return out;
      }
   }

   public static final class OnlinePage extends OptionPageBase {
      public OnlinePage() {
         super("online", Component.translatable("cfhgui_config.page.online"), Icon.WIFI);
      }

      protected List<Section> sections() {
         List<Section> out = new ArrayList();
         Section online = Section.of("online", Component.translatable("cfhgui_config.multiplayer.online"), VanillaOptions.all("realmsNotifications", "allowServerListing", "telemetryOptInExtra"));
         online.add(Binding.of("multiplayer:hide_server_address", OptionType.BOOLEAN, (Component)Component.translatable("cfhgui_config.multiplayer.hide_address")).tooltip((Component)Component.translatable("cfhgui_config.multiplayer.hide_address.tip")).getter(() -> Minecraft.getInstance().options.hideServerAddress).setter((v) -> {
            Minecraft.getInstance().options.hideServerAddress = Boolean.TRUE.equals(v);
            Minecraft.getInstance().options.save();
         }).def(Boolean.FALSE));
         online.add(Binding.of("multiplayer:skip_warning", OptionType.BOOLEAN, (Component)Component.translatable("cfhgui_config.multiplayer.skip_warning")).tooltip((Component)Component.translatable("cfhgui_config.multiplayer.skip_warning.tip")).getter(() -> Minecraft.getInstance().options.skipMultiplayerWarning).setter((v) -> {
            Minecraft.getInstance().options.skipMultiplayerWarning = Boolean.TRUE.equals(v);
            Minecraft.getInstance().options.save();
         }).def(Boolean.FALSE).advanced(true));
         out.add(online);
         if (Modules.isLoaded("cfhgui_multiplayer")) {
            out.add(SimplePages.cfhguiModule("multiplayer", "cfhgui_multiplayer", "cfhgui_config.multiplayer.cfhgui"));
         }

         return out;
      }
   }

   public static final class AccessibilityPage extends OptionPageBase {
      public AccessibilityPage() {
         super("accessibility", Component.translatable("cfhgui_config.page.accessibility"), Icon.ACCESSIBILITY);
      }

      protected List<Section> sections() {
         List<Section> out = new ArrayList();
         out.add(Section.of("reading", Component.translatable("cfhgui_config.accessibility.reading"), VanillaOptions.all("narrator", "narratorHotkey", "showSubtitles", "highContrast", "textBackgroundOpacity", "backgroundForChatOnly", "chatOpacity", "chatLineSpacing", "chatDelay", "notificationDisplayTime", "menuBackgroundBlurriness")).basic());
         Section motion = Section.of("motion", Component.translatable("cfhgui_config.accessibility.motion"), VanillaOptions.all("screenEffectScale", "fovEffectScale", "darknessEffectScale", "damageTiltStrength", "glintSpeed", "glintStrength", "hideLightningFlashes", "bobView", "panoramaScrollSpeed", "darkMojangStudiosBackground", "hideSplashTexts"));
         motion.addAll(CoreBindings.all("motion", "transitions", "containerOpen"));
         out.add(motion.basic());
         return out;
      }
   }
}
