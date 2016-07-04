package io.xol.chunkstories.renderer.lights;

import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.math.lalgb.Vector3f;

import java.util.Iterator;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.rendering.SpotLight;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LightsRenderer
{
	static int lightsBuffer = 0;

	public static void renderDefferedLight(RenderingContext renderingContext, Light light)
	{
		// Light culling
		if (!lightInFrustrum(renderingContext, light))
			return;

		lightShader.setUniformFloat("lightDecay[" + lightsBuffer + "]", light.getDecay());
		lightShader.setUniformFloat3("lightPos[" + lightsBuffer + "]", light.getPosition());
		lightShader.setUniformFloat3("lightColor[" + lightsBuffer + "]", light.getColor());
		if (light instanceof SpotLight)
		{
			SpotLight spotlight = (SpotLight)light;
			lightShader.setUniformFloat3("lightDir[" + lightsBuffer + "]", spotlight.getDirection());
			lightShader.setUniformFloat("lightAngle[" + lightsBuffer + "]", (float) (spotlight.getAngle() / 180 * Math.PI));
		}
		else
			lightShader.setUniformFloat("lightAngle[" + lightsBuffer + "]", 0f);

		//TexturesHandler.nowrap("res/textures/flashlight.png");

		lightsBuffer++;
		if (lightsBuffer == 64)
		{
			lightShader.setUniformInt("lightsToRender", lightsBuffer);
			ObjectRenderer.drawFSQuad(lightShader.getVertexAttributeLocation("vertexIn"));
			//drawFSQuad();
			lightsBuffer = 0;
		}
	}

	public static boolean lightInFrustrum(RenderingContext renderingContext, Light light)
	{
		return renderingContext.getCamera().isBoxInFrustrum(light.getPosition(), new Vector3f(light.getDecay() * 2f, light.getDecay() * 2f, light.getDecay() * 2f));
	}

	static ShaderProgram lightShader;
	
	public static void renderPendingLights(RenderingContext renderingContext)
	{
		lightShader = ShadersLibrary.getShaderProgram("light");
		lightsBuffer = 0;
		//Render entities lights
		Iterator<Light> lights = renderingContext.getAllLights();
		while(lights.hasNext())
		{
			renderDefferedLight(renderingContext, lights.next());
			lights.remove();
		}
		//Render particles's lights
		//Client.world.particlesHolder.renderLights(this);
		// Render remaining lights
		if (lightsBuffer > 0)
		{
			lightShader.setUniformInt("lightsToRender", lightsBuffer);
			ObjectRenderer.drawFSQuad(lightShader.getVertexAttributeLocation("vertexIn"));
		}
	}
}
