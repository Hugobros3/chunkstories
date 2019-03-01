//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.mesh;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.content.Content;
import xyz.chunkstories.api.content.mods.ModsManager;
import xyz.chunkstories.api.exceptions.content.MeshLoadException;
import xyz.chunkstories.api.graphics.representation.Model;
import xyz.chunkstories.content.GameContentStore;

import java.util.HashMap;
import java.util.Map;

public class MeshStore implements Content.Models {

	protected final Content content;
	protected final ModsManager modsManager;

	protected Map<String, Model> models = new HashMap<>();

	private final AssimpMeshLoader loader;

	public MeshStore(GameContentStore gameContentStore) {
		this.content = gameContentStore;
		this.loader = new AssimpMeshLoader(this);
		this.modsManager = gameContentStore.modsManager();
	}

	public void reloadAll() {
		models.clear();
	}

	@Override public Model get(String modelName) {

		Model model = models.get(modelName);

		if (model == null) {
			Asset a = modsManager.getAsset(modelName);
			if(a == null) {
				logger().error("model: "+modelName+" not found in assets");
				return getDefaultModel();
			}

			try {
				model = loader.load(a);
			} catch (MeshLoadException e) {
				e.printStackTrace();
				logger().error("Model " + modelName + " couldn't be load using " + loader.getClass().getName() + ", stack trace above.");
				return getDefaultModel();
			}

			models.put(modelName, model);
		}

		return model;
	}

	public Content parent() {
		return content;
	}

	private static final Logger logger = LoggerFactory.getLogger("content.meshes");

	public Logger logger() {
		return logger;
	}

	@NotNull @Override public Model getDefaultModel() {
		return get("models/error.obj");
	}

	@NotNull @Override public Model getOrLoadModel(@NotNull String s) {
		return get(s);
	}
}
