package io.xol.chunkstories.content.translator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.api.content.Content;

public class InitialContentTranslator extends AbstractContentTranslator {

	public InitialContentTranslator(Content content) {
		super(content);
		
		requiredMods = new HashSet<>();
		content.modsManager().getCurrentlyLoadedMods().forEach(m -> requiredMods.add(m.getModInfo().getInternalName()));
		
		AtomicInteger voxelIdsCounter = new AtomicInteger(1);
		voxelMappings = new HashMap<>();
		content.voxels().all().forEachRemaining(voxel -> {
			if(voxel.getName().equals("air"))
				voxelMappings.put(voxel, 0); // Air gets ID 0, always.
			else
				voxelMappings.put(voxel, voxelIdsCounter.getAndIncrement());
		});
		
		entityMappings = new HashMap<>();
		content.entities().all().forEachRemaining(entity -> entityMappings.put(entity, entityMappings.size()));
		
		itemMappings = new HashMap<>();
		content.items().all().forEachRemaining(item -> itemMappings.put(item, itemMappings.size()));

		AtomicInteger packetIdsCounter = new AtomicInteger(1);
		packetMappings = new HashMap<>();
		content.packets().all().forEachRemaining(packet -> {
			if(packet.getName().equals("text"))
				packetMappings.put(packet, 0); // 'text' packet gets ID 0 always too
			else
				packetMappings.put(packet, packetIdsCounter.getAndIncrement());
		});
		
		buildArrays();
	}
}
