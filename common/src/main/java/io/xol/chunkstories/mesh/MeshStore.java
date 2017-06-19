package io.xol.chunkstories.mesh;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshLibrary;
import io.xol.chunkstories.api.mesh.MeshLoader;
import io.xol.chunkstories.api.mesh.MultiPartMesh;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType;
import io.xol.chunkstories.content.GameContentStore;

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

		reload();
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
				parent().logger().log("Mesh "+meshName+" did not come with a valid suffix to lookup a loader from.", LogType.CONTENT_LOADING, LogLevel.ERROR);
				return null;
			}
			
			String suffix = s[s.length - 1];
			MeshLoader loader = loaders.get(suffix);
			
			if(loader == null)
			{
				parent().logger().log("There is no MeshLoader to load mesh "+meshName+" using extension "+suffix+ ".", LogType.CONTENT_LOADING, LogLevel.ERROR);
				return null;
			}

			try { 
				mesh = loader.loadMeshFromAsset(a);
			} catch (MeshLoadException e) {
				e.printStackTrace();
				parent().logger().log("Mesh "+meshName+" couldn't be load using MeshLoader "+loader.getClass().getName()+ " ,stack trace above.", LogType.CONTENT_LOADING, LogLevel.ERROR);
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

}
