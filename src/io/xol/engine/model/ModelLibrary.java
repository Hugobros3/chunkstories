package io.xol.engine.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.content.GameContent;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ModelLibrary
{
	// This class holds static model info
	// static Map<String, ObjMeshLegacy> models = new HashMap<String, ObjMeshLegacy>();
	
	static Map<File, ObjMeshRenderable> objModelsRenderable = new HashMap<File, ObjMeshRenderable>();
	static Map<File, ObjMeshComplete> objModelsComplete = new HashMap<File, ObjMeshComplete>();

	/**
	 * @return Returns an ObjMeshRenderable and does not requires to keep the vertex data on main memory. ( It can be present but isn't mandatory, ie if somewhere else in the code it is requested we use the complete objMesh )
	 */
	public static ObjMeshRenderable getRenderableMesh(String name)
	{
		File file = GameContent.getFileLocation(name);
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
		File file = GameContent.getFileLocation(name);
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

	/**
	 * Returns the ObjMesh found within the game's filesystem matching the given path.
	 * 
	 * @param name
	 * @return
	 */
	/*public static ObjMeshLegacy getMesh(String name)
	{
		ObjMeshLegacy mesh = null;
		if (models.containsKey(name))
			mesh = models.get(name);
		else
		{
			try
			{
				mesh = new ObjMeshLegacy(name);
			}
			catch (Exception e)
			{
				System.out.println("[ObjectLibrary] Coudln't load mesh \"" + name + "\"");
			}
		}
		models.put(name, mesh);
		
		//Source style error mesh
		//We learn from the best
		if (mesh == null)
			mesh = getMesh("./res/models/error.obj");
		return mesh;
	}*/

	public static void reloadAllModels()
	{
		/*for (ObjMeshLegacy m : models.values())
			if (m != null)
				m.destroy();
		models.clear();*/
		
		objModelsRenderable.clear();
	}
}
