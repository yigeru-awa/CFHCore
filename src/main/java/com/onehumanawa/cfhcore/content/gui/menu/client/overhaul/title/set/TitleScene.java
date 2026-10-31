package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildCache;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildSpec;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneLoader;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public enum TitleScene {
   HEARTH("hearth", HearthSet::new, new String[0]);

   public static final TitleScene DEFAULT = HEARTH;
   private final String id;
   private final Supplier<TitleSet> set;
   private final String[] mods;

   private TitleScene(final String id, final Supplier<TitleSet> set, final String... mods) {
      this.id = id;
      this.set = set;
      this.mods = mods;
   }

   public String id() {
      return this.id;
   }

   public ResourceLocation sceneId() {
      return CFHGuiMenu.id("title/" + this.id);
   }

   public Component title() {
      return Component.translatable("cfhgui.overhaul.scene." + this.id);
   }

   public Component description() {
      return Component.translatable("cfhgui.overhaul.scene." + this.id + ".desc");
   }

   public @Nullable TitleSet create() {
      return (TitleSet)this.set.get();
   }

   public boolean available() {
      for(String mod : this.mods) {
         if (!CFHGuiPlatform.get().isModLoaded(mod)) {
            return false;
         }
      }

      return true;
   }

   public @Nullable BuildSpec build() {
      try {
         return SceneLoader.load(this.sceneId()).build();
      } catch (RuntimeException var2) {
         return null;
      }
   }

   public boolean missing() {
      BuildSpec b = this.build();
      return b != null && b.download() != null && !BuildCache.present(b);
   }

   public boolean fetched() {
      BuildSpec b = this.build();
      return b != null && b.download() != null;
   }

   public @Nullable String problem() {
      BuildSpec b = this.build();
      return b != null && b.download() != null ? BuildCache.problem(b) : null;
   }

   public static @Nullable TitleScene of(BuildSpec spec) {
      for(TitleScene s : offered()) {
         BuildSpec b = s.build();
         if (b != null && b.key().equals(spec.key())) {
            return s;
         }
      }

      return null;
   }

   public static List<TitleScene> fetchable() {
      List<TitleScene> out = new ArrayList();

      for(TitleScene s : offered()) {
         if (s.fetched()) {
            out.add(s);
         }
      }

      return out;
   }

   public static List<TitleScene> offered() {
      List<TitleScene> out = new ArrayList();

      for(TitleScene s : values()) {
         if (s.available()) {
            out.add(s);
         }
      }

      return out;
   }

   public TitleScene step(int direction) {
      return DEFAULT;
   }

   public static TitleScene of(@Nullable String id) {
      if (id != null) {
         String key = id.trim().toLowerCase(Locale.ROOT);

         for(TitleScene s : values()) {
            if (s.id.equals(key) || s.name().equalsIgnoreCase(key)) {
               return s.available() ? s : DEFAULT;
            }
         }
      }

      return DEFAULT;
   }
}
