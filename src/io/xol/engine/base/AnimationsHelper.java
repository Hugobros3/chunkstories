package io.xol.engine.base;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.util.HashMap;
import java.util.Map;

public class AnimationsHelper
{

	static Map<String, Integer> animationStatus = new HashMap<String, Integer>();
	static Map<String, Long> animationMs = new HashMap<String, Long>();

	public static String animatedTextureName(String basename, int maxImages,
			int msDelay, boolean loop)
	{
		if (!animationStatus.containsKey(basename))
		{
			animationStatus.put(basename, 0);
			animationMs.put(basename, System.currentTimeMillis());
		}
		int currentValue = animationStatus.get(basename);
		if (animationMs.get(basename) + msDelay < System.currentTimeMillis())
		{
			animationMs.remove(basename);
			animationMs.put(basename, System.currentTimeMillis());
			animationStatus.remove(basename);
			currentValue++;
			if (loop && currentValue > maxImages)
				currentValue = 0;
			else if (!loop && currentValue > maxImages)
				currentValue--;
			animationStatus.put(basename, currentValue);
		}
		return basename + "_" + currentValue;
	}
}
