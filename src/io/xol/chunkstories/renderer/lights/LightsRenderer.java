package io.xol.chunkstories.renderer.lights;

import io.xol.engine.math.lalgb.Vector3f;

import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.rendering.SpotLight;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;

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
		
		/*Vector3f centerSphere = new Vector3f(light.getPosition());
		Camera camera = renderingContext.getCamera();
		double coneAngle = (camera.fov) * (camera.width / (camera.height * 1f));
		coneAngle = coneAngle / 180d * Math.PI;
		Vector3f v = new Vector3f();
		Vector3f.sub(centerSphere, new Vector3f((float)camera.camPosX, (float)camera.camPosY, (float)camera.camPosZ), v);
		Vector3f viewerCamDirVector = camera.getViewDirection();
		
		viewerCamDirVector.normalise(viewerCamDirVector);
		float a = Vector3f.dot(v, viewerCamDirVector);
		double b = a * Math.tan(coneAngle);
		double c = Math.sqrt(Vector3f.dot(v, v) - a * a);
		double d = c - b;
		double e = d * Math.cos(coneAngle);
		if (e >= Math.sqrt(light.getDecay() * light.getDecay() * 3)) // R
			return false;
		return true;*/
	}

	static ShaderProgram lightShader;
	
	public static void renderPendingLights(RenderingContext renderingContext)
	{
		lightShader = ShadersLibrary.getShaderProgram("light");
		lightsBuffer = 0;
		//Render entities lights
		for (Light light : renderingContext.lights)
			renderDefferedLight(renderingContext, light);
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
