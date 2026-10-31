package com.onehumanawa.cfhcore.content.gui.config.search;

import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class SearchIndex {
   private final List<Entry> entries = new ArrayList();
   private final List<String> haystacks = new ArrayList();

   public void add(Entry e) {
      this.entries.add(e);
      List var10000 = this.haystacks;
      String var10001 = e.binding.searchText();
      var10000.add((var10001 + " " + e.sectionTitle.getString() + " " + (e.tabTitle == null ? "" : e.tabTitle.getString()) + " " + e.pageTitle.getString()).toLowerCase(Locale.ROOT));
   }

   public void addAll(List<Entry> es) {
      for(Entry e : es) {
         this.add(e);
      }

   }

   public int size() {
      return this.entries.size();
   }

   public List<Hit> query(String text, String excludePath, int limit) {
      List<Hit> hits = new ArrayList();
      String q = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
      if (q.isEmpty()) {
         return hits;
      } else {
         String[] toks = q.split(" +");

         for(int i = 0; i < this.entries.size(); ++i) {
            Entry e = (Entry)this.entries.get(i);
            if (!e.path.equals(excludePath)) {
               String hay = (String)this.haystacks.get(i);
               boolean all = true;

               for(String t : toks) {
                  if (!hay.contains(t)) {
                     all = false;
                     break;
                  }
               }

               if (all) {
                  String label = e.binding.label().getString().toLowerCase(Locale.ROOT);
                  int score = 1;
                  if (label.equals(q)) {
                     score = 100;
                  } else if (label.startsWith(q)) {
                     score = 60;
                  } else if (label.contains(q)) {
                     score = 40;
                  } else if (label.contains(toks[0])) {
                     score = 20;
                  }

                  hits.add(new Hit(e, score));
               }
            }
         }

         hits.sort((a, b) -> Integer.compare(b.score, a.score));
         return (List<Hit>)(hits.size() > limit ? new ArrayList(hits.subList(0, limit)) : hits);
      }
   }

   public static record Entry(String path, Component pageTitle, @Nullable Component tabTitle, Component sectionTitle, OptionBinding binding) {
      public Entry(String path, Component pageTitle, Component sectionTitle, OptionBinding binding) {
         this(path, pageTitle, (Component)null, sectionTitle, binding);
      }

      public String pageId() {
         int i = this.path.indexOf(47);
         return i < 0 ? this.path : this.path.substring(0, i);
      }

      public Component crumb() {
         List<String> parts = new ArrayList();
         add(parts, this.pageTitle);
         if (this.tabTitle != null) {
            add(parts, this.tabTitle);
         }

         add(parts, this.sectionTitle);
         return Component.literal(String.join(" › ", parts));
      }

      private static void add(List<String> parts, Component c) {
         String s = c.getString().trim();
         if (!s.isEmpty() && (parts.isEmpty() || !((String)parts.get(parts.size() - 1)).equalsIgnoreCase(s))) {
            parts.add(s);
         }

      }
   }

   public static record Hit(Entry entry, int score) {
   }
}
