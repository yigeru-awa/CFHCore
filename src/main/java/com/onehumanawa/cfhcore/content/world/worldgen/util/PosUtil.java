package com.onehumanawa.cfhcore.content.world.worldgen.util;

public class PosUtil {
   public static long packMix(int left, float right) {
      return (long)Float.floatToRawIntBits(right) & 4294967295L | ((long)left & 4294967295L) << 32;
   }

   public static long packMix(float left, int right) {
      return (long)right & 4294967295L | ((long)Float.floatToRawIntBits(left) & 4294967295L) << 32;
   }

   public static long pack(int left, int right) {
      return (long)right & 4294967295L | ((long)left & 4294967295L) << 32;
   }

   public static long pack(float left, float right) {
      return (long)right & 4294967295L | ((long)left & 4294967295L) << 32;
   }

   public static int unpackLeft(long packed) {
      return (int)(packed >>> 32 & 4294967295L);
   }

   public static int unpackRight(long packed) {
      return (int)(packed & 4294967295L);
   }

   public static long packf(float left, float right) {
      return (long)Float.floatToRawIntBits(right) & 4294967295L | ((long)Float.floatToRawIntBits(left) & 4294967295L) << 32;
   }

   public static float unpackLeftf(long packed) {
      return Float.intBitsToFloat((int)(packed >>> 32 & 4294967295L));
   }

   public static float unpackRightf(long packed) {
      return Float.intBitsToFloat((int)(packed & 4294967295L));
   }

   public static boolean contains(int x, int z, int x1, int z1, int x2, int z2) {
      return x >= x1 && x < x2 && z >= z1 && z < z2;
   }

   public static boolean contains(float x, float z, float x1, float z1, float x2, float z2) {
      return x >= x1 && x < x2 && z >= z1 && z < z2;
   }
}
