//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.material;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.voxel.materials.VoxelMaterial;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.voxel.VoxelsStore;

public class VoxelMaterialsStore implements Content.Voxels.VoxelMaterials {
	private final GameContentStore store;
	@SuppressWarnings("unused")
	private final VoxelsStore voxels;

	private static final Logger logger = LoggerFactory.getLogger("content.materials");

	public Logger logger() {
		return logger;
	}

	public VoxelMaterialsStore(VoxelsStore voxels) {
		this.voxels = voxels;
		this.store = voxels.parent();
	}

	Map<String, VoxelMaterial> materials = new HashMap<String, VoxelMaterial>();

	public void reload() {
		materials.clear();

		Iterator<Asset> i = store.modsManager().getAllAssetsByExtension("materials");
		while (i.hasNext()) {
			Asset f = i.next();
			readDefinitions(f);
		}
	}

	private void readDefinitions(Asset f) {
		try {
			BufferedReader reader = new BufferedReader(f.reader());
			String line = "";

			VoxelMaterialImplementation material = null;
			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", "");
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				}
				// We shouldn't come accross end tags by ourselves, this is dealt with in the
				// constructors
				else if (line.startsWith("end")) {
					// if (material == null)
					{
						logger().warn("Syntax error in file : " + f + " : ");
						continue;
					}
				} else if (line.startsWith("material")) {
					if (line.contains(" ")) {
						String[] split = line.split(" ");
						String materialName = split[1];

						material = new VoxelMaterialImplementation(materialName, reader);

						// Eventually add the material
						materials.put(material.getName(), material);
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public VoxelMaterial getVoxelMaterial(String name) {
		VoxelMaterial material = materials.get(name);
		if (material != null)
			return material;

		return getVoxelMaterial("undefined");
	}

	@Override
	public Iterator<VoxelMaterial> all() {
		return materials.values().iterator();
	}

	@Override
	public Content parent() {
		return store;
	}
}
