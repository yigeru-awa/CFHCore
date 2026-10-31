package com.onehumanawa.cfhcore.content.gui.core.perm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class Perms {
   private static volatile @Nullable Resolver resolver;
   private static final Map<String, Node> NODES = new ConcurrentHashMap();
   private static final Set<String> DECLARED = ConcurrentHashMap.newKeySet();

   public static void install(@Nullable Resolver r) {
      resolver = r;
   }

   public static boolean installed() {
      return resolver != null;
   }

   public static Node declare(String id, int defaultLevel) {
      return put(new Node(id, clampLevel(defaultLevel), false, 0));
   }

   public static Node declareNumber(String id, int defaultNumber) {
      return put(new Node(id, 0, true, defaultNumber));
   }

   private static synchronized Node put(Node node) {
      Node old = (Node)NODES.get(node.id());
      if (old != null && DECLARED.contains(node.id())) {
         return old;
      } else {
         NODES.put(node.id(), node);
         DECLARED.add(node.id());
         return node;
      }
   }

   public static List<Node> declared() {
      List<Node> out = new ArrayList(NODES.values());
      out.sort(Comparator.comparing(Node::id));
      return out;
   }

   public static @Nullable Node node(String id) {
      return (Node)NODES.get(id);
   }

   public static boolean isDeclared(String id) {
      return DECLARED.contains(id);
   }

   private static int clampLevel(int level) {
      return Math.max(0, Math.min(4, level));
   }

   public static boolean has(ServerPlayer player, String node, int fallbackLevel) {
      if (!NODES.containsKey(node)) {
         NODES.putIfAbsent(node, new Node(node, clampLevel(fallbackLevel), false, 0));
      }

      Resolver r = resolver;
      if (r != null) {
         Boolean b = r.has(player, node);
         if (b != null) {
            return b;
         }
      }

      return fallbackLevel <= 0 || player.hasPermissions(fallbackLevel);
   }

   public static boolean has(CommandSourceStack source, String node, int fallbackLevel) {
      ServerPlayer p = source.getPlayer();
      if (p != null) {
         return has(p, node, fallbackLevel);
      } else {
         return fallbackLevel <= 0 || source.hasPermission(fallbackLevel);
      }
   }

   public static boolean has(ServerPlayer player, Node node) {
      return has(player, node.id(), node.defaultLevel());
   }

   public static boolean has(CommandSourceStack source, Node node) {
      return has(source, node.id(), node.defaultLevel());
   }

   public static int number(ServerPlayer player, String node, int fallback) {
      if (!NODES.containsKey(node)) {
         NODES.putIfAbsent(node, new Node(node, 0, true, fallback));
      }

      Resolver r = resolver;
      if (r != null) {
         Integer n = r.number(player, node);
         if (n != null) {
            return n;
         }
      }

      return fallback;
   }

   public static int number(ServerPlayer player, Node node) {
      return number(player, node.id(), node.defaultNumber());
   }

   private Perms() {
   }

   public interface Resolver {
      @Nullable Boolean has(ServerPlayer var1, String var2);

      default @Nullable Integer number(ServerPlayer player, String node) {
         return null;
      }

      default @Nullable Boolean hasOffline(UUID player, String node) {
         return null;
      }
   }

   public static record Node(String id, int defaultLevel, boolean number, int defaultNumber) {
      public String module() {
         int dot = this.id.indexOf(46);
         return dot < 0 ? this.id : this.id.substring(0, dot);
      }

      public String path() {
         int dot = this.id.indexOf(46);
         return dot < 0 ? "" : this.id.substring(dot + 1);
      }

      public String descriptionKey() {
         String var10000 = this.module();
         return var10000 + ".perm." + this.path();
      }
   }
}
