package io.xol.chunkstories.content.translator;

import java.util.HashSet;

import io.xol.chunkstories.api.content.Content;

public class InitialContentTranslator extends AbstractContentTranslator {

	public InitialContentTranslator(Content content) {
		super(content);
		
		requiredMods = new HashSet<>();
		content.modsManager().getCurrentlyLoadedMods().forEach(m -> requiredMods.add(m.getModInfo().getInternalName()));
		
		this.assignVoxelIds(true);
		this.assignEntityIds(true);
		this.assignItemIds(true);
		this.assignPacketIds(true);
		
		buildArrays();
	}
}
