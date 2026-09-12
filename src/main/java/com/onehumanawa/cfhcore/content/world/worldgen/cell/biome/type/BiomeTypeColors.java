package com.onehumanawa.cfhcore.content.world.worldgen.cell.biome.type;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public class BiomeTypeColors {
   private static final BiomeTypeColors INSTANCE = new BiomeTypeColors();
   private Map<String, Color> colors = new HashMap<>();

   private BiomeTypeColors() {
      try (InputStream inputStream = BiomeType.class.getResourceAsStream("/biomes.txt")) {
         Properties properties = new Properties();
         properties.load(inputStream);

         for (Entry<?, ?> entry : properties.entrySet()) {
            Color color = Color.decode("#" + entry.getValue().toString());
            this.colors.put(entry.getKey().toString(), color);
         }
      } catch (IOException var8) {
         var8.printStackTrace();
      }
   }

   public Color getColor(String name, Color defaultColor) {
      return this.colors.getOrDefault(name, defaultColor);
   }

   public static BiomeTypeColors getInstance() {
      return INSTANCE;
   }

   public static void main(String[] args) throws Throwable {
      try (FileWriter writer = new FileWriter("biome_colors.properties")) {
         Properties properties = new Properties();

         for (BiomeType type : BiomeType.values()) {
            int r = type.getColor().getRed();
            int g = type.getColor().getGreen();
            int b = type.getColor().getBlue();
            properties.setProperty(type.name(), String.format("%02x%02x%02x", r, g, b));
         }

         properties.store(writer, "CFHWorldGen BiomeType Hex Colors (do not include hash/pound character)");
      }
   }
}
