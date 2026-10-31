package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.ApplyQueue;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.world.Difficulty;

public final class GameplayGeneralPage extends OptionPageBase {
   private static final String TAB = "general";

   public GameplayGeneralPage() {
      super("general", Component.translatable("cfhgui_config.page.general"), Icon.SLIDERS);
   }

   protected boolean pills(String tabKey) {
      return false;
   }

   private static boolean canChangeDifficulty() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      return level != null && mc.hasSingleplayerServer() && !level.getLevelData().isDifficultyLocked() && !level.getLevelData().isHardcore();
   }

   static Binding difficultyBinding(Runnable rebuild) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      boolean locked = level != null && level.getLevelData().isDifficultyLocked();
      boolean hardcore = level != null && level.getLevelData().isHardcore();
      boolean single = mc.hasSingleplayerServer();
      List<Choice> choices = new ArrayList();

      for(Difficulty d : Difficulty.values()) {
         choices.add(new Choice(d.name(), d.getDisplayName()));
      }

      String why = hardcore ? "cfhgui_config.gameplay.difficulty.hardcore" : (locked ? "cfhgui_config.gameplay.difficulty.locked" : (!single ? "cfhgui_config.gameplay.difficulty.server" : "cfhgui_config.gameplay.difficulty.tip"));
      return Binding.of("gameplay:difficulty", OptionType.CHOICE, (Component)Component.translatable("options.difficulty")).tooltip((Component)Component.translatable(why)).choices(choices).getter(() -> {
         ClientLevel l = Minecraft.getInstance().level;
         return (l == null ? Difficulty.NORMAL : l.getDifficulty()).name();
      }).setter((v) -> {
         if (canChangeDifficulty() && mc.getConnection() != null) {
            String name = OptionValues.asString(v);

            for(Difficulty d : Difficulty.values()) {
               if (d.name().equals(name)) {
                  mc.getConnection().send(new ServerboundChangeDifficultyPacket(d));
               }
            }

            ApplyQueue.later("gameplay:difficulty", 250, rebuild);
         }
      }).enabledIf(GameplayGeneralPage::canChangeDifficulty).searchWords("difficulty peaceful easy normal hard world");
   }

   protected List<Section> sections() {
      Minecraft mc = Minecraft.getInstance();
      List<Section> out = new ArrayList();
      ClientLevel level = mc.level;
      if (level != null) {
         Section world = Section.of("world", Component.translatable("cfhgui_config.gameplay.world")).fixed().tab("general", this.title());
         boolean locked = level.getLevelData().isDifficultyLocked();
         boolean hardcore = level.getLevelData().isHardcore();
         boolean single = mc.hasSingleplayerServer();
         world.add(difficultyBinding(this::rebuild));
         if (single && !hardcore && !locked) {
            world.add(Binding.of("gameplay:lock_difficulty", OptionType.ACTION, (Component)Component.translatable("cfhgui_config.gameplay.lock")).tooltip((Component)Component.translatable("cfhgui_config.gameplay.lock.tip")).actionIcon(Icon.LOCK).action(Component.translatable("cfhgui_config.gameplay.lock.button"), () -> {
               ClientLevel l = Minecraft.getInstance().level;
               if (l != null) {
                  CFHGuiModal.confirmDanger(Component.translatable("difficulty.lock.title"), Component.translatable("difficulty.lock.question", new Object[]{l.getDifficulty().getDisplayName()}), Component.translatable("cfhgui_config.gameplay.lock.button"), () -> {
                     if (mc.getConnection() != null) {
                        mc.getConnection().send(new ServerboundLockDifficultyPacket(true));
                     }

                     ApplyQueue.later("gameplay:difficulty", 300, this::rebuild);
                  });
               }
            }).searchWords("difficulty lock world"));
         } else if (locked && !hardcore) {
            world.add(Binding.of("gameplay:difficulty_locked", OptionType.INFO, (Component)Component.translatable("cfhgui_config.gameplay.locked")).getter(() -> Component.translatable("cfhgui_config.gameplay.locked.value").getString()).searchWords("difficulty lock world"));
         }

         out.add(world);
      }

      out.add(Section.of("player", Component.translatable("cfhgui_config.gameplay.player"), VanillaOptions.all("mainHand", "attackIndicator", "operatorItemsTab")).fixed().tab("general", this.title()));
      return out;
   }
}
