package com.onehumanawa.cfhcore.content.world.worldgen.cell.terrain;

public interface ITerrain {
   default float erosionModifier() {
      return 1.0F;
   }

   default boolean isFlat() {
      return false;
   }

   default boolean isRiver() {
      return false;
   }

   default boolean isShallowOcean() {
      return false;
   }

   default boolean isDeepOcean() {
      return false;
   }

   default boolean isCoast() {
      return false;
   }

   default boolean isSubmerged() {
      return this.isDeepOcean() || this.isShallowOcean() || this.isRiver() || this.isLake();
   }

   default boolean isOverground() {
      return false;
   }

   default boolean overridesRiver() {
      return this.isDeepOcean() || this.isShallowOcean() || this.isCoast();
   }

   default boolean overridesCoast() {
      return this.isVolcano();
   }

   default boolean isLake() {
      return false;
   }

   default boolean isWetland() {
      return false;
   }

   default boolean isWateryButNotOcean() {
      return this.isLake() || this.isRiver() || this.isWetland();
   }

   default boolean isMountain() {
      return false;
   }

   default boolean isVolcano() {
      return false;
   }

   public interface Delegate extends ITerrain {
      ITerrain getDelegate();

      @Override
      default float erosionModifier() {
         return this.getDelegate().erosionModifier();
      }

      @Override
      default boolean isFlat() {
         return this.getDelegate().isFlat();
      }

      @Override
      default boolean isRiver() {
         return this.getDelegate().isRiver();
      }

      @Override
      default boolean isShallowOcean() {
         return this.getDelegate().isShallowOcean();
      }

      @Override
      default boolean isDeepOcean() {
         return this.getDelegate().isDeepOcean();
      }

      @Override
      default boolean isCoast() {
         return this.getDelegate().isCoast();
      }

      @Override
      default boolean overridesRiver() {
         return this.getDelegate().overridesRiver();
      }

      @Override
      default boolean overridesCoast() {
         return this.getDelegate().overridesCoast();
      }

      @Override
      default boolean isLake() {
         return this.getDelegate().isLake();
      }

      @Override
      default boolean isWetland() {
         return this.getDelegate().isWetland();
      }

      @Override
      default boolean isOverground() {
         return this.getDelegate().isOverground();
      }

      @Override
      default boolean isSubmerged() {
         return this.getDelegate().isSubmerged();
      }

      @Override
      default boolean isMountain() {
         return this.getDelegate().isMountain();
      }

      @Override
      default boolean isVolcano() {
         return this.getDelegate().isVolcano();
      }
   }
}
