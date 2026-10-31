package com.onehumanawa.cfhcore.content.gui.menu.client.stats.table;

import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatFmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class TableRow {
   public static final long NONE = Long.MIN_VALUE;
   public final boolean section;
   public final String id;
   public final String namespace;
   public final Component name;
   public final String label;
   public String search;
   public final RowIcon icon;
   public final long[] values;
   public final String[] keys;
   public @Nullable StatKind kind;
   public @Nullable ResourceLocation statId;
   public @Nullable Component note;
   public @Nullable Object payload;
   public int rank;
   public boolean own;
   private @Nullable StatFmt.Shown[] written;
   private @Nullable String[] writtenShort;
   private int nameWidth = -1;

   private TableRow(boolean section, String id, String namespace, Component name, RowIcon icon, long[] values, String[] keys) {
      this.section = section;
      this.id = id;
      this.namespace = namespace;
      this.name = name;
      this.label = name.getString();
      this.search = (this.label + " " + id).toLowerCase(Locale.ROOT);
      this.icon = icon;
      this.values = values;
      this.keys = keys;
   }

   public static TableRow of(String id, String namespace, Component name, RowIcon icon, long[] values, String[] keys) {
      return new TableRow(false, id, namespace, name, icon, values, keys);
   }

   public static TableRow of(ResourceLocation id, Component name, RowIcon icon, long[] values, String[] keys) {
      return new TableRow(false, id.toString(), id.getNamespace(), name, icon, values, keys);
   }

   public static TableRow section(String id, Component title) {
      return new TableRow(true, "section:" + id, "", title, RowIcon.NONE, new long[0], new String[0]);
   }

   public TableRow kind(@Nullable StatKind k, @Nullable ResourceLocation stat) {
      this.kind = k;
      this.statId = stat;
      this.written = null;
      return this;
   }

   public TableRow note(@Nullable Component text) {
      this.note = text;
      return this;
   }

   public TableRow payload(@Nullable Object o) {
      this.payload = o;
      return this;
   }

   public TableRow found(String words) {
      String var10001 = this.search;
      this.search = var10001 + " " + words.toLowerCase(Locale.ROOT);
      return this;
   }

   public int nameWidth() {
      if (this.nameWidth < 0) {
         this.nameWidth = CFHGuiDraw.width(this.name);
      }

      return this.nameWidth;
   }

   public long value(int column) {
      return column >= 0 && column < this.values.length ? this.values[column] : Long.MIN_VALUE;
   }

   public @Nullable String key(int column) {
      return column >= 0 && column < this.keys.length ? this.keys[column] : null;
   }

   StatFmt.Shown written(int column, StatKind kind) {
      if (this.written == null) {
         this.written = new StatFmt.Shown[this.values.length];
      }

      StatFmt.Shown s = this.written[column];
      if (s == null) {
         this.written[column] = s = StatFmt.of(kind, this.values[column], this.statId);
      }

      return s;
   }

   String writtenShort(int column) {
      if (this.writtenShort == null) {
         this.writtenShort = new String[this.values.length];
      }

      String s = this.writtenShort[column];
      if (s == null) {
         this.writtenShort[column] = s = StatFmt.compact(this.values[column]);
      }

      return s;
   }

   public boolean empty() {
      for(long v : this.values) {
         if (v != Long.MIN_VALUE && v != 0L) {
            return false;
         }
      }

      return true;
   }
}
