package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.io.BufferedReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public final class Waypoints {
   private static final int[] XAERO_COLOURS = new int[]{-16777216, -16777046, -16733696, -16733526, -5636096, -5635926, -22016, -5592406, -11184811, -11184641, -11141291, -11141121, -43691, -43521, -171, -1};

   public static boolean xaeroInstalled() {
      return CFHGuiPlatform.get().isModLoaded("xaerominimap") || CFHGuiPlatform.get().isModLoaded("xaeroworldmap") || CFHGuiPlatform.get().isModLoaded("xaerobetterpvp");
   }

   public static boolean journeyMapInstalled() {
      return CFHGuiPlatform.get().isModLoaded("journeymap");
   }

   public static boolean mapModInstalled() {
      return xaeroInstalled() || journeyMapInstalled();
   }

   public static List<Waypoint> ofWorld(String folder, String name) {
      Path game = CFHGuiPlatform.get().gameDir();
      List<Waypoint> out = new ArrayList();
      xaero(game.resolve("xaero").resolve("minimap").resolve(folder), out);
      if (out.isEmpty()) {
         xaero(game.resolve("XaeroWaypoints").resolve(folder), out);
      }

      Path jm = journeyMapFolder("sp", name);
      if (jm != null) {
         journeyMap(jm.resolve("waypoints"), out);
      }

      return out;
   }

   public static List<Waypoint> ofServer(String address, String name) {
      Path game = CFHGuiPlatform.get().gameDir();
      List<Waypoint> out = new ArrayList();
      String host = address.replace(':', '_');

      for(String root : new String[]{"xaero/minimap", "XaeroWaypoints"}) {
         Path base = game.resolve(root);
         if (Files.isDirectory(base, new LinkOption[0])) {
            try {
               Stream<Path> s = Files.list(base);

               try {
                  Objects.requireNonNull(s);

                  for(Path p : (Iterable<Path>)s::iterator) {
                     String n = p.getFileName().toString();
                     if (n.startsWith("Multiplayer_") && n.substring(12).equalsIgnoreCase(host)) {
                        xaero(p, out);
                     }
                  }
               } catch (Throwable var15) {
                  if (s != null) {
                     try {
                        s.close();
                     } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                     }
                  }

                  throw var15;
               }

               if (s != null) {
                  s.close();
               }
            } catch (Exception var16) {
            }

            if (!out.isEmpty()) {
               break;
            }
         }
      }

      Path jm = journeyMapFolder("mp", name);
      if (jm != null) {
         journeyMap(jm.resolve("waypoints"), out);
      }

      return out;
   }

   public static @Nullable Path journeyMapTilesOfWorld(String name) {
      Path world = journeyMapFolder("sp", name);
      return world == null ? null : journeyMapTiles(world);
   }

   public static @Nullable Path journeyMapTilesOfServer(String name) {
      Path server = journeyMapFolder("mp", name);
      return server == null ? null : journeyMapTiles(server);
   }

   private static @Nullable Path journeyMapFolder(String kind, String name) {
      Path dir = CFHGuiPlatform.get().gameDir().resolve("journeymap").resolve("data").resolve(kind);
      String want = plain(name);
      if (!want.isEmpty() && Files.isDirectory(dir, new LinkOption[0])) {
         Path same = null;
         Path alike = null;

         try {
            Stream<Path> s = Files.list(dir);

            Path var15;
            label101: {
               try {
                  Objects.requireNonNull(s);

                  for(Path p : (Iterable<Path>)s::iterator) {
                     if (Files.isDirectory(p, new LinkOption[0])) {
                        String n = p.getFileName().toString();
                        if (n.equals(name)) {
                           var15 = p;
                           break label101;
                        }

                        String got = plain(n);
                        String decoded = plain(decoded(n));
                        if (same != null || !got.equals(want) && !decoded.equals(want)) {
                           if (alike == null && (got.startsWith(want) || decoded.startsWith(want))) {
                              alike = p;
                           }
                        } else {
                           same = p;
                        }
                     }
                  }
               } catch (Throwable var13) {
                  if (s != null) {
                     try {
                        s.close();
                     } catch (Throwable var12) {
                        var13.addSuppressed(var12);
                     }
                  }

                  throw var13;
               }

               if (s != null) {
                  s.close();
               }

               return same != null ? same : alike;
            }

            if (s != null) {
               s.close();
            }

            return var15;
         } catch (Exception var14) {
            return same != null ? same : alike;
         }
      } else {
         return null;
      }
   }

   private static String plain(String s) {
      StringBuilder b = new StringBuilder(s.length());

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (Character.isLetterOrDigit(c)) {
            b.append(Character.toLowerCase(c));
         }
      }

      return b.toString();
   }

   private static String decoded(String s) {
      try {
         return URLDecoder.decode(s, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException var2) {
         return s;
      }
   }

   private static @Nullable Path journeyMapTiles(Path world) {
      for(String dim : new String[]{"overworld", "DIM0", "minecraft~overworld"}) {
         Path day = world.resolve(dim).resolve("day");
         if (Files.isDirectory(day, new LinkOption[0])) {
            return day;
         }
      }

      return null;
   }

   private static void xaero(Path world, List<Waypoint> out) {
      if (Files.isDirectory(world, new LinkOption[0])) {
         for(String dim : new String[]{"dim%0", "dim%minecraft$overworld", "null"}) {
            Path dir = world.resolve(dim);
            if (Files.isDirectory(dir, new LinkOption[0])) {
               try {
                  Stream<Path> s = Files.list(dir);

                  try {
                     Objects.requireNonNull(s);

                     for(Path file : (Iterable<Path>)s::iterator) {
                        if (file.getFileName().toString().endsWith(".txt")) {
                           xaeroFile(file, out);
                        }
                     }
                  } catch (Throwable var11) {
                     if (s != null) {
                        try {
                           s.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (s != null) {
                     s.close();
                  }
               } catch (Exception var12) {
               }

               if (!out.isEmpty()) {
                  return;
               }
            }
         }

      }
   }

   private static void xaeroFile(Path file, List<Waypoint> out) {
      try {
         BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8);

         String line;
         try {
            while((line = r.readLine()) != null) {
               if (line.startsWith("waypoint:")) {
                  String[] p = line.split(":");
                  if (p.length >= 8) {
                     try {
                        if (!Boolean.parseBoolean(p[7])) {
                           int y = p[4].equals("~") ? 64 : Integer.parseInt(p[4]);
                           int colour = Integer.parseInt(p[6]);
                           out.add(new Waypoint(p[1].replace("§§", ":"), p[2].replace("§§", ":"), Integer.parseInt(p[3]), y, Integer.parseInt(p[5]), XAERO_COLOURS[Math.floorMod(colour, XAERO_COLOURS.length)]));
                        }
                     } catch (NumberFormatException var8) {
                     }
                  }
               }
            }
         } catch (Throwable var9) {
            if (r != null) {
               try {
                  r.close();
               } catch (Throwable var7) {
                  var9.addSuppressed(var7);
               }
            }

            throw var9;
         }

         if (r != null) {
            r.close();
         }
      } catch (Exception var10) {
      }

   }

   private static void journeyMap(Path dir, List<Waypoint> out) {
      if (Files.isDirectory(dir, new LinkOption[0])) {
         try {
            Stream<Path> s = Files.list(dir);

            try {
               Objects.requireNonNull(s);

               for(Path file : (Iterable<Path>)s::iterator) {
                  if (file.getFileName().toString().endsWith(".json")) {
                     try {
                        BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8);

                        label150: {
                           label149: {
                              label148: {
                                 label147: {
                                    try {
                                       JsonElement el = JsonParser.parseReader(r);
                                       if (el.isJsonObject()) {
                                          JsonObject o = el.getAsJsonObject();
                                          if (o.has("enable") && !o.get("enable").getAsBoolean()) {
                                             break label147;
                                          }

                                          if (o.has("enabled") && !o.get("enabled").getAsBoolean()) {
                                             break label148;
                                          }

                                          String name = o.has("name") ? o.get("name").getAsString() : "";
                                          JsonObject pos = o.has("pos") && o.get("pos").isJsonObject() ? o.getAsJsonObject("pos") : o;
                                          if (!pos.has("x") || !pos.has("z")) {
                                             break label149;
                                          }

                                          int x = pos.get("x").getAsInt();
                                          int z = pos.get("z").getAsInt();
                                          int y = pos.has("y") ? pos.get("y").getAsInt() : 64;
                                          int argb = -1;
                                          if (o.has("r") && o.has("g") && o.has("b")) {
                                             argb = -16777216 | (o.get("r").getAsInt() & 255) << 16 | (o.get("g").getAsInt() & 255) << 8 | o.get("b").getAsInt() & 255;
                                          } else if (o.has("color")) {
                                             argb = -16777216 | o.get("color").getAsInt() & 16777215;
                                          }

                                          out.add(new Waypoint(name, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), x, y, z, argb));
                                          break label150;
                                       }
                                    } catch (Throwable var16) {
                                       if (r != null) {
                                          try {
                                             r.close();
                                          } catch (Throwable var15) {
                                             var16.addSuppressed(var15);
                                          }
                                       }

                                       throw var16;
                                    }

                                    if (r != null) {
                                       r.close();
                                    }
                                    continue;
                                 }

                                 if (r != null) {
                                    r.close();
                                 }
                                 continue;
                              }

                              if (r != null) {
                                 r.close();
                              }
                              continue;
                           }

                           if (r != null) {
                              r.close();
                           }
                           continue;
                        }

                        if (r != null) {
                           r.close();
                        }
                     } catch (Exception var17) {
                     }
                  }
               }
            } catch (Throwable var18) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var14) {
                     var18.addSuppressed(var14);
                  }
               }

               throw var18;
            }

            if (s != null) {
               s.close();
            }
         } catch (Exception var19) {
         }

      }
   }

   private Waypoints() {
   }

   public static record Waypoint(String name, String initials, int x, int y, int z, int argb) {
   }
}
