//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.AnimatableMesh;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.content.GameContentStore;

public class MeshStore implements MeshLibrary {

	protected final Content content;
	protected final ModsManager modsManager;

	protected Map<String, Mesh> meshes = new HashMap<String, Mesh>();

	AssimpMeshLoader loader = new AssimpMeshLoader(this);

	public MeshStore(GameContentStore gameContentStore) {
		this.content = gameContentStore;
		this.modsManager = gameContentStore.modsManager();
	}

	public void reloadAll() {
		meshes.clear();
	}

	@Override
	public Mesh getMesh(String meshName) {

		Mesh mesh = meshes.get(meshName);

		if (mesh == null) {
			Asset a = modsManager.getAsset(meshName);
			try {
				mesh = loader.load(a);
			} catch (MeshLoadException e) {
				e.printStackTrace();
				logger().error("Mesh " + meshName + " couldn't be load using MeshLoader " + loader.getClass().getName() + " ,stack trace above.");
				return null;
			}

			meshes.put(meshName, mesh);
		}

		return mesh;
	}

	@Override
	public AnimatableMesh getAnimatableMesh(String meshName) {

		Mesh mesh = this.getMesh(meshName);

		if (mesh == null || !(mesh instanceof AnimatableMesh))
			return null;

		return (AnimatableMesh) mesh;
	}

	@Override
	public Content parent() {
		return content;
	}

	private static final Logger logger = LoggerFactory.getLogger("content.meshes");

	public Logger logger() {
		return logger;
	}
}
