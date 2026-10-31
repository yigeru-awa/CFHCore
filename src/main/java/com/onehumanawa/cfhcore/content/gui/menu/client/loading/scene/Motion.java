package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.util.ArrayList;
import java.util.List;

final class Motion {
   float time;
   float memory = -1.0F;
   float overall;
   float needle;
   float lump;
   float beltA;
   float beltB;
   float pressDown;
   float stroke = -1.0F;
   float landed = 1.0F;
   final List<Item> items = new ArrayList();
   final List<Spark> sparks = new ArrayList();

   static final class Item {
      float s;
      boolean pressed;
      float leaving = -1.0F;
      int id;
   }

   static final class Spark {
      float x;
      float y;
      float z;
      float vx;
      float vy;
      float vz;
      float life;
      float span;
      float size;
      int colour;
      boolean smoke;
   }
}
