package com.onehumanawa.cfhcore.content.gui.core.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import org.jetbrains.annotations.Nullable;

public final class PlayerSkins {
   private static final long PAUSE_MS = 350L;
   private static final int AT_ONCE = 8;
   private static final int MOST_WAITING = 48;
   private static final Map<UUID, Account> ACCOUNTS = new HashMap();
   private static final LinkedBlockingDeque<Account> WAITING = new LinkedBlockingDeque();
   private static @Nullable Thread asker;
   private static volatile @Nullable Function<UUID, PlayerSkin> worn;

   private PlayerSkins() {
   }

   public static void worn(@Nullable Function<UUID, PlayerSkin> looks) {
      worn = looks;
   }

   public static PlayerSkin of(UUID id) {
      Function<UUID, PlayerSkin> looks = worn;
      if (looks != null) {
         PlayerSkin look = (PlayerSkin)looks.apply(id);
         if (look != null) {
            return look;
         }
      }

      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      PlayerInfo info = connection == null ? null : connection.getPlayerInfo(id);
      return info != null ? info.getSkin() : account(mc, id);
   }

   private static PlayerSkin account(Minecraft mc, UUID id) {
      Account a = (Account)ACCOUNTS.get(id);
      if (a == null) {
         a = new Account(id, DefaultPlayerSkin.get(id));
         ACCOUNTS.put(id, a);
         if (id.equals(mc.getUser().getProfileId())) {
            a.found = mc.getSkinManager().lookupInsecure(mc.getGameProfile());
         } else if (id.version() != 4) {
            a.answer = Optional.empty();
         }
      }

      if (a.found == null) {
         Optional<GameProfile> answer = a.answer;
         if (answer == null) {
            if (!a.queued) {
               queue(a);
            }

            return a.fallback;
         }

         PlayerSkin fallback = a.fallback;
         a.found = answer.isPresent() ? mc.getSkinManager().lookupInsecure((GameProfile)answer.get()) : () -> fallback;
      }

      return (PlayerSkin)a.found.get();
   }

   private static void queue(Account a) {
      a.queued = true;
      WAITING.addLast(a);

      while(WAITING.size() > 48) {
         Account passed = (Account)WAITING.pollFirst();
         if (passed != null) {
            passed.queued = false;
         }
      }

      if (asker == null) {
         asker = new Thread(PlayerSkins::ask, "cfhgui-skins");
         asker.setDaemon(true);
         asker.start();
      }

   }

   private static void ask() {
      Minecraft mc = Minecraft.getInstance();
      double saved = (double)8.0F;
      long counted = System.currentTimeMillis();

      try {
         while(true) {
            Account a = (Account)WAITING.takeLast();
            long now = System.currentTimeMillis();
            saved = Math.min((double)8.0F, saved + (double)(now - counted) / (double)350.0F);
            counted = now;
            if (saved < (double)1.0F) {
               Thread.sleep((long)Math.ceil(((double)1.0F - saved) * (double)350.0F));
               counted = System.currentTimeMillis();
               saved = (double)1.0F;
            }

            --saved;
            Optional<GameProfile> answer = Optional.empty();

            try {
               ProfileResult result = mc.getMinecraftSessionService().fetchProfile(a.id, false);
               if (result != null) {
                  answer = Optional.of(result.profile());
               }
            } catch (RuntimeException var10) {
            }

            a.answer = answer;
         }
      } catch (InterruptedException var11) {
         Thread.currentThread().interrupt();
      }
   }

   private static final class Account {
      final UUID id;
      final PlayerSkin fallback;
      boolean queued;
      volatile @Nullable Optional<GameProfile> answer;
      @Nullable Supplier<PlayerSkin> found;

      Account(UUID id, PlayerSkin fallback) {
         this.id = id;
         this.fallback = fallback;
      }
   }
}
