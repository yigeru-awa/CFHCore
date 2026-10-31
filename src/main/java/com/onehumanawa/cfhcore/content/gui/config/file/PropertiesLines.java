package com.onehumanawa.cfhcore.content.gui.config.file;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class PropertiesLines {
   private static final Pattern KEY_VALUE = Pattern.compile("^(\\s*)((?:\\\\.|[^=:\\s])+)(\\s*[=:]\\s*|\\s+)(.*)$");
   private static final Pattern VALID_VALUES = Pattern.compile("Valid values are\\s+(.*)", 2);
   private static final Pattern QUOTED = Pattern.compile("'([^']*)'");
   private final List<String> lines;
   private final String newline;
   private final List<Entry> entries = new ArrayList();

   private PropertiesLines(List<String> lines, String newline) {
      this.lines = lines;
      this.newline = newline;
   }

   public static PropertiesLines parse(String text) {
      PropertiesLines p = new PropertiesLines(new ArrayList(List.of(text.split("\\r?\\n", -1))), TextFile.lineSeparator(text));
      p.scan();
      return p;
   }

   public List<Entry> entries() {
      return this.entries;
   }

   public Optional<Entry> find(String key) {
      for(Entry e : this.entries) {
         if (e.key.equals(key)) {
            return Optional.of(e);
         }
      }

      return Optional.empty();
   }

   public String render() {
      return String.join(this.newline, this.lines);
   }

   private void scan() {
      this.entries.clear();
      List<String> pending = new ArrayList();

      for(int i = 0; i < this.lines.size(); ++i) {
         String line = (String)this.lines.get(i);
         String trimmed = line.trim();
         if (trimmed.isEmpty()) {
            pending = new ArrayList();
         } else if (!trimmed.startsWith("#") && !trimmed.startsWith("!")) {
            Matcher m = KEY_VALUE.matcher(line);
            if (!m.matches()) {
               pending = new ArrayList();
            } else {
               Entry e = new Entry(m.group(2).replace("\\ ", " ").replace("\\:", ":").replace("\\=", "="), pending);
               String var10001 = m.group(1);
               e.prefix = var10001 + m.group(2) + m.group(3);
               e.value = m.group(4);
               e.line = i;

               for(String c : pending) {
                  Matcher vm = VALID_VALUES.matcher(c);
                  if (vm.find()) {
                     List<String> vals = new ArrayList();
                     Matcher q = QUOTED.matcher(vm.group(1));

                     while(q.find()) {
                        vals.add(q.group(1));
                     }

                     if (!vals.isEmpty()) {
                        e.allowed = vals;
                     }
                  }
               }

               this.entries.add(e);
               pending = new ArrayList();
            }
         } else {
            String c = trimmed.substring(1);
            if (c.startsWith(" ")) {
               c = c.substring(1);
            }

            pending.add(c);
         }
      }

   }

   public boolean set(String key, String value) {
      Entry e = (Entry)this.find(key).orElse(null);
      if (e == null) {
         return false;
      } else {
         e.value = value == null ? "" : value;
         this.lines.set(e.line, e.prefix + e.value);
         return true;
      }
   }

   public static final class Entry {
      public final String key;
      public final List<String> comments;
      public String value;
      public String prefix;
      public int line;
      public @Nullable List<String> allowed;

      Entry(String key, List<String> comments) {
         this.key = key;
         this.comments = comments;
      }

      public List<String> description() {
         List<String> out = new ArrayList();

         for(String c : this.comments) {
            if (!PropertiesLines.VALID_VALUES.matcher(c).find()) {
               out.add(c);
            }
         }

         return out;
      }
   }
}
