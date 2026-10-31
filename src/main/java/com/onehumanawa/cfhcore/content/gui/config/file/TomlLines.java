package com.onehumanawa.cfhcore.content.gui.config.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class TomlLines {
   private static final Pattern KEY_VALUE = Pattern.compile("^(\\s*)((?:\"[^\"]*\"|'[^']*'|[A-Za-z0-9_\\-.]+))(\\s*=\\s*)(.*)$");
   private static final Pattern DEFAULT_META = Pattern.compile("^Default:\\s*(.*)$", 2);
   private static final Pattern RANGE_META = Pattern.compile("^Range:\\s*(.*)$", 2);
   private static final Pattern ALLOWED_META = Pattern.compile("^Allowed Values:\\s*(.*)$", 2);
   private static final Pattern INT = Pattern.compile("^[+-]?\\d[\\d_]*$");
   private static final Pattern FLOAT = Pattern.compile("^[+-]?(?:\\d[\\d_]*\\.\\d*(?:[eE][+-]?\\d+)?|\\d[\\d_]*[eE][+-]?\\d+|\\.\\d+(?:[eE][+-]?\\d+)?|inf|nan)$");
   private final List<String> lines;
   private final String newline;
   private final List<Entry> entries = new ArrayList();
   private final List<Section> sections = new ArrayList();

   private TomlLines(List<String> lines, String newline) {
      this.lines = lines;
      this.newline = newline;
   }

   public static TomlLines parse(String text) {
      String nl = TextFile.lineSeparator(text);
      List<String> lines = new ArrayList(List.of(text.split("\\r?\\n", -1)));
      TomlLines t = new TomlLines(lines, nl);
      t.scan();
      return t;
   }

   public List<Entry> entries() {
      return this.entries;
   }

   public List<Section> sections() {
      return this.sections;
   }

   public Optional<Entry> find(String path) {
      for(Entry e : this.entries) {
         if (e.path().equals(path)) {
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
      this.sections.clear();
      String section = "";
      List<String> pending = new ArrayList();
      Map<String, Integer> arrayTables = new HashMap();

      for(int i = 0; i < this.lines.size(); ++i) {
         String line = (String)this.lines.get(i);
         String trimmed = line.trim();
         if (trimmed.isEmpty()) {
            pending = new ArrayList();
         } else if (trimmed.startsWith("#")) {
            pending.add(stripComment(trimmed));
         } else if (trimmed.startsWith("[")) {
            boolean array = trimmed.startsWith("[[");
            int close = trimmed.indexOf(array ? "]]" : "]");
            if (close > 0) {
               String name = trimmed.substring(array ? 2 : 1, close).trim();
               name = unquoteKey(name);
               if (array) {
                  int n = (Integer)arrayTables.merge(name, 1, Integer::sum) - 1;
                  name = n == 0 ? name : name + "[" + n + "]";
               }

               section = name;
               this.sections.add(new Section(name, pending, i));
            }

            pending = new ArrayList();
         } else {
            Matcher m = KEY_VALUE.matcher(line);
            if (!m.matches()) {
               pending = new ArrayList();
            } else {
               Entry e = new Entry(section, unquoteKey(m.group(2)), pending);
               String var10001 = m.group(1);
               e.prefix = var10001 + m.group(2) + m.group(3);
               e.line = i;
               int[] end = new int[]{i};
               String[] parts = this.readValue(m.group(4), i, end);
               e.endLine = end[0];
               e.raw = parts[0];
               e.suffix = parts[1];
               e.value = parseValue(e.raw);
               applyMeta(e);
               this.entries.add(e);
               pending = new ArrayList();
               i = e.endLine;
            }
         }
      }

   }

   private static String stripComment(String trimmed) {
      String s = trimmed.substring(1);
      if (s.startsWith(" ")) {
         s = s.substring(1);
      }

      return s;
   }

   private static String unquoteKey(String k) {
      String s = k.trim();
      return s.length() < 2 || (!s.startsWith("\"") || !s.endsWith("\"")) && (!s.startsWith("'") || !s.endsWith("'")) ? s : s.substring(1, s.length() - 1);
   }

   private String[] readValue(String first, int startLine, int[] endLineOut) {
      String text = first;
      int lineIdx = startLine;
      String t = first.trim();
      if (!t.startsWith("\"\"\"") && !t.startsWith("'''")) {
         if (!t.startsWith("[") && !t.startsWith("{")) {
            endLineOut[0] = startLine;
            int end = scalarEnd(first);
            return new String[]{first.substring(0, end).trim(), first.substring(end)};
         } else {
            int end;
            for(end = balancedEnd(first); end < 0 && lineIdx + 1 < this.lines.size(); end = balancedEnd(text)) {
               ++lineIdx;
               text = text + "\n" + (String)this.lines.get(lineIdx);
            }

            endLineOut[0] = lineIdx;
            return end < 0 ? new String[]{text, ""} : new String[]{text.substring(0, end + 1), text.substring(end + 1)};
         }
      } else {
         String delim = t.substring(0, 3);

         int close;
         for(close = first.indexOf(delim, first.indexOf(delim) + 3); close < 0 && lineIdx + 1 < this.lines.size(); close = text.indexOf(delim, text.indexOf(delim) + 3)) {
            ++lineIdx;
            text = text + "\n" + (String)this.lines.get(lineIdx);
         }

         endLineOut[0] = lineIdx;
         return close < 0 ? new String[]{text, ""} : new String[]{text.substring(0, close + 3), text.substring(close + 3)};
      }
   }

   private static int balancedEnd(String s) {
      int depth = 0;
      char quote = 0;

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (quote != 0) {
            if (c == '\\' && quote == '"') {
               ++i;
            } else if (c == quote) {
               quote = 0;
            }
         } else if (c != '"' && c != '\'') {
            if (c == '#') {
               int nl = s.indexOf(10, i);
               if (nl < 0) {
                  return -1;
               }

               i = nl;
            } else if (c != '[' && c != '{') {
               if (c == ']' || c == '}') {
                  --depth;
                  if (depth == 0) {
                     return i;
                  }
               }
            } else {
               ++depth;
            }
         } else {
            quote = c;
         }
      }

      return -1;
   }

   private static int scalarEnd(String s) {
      char quote = 0;

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (quote != 0) {
            if (c == '\\' && quote == '"') {
               ++i;
            } else if (c == quote) {
               quote = 0;
            }
         } else if (c != '"' && c != '\'') {
            if (c == '#') {
               return i;
            }
         } else {
            quote = c;
         }
      }

      return s.length();
   }

   public static Object parseValue(String rawIn) {
      String raw = rawIn.trim();
      if (raw.isEmpty()) {
         return new Raw("");
      } else if (raw.equals("true")) {
         return Boolean.TRUE;
      } else if (raw.equals("false")) {
         return Boolean.FALSE;
      } else if (raw.startsWith("\"\"\"") && raw.endsWith("\"\"\"") && raw.length() >= 6) {
         return unescape(raw.substring(3, raw.length() - 3).replaceFirst("^\\r?\\n", ""));
      } else if (raw.startsWith("'''") && raw.endsWith("'''") && raw.length() >= 6) {
         return raw.substring(3, raw.length() - 3).replaceFirst("^\\r?\\n", "");
      } else if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
         return unescape(raw.substring(1, raw.length() - 1));
      } else if (raw.startsWith("'") && raw.endsWith("'") && raw.length() >= 2) {
         return raw.substring(1, raw.length() - 1);
      } else if (INT.matcher(raw).matches()) {
         try {
            return Long.parseLong(raw.replace("_", "").replace("+", ""));
         } catch (NumberFormatException var7) {
            return new Raw(raw);
         }
      } else if (FLOAT.matcher(raw).matches()) {
         try {
            return Double.parseDouble(raw.replace("_", ""));
         } catch (NumberFormatException var8) {
            return new Raw(raw);
         }
      } else if (raw.startsWith("[") && raw.endsWith("]")) {
         List<Object> out = new ArrayList();

         for(String el : splitTopLevel(raw.substring(1, raw.length() - 1))) {
            String e = el.trim();
            if (!e.isEmpty()) {
               Object v = parseValue(e);
               if (v instanceof List) {
                  return new Raw(raw);
               }

               out.add(v);
            }
         }

         return out;
      } else {
         return new Raw(raw);
      }
   }

   private static List<String> splitTopLevel(String s) {
      List<String> out = new ArrayList();
      int depth = 0;
      char quote = 0;
      StringBuilder cur = new StringBuilder();

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         if (quote != 0) {
            cur.append(c);
            if (c == '\\' && quote == '"' && i + 1 < s.length()) {
               ++i;
               cur.append(s.charAt(i));
            } else if (c == quote) {
               quote = 0;
            }
         } else if (c == '#') {
            int nl = s.indexOf(10, i);
            if (nl < 0) {
               break;
            }

            i = nl;
         } else if (c != '"' && c != '\'') {
            if (c == '[' || c == '{') {
               ++depth;
            }

            if (c == ']' || c == '}') {
               --depth;
            }

            if (c == ',' && depth == 0) {
               out.add(cur.toString());
               cur.setLength(0);
            } else {
               cur.append(c);
            }
         } else {
            quote = c;
            cur.append(c);
         }
      }

      if (!cur.toString().trim().isEmpty()) {
         out.add(cur.toString());
      }

      return out;
   }

   public static String unescape(String s) {
      if (s.indexOf(92) < 0) {
         return s;
      } else {
         StringBuilder sb = new StringBuilder(s.length());

         for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
               ++i;
               char n = s.charAt(i);
               switch (n) {
                  case '"':
                     sb.append('"');
                     break;
                  case '\\':
                     sb.append('\\');
                     break;
                  case 'n':
                     sb.append('\n');
                     break;
                  case 'r':
                     sb.append('\r');
                     break;
                  case 't':
                     sb.append('\t');
                     break;
                  case 'u':
                     if (i + 4 < s.length()) {
                        sb.append((char)Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                     }
                     break;
                  default:
                     sb.append('\\').append(n);
               }
            } else {
               sb.append(c);
            }
         }

         return sb.toString();
      }
   }

   public static String escape(String s) {
      StringBuilder sb = (new StringBuilder(s.length() + 2)).append('"');

      for(int i = 0; i < s.length(); ++i) {
         char c = s.charAt(i);
         switch (c) {
            case '\t':
               sb.append("\\t");
               break;
            case '\n':
               sb.append("\\n");
               break;
            case '\r':
               sb.append("\\r");
               break;
            case '"':
               sb.append("\\\"");
               break;
            case '\\':
               sb.append("\\\\");
               break;
            default:
               if (c < ' ') {
                  sb.append(String.format(Locale.ROOT, "\\u%04X", Integer.valueOf(c)));
               } else {
                  sb.append(c);
               }
         }
      }

      return sb.append('"').toString();
   }

   public static String format(Object value, @Nullable Object like) {
      if (value == null) {
         return "\"\"";
      } else if (value instanceof Raw) {
         Raw r = (Raw)value;
         return r.text();
      } else if (value instanceof Boolean) {
         Boolean b = (Boolean)value;
         return b.toString();
      } else if (value instanceof Number) {
         Number n = (Number)value;
         boolean wantDouble = like instanceof Double || like == null && n instanceof Double && n.doubleValue() != Math.rint(n.doubleValue());
         if (!wantDouble) {
            if (!(n instanceof Double)) {
               return Long.toString(n.longValue());
            }

            Double d = (Double)n;
            if (d == Math.rint(d)) {
               return Long.toString(n.longValue());
            }
         }

         double d = n.doubleValue();
         if (Double.isNaN(d)) {
            return "nan";
         } else if (Double.isInfinite(d)) {
            return d > (double)0.0F ? "inf" : "-inf";
         } else {
            String s = Double.toString(d);
            if (s.contains("E")) {
               s = String.format(Locale.ROOT, "%.10f", d).replaceAll("0+$", "").replaceAll("\\.$", ".0");
            }

            return s;
         }
      } else if (!(value instanceof List)) {
         return escape(String.valueOf(value));
      } else {
         List<?> l;
         StringBuilder sb;
         Object var10000;
         label122: {
            l = (List)value;
            sb = new StringBuilder("[");
            if (like instanceof List) {
               List<?> ll = (List)like;
               if (!ll.isEmpty()) {
                  var10000 = ll.get(0);
                  break label122;
               }
            }

            var10000 = null;
         }

         Object sample = var10000;

         for(int i = 0; i < l.size(); ++i) {
            if (i > 0) {
               sb.append(", ");
            }

            Object el = l.get(i);
            if (sample != null && !(el instanceof Raw)) {
               if (sample instanceof Boolean && el instanceof String) {
                  String s = (String)el;
                  sb.append(Boolean.parseBoolean(s));
                  continue;
               }

               if (sample instanceof Long && el instanceof String) {
                  String s = (String)el;

                  try {
                     sb.append(Long.parseLong(s.trim()));
                     continue;
                  } catch (NumberFormatException var10) {
                  }
               }

               if (sample instanceof Double && el instanceof String) {
                  String s = (String)el;

                  try {
                     sb.append(format(Double.parseDouble(s.trim()), sample));
                     continue;
                  } catch (NumberFormatException var9) {
                  }
               }
            }

            sb.append(format(el, sample));
         }

         return sb.append(']').toString();
      }
   }

   public boolean set(String path, Object value) {
      Entry e = (Entry)this.find(path).orElse(null);
      if (e == null) {
         return false;
      } else {
         String raw = format(value, e.value);
         String var10000 = e.prefix;
         String newLine = var10000 + raw + (e.suffix == null ? "" : e.suffix.replace("\n", ""));

         for(int i = e.endLine; i > e.line; --i) {
            this.lines.remove(i);
         }

         this.lines.set(e.line, newLine);
         int removed = e.endLine - e.line;
         e.endLine = e.line;
         e.raw = raw;
         e.value = parseValue(raw);
         if (removed > 0) {
            this.reindex(e.line, -removed);
         }

         return true;
      }
   }

   private void reindex(int afterLine, int delta) {
      for(Entry x : this.entries) {
         if (x.line > afterLine) {
            x.line += delta;
            x.endLine += delta;
         }
      }

      for(int i = 0; i < this.sections.size(); ++i) {
         Section s = (Section)this.sections.get(i);
         if (s.line() > afterLine) {
            this.sections.set(i, new Section(s.path(), s.comments(), s.line() + delta));
         }
      }

   }

   static boolean isMeta(String c) {
      return DEFAULT_META.matcher(c).matches() || RANGE_META.matcher(c).matches() || ALLOWED_META.matcher(c).matches();
   }

   private static void applyMeta(Entry e) {
      for(String c : e.comments) {
         Matcher m = DEFAULT_META.matcher(c);
         if (m.matches()) {
            Object v = parseValue(m.group(1).trim());
            e.def = v instanceof Raw ? m.group(1).trim() : v;
         } else {
            m = RANGE_META.matcher(c);
            if (m.matches()) {
               parseRange(e, m.group(1).trim());
            } else {
               m = ALLOWED_META.matcher(c);
               if (m.matches()) {
                  List<String> vals = new ArrayList();

                  for(String v : m.group(1).split(",")) {
                     if (!v.trim().isEmpty()) {
                        vals.add(v.trim());
                     }
                  }

                  e.allowed = vals;
               }
            }
         }
      }

   }

   private static void parseRange(Entry e, String spec) {
      try {
         if (spec.startsWith(">")) {
            e.min = Double.parseDouble(spec.substring(1).trim());
            return;
         }

         if (spec.startsWith("<")) {
            e.max = Double.parseDouble(spec.substring(1).trim());
            return;
         }

         String[] parts = spec.split("~");
         if (parts.length == 2) {
            e.min = Double.parseDouble(parts[0].trim());
            e.max = Double.parseDouble(parts[1].trim());
         }
      } catch (NumberFormatException var3) {
      }

   }

   public static final class Entry {
      public final String section;
      public final String key;
      public final List<String> comments;
      public String raw;
      public Object value;
      public String prefix;
      public String suffix;
      public int line;
      public int endLine;
      public @Nullable Object def;
      public @Nullable Double min;
      public @Nullable Double max;
      public @Nullable List<String> allowed;

      Entry(String section, String key, List<String> comments) {
         this.section = section;
         this.key = key;
         this.comments = comments;
      }

      public String path() {
         return this.section.isEmpty() ? this.key : this.section + "." + this.key;
      }

      public List<String> description() {
         List<String> out = new ArrayList();

         for(String c : this.comments) {
            if (!TomlLines.isMeta(c)) {
               out.add(c);
            }
         }

         return out;
      }
   }

   public static record Section(String path, List<String> comments, int line) {
   }

   public static record Raw(String text) {
      public String toString() {
         return this.text;
      }
   }
}
