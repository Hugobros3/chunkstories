package io.xol.chunkstories.mesh;

import assimp.AiScene;
import assimp.Importer;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.MeshLibrary;

public class AssimpMesh {
	protected final Asset mainAsset;
	protected final MeshLibrary store;

	public AssimpMesh(Asset mainAsset, MeshLibrary store) throws MeshLoadException {
		this.mainAsset = mainAsset;
		this.store = store;
		
		Importer im = new Importer();
		assimp.SettingsKt.setASSIMP_LOAD_TEXTURES(false);
		
		im.setIoHandler(new AssetIOSystem(store.parent()));
		AiScene scene = im.readFile(mainAsset.getName(), 0);
		System.out.println(scene);
		System.out.println("welp");
	}
}	
