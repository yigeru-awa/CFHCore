package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.mojang.datafixers.util.Pair;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuIo;
import com.onehumanawa.cfhcore.content.gui.menu.client.worlds.SamplePlay;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import org.jetbrains.annotations.Nullable;

public final class WorldActions {
   private static final Component READING = Component.translatable("selectWorld.data_read");

   public static CompletableFuture<List<LevelSummary>> loadAll() {
      LevelStorageSource source = Minecraft.getInstance().getLevelSource();
      Objects.requireNonNull(source);
      CompletableFuture<LevelStorageSource.LevelCandidates> var10000 = CompletableFuture.supplyAsync(source::findLevelCandidates, MenuIo.POOL);
      Objects.requireNonNull(source);
      return var10000.thenCompose(source::loadLevelSummaries);
   }

   public static void play(LevelSummary summary, @Nullable Screen parent) {
      Minecraft mc = Minecraft.getInstance();
      if (summary.primaryActionActive()) {
         if (summary instanceof LevelSummary.SymlinkLevelSummary) {
            mc.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> mc.setScreen(parent)));
         } else {
            mc.forceSetScreen(new GenericMessageScreen(READING));
            mc.createWorldOpenFlows().openWorld(summary.getLevelId(), () -> mc.setScreen(parent));
         }
      }
   }

   public static void play(String levelId, @Nullable Screen parent) {
      Minecraft mc = Minecraft.getInstance();
      if (levelId != null && !levelId.isBlank() && mc.getLevelSource().levelExists(levelId)) {
         mc.forceSetScreen(new GenericMessageScreen(READING));
         mc.createWorldOpenFlows().openWorld(levelId, () -> mc.setScreen(parent));
      } else {
         CFHGuiToasts.show(Component.translatable("cfhgui.worlds.missing"), Component.literal(levelId == null ? "" : levelId), Icon.WARNING);
      }
   }

   private static boolean madeUp(String id) {
      if (!SamplePlay.isSample(id)) {
         return false;
      } else {
         CFHGuiToasts.show(Component.translatable("cfhgui.worlds.missing"), Component.literal(id), Icon.WARNING);
         return true;
      }
   }

   public static void edit(LevelSummary summary, @Nullable Screen parent, Runnable onChanged) {
      Minecraft mc = Minecraft.getInstance();
      String id = summary.getLevelId();
      if (!madeUp(id)) {
         mc.forceSetScreen(new GenericMessageScreen(READING));

         try {
            LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().validateAndCreateAccess(id);
            mc.setScreen(EditWorldScreen.create(mc, access, (changed) -> {
               access.safeClose();
               if (changed) {
                  onChanged.run();
               }

               mc.setScreen(parent);
            }));
         } catch (IOException e) {
            SystemToast.onWorldAccessFailure(mc, id);
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] cannot open world {} for editing", id, e);
            mc.setScreen(parent);
         } catch (ContentValidationException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] {}", e.getMessage());
            mc.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> mc.setScreen(parent)));
         }

      }
   }

   public static boolean backup(LevelSummary summary) {
      Minecraft mc = Minecraft.getInstance();
      String id = summary.getLevelId();
      if (madeUp(id)) {
         return false;
      } else {
         try {
            LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(id);

            boolean var4;
            try {
               var4 = EditWorldScreen.makeBackupAndShowToast(access);
            } catch (Throwable var7) {
               if (access != null) {
                  try {
                     access.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (access != null) {
               access.close();
            }

            return var4;
         } catch (IOException e) {
            SystemToast.onWorldAccessFailure(mc, id);
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] backup of {} failed", id, e);
            return false;
         }
      }
   }

   public static boolean delete(LevelSummary summary) {
      Minecraft mc = Minecraft.getInstance();
      String id = summary.getLevelId();
      if (madeUp(id)) {
         return false;
      } else {
         try {
            LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(id);

            boolean var4;
            try {
               access.deleteLevel();
               WorldFavorites.forget(id);
               var4 = true;
            } catch (Throwable var7) {
               if (access != null) {
                  try {
                     access.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (access != null) {
               access.close();
            }

            return var4;
         } catch (IOException e) {
            SystemToast.onWorldDeleteFailure(mc, id);
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] delete of {} failed", id, e);
            return false;
         }
      }
   }

   public static void duplicate(LevelSummary summary, String newName, Runnable onDone) {
      if (!madeUp(summary.getLevelId())) {
         Minecraft mc = Minecraft.getInstance();
         LevelStorageSource source = mc.getLevelSource();
         String name = newName != null && !newName.isBlank() ? newName.trim() : summary.getLevelName() + " copy";
         Path from = source.getLevelPath(summary.getLevelId());
         Path base = source.getBaseDir();

         String folder;
         try {
            folder = FileUtil.findAvailableName(base, FileUtil.sanitizeName(name), "");
         } catch (IOException e) {
            CFHGuiToasts.show(Component.translatable("cfhgui.worlds.duplicate_failed"), Component.literal(e.getMessage() == null ? "" : e.getMessage()), Icon.ERROR);
            return;
         }

         Path to = base.resolve(folder);
         CompletableFuture.runAsync(() -> {
            try {
               copyTree(from, to);
            } catch (IOException e) {
               throw new UncheckedIOException(e);
            }
         }, MenuIo.POOL).whenCompleteAsync((v, err) -> {
            if (err != null) {
               CFHGuiMenu.LOGGER.error("[CFHGui Menu] duplicate of {} failed", summary.getLevelId(), err);
               CFHGuiToasts.show(Component.translatable("cfhgui.worlds.duplicate_failed"), Component.literal(String.valueOf(err.getMessage())), Icon.ERROR);
            } else {
               try {
                  LevelStorageSource.LevelStorageAccess access = source.createAccess(folder);

                  try {
                     access.renameLevel(name);
                  } catch (Throwable var11) {
                     if (access != null) {
                        try {
                           access.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (access != null) {
                     access.close();
                  }
               } catch (Exception e) {
                  CFHGuiMenu.LOGGER.warn("[CFHGui Menu] copied {} but could not rename it: {}", folder, e.toString());
               }

               CFHGuiToasts.show(Component.translatable("cfhgui.worlds.duplicated"), Component.literal(name), Icon.DUPLICATE);
            }

            onDone.run();
         }, mc);
      }
   }

   private static void copyTree(Path from, Path to) throws IOException {
      Stream<Path> s = Files.walk(from);

      try {
         Objects.requireNonNull(s);

         for(Path p : (Iterable<Path>)s::iterator) {
            Path rel = from.relativize(p);
            if (rel.getFileName() == null || !rel.getFileName().toString().equals("session.lock")) {
               Path target = to.resolve(rel.toString());
               if (Files.isDirectory(p, new LinkOption[0])) {
                  Files.createDirectories(target);
               } else {
                  Files.createDirectories(target.getParent());
                  Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
               }
            }
         }
      } catch (Throwable var8) {
         if (s != null) {
            try {
               s.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (s != null) {
         s.close();
      }

   }

   public static void openFolder(LevelSummary summary) {
      Util.getPlatform().openPath(Minecraft.getInstance().getLevelSource().getLevelPath(summary.getLevelId()));
   }

   public static void openSavesFolder() {
      Util.getPlatform().openPath(Minecraft.getInstance().getLevelSource().getBaseDir());
   }

   public static void recreate(LevelSummary summary, @Nullable Screen parent) {
      if (!madeUp(summary.getLevelId())) {
         Minecraft mc = Minecraft.getInstance();
         mc.forceSetScreen(new GenericMessageScreen(READING));

         try {
            LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().validateAndCreateAccess(summary.getLevelId());

            try {
               Pair<LevelSettings, WorldCreationContext> pair = mc.createWorldOpenFlows().recreateWorldData(access);
               LevelSettings settings = (LevelSettings)pair.getFirst();
               WorldCreationContext ctx = (WorldCreationContext)pair.getSecond();
               Path temp = CreateWorldScreen.createTempDataPackDirFromExistingWorld(access.getLevelPath(LevelResource.DATAPACK_DIR), mc);
               if (ctx.options().isOldCustomizedWorld()) {
                  mc.setScreen(new ConfirmScreen((ok) -> mc.setScreen((Screen)(ok ? CreateWorldScreen.createFromExisting(mc, parent, settings, ctx, temp) : parent)), Component.translatable("selectWorld.recreate.customized.title"), Component.translatable("selectWorld.recreate.customized.text"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
               } else {
                  mc.setScreen(CreateWorldScreen.createFromExisting(mc, parent, settings, ctx, temp));
               }
            } catch (Throwable var9) {
               if (access != null) {
                  try {
                     access.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (access != null) {
               access.close();
            }
         } catch (ContentValidationException e) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] {}", e.getMessage());
            mc.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> mc.setScreen(parent)));
         } catch (Exception e) {
            CFHGuiMenu.LOGGER.error("[CFHGui Menu] cannot recreate world {}", summary.getLevelId(), e);
            mc.setScreen(new AlertScreen(() -> mc.setScreen(parent), Component.translatable("selectWorld.recreate.error.title"), Component.translatable("selectWorld.recreate.error.text")));
         }

      }
   }

   public static void createNew(@Nullable Screen parent) {
      CreateWorldScreen.openFresh(Minecraft.getInstance(), parent);
   }

   public static CompletableFuture<Long> sizeOf(String levelId) {
      Path dir = Minecraft.getInstance().getLevelSource().getLevelPath(levelId);
      return CompletableFuture.supplyAsync(() -> {
         try {
            Stream<Path> s = Files.walk(dir);

            Long var2;
            try {
               var2 = s.filter((x$0) -> Files.isRegularFile(x$0, new LinkOption[0])).mapToLong((p) -> {
                  try {
                     return Files.size(p);
                  } catch (IOException var3) {
                     return 0L;
                  }
               }).sum();
            } catch (Throwable var5) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (s != null) {
               s.close();
            }

            return var2;
         } catch (IOException var6) {
            return -1L;
         }
      }, MenuIo.POOL);
   }

   private WorldActions() {
   }
}
