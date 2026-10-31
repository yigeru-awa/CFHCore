package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.Actions;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.stage.platform.StageCommands;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneCommandHandler;
import com.onehumanawa.cfhcore.content.gui.core.stage.test.StageTestScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class StageBootstrap {
   private static boolean initialised;

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         ScreenIds.register(StageTestScreen.class, "cfhgui:stage_test", "Stage test");
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stage_test", (p) -> new StageTestScreen(p, StageTestScreen.Variant.DEFAULT));
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stage_test/open", (p) -> new StageTestScreen(p, StageTestScreen.Variant.OPEN));
         CoreActions.SCREEN_FACTORIES.put("cfhgui:stage_test/motion0", (p) -> new StageTestScreen(p, StageTestScreen.Variant.MOTION0));
         CFHGuiEvents.SCREEN_OPENED.register(Stage::onScreenOpened);
         SceneCommandHandler handler = new SceneCommandHandler();
         Actions.register(new ActionType("cfhgui:scene_capture", Component.translatable("cfhgui.action.scene_capture"), List.of(ActionType.Arg.text("template", Component.translatable("cfhgui.action.arg.template"), "title"), ActionType.Arg.text("scene", Component.translatable("cfhgui.action.arg.scene"), "cfhgui:title"), ActionType.Arg.text("origin", Component.translatable("cfhgui.action.arg.origin"), "")), (a) -> {
            BlockPos origin = null;
            String o = ((String)a.getOrDefault("origin", "")).trim();
            if (!o.isEmpty()) {
               String[] parts = o.split("[ ,]+");
               if (parts.length == 3) {
                  try {
                     origin = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                  } catch (NumberFormatException var6) {
                  }
               }
            }

            Component result = handler.capture((String)a.getOrDefault("template", ""), (String)a.getOrDefault("scene", ""), origin);
            CFHGuiToasts.show(Component.translatable("cfhgui.stage.capture.title"), result, Icon.INFO);
            if (Minecraft.getInstance().player != null) {
               Minecraft.getInstance().player.displayClientMessage(result, false);
            }

         }));

         try {
            StageCommands.get().register(handler);
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] stage: client command not registered: {}", e.toString());
         }

      }
   }

   private StageBootstrap() {
   }
}
