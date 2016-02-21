package io.xol.engine.model;

import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ModelLibrary
{
	// This class holds static model info

	static Map<String, ObjMesh> models = new HashMap<String, ObjMesh>();

	public static ObjMesh getMesh(String name)
	{
		ObjMesh mesh = null;
		if (models.containsKey(name))
			mesh = models.get(name);
		else
		{
			try
			{
				mesh = new ObjMesh(name);
			}
			catch (Exception e)
			{
				System.out.println("[ObjectLibrary] Coudln't load mesh \"" + name + "\"");
			}
		}
		models.put(name, mesh);
		if (mesh == null)
			mesh = getMesh("./res/models/error.obj");
		return mesh;
	}

	public static void reloadAllModels()
	{
		for (ObjMesh m : models.values())
			m.destroy();
		models.clear();
	}
}
