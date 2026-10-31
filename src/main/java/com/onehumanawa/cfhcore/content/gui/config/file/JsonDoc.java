package com.onehumanawa.cfhcore.content.gui.config.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class JsonDoc {
   private static final Gson GSON = (new GsonBuilder()).disableHtmlEscaping().serializeNulls().setLenient().create();
   private JsonElement root;
   private final String indent;
   private final String newline;
   private final boolean hadComments;

   private JsonDoc(JsonElement root, String indent, String newline, boolean hadComments) {
      this.root = root;
      this.indent = indent;
      this.newline = newline;
      this.hadComments = hadComments;
   }

   public static JsonDoc parse(String text) {
      JsonReader r = new JsonReader(new StringReader(text));
      r.setLenient(true);
      JsonElement root = JsonParser.parseReader(r);
      return new JsonDoc((JsonElement)(root == null ? new JsonObject() : root), detectIndent(text), TextFile.lineSeparator(text), text.contains("//") || text.contains("/*"));
   }

   public static JsonDoc empty() {
      return new JsonDoc(new JsonObject(), "  ", "\n", false);
   }

   private static String detectIndent(String text) {
      for(String line : text.split("\\r?\\n")) {
         int i;
         for(i = 0; i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t'); ++i) {
         }

         if (i > 0 && i < line.length() && (line.charAt(i) == '"' || line.charAt(i) == '\'' || Character.isLetter(line.charAt(i)))) {
            return line.substring(0, i);
         }
      }

      return "  ";
   }

   public JsonElement root() {
      return this.root;
   }

   public boolean hadComments() {
      return this.hadComments;
   }

   public String render() {
      StringWriter sw = new StringWriter();
      JsonWriter w = new JsonWriter(sw);
      w.setIndent(this.indent);
      w.setLenient(true);
      GSON.toJson(this.root, w);
      String s = sw.toString();
      if (!this.newline.equals("\n")) {
         s = s.replace("\n", this.newline);
      }

      return s + this.newline;
   }

   public static List<String> tokens(String pointer) {
      List<String> out = new ArrayList();
      if (pointer != null && !pointer.isEmpty() && !pointer.equals("/")) {
         if (pointer.startsWith("/")) {
            for(String t : pointer.substring(1).split("/", -1)) {
               out.add(t.replace("~1", "/").replace("~0", "~"));
            }
         } else {
            for(String t : pointer.split("\\.")) {
               out.add(t);
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static String pointerOf(List<String> tokens) {
      StringBuilder sb = new StringBuilder();

      for(String t : tokens) {
         sb.append('/').append(t.replace("~", "~0").replace("/", "~1"));
      }

      return sb.toString();
   }

   public @Nullable JsonElement get(String pointer) {
      JsonElement cur = this.root;

      for(String t : tokens(pointer)) {
         if (cur == null) {
            return null;
         }

         if (cur.isJsonObject()) {
            cur = cur.getAsJsonObject().get(t);
         } else {
            if (!cur.isJsonArray()) {
               return null;
            }

            try {
               int i = Integer.parseInt(t);
               JsonArray a = cur.getAsJsonArray();
               cur = i >= 0 && i < a.size() ? a.get(i) : null;
            } catch (NumberFormatException var7) {
               return null;
            }
         }
      }

      return cur;
   }

   public boolean set(String pointer, JsonElement value) {
      List<String> toks = tokens(pointer);
      if (toks.isEmpty()) {
         this.root = value;
         return true;
      } else {
         JsonElement parent = this.get(pointerOf(toks.subList(0, toks.size() - 1)));
         String last = (String)toks.get(toks.size() - 1);
         if (parent == null) {
            return false;
         } else if (parent.isJsonObject()) {
            parent.getAsJsonObject().add(last, (JsonElement)(value == null ? JsonNull.INSTANCE : value));
            return true;
         } else {
            if (parent.isJsonArray()) {
               try {
                  int i = Integer.parseInt(last);
                  JsonArray a = parent.getAsJsonArray();
                  if (i >= 0 && i < a.size()) {
                     a.set(i, value);
                     return true;
                  }

                  if (i == a.size()) {
                     a.add(value);
                     return true;
                  }
               } catch (NumberFormatException var8) {
               }
            }

            return false;
         }
      }
   }
}
