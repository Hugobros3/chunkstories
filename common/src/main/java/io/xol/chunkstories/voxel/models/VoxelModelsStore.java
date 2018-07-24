//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.content.mods.AssetHierarchy;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.VoxelsStore;

public class VoxelModelsStore implements Content.Voxels.VoxelModels {
	private final VoxelsStore voxels;
	private final VoxelModelLoader loader = new VoxelModelLoader(this);

	private Map<String, VoxelModel> models = new HashMap<String, VoxelModel>();

	private final static Logger logger = LoggerFactory.getLogger("content.voxels.models");

	public VoxelModelsStore(VoxelsStore voxelsLoader) {
		this.voxels = voxelsLoader;

		resetAndLoadModels();
	}

	public void resetAndLoadModels() {
		models.clear();

		Iterator<AssetHierarchy> allFiles = voxels.parent().modsManager().getAllUniqueEntries();
		while (allFiles.hasNext()) {
			AssetHierarchy entry = allFiles.next();
			if (entry.getName().startsWith("./voxels/blockmodels/") && entry.getName().endsWith(".model")) {
				Asset f = entry.topInstance();
				loader.readBlockModel(f).forEach(model -> models.put(model.getName(), model));
			}
		}

	}

	@Override
	public CustomVoxelModel getVoxelModel(String name) {
		if (name.endsWith(".default"))
			name = name.substring(0, name.length() - 8);
		if (models.containsKey(name)) {
			CustomVoxelModel renderer = (CustomVoxelModel) models.get(name);
			if (renderer != null)
				return renderer;
		}
		logger().warn("Couldn't serve voxel model : " + name);
		return null;
	}

	@Override
	public Iterator<VoxelModel> all() {
		return models.values().iterator();
	}

	@Override
	public Voxels parent() {
		return voxels;
	}

	@Override
	public Logger logger() {
		return logger;
	}
}
