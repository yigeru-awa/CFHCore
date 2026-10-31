package com.onehumanawa.cfhcore.content.worldgen;

import java.util.OptionalInt;

public interface CFHChunk {
	void setMaxHeight(int maxHeight);
	
	OptionalInt getMaxHeight();	
}
