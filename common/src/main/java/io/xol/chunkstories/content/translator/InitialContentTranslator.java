package io.xol.chunkstories.content.translator;

import java.util.HashSet;

import io.xol.chunkstories.api.content.Content;

public class InitialContentTranslator extends AbstractContentTranslator {

	public InitialContentTranslator(Content content) {
		super(content);
		
		requiredMods = new HashSet<>();
		content.modsManager().getCurrentlyLoadedMods().forEach(m -> requiredMods.add(m.getModInfo().getInternalName()));
		
		this.assignVoxelIds();
		this.assignEntityIds();
		this.assignItemIds();
		//this.assignPacketIds(); - a priori not needed
		
		buildArrays();
	}
}
