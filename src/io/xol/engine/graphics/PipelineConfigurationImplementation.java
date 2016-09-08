package io.xol.engine.graphics;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.rendering.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.RenderingInterface;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Abstracts the OpenGL state machine, reduces state changes by tracking them
 */
public final class PipelineConfigurationImplementation implements PipelineConfiguration
{
	public static PipelineConfigurationImplementation DEFAULT = new PipelineConfigurationImplementation(DepthTestMode.LESS_OR_EQUAL, BlendMode.DISABLED, PolygonFillMode.FILL);

	private final DepthTestMode depthTestMode;
	private final BlendMode blendMode;
	private final PolygonFillMode polygonFillMode;

	public PipelineConfigurationImplementation(DepthTestMode depthTestMode, BlendMode blendMode, PolygonFillMode polygonFillMode)
	{
		this.depthTestMode = depthTestMode;
		this.blendMode = blendMode;
		this.polygonFillMode = polygonFillMode;
	}

	@Override
	public DepthTestMode getDepthTestMode()
	{
		return depthTestMode;
	}

	@Override
	public BlendMode getBlendMode()
	{
		return blendMode;
	}

	@Override
	public PolygonFillMode getPolygonFillMode()
	{
		return polygonFillMode;
	}

	public PipelineConfigurationImplementation setDepthTestMode(DepthTestMode depthTestMode)
	{
		return new PipelineConfigurationImplementation(depthTestMode, blendMode, polygonFillMode);
	}

	public PipelineConfigurationImplementation setBlendMode(BlendMode blendMode)
	{
		return new PipelineConfigurationImplementation(depthTestMode, blendMode, polygonFillMode);
	}

	public PipelineConfigurationImplementation setPolygonFillMode(PolygonFillMode polygonFillMode)
	{
		return new PipelineConfigurationImplementation(depthTestMode, blendMode, polygonFillMode);
	}

	public boolean equals(Object o)
	{
		if (o instanceof PipelineConfigurationImplementation)
		{
			PipelineConfigurationImplementation p = (PipelineConfigurationImplementation) o;

			if (p.depthTestMode != this.depthTestMode)
				return false;

			if (p.blendMode != this.blendMode)
				return false;

			if (p.polygonFillMode != this.polygonFillMode)
				return false;

			return true;
		}
		return false;
	}

	@Override
	public void setup(RenderingInterface renderingInterface)
	{
		switch (depthTestMode)
		{
		case DISABLED:
			depth(false);
			break;
		case LESS:
			depth(true);
			depthFunc(GL_LESS);
		case LESS_OR_EQUAL:
			depth(true);
			depthFunc(GL_LEQUAL);
		case EQUAL:
			depth(true);
			depthFunc(GL_EQUAL);
		case GREATER_OR_EQUAL:
			depth(true);
			depthFunc(GL_GEQUAL);
		case GREATER:
			depth(true);
			depthFunc(GL_GREATER);
		}

		switch (blendMode)
		{
		case DISABLED:
			alphaTest(false);
			blend(false);
			break;
		case ALPHA_TEST:
			alphaTest(true);
			blend(true);
			break;
		case MIX:
			alphaTest(true);
			blend(true);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			break;
		case ADD:
			alphaTest(true);
			blend(true);
			glBlendFunc(GL_ONE, GL_ONE);
			break;
		}
		
		//TODO polyFill
	}

	private void depth(boolean on)
	{
		if (on)
		{
			if (!isBlendEnabled)
				glEnable(GL_DEPTH_TEST);
		}
		else
		{
			if (isBlendEnabled)
				glDisable(GL_DEPTH_TEST);
		}
		isBlendEnabled = on;
	}

	private void depthFunc(int depthFunc)
	{
		if (depthFunc != currentDepthFunc)
		{
			glDepthFunc(depthFunc);
			currentDepthFunc = depthFunc;
		}
	}
	
	private void alphaTest(boolean on)
	{
		if (on)
		{
			if (!isAlphaTestEnabled)
				glEnable(GL_ALPHA_TEST);
		}
		else
		{
			if (isAlphaTestEnabled)
				glDisable(GL_ALPHA_TEST);
		}
		isAlphaTestEnabled = on;
	}

	private void blend(boolean on)
	{
		if (on)
		{
			if (!isBlendingEnabled)
				glEnable(GL_BLEND);
		}
		else
		{
			if (isBlendingEnabled)
				glDisable(GL_BLEND);
		}
		isBlendingEnabled = on;
	}
	
	private static boolean isBlendEnabled = false;
	private static int currentDepthFunc = -1;
	private static boolean isAlphaTestEnabled = false;
	private static boolean isBlendingEnabled = false;
}
