package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

final class TabFlow {
   private static final int NINE = 9;
   final int cols;
   final int stacks;
   private final int size;
   private final int blanks;
   private final int[] cells;
   private final int[] headings;

   private TabFlow(int cols, int stacks, int size, int blanks, int[] cells, int[] headings) {
      this.cols = cols;
      this.stacks = stacks;
      this.size = size;
      this.blanks = blanks;
      this.cells = cells;
      this.headings = headings;
   }

   static @Nullable TabFlow of(List<ItemStack> entries, int cols, @Nullable TabFlow last) {
      int n = entries.size();
      int blanks = 0;
      boolean any = false;

      for(int i = 0; i < n; ++i) {
         if (((ItemStack)entries.get(i)).isEmpty()) {
            blanks = blanks * 31 + i;
            any = true;
         }
      }

      if (!any) {
         return null;
      } else if (last != null && last.cols == cols && last.size == n && last.blanks == blanks) {
         return last;
      } else {
         boolean[] blank = new boolean[n];

         for(int i = 0; i < n; ++i) {
            blank[i] = ((ItemStack)entries.get(i)).isEmpty();
         }

         return lay(blank, cols, blanks);
      }
   }

   static TabFlow lay(boolean[] blank, int cols, int blanks) {
      int n = blank.length;
      IntArrayList cells = new IntArrayList();
      IntArrayList headings = new IntArrayList();
      int stacks = 0;
      int col = 0;
      int i = 0;

      while(i < n) {
         if (!blank[i]) {
            col = put(cells, headings, cols, col, i++);
            ++stacks;
         } else {
            int end;
            for(end = i; end < n && blank[end]; ++end) {
            }

            if (end == n) {
               break;
            }

            if (end / 9 > i / 9) {
               col = 0;

               for(int row = (i + 9 - 1) / 9; row < end / 9; ++row) {
                  headings.add(row);

                  for(int c = 0; c < cols; ++c) {
                     cells.add(-1);
                  }
               }
            } else {
               for(int gap = i; gap < end; ++gap) {
                  col = put(cells, headings, cols, col, -1);
               }
            }

            i = end;
         }
      }

      return new TabFlow(cols, stacks, n, blanks, cells.toIntArray(), headings.toIntArray());
   }

   private static int put(IntArrayList cells, IntArrayList headings, int cols, int col, int entry) {
      if (col == 0) {
         headings.add(-1);

         for(int c = 0; c < cols; ++c) {
            cells.add(-1);
         }
      }

      cells.set(cells.size() - cols + col, entry);
      return (col + 1) % cols;
   }

   int rows() {
      return this.headings.length;
   }

   int entry(int row, int col) {
      return row >= 0 && row < this.headings.length && col >= 0 && col < this.cols ? this.cells[row * this.cols + col] : -1;
   }

   int heading(int row) {
      return row >= 0 && row < this.headings.length ? this.headings[row] : -1;
   }
}
