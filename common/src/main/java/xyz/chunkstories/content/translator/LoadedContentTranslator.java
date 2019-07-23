//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content.translator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.entity.EntityDefinition;
import xyz.chunkstories.api.item.ItemDefinition;
import xyz.chunkstories.api.net.PacketDefinition;
import xyz.chunkstories.api.voxel.Voxel;

public class LoadedContentTranslator extends AbstractContentTranslator {

	public LoadedContentTranslator(Content content, BufferedReader reader)
			throws IOException, IncompatibleContentException {
		super(content);

		requiredMods = new HashSet<>();
		voxelMappings = new HashMap<>();
		entityMappings = new HashMap<>();
		itemMappings = new HashMap<>();
		packetMappings = new HashMap<>();

		String line;
		while ((line = reader.readLine()) != null) {
			String tokens[] = line.split(" ");

			if (line.startsWith("#"))
				continue;

			// 3-tokens lines are mappings
			if (tokens.length == 3) {
				String defType = tokens[0];
				int id = Integer.parseInt(tokens[1]);
				String defName = tokens[2];

				switch (defType) {
				case "voxel":
					Voxel voxel = content.getVoxels().getVoxel(defName);
					failIfNull(voxel, "Missing voxel definition " + defName);
					voxelMappings.put(voxel, id);
					break;
				case "entity":
					EntityDefinition entityDef = content.getEntities().getEntityDefinition(defName);
					failIfNull(entityDef, "Missing entity definition " + defName);
					entityMappings.put(entityDef, id);
					break;
				case "item":
					ItemDefinition itemDef = content.getItems().getItemDefinition(defName);
					failIfNull(itemDef, "Missing item definition " + defName);
					itemMappings.put(itemDef, id);
					break;
				case "packet":
					PacketDefinition packetDef = content.getPackets().getPacketByName(defName);
					failIfNull(packetDef, "Missing packet definition " + defName);
					packetMappings.put(packetDef, id);
					break;
				default:
					logger.warn("Unknown definition type '" + defType
							+ "' while parsing existing ContentTranslator. Ignoring.");
					break;
				}
			} else if (tokens.length == 2) {
				String tokenName = tokens[0];
				String tokenValue = tokens[1];

				switch (tokenName) {
				case "requiredMod":
					requiredMods.add(tokenValue);
					break;
				default:
					logger.warn(
							"Unknown token '" + tokenName + "' while parsing existing ContentTranslator. Ignoring.");
				}
			}
		}

		// Ensure the mods situation is okay too
		Set<String> missingMods = hasRequiredMods(content);
		if (missingMods.size() > 0)
			throw new IncompatibleContentException("Missing mods: " + missingMods.toString());

		// Assign ids to whatever was added
		this.assignVoxelIds(false);
		this.assignEntityIds(false);
		this.assignItemIds(false);
		this.assignPacketIds(false);

		content.getModsManager().getCurrentlyLoadedMods().forEach(m -> requiredMods.add(m.getModInfo().getInternalName()));

		buildArrays();
	}

	private void failIfNull(Object o, String err) throws IncompatibleContentException {
		if (o == null)
			throw new IncompatibleContentException(err);
	}

	public static LoadedContentTranslator loadFromFile(Content content, File file)
			throws IOException, IncompatibleContentException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		return new LoadedContentTranslator(content, reader);
	}
}
