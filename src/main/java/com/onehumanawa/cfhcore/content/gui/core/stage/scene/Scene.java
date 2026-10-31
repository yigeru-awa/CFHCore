package com.onehumanawa.cfhcore.content.gui.core.stage.scene;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageCamera;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPath;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.CameraPose;
import com.onehumanawa.cfhcore.content.gui.core.stage.anim.IdleMotion;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildCamera;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildSpec;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StructureNode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class Scene {
   private final ResourceLocation id;
   private final Background background;
   private final @Nullable StructureRef structure;
   private final CameraConfig camera;
   private final Light light;
   private final Map<String, Anchor> anchors;
   private final float resolution;
   private final @Nullable BuildSpec build;
   private final @Nullable SceneLook look;
   private final JsonObject json;
   private final Set<String> warned = new HashSet();

   Scene(ResourceLocation id, Background background, @Nullable StructureRef structure, CameraConfig camera, Light light, Map<String, Anchor> anchors, float resolution, @Nullable BuildSpec build, @Nullable SceneLook look, JsonObject json) {
      this.id = id;
      this.background = background;
      this.structure = structure;
      this.camera = camera;
      this.light = light;
      this.anchors = Map.copyOf(anchors);
      this.resolution = resolution;
      this.build = build;
      this.look = look;
      this.json = json;
   }

   public ResourceLocation id() {
      return this.id;
   }

   public Background background() {
      return this.background;
   }

   public @Nullable StructureRef structure() {
      return this.structure;
   }

   public CameraConfig camera() {
      return this.camera;
   }

   public Light light() {
      return this.light;
   }

   public Map<String, Anchor> anchors() {
      return this.anchors;
   }

   public float resolution() {
      return this.resolution;
   }

   public @Nullable BuildSpec build() {
      return this.build;
   }

   public @Nullable SceneLook look() {
      return this.look;
   }

   public @Nullable JsonObject section(String name) {
      return this.json.has(name) && this.json.get(name).isJsonObject() ? this.json.getAsJsonObject(name).deepCopy() : null;
   }

   public Optional<Anchor> anchor(String name) {
      return Optional.ofNullable((Anchor)this.anchors.get(name));
   }

   public Anchor anchorOr(String name, float x, float y, float z, float yaw) {
      Anchor a = (Anchor)this.anchors.get(name);
      if (a != null) {
         return a;
      } else {
         if (this.warned.add(name)) {
            CFHGui.LOGGER.warn("[CFHGui] scene {} has no anchor '{}', using the fallback at {},{},{}", new Object[]{this.id, name, x, y, z});
         }

         return new Anchor(name, x, y, z, yaw);
      }
   }

   public @Nullable StructureNode apply(Stage stage) {
      if (this.look != null) {
         this.look.addSky(stage);
      }

      if (this.background.transparent) {
         stage.background(0);
      } else if (this.background.top == this.background.bottom) {
         stage.background(this.background.top);
      } else {
         stage.gradient(this.background.top, this.background.bottom);
      }

      stage.lighting().sun(this.light.sun.x, this.light.sun.y, this.light.sun.z).ambient(this.light.ambient).light(this.light.blockLight, this.light.skyLight).brightness(this.light.brightness);
      stage.fog(this.light.fog, this.light.fogStart, this.light.fogEnd, this.light.fogColor);
      stage.resolutionScale(this.resolution);
      StageCamera cam = stage.camera();
      cam.pose().set(this.camera.rest);
      cam.clip(this.camera.near, this.camera.far);
      cam.idle(this.camera.idle);
      cam.timeline().clear();

      for(CameraPath p : this.camera.intro) {
         cam.timeline().add(p);
      }

      if (!this.camera.intro.isEmpty()) {
         cam.timeline().play();
      }

      StructureNode node = null;
      if (this.structure != null && this.structure.template != null) {
         node = (StructureNode)stage.add(new StructureNode(stage.level(), this.structure.template, this.structure.hideAnchorBlocks ? this.structure.hidden : null));
         if (this.structure.centered) {
            node.centered();
         }

         node.at(this.structure.offset.x, this.structure.offset.y, this.structure.offset.z);
         node.named("scene:" + this.id.getPath());
      }

      if (this.build != null && this.build.available()) {
         stage.add(new BuildNode(stage.level(), this.build, BuildCamera.of(this.camera)));
      }

      this.applyLook(stage);
      return node;
   }

   public void applyLook(Stage stage) {
      if (this.look != null) {
         this.look.applyLight(stage);
      }

   }

   public static record Background(int top, int bottom, boolean transparent) {
   }

   public static record Light(Vector3f sun, float ambient, int blockLight, int skyLight, float brightness, boolean fog, float fogStart, float fogEnd, int fogColor) {
   }

   public static record CameraConfig(CameraPose rest, List<CameraPath> intro, IdleMotion idle, float near, float far) {
   }

   public static record StructureRef(@Nullable CompoundTag template, Vector3f offset, boolean centered, boolean hideAnchorBlocks, Set<BlockPos> hidden) {
   }
}
