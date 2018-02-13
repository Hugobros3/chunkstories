package io.xol.chunkstories.content.translator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.ContentTranslator;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;
import io.xol.chunkstories.api.net.PacketDefinition;
import io.xol.chunkstories.api.voxel.Voxel;

/** Assigns IDs for everything that needs one */
public class SimpleContentTranslator implements ContentTranslator {

	final Content content;
	final Set<String> requiredMods;
	
	final Map<Voxel, Integer> voxelMappings;
	final Voxel[] voxelsArray;
	
	final Map<EntityDefinition, Integer> entityMappings;
	final EntityDefinition[] entitiesArray;
	
	final Map<ItemDefinition, Integer> itemMappings;
	final ItemDefinition[] itemsArray;
	
	final Map<PacketDefinition, Integer> packetMappings;
	final PacketDefinition[] packetsArray;
	
	/** Create an initial content translator */
	public SimpleContentTranslator(Content content) {
		this.content = content;
		
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
		
		// Create indexable arrays
		voxelsArray = new Voxel[voxelMappings.size()];
		voxelMappings.forEach((voxel, id) -> voxelsArray[id] = voxel);
		
		entitiesArray = new EntityDefinition[entityMappings.size()];
		entityMappings.forEach((entity, id) -> entitiesArray[id] = entity);
		
		itemsArray = new ItemDefinition[itemMappings.size()];
		itemMappings.forEach((item, id) -> itemsArray[id] = item);
		
		packetsArray = new PacketDefinition[packetMappings.size()];
		packetMappings.forEach((packet, id) -> packetsArray[id] = packet);
		
		test();
	}
	
	/** Derives a modified ContentTranslator that takes into account the new content */
	public SimpleContentTranslator loadWith(Content content) {
		return null;
	}
	
	/** Can we load an existing world with the current configuration without issues ? */
	public boolean compatibleWith(Content content) {
		return loadWith(content) != null;
	}
	
	@Override
	public Collection<String> getRequiredMods() {
		return requiredMods;
	}

	@Override
	public Content getContent() {
		return content;
	}
	
	@Override
	public int getIdForVoxel(Voxel voxel) {
		return voxelMappings.getOrDefault(voxel, -1);
	}

	@Override
	public Voxel getVoxelForId(int id) {
		if(id < 0 || id >= voxelsArray.length)
			return content.voxels().air();
		return voxelsArray[id];
	}

	@Override
	public int getIdForItem(ItemDefinition definition) {
		return itemMappings.getOrDefault(definition, -1);
	}

	@Override
	public int getIdForItem(Item item) {
		return 0;
	}

	@Override
	public ItemDefinition getItemForId(int id) {
		if(id < 0 || id >= itemsArray.length)
			return null;
		return itemsArray[id];
	}

	@Override
	public int getIdForEntity(EntityDefinition definition) {
		return entityMappings.getOrDefault(definition, -1);
	}

	@Override
	public int getIdForEntity(Entity entity) {
		return getIdForEntity(entity.getDefinition());
	}

	@Override
	public EntityDefinition getEntityForId(int id) {
		if(id < 0 || id >= entitiesArray.length)
			return null;
		return entitiesArray[id];
	}

	@Override
	public int getIdForPacket(PacketDefinition definition) {
		return packetMappings.getOrDefault(definition, -1);
	}

	@Override
	public PacketDefinition getPacketForId(int id) {
		if(id < 0 || id >= packetsArray.length)
			return null;
		return packetsArray[id];
	}
	
	public void write(BufferedWriter writer) throws IOException {
		for(Entry<Voxel, Integer> e : voxelMappings.entrySet()) {
			writer.write("voxel "+e.getValue()+" "+e.getKey().getName()+"\n");
		}
		
		for(Entry<ItemDefinition, Integer> e : itemMappings.entrySet()) {
			writer.write("item "+e.getValue()+" "+e.getKey().getName()+"\n");
		}
		
		for(Entry<EntityDefinition, Integer> e : entityMappings.entrySet()) {
			writer.write("entity "+e.getValue()+" "+e.getKey().getName()+"\n");
		}
		
		for(Entry<PacketDefinition, Integer> e : packetMappings.entrySet()) {
			writer.write("packet "+e.getValue()+" "+e.getKey().getName()+"\n");
		}
	}
	
	public void test() {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
			write(writer);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
