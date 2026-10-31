package com.onehumanawa.cfhcore.content.gui.core.stage.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SceneCapture {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

   public static Component capture(String template, String sceneId, @Nullable BlockPos origin) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         IntegratedServer server = mc.getSingleplayerServer();
         if (server == null) {
            return Component.translatable("cfhgui.stage.capture.singleplayer_only");
         } else {
            ResourceLocation templateId = ResourceLocation.tryParse(template.contains(":") ? template : "minecraft:" + template);
            ResourceLocation scene = ResourceLocation.tryParse(sceneId.contains(":") ? sceneId : "cfhgui:" + sceneId);
            if (templateId != null && scene != null) {
               Path nbtFile = server.getWorldPath(LevelResource.GENERATED_DIR).resolve(templateId.getNamespace()).resolve("structures").resolve(templateId.getPath() + ".nbt");
               if (!Files.isRegularFile(nbtFile, new LinkOption[0])) {
                  return Component.translatable("cfhgui.stage.capture.no_template", new Object[]{nbtFile.toString()});
               } else {
                  if (origin == null) {
                     origin = originFromLookedAtStructureBlock(mc);
                  }

                  if (origin == null) {
                     return Component.translatable("cfhgui.stage.capture.no_origin");
                  } else {
                     CompoundTag nbt;
                     try {
                        nbt = NbtIo.readCompressed(nbtFile, NbtAccounter.unlimitedHeap());
                     } catch (Exception e) {
                        return Component.translatable("cfhgui.stage.capture.failed", new Object[]{e.toString()});
                     }

                     Vec3 eye = mc.player.getEyePosition();
                     Vec3 look = mc.player.getViewVector(1.0F);
                     JsonObject json = build(nbt, eye.subtract((double)origin.getX(), (double)origin.getY(), (double)origin.getZ()), look, scene);

                     try {
                        Path dir = SceneLoader.configDir();
                        Files.createDirectories(dir);
                        Path jsonOut = dir.resolve(SceneLoader.configName(scene, "json"));
                        Path nbtOut = dir.resolve(SceneLoader.configName(scene, "nbt"));
                        Files.writeString(jsonOut, GSON.toJson(json));
                        Files.copy(nbtFile, nbtOut, StandardCopyOption.REPLACE_EXISTING);
                        SceneLoader.clear();
                        CFHGui.LOGGER.info("[CFHGui] scene {} captured from {} to {}", new Object[]{scene, templateId, jsonOut});
                        return Component.translatable("cfhgui.stage.capture.done", new Object[]{scene.toString(), json.getAsJsonObject("anchors").size(), jsonOut.toString()});
                     } catch (Exception e) {
                        return Component.translatable("cfhgui.stage.capture.failed", new Object[]{e.toString()});
                     }
                  }
               }
            } else {
               return Component.translatable("cfhgui.stage.capture.bad_id");
            }
         }
      } else {
         return Component.translatable("cfhgui.stage.capture.no_world");
      }
   }

   private static @Nullable BlockPos originFromLookedAtStructureBlock(Minecraft mc) {
      HitResult hit = mc.hitResult;
      if (hit instanceof BlockHitResult bhr) {
         if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(bhr.getBlockPos());
            if (be instanceof StructureBlockEntity) {
               StructureBlockEntity sbe = (StructureBlockEntity)be;
               return bhr.getBlockPos().offset(sbe.getStructurePos());
            }

            return null;
         }
      }

      return null;
   }

   static JsonObject build(CompoundTag nbt, Vec3 eyeRelative, Vec3 look, ResourceLocation scene) {
      JsonObject root = new JsonObject();
      root.addProperty("background", "#161615");
      JsonObject structure = new JsonObject();
      structure.addProperty("file", scene.getPath());
      structure.addProperty("hideAnchorBlocks", true);
      JsonArray hide = new JsonArray();
      JsonObject anchors = new JsonObject();
      collectAnchors(nbt, anchors, hide);
      structure.add("hide", hide);
      root.add("structure", structure);
      JsonObject camera = new JsonObject();
      camera.addProperty("fov", 50);
      Vec3 target = eyeRelative.add(look.scale((double)6.0F));
      JsonObject rest = pose(eyeRelative, target);
      camera.add("pose", rest);
      JsonArray intro = new JsonArray();
      JsonObject line = new JsonObject();
      line.addProperty("type", "line");
      line.addProperty("duration", 1800);
      line.addProperty("ease", "outCubic");
      line.add("from", pose(eyeRelative.subtract(look.scale((double)3.0F)).add((double)0.0F, 0.6, (double)0.0F), target));
      line.add("to", rest);
      intro.add(line);
      camera.add("intro", intro);
      JsonObject idle = new JsonObject();
      idle.addProperty("type", "sway");
      idle.addProperty("period", 14000);
      idle.addProperty("yaw", 2);
      idle.addProperty("pitch", 0.8);
      idle.addProperty("bob", 0.03);
      camera.add("idle", idle);
      root.add("camera", camera);
      JsonObject light = new JsonObject();
      light.add("sun", array(0.35, (double)1.0F, 0.55));
      light.addProperty("ambient", 0.45);
      root.add("light", light);
      root.add("anchors", anchors);
      return root;
   }

   private static void collectAnchors(CompoundTag nbt, JsonObject anchors, JsonArray hide) {
      ListTag palette;
      if (nbt.contains("palettes", 9)) {
         ListTag palettes = nbt.getList("palettes", 9);
         palette = palettes.isEmpty() ? new ListTag() : palettes.getList(0);
      } else {
         palette = nbt.getList("palette", 10);
      }

      ListTag blocks = nbt.getList("blocks", 10);

      for(int i = 0; i < blocks.size(); ++i) {
         CompoundTag b = blocks.getCompound(i);
         if (b.contains("nbt", 10)) {
            CompoundTag be = b.getCompound("nbt");
            if (be.contains("front_text", 10)) {
               ListTag messages = be.getCompound("front_text").getList("messages", 8);
               if (!messages.isEmpty()) {
                  String first = plainText(messages.getString(0)).trim();
                  if (first.startsWith("#") && first.length() >= 2) {
                     String name = first.substring(1).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
                     ListTag pos = b.getList("pos", 3);
                     if (pos.size() == 3) {
                        int x = pos.getInt(0);
                        int y = pos.getInt(1);
                        int z = pos.getInt(2);
                        float yaw = 0.0F;
                        int stateIndex = b.getInt("state");
                        if (stateIndex >= 0 && stateIndex < palette.size()) {
                           try {
                              BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(stateIndex));
                              if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
                                 yaw = RotationSegment.convertToDegrees((Integer)state.getValue(BlockStateProperties.ROTATION_16));
                              } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                                 yaw = ((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)).toYRot();
                              }
                           } catch (Exception var18) {
                           }
                        }

                        JsonObject a = new JsonObject();
                        a.add("pos", array((double)x + (double)0.5F, (double)y, (double)z + (double)0.5F));
                        a.add("rot", array((double)yaw, (double)0.0F, (double)0.0F));
                        anchors.add(name, a);
                        hide.add(array((double)x, (double)y, (double)z));
                     }
                  }
               }
            }
         }
      }

   }

   static String plainText(String json) {
      try {
         JsonElement el = JsonParser.parseString(json);
         if (el.isJsonPrimitive()) {
            return el.getAsString();
         }

         if (el.isJsonObject() && el.getAsJsonObject().has("text")) {
            return el.getAsJsonObject().get("text").getAsString();
         }
      } catch (Exception var2) {
      }

      return json;
   }

   private static JsonObject pose(Vec3 position, Vec3 target) {
      JsonObject o = new JsonObject();
      o.add("position", array(position.x, position.y, position.z));
      o.add("target", array(target.x, target.y, target.z));
      return o;
   }

   private static JsonArray array(double x, double y, double z) {
      JsonArray a = new JsonArray();
      a.add(round(x));
      a.add(round(y));
      a.add(round(z));
      return a;
   }

   private static double round(double v) {
      return (double)Math.round(v * (double)100.0F) / (double)100.0F;
   }

   private SceneCapture() {
   }
}
