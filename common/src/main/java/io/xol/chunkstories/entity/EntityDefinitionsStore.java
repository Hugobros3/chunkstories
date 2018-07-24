//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.EntityDefinitions;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.exceptions.content.IllegalEntityDeclarationException;
import io.xol.chunkstories.content.GameContentStore;

public class EntityDefinitionsStore implements EntityDefinitions {
	private final Content content;

	private Map<String, EntityDefinition> EntityDefinitionsByName = new HashMap<String, EntityDefinition>();

	private static final Logger logger = LoggerFactory.getLogger("content.entities");

	public Logger logger() {
		return logger;
	}

	public EntityDefinitionsStore(GameContentStore content) {
		this.content = content;
	}

	public void reload() {
		// EntityDefinitionsById.clear();
		EntityDefinitionsByName.clear();

		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("entities");
		while (i.hasNext()) {
			Asset f = i.next();
			readEntitiesDefinitions(f);
		}

		// this.entityComponents.reload();
	}

	private void readEntitiesDefinitions(Asset f) {
		if (f == null)
			return;

		logger().debug("Reading entities definitions in : " + f);
		try {
			BufferedReader reader = new BufferedReader(f.reader());
			String line = "";
			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", "");
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				} else {
					if (line.startsWith("entity ")) {
						String[] split = line.split(" ");
						String name = split[1];
						// short id = Short.parseShort(split[2]);

						try {
							EntityDefinitionImplementation entityType = new EntityDefinitionImplementation(this, name,
									reader);

							// this.EntityDefinitionsById.put(entityType.getId(), entityType);
							this.EntityDefinitionsByName.put(entityType.getName(), entityType);
						} catch (IllegalEntityDeclarationException e) {
							e.printStackTrace();
						}
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public EntityDefinition getEntityDefinition(String TraitName) {
		return EntityDefinitionsByName.get(TraitName);
	}

	@Override
	public Iterator<EntityDefinition> all() {
		return this.EntityDefinitionsByName.values().iterator();
	}

	@Override
	public Content parent() {
		return content;
	}
}
