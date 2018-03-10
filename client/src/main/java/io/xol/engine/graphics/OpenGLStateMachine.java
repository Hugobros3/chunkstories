//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.StateMachine;

import static org.lwjgl.opengl.GL11.*;

/**
 * Abstracts the OpenGL state machine, reduces state changes by tracking them
 */
public final class OpenGLStateMachine implements StateMachine
{
	public static OpenGLStateMachine DEFAULT = new OpenGLStateMachine(DepthTestMode.LESS_OR_EQUAL, BlendMode.DISABLED, CullingMode.COUNTERCLOCKWISE, PolygonFillMode.FILL);

	private DepthTestMode depthTestMode;
	private BlendMode blendMode;
	private CullingMode cullingMode;
	private PolygonFillMode polygonFillMode;

	public OpenGLStateMachine(DepthTestMode depthTestMode, BlendMode blendMode, CullingMode cullingMode, PolygonFillMode polygonFillMode)
	{
		this.depthTestMode = depthTestMode;
		this.blendMode = blendMode;
		this.cullingMode = cullingMode;
		this.polygonFillMode = polygonFillMode;
	}

	@Override
	public DepthTestMode getDepthTestMode() {
		return depthTestMode;
	}

	@Override
	public BlendMode getBlendMode() {
		return blendMode;
	}

	@Override
	public CullingMode getCullingMode() {
		return cullingMode;
	}

	@Override
	public PolygonFillMode getPolygonFillMode() {
		return polygonFillMode;
	}

	public void setDepthTestMode(DepthTestMode depthTestMode) {
		this.depthTestMode = depthTestMode;
	}

	public void setBlendMode(BlendMode blendMode) {
		this.blendMode = blendMode;
	}

	public void setCullingMode(CullingMode cullingMode) {
		this.cullingMode = cullingMode;
	}

	public void setPolygonFillMode(PolygonFillMode polygonFillMode) {
		this.polygonFillMode = polygonFillMode;
	}

	public void setup(RenderingInterface renderingInterface)
	{
		switch (depthTestMode)
		{
		case DISABLED:
			depth(false);
			//depthFunc(-1);
			break;
		case LESS:
			depth(true);
			depthFunc(GL_LESS);
			break;
		case LESS_OR_EQUAL:
			depth(true);
			depthFunc(GL_LEQUAL);
			break;
		case EQUAL:
			depth(true);
			depthFunc(GL_EQUAL);
			break;
		case GREATER_OR_EQUAL:
			depth(true);
			depthFunc(GL_GEQUAL);
			break;
		case GREATER:
			depth(true);
			depthFunc(GL_GREATER);
			break;
		}

		switch (blendMode)
		{
		case DISABLED:
			//alphaTest(false);
			blend(false);
			break;
		//case ALPHA_TEST:
		//	alphaTest(true);
		//	blend(false);
		//	break;
		case MIX:
			//alphaTest(false);
			blend(true);
			blendFunc(blendMode);
			break;
		case ADD:
			//alphaTest(true);
			blend(true);
			blendFunc(blendMode);
			break;
		case PREMULT_ALPHA:
			blend(true);
			blendFunc(blendMode);
		}

		//Culling mode
		switch (cullingMode)
		{
		case DISABLED:
			cull(false);
			break;
		case CLOCKWISE:
			cull(true);
			cullFF(GL_CW);
			break;
		case COUNTERCLOCKWISE:
			cull(true);
			cullFF(GL_CCW);
			break;
		}

		if (polygonFillMode != currentPolygonFillMode)
		{
			switch (polygonFillMode)
			{
			case FILL:
				glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
				break;
			case POINTS:
				glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
				break;
			case WIREFRAME:
				glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
				break;
			default:
				break;

			}
			currentPolygonFillMode = polygonFillMode;
		}
	}

	private void blendFunc(BlendMode blendMode)
	{
		if (blendMode.ordinal() == currentBlendFunc)
			return;

		switch (blendMode)
		{
		case ADD:
			glBlendFunc(GL_ONE, GL_ONE);
			break;
		case MIX:
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			break;
		case PREMULT_ALPHA:
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			break;
		default:
			break;
		}

		currentBlendFunc = blendMode.ordinal();
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

	private void cull(boolean on)
	{
		if (on)
		{
			if (!isCullingEnabled)
				glEnable(GL_CULL_FACE);
		}
		else
		{
			if (isCullingEnabled)
				glDisable(GL_CULL_FACE);
		}
		isCullingEnabled = on;
	}

	private void cullFF(int mode)
	{
		if (mode != currentCullFunc)
		{
			glFrontFace(mode);
			currentCullFunc = mode;
		}
	}

	private static boolean isBlendEnabled = false;
	private static int currentDepthFunc = -1;
	private static boolean isBlendingEnabled = false;
	private static int currentBlendFunc = -1;
	private static boolean isCullingEnabled = false;
	private static int currentCullFunc = -1;
	private static PolygonFillMode currentPolygonFillMode = PolygonFillMode.FILL;
}
