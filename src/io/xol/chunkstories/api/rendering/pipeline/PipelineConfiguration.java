package io.xol.chunkstories.api.rendering.pipeline;

import io.xol.chunkstories.api.rendering.RenderingInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PipelineConfiguration
{
	public DepthTestMode getDepthTestMode();

	public BlendMode getBlendMode();

	public CullingMode getCullingMode();
	
	public PolygonFillMode getPolygonFillMode();

	public static enum DepthTestMode {
		DISABLED, GREATER, GREATER_OR_EQUAL, EQUAL, LESS_OR_EQUAL, LESS;
	}
	
	public static enum BlendMode {
		DISABLED, ADD, MIX;
	}
	
	public static enum CullingMode {
		DISABLED, CLOCKWISE, COUNTERCLOCKWISE;
	}
	
	public static enum PolygonFillMode {
		FILL, WIREFRAME, POINTS;
	}

	public void setup(RenderingInterface renderingInterface);
}