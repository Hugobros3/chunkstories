package io.xol.engine.animation;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.content.GameContentStore;
import io.xol.engine.animation.BVHAnimation;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BVHLibrary
{
	// This class holds static model info

	private final GameContentStore store;
	//private final ModsManager modsManager;
	
	public BVHLibrary(GameContentStore store)
	{
		this.store = store;
		//this.modsManager = store.modsManager();
		
		reload();
	}
	
	Map<String, BVHAnimation> animations = new HashMap<String, BVHAnimation>();
	
	public BVHAnimation loadAnimation(String name)
	{
		BVHAnimation anim = new BVHAnimation(store.getAsset(name));
		animations.put(name, anim);
		return anim;
	}

	public BVHAnimation getAnimation(String name)
	{
		if (animations.containsKey(name))
			return animations.get(name);
		else
		{
			return loadAnimation(name);
		}
	}

	public void reload()
	{
		//for (BVHAnimation a : animations.values())
		//	a.destroy();
		animations.clear();
	}
}
