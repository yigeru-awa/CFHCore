package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

final class LookFacts {
   static final int MAX_EFFECTS = 6;
   static final int NAME_ROOM = 110;
   private static final double BLOCKS_A_SECOND = 42.15779058762798;
   static final Line NAME = new Line();
   static final Line KIND = new Line();
   static final Line HEALTH = new Line();
   static final Line ABSORPTION = new Line();
   static final Line ARMOR = new Line();
   static final Line OWNER = new Line();
   static final Line VILLAGER = new Line();
   static final Line SPEED = new Line();
   static final Line JUMP = new Line();
   static final Line MORE = new Line();
   static final Line L_BABY = new Line();
   static final Line L_OWNER = new Line();
   static final Line L_TAMED = new Line();
   static float health;
   static float absorption;
   static int healthRoom;
   static boolean baby;
   static boolean tamed;
   static boolean mount;
   static int effects;
   static final TextureAtlasSprite[] EFFECT = new TextureAtlasSprite[6];
   static int revision;
   private static int ticks;
   private static int seenHealth = -1;
   private static int seenMost = -1;
   private static int seenExtra = -1;
   private static int seenArmor = -1;
   private static int seenLevel = -1;
   private static int seenSpeed = -1;
   private static int seenJump = -1;
   private static int seenEffects;
   private static int effectsSign;
   private static boolean ownerKnown;
   private static @Nullable UUID seenOwner;
   private static @Nullable VillagerProfession seenProfession;
   private static @Nullable String language;

   private LookFacts() {
   }

   static void forget() {
      ticks = 0;
      seenJump = -1;
      seenSpeed = -1;
      seenLevel = -1;
      seenArmor = -1;
      seenExtra = -1;
      seenMost = -1;
      seenHealth = -1;
      effectsSign = 0;
      seenEffects = 0;
      seenOwner = null;
      seenProfession = null;
      ownerKnown = false;
      language = null;
      mount = false;
      tamed = false;
      baby = false;
      effects = 0;

      for(int i = 0; i < 6; ++i) {
         EFFECT[i] = null;
      }

      NAME.clear();
      KIND.clear();
      HEALTH.clear();
      ABSORPTION.clear();
      ARMOR.clear();
      OWNER.clear();
      VILLAGER.clear();
      SPEED.clear();
      JUMP.clear();
      MORE.clear();
      ++revision;
   }

   private static void labels(Minecraft mc, Font font) {
      String selected = mc.getLanguageManager().getSelected();
      if (!selected.equals(language)) {
         forget();
         language = selected;
         L_BABY.clear();
         L_OWNER.clear();
         L_TAMED.clear();
         L_BABY.set(I18n.get("cfhgui_lookat.baby", new Object[0]), font);
         L_OWNER.set(I18n.get("cfhgui_lookat.owner", new Object[0]), font);
         L_TAMED.set(I18n.get("cfhgui_lookat.tamed", new Object[0]), font);
      }
   }

   static void read(Minecraft mc, Font font, LivingEntity mob) {
      labels(mc, font);
      boolean changed = ticks++ % 10 == 0 && name(font, mob);
      changed |= vitals(font, mob);
      changed |= owner(mc, font, mob);
      changed |= villager(font, mob);
      changed |= mount(font, mob);
      changed |= effects(mc, font, mob);
      if (changed) {
         ++revision;
      }

   }

   private static boolean name(Font font, LivingEntity mob) {
      Component shown = mob.getDisplayName();
      boolean changed = NAME.set(shown, shown.getString(), font, 110);
      boolean named = mob.hasCustomName() || mob instanceof Player;
      changed |= named ? KIND.set(kind(mob), font) : KIND.clear();
      return changed;
   }

   private static String kind(LivingEntity mob) {
      if (mob instanceof Villager villager) {
         return profession(villager.getVillagerData().getProfession());
      } else {
         return mob.getType().getDescription().getString();
      }
   }

   private static boolean vitals(Font font, LivingEntity mob) {
      float points = Math.max(0.0F, mob.getHealth());
      float most = Math.max(0.0F, mob.getMaxHealth());
      float extra = Math.max(0.0F, mob.getAbsorptionAmount());
      float whole = Math.max(most, points + extra);
      health = whole <= 0.0F ? 0.0F : Mth.clamp(points / whole, 0.0F, 1.0F);
      absorption = whole <= 0.0F ? 0.0F : Mth.clamp(extra / whole, 0.0F, 1.0F);
      boolean changed = false;
      int shown = Mth.ceil(points);
      int shownMost = Mth.ceil(most);
      int shownExtra = Mth.ceil(extra);
      if (shown != seenHealth || shownMost != seenMost) {
         if (shownMost != seenMost) {
            String longest = Integer.toString(Math.max(shown, shownMost));
            healthRoom = font.width(I18n.get("cfhgui_lookat.health", new Object[]{longest, longest}));
         }

         seenHealth = shown;
         seenMost = shownMost;
         changed = HEALTH.set(I18n.get("cfhgui_lookat.health", new Object[]{Integer.toString(shown), Integer.toString(shownMost)}), font);
      }

      if (shownExtra != seenExtra) {
         seenExtra = shownExtra;
         changed |= shownExtra > 0 ? ABSORPTION.set(I18n.get("cfhgui_lookat.absorption", new Object[]{Integer.toString(shownExtra)}), font) : ABSORPTION.clear();
      }

      int armor = mob.getArmorValue();
      if (armor != seenArmor) {
         seenArmor = armor;
         changed |= armor > 0 ? ARMOR.set(Integer.toString(armor), font) : ARMOR.clear();
      }

      boolean young = mob.isBaby();
      if (young != baby) {
         baby = young;
         changed = true;
      }

      return changed;
   }

   private static boolean owner(Minecraft mc, Font font, LivingEntity mob) {
      UUID var10000;
      if (mob instanceof OwnableEntity owned) {
         var10000 = owned.getOwnerUUID();
      } else {
         var10000 = null;
      }

      UUID id;
      boolean var12;
      label77: {
         id = var10000;
         if (id == null) {
            label74: {
               if (mob instanceof TamableAnimal) {
                  TamableAnimal pet = (TamableAnimal)mob;
                  if (pet.isTame()) {
                     break label74;
                  }
               }

               if (mob instanceof AbstractHorse) {
                  AbstractHorse horse = (AbstractHorse)mob;
                  if (horse.isTamed()) {
                     break label74;
                  }
               }

               var12 = false;
               break label77;
            }
         }

         var12 = true;
      }

      boolean tame = var12;
      boolean again = id != null && !ownerKnown && ticks % 20 == 1;
      if (Objects.equals(id, seenOwner) && tame == tamed && !again) {
         return false;
      } else {
         boolean was = tamed;
         seenOwner = id;
         tamed = tame;
         String name = id == null ? null : playerName(mc, id);
         ownerKnown = name != null;
         boolean words = name != null ? OWNER.set(name, font) : OWNER.clear();
         return words || tame != was;
      }
   }

   private static @Nullable String playerName(Minecraft mc, UUID id) {
      ClientPacketListener connection = mc.getConnection();
      PlayerInfo listed = connection == null ? null : connection.getPlayerInfo(id);
      if (listed != null) {
         return listed.getProfile().getName();
      } else {
         Player near = mc.level == null ? null : mc.level.getPlayerByUUID(id);
         return near == null ? null : near.getGameProfile().getName();
      }
   }

   private static boolean villager(Font font, LivingEntity mob) {
      if (mob instanceof VillagerDataHolder holder) {
         VillagerData data = holder.getVillagerData();
         VillagerProfession profession = data.getProfession();
         int level = data.getLevel();
         if (profession == seenProfession && level == seenLevel) {
            return false;
         } else {
            seenProfession = profession;
            seenLevel = level;
            if (profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT) {
               String key = "merchant.level." + level;
               String rank = I18n.exists(key) ? I18n.get(key, new Object[0]) : I18n.get("cfhgui_lookat.villager.level", new Object[]{Integer.toString(level)});
               return VILLAGER.set(mob instanceof Villager ? rank : I18n.get("cfhgui_lookat.villager.both", new Object[]{profession(profession), rank}), font);
            } else {
               return VILLAGER.clear();
            }
         }
      } else {
         return VILLAGER.clear();
      }
   }

   private static String profession(VillagerProfession profession) {
      ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      String stem = "entity.minecraft.villager.";
      if (!"minecraft".equals(id.getNamespace())) {
         String var10000 = id.getNamespace();
         if (I18n.exists("entity.minecraft.villager." + var10000 + "." + id.getPath())) {
            return I18n.get("entity.minecraft.villager." + id.getNamespace() + "." + id.getPath(), new Object[0]);
         }
      }

      if (I18n.exists("entity.minecraft.villager." + id.getPath())) {
         return I18n.get("entity.minecraft.villager." + id.getPath(), new Object[0]);
      } else {
         String path = id.getPath();
         StringBuilder words = new StringBuilder();
         boolean up = true;

         for(int i = path.lastIndexOf(47) + 1; i < path.length(); ++i) {
            char c = path.charAt(i);
            if (c == '_') {
               words.append(' ');
               up = true;
            } else {
               words.append(up ? Character.toUpperCase(c) : c);
               up = false;
            }
         }

         return words.toString();
      }
   }

   private static boolean mount(Font font, LivingEntity mob) {
      boolean steed = horse(mob) && mob.getAttribute(Attributes.MOVEMENT_SPEED) != null && mob.getAttribute(Attributes.JUMP_STRENGTH) != null;
      if (!steed) {
         if (!mount) {
            return false;
         } else {
            mount = false;
            seenJump = -1;
            seenSpeed = -1;
            SPEED.clear();
            JUMP.clear();
            return true;
         }
      } else {
         int speed = (int)Math.round(mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 42.15779058762798 * (double)10.0F);
         int jump = (int)Math.round(jumpHeight(mob.getAttributeBaseValue(Attributes.JUMP_STRENGTH)) * (double)10.0F);
         if (mount && speed == seenSpeed && jump == seenJump) {
            return false;
         } else {
            mount = true;
            seenSpeed = speed;
            seenJump = jump;
            SPEED.set(I18n.get("cfhgui_lookat.speed", new Object[]{tenths(speed)}), font);
            JUMP.set(I18n.get("cfhgui_lookat.jump", new Object[]{tenths(jump)}), font);
            return true;
         }
      }
   }

   static boolean horse(LivingEntity mob) {
      return mob instanceof AbstractHorse && !(mob instanceof Llama) && !(mob instanceof Camel);
   }

   static double jumpHeight(double strength) {
      double height = (double)0.0F;
      double speed = strength;

      for(int tick = 0; tick < 200 && speed > (double)0.0F; ++tick) {
         height += speed;
         speed = (speed - 0.08) * 0.98 * 0.98;
      }

      return height;
   }

   private static String tenths(int tenths) {
      return tenths / 10 + "." + Math.abs(tenths % 10);
   }

   private static boolean effects(Minecraft mc, Font font, LivingEntity mob) {
      Collection<MobEffectInstance> active = mob.getActiveEffects();
      int count = 0;
      int sign = 0;
      if (!active.isEmpty()) {
         for(MobEffectInstance effect : active) {
            if (running(effect)) {
               ++count;
               sign = sign * 31 + System.identityHashCode(effect.getEffect());
            }
         }
      }

      if (count == seenEffects && sign == effectsSign) {
         return false;
      } else {
         seenEffects = count;
         effectsSign = sign;
         effects = 0;
         if (count > 0) {
            for(MobEffectInstance effect : active) {
               if (running(effect)) {
                  if (effects == 6) {
                     break;
                  }

                  EFFECT[effects++] = mc.getMobEffectTextures().get(effect.getEffect());
               }
            }
         }

         for(int i = effects; i < 6; ++i) {
            EFFECT[i] = null;
         }

         if (count > effects) {
            MORE.set(I18n.get("cfhgui_lookat.more", new Object[]{Integer.toString(count - effects)}), font);
         } else {
            MORE.clear();
         }

         return true;
      }
   }

   private static boolean running(MobEffectInstance effect) {
      return effect.showIcon() && (effect.isInfiniteDuration() || effect.getDuration() > 0);
   }
}
