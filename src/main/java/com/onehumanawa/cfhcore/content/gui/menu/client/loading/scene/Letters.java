package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

final class Letters {
   private static final String[][] WORD = new String[][]{{"#...#", "##.##", "#.#.#", "#...#", "#...#"}, {"#", "#", "#", "#", "#"}, {"#..#", "##.#", "#.##", "#..#", "#..#"}, {"###", "#..", "##.", "#..", "###"}, {"###", "#..", "#..", "#..", "###"}, {"###.", "#..#", "###.", "#.#.", "#..#"}, {"###", "#.#", "###", "#.#", "#.#"}, {"###", "#..", "##.", "#..", "#.."}, {"###", ".#.", ".#.", ".#.", ".#."}};
   static final int HEIGHT = 5;
   static final float WIDTH;
   private static final float DEPTH = 0.9F;
   private static final float GAP = 0.8F;
   private static final Tile[] STONES;

   static void build(Mesh m) {
      float x = -WIDTH / 2.0F;

      for(String[] letter : WORD) {
         int w = letter[0].length();

         for(int row = 0; row < 5; ++row) {
            for(int col = 0; col < w; ++col) {
               if (filled(letter, col, row)) {
                  float x0 = x + (float)col;
                  float y0 = (float)(4 - row);
                  int sides = 0;
                  if (!filled(letter, col, row - 1)) {
                     sides |= 1;
                  }

                  if (!filled(letter, col, row + 1)) {
                     sides |= 2;
                  }

                  if (!filled(letter, col - 1, row)) {
                     sides |= 32;
                  }

                  if (!filled(letter, col + 1, row)) {
                     sides |= 16;
                  }

                  int n = Math.round(x0 * 2.0F) + 128;
                  int tone = Atlas.shade(-1, (0.88F + 0.12F * Atlas.hash(n, row, 5)) * (1.0F - 0.035F * (float)row));
                  m.reset();
                  m.tile(STONES[(int)(Atlas.hash(n, row, 9) * 2.999F)]).faces(4).gloss(0.12F).colour(tone);
                  m.box(x0, y0, -0.9F, x0 + 1.0F, y0 + 1.0F, 0.9F);
                  if (sides != 0) {
                     m.tile(Tile.STONE_SIDE).faces(sides);
                     m.box(x0, y0, -0.9F, x0 + 1.0F, y0 + 1.0F, 0.9F);
                  }
               }
            }
         }

         x += (float)w + 0.8F;
      }

      m.reset();
   }

   private static boolean filled(String[] letter, int col, int row) {
      return row >= 0 && row < 5 && col >= 0 && col < letter[0].length() && letter[row].charAt(col) == '#';
   }

   private Letters() {
   }

   static {
      STONES = new Tile[]{Tile.STONE, Tile.STONE_B, Tile.STONE_C};
      float w = (float)(WORD.length - 1) * 0.8F;

      for(String[] letter : WORD) {
         w += (float)letter[0].length();
      }

      WIDTH = w;
   }
}
