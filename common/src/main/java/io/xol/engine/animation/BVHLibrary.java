//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.animation;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.AnimationsLibrary;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.engine.animation.BVHAnimation;



public class BVHLibrary implements AnimationsLibrary
{
	private final GameContentStore store;
	
	private static final Logger logger = LoggerFactory.getLogger("content.animations");
	public Logger logger() {
		return logger;
	}
	
	public BVHLibrary(GameContentStore store)
	{
		this.store = store;
		//this.modsManager = store.modsManager();
		
		//reload();
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

	@Override
	public Content parent() {
		return store;
	}
}
