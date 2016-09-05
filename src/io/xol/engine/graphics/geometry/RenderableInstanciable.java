package io.xol.engine.graphics.geometry;

import java.util.Collection;

import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderableInstanciable extends Renderable
{
	public void renderInstanced(RenderingContext renderingContext, Collection<InstanceData> instancesData);
	
	public interface InstanceData {
		
	}
}
