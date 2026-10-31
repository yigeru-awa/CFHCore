package com.onehumanawa.cfhcore.content.gui.core.stage.scene;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import org.joml.Vector3f;

public record Anchor(String name, Vector3f position, float yaw, float pitch, float roll) {
   public Anchor(String name, float x, float y, float z, float yaw) {
      this(name, new Vector3f(x, y, z), yaw, 0.0F, 0.0F);
   }

   public float x() {
      return this.position.x;
   }

   public float y() {
      return this.position.y;
   }

   public float z() {
      return this.position.z;
   }

   public <T extends StageNode> T place(T node) {
      node.at(this.position.x, this.position.y, this.position.z);
      node.rotate(this.yaw, this.pitch, this.roll);
      return node;
   }

   public Anchor offset(float dx, float dy, float dz) {
      return new Anchor(this.name, new Vector3f(this.position.x + dx, this.position.y + dy, this.position.z + dz), this.yaw, this.pitch, this.roll);
   }
}
