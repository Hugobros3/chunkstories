package io.xol.chunkstories.mesh;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.api.mesh.MeshLoader;
import io.xol.chunkstories.api.mesh.MultiPartMesh;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.ModsManager;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.engine.model.WavefrontLoader;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MeshStore implements MeshLibrary {
	
	protected final Content content;
	protected final ModsManager modsManager;
	
	protected Map<String, Mesh> meshes = new HashMap<String, Mesh>();
	
	protected Map<String, MeshLoader> loaders = new HashMap<String, MeshLoader>();
	
	public MeshStore(GameContentStore gameContentStore)
	{
		this.content = gameContentStore;
		this.modsManager = gameContentStore.modsManager();
		
		//Default .obj loader
		//TODO add custom ones ?
		MeshLoader waveFrontLoader = new WavefrontLoader();
		loaders.put(waveFrontLoader.getExtension(), waveFrontLoader);

		//reload();
	}
	
	public void reload() {
		meshes.clear();
	}

	@Override
	public Mesh getMeshByName(String meshName) {
		
		Mesh mesh = meshes.get(meshName);
		
		if(mesh == null)
		{
			Asset a = modsManager.getAsset(meshName);
			String s[] = a.getName().split("[.]");
			
			if(s.length <= 1)
			{
				logger().warn("Mesh "+meshName+" did not come with a valid suffix to lookup a loader from.");
				return null;
			}
			
			String suffix = s[s.length - 1];
			MeshLoader loader = loaders.get(suffix);
			
			if(loader == null)
			{
				logger().warn("There is no MeshLoader to load mesh "+meshName+" using extension "+suffix+ ".");
				return null;
			}

			try { 
				mesh = loader.loadMeshFromAsset(a);
			} catch (MeshLoadException e) {
				e.printStackTrace();
				logger().error("Mesh "+meshName+" couldn't be load using MeshLoader "+loader.getClass().getName()+ " ,stack trace above.");
				return null;
			}
			
			meshes.put(meshName, mesh);
		}
		
		return mesh;
	}

	@Override
	public MultiPartMesh getMultiPartMeshByName(String meshName) {
		
		Mesh mesh = this.getMeshByName(meshName);
		
		if(mesh == null || !(mesh instanceof MultiPartMesh))
			return null;
		
		return (MultiPartMesh)mesh;
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
