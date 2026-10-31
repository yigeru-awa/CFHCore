package com.onehumanawa.cfhcore.content.gui.core.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ScreenLayout {
   public int version = 1;
   public String screen = "";
   public List<String> hidden = new ArrayList();
   public Map<String, Placement> moved = new LinkedHashMap();
   public List<Element> elements = new ArrayList();
   public Background background = null;

   public boolean isEmpty() {
      return this.hidden.isEmpty() && this.moved.isEmpty() && this.elements.isEmpty() && this.background == null;
   }

   public boolean hides(String key) {
      return this.hidden.contains(key);
   }

   public boolean moves(String key) {
      return this.moved.containsKey(key);
   }

   public boolean hasBackground() {
      if (this.background != null && this.background.kind != null) {
         String kind = this.background.kind.trim().toLowerCase(Locale.ROOT);
         return !kind.isEmpty() && !kind.equals("default");
      } else {
         return false;
      }
   }

   public ScreenLayout fillNulls() {
      if (this.hidden == null) {
         this.hidden = new ArrayList();
      }

      this.hidden.removeIf(Objects::isNull);
      if (this.moved == null) {
         this.moved = new LinkedHashMap();
      }

      this.moved.values().removeIf(Objects::isNull);
      if (this.elements == null) {
         this.elements = new ArrayList();
      }

      this.elements.removeIf(Objects::isNull);
      return this;
   }

   public ScreenLayout copy() {
      ScreenLayout l = new ScreenLayout();
      l.version = this.version;
      l.screen = this.screen;
      l.hidden = new ArrayList(this.hidden);
      this.moved.forEach((k, v) -> l.moved.put(k, v.copy()));

      for(Element e : this.elements) {
         l.elements.add(e.copy());
      }

      if (this.background != null) {
         l.background = new Background();
         l.background.kind = this.background.kind;
         l.background.value = this.background.value;
         l.background.dim = this.background.dim;
      }

      return l;
   }

   public static final class Placement {
      public String anchor = "TOP_LEFT";
      public int x;
      public int y;
      public int w;
      public int h;

      public Placement() {
      }

      public Placement(String anchor, int x, int y, int w, int h) {
         this.anchor = anchor;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }

      public Placement copy() {
         return new Placement(this.anchor, this.x, this.y, this.w, this.h);
      }
   }

   public static final class Element {
      public String id = "";
      public String type = "button";
      public Placement place = new Placement();
      public boolean visible = true;
      public boolean locked = false;
      public Map<String, String> props = new LinkedHashMap();
      public List<Action> actions = new ArrayList();

      public Element copy() {
         Element e = new Element();
         e.id = this.id;
         e.type = this.type;
         e.place = this.place.copy();
         e.visible = this.visible;
         e.locked = this.locked;
         e.props = new LinkedHashMap(this.props);

         for(Action a : this.actions) {
            e.actions.add(a.copy());
         }

         return e;
      }
   }

   public static final class Action {
      public String type = "";
      public Map<String, String> args = new LinkedHashMap();

      public Action() {
      }

      public Action(String type, Map<String, String> args) {
         this.type = type;
         this.args = new LinkedHashMap(args);
      }

      public Action copy() {
         return new Action(this.type, this.args);
      }
   }

   public static final class Background {
      public String kind = "default";
      public String value = "";
      public boolean dim = true;
   }
}
