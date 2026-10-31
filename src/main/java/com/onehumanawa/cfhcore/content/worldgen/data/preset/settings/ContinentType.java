package com.onehumanawa.cfhcore.content.worldgen.data.preset.settings;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import com.onehumanawa.cfhcore.content.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.Continent;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.advanced.AdvancedContinentGenerator;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.simple.MultiContinentGenerator;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.simple.SingleContinentGenerator;
import com.onehumanawa.cfhcore.content.worldgen.cell.continent.uplift.UpliftContinentGenerator;
import com.onehumanawa.cfhcore.content.worldgen.util.Seed;

public enum ContinentType implements StringRepresentable {
    MULTI {
        
    	@Override
        public MultiContinentGenerator create(Seed seed, GeneratorContext context) {
            return new MultiContinentGenerator(seed, context);
        }
    }, 
    SINGLE {
        
    	@Override
        public SingleContinentGenerator create(Seed seed, GeneratorContext context) {
            return new SingleContinentGenerator(seed, context);
        }
    }, 
    MULTI_IMPROVED {
        
    	@Override
        public AdvancedContinentGenerator create(Seed seed, GeneratorContext context) {
            return new AdvancedContinentGenerator(seed, context);
        }
    },
    UPLIFT {

        @Override
        public UpliftContinentGenerator create(Seed seed, GeneratorContext context) {
            return new UpliftContinentGenerator(seed, context);
        }
    };
	
	public static final Codec<ContinentType> CODEC = StringRepresentable.fromEnum(ContinentType::values);
    
    public abstract Continent create(Seed seed, GeneratorContext context);

    @Override
	public String getSerializedName() {
		return this.name();
	}
}
