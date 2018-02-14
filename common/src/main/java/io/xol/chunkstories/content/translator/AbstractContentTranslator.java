package io.xol.chunkstories.content.translator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.ContentTranslator;
import io.xol.chunkstories.api.content.mods.Mod;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;
import io.xol.chunkstories.api.net.PacketDefinition;
import io.xol.chunkstories.api.voxel.Voxel;

/** Assigns IDs for everything that needs one */
public abstract class AbstractContentTranslator implements ContentTranslator {

	final static Logger logger = LoggerFactory.getLogger("content.translator");
	
	final Content content;
	Set<String> requiredMods;
	
	Map<Voxel, Integer> voxelMappings;
	private Voxel[] voxelsArray;
	
	Map<EntityDefinition, Integer> entityMappings;
	private EntityDefinition[] entitiesArray;
	
	Map<ItemDefinition, Integer> itemMappings;
	private ItemDefinition[] itemsArray;
	
	Map<PacketDefinition, Integer> packetMappings;
	private PacketDefinition[] packetsArray;
	
	/** Create an initial content translator */
	public AbstractContentTranslator(Content content) {
		this.content = content;
	}
	
	public void buildArrays() {
		// Create indexable arrays
		voxelsArray = new Voxel[Collections.max(voxelMappings.values())+1];
		voxelMappings.forEach((voxel, id) -> voxelsArray[id] = voxel);
		
		entitiesArray = new EntityDefinition[Collections.max(entityMappings.values())+1];
		entityMappings.forEach((entity, id) -> entitiesArray[id] = entity);
		
		itemsArray = new ItemDefinition[Collections.max(itemMappings.values())+1];
		itemMappings.forEach((item, id) -> itemsArray[id] = item);
		
		packetsArray = new PacketDefinition[Collections.max(packetMappings.values())+1];
		packetMappings.forEach((packet, id) -> packetsArray[id] = packet);
	}
	
	/** Derives a modified ContentTranslator that takes into account the new content */
	public AbstractContentTranslator loadWith(Content content) {
		return null;
	}
	
	/** Internal method to check if a content has the right mods loaded. Returns the missing mods.*/
	Set<String> hasRequiredMods(Content content) {
		Collection<Mod> loadedMods = content.modsManager().getCurrentlyLoadedMods();
		Set<String> loadedModsAsString = new HashSet<>();
		loadedMods.forEach(mod -> loadedModsAsString.add(mod.getModInfo().getInternalName()));
		
		Set<String> missing = new HashSet<>();
		
		for(String internalName : requiredMods) {
			if(!loadedModsAsString.contains(internalName))
				missing.add(internalName);
		}
		
		/*if(strict) {
			for(String internalName : loadedModsAsString) {
				if(!requiredMods.contains(internalName))
					return false;
			}
		}*/
		
		return missing;
	}
	
	/** Can we load an existing world with the current configuration without issues ? */
	public boolean compatibleWith(Content content) {
		//Check every needed mod is present
		if(hasRequiredMods(content).size() > 0)
			return false;
		
		//Check every translatable definition has a match
		for(Voxel voxel : voxelMappings.keySet())
			if(content.voxels().getVoxelByName(voxel.getName()) == null)
				return false;
		
		for(EntityDefinition entity : entityMappings.keySet())
			if(content.entities().getEntityTypeByName(entity.getName()) == null)
				return false;
		
		for(ItemDefinition item : itemMappings.keySet())
			if(content.items().getItemTypeByName(item.getName()) == null)
				return false;
		
		for(PacketDefinition packet : packetMappings.keySet())
			if(content.packets().getPacketTypeByName(packet.getName()) == null)
				return false;
		
		return true;
		//return loadWith(content) != null;
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
		for(String internalName : requiredMods) {
			writer.write("requiredMod "+internalName+"\n");
		}
		
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

	public void save(File file) {
		try {
			file.getParentFile().mkdirs();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			write(writer);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
