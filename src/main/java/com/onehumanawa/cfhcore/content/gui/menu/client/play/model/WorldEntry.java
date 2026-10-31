package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.Nullable;

public final class WorldEntry {
   public final LevelSummary summary;
   public boolean favorite;
   public List<String> tags;
   public long size = -2L;
   public Textures.@Nullable Loaded icon;
   private boolean iconRequested;

   public WorldEntry(LevelSummary summary) {
      this.summary = summary;
      this.favorite = WorldFavorites.isFavorite(summary.getLevelId());
      this.tags = WorldFavorites.tags(summary.getLevelId());
   }

   public String id() {
      return this.summary.getLevelId();
   }

   public String name() {
      String n = this.summary.getLevelName();
      return n != null && !n.isBlank() ? n : this.summary.getLevelId();
   }

   public boolean matches(String query) {
      if (query != null && !query.isBlank()) {
         String q = query.toLowerCase(Locale.ROOT).trim();
         if (!this.name().toLowerCase(Locale.ROOT).contains(q) && !this.id().toLowerCase(Locale.ROOT).contains(q)) {
            for(String t : this.tags) {
               if (t.toLowerCase(Locale.ROOT).contains(q)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public void requestSize() {
      if (this.size == -2L) {
         this.size = -3L;
         WorldActions.sizeOf(this.id()).whenCompleteAsync((v, err) -> this.size = err == null && v != null ? v : -1L, Minecraft.getInstance());
      }
   }

   public void requestIcon() {
      if (!this.iconRequested) {
         this.iconRequested = true;
         Path p = this.summary.getIcon();
         if (p != null && Files.isRegularFile(p, new LinkOption[0]) && !Files.isSymbolicLink(p)) {
            Textures.load(p, (l) -> this.icon = l);
         }

      }
   }

   public void reloadMeta() {
      this.favorite = WorldFavorites.isFavorite(this.id());
      this.tags = WorldFavorites.tags(this.id());
   }
}
