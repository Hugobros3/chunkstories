package io.xol.chunkstories.api.rendering;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PipelineConfiguration
{
	public DepthTestMode getDepthTestMode();

	public BlendMode getBlendMode();

	public PolygonFillMode getPolygonFillMode();
	
	//TODO depthMask

	public static enum DepthTestMode {
		DISABLED, GREATER, GREATER_OR_EQUAL, EQUAL, LESS_OR_EQUAL, LESS;
	}
	
	public static enum BlendMode {
		DISABLED, ADD, MIX, ALPHA_TEST;
	}
	
	public static enum PolygonFillMode {
		FILL, WIREFRAME, POINTS;
	}

	public void setup(RenderingInterface renderingInterface);
}