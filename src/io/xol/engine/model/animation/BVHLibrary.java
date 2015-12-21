package io.xol.engine.model.animation;

import io.xol.engine.model.animation.BVHAnimation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BVHLibrary
{
	// This class holds static model info

	static Map<String, BVHAnimation> animations = new HashMap<String, BVHAnimation>();

	// static Map<String,Integer> displayLists = new HashMap<String,Integer>();

	public static BVHAnimation loadAnimation(String name)
	{
		BVHAnimation anim = new BVHAnimation(new File(name));
		animations.put(name, anim);
		return anim;
	}

	public static BVHAnimation getAnimation(String name)
	{
		if (animations.containsKey(name))
			return animations.get(name);
		else
		{
			return loadAnimation(name);
		}
	}

	public static void reloadAllAnimations()
	{
		//for (BVHAnimation a : animations.values())
		//	a.destroy();
		animations.clear();
	}
}
