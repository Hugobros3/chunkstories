package io.xol.chunkstories.renderer.lights;

import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.rendering.lightning.SpotLight;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.math.lalgb.vector.sp.Vector3fm;

import java.util.Iterator;

//(c) 2015-2017 XolioWare Interactive
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

		lightShader.setUniform1f("lightDecay[" + lightsBuffer + "]", light.getDecay());
		lightShader.setUniform3f("lightPos[" + lightsBuffer + "]", light.getPosition());
		lightShader.setUniform3f("lightColor[" + lightsBuffer + "]", light.getColor());
		if (light instanceof SpotLight)
		{
			SpotLight spotlight = (SpotLight)light;
			lightShader.setUniform3f("lightDir[" + lightsBuffer + "]", spotlight.getDirection());
			lightShader.setUniform1f("lightAngle[" + lightsBuffer + "]", (float) (spotlight.getAngle() / 180 * Math.PI));
		}
		else
			lightShader.setUniform1f("lightAngle[" + lightsBuffer + "]", 0f);

		//TexturesHandler.nowrap("res/textures/flashlight.png");

		lightsBuffer++;
		if (lightsBuffer == 64)
		{
			lightShader.setUniform1i("lightsToRender", lightsBuffer);
			renderingContext.drawFSQuad();
			//drawFSQuad();
			lightsBuffer = 0;
		}
	}

	public static boolean lightInFrustrum(RenderingContext renderingContext, Light light)
	{
		return renderingContext.getCamera().isBoxInFrustrum(new Vector3fm(light.getPosition().getX() - light.getDecay(), light.getPosition().getY() - light.getDecay(), light.getPosition().getZ() - light.getDecay()), new Vector3fm(light.getDecay() * 2f, light.getDecay() * 2f, light.getDecay() * 2f));
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
			lightShader.setUniform1i("lightsToRender", lightsBuffer);
			renderingContext.drawFSQuad();
		}
	}
}
