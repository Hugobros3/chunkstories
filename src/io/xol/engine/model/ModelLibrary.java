package io.xol.engine.model;

import io.xol.engine.model.animation.BVHAnimation;
import io.xol.engine.model.animation.BVHLibrary;

import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ModelLibrary
{
	// This class holds static model info

	static Map<String, ObjMesh> models = new HashMap<String, ObjMesh>();

	// static Map<String,Integer> displayLists = new HashMap<String,Integer>();

	public static int loadMesh(String name)
	{
		int dl = -1;
		ObjMesh mesh = new ObjMesh();
		try
		{
			/*
			 * mesh = ModelMeshLoader.loadTexturedModel(name); dl =
			 * ModelMeshLoader.createTexturedDisplayList(mesh);
			 * displayLists.put(name, dl);
			 */
			mesh.loadMesh(name);
			models.put(name, mesh);
		}
		catch (Exception e)
		{
			System.out.println("[ObjectLibrary] Coudln't load mesh \"" + name + "\"");
		}
		return dl;
	}

	public static void loadAndRenderAnimatedMesh(String name, String string, int i)
	{
		BVHAnimation animation = BVHLibrary.getAnimation(string);
		if(animation != null)
			loadAndRenderAnimatedMesh(name, animation, i);
		else
			System.out.println("Failed to obtain BVH Skeleton.");
	}

	public static void loadAndRenderAnimatedMesh(String name, BVHAnimation animationData, int frame)
	{
		if (models.containsKey(name))
			models.get(name).renderUsingBVHTree(animationData, frame);
		else
		{
			loadMesh(name);
			renderMesh(name);
		}
	}

	public static void loadAndRenderMesh(String name)
	{
		if (models.containsKey(name))
			models.get(name).render();
		else
		{
			loadMesh(name);
			renderMesh(name);
		}
	}

	public static void renderMesh(String name)
	{
		if (models.containsKey(name))
			models.get(name).render();
		else
			System.out.println("[ObjectLibrary] Coudln't find displayList for mesh \"" + name + "\"");
	}

	public static void reloadAllModels()
	{
		for (ObjMesh m : models.values())
			m.destroy();
		models.clear();
	}
}
