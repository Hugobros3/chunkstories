package io.xol.engine.model;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.client.Client;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ModelLibrary
{
	// This class holds static model info
	// static Map<String, ObjMeshLegacy> models = new HashMap<String, ObjMeshLegacy>();
	
	static Map<Asset, ObjMeshRenderable> objModelsRenderable = new HashMap<Asset, ObjMeshRenderable>();
	static Map<Asset, ObjMeshComplete> objModelsComplete = new HashMap<Asset, ObjMeshComplete>();

	/**
	 * @return Returns an ObjMeshRenderable and does not requires to keep the vertex data on main memory. ( It can be present but isn't mandatory, ie if somewhere else in the code it is requested we use the complete objMesh )
	 */
	public static ObjMeshRenderable getRenderableMesh(String name)
	{
		Asset file = Client.getInstance().getContent().getAsset(name);
		if(file != null)
		{
			//Don't load something that is already loaded !
			if(objModelsComplete.containsKey(file))
				return objModelsComplete.get(file);
			
			if(objModelsRenderable.containsKey(file))
				return objModelsRenderable.get(file);
			
			//TODO load multiple formats :D
			ObjMeshRenderable obj = WavefrontLoader.loadWavefrontFormatMeshRenderable(file);
			objModelsRenderable.put(file, obj);
			
			return obj;
		}
		
		//Not found ? Throw the error mesh in
		return getCompleteMesh("./models/error.obj");
	}

	/**
	 * @return Returns an ObjMesh containing the complete data of the mesh. Can be used for both rendering and vertices work This object doesn't have the mesh data on gpu by default, it's only uploaded when accessed, take note of that for performance concerns
	 */
	public static ObjMeshComplete getCompleteMesh(String name)
	{
		Asset file = Client.getInstance().getContent().getAsset(name);
		if(file != null)
		{
			if(objModelsComplete.containsKey(file))
				return objModelsComplete.get(file);
			
			//TODO load multiple formats :D
			ObjMeshComplete obj = WavefrontLoader.loadWavefrontFormatMesh(file);
			objModelsComplete.put(file, obj);
			
			return obj;
		}
			
		//Not found ? Throw the error mesh in
		if(!name.equals("./models/error.obj"))
			return getCompleteMesh("./models/error.obj");
		//Avoid infinite loop in case someone was a moron
		throw new RuntimeException("File ./models/error.obj absent !");
	}

	public static void reloadAllModels()
	{
		objModelsRenderable.clear();
	}
}
