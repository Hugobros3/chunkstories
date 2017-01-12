package io.xol.engine.animation;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.client.Client;
import io.xol.engine.animation.BVHAnimation;

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
		System.out.println(name);
		BVHAnimation anim = new BVHAnimation(Client.getInstance().getContent().getAsset(name));
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
